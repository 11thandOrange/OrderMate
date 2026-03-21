package com.orderMate.utils

import com.clover.sdk.v3.order.Order
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import java.util.Date

/**
 * Builds filter categories from Clover orders + OrderMate widgets
 * 
 * Filter Modal displays:
 * 1. Clover filters (Payment Status, Order Status, Payment Type, Employee)
 *    - Options extracted from actual order data at runtime
 * 2. OrderMate widget filters (where isEnabled=true and showInFilter=true)
 *    - Options from widget configuration in DB
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
        val icon: Int? = null              // Optional icon resource
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
    const val CLOVER_ORDER_DATE = "clover_order_date"  // Always shown, not editable
    const val CLOVER_PAYMENT_STATUS = "clover_payment_status"
    const val CLOVER_ORDER_STATUS = "clover_order_status"
    const val CLOVER_PAYMENT_TYPE = "clover_payment_type"
    const val CLOVER_EMPLOYEE = "clover_employee"
    
    // Widget filter ID prefix
    const val WIDGET_PREFIX = "widget_"
    
    /**
     * Build all filter categories for the filter modal
     * 
     * @param orders List of orders to extract Clover filter options from
     * @param widgets List of widgets from OrderMate DB
     * @return List of filter categories to display
     */
    fun buildCategories(
        orders: List<Order?>,
        widgets: List<WidgetConfig>
    ): List<FilterCategory> {
        val categories = mutableListOf<FilterCategory>()
        
        // 1. Order Date - ALWAYS shown (Clover data, not user-editable widget)
        categories.add(buildOrderDateFilter())
        
        // 2. Clover filters (shown if they have options from order data)
        val paymentStatusFilter = buildPaymentStatusFilter(orders)
        if (paymentStatusFilter.options.isNotEmpty()) {
            categories.add(paymentStatusFilter)
        }
        
        val orderStatusFilter = buildOrderStatusFilter(orders)
        if (orderStatusFilter.options.isNotEmpty()) {
            categories.add(orderStatusFilter)
        }
        
        val paymentTypeFilter = buildPaymentTypeFilter(orders)
        if (paymentTypeFilter.options.isNotEmpty()) {
            categories.add(paymentTypeFilter)
        }
        
        val employeeFilter = buildEmployeeFilter(orders)
        if (employeeFilter.options.isNotEmpty()) {
            categories.add(employeeFilter)
        }
        
        // 3. ALL enabled OrderMate widgets (not TEXT_BOX - can't filter text)
        widgets
            .filter { it.isEnabled && it.type != WidgetType.TEXT_BOX }
            .sortedBy { it.order }
            .forEach { widget ->
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
     * Build Payment Status filter from Clover order data
     */
    private fun buildPaymentStatusFilter(orders: List<Order?>): FilterCategory {
        val options = orders
            .mapNotNull { it?.paymentState?.name }
            .distinct()
            .map { status ->
                FilterOption(
                    id = status.lowercase(),
                    label = formatPaymentStatus(status),
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
     * Build Order Status filter from Clover order data
     */
    private fun buildOrderStatusFilter(orders: List<Order?>): FilterCategory {
        val options = orders
            .mapNotNull { it?.state }
            .distinct()
            .map { status ->
                FilterOption(
                    id = status.lowercase(),
                    label = formatOrderStatus(status),
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
     * Build Payment Type filter from Clover payment tender data
     */
    private fun buildPaymentTypeFilter(orders: List<Order?>): FilterCategory {
        val options = orders
            .flatMap { it?.payments ?: emptyList() }
            .mapNotNull { it?.tender?.label }
            .distinct()
            .map { tenderLabel ->
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
        
        return FilterCategory(
            id = "$WIDGET_PREFIX${widget.id}",
            label = widget.label,
            type = filterType,
            source = FilterSource.ORDERMATE,
            options = options
        )
    }
    
    // ==================== Formatters ====================
    
    /**
     * Format Clover payment status for display
     */
    private fun formatPaymentStatus(status: String): String {
        return when (status.uppercase()) {
            "PAID" -> "Paid"
            "NOT_PAID" -> "Unpaid"
            "PARTIALLY_PAID" -> "Partial"
            "REFUNDED" -> "Refunded"
            "PARTIALLY_REFUNDED" -> "Partial Refund"
            "OPEN" -> "Open"
            else -> status.lowercase()
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
    
    /**
     * Format Clover order status for display
     */
    private fun formatOrderStatus(status: String): String {
        return when (status.lowercase()) {
            "open" -> "Open"
            "locked" -> "Closed"
            else -> status.lowercase()
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
    
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
     */
    fun getWidgetId(categoryId: String): String? {
        return if (isWidgetFilter(categoryId)) {
            categoryId.removePrefix(WIDGET_PREFIX)
        } else {
            null
        }
    }
}
