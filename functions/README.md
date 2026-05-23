# OrderMate Cloud Functions

Firebase Cloud Functions for OrderMate - Clover POS integration webhooks.

## Overview

This package contains Cloud Functions that handle Clover webhook events for merchant lifecycle tracking. When merchants install, uninstall, or change subscriptions for your Clover app, these functions automatically update Firebase Realtime Database.

## Features

### cloverWebhook

HTTPS endpoint that handles Clover webhook events:

| Event | Description |
|-------|-------------|
| **Verification** | Responds to Clover's webhook verification requests |
| **APP_INSTALLED** | Stores merchant info, initializes subscription |
| **APP_UNINSTALLED** | Records uninstall date, cancels subscription |
| **SUBSCRIPTION_CHANGED** | Updates subscription plan, records upgrade/downgrade |

## Quick Start

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Configure Environment

Set Firebase environment variables:

```bash
firebase functions:config:set clover.api_token="your-clover-api-token"
firebase functions:config:set clover.base_url="https://api.clover.com"
```

For local development, create `.runtimeconfig.json`:

```json
{
  "clover": {
    "api_token": "your-clover-api-token",
    "base_url": "https://api.clover.com"
  }
}
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

### Test with Postman or cURL

**Verification test:**
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{"verificationCode": "test-123"}'
# Expected response: OK
```

**APP_INSTALLED test:**
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "TEST_APP",
    "merchants": {
      "TEST_MERCHANT_123": [{
        "objectId": "A:TEST_APP",
        "type": "CREATE",
        "ts": 1699000000000
      }]
    }
  }'
```

**Check logs:**
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
└── events/{eventId}/
    ├── id: string
    ├── type: "INSTALL" | "UNINSTALL" | "SUBSCRIPTION_UPGRADE" | "SUBSCRIPTION_DOWNGRADE"
    ├── timestamp: timestamp
    ├── details: object
    └── processed: boolean
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CLOVER_API_TOKEN` | Clover API access token for fetching merchant details | - |
| `CLOVER_BASE_URL` | Clover API base URL | `https://api.clover.com` |

**Sandbox:** Use `https://sandbox.dev.clover.com` for testing.

## File Structure

```
functions/
├── src/
│   ├── index.ts                 # Main entry, exports functions
│   └── webhooks/
│       └── cloverWebhook.ts     # Clover webhook handler
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
