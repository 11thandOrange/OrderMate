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
