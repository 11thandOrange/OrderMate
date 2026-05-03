/**
 * Clover Webhook Handler
 * Issue #98: Create webhooks for user lifecycle events
 * 
 * Handles:
 * - APP_INSTALLED: Store merchant info, send welcome email
 * - APP_UNINSTALLED: Update uninstall date, send farewell email
 * - SUBSCRIPTION_CHANGED: Update subscription, send upgrade/downgrade email
 * - METERED_EVENT: Track usage tier breaks
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as crypto from 'crypto';
import axios from 'axios';
import { sendMailchimpEmail } from '../email/mailchimp';

const db = admin.database();

// Webhook event types from Clover
type CloverEventType = 
    | 'APP_INSTALLED' 
    | 'APP_UNINSTALLED' 
    | 'SUBSCRIPTION_CHANGED' 
    | 'METERED_EVENT'
    | 'MERCHANT_UPDATED';

interface CloverWebhookPayload {
    merchantId: string;
    type: CloverEventType;
    timestamp?: number;
    appId?: string;
    // Subscription change fields
    oldPlan?: string;
    newPlan?: string;
    // Metered event fields
    usageCount?: number;
    usageTier?: string;
    // Generic data
    data?: Record<string, unknown>;
}

/**
 * Main Clover webhook endpoint
 * URL: https://{project}.cloudfunctions.net/cloverWebhook
 */
export const cloverWebhook = functions.https.onRequest(async (req, res) => {
    // Only accept POST requests
    if (req.method !== 'POST') {
        res.status(405).send('Method Not Allowed');
        return;
    }

    // Verify webhook signature
    const signature = req.headers['x-clover-signature'] as string | undefined;
    if (!verifySignature(req.rawBody, signature)) {
        console.error('Invalid webhook signature');
        res.status(401).send('Invalid signature');
        return;
    }

    const payload = req.body as CloverWebhookPayload;
    const { merchantId, type } = payload;

    if (!merchantId || !type) {
        console.error('Missing merchantId or type in webhook payload');
        res.status(400).send('Bad Request: Missing merchantId or type');
        return;
    }

    console.log(`Received Clover webhook: ${type} for merchant ${merchantId}`);

    try {
        switch (type) {
            case 'APP_INSTALLED':
                await handleInstall(merchantId, payload);
                break;

            case 'APP_UNINSTALLED':
                await handleUninstall(merchantId, payload);
                break;

            case 'SUBSCRIPTION_CHANGED':
                await handleSubscriptionChange(merchantId, payload);
                break;

            case 'METERED_EVENT':
                await handleMeteredEvent(merchantId, payload);
                break;

            case 'MERCHANT_UPDATED':
                await handleMerchantUpdated(merchantId, payload);
                break;

            default:
                console.log(`Unhandled webhook type: ${type}`);
        }

        res.status(200).send('OK');
    } catch (error) {
        console.error('Error processing webhook:', error);
        res.status(500).send('Internal Server Error');
    }
});

/**
 * Verify Clover webhook signature using HMAC-SHA256
 */
function verifySignature(rawBody: Buffer | undefined, signature: string | undefined): boolean {
    if (!rawBody || !signature) {
        return false;
    }

    const secret = functions.config().clover?.webhook_secret;
    if (!secret) {
        console.warn('Clover webhook secret not configured, skipping verification');
        return true; // Allow in development
    }

    const expectedSig = crypto
        .createHmac('sha256', secret)
        .update(rawBody)
        .digest('hex');

    try {
        return crypto.timingSafeEqual(
            Buffer.from(signature),
            Buffer.from(expectedSig)
        );
    } catch {
        return false;
    }
}

/**
 * Fetch merchant details from Clover API
 */
