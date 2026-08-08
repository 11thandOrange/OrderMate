# OrderMate

Order management application for Clover POS systems. Provides enhanced order tracking, calendar scheduling, customizable notifications, and merchant analytics.

## Features

- **Order Management** - View, filter, and manage orders from Clover POS
- **Calendar Scheduling** - Day/week/month views with order events
- **Custom Notifications** - Send SMS/email notifications to customers
- **Widget System** - Customizable order/item popup widgets
- **Merchant Analytics** - Track installs, subscriptions, and usage via webhooks

## Tech Stack

| Component | Technology |
|-----------|------------|
| Platform | Android (Kotlin) |
| Build System | Gradle (Kotlin DSL) |
| POS Integration | Clover SDK v3 |
| Backend | Firebase Realtime Database |
| Cloud Functions | Firebase Functions (TypeScript) |

## Project Structure

```
OrderMate/
├── app/                          # Android application
│   └── src/main/java/com/orderMate/
│       ├── activities/           # Main activities
│       ├── adapters/             # RecyclerView adapters
│       ├── fragment/             # UI fragments
│       │   ├── orderDetail/      # Order detail screens
│       │   ├── orderHistory/     # Order history view
│       │   └── customFields/     # Custom fields config
│       ├── modals/               # Data models
│       ├── repository/           # Clover API repository
│       ├── services/             # Background services
│       ├── utils/                # Utilities and managers
│       └── viewmodel/            # ViewModels
├── functions/                    # Firebase Cloud Functions
│   └── src/
│       └── webhooks/             # Clover webhook handlers
├── Cert/                         # Signing certificates
└── docs/                         # Documentation
```

## Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Clover Developer account
- Firebase project

### Build & Run

1. **Open in Android Studio**
   - File → Open → Select `OrderMate` folder
   - Wait for Gradle sync to complete

2. **Build Debug APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or use terminal: `./gradlew assembleDebug` (if gradlew exists)

3. **Build Release APK**
   - Build → Generate Signed Bundle / APK
   - Select APK → Choose keystore from `Cert/` folder

4. **Run on Device**
   - Connect Clover device via USB
   - Run → Run 'app'

### Connect to Clover

1. Go to [Clover Developer Dashboard](https://www.clover.com/developer-home)
2. Create or select your app
3. Use **Preview in App Market** → **Connect the App**
4. Data will sync from Clover to the app

## Permission Guardrails

OrderMate implements role-based permission controls for Settings access.

### How It Works

| Check Point | Trigger | Action |
|-------------|---------|--------|
| **MainActivity.onResume()** | App launch, background→foreground, screen unlock | Hide/show Settings nav icon based on permissions |
| **SettingsFragment.onViewCreated()** | Entering Settings page | Redirect non-permitted users to Order List |
| **Advanced Tab** | Viewing Advanced settings | Hide permission settings card for non-owners |

### Permission Flow

```
App Launch / Resume / Screen Unlock
         │
         ▼
checkSettingsNavVisibility()
         │
         ▼
EmployeeRoleUtils.canAccessSettings(employee, advancedSettings)
         │
         ├─ Owner → Always has access
         ├─ Admin → Check advancedSettings.allowAdminUpdateSettings
         ├─ Manager → Check advancedSettings.allowManagersUpdateSettings
         └─ Employee → Check advancedSettings.allowEmployeesUpdateSettings
         │
         ▼
Settings nav icon visible/hidden accordingly
```

### Covered Scenarios

- ✅ App launch with different employee roles
- ✅ Lock screen → different user logs in (Clover device)
- ✅ App backgrounded → resumed
- ✅ Direct navigation attempts to Settings (failsafe redirect)
- ✅ Non-owners viewing Advanced tab (permission card hidden)

### Known Limitation

If an Owner revokes permissions while an employee is actively using the app (without backgrounding), the nav visibility updates only when:
- App is backgrounded and resumed
- Device screen is locked/unlocked
- App is restarted

Switching between pages within the app (e.g., Calendar → Order List) does **not** trigger a permission re-check since all pages are fragments within the same Activity.

## Cloud Functions

The `functions/` directory contains Firebase Cloud Functions for:

- **Clover Webhooks** - Track app installs, uninstalls, subscription changes
- **Merchant Analytics** - Store merchant lifecycle data in Firebase

See [functions/README.md](./functions/README.md) for setup instructions, the full list of tracked webhook events, where each one is stored, and how to test them.

## Referrals

Owners can submit partner-referral records from Profile Settings (repeatedly - the button no longer hides after the first submission). Each referral is stored twice:

- `merchants/{merchantId}/referrals/{referralId}` - all referrals made by one merchant
- `referralPartners/{partnerKey}/{referralId}` - a denormalized top-level index (`partnerKey` = the partner name, lowercased/trimmed/with `.#$[]/` stripped) so referrals for one partner can be looked up across every merchant without scanning the whole database

Both are written atomically by `FirebaseConfigManager.saveReferral()`. Query methods: `getReferrals(merchantId)` (per-merchant) and `getReferralsForPartner(partnerName)` (per-partner, across merchants). There is currently no payout/commission field on a referral record - the data model only tracks who referred whom and when, not how much is owed to a partner.

## Discounts

`MerchantDiscount` (`merchants/{merchantId}/discounts/{discountId}`) is a **tracking-only record** of which merchants have been given a discount - it does not itself cause any discount to be applied. Discounts are never written by the app (read-only in-app, admin writes via Firebase Console/Postman).

The actual discounting happens entirely on Clover's side, outside this repo:
1. Create a new, separately-priced subscription tier for OrderMate in the Clover Developer Dashboard.
2. Share that tier's install link with the specific merchant, so they install under the discounted tier.
3. Disable that tier afterward so no one else can install under it. Clover pricing tiers can't be deleted, only disabled - a disabled tier still applies to merchants already on it, it just stops being offered to new installs.

The Firebase `MerchantDiscount` record is written separately (manually) as the internal paper trail for "which merchant got which discount," matching what was actually done in Clover - it is not read by any billing calculation in this codebase.

## Documentation

| Document | Description |
|----------|-------------|
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | V2 redesign implementation plan with 57 sub-tickets |
| [WEBHOOK_CRON_IMPLEMENTATION.md](./WEBHOOK_CRON_IMPLEMENTATION.md) | Webhooks, cron jobs, and database schema |
| [functions/README.md](./functions/README.md) | Cloud Functions setup and usage |
| [functions/WEBHOOK_SETUP.md](./functions/WEBHOOK_SETUP.md) | Clover webhook registration guide |

## APK Signing (Clover App Market)

To upload to Clover App Market, sign with V1 signature:

```bash
# Path to apksigner (adjust for your SDK version)
APKSIGNER="$ANDROID_HOME/build-tools/31.0.0/apksigner"

# Sign the APK
$APKSIGNER sign \
  --ks ./Cert \
  --v1-signing-enabled=true \
  --v2-signing-enabled=false \
  --v3-signing-enabled=false \
  --v1-signer-name Cert \
  ./app/release/app-release.apk
```

## Branches

| Branch | Description |
|--------|-------------|
| `main` | Production branch |
| `complete_v2_redesign_2` | V2 redesign working branch |
| `#98/implement-webhooks` | Webhook implementation |

## Contributing

1. Create a feature branch from `complete_v2_redesign_2`
2. Follow the [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) for ticket structure
3. Submit PR with descriptive title and linked issue

## License

Proprietary - 11th and Orange

## Support

- [Clover Developer Documentation](https://docs.clover.com/)
- [Firebase Documentation](https://firebase.google.com/docs) 
