# Clover Webhook Setup Guide

Complete guide for registering and verifying the OrderMate webhook with Clover.

## Prerequisites

- [ ] Firebase project configured (`ordermate-53077`)
- [ ] Cloud Functions deployed
- [ ] Clover Developer account
- [ ] Clover app created

## Webhook URL

```
https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook
```

## Step-by-Step Setup

### Step 1: Deploy the Cloud Function

```bash
cd functions
npm install
npm run build
firebase deploy --only functions:cloverWebhook
```

Verify deployment:
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{"verificationCode": "test"}'
```

Expected response: `OK`

### Step 2: Open Clover Developer Dashboard

1. Go to [Clover Developer Dashboard](https://www.clover.com/developer-home)
   - **Production:** https://www.clover.com/developer-home
   - **Sandbox:** https://sandbox.dev.clover.com/developer-home

2. Sign in with your developer account

3. Select your app from the list

### Step 3: Configure Webhook URL

1. Click **Edit Settings** (or the settings/gear icon)

2. Navigate to **Webhooks** section

3. In the **Webhook URL** field, enter:
   ```
   https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook
   ```

4. **Important:** Make sure there are no trailing spaces or slashes

### Step 4: Verify the Webhook

1. Open a terminal and start watching logs:
   ```bash
   firebase functions:log --only cloverWebhook
   ```

2. In Clover Dashboard, click **Send verification code**

3. Watch the terminal for a log entry like:
   ```
   VERIFICATION CODE: abc123-def456-ghi789 | Request: xxxxx
   ```

4. Copy the verification code (e.g., `abc123-def456-ghi789`)

5. Paste it into the verification field in Clover

6. Click **Verify**

### Step 5: Subscribe to Events

After verification succeeds:

1. In the **Webhooks** section, find **Event Subscriptions**

2. Enable the following:
   - **Apps** - For install, uninstall, and subscription events

3. Click **Save**

### Merchant enrichment is not available for this app

The webhook alone only tells you *that* a merchant installed/uninstalled -
it does not give you a token to call Clover's REST API on that merchant's
behalf (`merchantInfo.name`/`email`/`storeName` stay blank; everything else
still works). Getting that token normally means registering a **Web** REST
client on the Developer Dashboard and using an OAuth redirect - but
OrderMate's App Type is Android-only (Flex/Mini/Station devices), and its
Developer Dashboard has no "REST Configuration"/Web client section at all
(confirmed directly in the dashboard - there is no "App URL"/"Configuration"
page to set). That flow is simply not available for this app.

If merchant name/email is ever needed, the only path is on-device: the
Android app already holds a `CloverAccount` and could call
`CloverAuth.authenticate()` locally to get its own token and write the info
to Firebase directly - a separate feature, not part of this webhook.

## Verification Flow Diagram

```
┌─────────────────┐     POST {"verificationCode": "xxx"}     ┌──────────────────┐
│  Clover         │ ──────────────────────────────────────▶  │  Your Firebase   │
│  Dashboard      │                                          │  Function        │
│                 │     ◀────────────────────────────────────│                  │
│                 │              200 OK                      │  Logs: "xxx"     │
└────────┬────────┘                                          └──────────────────┘
         │
         │ You copy code from logs
         │ and paste into Clover
         ▼
