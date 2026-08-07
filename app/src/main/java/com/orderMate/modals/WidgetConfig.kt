package com.orderMate.modals

import java.util.UUID

/**
 * Note level for widgets - distinguishes item-level vs order-level notes
 */
enum class NoteLevel {
    ITEM,   // Notes attached to line items (LineItem.note)
    ORDER   // Notes attached to orders (Order.note)
}

/**
 * Widget configuration stored in Firebase
 * Path: merchants/{merchantId}/widgets/{widgetId}
 */
data class WidgetConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType = WidgetType.TEXT_BOX,
    var label: String = "",
    var isEnabled: Boolean = true,
    var isRequired: Boolean = false,
    var showInFilter: Boolean = true,
    var options: MutableList<WidgetOption> = mutableListOf(),
    var order: Int = 0,
    var level: NoteLevel = NoteLevel.ITEM
) {
    // Firebase requires no-arg constructor
    constructor() : this(
        id = UUID.randomUUID().toString(),
        type = WidgetType.TEXT_BOX,
        label = "",
        level = NoteLevel.ITEM
    )
    
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "label" to label,
        "isEnabled" to isEnabled,
        "isRequired" to isRequired,
        "showInFilter" to showInFilter,
        "order" to order,
        "level" to level.name,
        "options" to options.associate { it.id to it.toMap() }
    )
}

/**
 * Option for SINGLE_SELECT and MULTI_SELECT widgets
 */
data class WidgetOption(
    val id: String = UUID.randomUUID().toString(),
    var label: String = "",
    var value: String = "",
    var isDefault: Boolean = false,
    var color: String? = null
) {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        label = "",
        value = ""
    )
    
    fun toMap(): Map<String, Any?> = mapOf(
        "label" to label,
        "value" to value,
        "isDefault" to isDefault,
        "color" to color
    )
}

/**
 * Widget types for pop-up editor
 */
enum class WidgetType(val displayName: String) {
    SINGLE_SELECT("Single Select"),
    MULTI_SELECT("Multi Select"),
    TEXT_BOX("Text Box"),
    CALENDAR("Calendar"),
    QUANTITY("Quantity");

    /**
     * Whether this widget type has discrete values a merchant could filter the order list
     * by. TEXT_BOX (free text) and QUANTITY (a live number, not a value with a fixed set of
     * options) don't - they're excluded from the order-list filter dialog and from the
     * "Filter" toggle list in Settings entirely, not just defaulted off.
     */
    val isFilterable: Boolean
        get() = this != TEXT_BOX && this != QUANTITY

    /**
     * Whether this widget's value is stored in the item/order note (Order.note /
     * LineItem.note, synced to Firebase-configured widgets) versus written directly to a
     * live Clover field with no note/DB storage at all. QUANTITY reads and writes
     * LineItem.unitQty directly - it has no note representation. Widgets that are
     * `!savesToNotes` render outside the notes section in the popup (see
     * ItemNoteDialogFragment) since they aren't "notes."
     */
    val savesToNotes: Boolean
        get() = this != QUANTITY

    companion object {
        @JvmStatic
        fun fromString(value: String?): WidgetType {
            if (value == null) return TEXT_BOX
            return WidgetType.values().firstOrNull { it.name == value } ?: TEXT_BOX
        }
    }
}
