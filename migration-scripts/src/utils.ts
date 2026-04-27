/**
 * Shared utilities for migration scripts
 */
import { v4 as uuidv4 } from 'uuid';
import * as fs from 'fs';
import * as path from 'path';
import {
  ParsedNote,
  NoteFormat,
  WidgetType,
  LegacyNoteType,
  WidgetConfig,
  WidgetOption,
  NoteLevel
} from './types';

// Output directory for migration results
export const OUTPUT_DIR = path.join(__dirname, '..', 'output');

// Ensure output directory exists
export function ensureOutputDir(): void {
  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }
}

// Write JSON output to file
export function writeOutput(filename: string, data: unknown): string {
  ensureOutputDir();
  const filepath = path.join(OUTPUT_DIR, filename);
  fs.writeFileSync(filepath, JSON.stringify(data, null, 2));
  console.log(`Output written to: ${filepath}`);
  return filepath;
}

// Read JSON input from file
export function readInput<T>(filename: string): T {
  const filepath = path.join(OUTPUT_DIR, filename);
  const content = fs.readFileSync(filepath, 'utf-8');
  return JSON.parse(content) as T;
}

// Check if file exists
export function fileExists(filename: string): boolean {
  const filepath = path.join(OUTPUT_DIR, filename);
  return fs.existsSync(filepath);
}

/**
 * Parse a note string into structured entries
 * Handles both legacy format (label:value) and V2 format ([widgetId]label:value)
 */
export function parseNote(note: string | null | undefined): ParsedNote[] {
  if (!note || note.trim() === '') {
    return [];
  }

  const results: ParsedNote[] = [];
  
  // Determine delimiter (V2 uses "•", legacy might use "|" or ",")
  const delimiter = note.includes('•') ? '•' : (note.includes('|') ? '|' : ',');
  const parts = note.split(delimiter).map(p => p.trim()).filter(p => p.length > 0);

  for (const part of parts) {
    const colonIndex = part.indexOf(':');
    if (colonIndex <= 0) continue;

    const keyPart = part.substring(0, colonIndex).trim();
    const value = part.substring(colonIndex + 1).trim();

    if (!keyPart || !value) continue;

    // Check for V2 format: [widgetId]label
    if (keyPart.startsWith('[') && keyPart.includes(']')) {
      const closeBracket = keyPart.indexOf(']');
      const widgetId = keyPart.substring(1, closeBracket);
      const label = keyPart.substring(closeBracket + 1);
      results.push({ widgetId, label, value });
    } else {
      // Legacy format: just label
      results.push({ label: keyPart, value });
    }
  }

  return results;
}

/**
 * Determine the format of a note string
 */
export function classifyNoteFormat(note: string | null | undefined): NoteFormat {
  if (!note || note.trim() === '') {
    return NoteFormat.EMPTY;
  }

  const parsed = parseNote(note);
  if (parsed.length === 0) {
    return NoteFormat.EMPTY;
  }

  // If any entry has a widgetId, it's V2
  const hasV2Format = parsed.some(p => p.widgetId !== undefined);
  return hasV2Format ? NoteFormat.V2 : NoteFormat.LEGACY;
}

/**
 * Infer legacy note type from label name
 */
export function inferLegacyTypeFromLabel(label: string): LegacyNoteType {
  const normalizedLabel = label.toLowerCase().trim();
  
  // Date-related labels
  if (normalizedLabel.includes('pickup') || 
      normalizedLabel.includes('due') ||
      normalizedLabel.includes('date') ||
      normalizedLabel.includes('deadline')) {
    return LegacyNoteType.PickUpDate;
  }
  
  // Status-related labels
  if (normalizedLabel.includes('status') ||
      normalizedLabel.includes('progress') ||
      normalizedLabel.includes('state')) {
    return LegacyNoteType.Status;
  }
  
  // Category-related labels
  if (normalizedLabel === 'category' ||
      normalizedLabel.includes('categories')) {
    return LegacyNoteType.Category;
  }
  
  // Subcategory-related labels
  if (normalizedLabel.includes('subcategor') ||
      normalizedLabel.includes('sub-categor') ||
      normalizedLabel.includes('sub categor')) {
    return LegacyNoteType.SubCategory;
  }
  
  // Type-related labels
  if (normalizedLabel === 'type' ||
      normalizedLabel.includes('order type') ||
      normalizedLabel.includes('types')) {
    return LegacyNoteType.Type;
  }
  
  // Description-related labels
  if (normalizedLabel.includes('description') ||
      normalizedLabel.includes('note') ||
      normalizedLabel.includes('details') ||
      normalizedLabel.includes('comment') ||
      normalizedLabel.includes('text')) {
    return LegacyNoteType.Description;
  }
  
  // Default to Category for unknown labels with dropdown options
  return LegacyNoteType.Category;
}

