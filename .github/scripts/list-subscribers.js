/**
 * Lists OrderMate merchants with subscription status and last recorded activity.
 *
 * Reads merchants/{merchantId} and reports, per merchant, the most recent
 * timestamp found across three sources, since merchantInfo.lastActiveDate is
 * only written by the lifecycle webhook and is absent for merchants that
 * installed before it existed:
 *
 *   merchantInfo.lastActiveDate  - webhook-recorded activity
 *   meta.updatedAt               - last write by the app itself
 *   events/{eventId}.timestamp   - most recent lifecycle event
 *
 * Auth comes from FIREBASE_SERVICE_ACCOUNT and FIREBASE_DATABASE_URL.
 */

const admin = require("firebase-admin");

const ACTIVE = "active";
const PLAN_FREE = "free";
// Clover merchant IDs are 13 characters; anything else under /merchants is a
// stray node written to the wrong path, not a merchant.
const MERCHANT_ID_LENGTH = 13;

function required(name) {
  const value = process.env[name];
  if (!value) {
    console.error(`Missing ${name}. Add it under Settings > Secrets and variables > Actions.`);
    process.exit(1);
  }
  return value;
}

function parseServiceAccount(raw) {
  try {
    return JSON.parse(raw);
  } catch (err) {
    console.error("FIREBASE_SERVICE_ACCOUNT is not valid JSON - paste the whole downloaded key file.");
    process.exit(1);
  }
}

function toMillis(value) {
  const millis = Number(value);
  return Number.isFinite(millis) && millis > 0 ? millis : null;
}

function formatDate(millis) {
  return millis ? new Date(millis).toISOString().slice(0, 10) : "-";
}

/**
 * Picks the most recent timestamp across every source that recorded one.
 * @param {object} merchant - The merchant subtree
 * @return {{millis: ?number, source: string}} Latest activity and its origin
 */
function lastActivity(merchant) {
  const info = merchant.merchantInfo || {};
  const meta = merchant.meta || {};
  const events = merchant.events || {};

  const candidates = [
    {millis: toMillis(info.lastActiveDate), source: "merchantInfo"},
    {millis: toMillis(info.uninstallDate), source: "uninstall"},
    {millis: toMillis(info.installDate), source: "install"},
    {millis: toMillis(meta.updatedAt), source: "meta.updatedAt"},
    {millis: toMillis(meta.createdAt), source: "meta.createdAt"},
    ...Object.values(events).map((e) => ({
      millis: toMillis(e && e.timestamp),
      source: "event",
    })),
  ].filter((c) => c.millis !== null);

  if (candidates.length === 0) return {millis: null, source: "none"};
  return candidates.reduce((a, b) => (b.millis > a.millis ? b : a));
}

/**
 * Summarises merchants/{id}/subscription/billingHistory, which holds one
 * BillingRecord per payment (amount, dueDate, paidDate, status, lateDays).
 * @param {object} subscription - The merchant's subscription subtree
 * @return {object} Payment counts, amounts and the most recent payment date
 */
function billingSummary(subscription) {
  const history = (subscription && subscription.billingHistory) || {};
  const records = Object.values(history).filter(Boolean);

  const paid = records.filter((r) => r.status === "paid");
  const late = records.filter((r) => r.status === "late" || Number(r.lateDays) > 0);
  const unpaid = records.filter((r) => r.status === "pending" || r.status === "missed");

  const paidTotal = paid.reduce((sum, r) => sum + (Number(r.amount) || 0), 0);
  const owedTotal = unpaid.reduce((sum, r) => sum + (Number(r.amount) || 0), 0);
  const lastPaid = paid
    .map((r) => toMillis(r.paidDate))
    .filter(Boolean)
    .sort((a, b) => b - a)[0] || null;

  return {
    records: records.length,
    paidCount: paid.length,
    lateCount: late.length,
    unpaidCount: unpaid.length,
    paidTotal,
    owedTotal,
    lastPaid,
  };
}

