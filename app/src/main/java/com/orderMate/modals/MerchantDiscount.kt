package com.orderMate.modals

import java.util.UUID

/**
 * Merchant discount information (#81)
 * Stored at: merchants/{merchantId}/discounts/{discountId}/
 * 
 * Admin-only: Discounts are added via Postman or Firebase Console
 * App has read-only access to discounts
 */
data class MerchantDiscount(
    val id: String = "",
    val amount: Double = 0.0,           // Discount amount in dollars
    val startDate: Long = 0,            // When discount becomes active (Unix ms)
    val endDate: Long = 0,              // When discount expires (Unix ms)
    val discountCode: String? = null,   // Optional code name (e.g., "STRIPE50")
    val createdAt: Long = 0,            // When record was created
    val isActive: Boolean = true        // Manual on/off toggle
) {
    companion object {
        fun generateId(): String = "disc_${UUID.randomUUID().toString().take(8)}"
    }
    
    /** Check if discount period has started */
    fun hasStarted(): Boolean = System.currentTimeMillis() >= startDate
    
    /** Check if discount has expired */
    fun hasExpired(): Boolean = System.currentTimeMillis() > endDate
    
    /** Check if discount is currently valid and applicable */
    fun isValid(): Boolean = isActive && hasStarted() && !hasExpired() && amount > 0
    
    /** Get current status of the discount */
    fun status(): DiscountStatus = when {
        !isActive -> DiscountStatus.INACTIVE
        !hasStarted() -> DiscountStatus.SCHEDULED
        hasExpired() -> DiscountStatus.EXPIRED
        else -> DiscountStatus.ACTIVE
    }
    
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "amount" to amount,
        "startDate" to startDate,
        "endDate" to endDate,
        "discountCode" to discountCode,
        "createdAt" to createdAt,
        "isActive" to isActive
    )
}

/**
 * Discount status enum
 */
enum class DiscountStatus {
    SCHEDULED,   // Future discount (startDate not reached)
    ACTIVE,      // Currently valid
    EXPIRED,     // Past endDate
    INACTIVE     // Manually deactivated
}
