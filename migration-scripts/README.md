# OrderMate Migration Scripts

Migration scripts for creating V2 widget configuration from legacy OrderMate note patterns.

## Overview

These scripts analyze legacy order notes and create V2 widget configuration:
1. **Analyze Notes** - Scan legacy orders to find all labels and values
2. **Create V2 Widgets** - Create widget configuration based on legacy patterns
3. **Validate Widgets** - Validate the V2 widget configuration before upload

**NOTE:** Legacy code and orders are NOT touched. Only V2 widget configuration is created.

## Prerequisites

- Node.js 18+
- npm or yarn

## Installation

```bash
cd migration-scripts
npm install
```

## Data Source

The scripts can read from two sources:

### 1. Mock Data (Default)
If no Clover credentials are set, scripts use generated mock data for testing.

### 2. Clover API (Production)
Set environment variables to read real merchant data from Clover:

```bash
export CLOVER_API_TOKEN="your-oauth-token"
export CLOVER_MERCHANT_ID="your-merchant-id"
export CLOVER_ENVIRONMENT="sandbox"  # or "production"
```

| Variable | Required | Description |
|----------|----------|-------------|
| `CLOVER_API_TOKEN` | Yes | Clover OAuth API token |
| `CLOVER_MERCHANT_ID` | Yes | Merchant ID to fetch orders for |
| `CLOVER_ENVIRONMENT` | No | `sandbox` (default) or `production` |

## Scripts

### Step 1: Analyze Notes
Analyzes all order notes to find legacy labels and values.

```bash
npm run step1
# or for a specific merchant:
npm run step1 -- MERCHANT_ID
```

**Output:**
- `output/step1_analysis_all.json` - Combined analysis
- `output/step1_analysis_<merchantId>.json` - Per-merchant analysis

### Step 2: Create V2 Widgets
Creates V2 widget configurations based on legacy note patterns found in Step 1.

```bash
npm run step2
# or for a specific merchant:
npm run step2 -- MERCHANT_ID
```

**Widget Type Mapping:**
| Legacy Label Pattern | V2 Widget Type |
|---------------------|----------------|
| Pickup Date, Due Date, Deadline | CALENDAR |
| Status, Progress | SINGLE_SELECT |
| Category | SINGLE_SELECT |
| SubCategory | MULTI_SELECT |
| Type | MULTI_SELECT |
| Description, Notes, Details | TEXT_BOX |

**Output:**
- `output/step2_widgets_<merchantId>.json` - Widget configs with label mapping
- `output/step2_firebase_widgets_<merchantId>.json` - Firebase upload format
- `output/step2_summary.json` - Summary of all widgets created

### Step 3: Validate Widgets
Validates the V2 widget configuration for format and data errors.

```bash
npm run step3
# or for a specific merchant:
npm run step3 -- MERCHANT_ID
```

**Validation Checks:**
- Widget structure and required fields
- Valid widget types (SINGLE_SELECT, MULTI_SELECT, CALENDAR, TEXT_BOX)
- Valid note levels (ITEM, ORDER)
- Options have required fields (id, label, value)
- No duplicate widget IDs or option IDs
- No duplicate labels within a merchant

**Output:**
- `output/step3_validation_<merchantId>.json` - Detailed validation results
- `output/step3_validation_summary.json` - Validation summary

### Run All Steps

```bash
npm run run-all
```

### Clean Output

```bash
npm run clean
```

## Applying to Firebase

After validation passes, upload the widget configuration to Firebase:

1. Open `output/step2_firebase_widgets_<merchantId>.json`
2. Upload the `data` object to: `merchants/<merchantId>/widgets`

## Output Directory Structure

```
output/
├── mock_orders_<merchantId>.json           # Mock data for testing
├── mock_legacy_<merchantId>.json           # Mock legacy Firebase data
├── step1_analysis_all.json                 # Combined analysis
├── step1_analysis_<merchantId>.json        # Per-merchant analysis
├── step2_widgets_<merchantId>.json         # Created widgets
├── step2_firebase_widgets_<merchantId>.json # Firebase format
├── step2_summary.json                      # Widget creation summary
├── step3_validation_<merchantId>.json      # Validation results
└── step3_validation_summary.json           # Validation summary
```

## Widget Configuration (V2)

```typescript
interface WidgetConfig {
  id: string;              // UUID
  type: WidgetType;        // SINGLE_SELECT | MULTI_SELECT | TEXT_BOX | CALENDAR
  label: string;           // Display label (same as legacy label)
  isEnabled: boolean;      // Widget is active
  isRequired: boolean;     // Value required
  showInFilter: boolean;   // Show in filter dropdown
  order: number;           // Display order
  level: NoteLevel;        // ITEM (all legacy notes are item-level)
  options: WidgetOption[]; // For select widgets (populated from legacy values)
}

interface WidgetOption {
  id: string;              // UUID
  label: string;           // Display label (same as legacy value)
  value: string;           // Stored value (same as legacy value)
  isDefault: boolean;      // Default selection
  color?: string;          // Optional pill color
}
```

## Firebase Paths

- Legacy: `customData/{merchantId}/data`
- V2: `merchants/{merchantId}/widgets/{widgetId}`

## Development

```bash
# Build TypeScript
npm run build

# Generate mock data only
npm run setup-mock
```