function rowsFor(snapshot) {
  return Object.entries(snapshot.val() || {})
    .filter(([key]) => key.length === MERCHANT_ID_LENGTH)
    .map(([merchantId, merchant]) => {
      const m = merchant || {};
      const sub = m.subscription || {};
      const info = m.merchantInfo || {};
      const activity = lastActivity(m);
      const billing = billingSummary(sub);
      return {
        merchantId,
        plan: typeof sub.plan === "string" ? sub.plan : PLAN_FREE,
        status: typeof sub.status === "string" ? sub.status : ACTIVE,
        hasRecord: typeof sub.status === "string",
        dueDay: Number(sub.monthlyDueDate) || null,
        billing,
        installed: formatDate(toMillis(info.installDate)),
        lastActivityMillis: activity.millis,
        lastActivity: formatDate(activity.millis),
        source: activity.source,
      };
    })
    .sort((a, b) => (b.lastActivityMillis || 0) - (a.lastActivityMillis || 0));
}

function printTable(title, rows) {
  console.log(`\n${title} (${rows.length})`);
  console.log("-".repeat(title.length + 6));
  if (rows.length === 0) {
    console.log("(none)");
    return;
  }
  console.table(rows.map((r) => ({
    merchantId: r.merchantId,
    plan: r.plan,
    status: r.status + (r.hasRecord ? "" : " (no record)"),
    dueDay: r.dueDay || "-",
    payments: r.billing.records,
    paid: r.billing.paidCount,
    late: r.billing.lateCount,
    unpaid: r.billing.unpaidCount,
    paidTotal: r.billing.paidTotal ? r.billing.paidTotal.toFixed(2) : "-",
    owed: r.billing.owedTotal ? r.billing.owedTotal.toFixed(2) : "-",
    lastPaid: formatDate(r.billing.lastPaid),
    lastActivity: r.lastActivity,
  })));
}

async function main() {
  admin.initializeApp({
    credential: admin.credential.cert(parseServiceAccount(required("FIREBASE_SERVICE_ACCOUNT"))),
    databaseURL: required("FIREBASE_DATABASE_URL"),
  });

  const snapshot = await admin.database().ref("merchants").once("value");
  const rows = rowsFor(snapshot);

  if (rows.length === 0) {
    console.log("No merchants found under /merchants.");
    return;
  }

  printTable("ACTIVE", rows.filter((r) => r.status === ACTIVE));
  printTable("NOT ACTIVE", rows.filter((r) => r.status !== ACTIVE));

  const noActivity = rows.filter((r) => r.lastActivityMillis === null);
  console.log("\nSUMMARY");
  console.log("-------");
  console.log(`Merchants          : ${rows.length}`);
  console.log(`Active             : ${rows.filter((r) => r.status === ACTIVE).length}`);
  console.log(`Not active         : ${rows.filter((r) => r.status !== ACTIVE).length}`);
  console.log(`Paying (plan != free) : ${rows.filter((r) => r.plan !== PLAN_FREE).length}`);
  console.log(`No timestamp anywhere : ${noActivity.length}`);

  const withBilling = rows.filter((r) => r.billing.records > 0);
  const revenue = rows.reduce((sum, r) => sum + r.billing.paidTotal, 0);
  const outstanding = rows.reduce((sum, r) => sum + r.billing.owedTotal, 0);
  console.log(`\nBILLING`);
  console.log("-------");
  console.log(`Merchants with any billing record : ${withBilling.length}`);
  console.log(`Total paid       : ${revenue.toFixed(2)}`);
  console.log(`Total outstanding: ${outstanding.toFixed(2)}`);
  const planCounts = rows.reduce((acc, r) => {
    acc[r.plan] = (acc[r.plan] || 0) + 1;
    return acc;
  }, {});
  console.log(`Plans : ${JSON.stringify(planCounts)}`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Query failed:", err.message);
    process.exit(1);
  });
