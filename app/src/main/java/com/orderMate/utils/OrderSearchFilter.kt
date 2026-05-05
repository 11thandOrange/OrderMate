package com.orderMate.utils

import com.clover.sdk.v3.order.Order
import com.orderMate.fragment.FilterDialogFragment
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Shared search and filter logic for List Tab and Calendar Tab
 * Eliminates duplicate code and ensures consistent behavior
 */
object OrderSearchFilter {

    /**
     * Check if an order matches the search query
     * Searches: orderId, customer name, employee name, payment status, 
     * customer contact, line item notes and names
     */
    fun matchesSearch(order: Order?, query: String): Boolean {
        if (order == null || query.isBlank()) return true

        val lowerQuery = query.lowercase()

        // Order ID
        val orderId = order.id?.lowercase() ?: ""
        if (orderId.contains(lowerQuery)) return true

        // Customer name (first + last)
        val customerName = try {
            val customer = order.customers?.firstOrNull()
            "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".lowercase()
        } catch (e: Exception) { "" }
        if (customerName.contains(lowerQuery)) return true

        // Employee name
        val employeeName = try {
            order.employee?.jsonObject?.get(Constants.name)?.toString()?.lowercase() ?: ""
        } catch (e: Exception) { "" }
        if (employeeName.contains(lowerQuery)) return true

        // Payment status
        val paymentStatus = order.paymentState?.name?.lowercase() ?: ""
        if (paymentStatus.contains(lowerQuery)) return true

        // Customer contact (email, phone)
        try {
            val customer = order.customers?.firstOrNull()
            val email = customer?.emailAddresses?.firstOrNull()?.emailAddress?.lowercase() ?: ""
            val phone = customer?.phoneNumbers?.firstOrNull()?.phoneNumber?.lowercase() ?: ""
            if (email.contains(lowerQuery) || phone.contains(lowerQuery)) return true
        } catch (e: Exception) { }

        // Order-level note (ORDER widgets like Pickup Date, Order Type, etc.)
        val orderNote = order.note?.lowercase() ?: ""
        if (orderNote.contains(lowerQuery)) return true

        // Line item notes and names
        val lineItemMatch = order.lineItems?.any { lineItem ->
            val note = lineItem?.note?.lowercase() ?: ""
            val itemName = lineItem?.getName()?.lowercase() ?: ""
            note.contains(lowerQuery) || itemName.contains(lowerQuery)
        } ?: false
        if (lineItemMatch) return true

        return false
    }

