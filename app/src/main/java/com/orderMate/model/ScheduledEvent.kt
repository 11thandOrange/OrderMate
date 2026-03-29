package com.orderMate.model

import java.util.Date

/**
 * Scheduled Event model (Issue #82)
 * 
 * Represents a scheduled order event for the calendar
 */
data class ScheduledEvent(
    val id: Long,
    val orderId: String,
    val customerName: String,
    val type: EventType,
    val dueDate: Date,
    val total: Double,
    val itemCount: Int,
    val note: String? = null,
    val gmailEventId: String? = null,
    val notificationScheduled: Boolean = false,
    val lineItems: List<LineItemPreview> = emptyList(),
    val orderNote: String? = null  // Order-level note from Order.note (#93)
) {
    // For backwards compatibility
    val lineItemNames: List<String>
        get() = lineItems.map { it.name }
}

/**
 * Line item preview for event dialog
 */
data class LineItemPreview(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val customNotes: List<CustomNote> = emptyList()
)

/**
 * Custom note for line item (matches HTML structure)
 * Types: "select" (category), "multiselect" (tags), "text" (description)
 */
data class CustomNote(
    val type: String,  // "select", "multiselect", "text"
    val value: Any     // String for select/text, List<String> for multiselect
) {
    fun getStringValue(): String? = value as? String
    
    @Suppress("UNCHECKED_CAST")
    fun getListValue(): List<String> = (value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
}

/**
 * Event type enum for calendar events
 */
enum class EventType {
    PICKUP,
    DELIVERY,
    PREORDER;
    
    fun getDisplayName(): String {
        return when (this) {
            PICKUP -> "Pickup"
            DELIVERY -> "Delivery"
            PREORDER -> "Preorder"
        }
    }
    
    fun getColorHex(): String {
        return when (this) {
            PICKUP -> "#4CAF50"    // Green
            DELIVERY -> "#2196F3"  // Blue
            PREORDER -> "#9C27B0"  // Purple
        }
    }
}
