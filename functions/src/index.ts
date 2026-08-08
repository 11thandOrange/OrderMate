/**
 * OrderMate Firebase Cloud Functions
 *
 * Issue #98: Webhooks for user lifecycle events
 */

import * as admin from "firebase-admin";

// Initialize Firebase Admin
admin.initializeApp();

// Export webhook functions
export {cloverWebhook} from "./webhooks/cloverWebhook";

// Export OAuth install callback - exchanges the code Clover sends on
// install for a merchant-specific access token (no hardcoded API keys).
export {cloverOAuthCallback} from "./oauth/cloverOAuthCallback";
