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

function rowsFor(snapshot) {
  return Object.entries(snapshot.val() || {})
    .filter(([key]) => key.length === MERCHANT_ID_LENGTH)
    .map(([merchantId, merchant]) => {
      const m = merchant || {};
      const sub = m.subscription || {};
      const info = m.merchantInfo || {};
      const activity = lastActivity(m);
      return {
        merchantId,
        plan: typeof sub.plan === "string" ? sub.plan : PLAN_FREE,
        status: typeof sub.status === "string" ? sub.status : ACTIVE,
        hasRecord: typeof sub.status === "string",
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
    installed: r.installed,
    lastActivity: r.lastActivity,
    source: r.source,
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
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Query failed:", err.message);
    process.exit(1);
  });
