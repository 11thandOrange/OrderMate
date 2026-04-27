/**
 * Step 3: Validate V2 Widgets
 * 
 * This script validates the V2 widget configuration created in Step 2:
 * - Checks widget structure and required fields
 * - Validates widget types are valid (SINGLE_SELECT, MULTI_SELECT, CALENDAR, TEXT_BOX)
 * - Validates note levels are valid (ITEM, ORDER)
 * - Checks options have required fields (id, label, value)
 * - Verifies no duplicate widget IDs or option IDs
 * - Verifies no duplicate labels within a merchant
 * 
 * NOTE: This validates the LOCAL output files, not Firebase.
 * Run this before uploading to Firebase to catch errors.
 * 
 * Usage: npx ts-node src/step3-validate-widgets.ts [merchantId]
 * 
 * Example Output (step3_validation_MERCHANT_001.json):
 * {
 *   "timestamp": "2026-04-27T01:00:00.000Z",
 *   "merchantId": "MERCHANT_001",
 *   "totalWidgets": 8,
 *   "validWidgets": 8,
 *   "invalidWidgets": 0,
 *   "errors": [],
 *   "warnings": []
 * }
 */

import * as fs from 'fs';
import * as path from 'path';
import {
  WidgetConfig,
  WidgetOption,
  WidgetType,
  NoteLevel,
  Step2Output
} from './types';
import {
  writeOutput,
  readInput,
  fileExists,
  OUTPUT_DIR,
  ensureOutputDir
} from './utils';
import { MOCK_MERCHANT_IDS } from './mockData';

// Valid widget types
const VALID_WIDGET_TYPES = ['SINGLE_SELECT', 'MULTI_SELECT', 'CALENDAR', 'TEXT_BOX'];

// Valid note levels
const VALID_NOTE_LEVELS = ['ITEM', 'ORDER'];

// Validation result types
interface ValidationError {
  widgetId?: string;
  widgetLabel?: string;
  field: string;
  message: string;
}

interface ValidationWarning {
  widgetId?: string;
  widgetLabel?: string;
  field: string;
  message: string;
}

