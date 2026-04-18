package com.orderMate.utils

import com.clover.sdk.v3.order.Order
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * (#29) Resolves the due date for an order using widget-based lookup.
 * 
 * Three Priority Logic:
 * P1: Order-level due date → widgets where level=ORDER, type=CALENDAR → parse order.note
 * P2: Item-level due date → widgets where level=ITEM, type=CALENDAR → parse lineItem.note (earliest date)
 * P3: order.createdTime
 */
object OrderDueDateResolver {
    
    /**
     * (#29) Get the resolved due date using widget configuration.
     */
    fun resolveDueDate(order: Order, widgets: List<WidgetConfig>): Date? {
        // P1: Order-level CALENDAR widgets → parse order.note
        val orderDueDate = OrderNoteParser.extractDateFromNote(order.note, widgets, NoteLevel.ORDER)
        if (orderDueDate != null) return orderDueDate
        
        // P2: Item-level CALENDAR widgets → parse lineItem.note (earliest date)
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            OrderNoteParser.extractDateFromNote(lineItem?.note, widgets, NoteLevel.ITEM)
        }
        val earliestItemDueDate = itemDueDates?.minByOrNull { it.time }
        if (earliestItemDueDate != null) return earliestItemDueDate
        
        // P3: Fall back to order creation date
        return order.createdTime?.let { Date(it) }
    }
    
    /**
     * Legacy method - uses label-based parsing for backwards compatibility.
     */
    fun resolveDueDate(order: Order): Date? {
        val orderDueDate = extractDueDateFromNoteLegacy(order.note)
        if (orderDueDate != null) return orderDueDate
        
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            extractDueDateFromNoteLegacy(lineItem?.note)
        }
        val earliestItemDueDate = itemDueDates?.minByOrNull { it.time }
        if (earliestItemDueDate != null) return earliestItemDueDate
        
        return order.createdTime?.let { Date(it) }
    }
    
    /**
     * Get the due date source for display purposes.
     */
    fun getDueDateSource(order: Order, widgets: List<WidgetConfig>): DueDateSource {
        val orderDueDate = OrderNoteParser.extractDateFromNote(order.note, widgets, NoteLevel.ORDER)
        if (orderDueDate != null) return DueDateSource.ORDER_LEVEL
        
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            OrderNoteParser.extractDateFromNote(lineItem?.note, widgets, NoteLevel.ITEM)
        }
        if (itemDueDates?.isNotEmpty() == true) return DueDateSource.ITEM_LEVEL
        
        return DueDateSource.ORDER_CREATED
    }
    
    fun getDueDateSource(order: Order): DueDateSource {
        val orderDueDate = extractDueDateFromNoteLegacy(order.note)
        if (orderDueDate != null) return DueDateSource.ORDER_LEVEL
        
        val itemDueDates = order.lineItems?.mapNotNull { lineItem ->
            extractDueDateFromNoteLegacy(lineItem?.note)
        }
        if (itemDueDates?.isNotEmpty() == true) return DueDateSource.ITEM_LEVEL
        
        return DueDateSource.ORDER_CREATED
    }
    
    fun hasDueDate(order: Order, widgets: List<WidgetConfig>): Boolean {
        val orderDate = OrderNoteParser.extractDateFromNote(order.note, widgets, NoteLevel.ORDER)
        if (orderDate != null) return true
        return order.lineItems?.any { lineItem ->
            OrderNoteParser.extractDateFromNote(lineItem?.note, widgets, NoteLevel.ITEM) != null
        } == true
    }
    
    fun hasDueDate(order: Order): Boolean {
        return extractDueDateFromNoteLegacy(order.note) != null ||
               order.lineItems?.any { extractDueDateFromNoteLegacy(it?.note) != null } == true
    }
    
    private fun extractDueDateFromNoteLegacy(note: String?): Date? {
        if (note.isNullOrBlank()) return null
        
        val delimiter = if (note.contains("•")) "•" else "|"
        val parts = note.split(delimiter).map { it.trim() }
        
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val value = part.substring(colonIndex + 1).trim()
                
                if (label.contains("due") && label.contains("date") || 
                    label == "due date" || label == "pickup date" ||
                    label == "delivery date" || label == "event date") {
                    return OrderNoteParser.parseDate(value)
                }
            }
        }
        return null
    }
    
    fun formatDate(date: Date): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
    
    fun formatDateTime(date: Date): String {
        return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
    }
    
    fun isToday(date: Date): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        return today == dateStr
    }
    
    fun isPast(date: Date): Boolean {
        return date.time < System.currentTimeMillis()
    }
    
    enum class DueDateSource {
        ORDER_LEVEL,    // P1: From Order.note CALENDAR widget
        ITEM_LEVEL,     // P2: From LineItem.note CALENDAR widget  
        ORDER_CREATED   // P3: Fallback to Order.createdTime
    }
}
