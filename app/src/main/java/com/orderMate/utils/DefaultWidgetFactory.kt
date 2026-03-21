package com.orderMate.utils

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
     * Creates default widgets for new merchants
     * Each call generates new UUIDs
     */
    fun createDefaults(): List<WidgetConfig> = listOf(
        createWidget(
            type = WidgetType.CALENDAR,
            label = "Pickup Date",
            order = 0
        ),
        createWidget(
            type = WidgetType.SINGLE_SELECT,
            label = "Type",
            order = 1,
            options = listOf(
                "Pickup" to "Pickup",
                "Delivery" to "Delivery",
                "Preorder" to "Preorder"
            )
        ),
        createWidget(
            type = WidgetType.SINGLE_SELECT,
            label = "Status",
            order = 2,
            options = listOf(
                "In Progress" to "In Progress",
                "Ready" to "Ready",
                "Completed" to "Completed"
            )
        ),
        createWidget(
            type = WidgetType.MULTI_SELECT,
            label = "Category",
            order = 3,
            options = listOf(
                "Birthday" to "Birthday",
                "Wedding" to "Wedding",
                "Custom" to "Custom"
            )
        ),
        createWidget(
            type = WidgetType.MULTI_SELECT,
            label = "Sub-Category",
            order = 4,
            options = emptyList()
        ),
        createWidget(
            type = WidgetType.TEXT_BOX,
            label = "Description",
            showInFilter = false,
            order = 5
        )
    )
    
    /**
     * Create a widget with new UUID
     */
    fun createWidget(
        type: WidgetType,
        label: String,
        showInFilter: Boolean = type != WidgetType.TEXT_BOX,
        order: Int = 0,
        options: List<Pair<String, String>> = emptyList()
    ): WidgetConfig {
        return WidgetConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label,
            isEnabled = true,
            isRequired = false,
            showInFilter = showInFilter,
            order = order,
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
     * Create an empty widget of given type
     */
    fun createEmpty(type: WidgetType, order: Int): WidgetConfig {
        return WidgetConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            label = type.displayName,
            isEnabled = true,
            isRequired = false,
            showInFilter = type != WidgetType.TEXT_BOX,
            order = order,
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
