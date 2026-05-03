/**
 * OrderMate Firebase Cloud Functions
 * 
 * Issue #98: Webhooks for user lifecycle events
 * Issue #94: Weekly cron jobs for analytics
 */

import * as admin from 'firebase-admin';

// Initialize Firebase Admin
admin.initializeApp();

// Export webhook functions
export { cloverWebhook } from './webhooks/cloverWebhook';

// Export cron functions
export { weeklyReport } from './cron/weeklyReport';
