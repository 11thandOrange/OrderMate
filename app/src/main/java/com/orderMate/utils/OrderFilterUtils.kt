package com.orderMate.utils

import android.content.Context
import com.clover.sdk.v3.order.Order
import com.orderMate.fragment.FilterDialogFragment
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared filter utilities for order matching.
 * Single source of truth used by both OrderListRedesignFragment and CalendarFragment.
 * 
 * Note: Calendar events still render on due date using OrderDueDateResolver's 3-tier priority.
 * This class only handles filter MATCHING, not event date resolution.
 */
object OrderFilterUtils {

    /**
     * Check if an order matches the given filter state.
     * Used by both List and Calendar pages for consistent filtering.
     *
     * @param order The order to check
     * @param filters The current filter state from FilterDialogFragment
     * @param context Context for accessing WidgetManager
     * @return true if order matches all active filters
     */
    fun orderMatchesFilters(
        order: Order?,
        filters: FilterDialogFragment.FilterState,
        context: Context
    ): Boolean {
        if (order == null) return false

        // Check selection filters (multi-select chips)
        for ((categoryId, selectedValues) in filters.selections) {
            if (selectedValues.isEmpty()) continue
            
            when (categoryId) {
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS -> {
                    // Use shared function for correct fallback to "OPEN"
                    // Filter values: "OPEN", "PAID", "PARTIALLY_PAID", etc.
                    val orderPayment = getPaymentStateFromOrder(order)
                    if (!selectedValues.contains(orderPayment)) return false
                }
                FilterCategoryBuilder.CLOVER_ORDER_STATUS -> {
                    // Filter values: "open", "locked" (lowercase)
                    // order.state is lowercase ("open", "locked")
                    val orderState = order.state?.lowercase() ?: "open"
                    if (!selectedValues.any { it.lowercase() == orderState }) return false
                }
                FilterCategoryBuilder.CLOVER_PAYMENT_TYPE -> {
                    val paymentTypes = order.payments?.mapNotNull { 
                        it?.tender?.label?.lowercase() 
                    } ?: emptyList()
                    val selectedLower = selectedValues.map { it.lowercase() }
                    if (!paymentTypes.any { it in selectedLower }) return false
                }
                FilterCategoryBuilder.CLOVER_EMPLOYEE -> {
                    val employeeName = try {
                        order.employee?.jsonObject?.getString("name") ?: ""
                    } catch (e: Exception) { "" }
                    if (!selectedValues.contains(employeeName)) return false
                }
                else -> {
                    // OrderMate widget filters
                    if (FilterCategoryBuilder.isWidgetFilter(categoryId)) {
                        val widgetId = FilterCategoryBuilder.getWidgetId(categoryId) ?: continue
                        val widget = WidgetManager.getInstance(context).getWidgetById(widgetId) ?: continue
                        val orderValues = extractWidgetValues(order, widget)
                        if (!selectedValues.any { it in orderValues }) return false
                    }
                }
            }
        }

        // Check date filters (date picker selections)
        for ((categoryId, dates) in filters.dateSelections) {
            if (dates.isEmpty()) continue
            
            when (categoryId) {
                FilterCategoryBuilder.CLOVER_ORDER_DATE -> {
                    // Filter by order creation date
                    val orderDate = order.createdTime?.let { Date(it) } ?: return false
                    if (!dates.any { isSameDay(orderDate, it) }) return false
                }
                else -> {
                    // Widget date filters (e.g., Pickup Date, Delivery Date)
                    if (FilterCategoryBuilder.isWidgetFilter(categoryId)) {
                        val widgetId = FilterCategoryBuilder.getWidgetId(categoryId) ?: continue
                        val widget = WidgetManager.getInstance(context).getWidgetById(widgetId) ?: continue
                        val orderDateValues = extractWidgetValues(order, widget)
                        
                        // Match date format used in notes (M/d/yy)
                        val dateFormat = SimpleDateFormat("M/d/yy", Locale.getDefault())
                        val matchesAny = dates.any { filterDate ->
                            val dateStr = dateFormat.format(filterDate)
                            orderDateValues.any { it.contains(dateStr) }
                        }
                        if (!matchesAny) return false
                    }
                }
            }
        }

        return true
    }

    /**
     * Check if an order matches a search query.
     * Delegates to OrderSearchFilter for consistent search behavior.
     */
    fun orderMatchesSearch(order: Order?, query: String): Boolean {
        if (query.isEmpty()) return true
        return OrderSearchFilter.matchesSearch(order, query)
    }

    /**
     * Extract widget values from an order based on widget level.
     */
    private fun extractWidgetValues(order: Order, widget: WidgetConfig): Set<String> {
        return if (widget.level == NoteLevel.ORDER) {
            extractValuesFromNote(order.note, widget.id)
        } else {
            // ITEM level - collect from all line items
            order.lineItems?.flatMap { lineItem ->
                extractValuesFromNote(lineItem?.note, widget.id)
            }?.toSet() ?: emptySet()
        }
    }

    /**
     * Extract values for a specific widget from a note string.
     * Note format: "[widgetId]Label: Value • [widgetId2]Label2: Value2"
     */
    private fun extractValuesFromNote(note: String?, widgetId: String): Set<String> {
        if (note.isNullOrBlank()) return emptySet()
        
        val values = mutableSetOf<String>()
        // Match by widget ID in format [widgetId]label:value
        val pattern = "\\[$widgetId\\][^:]*:([^•|]+)".toRegex()
        
        pattern.findAll(note).forEach { match ->
            val value = match.groupValues[1].trim()
            if (value.isNotEmpty()) {
                // Handle multi-select comma-separated values
                value.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { values.add(it) }
            }
        }
        return values
    }

    /**
     * Check if two dates are the same day (ignoring time).
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