/**
 * Map legacy note type to V2 widget type
 */
export function mapLegacyTypeToWidgetType(legacyType: LegacyNoteType): WidgetType {
  switch (legacyType) {
    case LegacyNoteType.PickUpDate:
      return WidgetType.CALENDAR;
    case LegacyNoteType.Description:
      return WidgetType.TEXT_BOX;
    case LegacyNoteType.Status:
    case LegacyNoteType.OrderProgress:
    case LegacyNoteType.Category:
    case LegacyNoteType.OrderType:
      return WidgetType.SINGLE_SELECT;
    case LegacyNoteType.SubCategory:
    case LegacyNoteType.Type:
    case LegacyNoteType.OrderCategories:
    case LegacyNoteType.OrderSubCategories:
      return WidgetType.MULTI_SELECT;
    default:
      return WidgetType.SINGLE_SELECT;
  }
}

/**
 * Map label name to widget type based on inference rules from requirements:
 * - pickup date -> calendar widget
 * - status -> single select
 * - category -> single select
 * - subcategory -> multi select
 * - type -> multi select
 * - description -> text box widget
 */
export function getWidgetTypeForLabel(label: string): WidgetType {
  const legacyType = inferLegacyTypeFromLabel(label);
  return mapLegacyTypeToWidgetType(legacyType);
}

/**
 * Create a new widget configuration
 */
export function createWidget(
  label: string,
  type: WidgetType,
  values: string[],
  order: number,
  level: NoteLevel = NoteLevel.ORDER
): WidgetConfig {
  const options: WidgetOption[] = values.map((value, index) => ({
    id: uuidv4(),
    label: value,
    value: value,
    isDefault: index === 0,
    color: undefined
  }));

  return {
    id: uuidv4(),
    type,
    label,
    isEnabled: true,
    isRequired: false,
    showInFilter: type !== WidgetType.TEXT_BOX,
    order,
    level,
    options
  };
}

/**
 * Build a V2 note string from parsed notes and widget mapping
 */
export function buildV2Note(
  parsedNotes: ParsedNote[],
  widgetMapping: Map<string, string>  // label -> widgetId
): string {
  const parts: string[] = [];
  
  for (const note of parsedNotes) {
    const widgetId = widgetMapping.get(note.label);
    if (widgetId) {
      parts.push(`[${widgetId}]${note.label}:${note.value}`);
    } else {
      // Keep original format if no widget mapping found
      parts.push(`${note.label}:${note.value}`);
    }
  }
  
  return parts.join(' • ');
}

/**
 * Validate a V2 note against merchant widgets
 */
export function validateV2Note(
  note: string,
  widgets: WidgetConfig[]
): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];
  const parsed = parseNote(note);
  
  if (parsed.length === 0 && note.trim() !== '') {
    errors.push('Note could not be parsed');
    return { isValid: false, errors };
  }
  
  const widgetMap = new Map(widgets.map(w => [w.id, w]));
  
  for (const entry of parsed) {
    if (!entry.widgetId) {
      errors.push(`Missing widget ID for label: ${entry.label}`);
      continue;
    }
    
    const widget = widgetMap.get(entry.widgetId);
    if (!widget) {
      errors.push(`Invalid widget ID: ${entry.widgetId}`);
      continue;
    }
    
    if (widget.label !== entry.label) {
      errors.push(`Label mismatch: expected "${widget.label}", got "${entry.label}"`);
    }
    
    // For select widgets, validate value is in options
    if (widget.type === WidgetType.SINGLE_SELECT || widget.type === WidgetType.MULTI_SELECT) {
      const validValues = widget.options.map(o => o.value);
      const noteValues = entry.value.split(',').map(v => v.trim());
      
      for (const val of noteValues) {
        if (!validValues.includes(val)) {
          // Add the value as a new option (dynamic options are allowed)
          // But log a warning
          console.warn(`Value "${val}" not in predefined options for widget "${widget.label}"`);
        }
      }
    }
  }
  
  return { isValid: errors.length === 0, errors };
}

/**
 * Generate timestamp string for output files
 */
export function getTimestamp(): string {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

/**
 * Convert widget config to Firebase format
 */
export function widgetToFirebaseFormat(widget: WidgetConfig): Record<string, unknown> {
  const optionsMap: Record<string, unknown> = {};
  for (const opt of widget.options) {
    optionsMap[opt.id] = {
      label: opt.label,
      value: opt.value,
      isDefault: opt.isDefault,
      color: opt.color
    };
  }
  
  return {
    type: widget.type,
    label: widget.label,
    isEnabled: widget.isEnabled,
    isRequired: widget.isRequired,
    showInFilter: widget.showInFilter,
    order: widget.order,
    level: widget.level,
    options: optionsMap
  };
}
