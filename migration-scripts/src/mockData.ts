/**
 * Mock data generator for testing migration scripts
 * This simulates data that would come from Firebase/Clover
 */
import { v4 as uuidv4 } from 'uuid';
import {
  CloverOrder,
  CloverLineItem,
  LegacyCustomItemJson,
  LegacyModalData,
  WidgetConfig,
  NoteLevel
} from './types';

// Sample merchant IDs for testing
export const MOCK_MERCHANT_IDS = [
  'MERCHANT_001',
  'MERCHANT_002',
  'MERCHANT_003'
];

/**
 * Generate legacy format notes (label:value)
 */
function generateLegacyNote(): string {
  const noteTemplates = [
    // Category-based notes
    'Category:Birthday',
    'Category:Wedding',
    'Category:Custom Cake',
    'Category:Anniversary',
    // Status-based notes
    'Status:In Progress',
    'Status:Ready',
    'Status:Pending',
    'Status:Completed',
    // Multiple notes combined
    'Category:Birthday, Status:In Progress',
    'Category:Wedding, Status:Ready, Description:Custom design requested',
    'Category:Custom Cake, Pickup Date:Apr 20, 2026',
    'Type:Delivery, Status:Pending',
    'SubCategory:Chocolate, SubCategory:Vanilla, Category:Custom',
    // Date notes
    'Pickup Date:Apr 25, 2026',
    'Due Date:May 1, 2026',
    // Description notes
    'Description:Red roses with white frosting',
    'Notes:Customer prefers less sugar',
    // Complex combinations
    'Category:Wedding, SubCategory:3-Tier, Status:In Progress, Pickup Date:Jun 15, 2026',
    'Type:Rush, Type:Delivery, Category:Birthday, Description:Need by 3pm',
  ];
  
  return noteTemplates[Math.floor(Math.random() * noteTemplates.length)];
}

/**
 * Generate V2 format notes ([widgetId]label:value)
 */
function generateV2Note(widgets: WidgetConfig[]): string {
  const parts: string[] = [];
  
  // Randomly select 1-3 widgets to populate
  const numWidgets = Math.floor(Math.random() * 3) + 1;
  const shuffled = widgets.sort(() => Math.random() - 0.5);
  const selected = shuffled.slice(0, numWidgets);
  
  for (const widget of selected) {
    let value: string;
    if (widget.options.length > 0) {
      // Pick random option
      const opt = widget.options[Math.floor(Math.random() * widget.options.length)];
      value = opt.value;
    } else {
      // Text or date
      value = widget.type === 'CALENDAR' ? 'May 10, 2026' : 'Sample text value';
    }
    parts.push(`[${widget.id}]${widget.label}:${value}`);
  }
  
  return parts.join(' • ');
}

/**
 * Generate empty note
 */
function generateEmptyNote(): string {
  return '';
}

/**
 * Generate mock Clover orders for a merchant
 */
export function generateMockOrders(
  merchantId: string,
  count: number,
  widgets?: WidgetConfig[]
): CloverOrder[] {
  const orders: CloverOrder[] = [];
  
  for (let i = 0; i < count; i++) {
    const orderId = `ORDER_${merchantId}_${i.toString().padStart(4, '0')}`;
    
    // Determine note type distribution:
    // 40% legacy, 30% empty, 30% v2 (if widgets provided)
    const rand = Math.random();
    let note: string;
    
    if (rand < 0.4) {
      note = generateLegacyNote();
    } else if (rand < 0.7 || !widgets || widgets.length === 0) {
      note = generateEmptyNote();
    } else {
      note = generateV2Note(widgets);
    }
    
    // Generate line items with their own notes
    const lineItems: CloverLineItem[] = [];
    const numLineItems = Math.floor(Math.random() * 5) + 1;
    
    for (let j = 0; j < numLineItems; j++) {
      const itemRand = Math.random();
      let itemNote: string;
      
      if (itemRand < 0.5) {
        itemNote = generateLegacyNote();
      } else if (itemRand < 0.8 || !widgets) {
        itemNote = generateEmptyNote();
      } else {
        itemNote = generateV2Note(widgets.filter(w => w.level === NoteLevel.ITEM));
      }
      
      lineItems.push({
        id: `ITEM_${orderId}_${j}`,
        name: `Item ${j + 1}`,
        note: itemNote
      });
    }
    
    orders.push({
      id: orderId,
      note,
      lineItems: { elements: lineItems }
    });
  }
  
  return orders;
}

/**
 * Generate mock legacy data from Firebase customData
 */
