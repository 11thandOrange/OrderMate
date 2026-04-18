package com.orderMate.utils

import com.clover.sdk.v3.order.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resolves the due date for an order (#93 requirement)
 * 
 * Priority:
 * 1. Order-level "Due Date" widget from Order.note
 * 2. Nearest item-level "Due Date" from LineItem.note
 * 3. Order creation date (Order.createdTime)
 */
object OrderDueDateResolver {
    
    private val dateFormats = listOf(
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    
    /**
     * Get the resolved due date for an order
     * Returns Date or null if no date could be determined
     */
    fun resolveDueDate(order: Order): Date? {
        // 1. Check order-level "Due Date" widget
        val orderDueDate = extractDueDateFromNote(order.note)
        if (orderDueDate != null) return orderDueDate
        
        // 2. Check item-level "Due Date" widgets - find the nearest one
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            extractDueDateFromNote(lineItem?.note)
        }?.filter { it.time >= System.currentTimeMillis() }
        
        val nearestItemDueDate = itemDueDates?.minByOrNull { it.time }
        if (nearestItemDueDate != null) return nearestItemDueDate
        
        // 3. Fall back to order creation date
        return order.createdTime?.let { Date(it) }
    }
    
    /**
     * Get the due date source for display purposes
     */
    fun getDueDateSource(order: Order): DueDateSource {
        val orderDueDate = extractDueDateFromNote(order.note)
        if (orderDueDate != null) return DueDateSource.ORDER_LEVEL
        
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            extractDueDateFromNote(lineItem?.note)
        }?.filter { it.time >= System.currentTimeMillis() }
        
        if (itemDueDates?.isNotEmpty() == true) return DueDateSource.ITEM_LEVEL
        
        return DueDateSource.ORDER_CREATED
    }
    
    /**
     * Extract due date from a note string
     * Looks for "Due Date:value" pattern
     */
    private fun extractDueDateFromNote(note: String?): Date? {
        if (note.isNullOrBlank()) return null
        
        val delimiter = if (note.contains("•")) "•" else "|"
        val parts = note.split(delimiter).map { it.trim() }
        
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val value = part.substring(colonIndex + 1).trim()
                
                // Check for due date labels
                if (label.contains("due") && label.contains("date") || 
                    label == "due date" || 
                    label == "pickup date" ||
                    label == "delivery date" ||
                    label == "event date") {
                    return parseDate(value)
                }
            }
        }
        
        return null
    }
    
    /**
     * Parse a date string using multiple formats
     */
    private fun parseDate(dateString: String): Date? {
        for (format in dateFormats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
    
    /**
     * Check if an order has a due date from any source
     */
    fun hasDueDate(order: Order): Boolean {
        return extractDueDateFromNote(order.note) != null ||
               order.lineItems?.any { extractDueDateFromNote(it?.note) != null } == true
    }
    
    /**
     * Format a date for display
     */
    fun formatDate(date: Date): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
    
    /**
     * Format a date with time for display
     */
    fun formatDateTime(date: Date): String {
        return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
    }
    
    /**
     * Check if a date is today
     */
    fun isToday(date: Date): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        return today == dateStr
    }
    
    /**
     * Check if a date is in the past
     */
    fun isPast(date: Date): Boolean {
        return date.time < System.currentTimeMillis()
    }
    
    enum class DueDateSource {
        ORDER_LEVEL,    // From Order.note "Due Date" widget
        ITEM_LEVEL,     // From LineItem.note "Due Date" widget  
        ORDER_CREATED   // Fallback to Order.createdTime
    }
}
