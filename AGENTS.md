# OrderMate Agent Guide

## Project Overview
OrderMate is an Android application built with Kotlin that integrates with Clover POS systems. It provides order management, calendar scheduling, customizable notification features, and merchant analytics for merchants.

## Agent System Architecture

```
ordermate-agent (Orchestrator)
├── code-auditor      - Bug/tech debt discovery and code quality analysis
├── ticket-manager    - GitHub Issues operations (create, investigate, plan, resolve)
├── tester            - Unit, integration, and e2e testing
├── build-release     - APK builds, version management, releases
├── postman-manager   - API testing with Postman collections
└── pr-reviewer       - Pull request reviews and GitHub interactions
```

### Critical Safety Rules

**All agents must follow these rules:**

1. **NEVER merge branches or push to main without explicit user confirmation**
2. **NEVER approve PRs without explicit user confirmation**
3. **NEVER create release tags without explicit user confirmation**
4. **NEVER delete branches or force push without explicit user confirmation**

### Agent Files Location

All agent definitions are stored in `.agents/agents/`:
- `ordermate-agent.md` - Main orchestrator
- `code-auditor.md` - Code quality & bug detection
- `ticket-manager.md` - GitHub Issues management
- `tester.md` - Testing specialist
- `build-release.md` - Build & release management
- `postman-manager.md` - API testing with Postman
- `pr-reviewer.md` - PR review & GitHub interactions

### Common Agent Commands

| Command | Agent |
|---------|-------|
| "Review the repo for bugs and tech debt" | code-auditor |
| "Create an issue for [problem]" | ticket-manager |
| "Investigate issue #123" | ticket-manager |
| "Run the unit tests" | tester |
| "Build a debug APK" | build-release |
| "Review PR #42" | pr-reviewer |
| "Create Postman collections" | postman-manager |

## Tech Stack
- **Language**: Kotlin (Android), TypeScript (Cloud Functions)
- **Platform**: Android (targets Clover devices)
- **Build System**: Gradle (Kotlin DSL)
- **Backend**: Firebase Realtime Database
- **Cloud Functions**: Firebase Functions (Node.js/TypeScript)
- **POS Integration**: Clover SDK v3

## Project Structure

### Android App
```
app/src/main/java/com/orderMate/
├── activities/       # MainActivity, OverlayActivity (register overlay)
├── adapters/         # RecyclerView adapters for lists
├── broadcast/        # Broadcast receivers
├── communicators/    # Interface definitions for component communication
├── fragment/         # UI fragments organized by feature
│   ├── orderDetail/  # Order detail screens and dialogs
│   ├── orderHistory/ # Order history view
│   └── customFields/ # Custom fields configuration
├── modals/           # Data models (WidgetConfig, PopupSettings, etc.)
├── model/            # Additional models (ScheduledEvent)
├── networkManager/   # API and Retrofit setup
├── repository/       # CloverRepository for Clover API
├── services/         # Background services
├── utils/            # Utility classes and managers
└── viewmodel/        # ViewModels (SharedFilterViewModel)
```

### Cloud Functions
```
functions/
├── src/
│   ├── index.ts                 # Main entry point
│   └── webhooks/
│       └── cloverWebhook.ts     # Clover webhook handler
├── package.json
├── tsconfig.json
├── README.md
└── WEBHOOK_SETUP.md
```

## Key Files

### Android
- `CommonFunctions.kt` - Utility functions including `getThePaymentState()`
- `ProfileSettingsFragment.kt` - Theme color picker and avatar management
- `CalendarFragment.kt` - Calendar view with day/week/month modes
- `OrderListRedesignFragment.kt` - Redesigned order list view
- `OrderCardRedesignAdapter.kt` - Order card UI with pill badges
- `SettingsFragment.kt` - App settings with multiple tabs
- `OrderDetailFragment.kt` - Order detail page with customer/item/history cards
- `WidgetManager.kt` - Widget configuration management (V2 schema)
- `FirebaseConfigManager.kt` - Firebase configuration persistence

