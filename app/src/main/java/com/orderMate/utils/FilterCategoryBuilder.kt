package com.orderMate.utils

import com.clover.sdk.v3.order.Order
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import java.util.Date

/**
 * Builds filter categories from Clover orders + OrderMate widgets
 * 
 * Filter Modal displays:
 * 1. Clover filters (Payment Status, Order Status, Payment Type, Employee)
 *    - Options extracted from actual order data at runtime
 * 2. Item-level widget filters (isEnabled=true, level=ITEM)
 * 3. Order-level widget filters (isEnabled=true, level=ORDER) (#93)
 */
object FilterCategoryBuilder {
    
    /**
     * A filter category/section in the filter modal
     */
    data class FilterCategory(
        val id: String,                    // Unique identifier
        val label: String,                 // Display label
        val type: FilterType,              // Type of filter UI
        val source: FilterSource,          // Where options come from
        val options: List<FilterOption>,   // Available options
        val icon: Int? = null,             // Optional icon resource
        val level: NoteLevel? = null       // ITEM or ORDER level for widgets (null for Clover filters)
    )
    
    /**
     * An option within a filter category
     */
    data class FilterOption(
        val id: String,        // Unique identifier
        val label: String,     // Display text
        val value: String      // Value used for filtering
    )
    
    /**
     * Type of filter UI to render
     */
    enum class FilterType {
        MULTI_SELECT,    // Chips that can be multi-selected
        DATE_PICKER      // Date selection
    }
    
    /**
     * Source of filter options
     */
    enum class FilterSource {
        CLOVER,      // Extracted from Clover order data
        ORDERMATE    // From OrderMate widget configuration
    }
    
    // Clover filter IDs (prefixed to avoid collision with widget IDs)
    const val CLOVER_ORDER_DATE = "clover_order_date"  // Order creation date
    const val CLOVER_DUE_DATE = "clover_due_date"      // Due/pickup date (#12)
    const val CLOVER_PAYMENT_STATUS = "clover_payment_status"
    const val CLOVER_ORDER_STATUS = "clover_order_status"
    const val CLOVER_PAYMENT_TYPE = "clover_payment_type"
    const val CLOVER_EMPLOYEE = "clover_employee"
    
    // Widget filter ID prefix
    const val WIDGET_PREFIX = "widget_"
    
    // Clover enum values (from Clover SDK)
    private val CLOVER_PAYMENT_STATUS_VALUES = listOf("OPEN", "PAID", "PARTIALLY_PAID", "REFUNDED", "PARTIALLY_REFUNDED", "CREDITED")
    private val CLOVER_ORDER_STATUS_VALUES = listOf("open", "locked")
    private val CLOVER_PAYMENT_TYPE_VALUES = listOf("Cash", "Credit Card", "Debit Card", "Check", "Gift Card", "External Gift Card", "Other")
    
    /**
     * Build all filter categories for the filter modal
     * 
     * @param orders List of orders to extract Clover filter options from
     * @param widgets List of widgets from OrderMate DB
     * @param settingsManager SettingsManager to check filter visibility settings
     * @return List of filter categories to display
     */
    fun buildCategories(
        orders: List<Order?>,
        widgets: List<WidgetConfig>,
        settingsManager: SettingsManager? = null
    ): List<FilterCategory> {
        val categories = mutableListOf<FilterCategory>()
        
        // 1. Order Date - check settings (default: shown)
        if (settingsManager?.getShowFilterOrderDate() != false) {
            categories.add(buildOrderDateFilter())
        }
        
        // 2. Clover filters - check settings for each (default: shown)
        if (settingsManager?.getShowFilterPaymentStatus() != false) {
            categories.add(buildPaymentStatusFilter())
        }
        if (settingsManager?.getShowFilterOrderStatus() != false) {
            categories.add(buildOrderStatusFilter())
        }
        if (settingsManager?.getShowFilterPaymentType() != false) {
            categories.add(buildPaymentTypeFilter())
        }
        
        // Employee filter - dynamic, extracted from orders (can't predefine employees)
        if (settingsManager?.getShowFilterEmployee() != false) {
            val employeeFilter = buildEmployeeFilter(orders)
            if (employeeFilter.options.isNotEmpty()) {
                categories.add(employeeFilter)
            }
        }
        
        // 3. Item-level widgets (ITEM level, enabled, showInFilter=true, not TEXT_BOX)
        val itemWidgets = widgets
            .filter { 
                it.isEnabled && 
                it.showInFilter && 
                it.type != WidgetType.TEXT_BOX && 
                it.level == NoteLevel.ITEM 
            }
            .sortedBy { it.order }
        
        itemWidgets.forEach { widget ->
            categories.add(buildWidgetFilter(widget))
        }
        
        // 4. Order-level widgets (ORDER level, enabled, showInFilter=true, not TEXT_BOX) (#93)
        val orderWidgets = widgets
            .filter { 
                it.isEnabled && 
                it.showInFilter && 
                it.type != WidgetType.TEXT_BOX && 
                it.level == NoteLevel.ORDER 
            }
            .sortedBy { it.order }
        
        orderWidgets.forEach { widget ->
            categories.add(buildWidgetFilter(widget))
        }
        
        return categories
    }
    
