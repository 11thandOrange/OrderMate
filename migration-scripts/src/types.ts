/**
 * Migration Types for Legacy to V2 OrderMate schema migration
 */

// Widget types matching Android WidgetType enum
export enum WidgetType {
  SINGLE_SELECT = 'SINGLE_SELECT',
  MULTI_SELECT = 'MULTI_SELECT',
  TEXT_BOX = 'TEXT_BOX',
  CALENDAR = 'CALENDAR'
}

// Note level matching Android NoteLevel enum
export enum NoteLevel {
  ITEM = 'ITEM',
  ORDER = 'ORDER'
}

// Legacy modal dialog categories (from ModalDialogCategories.kt)
export enum LegacyNoteType {
  Description = 'Description',
  OrderType = 'OrderType',
  OrderProgress = 'OrderProgress',
  OrderCategories = 'OrderCategories',
  OrderSubCategories = 'OrderSubCategories',
  PickUpDate = 'PickUpDate',
  ModalShown = 'ModalShown',
  BasketShown = 'BasketShown',
  // Generic types inferred from note content
  Status = 'Status',
  Category = 'Category',
  SubCategory = 'SubCategory',
  Type = 'Type'
}

// Widget option for V2 schema
export interface WidgetOption {
  id: string;
  label: string;
  value: string;
  isDefault: boolean;
  color: string | null;  // null is default per Android WidgetOption model
}

// Widget configuration for V2 schema
export interface WidgetConfig {
  id: string;
  type: WidgetType;
  label: string;
  isEnabled: boolean;
  isRequired: boolean;
  showInFilter: boolean;
  order: number;
  level: NoteLevel;
  options: WidgetOption[];
}

// Legacy modal data from Firebase customData
export interface LegacyModalData {
  name: string;
  type: string;
  hasDropDown: boolean;
  isActive: boolean;
  list: string[];
}

// Legacy custom item JSON structure
export interface LegacyCustomItemJson {
  types: LegacyModalData[];
}

// Parsed note entry
export interface ParsedNote {
  widgetId?: string;  // Present in V2 format [widgetId]
  label: string;
  value: string;
}

// Note format classification
export enum NoteFormat {
  EMPTY = 'EMPTY',
  LEGACY = 'LEGACY',  // label:value (no widget ID)
  V2 = 'V2'           // [widgetId]label:value
}

// Order note analysis result
export interface OrderNoteAnalysis {
  orderId: string;
  merchantId: string;
  originalNote: string;
  format: NoteFormat;
  parsedNotes: ParsedNote[];
  noteLevel: NoteLevel;
}

// Aggregated analysis result
export interface MerchantNoteAnalysis {
  merchantId: string;
  totalOrders: number;
  emptyNotes: number;
  legacyNotes: number;
  v2Notes: number;
  labelValueMap: Map<string, Set<string>>;  // label -> unique values
  notesByType: Map<string, OrderNoteAnalysis[]>;  // label -> notes with that label
}

// Analysis output for Step 1
export interface Step1Output {
  timestamp: string;
  totalMerchants: number;
  merchants: MerchantAnalysisOutput[];
}

export interface NoteLevelCounts {
  empty: number;
  legacy: number;
  v2: number;
}

export interface MerchantAnalysisOutput {
  merchantId: string;
  totalOrders: number;
  totalLineItems: number;
  orderLevelNotes: NoteLevelCounts;
  itemLevelNotes: NoteLevelCounts;
  labelStats: LabelStats[];
}

export interface LabelStats {
  label: string;
  occurrences: number;
  uniqueValues: string[];
  inferredType: string;
}

// Step 2 output - widget migration
export interface Step2Output {
  timestamp: string;
  merchantId: string;
  createdWidgets: WidgetConfig[];
  widgetLabelMapping: Record<string, string>;  // label -> widgetId
}

// Step 3 output - order note migration
export interface Step3Output {
  timestamp: string;
  merchantId: string;
  totalOrders: number;
  migratedOrders: number;
  skippedOrders: number;
  orders: MigratedOrder[];
}

export interface MigratedOrder {
  orderId: string;
  originalNote: string;
  migratedNote: string;
  noteLevel: NoteLevel;
  wasLegacy: boolean;
}

// Step 4 output - validation
export interface Step4Output {
  timestamp: string;
  merchantId: string;
  totalOrders: number;
  validOrders: number;
  invalidOrders: number;
  validationResults: ValidationResult[];
}

export interface ValidationResult {
  orderId: string;
  note: string;
  isValid: boolean;
  errors: string[];
}

// Firebase raw data structures
export interface FirebaseOrder {
  id: string;
  note?: string;
  lineItems?: FirebaseLineItem[];
}

export interface FirebaseLineItem {
  id: string;
  note?: string;
  name?: string;
}

// Mock data structure for testing (Clover-like)
export interface CloverOrder {
  id: string;
  note?: string;
  lineItems?: {
    elements?: CloverLineItem[];
  };
}

export interface CloverLineItem {
  id: string;
  note?: string;
  name?: string;
}
