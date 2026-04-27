package com.orderMate.utils

import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import java.util.UUID

/**
 * Factory for creating widget instances with unique IDs
 * All IDs are dynamically generated UUIDs - nothing is hardcoded
 */
object DefaultWidgetFactory {
    
    /**
     * Creates default widgets for new merchants (both item-level and order-level)
     * Each call generates new UUIDs
     * - Item-level widgets: enabled by default
     * - Order-level widgets: disabled by default
     */
    fun createDefaults(): List<WidgetConfig> = createItemLevelDefaults() + createOrderLevelDefaults()
    
    /**
     * Creates default item-level widgets (enabled by default)
     * WARNING: Each call generates NEW UUIDs - only call when truly creating new widgets!
     */
    fun createItemLevelDefaults(): List<WidgetConfig> {
        android.util.Log.w("WidgetFactoryDebug", "⚠️ createItemLevelDefaults() CALLED - GENERATING NEW UUIDs!")
        Thread.currentThread().stackTrace.take(10).forEach { 
            android.util.Log.d("WidgetFactoryDebug", "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
        }
        return listOf(
        createWidget(
            type = WidgetType.CALENDAR,
            label = "Due Date",
            level = NoteLevel.ITEM,
            isEnabled = true,
            order = 0
        ),
        createWidget(
            type = WidgetType.SINGLE_SELECT,
            label = "Category",
            level = NoteLevel.ITEM,
            isEnabled = true,
            order = 1,
            options = listOf(
                "Birthday" to "Birthday",
                "Wedding" to "Wedding",
                "Custom" to "Custom"
            )
        ),
        createWidget(
            type = WidgetType.MULTI_SELECT,
            label = "Tags",
            level = NoteLevel.ITEM,
            isEnabled = true,
            order = 2,
            options = listOf(
                "Rush" to "Rush",
                "VIP" to "VIP",
                "Delivery" to "Delivery"
            )
        ),
        createWidget(
            type = WidgetType.TEXT_BOX,
            label = "Description",
            level = NoteLevel.ITEM,
            isEnabled = true,
            showInFilter = false,
            order = 3
        )
    )
    }
    
    /**
     * Creates default order-level widgets (disabled by default)
     * WARNING: Each call generates NEW UUIDs - only call when truly creating new widgets!
     */
    fun createOrderLevelDefaults(): List<WidgetConfig> {
        android.util.Log.w("WidgetFactoryDebug", "⚠️ createOrderLevelDefaults() CALLED - GENERATING NEW UUIDs!")
        Thread.currentThread().stackTrace.take(10).forEach { 
            android.util.Log.d("WidgetFactoryDebug", "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
        }
        return listOf(
        createWidget(
            type = WidgetType.CALENDAR,
            label = "Deadline",
            level = NoteLevel.ORDER,
            isEnabled = false,
            order = 0
        ),
        createWidget(
            type = WidgetType.SINGLE_SELECT,
            label = "Group",
            level = NoteLevel.ORDER,
            isEnabled = false,
            order = 1,
            options = listOf(
                "Catering" to "Catering",
                "Retail" to "Retail",
                "Event" to "Event"
            )
        ),
        createWidget(
            type = WidgetType.MULTI_SELECT,
            label = "Order Tags",  // Unique label (ITEM level has "Tags")
            level = NoteLevel.ORDER,
            isEnabled = false,
            order = 2,
            options = listOf(
                "Priority" to "Priority",
                "Fragile" to "Fragile",
                "Gift" to "Gift"
            )
        ),
        createWidget(
            type = WidgetType.TEXT_BOX,
            label = "Details",
            level = NoteLevel.ORDER,
            isEnabled = false,
            showInFilter = false,
            order = 3
        )
    )
    }
    
    /**
     * Create a widget with new UUID
     */
    fun createWidget(
        type: WidgetType,
        label: String,
        level: NoteLevel = NoteLevel.ITEM,
        isEnabled: Boolean = true,
        showInFilter: Boolean = type != WidgetType.TEXT_BOX,
        order: Int = 0,
        options: List<Pair<String, String>> = emptyList()
    ): WidgetConfig {
        return WidgetConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label,
            isEnabled = isEnabled,
            isRequired = false,
            showInFilter = showInFilter,
            order = order,
            level = level,
            options = options.mapIndexed { index, (optLabel, optValue) ->
                WidgetOption(
                    id = UUID.randomUUID().toString(),
                    label = optLabel,
                    value = optValue,
                    isDefault = index == 0
                )
            }.toMutableList()
        )
    }
    
    /**
     * Create an empty widget of given type and level
     */
    fun createEmpty(type: WidgetType, order: Int, level: NoteLevel = NoteLevel.ITEM): WidgetConfig {
        return WidgetConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            label = type.displayName,
            isEnabled = true,
            isRequired = false,
            showInFilter = type != WidgetType.TEXT_BOX,
            order = order,
            level = level,
            options = mutableListOf()
        )
    }
    
    /**
     * Create a new option with UUID
     */
    fun createOption(label: String, value: String = label): WidgetOption {
        return WidgetOption(
            id = UUID.randomUUID().toString(),
            label = label,
            value = value,
            isDefault = false
        )
    }
}