### Cloud Functions
- `cloverWebhook.ts` - Handles Clover webhook events (install, uninstall, subscription)

## Widget System (V2)
The app uses a V2 widget schema defined in:
- `WidgetConfig.kt` - Widget type, label, options, colors
- `PopupSettings.kt` - Order/item-level popup configurations
- `DefaultWidgetFactory.kt` - Creates default widget sets

## Color System
Colors are defined in `res/values/colors.xml` with categories:
- Glass morphism UI colors (`glass_background`, `card_background`)
- Tag/pill colors (`tag_category_bg`, `tag_pill_bg`)
- Calendar event colors (`event_pickup_bg`, `event_delivery_bg`)
- Status colors (`status_open_bg`, `status_paid_bg`)

## Firebase Database Structure
```
merchants/{merchantId}/
├── merchantInfo/          # Name, email, install date, etc.
├── subscription/          # Plan, status, billing
├── events/                # Lifecycle events (install, uninstall, etc.)
├── settings/              # App settings
├── widgets/               # Widget configurations
├── templates/             # Notification templates
├── profiles/              # Employee profiles
├── referrals/             # Referral data
└── discounts/             # Discount configurations
```

## Build Commands

### Android (via Android Studio)
- **Debug APK:** Build → Build Bundle(s) / APK(s) → Build APK(s)
- **Release APK:** Build → Generate Signed Bundle / APK
- **Run Tests:** Run → Run 'All Tests'

Or if `gradlew` wrapper exists:
```bash
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew test               # Run unit tests
```

### Cloud Functions
```bash
cd functions
npm install                  # Install dependencies
npm run build                # Build TypeScript
firebase deploy --only functions:cloverWebhook  # Deploy
firebase functions:log --only cloverWebhook     # View logs
```

## Testing
- Android unit tests: `app/src/test/java/com/orderMate/`
- Cloud Functions: Test with Postman or cURL (see `functions/WEBHOOK_SETUP.md`)

## Branches
- `main` - Production branch
- `complete_redesign_logic` - V2 redesign with iOS-style UI
- `complete_v2_redesign_2` - Working branch for ticket implementation
- `#98/implement-webhooks` - Webhook implementation branch

## GitHub Issues Structure
Parent tickets organize work into 7 areas:
1. #7: General/Global UI Refinements
2. #11: Main Header Refinements
3. #16: List Page UI Refinements  
4. #20: Calendar Page Refinements & Bug Fixes
5. #31: Settings Page Refinements
6. #43: Order Details Page Refinements
7. #69: Stretch Goals / Future Enhancements

## Clover Tag Mapping
| Clover API Value | Human Readable |
|------------------|----------------|
| OPEN | Open |
| PAID | Paid |
| PARTIALLY_PAID | Partially Paid |
| PARTIALLY_REFUNDED | Partially Refunded |
| REFUNDED | Refunded |
| LOCKED | Closed |

## Clover Webhook Events
| Event | objectId Prefix | type | Action |
|-------|-----------------|------|--------|
| App Installed | `A:` | `CREATE` | Store merchant info, init subscription |
| App Uninstalled | `A:` | `DELETE` | Set uninstall date, cancel subscription |
| Subscription Changed | `A:` | `UPDATE` | Update plan, record event |

## Environment Variables (Cloud Functions)
| Variable | Description |
|----------|-------------|
| `CLOVER_API_TOKEN` | Clover API access token |
| `CLOVER_BASE_URL` | `https://api.clover.com` (prod) or `https://sandbox.dev.clover.com` |

## Webhook URL
```
https://us-central1-ordermate-53077.cloudfunctions.net/cloverWebhook
```

## Common Tasks

### Add a new webhook event handler
1. Edit `functions/src/webhooks/cloverWebhook.ts`
2. Add handler function
3. Update `handleCloverWebhookEvent()` switch statement
4. Deploy: `firebase deploy --only functions:cloverWebhook`

### Debug webhook issues
1. Check logs: `firebase functions:log --only cloverWebhook`
2. Test with Postman: POST to webhook URL with JSON body
3. Verify Clover dashboard webhook configuration
