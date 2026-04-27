/**
 * Step 1: Analyze Notes
 * 
 * This script analyzes all order notes from the database and:
 * - Counts legacy vs V2 vs empty notes (both order-level and item-level)
 * - Tracks all note labels and their values
 * - Outputs statistics to a file for use by subsequent migration steps
 * 
 * Data Source:
 * - If CLOVER_API_TOKEN and CLOVER_MERCHANT_ID are set, uses Clover API
 * - Otherwise, uses mock data for testing
 * 
 * Environment Variables (for Clover API):
 *   CLOVER_API_TOKEN     - Clover OAuth API token
 *   CLOVER_MERCHANT_ID   - Merchant ID to fetch orders for
 *   CLOVER_ENVIRONMENT   - 'sandbox' or 'production' (default: sandbox)
 * 
 * Usage: npx ts-node src/step1-analyze-notes.ts [merchantId]
 * 
 * If merchantId is provided, only that merchant is analyzed.
 * Otherwise, uses CLOVER_MERCHANT_ID or mock merchants.
 * 
 * Example Output (step1_analysis_MERCHANT_001.json):
 * {
 *   "merchantId": "MERCHANT_001",
 *   "totalOrders": 50,
 *   "totalLineItems": 150,
 *   "orderLevelNotes": {
 *     "empty": 50,
 *     "legacy": 0,
 *     "v2": 0
 *   },
 *   "itemLevelNotes": {
 *     "empty": 80,
 *     "legacy": 70,
 *     "v2": 0
 *   },
 *   "labelStats": [
 *     {
 *       "label": "Category",
 *       "occurrences": 15,
 *       "uniqueValues": ["Birthday", "Wedding", "Custom Cake"],
 *       "inferredType": "Category"
 *     },
 *     {
 *       "label": "Status",
 *       "occurrences": 12,
 *       "uniqueValues": ["Pending", "In Progress", "Ready"],
 *       "inferredType": "Status"
 *     },
 *     {
 *       "label": "Pickup Date",
 *       "occurrences": 8,
 *       "uniqueValues": ["Apr 25, 2026", "May 1, 2026"],
 *       "inferredType": "PickUpDate"
 *     },
 *     {
 *       "label": "Description",
 *       "occurrences": 5,
 *       "uniqueValues": ["Red roses with white frosting", "Custom design"],
 *       "inferredType": "Description"
 *     }
 *   ]
 * }
 */

import * as fs from 'fs';
import * as path from 'path';
import {
  CloverOrder,
  CloverLineItem,
  NoteFormat,
  NoteLevel,
  OrderNoteAnalysis,
  MerchantAnalysisOutput,
  Step1Output,
  LabelStats,
  ParsedNote
} from './types';
import {
  parseNote,
  classifyNoteFormat,
  inferLegacyTypeFromLabel,
  getTimestamp,
  writeOutput,
  OUTPUT_DIR,
  ensureOutputDir
} from './utils';
import { MOCK_MERCHANT_IDS, generateMockOrders, KNOWN_TEST_ORDERS } from './mockData';
import { isCloverConfigured, fetchCloverOrders } from './cloverApi';

/**
 * Determine data source based on environment
 * - If CLOVER_API_TOKEN and CLOVER_MERCHANT_ID are set, use Clover API
 * - Otherwise, use mock data
 */
function useCloverApi(): boolean {
  return isCloverConfigured();
}

/**
 * Load orders for a merchant (from mock data or Clover API)
 */
async function loadOrders(merchantId: string): Promise<CloverOrder[]> {
  if (useCloverApi()) {
    // Use Clover API
    console.log(`  Data source: Clover API`);
    return await fetchCloverOrders(merchantId);
  }
  
  // Use mock data
  console.log(`  Data source: Mock data`);
  const mockFile = path.join(OUTPUT_DIR, `mock_orders_${merchantId}.json`);
  if (fs.existsSync(mockFile)) {
    const content = fs.readFileSync(mockFile, 'utf-8');
    return JSON.parse(content) as CloverOrder[];
  }
  // Generate mock data if file doesn't exist
  return generateMockOrders(merchantId, 50);
}

