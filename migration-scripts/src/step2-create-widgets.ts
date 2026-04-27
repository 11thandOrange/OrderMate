/**
 * Step 2: Create V2 Widgets
 * 
 * This script reads the analysis from Step 1 and creates V2 widgets for each merchant:
 * - For each legacy note type/label found, creates a V2 widget with the exact same label
 * - Applies all legacy note values as widget options
 * - Widget type is determined by label name inference:
 *   - pickup date -> CALENDAR
 *   - status -> SINGLE_SELECT  
 *   - category -> SINGLE_SELECT
 *   - subcategory -> MULTI_SELECT
 *   - type -> MULTI_SELECT
 *   - description -> TEXT_BOX
 * 
 * Output is written to local files for testing.
 * To apply to Firebase: upload step2_firebase_widgets_<merchantId>.json
 * to: merchants/<merchantId>/widgets
 * 
 * NOTE: Legacy code and orders are NOT touched.
 * 
 * Usage: npx ts-node src/step2-create-widgets.ts [merchantId]
 * 
 * Example Output (step2_widgets_MERCHANT_001.json):
 * {
 *   "timestamp": "2026-04-27T01:00:00.000Z",
 *   "merchantId": "MERCHANT_001",
 *   "createdWidgets": [
 *     {
 *       "id": "uuid-1",
 *       "type": "SINGLE_SELECT",
 *       "label": "Category",
 *       "level": "ITEM",
 *       "options": [
 *         { "id": "opt-1", "label": "Birthday", "value": "Birthday" },
 *         { "id": "opt-2", "label": "Wedding", "value": "Wedding" }
 *       ]
 *     },
 *     {
 *       "id": "uuid-2",
 *       "type": "CALENDAR",
 *       "label": "Pickup Date",
 *       "level": "ITEM",
 *       "options": []
 *     }
 *   ],
 *   "widgetLabelMapping": {
 *     "Category": "uuid-1",
 *     "Pickup Date": "uuid-2"
 *   }
 * }
 */

import * as fs from 'fs';
import * as path from 'path';
import { v4 as uuidv4 } from 'uuid';
import {
  WidgetConfig,
  WidgetOption,
  WidgetType,
  NoteLevel,
  Step1Output,
  MerchantAnalysisOutput,
  Step2Output,
  LabelStats
} from './types';
import {
  getWidgetTypeForLabel,
  inferLegacyTypeFromLabel,
  writeOutput,
  readInput,
  fileExists,
  OUTPUT_DIR,
  ensureOutputDir,
  widgetToFirebaseFormat
} from './utils';

/**
 * Create a widget from label statistics
 */
function createWidgetFromLabelStats(
  labelStat: LabelStats,
  order: number
): WidgetConfig {
  const widgetType = getWidgetTypeForLabel(labelStat.label);
  
  // Create options from unique values (only for select widgets)
  let options: WidgetOption[] = [];
  if (widgetType === WidgetType.SINGLE_SELECT || widgetType === WidgetType.MULTI_SELECT) {
    options = labelStat.uniqueValues.map((value, index) => ({
      id: uuidv4(),
      label: value,
      value: value,
      isDefault: index === 0,
      color: undefined
    }));
  }
  
  return {
    id: uuidv4(),
    type: widgetType,
    label: labelStat.label,
    isEnabled: true,
    isRequired: false,
    showInFilter: widgetType !== WidgetType.TEXT_BOX,
    order,
    level: NoteLevel.ITEM,  // All legacy notes are ITEM level
    options
  };
}

/**
 * Process a single merchant's analysis and create widgets
 */
function createWidgetsForMerchant(merchantAnalysis: MerchantAnalysisOutput): Step2Output {
  console.log(`\nCreating widgets for merchant: ${merchantAnalysis.merchantId}`);
  
  const createdWidgets: WidgetConfig[] = [];
  const widgetLabelMapping: Record<string, string> = {};
  
  // Process each label found in the analysis
  let order = 0;
  for (const labelStat of merchantAnalysis.labelStats) {
    // Skip if no legacy notes with this label (already V2)
    if (labelStat.occurrences === 0) {
      continue;
    }
    
    // Create widget
    const widget = createWidgetFromLabelStats(labelStat, order);
    createdWidgets.push(widget);
    widgetLabelMapping[labelStat.label] = widget.id;
    
    console.log(`  Created widget: "${widget.label}" (${widget.type})`);
    console.log(`    ID: ${widget.id}`);
    console.log(`    Options: ${widget.options.length}`);
    if (widget.options.length > 0 && widget.options.length <= 5) {
      console.log(`    Values: ${widget.options.map(o => o.value).join(', ')}`);
    }
    
    order++;
  }
  
  console.log(`\n  Total widgets created: ${createdWidgets.length}`);
  
  return {
    timestamp: new Date().toISOString(),
    merchantId: merchantAnalysis.merchantId,
    createdWidgets,
    widgetLabelMapping
  };
}

