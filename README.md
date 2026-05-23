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

See [functions/README.md](./functions/README.md) for setup instructions.

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