async function fetchMerchantFromClover(merchantId: string): Promise<{
    name?: string;
    owner?: { name?: string; email?: string };
}> {
    const apiToken = functions.config().clover?.api_token;
    const baseUrl = functions.config().clover?.base_url || 'https://api.clover.com';

    if (!apiToken) {
        console.warn('Clover API token not configured');
        return {};
    }

    try {
        const response = await axios.get(
            `${baseUrl}/v3/merchants/${merchantId}?expand=owner`,
            {
                headers: {
                    'Authorization': `Bearer ${apiToken}`,
                    'Content-Type': 'application/json'
                }
            }
        );

        return response.data;
    } catch (error) {
        console.error('Failed to fetch merchant from Clover:', error);
        return {};
    }
}

/**
 * Handle APP_INSTALLED webhook
 * - Fetch merchant info from Clover API
 * - Store in Firebase
 * - Send welcome email
 */
async function handleInstall(merchantId: string, payload: CloverWebhookPayload): Promise<void> {
    console.log(`Processing install for merchant ${merchantId}`);

    // Fetch merchant info from Clover API
    const merchantData = await fetchMerchantFromClover(merchantId);
    const timestamp = admin.database.ServerValue.TIMESTAMP;

    // Store merchant info
    const merchantInfo = {
        merchantId: merchantId,
        name: merchantData.owner?.name || '',
        email: merchantData.owner?.email || '',
        storeName: merchantData.name || '',
        installDate: timestamp,
        uninstallDate: null,
        lastActiveDate: timestamp
    };

    await db.ref(`merchants/${merchantId}/merchantInfo`).set(merchantInfo);

    // Initialize subscription with free plan
    await db.ref(`merchants/${merchantId}/subscription`).set({
        plan: 'free',
        status: 'active',
        monthlyDueDate: 1
    });

    // Record install event
    const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
    await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
        id: eventId,
        type: 'INSTALL',
        timestamp: timestamp,
        details: { source: 'webhook', appId: payload.appId },
        processed: false
    });

    // Send welcome email
    const email = merchantData.owner?.email;
    if (email) {
        const emailSent = await sendMailchimpEmail(email, 'welcome', {
            STORE_NAME: merchantData.name || 'Your Store',
            OWNER_NAME: merchantData.owner?.name || 'there'
        });

        if (emailSent) {
            await db.ref(`merchants/${merchantId}/events/${eventId}/processed`).set(true);
        }
    }

    console.log(`Install processed for merchant ${merchantId}`);
}

/**
 * Handle APP_UNINSTALLED webhook
 * - Update uninstall date
 * - Send farewell email
 */
async function handleUninstall(merchantId: string, payload: CloverWebhookPayload): Promise<void> {
    console.log(`Processing uninstall for merchant ${merchantId}`);

    const timestamp = admin.database.ServerValue.TIMESTAMP;

    // Update uninstall date
    await db.ref(`merchants/${merchantId}/merchantInfo/uninstallDate`).set(timestamp);

    // Update subscription status
    await db.ref(`merchants/${merchantId}/subscription/status`).set('cancelled');

    // Record uninstall event
    const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
    await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
        id: eventId,
        type: 'UNINSTALL',
        timestamp: timestamp,
        details: payload.data || {},
        processed: false
    });

    // Get merchant email for farewell email
    const merchantInfoSnapshot = await db.ref(`merchants/${merchantId}/merchantInfo`).once('value');
    const merchantInfo = merchantInfoSnapshot.val();
    const email = merchantInfo?.email;

    if (email) {
        const emailSent = await sendMailchimpEmail(email, 'farewell', {
            STORE_NAME: merchantInfo?.storeName || 'Your Store',
            OWNER_NAME: merchantInfo?.name || 'there'
        });

        if (emailSent) {
            await db.ref(`merchants/${merchantId}/events/${eventId}/processed`).set(true);
        }
    }

    console.log(`Uninstall processed for merchant ${merchantId}`);
}

/**
 * Handle SUBSCRIPTION_CHANGED webhook
 * - Update subscription plan
 * - Send upgrade/downgrade email
 */
