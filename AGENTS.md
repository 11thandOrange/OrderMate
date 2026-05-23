# OrderMate Agent Guide

## Project Overview
OrderMate is an Android application built with Kotlin that integrates with Clover POS systems. It provides order management, calendar scheduling, and customizable notification features for merchants.

## Agent System Architecture

```
ordermate-agent (Orchestrator)
├── code-auditor      - Bug/tech debt discovery and code quality analysis
├── ticket-manager    - GitHub Issues operations (create, investigate, plan, resolve)
├── tester            - Unit, integration, and e2e testing
├── build-release     - APK builds, version management, releases
├── postman-manager   - API testing with Postman collections
└── pr-reviewer       - Pull request reviews and GitHub interactions

docs-agent (Documentation Orchestrator)
├── docs-writer       - Writes and updates documentation content
├── api-spec-generator - Extracts API specs from Kotlin code
├── changelog-agent   - Generates changelogs from git commits
└── site-deployer     - Builds and deploys to GitHub Pages
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
- `docs-agent.md` - Documentation orchestrator
- `docs-writer.md` - Documentation content writer
- `api-spec-generator.md` - API spec extraction from Kotlin
- `changelog-agent.md` - Changelog generation
- `site-deployer.md` - GitHub Pages deployment

### Skills Location

Custom skills are stored in `.agents/skills/`:
- `openapi-extractor.md` - Parse Kotlin/Retrofit for API definitions
- `docs-deploy.md` - GitHub Pages deployment configuration

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
| "Update the documentation site" | docs-agent |
| "Generate API docs from Kotlin" | api-spec-generator |
| "Create changelog for latest release" | changelog-agent |
| "Deploy docs to GitHub Pages" | site-deployer |

## Tech Stack
- **Language**: Kotlin
- **Platform**: Android (Min SDK not specified, targets Clover devices)
- **Build System**: Gradle (Kotlin DSL)
- **Backend**: Firebase Realtime Database for configuration storage
- **POS Integration**: Clover SDK v3

## Project Structure
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

## Key Files
- `CommonFunctions.kt` - Utility functions including `getThePaymentState()`
- `ProfileSettingsFragment.kt` - Theme color picker and avatar management
- `CalendarFragment.kt` - Calendar view with day/week/month modes
- `OrderListRedesignFragment.kt` - Redesigned order list view
- `OrderCardRedesignAdapter.kt` - Order card UI with pill badges
- `SettingsFragment.kt` - App settings with multiple tabs
- `OrderDetailFragment.kt` - Order detail page with customer/item/history cards
- `WidgetManager.kt` - Widget configuration management (V2 schema)
- `FirebaseConfigManager.kt` - Firebase configuration persistence

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

## Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

## Testing
Unit tests located in `app/src/test/java/com/orderMate/`

## Branches
- `main` - Production branch
- `complete_redesign_logic` - V2 redesign with iOS-style UI
- `complete_v2_redesign_2` - Working branch for ticket implementation

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

## Documentation Site

The documentation site is located in `docs-site/` and uses:
- **Frontend**: React + TypeScript + Tailwind CSS v4 + Vite
- **Backend**: Python FastAPI (proxy/mock endpoints)
- **Style**: Stripe-style 3-panel API documentation layout

### Documentation Structure
```
docs-site/
├── frontend/
│   ├── src/
│   │   ├── components/     # UI components (Layout, ApiReference, ui)
│   │   ├── pages/          # Route pages (Home, GettingStarted, Api/*)
│   │   ├── data/           # endpoints.ts, navigation.ts
│   │   └── types/          # TypeScript definitions
│   └── dist/               # Built output
└── backend/
    └── app/                # FastAPI application
```

### Documentation Commands
```bash
# Development
cd docs-site/frontend && npm run dev

# Build
cd docs-site/frontend && npm run build

# Deploy (via GitHub Actions)
git push origin main  # Triggers .github/workflows/deploy-docs.yml
```

### Live Site
- **URL**: https://11thandorange.github.io/OrderMate/
- **Deployment**: GitHub Pages (automatic on merge to main)
