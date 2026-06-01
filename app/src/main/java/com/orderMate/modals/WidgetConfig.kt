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
        "id" to id,
        "type" to type.name,
        "label" to label,
        "isEnabled" to isEnabled,
        "isRequired" to isRequired,
        "showInFilter" to showInFilter,
        "order" to order.toLong(),
        "level" to level.name,
        "options" to options.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): WidgetConfig {
            val rawOptions = map["options"]
            val options: MutableList<WidgetOption> = when (rawOptions) {
                is List<*> -> (rawOptions as List<Map<String, Any?>>)
                    .map { WidgetOption.fromMap(it) }.toMutableList()
                is Map<*, *> -> (rawOptions as Map<String, Map<String, Any?>>)
                    .values.map { WidgetOption.fromMap(it) }.toMutableList()
                else -> mutableListOf()
            }
            return WidgetConfig(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                type = WidgetType.fromString(map["type"] as? String),
                label = map["label"] as? String ?: "",
                isEnabled = map["isEnabled"] as? Boolean ?: true,
                isRequired = map["isRequired"] as? Boolean ?: false,
                showInFilter = map["showInFilter"] as? Boolean ?: true,
                options = options,
                order = (map["order"] as? Long)?.toInt() ?: map["order"] as? Int ?: 0,
                level = NoteLevel.values().firstOrNull { it.name == map["level"] as? String }
                    ?: NoteLevel.ITEM
            )
        }
    }
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
        "id" to id,
        "label" to label,
        "value" to value,
        "isDefault" to isDefault,
        "color" to color
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): WidgetOption = WidgetOption(
            id = map["id"] as? String ?: UUID.randomUUID().toString(),
            label = map["label"] as? String ?: "",
            value = map["value"] as? String ?: "",
            isDefault = map["isDefault"] as? Boolean ?: false,
            color = map["color"] as? String
        )
    }
}

/**
 * 4 widget types for pop-up editor
 */
enum class WidgetType(val displayName: String) {
    SINGLE_SELECT("Category"),
    MULTI_SELECT("Tags"),
    TEXT_BOX("Description"),
    CALENDAR("Due Date");

    companion object {
        @JvmStatic
        fun fromString(value: String?): WidgetType {
            if (value == null) return TEXT_BOX
            return WidgetType.values().firstOrNull { it.name == value } ?: TEXT_BOX
        }
    }
}
