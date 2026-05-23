# Webhooks, Cron Jobs & Database Implementation Plan

This document outlines the implementation steps for GitHub issues:
- [#94](https://github.com/11thandOrange/OrderMate/issues/94) - Weekly Cron Jobs For Webhook Events
- [#97](https://github.com/11thandOrange/OrderMate/issues/97) - Update Database Storage
- [#98](https://github.com/11thandOrange/OrderMate/issues/98) - Webhooks For User Lifecycle Events

## Status

| Issue | Component | Status |
|-------|-----------|--------|
| #98 | Clover Webhooks | ✅ Implemented (`functions/src/webhooks/cloverWebhook.ts`) |
| #97 | Database Schema | ✅ Implemented (Firebase paths defined) |
| #94 | Cron Jobs | 🔲 Not started |

## Related Documentation

- [functions/README.md](./functions/README.md) - Cloud Functions setup
- [functions/WEBHOOK_SETUP.md](./functions/WEBHOOK_SETUP.md) - Clover webhook registration guide

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Database Schema Updates](#database-schema-updates)
3. [Clover Webhook Integration](#clover-webhook-integration)
4. [Cron Job Setup](#cron-job-setup)
5. [Email Integration (Mailchimp)](#email-integration-mailchimp)
6. [Implementation Steps](#implementation-steps)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         OrderMate Backend                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌───────────────┐     ┌───────────────┐     ┌───────────────┐          │
│  │ Clover        │     │ Firebase      │     │ Cloud         │          │
│  │ Webhooks      │────▶│ Realtime DB   │◀───▶│ Functions     │          │
│  └───────────────┘     └───────────────┘     └───────────────┘          │
│         │                     │                     │                    │
│         │                     │                     │                    │
│         ▼                     ▼                     ▼                    │
│  ┌───────────────────────────────────────────────────────────┐          │
│  │                    Event Processing                        │          │
│  │  • App Install/Uninstall                                   │          │
│  │  • Subscription Changes                                    │          │
│  │  • Usage Tier Upgrades                                     │          │
│  └───────────────────────────────────────────────────────────┘          │
│                               │                                          │
│                               ▼                                          │
│  ┌───────────────────────────────────────────────────────────┐          │
│  │                    Email Services                          │          │
│  │  • Auto emails (Mailchimp) - merchant notifications        │          │
│  │  • Internal reports (weekly cron) - admin analytics        │          │
│  └───────────────────────────────────────────────────────────┘          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Database Schema Updates (Issue #97)

### Proposed Firebase Schema

Update `FirebasePaths.kt` to add merchant analytics data:

```kotlin
/**
 * Firebase Realtime Database Structure (Updated)
 * 
 * merchants/{merchantId}/
 *   ├── meta/
 *   │   ├── schemaVersion
 *   │   ├── createdAt
 *   │   └── updatedAt
 *   ├── merchantInfo/                    ← NEW: Core merchant data
 *   │   ├── merchantId                   ← Clover merchant ID
 *   │   ├── name                         ← Business owner name
 *   │   ├── email                        ← Contact email
 *   │   ├── storeName                    ← Business/store name
 *   │   ├── installDate                  ← App install timestamp
 *   │   ├── uninstallDate                ← App uninstall timestamp (null if active)
 *   │   └── lastActiveDate               ← Last app usage timestamp
 *   ├── subscription/                    ← NEW: Subscription & billing
 *   │   ├── plan                         ← Current plan (free/basic/premium)
 *   │   ├── status                       ← active/cancelled/past_due
 *   │   ├── monthlyDueDate               ← Day of month payment is due
 *   │   └── billingHistory/              ← Payment records
 *   │       └── {paymentId}/
 *   │           ├── amount
 *   │           ├── dueDate
 *   │           ├── paidDate
 *   │           ├── status               ← paid/late/missed
 *   │           └── lateDays             ← Days late (0 if on time)
 *   ├── events/                          ← NEW: Lifecycle events
 *   │   └── {eventId}/
 *   │       ├── type                     ← install/uninstall/upgrade/refund
 *   │       ├── timestamp
 *   │       ├── details                  ← JSON with event-specific data
 *   │       └── processed                ← Email sent flag
 *   ├── settings/                        ← Existing
 *   ├── widgets/{widgetId}/              ← Existing
 *   ├── templates/{templateId}/          ← Existing
 *   ├── profiles/{employeeId}/           ← Existing
 *   ├── referrals/{referralId}/          ← Existing
 *   └── discounts/{discountId}/          ← Existing
 */
```

### New Data Models

**MerchantInfo.kt:**
```kotlin
data class MerchantInfo(
    val merchantId: String = "",
    val name: String = "",              // Owner/contact name
    val email: String = "",             // Contact email
    val storeName: String = "",         // Business name
    val installDate: Long = 0,          // Timestamp
    val uninstallDate: Long? = null,    // Null if still active
    val lastActiveDate: Long = 0        // Last app usage
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "merchantId" to merchantId,
        "name" to name,
        "email" to email,
        "storeName" to storeName,
        "installDate" to installDate,
        "uninstallDate" to uninstallDate,
        "lastActiveDate" to lastActiveDate
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>?): MerchantInfo {
            if (map == null) return MerchantInfo()
            return MerchantInfo(
                merchantId = map["merchantId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                storeName = map["storeName"] as? String ?: "",
                installDate = (map["installDate"] as? Number)?.toLong() ?: 0,
                uninstallDate = (map["uninstallDate"] as? Number)?.toLong(),
                lastActiveDate = (map["lastActiveDate"] as? Number)?.toLong() ?: 0
            )
        }
    }
}
```

**SubscriptionInfo.kt:**
```kotlin
data class SubscriptionInfo(
    val plan: String = "free",           // free/basic/premium
    val status: String = "active",       // active/cancelled/past_due
    val monthlyDueDate: Int = 1          // Day of month (1-28)
)

data class BillingRecord(
    val id: String = "",
    val amount: Double = 0.0,
    val dueDate: Long = 0,
    val paidDate: Long? = null,
    val status: String = "pending",      // pending/paid/late/missed
    val lateDays: Int = 0
)
```

**MerchantEvent.kt:**
```kotlin
enum class EventType {
    INSTALL,
    UNINSTALL,
    SUBSCRIPTION_UPGRADE,
    SUBSCRIPTION_DOWNGRADE,
    PAYMENT_RECEIVED,
    PAYMENT_LATE,
    REFUND,
    DISCOUNT_APPLIED
}

data class MerchantEvent(
    val id: String = "",
    val type: EventType = EventType.INSTALL,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any?> = emptyMap(),
    val processed: Boolean = false       // Has email been sent?
)
```

---

## 3. Clover Webhook Integration (Issue #98)

### Step 1: Register Webhooks with Clover

Clover supports webhooks for app lifecycle events. Register via Clover Developer Dashboard:

1. Go to **Clover Developer Dashboard** > Your App > **Webhooks**
2. Register endpoint URL: `https://your-backend.com/api/webhooks/clover`
3. Enable these webhook events:
   - `APP_INSTALLED` - Merchant installs app
   - `APP_UNINSTALLED` - Merchant uninstalls app
   - `SUBSCRIPTION_CHANGED` - Subscription tier changes
   - `METERED_EVENT` - Usage-based billing events

### Step 2: Create Webhook Receiver (Cloud Function)

**functions/src/webhooks/cloverWebhook.ts:**
```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

const db = admin.database();

export const cloverWebhook = functions.https.onRequest(async (req, res) => {
    // Verify webhook signature (Clover uses HMAC-SHA256)
    const signature = req.headers['x-clover-signature'];
    if (!verifySignature(req.body, signature)) {
        res.status(401).send('Invalid signature');
        return;
    }
    
    const payload = req.body;
    const merchantId = payload.merchantId;
    const eventType = payload.type;
    
    try {
        switch (eventType) {
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
        }
        
        res.status(200).send('OK');
    } catch (error) {
        console.error('Webhook error:', error);
        res.status(500).send('Error processing webhook');
    }
});

async function handleInstall(merchantId: string, payload: any) {
    const merchantRef = db.ref(`merchants/${merchantId}`);
    
    // Fetch merchant info from Clover API
    const merchantInfo = await fetchMerchantFromClover(merchantId);
    
    await merchantRef.child('merchantInfo').set({
        merchantId: merchantId,
        name: merchantInfo.owner?.name || '',
        email: merchantInfo.owner?.email || '',
        storeName: merchantInfo.name || '',
        installDate: admin.database.ServerValue.TIMESTAMP,
        uninstallDate: null,
        lastActiveDate: admin.database.ServerValue.TIMESTAMP
    });
    
    // Create install event
    const eventRef = merchantRef.child('events').push();
    await eventRef.set({
        id: eventRef.key,
        type: 'INSTALL',
        timestamp: admin.database.ServerValue.TIMESTAMP,
        details: { source: 'webhook' },
        processed: false
    });
    
    // Trigger welcome email via Mailchimp
    await sendMailchimpEmail(merchantInfo.owner?.email, 'welcome');
}

async function handleUninstall(merchantId: string, payload: any) {
    const merchantRef = db.ref(`merchants/${merchantId}`);
    
    // Update uninstall date
    await merchantRef.child('merchantInfo/uninstallDate').set(
        admin.database.ServerValue.TIMESTAMP
    );
    
    // Create uninstall event
    const eventRef = merchantRef.child('events').push();
    await eventRef.set({
        id: eventRef.key,
        type: 'UNINSTALL',
        timestamp: admin.database.ServerValue.TIMESTAMP,
        details: payload,
        processed: false
    });
    
    // Get merchant email for farewell email
    const merchantInfo = await merchantRef.child('merchantInfo').once('value');
    const email = merchantInfo.val()?.email;
    
    if (email) {
        await sendMailchimpEmail(email, 'farewell');
    }
}

async function handleSubscriptionChange(merchantId: string, payload: any) {
    const merchantRef = db.ref(`merchants/${merchantId}`);
    
    // Update subscription info
    await merchantRef.child('subscription').update({
        plan: payload.newPlan,
        status: 'active'
    });
    
    // Create event
    const eventRef = merchantRef.child('events').push();
    const isUpgrade = isPlanUpgrade(payload.oldPlan, payload.newPlan);
    
    await eventRef.set({
        id: eventRef.key,
        type: isUpgrade ? 'SUBSCRIPTION_UPGRADE' : 'SUBSCRIPTION_DOWNGRADE',
        timestamp: admin.database.ServerValue.TIMESTAMP,
        details: {
            oldPlan: payload.oldPlan,
            newPlan: payload.newPlan
        },
        processed: false
    });
    
    // Send tier change email
    const merchantInfo = await merchantRef.child('merchantInfo').once('value');
    const email = merchantInfo.val()?.email;
    
    if (email) {
        await sendMailchimpEmail(email, isUpgrade ? 'upgrade' : 'downgrade');
    }
}
```

### Step 3: Verify Webhook Signature

```typescript
import * as crypto from 'crypto';

function verifySignature(payload: any, signature: string): boolean {
    const secret = functions.config().clover.webhook_secret;
    const expectedSig = crypto
        .createHmac('sha256', secret)
        .update(JSON.stringify(payload))
        .digest('hex');
    
    return crypto.timingSafeEqual(
        Buffer.from(signature),
        Buffer.from(expectedSig)
    );
}
```

### Step 4: Fetch Merchant Data from Clover API

```typescript
import axios from 'axios';

async function fetchMerchantFromClover(merchantId: string): Promise<any> {
    const apiToken = functions.config().clover.api_token;
    const baseUrl = functions.config().clover.base_url; // sandbox or production
    
    const response = await axios.get(
        `${baseUrl}/v3/merchants/${merchantId}`,
        {
            headers: {
                'Authorization': `Bearer ${apiToken}`,
                'Content-Type': 'application/json'
            }
        }
    );
    
    return response.data;
}
```

---

## 4. Cron Job Setup (Issue #94)

### Weekly Report Cron Job

Create a Cloud Function scheduled to run weekly:

**functions/src/cron/weeklyReport.ts:**
```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';

const db = admin.database();

// Run every Monday at 9:00 AM EST
export const weeklyReport = functions.pubsub
    .schedule('0 9 * * 1')
    .timeZone('America/New_York')
    .onRun(async (context) => {
        const report = await generateWeeklyReport();
        await sendReportEmail(report);
        return null;
    });

interface WeeklyReport {
    period: { start: Date; end: Date };
    installs: MerchantSummary[];
    uninstalls: MerchantSummary[];
    refunds: RefundSummary[];
    discountsUsed: DiscountSummary[];
    summary: {
        totalInstalls: number;
        totalUninstalls: number;
        netGrowth: number;
        totalRefunds: number;
        totalDiscountValue: number;
    };
}

interface MerchantSummary {
    merchantId: string;
    name: string;
    email: string;
    storeName: string;
    date: Date;
}

async function generateWeeklyReport(): Promise<WeeklyReport> {
    const now = new Date();
    const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    
    // Query all merchants
    const merchantsSnapshot = await db.ref('merchants').once('value');
    const merchants = merchantsSnapshot.val() || {};
    
    const installs: MerchantSummary[] = [];
    const uninstalls: MerchantSummary[] = [];
    const refunds: RefundSummary[] = [];
    const discountsUsed: DiscountSummary[] = [];
    
    for (const [merchantId, data] of Object.entries(merchants)) {
        const merchantData = data as any;
        const info = merchantData.merchantInfo || {};
        const events = merchantData.events || {};
        
        // Process events from the past week
        for (const [eventId, event] of Object.entries(events)) {
            const e = event as any;
            const eventDate = new Date(e.timestamp);
            
            if (eventDate < weekAgo) continue;
            
            switch (e.type) {
                case 'INSTALL':
                    installs.push({
                        merchantId,
                        name: info.name || 'Unknown',
                        email: info.email || 'N/A',
                        storeName: info.storeName || 'Unknown Store',
                        date: eventDate
                    });
                    break;
                    
                case 'UNINSTALL':
                    uninstalls.push({
                        merchantId,
                        name: info.name || 'Unknown',
                        email: info.email || 'N/A',
                        storeName: info.storeName || 'Unknown Store',
                        date: eventDate
                    });
                    break;
                    
                case 'REFUND':
                    refunds.push({
                        merchantId,
                        name: info.name || 'Unknown',
                        amount: e.details?.amount || 0,
                        date: eventDate
                    });
                    break;
                    
                case 'DISCOUNT_APPLIED':
                    discountsUsed.push({
                        merchantId,
                        name: info.name || 'Unknown',
                        discountCode: e.details?.discountCode || '',
                        amount: e.details?.amount || 0,
                        date: eventDate
                    });
                    break;
            }
        }
    }
    
    return {
        period: { start: weekAgo, end: now },
        installs,
        uninstalls,
        refunds,
        discountsUsed,
        summary: {
            totalInstalls: installs.length,
            totalUninstalls: uninstalls.length,
            netGrowth: installs.length - uninstalls.length,
            totalRefunds: refunds.reduce((sum, r) => sum + r.amount, 0),
            totalDiscountValue: discountsUsed.reduce((sum, d) => sum + d.amount, 0)
        }
    };
}

async function sendReportEmail(report: WeeklyReport) {
    const adminEmail = functions.config().admin.email;
    
    const transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: functions.config().email.user,
            pass: functions.config().email.pass
        }
    });
    
    const html = generateReportHTML(report);
    
    await transporter.sendMail({
        from: '"OrderMate Analytics" <analytics@ordermate.app>',
        to: adminEmail,
        subject: `OrderMate Weekly Report - ${formatDate(report.period.start)} to ${formatDate(report.period.end)}`,
        html: html
    });
}

function generateReportHTML(report: WeeklyReport): string {
    return `
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; }
            .header { background: #3C4B80; color: white; padding: 20px; }
            .summary { display: flex; gap: 20px; padding: 20px; }
            .stat { background: #f5f5f5; padding: 15px; border-radius: 8px; text-align: center; }
            .stat-value { font-size: 32px; font-weight: bold; color: #3C4B80; }
            .stat-label { color: #666; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
            th { background: #f5f5f5; }
            .section { margin: 30px 0; }
            .section-title { font-size: 20px; color: #3C4B80; border-bottom: 2px solid #3C4B80; padding-bottom: 10px; }
        </style>
    </head>
    <body>
        <div class="header">
            <h1>📊 OrderMate Weekly Report</h1>
            <p>${formatDate(report.period.start)} - ${formatDate(report.period.end)}</p>
        </div>
        
        <div class="summary">
            <div class="stat">
                <div class="stat-value">${report.summary.totalInstalls}</div>
                <div class="stat-label">New Installs</div>
            </div>
            <div class="stat">
                <div class="stat-value">${report.summary.totalUninstalls}</div>
                <div class="stat-label">Uninstalls</div>
            </div>
            <div class="stat">
                <div class="stat-value" style="color: ${report.summary.netGrowth >= 0 ? 'green' : 'red'}">
                    ${report.summary.netGrowth >= 0 ? '+' : ''}${report.summary.netGrowth}
                </div>
                <div class="stat-label">Net Growth</div>
            </div>
            <div class="stat">
                <div class="stat-value">$${report.summary.totalRefunds.toFixed(2)}</div>
                <div class="stat-label">Refunds</div>
            </div>
        </div>
        
        <div class="section">
            <h2 class="section-title">📥 New Installs (${report.installs.length})</h2>
            ${report.installs.length > 0 ? `
            <table>
                <tr>
                    <th>Store Name</th>
                    <th>Owner</th>
                    <th>Email</th>
                    <th>Merchant ID</th>
                    <th>Install Date</th>
                </tr>
                ${report.installs.map(i => `
                <tr>
                    <td>${i.storeName}</td>
                    <td>${i.name}</td>
                    <td>${i.email}</td>
                    <td><code>${i.merchantId}</code></td>
                    <td>${formatDate(i.date)}</td>
                </tr>
                `).join('')}
            </table>
            ` : '<p>No new installs this week.</p>'}
        </div>
        
        <div class="section">
            <h2 class="section-title">📤 Uninstalls (${report.uninstalls.length})</h2>
            ${report.uninstalls.length > 0 ? `
            <table>
                <tr>
                    <th>Store Name</th>
                    <th>Owner</th>
                    <th>Email</th>
                    <th>Merchant ID</th>
                    <th>Uninstall Date</th>
                </tr>
                ${report.uninstalls.map(u => `
                <tr>
                    <td>${u.storeName}</td>
                    <td>${u.name}</td>
                    <td>${u.email}</td>
                    <td><code>${u.merchantId}</code></td>
                    <td>${formatDate(u.date)}</td>
                </tr>
                `).join('')}
            </table>
            ` : '<p>No uninstalls this week.</p>'}
        </div>
        
        <div class="section">
            <h2 class="section-title">💰 Refunds (${report.refunds.length})</h2>
            ${report.refunds.length > 0 ? `
            <table>
                <tr>
                    <th>Merchant</th>
                    <th>Amount</th>
                    <th>Date</th>
                </tr>
                ${report.refunds.map(r => `
                <tr>
                    <td>${r.name}</td>
                    <td>$${r.amount.toFixed(2)}</td>
                    <td>${formatDate(r.date)}</td>
                </tr>
                `).join('')}
            </table>
            ` : '<p>No refunds this week.</p>'}
        </div>
        
        <div class="section">
            <h2 class="section-title">🏷️ Discounts Used (${report.discountsUsed.length})</h2>
            ${report.discountsUsed.length > 0 ? `
            <table>
                <tr>
                    <th>Merchant</th>
                    <th>Discount Code</th>
                    <th>Amount</th>
                    <th>Date</th>
                </tr>
                ${report.discountsUsed.map(d => `
                <tr>
                    <td>${d.name}</td>
                    <td><code>${d.discountCode}</code></td>
                    <td>$${d.amount.toFixed(2)}</td>
                    <td>${formatDate(d.date)}</td>
                </tr>
                `).join('')}
            </table>
            ` : '<p>No discounts used this week.</p>'}
        </div>
    </body>
    </html>
    `;
}

function formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}
```

### Firebase Query for Weekly Report Data

Direct Firebase queries you can use:

```javascript
// Get all installs in the last 7 days
const weekAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);

firebase.database()
    .ref('merchants')
    .orderByChild('merchantInfo/installDate')
    .startAt(weekAgo)
    .once('value')
    .then(snapshot => {
        // Process installs
    });

// Get all events of a specific type
firebase.database()
    .ref('merchants')
    .once('value')
    .then(snapshot => {
        const merchants = snapshot.val();
        const weeklyEvents = [];
        
        Object.entries(merchants).forEach(([merchantId, data]) => {
            const events = data.events || {};
            Object.values(events).forEach(event => {
                if (event.timestamp >= weekAgo) {
                    weeklyEvents.push({ merchantId, ...event });
                }
            });
        });
        
        return weeklyEvents;
    });
```

---

## 5. Email Integration (Mailchimp)

### Mailchimp Setup

1. **Create Mailchimp Account** and get API key
2. **Create Email Templates** for each event:
   - `welcome` - App install welcome email
   - `farewell` - App uninstall follow-up
   - `upgrade` - Subscription upgrade confirmation
   - `downgrade` - Subscription downgrade notification
   - `payment_reminder` - Upcoming payment due
   - `payment_late` - Late payment notice

### Mailchimp Integration

**functions/src/email/mailchimp.ts:**
```typescript
import * as functions from 'firebase-functions';
import * as mailchimp from '@mailchimp/mailchimp_transactional';

const client = mailchimp(functions.config().mailchimp.api_key);

interface EmailTemplate {
    templateName: string;
    subject: string;
}

const TEMPLATES: Record<string, EmailTemplate> = {
    'welcome': {
        templateName: 'ordermate-welcome',
        subject: 'Welcome to OrderMate! 🎉'
    },
    'farewell': {
        templateName: 'ordermate-farewell',
        subject: "We're sorry to see you go"
    },
    'upgrade': {
        templateName: 'ordermate-upgrade',
        subject: 'Your OrderMate plan has been upgraded! 🚀'
    },
    'downgrade': {
        templateName: 'ordermate-downgrade',
        subject: 'Your OrderMate plan has changed'
    },
    'payment_reminder': {
        templateName: 'ordermate-payment-reminder',
        subject: 'Your OrderMate payment is coming up'
    },
    'payment_late': {
        templateName: 'ordermate-payment-late',
        subject: 'Action Required: OrderMate payment overdue'
    }
};

export async function sendMailchimpEmail(
    email: string,
    templateKey: string,
    mergeVars?: Record<string, string>
) {
    const template = TEMPLATES[templateKey];
    if (!template) {
        console.error(`Unknown email template: ${templateKey}`);
        return;
    }
    
    try {
        const response = await client.messages.sendTemplate({
            template_name: template.templateName,
            template_content: [],
            message: {
                to: [{ email, type: 'to' }],
                subject: template.subject,
                from_email: 'support@ordermate.app',
                from_name: 'OrderMate',
                global_merge_vars: mergeVars 
                    ? Object.entries(mergeVars).map(([name, content]) => ({ name, content }))
                    : []
            }
        });
        
        console.log(`Email sent to ${email}:`, response);
    } catch (error) {
        console.error(`Failed to send email to ${email}:`, error);
    }
}
```

---

## 6. Implementation Steps

### Phase 1: Database Schema (Issue #97)

1. **Create new data models:**
   - [ ] `MerchantInfo.kt` - Core merchant data
   - [ ] `SubscriptionInfo.kt` - Subscription & billing
   - [ ] `BillingRecord.kt` - Payment history
   - [ ] `MerchantEvent.kt` - Lifecycle events

2. **Update FirebasePaths.kt:**
   - [ ] Add paths for `merchantInfo`, `subscription`, `events`, `billingHistory`

3. **Update FirebaseConfigManager.kt:**
   - [ ] Add CRUD methods for new data structures
   - [ ] Add methods to record events

4. **Migration:**
   - [ ] Create migration function to populate `merchantInfo` for existing merchants

### Phase 2: Webhooks (Issue #98)

1. **Backend setup:**
   - [ ] Create Firebase Cloud Functions project
   - [ ] Implement `cloverWebhook` HTTPS function
   - [ ] Implement signature verification
   - [ ] Deploy to Firebase

2. **Clover configuration:**
   - [ ] Register webhook URL in Clover Developer Dashboard
   - [ ] Enable required webhook events

3. **Email integration:**
   - [ ] Set up Mailchimp account
   - [ ] Create email templates
   - [ ] Implement `sendMailchimpEmail` function

4. **Event handlers:**
   - [ ] `handleInstall` - Store merchant info, send welcome email
   - [ ] `handleUninstall` - Update uninstall date, send farewell email
   - [ ] `handleSubscriptionChange` - Update subscription, send notification

### Phase 3: Cron Jobs (Issue #94)

1. **Weekly report function:**
   - [ ] Implement `weeklyReport` scheduled function
   - [ ] Implement `generateWeeklyReport` data aggregation
   - [ ] Implement `generateReportHTML` email template
   - [ ] Configure email settings (admin email, SMTP)

2. **Deployment:**
   - [ ] Deploy scheduled function to Firebase
   - [ ] Test with manual trigger
   - [ ] Verify email delivery

### Phase 4: Testing & Monitoring

1. **Testing:**
   - [ ] Test webhooks with Clover sandbox
   - [ ] Test email delivery
   - [ ] Test weekly report generation

2. **Monitoring:**
   - [ ] Set up Firebase Cloud Functions logs
   - [ ] Create alerts for failed webhooks
   - [ ] Monitor email delivery rates

---

## Adding Additional Lifecycle Events

To add new lifecycle events in the future:

1. **Add event type to `EventType` enum:**
   ```kotlin
   enum class EventType {
       INSTALL,
       UNINSTALL,
       // Add new type here
       NEW_EVENT_TYPE
   }
   ```

2. **Register for webhook in Clover Dashboard:**
   - Go to Clover Developer Dashboard > Your App > Webhooks
   - Enable the new webhook event type

3. **Add handler in Cloud Function:**
   ```typescript
   // In cloverWebhook.ts
   case 'NEW_CLOVER_EVENT':
       await handleNewEvent(merchantId, payload);
       break;
   
   async function handleNewEvent(merchantId: string, payload: any) {
       // 1. Update relevant Firebase data
       // 2. Create event record
       // 3. Trigger email if needed
   }
   ```

4. **Create email template in Mailchimp:**
   - Create new template in Mailchimp
   - Add template mapping to `TEMPLATES` object

5. **Update weekly report:**
   - Add new event type handling in `generateWeeklyReport()`
   - Add new section in `generateReportHTML()`

---

## Environment Configuration

Set these Firebase config values:

```bash
# Clover API
firebase functions:config:set clover.api_token="YOUR_CLOVER_API_TOKEN"
firebase functions:config:set clover.webhook_secret="YOUR_WEBHOOK_SECRET"
firebase functions:config:set clover.base_url="https://api.clover.com" # or sandbox URL

# Mailchimp
firebase functions:config:set mailchimp.api_key="YOUR_MAILCHIMP_API_KEY"

# Admin email for reports
firebase functions:config:set admin.email="your-email@example.com"

# SMTP for direct email (alternative to Mailchimp)
firebase functions:config:set email.user="smtp-user"
firebase functions:config:set email.pass="smtp-password"
```

---

## 7. Order History Tracking (Note Changes)

### Firebase Schema for Order History

Add order-level history tracking to capture note changes and other order modifications:

```
merchants/{merchantId}/
  └── orderHistory/                      ← NEW: Order change history
      └── {orderId}/
          └── changes/
              └── {changeId}/
                  ├── type              ← NOTE_CHANGED/ITEM_NOTE_CHANGED/STATUS_CHANGED/etc
                  ├── timestamp         ← When change occurred
                  ├── employeeId        ← Who made the change
                  ├── employeeName      ← Employee name for display
                  ├── description       ← Human-readable description
                  ├── oldValue          ← Previous value (if applicable)
                  └── newValue          ← New value
```

### OrderChange Data Model

**OrderChange.kt:**
```kotlin
enum class OrderChangeType {
    NOTE_CHANGED,           // Order-level note changed
    ITEM_NOTE_CHANGED,      // Line item note changed
    STATUS_CHANGED,         // Order status changed
    PAYMENT_ADDED,          // Payment received
    ITEM_ADDED,             // Line item added
    ITEM_REMOVED,           // Line item removed
    CUSTOMER_ADDED,         // Customer assigned
    SCHEDULED_TIME_CHANGED  // Pickup/delivery time changed
}

data class OrderChange(
    val id: String = "",
    val type: OrderChangeType = OrderChangeType.NOTE_CHANGED,
    val timestamp: Long = System.currentTimeMillis(),
    val employeeId: String? = null,
    val employeeName: String? = null,
    val description: String = "",
    val oldValue: String? = null,
    val newValue: String? = null,
    val itemId: String? = null,       // For item-level changes
    val itemName: String? = null      // For item-level changes
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "timestamp" to timestamp,
        "employeeId" to employeeId,
        "employeeName" to employeeName,
        "description" to description,
        "oldValue" to oldValue,
        "newValue" to newValue,
        "itemId" to itemId,
        "itemName" to itemName
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>?): OrderChange? {
            if (map == null) return null
            return OrderChange(
                id = map["id"] as? String ?: "",
                type = try { 
                    OrderChangeType.valueOf(map["type"] as? String ?: "NOTE_CHANGED") 
                } catch (e: Exception) { 
                    OrderChangeType.NOTE_CHANGED 
                },
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0,
                employeeId = map["employeeId"] as? String,
                employeeName = map["employeeName"] as? String,
                description = map["description"] as? String ?: "",
                oldValue = map["oldValue"] as? String,
                newValue = map["newValue"] as? String,
                itemId = map["itemId"] as? String,
                itemName = map["itemName"] as? String
            )
        }
    }
}
```

### FirebasePaths Update

Add to `FirebasePaths.kt`:
```kotlin
// Order History
const val ORDER_HISTORY = "orderHistory"
const val CHANGES = "changes"

fun orderHistory(merchantId: String) = "${merchant(merchantId)}/$ORDER_HISTORY"

fun orderChanges(merchantId: String, orderId: String) = 
    "${orderHistory(merchantId)}/$orderId/$CHANGES"

fun orderChange(merchantId: String, orderId: String, changeId: String) =
    "${orderChanges(merchantId, orderId)}/$changeId"
```

### FirebaseConfigManager Methods

Add to `FirebaseConfigManager.kt`:
```kotlin
// ==================== Order History ====================

/**
 * Record a change to an order
 */
fun recordOrderChange(
    merchantId: String,
    orderId: String,
    change: OrderChange,
    callback: (Boolean) -> Unit
) {
    val changeWithId = if (change.id.isEmpty()) {
        change.copy(id = java.util.UUID.randomUUID().toString())
    } else {
        change
    }
    
    db.getReference(FirebasePaths.orderChange(merchantId, orderId, changeWithId.id))
        .setValue(changeWithId.toMap())
        .addOnSuccessListener { callback(true) }
        .addOnFailureListener { callback(false) }
}

/**
 * Get all changes for an order
 */
fun getOrderChanges(
    merchantId: String,
    orderId: String,
    callback: (List<OrderChange>) -> Unit
) {
    db.getReference(FirebasePaths.orderChanges(merchantId, orderId))
        .orderByChild("timestamp")
        .get()
        .addOnSuccessListener { snapshot ->
            val changes = mutableListOf<OrderChange>()
            snapshot.children.forEach { child ->
                val map = child.value as? Map<String, Any?>
                OrderChange.fromMap(map)?.let { changes.add(it) }
            }
            callback(changes.sortedByDescending { it.timestamp })
        }
        .addOnFailureListener {
            callback(emptyList())
        }
}
```

### Integration with OrderDetailFragment

Update `OrderDetailFragment.kt` to record note changes:

```kotlin
// In the onNoteSaved callback:
override fun onNoteSaved(itemId: String?, note: String) {
    val orderId = orderArguments?.id ?: return
    val merchantId = myApp.getMerchantId() ?: return
    val employeeId = myApp.getEmployeeId()
    val employeeName = myApp.getEmployeeName()
    
    // Find old note value
    val oldNote = lineItems.find { it?.order?.item?.id == itemId }?.order?.note ?: ""
    val itemName = lineItems.find { it?.order?.item?.id == itemId }?.order?.itemName ?: "Item"
    
    // Only record if note actually changed
    if (oldNote != note) {
        val change = OrderChange(
            type = OrderChangeType.ITEM_NOTE_CHANGED,
            employeeId = employeeId,
            employeeName = employeeName,
            description = "Note changed for $itemName",
            oldValue = oldNote.ifEmpty { null },
            newValue = note.ifEmpty { null },
            itemId = itemId,
            itemName = itemName
        )
        
        FirebaseConfigManager.getInstance().recordOrderChange(
            merchantId, orderId, change
        ) { success ->
            if (success) {
                android.util.Log.d("OrderHistory", "Note change recorded")
            }
        }
    }
    
    // ... existing code to save to Clover
}
```

### For Order-Level Note Changes

```kotlin
// In OrderNoteDialogFragment or wherever order note is saved:
fun saveOrderNote(newNote: String) {
    val orderId = order?.id ?: return
    val merchantId = myApp.getMerchantId() ?: return
    val oldNote = order?.note ?: ""
    
    if (oldNote != newNote) {
        val change = OrderChange(
            type = OrderChangeType.NOTE_CHANGED,
            employeeId = myApp.getEmployeeId(),
            employeeName = myApp.getEmployeeName(),
            description = "Order note changed",
            oldValue = oldNote.ifEmpty { null },
            newValue = newNote.ifEmpty { null }
        )
        
        FirebaseConfigManager.getInstance().recordOrderChange(
            merchantId, orderId, change
        ) { /* ... */ }
    }
    
    // Save to Clover...
}
```

### Update OrderHistoryDialog to Show Note Changes

Update `OrderHistoryDialog.kt` to fetch and display Firebase history:

```kotlin
private fun loadHistoryData() {
    historyItems.clear()
    
    order?.let { o ->
        // ... existing Clover history items (created, modified, payments)
        
        // Fetch note changes from Firebase
        val orderId = o.id ?: return@let
        val merchantId = (activity?.application as? MyApp)?.getMerchantId() ?: return@let
        
        FirebaseConfigManager.getInstance().getOrderChanges(merchantId, orderId) { changes ->
            changes.forEach { change ->
                val icon = when (change.type) {
                    OrderChangeType.NOTE_CHANGED -> R.drawable.ic_note
                    OrderChangeType.ITEM_NOTE_CHANGED -> R.drawable.ic_edit
                    OrderChangeType.STATUS_CHANGED -> R.drawable.ic_status
                    else -> R.drawable.ic_history
                }
                
                historyItems.add(
                    HistoryItem(
                        title = change.description,
                        timestamp = change.timestamp,
                        iconRes = icon,
                        messageBody = buildChangeDescription(change)
                    )
                )
            }
            
            historyItems.sortByDescending { it.timestamp }
            
            activity?.runOnUiThread {
                updateHistoryUI()
                binding.historyRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }
}

private fun buildChangeDescription(change: OrderChange): String {
    return buildString {
        change.employeeName?.let { append("By $it\n") }
        
        if (change.oldValue != null && change.newValue != null) {
            append("\"${change.oldValue}\" → \"${change.newValue}\"")
        } else if (change.newValue != null) {
            append("Set to: \"${change.newValue}\"")
        } else if (change.oldValue != null) {
            append("Removed: \"${change.oldValue}\"")
        }
    }
}
```

### Example History Display

```
┌─────────────────────────────────────────────────────────────┐
│ 📋 Order History                                            │
├─────────────────────────────────────────────────────────────┤
│ 📝 Note changed for Cappuccino                              │
│    May 3, 2026 • 2:15 PM                                    │
│    By John Smith                                             │
│    "Extra hot" → "Extra hot, no foam"                       │
├─────────────────────────────────────────────────────────────┤
│ 📝 Order note changed                                       │
│    May 3, 2026 • 2:10 PM                                    │
│    By John Smith                                             │
│    Set to: "Customer will pick up at side door"             │
├─────────────────────────────────────────────────────────────┤
│ 💳 Payment received $15.50                                  │
│    May 3, 2026 • 2:05 PM                                    │
├─────────────────────────────────────────────────────────────┤
│ ➕ Order created                                            │
│    May 3, 2026 • 2:00 PM                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

| Issue | Component | Status |
|-------|-----------|--------|
| #97 | Database Schema | New models + Firebase paths |
| #98 | Webhooks | Cloud Function + Clover integration |
| #94 | Cron Jobs | Weekly scheduled report |
| - | Order History | Note change tracking |

This implementation provides:
- ✅ Complete merchant data storage (name, email, store, install date, etc.)
- ✅ Subscription and billing tracking
- ✅ Automated emails via Mailchimp on app lifecycle events
- ✅ Weekly analytics reports to admin
- ✅ Extensible architecture for future event types
- ✅ Order history tracking for note changes with old/new values