interface Step3Output {
  timestamp: string;
  merchantId: string;
  totalWidgets: number;
  validWidgets: number;
  invalidWidgets: number;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

/**
 * Validate a single widget option
 */
function validateOption(
  option: WidgetOption,
  widgetId: string,
  widgetLabel: string,
  optionIndex: number
): { errors: ValidationError[]; warnings: ValidationWarning[] } {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  // Check required fields
  if (!option.id || option.id.trim() === '') {
    errors.push({
      widgetId,
      widgetLabel,
      field: `options[${optionIndex}].id`,
      message: 'Option ID is required'
    });
  }

  if (!option.label || option.label.trim() === '') {
    errors.push({
      widgetId,
      widgetLabel,
      field: `options[${optionIndex}].label`,
      message: 'Option label is required'
    });
  }

  if (!option.value || option.value.trim() === '') {
    errors.push({
      widgetId,
      widgetLabel,
      field: `options[${optionIndex}].value`,
      message: 'Option value is required'
    });
  }

  // Check for UUID format (warning only)
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (option.id && !uuidPattern.test(option.id)) {
    warnings.push({
      widgetId,
      widgetLabel,
      field: `options[${optionIndex}].id`,
      message: `Option ID "${option.id}" is not a valid UUID format`
    });
  }

  return { errors, warnings };
}

/**
 * Validate a single widget
 */
function validateWidget(
  widget: WidgetConfig,
  index: number
): { errors: ValidationError[]; warnings: ValidationWarning[] } {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  const widgetId = widget.id || `(index ${index})`;
  const widgetLabel = widget.label || '(no label)';

  // Check required fields
  if (!widget.id || widget.id.trim() === '') {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'id',
      message: 'Widget ID is required'
    });
  }

  if (!widget.label || widget.label.trim() === '') {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'label',
      message: 'Widget label is required'
    });
  }

  // Validate widget type
  if (!widget.type) {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'type',
      message: 'Widget type is required'
    });
  } else if (!VALID_WIDGET_TYPES.includes(widget.type)) {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'type',
      message: `Invalid widget type "${widget.type}". Must be one of: ${VALID_WIDGET_TYPES.join(', ')}`
    });
  }

  // Validate note level
  if (!widget.level) {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'level',
      message: 'Widget level is required'
    });
  } else if (!VALID_NOTE_LEVELS.includes(widget.level)) {
    errors.push({
      widgetId,
      widgetLabel,
      field: 'level',
      message: `Invalid note level "${widget.level}". Must be one of: ${VALID_NOTE_LEVELS.join(', ')}`
    });
  }

  // Check for UUID format (warning only)
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (widget.id && !uuidPattern.test(widget.id)) {
    warnings.push({
      widgetId,
      widgetLabel,
      field: 'id',
      message: `Widget ID "${widget.id}" is not a valid UUID format`
    });
  }

  // Validate order is a number
  if (typeof widget.order !== 'number') {
    warnings.push({
      widgetId,
      widgetLabel,
      field: 'order',
      message: `Order should be a number, got ${typeof widget.order}`
    });
  }

  // Validate boolean fields
  if (typeof widget.isEnabled !== 'boolean') {
    warnings.push({
      widgetId,
      widgetLabel,
      field: 'isEnabled',
      message: `isEnabled should be a boolean, got ${typeof widget.isEnabled}`
    });
  }

  // Validate options for select widgets
  if (widget.type === WidgetType.SINGLE_SELECT || widget.type === WidgetType.MULTI_SELECT) {
    if (!widget.options || !Array.isArray(widget.options)) {
      errors.push({
        widgetId,
        widgetLabel,
        field: 'options',
        message: 'Select widgets must have an options array'
      });
    } else if (widget.options.length === 0) {
      warnings.push({
        widgetId,
        widgetLabel,
        field: 'options',
        message: 'Select widget has no options defined'
      });
    } else {
      // Validate each option
      const optionIds = new Set<string>();
      widget.options.forEach((opt, optIndex) => {
        const optResult = validateOption(opt, widgetId, widgetLabel, optIndex);
        errors.push(...optResult.errors);
        warnings.push(...optResult.warnings);

        // Check for duplicate option IDs
        if (opt.id && optionIds.has(opt.id)) {
          errors.push({
            widgetId,
            widgetLabel,
            field: `options[${optIndex}].id`,
            message: `Duplicate option ID: ${opt.id}`
          });
        }
        if (opt.id) optionIds.add(opt.id);
      });
    }
  }

  // Calendar and TextBox should not have options
  if ((widget.type === WidgetType.CALENDAR || widget.type === WidgetType.TEXT_BOX) &&
      widget.options && widget.options.length > 0) {
    warnings.push({
      widgetId,
      widgetLabel,
      field: 'options',
      message: `${widget.type} widgets should not have options`
    });
  }

  return { errors, warnings };
}

/**
 * Validate all widgets for a merchant
 */