/**
 * Analyze a single note string
 */
function analyzeNote(
  note: string | undefined,
  orderId: string,
  merchantId: string,
  level: NoteLevel
): OrderNoteAnalysis {
  const originalNote = note || '';
  const format = classifyNoteFormat(originalNote);
  const parsedNotes = parseNote(originalNote);
  
  return {
    orderId,
    merchantId,
    originalNote,
    format,
    parsedNotes,
    noteLevel: level
  };
}

/**
 * Analyze all orders for a merchant
 */
async function analyzeOrdersForMerchant(merchantId: string): Promise<MerchantAnalysisOutput> {
  console.log(`\nAnalyzing merchant: ${merchantId}`);
  
  const orders = await loadOrders(merchantId);
  console.log(`  Loaded ${orders.length} orders`);
  
  // Track statistics - order level
  let orderEmptyNotes = 0;
  let orderLegacyNotes = 0;
  let orderV2Notes = 0;
  
  // Track statistics - item level
  let itemEmptyNotes = 0;
  let itemLegacyNotes = 0;
  let itemV2Notes = 0;
  
  const labelValueMap = new Map<string, Set<string>>();
  const allAnalyses: OrderNoteAnalysis[] = [];
  
  // Analyze order-level notes
  for (const order of orders) {
    const analysis = analyzeNote(order.note, order.id, merchantId, NoteLevel.ORDER);
    allAnalyses.push(analysis);
    
    switch (analysis.format) {
      case NoteFormat.EMPTY:
        orderEmptyNotes++;
        break;
      case NoteFormat.LEGACY:
        orderLegacyNotes++;
        break;
      case NoteFormat.V2:
        orderV2Notes++;
        break;
    }
    
    // Track label -> values mapping
    for (const parsed of analysis.parsedNotes) {
      if (!labelValueMap.has(parsed.label)) {
        labelValueMap.set(parsed.label, new Set());
      }
      labelValueMap.get(parsed.label)!.add(parsed.value);
    }
    
    // Analyze line item notes
    const lineItems = order.lineItems?.elements || [];
    for (const item of lineItems) {
      const itemAnalysis = analyzeNote(item.note, `${order.id}:${item.id}`, merchantId, NoteLevel.ITEM);
      allAnalyses.push(itemAnalysis);
      
      switch (itemAnalysis.format) {
        case NoteFormat.EMPTY:
          itemEmptyNotes++;
          break;
        case NoteFormat.LEGACY:
          itemLegacyNotes++;
          break;
        case NoteFormat.V2:
          itemV2Notes++;
          break;
      }
      
      // Track label -> values mapping
      for (const parsed of itemAnalysis.parsedNotes) {
        if (!labelValueMap.has(parsed.label)) {
          labelValueMap.set(parsed.label, new Set());
        }
        labelValueMap.get(parsed.label)!.add(parsed.value);
      }
    }
  }
  
  // Build label statistics
  const labelStats: LabelStats[] = [];
  for (const [label, values] of labelValueMap) {
    // Count occurrences of this label across all analyses
    const occurrences = allAnalyses.filter(a => 
      a.parsedNotes.some(p => p.label === label)
    ).length;
    
    const inferredType = inferLegacyTypeFromLabel(label);
    
    labelStats.push({
      label,
      occurrences,
      uniqueValues: Array.from(values),
      inferredType
    });
  }
  
  // Sort by occurrences (descending)
  labelStats.sort((a, b) => b.occurrences - a.occurrences);
  
  // Count total line items
  const totalLineItems = orders.reduce((sum, o) => sum + (o.lineItems?.elements?.length || 0), 0);
  
  console.log(`  Results:`);
  console.log(`    Order-level notes: ${orderEmptyNotes} empty, ${orderLegacyNotes} legacy, ${orderV2Notes} v2`);
  console.log(`    Item-level notes:  ${itemEmptyNotes} empty, ${itemLegacyNotes} legacy, ${itemV2Notes} v2`);
  console.log(`    Unique labels found: ${labelStats.length}`);
  
  return {
    merchantId,
    totalOrders: orders.length,
    totalLineItems,
    orderLevelNotes: {
      empty: orderEmptyNotes,
      legacy: orderLegacyNotes,
      v2: orderV2Notes
    },
    itemLevelNotes: {
      empty: itemEmptyNotes,
      legacy: itemLegacyNotes,
      v2: itemV2Notes
    },
    labelStats
  };
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  console.log('='.repeat(60));
  console.log('Step 1: Analyze Notes');
  console.log('='.repeat(60));
  
  ensureOutputDir();
  
  // Get merchant ID from command line or use all mock merchants
  const args = process.argv.slice(2);
  const targetMerchantId = args[0];
  
  let merchantIds: string[];
  if (targetMerchantId) {
    merchantIds = [targetMerchantId];
    console.log(`Analyzing single merchant: ${targetMerchantId}`);
  } else if (useCloverApi()) {
    // Use merchant ID from environment
    const config = process.env.CLOVER_MERCHANT_ID;
    if (config) {
      merchantIds = [config];
      console.log(`Analyzing merchant from Clover API: ${config}`);
    } else {
      throw new Error('CLOVER_MERCHANT_ID not set');
    }
  } else {
    // Use mock data
    merchantIds = MOCK_MERCHANT_IDS;
    console.log(`Analyzing ${merchantIds.length} merchants (mock data)`);
    
    // Generate mock data files if they don't exist
    console.log('\nGenerating mock data files...');
    const { setupMockDataFiles } = await import('./mockData');
    setupMockDataFiles();
  }
  
  // Analyze each merchant
  const merchants: MerchantAnalysisOutput[] = [];
  
  for (const merchantId of merchantIds) {
    const analysis = await analyzeOrdersForMerchant(merchantId);
    merchants.push(analysis);
    
    // Write individual merchant analysis
    writeOutput(`step1_analysis_${merchantId}.json`, analysis);
  }
  
  // Write combined output
  const output: Step1Output = {
    timestamp: new Date().toISOString(),
    totalMerchants: merchants.length,
    merchants
  };
  
  const outputFile = writeOutput('step1_analysis_all.json', output);
  
  // Print summary
  console.log('\n' + '='.repeat(60));
  console.log('Summary');
  console.log('='.repeat(60));
  
  let totalOrderLegacy = 0;
  let totalItemLegacy = 0;
  const allLabels = new Set<string>();
  
  for (const m of merchants) {
    totalOrderLegacy += m.orderLevelNotes.legacy;
    totalItemLegacy += m.itemLevelNotes.legacy;
    m.labelStats.forEach(l => allLabels.add(l.label));
  }
  
  console.log(`Total merchants analyzed: ${merchants.length}`);
  console.log(`Total order-level legacy notes: ${totalOrderLegacy}`);
  console.log(`Total item-level legacy notes: ${totalItemLegacy}`);
  console.log(`Unique labels found: ${allLabels.size}`);
  
  console.log('\nLabels by type:');
  const labelsByType = new Map<string, string[]>();
  for (const m of merchants) {
    for (const ls of m.labelStats) {
      if (!labelsByType.has(ls.inferredType)) {
        labelsByType.set(ls.inferredType, []);
      }
      if (!labelsByType.get(ls.inferredType)!.includes(ls.label)) {
        labelsByType.get(ls.inferredType)!.push(ls.label);
      }
    }
  }
  
  for (const [type, labels] of labelsByType) {
    console.log(`  ${type}: ${labels.join(', ')}`);
  }
  
  console.log(`\nOutput written to: ${outputFile}`);
  console.log('\nStep 1 complete. Run step2-create-widgets.ts next.');
}

// Run main
main().catch(error => {
  console.error('Error:', error);
  process.exit(1);
});
