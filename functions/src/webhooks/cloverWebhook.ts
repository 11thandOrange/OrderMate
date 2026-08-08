/**
 * Clover Webhook Handler
 * Issue #98: Create webhooks for user lifecycle events
 * Issue #142: Auth verification, error handling, and idempotency
 *
 * Handles:
 * - Verification: GET request with verificationCode
 * - APP_INSTALLED: Store merchant info
 * - APP_UNINSTALLED: Update uninstall date
 * - SUBSCRIPTION_CHANGED: Update subscription
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.database();

/**
 * Verifies the X-Clover-Auth header Clover sends with each webhook event
 * against CLOVER_AUTH_CODE. Without this, the endpoint accepts unauthenticated
 * POSTs and anyone can forge lifecycle events for any merchantId.
 * @param {functions.https.Request} req - The incoming request
 * @return {boolean} Whether the request is authorized
 */
function isAuthorizedEvent(req: functions.https.Request): boolean {
  const expected = process.env.CLOVER_AUTH_CODE;
  if (!expected) {
    console.error("CLOVER_AUTH_CODE not configured - rejecting webhook event");
    return false;
  }
  return req.headers["x-clover-auth"] === expected;
}

/**
 * Main Clover webhook endpoint
 * URL: https://{project}.cloudfunctions.net/cloverWebhook
 */
export const cloverWebhook = functions.https.onRequest(async (req, res) => {
  // Handle GET requests (health check)
  if (req.method === "GET") {
    res.status(200).send("OK");
    return;
  }

  // Handle POST requests
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  // Handle Clover's verification request (POST with verificationCode)
  // Clover sends POST with {"verificationCode":"xxx"}
  // Respond with 200 OK, then log the code for manual entry in Clover UI.
  // Intentionally not auth-gated: this is the one-time setup handshake used to
  // confirm dashboard control before CLOVER_AUTH_CODE is even configured.
  if (req.body.verificationCode) {
    const code = req.body.verificationCode;
    const requestId = req.headers["x-cloud-trace-context"] || "no-trace";
    // Respond immediately to avoid timeout/retry
    res.status(200).send("OK");
    console.log(`VERIFICATION CODE: ${code} | Request: ${requestId}`);
    return;
  }

  // Every other POST is a real lifecycle event - require auth
  if (!isAuthorizedEvent(req)) {
    res.status(401).send("Unauthorized");
    return;
  }

  // Handle actual webhook events
  const {appId, merchants} = req.body;

  // If it's a Clover webhook event format
  if (appId && merchants) {
    console.log(`Received Clover webhook for app ${appId}`);
    try {
      const {hadFailures} = await handleCloverWebhookEvent(req.body);
      if (hadFailures) {
        // At least one merchant's update failed. Respond with an error so
        // Clover's own retry mechanism re-delivers this payload - merchants
        // that already succeeded are unaffected by the retry (writes are
        // idempotent), and the failed one gets another chance instead of
        // being silently dropped.
        res.status(500).send("Partial failure processing webhook - see logs");
      } else {
        res.status(200).send("OK");
      }
    } catch (error) {
      console.error("Error processing webhook:", error);
      res.status(500).send("Internal Server Error");
    }
    return;
  }

  // Legacy format support (merchantId, type)
  const {merchantId, type} = req.body;
  if (!merchantId || !type) {
    console.error("Unknown webhook payload format");
    res.status(400).send("Bad Request: Unknown payload format");
    return;
  }

  console.log(`Received Clover webhook: ${type} for merchant ${merchantId}`);

  try {
    switch (type) {
    case "APP_INSTALLED":
      await handleInstall(merchantId, req.body);
      break;

    case "APP_UNINSTALLED":
      await handleUninstall(merchantId, req.body);
      break;

    case "SUBSCRIPTION_CHANGED":
      await handleSubscriptionChange(merchantId, req.body);
      break;

    default:
      console.log(`Unhandled webhook type: ${type}`);
    }

    res.status(200).send("OK");
  } catch (error) {
    console.error("Error processing webhook:", error);
    res.status(500).send("Internal Server Error");
  }
});

interface CloverUpdate {
  objectId: string;
  type: "CREATE" | "UPDATE" | "DELETE";
  ts: number;
}

interface CloverWebhookPayload {
  appId: string;
  merchants: Record<string, CloverUpdate[]>;
}

/**
 * Handle Clover webhook event in standard format.
 *
 * Isolates failures per-update so one merchant's error doesn't stop the rest
 * of the batch from being processed - but still reports whether anything
 * failed, so the caller can tell Clover to retry the delivery rather than
 * silently treating a failed write as successful.
 * @param {CloverWebhookPayload} payload - The Clover webhook payload
 * @return {Promise<{hadFailures: boolean}>} Whether any update failed
 */
async function handleCloverWebhookEvent(
  payload: CloverWebhookPayload
): Promise<{hadFailures: boolean}> {
  const {appId, merchants} = payload;
  let hadFailures = false;

  for (const [merchantId, updates] of Object.entries(merchants)) {
    for (const update of updates) {
      const eventKey = update.objectId.split(":")[0];

      try {
        // A = App events (install, uninstall, subscription change)
        if (eventKey === "A") {
          if (update.type === "CREATE") {
            await handleInstall(merchantId, {appId}, update.ts);
          } else if (update.type === "DELETE") {
            await handleUninstall(merchantId, {}, update.ts);
          } else if (update.type === "UPDATE") {
            // Subscription change
            await handleSubscriptionChange(merchantId, {}, update.ts);
          }
        }

        // Log other event types for now
        console.log(`Event: ${eventKey}, Type: ${update.type}, ` +
          `Merchant: ${merchantId}, Object: ${update.objectId}`);
      } catch (error) {
        hadFailures = true;
        console.error(
          `Failed to process event for merchant ${merchantId}, ` +
          `object ${update.objectId}:`, error
        );
      }
    }
  }

  return {hadFailures};
}

