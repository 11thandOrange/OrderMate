package com.orderMate.modals

/**
 * Event types for merchant lifecycle tracking
 * Issue #97/#98: Track lifecycle events for webhooks and reporting
 */
enum class EventType {
    INSTALL,                    // App installed
    UNINSTALL,                  // App uninstalled
    SUBSCRIPTION_UPGRADE,       // Plan upgraded
    SUBSCRIPTION_DOWNGRADE,     // Plan downgraded
    PAYMENT_RECEIVED,           // Payment received
    PAYMENT_LATE,               // Payment overdue
    REFUND,                     // Refund processed
    DISCOUNT_APPLIED,           // Discount/promo code used
    USAGE_TIER_BREAK            // Usage exceeded tier limit
}

/**
 * Merchant lifecycle event record
 * Path: merchants/{merchantId}/events/{eventId}
 * 
 * Issue #97: Store lifecycle events for analytics
 * Issue #98: Events created by webhooks
 */
data class MerchantEvent(
    val id: String = "",
    val type: EventType = EventType.INSTALL,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any?> = emptyMap(),
    val processed: Boolean = false       // Has email been sent?
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "timestamp" to timestamp,
        "details" to details,
        "processed" to processed
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): MerchantEvent? {
            if (map == null) return null
            return MerchantEvent(
                id = map["id"] as? String ?: "",
                type = try {
                    EventType.valueOf(map["type"] as? String ?: "INSTALL")
                } catch (e: Exception) {
                    EventType.INSTALL
                },
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0,
                details = (map["details"] as? Map<String, Any?>) ?: emptyMap(),
                processed = map["processed"] as? Boolean ?: false
            )
        }

        fun generateId(): String = java.util.UUID.randomUUID().toString()

        /**
         * Create an install event
         */
        fun createInstallEvent(): MerchantEvent = MerchantEvent(
            id = generateId(),
            type = EventType.INSTALL,
            details = mapOf("source" to "webhook")
        )

        /**
         * Create an uninstall event
         */
        fun createUninstallEvent(reason: String? = null): MerchantEvent = MerchantEvent(
            id = generateId(),
            type = EventType.UNINSTALL,
            details = mapOf("reason" to reason)
        )

        /**
         * Create a subscription change event
         */
        fun createSubscriptionEvent(
            oldPlan: String,
            newPlan: String
        ): MerchantEvent {
            val isUpgrade = SubscriptionInfo.isUpgrade(oldPlan, newPlan)
            return MerchantEvent(
                id = generateId(),
                type = if (isUpgrade) EventType.SUBSCRIPTION_UPGRADE else EventType.SUBSCRIPTION_DOWNGRADE,
                details = mapOf(
                    "oldPlan" to oldPlan,
                    "newPlan" to newPlan
                )
            )
        }

        /**
         * Create a refund event
         */
        fun createRefundEvent(amount: Double, reason: String? = null): MerchantEvent = MerchantEvent(
            id = generateId(),
            type = EventType.REFUND,
            details = mapOf(
                "amount" to amount,
                "reason" to reason
            )
        )

        /**
         * Create a discount applied event
         */
        fun createDiscountEvent(
            discountCode: String,
            amount: Double
        ): MerchantEvent = MerchantEvent(
            id = generateId(),
            type = EventType.DISCOUNT_APPLIED,
            details = mapOf(
                "discountCode" to discountCode,
                "amount" to amount
            )
        )
    }
}
