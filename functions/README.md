# OrderMate Cloud Functions

Firebase Cloud Functions for OrderMate webhooks and scheduled tasks.

## Features

### Issue #98: Webhooks for User Lifecycle Events
- **cloverWebhook**: HTTPS endpoint that handles Clover webhook events
  - `APP_INSTALLED`: Stores merchant info, sends welcome email
  - `APP_UNINSTALLED`: Updates uninstall date, sends farewell email  
  - `SUBSCRIPTION_CHANGED`: Updates subscription, sends upgrade/downgrade email
  - `METERED_EVENT`: Tracks usage tier breaks, sends usage alerts
  - `MERCHANT_UPDATED`: Syncs merchant info changes

### Issue #94: Weekly Cron Jobs
- **weeklyReport**: Runs every Monday at 9:00 AM EST
  - Aggregates install/uninstall data for the past week
  - Includes refunds and discount usage
  - Sends formatted HTML email to admin

## Setup

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Configure Firebase

```bash
# Set Clover API credentials
firebase functions:config:set clover.api_token="YOUR_CLOVER_API_TOKEN"
firebase functions:config:set clover.webhook_secret="YOUR_WEBHOOK_SECRET"
firebase functions:config:set clover.base_url="https://api.clover.com"

# Set Mailchimp API key
firebase functions:config:set mailchimp.api_key="YOUR_MAILCHIMP_API_KEY"

# Set admin email for weekly reports
firebase functions:config:set admin.email="your-email@example.com"
```

### 3. Build

```bash
npm run build
```

### 4. Deploy

```bash
npm run deploy
# or
firebase deploy --only functions
```

## Register Webhook with Clover

1. Go to [Clover Developer Dashboard](https://www.clover.com/developer-home)
2. Select your app
3. Navigate to **Webhooks**
4. Add webhook URL: `https://{your-project}.cloudfunctions.net/cloverWebhook`
5. Enable these events:
   - `APP_INSTALLED`
   - `APP_UNINSTALLED`
   - `SUBSCRIPTION_CHANGED`
   - `METERED_EVENT`

## Create Mailchimp Templates

Create these templates in Mailchimp:

| Template Name | Purpose |
|--------------|---------|
| `ordermate-welcome` | Welcome email on app install |
| `ordermate-farewell` | Goodbye email on app uninstall |
| `ordermate-upgrade` | Subscription upgrade notification |
| `ordermate-downgrade` | Subscription downgrade notification |
| `ordermate-usage-alert` | Usage tier break warning |

### Merge Variables

Templates can use these merge variables:
- `*|STORE_NAME|*` - Merchant's store name
- `*|OWNER_NAME|*` - Merchant owner's name
- `*|OLD_PLAN|*` - Previous subscription plan
- `*|NEW_PLAN|*` - New subscription plan
- `*|USAGE_COUNT|*` - Current usage count
- `*|USAGE_TIER|*` - Current usage tier

## Local Development

```bash
# Start emulators
npm run serve

# View logs
npm run logs
```

## Testing

### Test Webhook Locally

```bash
# Start emulator
firebase emulators:start --only functions

# Send test webhook
curl -X POST http://localhost:5001/{project}/us-central1/cloverWebhook \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "TEST123",
    "type": "APP_INSTALLED"
  }'
```

### Trigger Weekly Report Manually

```bash
firebase functions:call weeklyReport
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `clover.api_token` | Clover API access token |
| `clover.webhook_secret` | Clover webhook signing secret |
| `clover.base_url` | Clover API base URL |
| `mailchimp.api_key` | Mailchimp Transactional API key |
| `admin.email` | Email address for weekly reports |

## File Structure

```
functions/
├── src/
│   ├── index.ts           # Main entry point
│   ├── webhooks/
│   │   └── cloverWebhook.ts   # Clover webhook handler
│   ├── cron/
│   │   └── weeklyReport.ts    # Weekly report generator
│   └── email/
│       └── mailchimp.ts       # Email service
├── package.json
├── tsconfig.json
└── README.md
```