┌─────────────────┐
│  Enter code     │
│  Click Verify   │
└─────────────────┘
```

## Event Payloads

### Clover Standard Format

When events occur, Clover sends:

```json
{
  "appId": "YOUR_APP_ID",
  "merchants": {
    "MERCHANT_ID_1": [
      {
        "objectId": "A:YOUR_APP_ID",
        "type": "CREATE",
        "ts": 1699000000000
      }
    ]
  }
}
```

**Event Types:**

| objectId Prefix | type | Meaning |
|-----------------|------|---------|
| `A:` | `CREATE` | App installed |
| `A:` | `DELETE` | App uninstalled |
| `A:` | `UPDATE` | Subscription changed |

### Legacy Format (also supported)

```json
{
  "merchantId": "MERCHANT_ID",
  "type": "APP_INSTALLED"
}
```

## Troubleshooting

### "Invalid verification code" Error

**Cause:** The code you entered doesn't match what Clover expects.

**Solutions:**
1. Make sure you're copying the **most recent** code from logs
2. Don't click "Send verification code" multiple times - each click generates a new code
3. Enter the code quickly - it may expire
4. Try in an incognito/private browser window

### No Logs Appearing

**Cause:** The webhook URL may not be saved, or Clover isn't reaching your function.

**Solutions:**
1. Verify the URL is exactly correct (case-sensitive)
2. Test the endpoint directly with curl
3. Check Firebase deployment status:
   ```bash
   firebase functions:list
   ```
4. Check for function errors:
   ```bash
   firebase functions:log
   ```

### 404 Error on Verify

**Cause:** Clover's internal `/webhook/validate` endpoint is failing.

**Solutions:**
1. Clear browser cache and cookies
2. Try a different browser or incognito mode
3. Wait a few minutes and retry
4. Contact Clover support if persistent

### Multiple Verification Codes in Logs

**Cause:** Multiple requests are being sent.

**Reasons:**
- Clicked "Send verification code" multiple times
- Cloud Function auto-scaling created multiple instances
- Clover retried due to slow response

**Solution:** Always use the **latest** code from the logs.

### Function Returns Error

Check logs for specific errors:
```bash
firebase functions:log --only cloverWebhook
```

Common issues:
- Missing Firebase Admin initialization
- TypeScript compilation errors
- Missing dependencies

## Testing Webhooks Locally

### Using Firebase Emulator

```bash
cd functions
npm run serve
```

This starts a local emulator. Use ngrok to expose it:

```bash
ngrok http 5001
```

Use the ngrok URL in Clover for testing.

### Simulating Webhook Events

**Install event:**
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "TEST_APP",
    "merchants": {
      "TEST_MERCHANT": [{
        "objectId": "A:TEST_APP",
        "type": "CREATE",
        "ts": 1699000000000
      }]
    }
  }'
```

**Uninstall event:**
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "TEST_APP",
    "merchants": {
      "TEST_MERCHANT": [{
        "objectId": "A:TEST_APP",
        "type": "DELETE",
        "ts": 1699000000000
      }]
    }
  }'
```

**Subscription change:**
```bash
curl -X POST https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "TEST_APP",
    "merchants": {
      "TEST_MERCHANT": [{
        "objectId": "A:TEST_APP",
        "type": "UPDATE",
        "ts": 1699000000000
      }]
    }
  }'
```

## Verifying Data in Firebase

After webhook events are processed, check Firebase Realtime Database:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select `ordermate-53077` project
3. Navigate to **Realtime Database**
4. Look for `merchants/{merchantId}/` nodes

## Security

### Verify Webhook Origin

Clover includes an `X-Clover-Auth` header with each webhook request. `cloverWebhook.ts` verifies this matches `CLOVER_AUTH_CODE` (env var) before processing any real event - requests without a matching header get a `401`. This does **not** apply to the verification handshake (`{verificationCode: "..."}`), since that's the one-time setup step used before `CLOVER_AUTH_CODE` is even configured.

Set `CLOVER_AUTH_CODE` in `functions/.env` before deploying (Gen 2 functions
read `process.env`, not the legacy `firebase functions:config:set` store) -
see your app's Clover Developer Dashboard, Webhooks section, for the value
(it only appears after the verification handshake in Step 4 succeeds).

### No Merchant API Token in This Codebase

`CLOVER_API_TOKEN` used to be a single static value pasted into `.env`,
which only ever worked for whichever one merchant it was copied from -
every other merchant got a `401` calling Clover's REST API. It has been
removed rather than replaced: as covered in
[Merchant enrichment is not available for this app](#merchant-enrichment-is-not-available-for-this-app),
OrderMate's Android-only app registration has no way to obtain a
per-merchant (or any) Clover REST API token server-side, so this webhook no
longer attempts to call Clover's REST API at all. No token - static,
per-merchant, or otherwise - is stored or used anywhere in this codebase.

### HTTPS Only

Clover only sends webhooks to HTTPS URLs. Firebase Cloud Functions URLs are HTTPS by default.

## Support

- [Clover Developer Documentation](https://docs.clover.com/dev/docs/webhooks)
- [Clover Developer Community](https://community.clover.com/)
- [Firebase Cloud Functions Docs](https://firebase.google.com/docs/functions)