async function handleSubscriptionChange(merchantId: string, payload: CloverWebhookPayload): Promise<void> {
    console.log(`Processing subscription change for merchant ${merchantId}`);

    const { oldPlan = 'free', newPlan = 'free' } = payload;
    const timestamp = admin.database.ServerValue.TIMESTAMP;

    // Update subscription
    await db.ref(`merchants/${merchantId}/subscription`).update({
        plan: newPlan,
        status: 'active'
    });

    // Determine if upgrade or downgrade
    const planRanking: Record<string, number> = { free: 0, basic: 1, premium: 2 };
    const isUpgrade = (planRanking[newPlan] || 0) > (planRanking[oldPlan] || 0);
    const eventType = isUpgrade ? 'SUBSCRIPTION_UPGRADE' : 'SUBSCRIPTION_DOWNGRADE';

    // Record event
    const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
    await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
        id: eventId,
        type: eventType,
        timestamp: timestamp,
        details: { oldPlan, newPlan },
        processed: false
    });

    // Get merchant email
    const merchantInfoSnapshot = await db.ref(`merchants/${merchantId}/merchantInfo`).once('value');
    const merchantInfo = merchantInfoSnapshot.val();
    const email = merchantInfo?.email;

    if (email) {
        const templateKey = isUpgrade ? 'upgrade' : 'downgrade';
        const emailSent = await sendMailchimpEmail(email, templateKey, {
            STORE_NAME: merchantInfo?.storeName || 'Your Store',
            OLD_PLAN: oldPlan.toUpperCase(),
            NEW_PLAN: newPlan.toUpperCase()
        });

        if (emailSent) {
            await db.ref(`merchants/${merchantId}/events/${eventId}/processed`).set(true);
        }
    }

    console.log(`Subscription change processed for merchant ${merchantId}: ${oldPlan} -> ${newPlan}`);
}

/**
 * Handle METERED_EVENT webhook
 * - Track usage tier breaks
 * - Send usage alert email if threshold exceeded
 */
async function handleMeteredEvent(merchantId: string, payload: CloverWebhookPayload): Promise<void> {
    console.log(`Processing metered event for merchant ${merchantId}`);

    const { usageCount, usageTier } = payload;
    const timestamp = admin.database.ServerValue.TIMESTAMP;

    // Record event
    const eventId = db.ref(`merchants/${merchantId}/events`).push().key;
    await db.ref(`merchants/${merchantId}/events/${eventId}`).set({
        id: eventId,
        type: 'USAGE_TIER_BREAK',
        timestamp: timestamp,
        details: { usageCount, usageTier, ...payload.data },
        processed: false
    });

    // Get merchant email for usage alert
    const merchantInfoSnapshot = await db.ref(`merchants/${merchantId}/merchantInfo`).once('value');
    const merchantInfo = merchantInfoSnapshot.val();
    const email = merchantInfo?.email;

    if (email) {
        const emailSent = await sendMailchimpEmail(email, 'usage_alert', {
            STORE_NAME: merchantInfo?.storeName || 'Your Store',
            USAGE_COUNT: String(usageCount || 0),
            USAGE_TIER: usageTier || 'Unknown'
        });

        if (emailSent) {
            await db.ref(`merchants/${merchantId}/events/${eventId}/processed`).set(true);
        }
    }

    console.log(`Metered event processed for merchant ${merchantId}`);
}

/**
 * Handle MERCHANT_UPDATED webhook
 * - Sync merchant info changes
 */
async function handleMerchantUpdated(merchantId: string, payload: CloverWebhookPayload): Promise<void> {
    console.log(`Processing merchant update for merchant ${merchantId}`);

    // Fetch latest merchant info from Clover
    const merchantData = await fetchMerchantFromClover(merchantId);

    // Update stored merchant info
    const updates: Record<string, unknown> = {};
    if (merchantData.name) {
        updates['storeName'] = merchantData.name;
    }
    if (merchantData.owner?.name) {
        updates['name'] = merchantData.owner.name;
    }
    if (merchantData.owner?.email) {
        updates['email'] = merchantData.owner.email;
    }

    if (Object.keys(updates).length > 0) {
        await db.ref(`merchants/${merchantId}/merchantInfo`).update(updates);
    }

    console.log(`Merchant update processed for merchant ${merchantId}`);
}