/**
 * Generate Firebase-format output for widgets
 */
function generateFirebaseWidgetsJson(widgets: WidgetConfig[]): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  
  for (const widget of widgets) {
    result[widget.id] = widgetToFirebaseFormat(widget);
  }
  
  return result;
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  console.log('='.repeat(60));
  console.log('Step 2: Create V2 Widgets');
  console.log('='.repeat(60));
  
  ensureOutputDir();
  
  // Get merchant ID from command line
  const args = process.argv.slice(2);
  const targetMerchantId = args[0];
  
  // Load Step 1 analysis
  const step1File = 'step1_analysis_all.json';
  if (!fileExists(step1File)) {
    console.error(`Error: Step 1 output not found. Run step1-analyze-notes.ts first.`);
    process.exit(1);
  }
  
  const step1Output = readInput<Step1Output>(step1File);
  console.log(`Loaded Step 1 analysis from ${step1Output.timestamp}`);
  console.log(`Found ${step1Output.merchants.length} merchants`);
  
  // Filter to target merchant if specified
  let merchantsToProcess = step1Output.merchants;
  if (targetMerchantId) {
    merchantsToProcess = step1Output.merchants.filter(m => m.merchantId === targetMerchantId);
    if (merchantsToProcess.length === 0) {
      console.error(`Error: Merchant ${targetMerchantId} not found in Step 1 analysis.`);
      process.exit(1);
    }
    console.log(`Processing single merchant: ${targetMerchantId}`);
  }
  
  // Process each merchant
  const allOutputs: Step2Output[] = [];
  
  for (const merchantAnalysis of merchantsToProcess) {
    const output = createWidgetsForMerchant(merchantAnalysis);
    allOutputs.push(output);
    
    // Write individual merchant output
    writeOutput(`step2_widgets_${merchantAnalysis.merchantId}.json`, output);
    
    // Also write Firebase-format JSON (what would be uploaded)
    const firebaseWidgets = generateFirebaseWidgetsJson(output.createdWidgets);
    writeOutput(`step2_firebase_widgets_${merchantAnalysis.merchantId}.json`, {
      merchantId: merchantAnalysis.merchantId,
      path: `merchants/${merchantAnalysis.merchantId}/widgets`,
      data: firebaseWidgets
    });
  }
  
  // Write combined output
  const combinedOutput = {
    timestamp: new Date().toISOString(),
    totalMerchants: allOutputs.length,
    merchants: allOutputs.map(o => ({
      merchantId: o.merchantId,
      widgetCount: o.createdWidgets.length,
      labels: Object.keys(o.widgetLabelMapping)
    }))
  };
  
  writeOutput('step2_summary.json', combinedOutput);
  
  // Print summary
  console.log('\n' + '='.repeat(60));
  console.log('Summary');
  console.log('='.repeat(60));
  
  let totalWidgets = 0;
  for (const output of allOutputs) {
    console.log(`\nMerchant: ${output.merchantId}`);
    console.log(`  Widgets created: ${output.createdWidgets.length}`);
    console.log(`  Label -> Widget ID mapping:`);
    for (const [label, widgetId] of Object.entries(output.widgetLabelMapping)) {
      console.log(`    "${label}" -> ${widgetId}`);
    }
    totalWidgets += output.createdWidgets.length;
  }
  
  console.log(`\nTotal widgets created: ${totalWidgets}`);
  console.log('\nOutput files:');
  console.log(`  - step2_widgets_<merchantId>.json (widget configs)`);
  console.log(`  - step2_firebase_widgets_<merchantId>.json (Firebase format)`);
  console.log(`  - step2_summary.json (summary of all merchants)`);
  
  console.log('\n' + '='.repeat(60));
  console.log('NOTE: Widgets have been written to LOCAL FILES only.');
  console.log('To apply to Firebase, manually upload the firebase_widgets JSON');
  console.log('to: merchants/<merchantId>/widgets');
  console.log('='.repeat(60));
  
  console.log('\nStep 2 complete. Run step3-migrate-orders.ts next.');
}

// Run main
main().catch(error => {
  console.error('Error:', error);
  process.exit(1);
});