export function generateMockLegacyData(merchantId: string): LegacyCustomItemJson {
  const types: LegacyModalData[] = [
    {
      name: 'Category',
      type: 'OrderCategories',
      hasDropDown: true,
      isActive: true,
      list: ['Birthday', 'Wedding', 'Custom Cake', 'Anniversary', 'Holiday']
    },
    {
      name: 'Status',
      type: 'OrderProgress',
      hasDropDown: true,
      isActive: true,
      list: ['Pending', 'In Progress', 'Ready', 'Completed', 'Delivered']
    },
    {
      name: 'Pickup Date',
      type: 'PickUpDate',
      hasDropDown: false,
      isActive: true,
      list: []
    },
    {
      name: 'Description',
      type: 'Description',
      hasDropDown: false,
      isActive: true,
      list: []
    },
    {
      name: 'Type',
      type: 'OrderType',
      hasDropDown: true,
      isActive: true,
      list: ['Pickup', 'Delivery', 'Dine-in', 'Rush']
    },
    {
      name: 'SubCategory',
      type: 'OrderSubCategories',
      hasDropDown: true,
      isActive: true,
      list: ['Chocolate', 'Vanilla', 'Red Velvet', 'Carrot', 'Fruit']
    }
  ];
  
  return { types };
}

/**
 * Generate sample V2 widgets for testing
 */
export function generateMockV2Widgets(merchantId: string): WidgetConfig[] {
  return [
    {
      id: uuidv4(),
      type: 'CALENDAR' as any,
      label: 'Due Date',
      isEnabled: true,
      isRequired: false,
      showInFilter: true,
      order: 0,
      level: NoteLevel.ORDER,
      options: []
    },
    {
      id: uuidv4(),
      type: 'SINGLE_SELECT' as any,
      label: 'Category',
      isEnabled: true,
      isRequired: false,
      showInFilter: true,
      order: 1,
      level: NoteLevel.ORDER,
      options: [
        { id: uuidv4(), label: 'Birthday', value: 'Birthday', isDefault: true, color: null },
        { id: uuidv4(), label: 'Wedding', value: 'Wedding', isDefault: false, color: null },
        { id: uuidv4(), label: 'Custom', value: 'Custom', isDefault: false, color: null }
      ]
    },
    {
      id: uuidv4(),
      type: 'SINGLE_SELECT' as any,
      label: 'Status',
      isEnabled: true,
      isRequired: false,
      showInFilter: true,
      order: 2,
      level: NoteLevel.ORDER,
      options: [
        { id: uuidv4(), label: 'Pending', value: 'Pending', isDefault: true, color: null },
        { id: uuidv4(), label: 'In Progress', value: 'In Progress', isDefault: false, color: null },
        { id: uuidv4(), label: 'Ready', value: 'Ready', isDefault: false, color: null }
      ]
    },
    {
      id: uuidv4(),
      type: 'TEXT_BOX' as any,
      label: 'Description',
      isEnabled: true,
      isRequired: false,
      showInFilter: false,
      order: 3,
      level: NoteLevel.ORDER,
      options: []
    }
  ];
}

/**
 * Sample test data with known formats for verification
 */
export const KNOWN_TEST_ORDERS: CloverOrder[] = [
  {
    id: 'TEST_LEGACY_001',
    note: 'Category:Birthday, Status:In Progress',
    lineItems: {
      elements: [
        { id: 'ITEM_001', name: 'Cake', note: 'Description:Chocolate with sprinkles' }
      ]
    }
  },
  {
    id: 'TEST_LEGACY_002',
    note: 'Pickup Date:Apr 20, 2026',
    lineItems: {
      elements: [
        { id: 'ITEM_002', name: 'Cupcakes', note: 'Category:Birthday, SubCategory:Vanilla' }
      ]
    }
  },
  {
    id: 'TEST_EMPTY_001',
    note: '',
    lineItems: {
      elements: [
        { id: 'ITEM_003', name: 'Cookie', note: '' }
      ]
    }
  },
  {
    id: 'TEST_LEGACY_003',
    note: 'Type:Delivery, Type:Rush, Category:Wedding',
    lineItems: {
      elements: [
        { id: 'ITEM_004', name: 'Wedding Cake', note: 'Status:In Progress, Description:5 tier' }
      ]
    }
  }
];

/**
 * Write mock data to output files for use by migration scripts
 */
export function setupMockDataFiles(): void {
  const fs = require('fs');
  const path = require('path');
  const outputDir = path.join(__dirname, '..', 'output');
  
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  
  // Generate mock data for each merchant
  for (const merchantId of MOCK_MERCHANT_IDS) {
    // Legacy Firebase data
    const legacyData = generateMockLegacyData(merchantId);
    fs.writeFileSync(
      path.join(outputDir, `mock_legacy_${merchantId}.json`),
      JSON.stringify(legacyData, null, 2)
    );
    
    // Mock orders (mix of legacy, empty, and some v2)
    const orders = generateMockOrders(merchantId, 50);
    fs.writeFileSync(
      path.join(outputDir, `mock_orders_${merchantId}.json`),
      JSON.stringify(orders, null, 2)
    );
  }
  
  // Also save known test orders
  fs.writeFileSync(
    path.join(outputDir, 'mock_orders_TEST.json'),
    JSON.stringify(KNOWN_TEST_ORDERS, null, 2)
  );
  
  console.log('Mock data files created in output directory');
}

// Run setup if executed directly
if (require.main === module) {
  setupMockDataFiles();
}