    /**
     * Parse dates from search query (e.g., "3/16, 3/18" -> list of dates)
     * Supports formats: 3/16, 03/16, 3-16, 3/16/24, 3/16/2024
     */
    fun parseSearchDates(searchQuery: String, contextYear: Int = Calendar.getInstance().get(Calendar.YEAR)): List<Date> {
        if (searchQuery.isBlank()) return emptyList()

        val dates = mutableListOf<Date>()
        val pattern = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?")
        val matcher = pattern.matcher(searchQuery)

        while (matcher.find()) {
            val month = matcher.group(1)?.toIntOrNull() ?: continue
            val day = matcher.group(2)?.toIntOrNull() ?: continue
            var year = matcher.group(3)?.toIntOrNull() ?: contextYear

            // Handle 2-digit years
            if (year < 100) {
                year += 2000
            }

            // Validate date
            if (month in 1..12 && day in 1..31) {
                val calendar = Calendar.getInstance().apply {
                    set(year, month - 1, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Verify the date is valid (e.g., Feb 30 would fail)
                if (calendar.get(Calendar.MONTH) == month - 1) {
                    dates.add(calendar.time)
                }
            }
        }

        return dates.sortedBy { it.time }
    }

    /**
     * Check if an order matches filter criteria
     */
    fun matchesFilters(order: Order?, filters: FilterDialogFragment.FilterState): Boolean {
        if (order == null) return false
        if (!filters.hasActiveFilters()) return true

        // Check selections (status, payment, etc.)
        // selections is Map<String, Set<String>> - category -> set of selected values
        for ((category, selectedValues) in filters.selections) {
            if (selectedValues.isEmpty()) continue
            if (!matchesAnySelection(order, category, selectedValues)) {
                return false
            }
        }

        // Check date selections
        // dateSelections is Map<String, List<Date>> - category -> list of selected dates
        for ((dateType, selectedDates) in filters.dateSelections) {
            if (selectedDates.isEmpty()) continue
            if (!matchesAnyDateSelection(order, dateType, selectedDates)) {
                return false
            }
        }

        return true
    }

    private fun matchesAnySelection(order: Order, category: String, selectedValues: Set<String>): Boolean {
        // Order must match at least one of the selected values
        return selectedValues.any { value ->
            matchesSelection(order, category, value)
        }
    }

    private fun matchesSelection(order: Order, category: String, selectedValue: String): Boolean {
        return when (category.lowercase()) {
            "payment status" -> matchesPaymentStatus(order, selectedValue)
            "order status" -> matchesOrderStatus(order, selectedValue)
            "payment type" -> matchesPaymentType(order, selectedValue)
            else -> matchesNoteCategory(order, category, selectedValue)
        }
    }

    private fun matchesPaymentStatus(order: Order, value: String): Boolean {
        val state = order.paymentState?.name?.uppercase()
        // If no payment state, order is unpaid - but we don't filter by "unpaid" anymore
        // Use order status (open/closed) filter instead
        if (state == null) return false
        
        return when (value.lowercase()) {
            "paid" -> state == "PAID"
            "refunded" -> state == "REFUNDED" || state == "PARTIALLY_REFUNDED"
            "partial", "partially paid" -> state == "PARTIALLY_PAID"
            "partially refunded" -> state == "PARTIALLY_REFUNDED"
            "credited" -> state == "CREDITED"
            else -> true
        }
    }

    private fun matchesOrderStatus(order: Order, value: String): Boolean {
        val state = order.state?.lowercase() ?: ""
        return state == value.lowercase() || value.lowercase() == "all"
    }

    private fun matchesPaymentType(order: Order, value: String): Boolean {
        val type = order.payType?.name?.lowercase() ?: ""
        return when (value.lowercase()) {
            "card", "credit card" -> type.contains("card") || type.contains("credit")
            "cash" -> type.contains("cash")
            else -> true
        }
    }

    private fun matchesNoteCategory(order: Order, category: String, value: String): Boolean {
        // Check line item notes for category/subcategory/progress matches
        return order.lineItems?.any { lineItem ->
            val note = lineItem?.note?.lowercase() ?: ""
            note.contains(value.lowercase().replace("-", " "))
        } ?: false
    }


    private fun matchesAnyDateSelection(order: Order, dateType: String, selectedDates: List<Date>): Boolean {
        // Order must match at least one of the selected dates
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        
        return when (dateType.lowercase()) {
            "orderdate", "order date" -> {
                val orderDate = order.createdTime?.let { dateFormat.format(Date(it)) } ?: return false
                selectedDates.any { date -> dateFormat.format(date) == orderDate }
            }
            "pickupdate", "pickup date" -> {
                val pickupDateStr = extractPickupDateFromNotes(order) ?: return false
                selectedDates.any { date -> 
                    val selectedStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    pickupDateStr == selectedStr 
                }
            }
            else -> true
        }
    }
    private fun matchesDateSelection(order: Order, dateType: String, dateValue: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        return when (dateType.lowercase()) {
            "orderdate", "order date" -> {
                val orderDate = order.createdTime?.let { 
                    dateFormat.format(Date(it)) 
                } ?: return false
                orderDate == dateValue
            }
            "pickupdate", "pickup date" -> {
                // Parse pickup date from line item notes
                val pickupDate = extractPickupDateFromNotes(order)
                pickupDate == dateValue
            }
            else -> true
        }
    }

    /**
     * Extract pickup date from line item notes
     * Looks for patterns like "pickup date: 03/15/24" or "pickup: 3/15/2024"
     */
    fun extractPickupDateFromNotes(order: Order): String? {
        val pattern = Pattern.compile("pickup\\s*(?:date)?\\s*:?\\s*(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2,4})", Pattern.CASE_INSENSITIVE)
        
        order.lineItems?.forEach { lineItem ->
            val note = lineItem?.note ?: return@forEach
            val matcher = pattern.matcher(note)
            if (matcher.find()) {
                val month = matcher.group(1)?.padStart(2, '0')
                val day = matcher.group(2)?.padStart(2, '0')
                var year = matcher.group(3)
                if (year?.length == 2) {
                    year = "20$year"
                }
                return "$year-$month-$day"
            }
        }
        return null
    }

    /**
     * Filter orders by a specific date
     */
    fun filterByDate(orders: List<Order?>, date: Date): List<Order?> {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val targetDate = dateFormat.format(date)

        return orders.filter { order ->
            val orderDate = order?.createdTime?.let { dateFormat.format(Date(it)) }
            orderDate == targetDate
        }
    }

    /**
     * Filter orders by multiple dates
     */
    fun filterByDates(orders: List<Order?>, dates: List<Date>): List<Order?> {
        if (dates.isEmpty()) return orders

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val targetDates = dates.map { dateFormat.format(it) }.toSet()

        return orders.filter { order ->
            val orderDate = order?.createdTime?.let { dateFormat.format(Date(it)) }
            orderDate in targetDates
        }
    }

    /**
     * Get customer contact details (email, phone)
     */
    fun getCustomerContactDetails(customer: com.clover.sdk.v3.customers.Customer?): Pair<String, String> {
        return try {
            val email = customer?.emailAddresses?.firstOrNull()?.emailAddress ?: ""
            val phone = customer?.phoneNumbers?.firstOrNull()?.phoneNumber ?: ""
            Pair(email, phone)
        } catch (e: Exception) {
            Pair("", "")
        }
    }
}