/**
 * Builds a deterministic event log key so a retried webhook delivery updates
 * the same event record instead of creating a duplicate. Falls back to the
 * current time when no source timestamp is available (e.g. the manual-test
 * legacy payload format, which Clover never retries).
 * @param {string} merchantId - The Clover merchant ID
 * @param {string} type - The event type (e.g. "INSTALL")
 * @param {number} sourceTs - The webhook update's own timestamp, if known
 * @return {string} A Firebase-key-safe, deterministic event id
 */
function buildEventId(merchantId: string, type: string, sourceTs?: number): string {
  const tsPart = sourceTs !== undefined ? String(sourceTs) : String(Date.now());
  return `${merchantId}_${type}_${tsPart}`.replace(/[.#$[\]/]/g, "_");
}

interface WebhookPayload {
  appId?: string;
  data?: Record<string, unknown>;
  oldPlan?: string;
  newPlan?: string;
}

/**
 * Handle APP_INSTALLED webhook.
 *
 * name/email/storeName are left blank here - OrderMate is a native
 * Android-only Clover app (no "Web" REST client registered), so this
 * server-side function has no way to obtain a per-merchant Clover REST API
 * token to enrich these fields. Only the on-device app itself can do that,
 * via CloverAuth.authenticate() against the CloverAccount already present
 * on the device - filling these in would be a separate, on-device feature,
 * not something this webhook can do.
 * @param {string} merchantId - The Clover merchant ID
 * @param {WebhookPayload} payload - The webhook payload
 */
async function handleInstall(
  merchantId: string,
  payload: WebhookPayload,
  sourceTs?: number
): Promise<void> {
  console.log(`Processing install for merchant ${merchantId}`);

  const timestamp = admin.database.ServerValue.TIMESTAMP;

  // Store merchant info
  await db.ref(`merchants/${merchantId}/merchantInfo`).set({
    merchantId: merchantId,
    name: "",
    email: "",
    storeName: "",
    installDate: timestamp,
    uninstallDate: null,
    lastActiveDate: timestamp,
  });

  // Initialize subscription with free plan
  await db.ref(`merchants/${merchantId}/subscription`).set({
    plan: "free",
    status: "active",
    monthlyDueDate: 1,
  });

  // Record install event - deterministic key so a Clover retry updates this
  // same record instead of creating a duplicate.
  const eventId = buildEventId(merchantId, "INSTALL", sourceTs);
  await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
    id: eventId,
    type: "INSTALL",
    timestamp: timestamp,
    details: {source: "webhook", appId: payload.appId},
    processed: false,
  });

  console.log(`Install processed for merchant ${merchantId}`);
}

/**
 * Handle APP_UNINSTALLED webhook
 * @param {string} merchantId - The Clover merchant ID
 * @param {WebhookPayload} payload - The webhook payload
 */
async function handleUninstall(
  merchantId: string,
  payload: WebhookPayload,
  sourceTs?: number
): Promise<void> {
  console.log(`Processing uninstall for merchant ${merchantId}`);

  const timestamp = admin.database.ServerValue.TIMESTAMP;

  // Update uninstall date
  const uninstallPath = `merchants/${merchantId}/merchantInfo/uninstallDate`;
  await db.ref(uninstallPath).set(timestamp);

  // Update subscription status
  await db.ref(`merchants/${merchantId}/subscription/status`).set("cancelled");

  // Record uninstall event - deterministic key so a Clover retry updates this
  // same record instead of creating a duplicate.
  const eventId = buildEventId(merchantId, "UNINSTALL", sourceTs);
  await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
    id: eventId,
    type: "UNINSTALL",
    timestamp: timestamp,
    details: payload.data || {},
    processed: false,
  });

  console.log(`Uninstall processed for merchant ${merchantId}`);
}

/**
 * Handle SUBSCRIPTION_CHANGED webhook
 * @param {string} merchantId - The Clover merchant ID
 * @param {WebhookPayload} payload - The webhook payload
 */
async function handleSubscriptionChange(
  merchantId: string,
  payload: WebhookPayload,
  sourceTs?: number
): Promise<void> {
  console.log(`Processing subscription change for merchant ${merchantId}`);

  const oldPlan = payload.oldPlan || "free";
  const newPlan = payload.newPlan || "free";
  const timestamp = admin.database.ServerValue.TIMESTAMP;

  // Update subscription
  await db.ref(`merchants/${merchantId}/subscription`).update({
    plan: newPlan,
    status: "active",
  });

  // Determine if upgrade or downgrade
  const planRanking: Record<string, number> = {free: 0, basic: 1, premium: 2};
  const isUpgrade = (planRanking[newPlan] || 0) > (planRanking[oldPlan] || 0);
  const upgradeType = "SUBSCRIPTION_UPGRADE";
  const downgradeType = "SUBSCRIPTION_DOWNGRADE";
  const eventType = isUpgrade ? upgradeType : downgradeType;

  // Record event - deterministic key so a Clover retry updates this same
  // record instead of creating a duplicate.
  const eventId = buildEventId(merchantId, eventType, sourceTs);
  await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
    id: eventId,
    type: eventType,
    timestamp: timestamp,
    details: {oldPlan, newPlan},
    processed: false,
  });

  const logMsg = `Subscription change: ${oldPlan} -> ${newPlan}`;
  console.log(`${logMsg} for merchant ${merchantId}`);
}
