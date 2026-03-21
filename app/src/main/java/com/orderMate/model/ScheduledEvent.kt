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
    val lineItems: List<LineItemPreview> = emptyList()
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
    val quantity: Int = 1
)

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
