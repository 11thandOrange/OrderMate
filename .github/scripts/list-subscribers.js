/**
 * Lists OrderMate merchants grouped by subscription status.
 *
 * Reads merchants/{merchantId}/subscription and merchantInfo from the
 * Realtime Database. Mirrors SubscriptionInfo.fromMap(): a merchant with no
 * subscription node defaults to plan "free", status "active".
 *
 * Auth comes from FIREBASE_SERVICE_ACCOUNT (the service-account JSON) and
 * FIREBASE_DATABASE_URL, both supplied as GitHub secrets.
 */

const admin = require("firebase-admin");

const ACTIVE = "active";
const PLAN_FREE = "free";

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

function subscriptionOf(merchant) {
  const sub = (merchant && merchant.subscription) || {};
  return {
    plan: typeof sub.plan === "string" ? sub.plan : PLAN_FREE,
    status: typeof sub.status === "string" ? sub.status : ACTIVE,
    // Flags merchants whose status is inferred rather than stored, since those
    // read as active without anyone having written a subscription.
    defaulted: typeof sub.status !== "string",
  };
}

function formatDate(millis) {
  if (!millis) return "-";
  return new Date(Number(millis)).toISOString().slice(0, 10);
}

function rowsFor(snapshot) {
  const merchants = snapshot.val() || {};
  return Object.entries(merchants).map(([merchantId, merchant]) => {
    const info = (merchant && merchant.merchantInfo) || {};
    const {plan, status, defaulted} = subscriptionOf(merchant);
    return {
      merchantId,
      storeName: info.storeName || info.name || "-",
      plan,
      status,
      defaulted,
      installDate: formatDate(info.installDate),
      uninstallDate: formatDate(info.uninstallDate),
      lastActiveDate: formatDate(info.lastActiveDate),
    };
  });
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
    store: r.storeName,
    plan: r.plan,
    status: r.status + (r.defaulted ? " (no record)" : ""),
    installed: r.installDate,
    uninstalled: r.uninstallDate,
    lastActive: r.lastActiveDate,
  })));
}

async function main() {
  const serviceAccount = parseServiceAccount(required("FIREBASE_SERVICE_ACCOUNT"));
  const databaseURL = required("FIREBASE_DATABASE_URL");

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL,
  });

  const snapshot = await admin.database().ref("merchants").once("value");
  const rows = rowsFor(snapshot);

  if (rows.length === 0) {
    console.log("No merchants found under /merchants.");
    console.log("Check that FIREBASE_DATABASE_URL points at the right instance.");
    return;
  }

  const active = rows.filter((r) => r.status === ACTIVE);
  const inactive = rows.filter((r) => r.status !== ACTIVE);

  printTable("ACTIVE", active);
  printTable("NOT ACTIVE", inactive);

  const paying = active.filter((r) => r.plan !== PLAN_FREE);
  const byStatus = rows.reduce((acc, r) => {
    acc[r.status] = (acc[r.status] || 0) + 1;
    return acc;
  }, {});
  const byPlan = rows.reduce((acc, r) => {
    acc[r.plan] = (acc[r.plan] || 0) + 1;
    return acc;
  }, {});

  console.log("\nSUMMARY");
  console.log("-------");
  console.log(`Total merchants : ${rows.length}`);
  console.log(`Active          : ${active.length}`);
  console.log(`Not active      : ${inactive.length}`);
  console.log(`Paying (active, plan != free) : ${paying.length}`);
  console.log(`By status : ${JSON.stringify(byStatus)}`);
  console.log(`By plan   : ${JSON.stringify(byPlan)}`);

  const defaulted = rows.filter((r) => r.defaulted).length;
  if (defaulted > 0) {
    console.log(`\nNote: ${defaulted} merchant(s) have no subscription record and are counted as active/free, matching SubscriptionInfo.fromMap() defaults.`);
  }
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Query failed:", err.message);
    process.exit(1);
  });