function validateMerchantWidgets(merchantId: string): Step3Output | null {
  console.log(`\nValidating widgets for merchant: ${merchantId}`);

  // Load Step 2 output
  const step2File = `step2_widgets_${merchantId}.json`;
  if (!fileExists(step2File)) {
    console.error(`  Error: Step 2 output not found for ${merchantId}.`);
    return null;
  }

  const step2Output = readInput<Step2Output>(step2File);
  const widgets = step2Output.createdWidgets;

  console.log(`  Loaded ${widgets.length} widgets from Step 2`);

  const allErrors: ValidationError[] = [];
  const allWarnings: ValidationWarning[] = [];
  let invalidCount = 0;

  // Check for duplicate widget IDs
  const widgetIds = new Set<string>();
  const widgetLabels = new Set<string>();

  widgets.forEach((widget, index) => {
    // Check for duplicate IDs
    if (widget.id && widgetIds.has(widget.id)) {
      allErrors.push({
        widgetId: widget.id,
        widgetLabel: widget.label,
        field: 'id',
        message: `Duplicate widget ID: ${widget.id}`
      });
    }
    if (widget.id) widgetIds.add(widget.id);

    // Check for duplicate labels
    if (widget.label && widgetLabels.has(widget.label.toLowerCase())) {
      allWarnings.push({
        widgetId: widget.id,
        widgetLabel: widget.label,
        field: 'label',
        message: `Duplicate widget label: ${widget.label}`
      });
    }
    if (widget.label) widgetLabels.add(widget.label.toLowerCase());

    // Validate widget
    const result = validateWidget(widget, index);
    allErrors.push(...result.errors);
    allWarnings.push(...result.warnings);

    if (result.errors.length > 0) {
      invalidCount++;
    }
  });

  // Log results
  console.log(`  Valid widgets: ${widgets.length - invalidCount}`);
  console.log(`  Invalid widgets: ${invalidCount}`);
  console.log(`  Errors: ${allErrors.length}`);
  console.log(`  Warnings: ${allWarnings.length}`);

  if (allErrors.length > 0) {
    console.log('  Errors found:');
    allErrors.slice(0, 5).forEach(err => {
      console.log(`    - [${err.widgetLabel}] ${err.field}: ${err.message}`);
    });
    if (allErrors.length > 5) {
      console.log(`    ... and ${allErrors.length - 5} more errors`);
    }
  }

  return {
    timestamp: new Date().toISOString(),
    merchantId,
    totalWidgets: widgets.length,
    validWidgets: widgets.length - invalidCount,
    invalidWidgets: invalidCount,
    errors: allErrors,
    warnings: allWarnings
  };
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  console.log('='.repeat(60));
  console.log('Step 3: Validate V2 Widgets');
  console.log('='.repeat(60));

  ensureOutputDir();

  // Get merchant ID from command line
  const args = process.argv.slice(2);
  const targetMerchantId = args[0];

  // Determine which merchants to process
  let merchantIds: string[];
  if (targetMerchantId) {
    merchantIds = [targetMerchantId];
    console.log(`Validating single merchant: ${targetMerchantId}`);
  } else {
    merchantIds = MOCK_MERCHANT_IDS;
    console.log(`Validating ${merchantIds.length} merchants`);
  }

  const allOutputs: Step3Output[] = [];

  for (const merchantId of merchantIds) {
    const output = validateMerchantWidgets(merchantId);

    if (output) {
      allOutputs.push(output);
      writeOutput(`step3_validation_${merchantId}.json`, output);
    }
  }

  // Write combined summary
  const totalErrors = allOutputs.reduce((sum, o) => sum + o.errors.length, 0);
  const totalWarnings = allOutputs.reduce((sum, o) => sum + o.warnings.length, 0);
  const totalInvalid = allOutputs.reduce((sum, o) => sum + o.invalidWidgets, 0);

  const summary = {
    timestamp: new Date().toISOString(),
    totalMerchants: allOutputs.length,
    totalWidgets: allOutputs.reduce((sum, o) => sum + o.totalWidgets, 0),
    totalValid: allOutputs.reduce((sum, o) => sum + o.validWidgets, 0),
    totalInvalid,
    totalErrors,
    totalWarnings,
    merchants: allOutputs.map(o => ({
      merchantId: o.merchantId,
      totalWidgets: o.totalWidgets,
      validWidgets: o.validWidgets,
      invalidWidgets: o.invalidWidgets,
      errors: o.errors.length,
      warnings: o.warnings.length
    }))
  };

  writeOutput('step3_validation_summary.json', summary);

  // Print summary
  console.log('\n' + '='.repeat(60));
  console.log('Validation Summary');
  console.log('='.repeat(60));

  for (const output of allOutputs) {
    console.log(`\nMerchant: ${output.merchantId}`);
    console.log(`  Widgets: ${output.totalWidgets} total, ${output.validWidgets} valid, ${output.invalidWidgets} invalid`);
    console.log(`  Errors: ${output.errors.length}, Warnings: ${output.warnings.length}`);
  }

  console.log('\n' + '='.repeat(60));
  console.log('Overall Results');
  console.log('='.repeat(60));
  console.log(`Total merchants: ${allOutputs.length}`);
  console.log(`Total widgets: ${summary.totalWidgets}`);
  console.log(`Valid: ${summary.totalValid}`);
  console.log(`Invalid: ${summary.totalInvalid}`);
  console.log(`Errors: ${totalErrors}`);
  console.log(`Warnings: ${totalWarnings}`);

  console.log('\nOutput files:');
  console.log(`  - step3_validation_<merchantId>.json (detailed results)`);
  console.log(`  - step3_validation_summary.json (summary)`);

  // Final status
  console.log('\n' + '='.repeat(60));
  if (totalErrors === 0) {
    console.log('✅ ALL WIDGETS VALIDATED SUCCESSFULLY');
    console.log('Ready to upload to Firebase.');
  } else {
    console.log('❌ VALIDATION ERRORS FOUND');
    console.log('Fix errors before uploading to Firebase.');
  }
  console.log('='.repeat(60));

  console.log('\nStep 3 complete.');
}

// Run main
main().catch(error => {
  console.error('Error:', error);
  process.exit(1);
});
