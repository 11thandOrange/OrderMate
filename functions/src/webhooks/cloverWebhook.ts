/**
 * Clover Webhook Handler
 * Issue #98: Create webhooks for user lifecycle events
 *
 * Handles:
 * - Verification: GET request with verificationCode
 * - APP_INSTALLED: Store merchant info
 * - APP_UNINSTALLED: Update uninstall date
 * - SUBSCRIPTION_CHANGED: Update subscription
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import axios from "axios";

const db = admin.database();

/**
 * Main Clover webhook endpoint
 * URL: https://{project}.cloudfunctions.net/cloverWebhook
 */
export const cloverWebhook = functions.https.onRequest(async (req, res) => {
  // Handle Clover's verification request (GET with verificationCode)
  if (req.method === "GET") {
    const verificationCode = req.query.verificationCode;
    if (verificationCode) {
      console.log("Webhook verification request received");
      res.set("Content-Type", "text/plain");
      res.status(200).send(String(verificationCode));
      return;
    }
    // GET without verificationCode - return OK for health checks
    res.status(200).send("OK");
    return;
  }

  // Handle actual webhook events (POST)
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const {merchantId, type} = req.body;
  if (!merchantId || !type) {
    console.error("Missing merchantId or type in webhook payload");
    res.status(400).send("Bad Request: Missing merchantId or type");
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

interface MerchantData {
  name?: string;
  owner?: {name?: string; email?: string};
}

/**
 * Fetch merchant details from Clover API
 * @param {string} merchantId - The Clover merchant ID
 * @return {Promise<MerchantData>} Merchant data from Clover
 */
async function fetchMerchantFromClover(
  merchantId: string
): Promise<MerchantData> {
  const apiToken = process.env.CLOVER_API_TOKEN;
  const baseUrl = process.env.CLOVER_BASE_URL || "https://api.clover.com";

  if (!apiToken) {
    console.warn("CLOVER_API_TOKEN not configured");
    return {};
  }

  try {
    const response = await axios.get(
      `${baseUrl}/v3/merchants/${merchantId}?expand=owner`,
      {
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
        },
      }
    );

    return response.data;
  } catch (error) {
    console.error("Failed to fetch merchant from Clover:", error);
    return {};
  }
}

interface WebhookPayload {
  appId?: string;
  data?: Record<string, unknown>;
  oldPlan?: string;
  newPlan?: string;
}

/**
 * Handle APP_INSTALLED webhook
 * @param {string} merchantId - The Clover merchant ID
 * @param {WebhookPayload} payload - The webhook payload
 */
async function handleInstall(
  merchantId: string,
  payload: WebhookPayload
): Promise<void> {
  console.log(`Processing install for merchant ${merchantId}`);

  const merchantData = await fetchMerchantFromClover(merchantId);
  const timestamp = admin.database.ServerValue.TIMESTAMP;

  // Store merchant info
  await db.ref(`merchants/${merchantId}/merchantInfo`).set({
    merchantId: merchantId,
    name: merchantData.owner?.name || "",
    email: merchantData.owner?.email || "",
    storeName: merchantData.name || "",
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

  // Record install event
  const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
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
  payload: WebhookPayload
): Promise<void> {
  console.log(`Processing uninstall for merchant ${merchantId}`);

  const timestamp = admin.database.ServerValue.TIMESTAMP;

  // Update uninstall date
  const uninstallPath = `merchants/${merchantId}/merchantInfo/uninstallDate`;
  await db.ref(uninstallPath).set(timestamp);

  // Update subscription status
  await db.ref(`merchants/${merchantId}/subscription/status`).set("cancelled");

  // Record uninstall event
  const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
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
  payload: WebhookPayload
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

  // Record event
  const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
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