    /**
     * Build Order Date filter - always shown, from Clover data
     * This is NOT a widget users can edit in settings
     */
    private fun buildOrderDateFilter(): FilterCategory {
        return FilterCategory(
            id = CLOVER_ORDER_DATE,
            label = "Order Date",
            type = FilterType.DATE_PICKER,
            source = FilterSource.CLOVER,
            options = emptyList()  // Date picker doesn't use options
        )
    }
    
    /**
     * Build Payment Status filter using Clover's known values
     * (#81 QA) Uses title case for filter popup and pills
     */
    private fun buildPaymentStatusFilter(): FilterCategory {
        val options = CLOVER_PAYMENT_STATUS_VALUES.map { status ->
            FilterOption(
                id = status.lowercase(),
                label = formatPaymentStateTitleCase(status),
                value = status
            )
        }
        
        return FilterCategory(
            id = CLOVER_PAYMENT_STATUS,
            label = "Payment Status",
            type = FilterType.MULTI_SELECT,
            source = FilterSource.CLOVER,
            options = options
        )
    }
    
    /**
     * Build Order Status filter using Clover's known values
     * (#81 QA) Uses title case for filter popup and pills
     */
    private fun buildOrderStatusFilter(): FilterCategory {
        val options = CLOVER_ORDER_STATUS_VALUES.map { status ->
            FilterOption(
                id = status.lowercase(),
                label = formatOrderStateTitleCase(status),
                value = status
            )
        }
        
        return FilterCategory(
            id = CLOVER_ORDER_STATUS,
            label = "Order Status",
            type = FilterType.MULTI_SELECT,
            source = FilterSource.CLOVER,
            options = options
        )
    }
    
    /**
     * Build Payment Type filter using Clover's known tender types
     */
    private fun buildPaymentTypeFilter(): FilterCategory {
        val options = CLOVER_PAYMENT_TYPE_VALUES.map { tenderLabel ->
            FilterOption(
                id = tenderLabel.lowercase().replace(" ", "_"),
                label = tenderLabel,
                value = tenderLabel
            )
        }
        
        return FilterCategory(
            id = CLOVER_PAYMENT_TYPE,
            label = "Payment Type",
            type = FilterType.MULTI_SELECT,
            source = FilterSource.CLOVER,
            options = options
        )
    }
    
    /**
     * Build Employee filter from Clover order data
     */
    private fun buildEmployeeFilter(orders: List<Order?>): FilterCategory {
        val options = orders
            .mapNotNull { order ->
                try {
                    order?.employee?.jsonObject?.getString("name")
                } catch (e: Exception) {
                    null
                }
            }
            .distinct()
            .map { employeeName ->
                FilterOption(
                    id = employeeName.lowercase().replace(" ", "_"),
                    label = employeeName,
                    value = employeeName
                )
            }
        
        return FilterCategory(
            id = CLOVER_EMPLOYEE,
            label = "Employee",
            type = FilterType.MULTI_SELECT,
            source = FilterSource.CLOVER,
            options = options
        )
    }
    
    /**
     * Build filter from OrderMate widget configuration
     * Labels no longer include "Item: " or "Order: " prefix - sections are separated by dividers
     * 
     * @param widget The widget configuration
     */
    private fun buildWidgetFilter(widget: WidgetConfig): FilterCategory {
        val filterType = when (widget.type) {
            WidgetType.CALENDAR -> FilterType.DATE_PICKER
            else -> FilterType.MULTI_SELECT
        }
        
        val options = widget.options.map { opt ->
            FilterOption(
                id = opt.id,
                label = opt.label,
                value = opt.value
            )
        }
        
        // Include level in ID to differentiate item vs order level widgets
        val levelSuffix = if (widget.level == NoteLevel.ORDER) "_order" else "_item"
        
        return FilterCategory(
            id = "$WIDGET_PREFIX${widget.id}$levelSuffix",
            label = widget.label,  // No prefix - just the widget label
            type = filterType,
            source = FilterSource.ORDERMATE,
            options = options,
            level = widget.level   // Track level for section dividers
        )
    }
    
    // ==================== Formatters ====================
    
    // ==================== Helpers ====================
    
    /**
     * Check if a category ID is a Clover filter
     */
    fun isCloverFilter(categoryId: String): Boolean {
        return categoryId.startsWith("clover_")
    }
    
    /**
     * Check if a category ID is an OrderMate widget filter
     */
    fun isWidgetFilter(categoryId: String): Boolean {
        return categoryId.startsWith(WIDGET_PREFIX)
    }
    
    /**
     * Extract widget ID from filter category ID
     * Category ID format: widget_{widgetId}_item or widget_{widgetId}_order
     */
    fun getWidgetId(categoryId: String): String? {
        return if (isWidgetFilter(categoryId)) {
            categoryId
                .removePrefix(WIDGET_PREFIX)
                .removeSuffix("_item")
                .removeSuffix("_order")
        } else {
            null
        }
    }
}
