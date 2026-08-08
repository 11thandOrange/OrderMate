# OrderMate Cloud Functions

Firebase Cloud Functions for OrderMate - Clover POS integration webhooks.

## Overview

This package contains Cloud Functions that handle Clover webhook events for merchant lifecycle tracking. When merchants install, uninstall, or change subscriptions for your Clover app, these functions automatically update Firebase Realtime Database.

## Features

### cloverWebhook

HTTPS endpoint that handles Clover webhook events. Clover sends events in one request per merchant batch (`{appId, merchants: {merchantId: [{objectId, type, ts}]}}`); `objectId`'s `A:` prefix identifies app-lifecycle events, and `type` (`CREATE`/`DELETE`/`UPDATE`) maps to install/uninstall/subscription-changed. A `{merchantId, type}` "legacy" format is also accepted for manual `curl`/Postman testing (Clover never sends this shape).

Every event, once authenticated (see [Environment Variables](#environment-variables)), does two things: it updates the merchant's current state, and it appends an entry to that merchant's event log (`merchants/{merchantId}/events/{eventId}`) for the analytics/history that were never fully built out (see [WEBHOOK_CRON_IMPLEMENTATION.md](../WEBHOOK_CRON_IMPLEMENTATION.md) - the weekly-report/email pieces described there were never implemented; events accumulate but nothing currently consumes them).

| Event | How it's triggered | What it updates | Event log `type` |
|-------|---------------------|------------------|-------------------|
| **Verification** | Clicking "Send verification code" in Clover's dashboard | Nothing - the code is only logged (`firebase functions:log`) for you to copy back into Clover | - (not logged as an event) |
| **APP_INSTALLED** | Merchant installs the app (`A:` object, `CREATE`) | `merchants/{id}/merchantInfo` (name/email/storeName always blank - see note below), `merchants/{id}/subscription` initialized to `{plan: "free", status: "active"}` | `INSTALL` |
| **APP_UNINSTALLED** | Merchant uninstalls the app (`A:` object, `DELETE`) | `merchants/{id}/merchantInfo/uninstallDate`, `merchants/{id}/subscription/status` set to `"cancelled"` | `UNINSTALL` |
| **SUBSCRIPTION_CHANGED** | Merchant's plan changes on Clover's side (`A:` object, `UPDATE`) | `merchants/{id}/subscription/plan` | `SUBSCRIPTION_UPGRADE` or `SUBSCRIPTION_DOWNGRADE`, based on a fixed free < basic < premium ranking |

Every event log write uses a deterministic key (`{merchantId}_{type}_{ts}`) instead of a random push key, so a Clover retry of the same delivery overwrites the same record rather than creating a duplicate.

Only the "Apps" webhook category (install/uninstall/subscription-changed) is implemented. Orders, Payments, Refunds, Customers, and Inventory categories are not subscribed to or handled - `handleCloverWebhookEvent()` logs and drops any event whose `objectId` prefix isn't `A:`.

### Why `merchantInfo.name`/`email`/`storeName` are always blank

Fetching those from Clover's REST API (`v3/merchants/{id}?expand=owner`) requires a Clover REST API bearer token scoped to that merchant. Getting one normally means registering a "Web" REST client on the Developer Dashboard and using an OAuth authorization-code redirect - but **OrderMate has no such client registered**; its App Type is Android-only (Flex/Mini/Station devices), and the Developer Dashboard for this app has no "REST Configuration"/Web client section at all. That flow simply isn't available to this app, so this server-side function has no way to get a merchant token, and never will unless a "Web" REST client is added to the app's listing.

The only Clover-provided mechanism this app *does* have is on-device: the Android app itself already holds a `CloverAccount` (see `OrderAppApplication.kt`) and could call `CloverAuth.authenticate()` locally to get its own REST token and write real merchant info to Firebase directly from the device - a separate, not-yet-built feature, unrelated to this Cloud Function.

## Quick Start

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Configure Environment

Copy `.env.example` to `.env` and fill in `CLOVER_AUTH_CODE` (from the
Developer Dashboard's Webhooks page, after completing the verification
handshake - see [Register Webhook with Clover](#register-webhook-with-clover)
below). These are Gen 2 functions - they read `process.env` directly from
`.env`, not the legacy `firebase functions:config:set` store.

```bash
cp .env.example .env
```

### 3. Build & Deploy

```bash
npm run build
firebase deploy --only functions:cloverWebhook
```

## Webhook URL

**Production:**
```
https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook
```

## Register Webhook with Clover

See [WEBHOOK_SETUP.md](./WEBHOOK_SETUP.md) for detailed instructions.

**Quick steps:**
1. Go to [Clover Developer Dashboard](https://www.clover.com/developer-home)
2. Select your app → **Edit Settings** → **Webhooks**
3. Enter webhook URL
4. Click **Send verification code**
5. Check Firebase logs: `firebase functions:log --only cloverWebhook`
6. Copy the verification code and paste into Clover
7. Click **Verify**
8. Subscribe to events: Apps (install/uninstall/subscription)

## Testing

### Automated tests

```bash
npm test
```

Runs the Jest suite in `src/webhooks/cloverWebhook.test.ts` against a mocked `firebase-admin` (no real Firebase project needed). Covers: the verification handshake, both payload formats, auth rejection (missing/wrong header, unconfigured `CLOVER_AUTH_CODE`), the idempotent event-key behavior on a simulated Clover retry, and that a batch with multiple merchants keeps processing the healthy ones when one fails - while still reporting the failure (`500`, not `200`) so Clover retries the delivery instead of the failed write being silently dropped.

### Test with Postman or cURL

Every request except the verification handshake requires an `x-clover-auth` header whose value **exactly** matches `CLOVER_AUTH_CODE` in the *deployed* function's `.env` (see [Environment Variables](#environment-variables)) - requests without a matching header get a `401`.

**Postman setup, in order:**
1. Method: `POST`
2. URL: `https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook`
3. Headers tab: `Content-Type: application/json` and `x-clover-auth: <value from Clover's Webhooks page, same string as CLOVER_AUTH_CODE>`
4. Body tab: select **raw** + **JSON**, paste one of the bodies below

**1. Verification handshake** (no `x-clover-auth` header needed - this is the one exception):
```json
{"verificationCode": "test-123"}
```
Expected: `200 OK`.

**2. APP_INSTALLED:**
```json
{
  "appId": "TEST_APP",
  "merchants": {
    "TEST_MERCHANT_123": [{"objectId": "A:TEST_APP", "type": "CREATE", "ts": 1699000000000}]
  }
}
```

**3. APP_UNINSTALLED:**
```json
{
  "appId": "TEST_APP",
  "merchants": {
    "TEST_MERCHANT_123": [{"objectId": "A:TEST_APP", "type": "DELETE", "ts": 1699000000000}]
  }
}
```

**4. SUBSCRIPTION_CHANGED:**
```json
{
  "appId": "TEST_APP",
  "merchants": {
    "TEST_MERCHANT_123": [{"objectId": "A:TEST_APP", "type": "UPDATE", "ts": 1699000000000}]
  }
}
```

Expected for 2-4: `200 OK`, and a new/updated node under `merchants/TEST_MERCHANT_123/` in the Realtime Database (see [Firebase Database Structure](#firebase-database-structure)).

**If you're getting `401 Unauthorized` on 2-4, check in this order:**
1. Did you redeploy *after* setting `CLOVER_AUTH_CODE` in `functions/.env`? Setting the file alone does nothing until `firebase deploy --only functions:cloverWebhook` runs again.
2. Does the header value match **exactly** what's shown on Clover's Webhooks page right now? Values there change if "Send Verification Code" gets clicked again - always copy the *current* one, not a code from an earlier attempt.
3. Is there a trailing space or newline in either the header value or `.env`? Terminal/heredoc copy-paste is the usual source.
4. Confirm what's actually deployed: `firebase functions:log --only cloverWebhook` should show `CLOVER_AUTH_CODE not configured - rejecting webhook event` if the env var is missing entirely (as opposed to just mismatched), which tells you it's a deploy/config problem rather than a typo.

**If you're getting `400 Bad Request`:** the body doesn't match either supported shape (Clover-standard `{appId, merchants}` or legacy `{merchantId, type}`) - check for a JSON syntax error or a typo'd field name.

**Check logs after every attempt:**
```bash
firebase functions:log --only cloverWebhook
```

## Firebase Database Structure

When webhooks are processed, data is stored at:

```
merchants/{merchantId}/
├── merchantInfo/
│   ├── merchantId: string
│   ├── name: string          # Owner's name
│   ├── email: string         # Owner's email
│   ├── storeName: string     # Business name
│   ├── installDate: timestamp
│   ├── uninstallDate: timestamp | null
│   └── lastActiveDate: timestamp
├── subscription/
│   ├── plan: "free" | "basic" | "premium"
│   ├── status: "active" | "cancelled"
│   └── monthlyDueDate: number
└── events/{eventId}/          # eventId = "{merchantId}_{type}_{ts}", deterministic
    ├── id: string             # so Clover retries overwrite, not duplicate
    ├── type: "INSTALL" | "UNINSTALL" | "SUBSCRIPTION_UPGRADE" | "SUBSCRIPTION_DOWNGRADE"
    ├── timestamp: timestamp
    ├── details: object
    └── processed: boolean     # always false today - nothing sets this true (see below)
```

`processed` was intended to gate a follow-up email/notification step (per [WEBHOOK_CRON_IMPLEMENTATION.md](../WEBHOOK_CRON_IMPLEMENTATION.md)), but that step was never built - no cron job or email integration exists in this repo, so events accumulate in Firebase and are never read back by anything.

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CLOVER_AUTH_CODE` | Required. Must match the `X-Clover-Auth` header Clover sends with every webhook event. Without this set, all real events (everything except the verification handshake) are rejected with 401. | - |

There is intentionally no `CLOVER_API_TOKEN` / `CLOVER_CLIENT_ID` / `CLOVER_CLIENT_SECRET` - see [Why merchantInfo fields are always blank](#why-merchantinfonameemailstorename-are-always-blank) above for why none of them can work for this app.

## File Structure

```
functions/
├── src/
│   ├── index.ts                      # Main entry, exports functions
│   └── webhooks/
│       ├── cloverWebhook.ts          # Clover webhook handler
│       └── cloverWebhook.test.ts     # Jest tests (npm test)
├── jest.config.js
├── package.json
├── tsconfig.json
├── README.md                    # This file
└── WEBHOOK_SETUP.md             # Detailed setup guide
```

## Troubleshooting

See [WEBHOOK_SETUP.md](./WEBHOOK_SETUP.md#troubleshooting) for common issues.

## Related

- [Clover Webhooks Documentation](https://docs.clover.com/dev/docs/webhooks)
- [Firebase Cloud Functions](https://firebase.google.com/docs/functions)
- [Issue #98](https://github.com/11thandOrange/OrderMate/issues/98) - Create webhooks for user lifecycle events
