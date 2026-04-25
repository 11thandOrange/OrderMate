package com.orderMate.utils

import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * (#29, #30) Parses order and line item notes based on widget configuration.
 * 
 * Note format: "label:value • label:value • label:value"
 * Example: "Category:Custom Cake • Status:In Progress • Due Date:Apr 20, 2026"
 */
object OrderNoteParser {
    
    private val dateFormats = listOf(
        // DateTime formats (with time) - check these first
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        // Date-only formats
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    
    /**
     * Parse a note string and match values to widgets by ID.
     * Format: [widgetId]label:value
     */
    fun parseNotesByWidgetType(
        note: String?,
        widgets: List<WidgetConfig>,
        level: NoteLevel
    ): Map<WidgetConfig, String> {
        if (note.isNullOrBlank()) return emptyMap()
        
        val filteredWidgets = widgets.filter { it.level == level && it.isEnabled }
        if (filteredWidgets.isEmpty()) return emptyMap()
        
        val result = mutableMapOf<WidgetConfig, String>()
        val parsedValues = parseNoteToMapWithIds(note)
        
        for (widget in filteredWidgets) {
            val value = parsedValues.entries.find { (key, _) ->
                key.widgetId == widget.id
            }?.value
            
            if (!value.isNullOrBlank()) {
                result[widget] = value
            }
        }
        
        return result
    }
    
    /**
     * Parsed key containing optional widget ID and label.
     */
    data class ParsedKey(val widgetId: String?, val label: String)
    
    /**
     * Parse a note string into a map of ParsedKey -> value.
     * Handles both new format [widgetId]label:value and old format label:value
     */
    private fun parseNoteToMapWithIds(note: String?): Map<ParsedKey, String> {
        if (note.isNullOrBlank()) return emptyMap()
        
        val result = mutableMapOf<ParsedKey, String>()
        val delimiter = if (note.contains("•")) "•" else "|"
        val parts = note.split(delimiter).map { it.trim() }
        
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val keyPart = part.substring(0, colonIndex).trim()
                val value = part.substring(colonIndex + 1).trim()
                
                if (keyPart.isNotBlank() && value.isNotBlank()) {
                    // Try to extract widget ID from new format: [widgetId]label
                    val parsedKey = if (keyPart.startsWith("[") && keyPart.contains("]")) {
                        val closeBracket = keyPart.indexOf(']')
                        val widgetId = keyPart.substring(1, closeBracket)
                        val label = keyPart.substring(closeBracket + 1)
                        ParsedKey(widgetId, label)
                    } else {
                        // Old format: just label
                        ParsedKey(null, keyPart)
                    }
                    result[parsedKey] = value
                }
            }
        }
        
        return result
    }
    
    /**
     * Parse a note string into a map of label -> value.
     * Note: This strips widget IDs for backward compatibility.
     */
    fun parseNoteToMap(note: String?): Map<String, String> {
        if (note.isNullOrBlank()) return emptyMap()
        
        val parsed = parseNoteToMapWithIds(note)
        return parsed.map { (key, value) -> key.label to value }.toMap()
    }
    
    /**
     * (#29) Extract date value from note for widgets with type=CALENDAR.
     */
    fun extractDateFromNote(
        note: String?,
        widgets: List<WidgetConfig>,
        level: NoteLevel
    ): Date? {
        val parsed = parseNotesByWidgetType(note, widgets, level)
        val calendarEntry = parsed.entries.find { it.key.type == WidgetType.CALENDAR }
        return calendarEntry?.value?.let { parseDate(it) }
    }
    
    /**
     * (#30) Extract all tag values (SINGLE_SELECT, MULTI_SELECT, CALENDAR, TEXT_BOX) from note.
     * Use includeTextBox parameter to control whether TEXT_BOX widgets are included.
     */
    fun extractTagsFromNote(
        note: String?,
        widgets: List<WidgetConfig>,
        level: NoteLevel,
        includeTextBox: Boolean = true
    ): List<ParsedTag> {
        val parsed = parseNotesByWidgetType(note, widgets, level)
        val tags = mutableListOf<ParsedTag>()
        
        parsed.forEach { (widget, value) ->
            when (widget.type) {
                WidgetType.SINGLE_SELECT -> {
                    tags.add(ParsedTag(widget.label, value, TagType.SINGLE_SELECT, widget.type))
                }
                WidgetType.MULTI_SELECT -> {
                    value.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { v ->
                        tags.add(ParsedTag(widget.label, v, TagType.MULTI_SELECT, widget.type))
                    }
                }
                WidgetType.CALENDAR -> {
                    tags.add(ParsedTag(widget.label, value, TagType.CALENDAR, widget.type))
                }
                WidgetType.TEXT_BOX -> {
                    if (includeTextBox && value.isNotBlank()) {
                        tags.add(ParsedTag(widget.label, value, TagType.TEXT_BOX, widget.type))
                    }
                }
            }
        }
        
        return tags
    }
    
    /**
     * Extract description (TEXT_BOX) from note.
     */
    fun extractDescriptionFromNote(
        note: String?,
        widgets: List<WidgetConfig>,
        level: NoteLevel
    ): String? {
        val parsed = parseNotesByWidgetType(note, widgets, level)
        val textEntry = parsed.entries.find { it.key.type == WidgetType.TEXT_BOX }
        return textEntry?.value
    }
    
    /**
     * Parse a date string using multiple formats (supports both date-only and datetime).
     */
    fun parseDate(dateString: String): Date? {
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(dateString)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
    
    data class ParsedTag(
        val label: String,
        val value: String,
        val type: TagType,
        val widgetType: com.orderMate.modals.WidgetType
    )
    
    enum class TagType {
        SINGLE_SELECT,
        MULTI_SELECT,
        CALENDAR,
        TEXT_BOX
    }
}
