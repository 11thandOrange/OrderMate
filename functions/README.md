# OrderMate Cloud Functions

Firebase Cloud Functions for OrderMate webhooks.

## Features

### Issue #98: Webhooks for User Lifecycle Events
- **cloverWebhook**: HTTPS endpoint that handles Clover webhook events
  - `GET ?verificationCode=xxx`: Returns verification code for Clover webhook registration
  - `APP_INSTALLED`: Stores merchant info in Firebase
  - `APP_UNINSTALLED`: Updates uninstall date
  - `SUBSCRIPTION_CHANGED`: Updates subscription plan

## Setup

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Configure Environment

Create a `.env` file in the `functions/` directory:

```bash
CLOVER_API_TOKEN=your-clover-api-token
CLOVER_BASE_URL=https://api.clover.com
```

For sandbox testing:
```bash
CLOVER_BASE_URL=https://sandbox.dev.clover.com
```

### 3. Build

```bash
npm run build
```

### 4. Deploy

```bash
firebase deploy --only functions
```

## Register Webhook with Clover

1. Go to [Clover Developer Dashboard](https://www.clover.com/developer-home)
2. Select your app → **Webhooks**
3. Add webhook URL: `https://{your-project}.cloudfunctions.net/cloverWebhook`
4. Clover will send a verification code - the function will return it automatically
5. Enable events: `APP_INSTALLED`, `APP_UNINSTALLED`, `SUBSCRIPTION_CHANGED`

## Testing

### Test Verification

```bash
curl "https://{your-project}.cloudfunctions.net/cloverWebhook?verificationCode=test123"
# Should return: test123
```

### Test Webhook Event

```bash
curl -X POST https://{your-project}.cloudfunctions.net/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "TEST123",
    "type": "APP_INSTALLED"
  }'
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `CLOVER_API_TOKEN` | Clover API access token |
| `CLOVER_BASE_URL` | Clover API base URL |

## File Structure

```
functions/
├── src/
│   ├── index.ts              # Main entry point
│   └── webhooks/
│       └── cloverWebhook.ts  # Clover webhook handler
├── .env                      # Environment variables (not in git)
├── .env.example              # Example env file
├── package.json
├── tsconfig.json
└── README.md
```
