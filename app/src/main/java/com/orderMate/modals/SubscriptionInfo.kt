package com.orderMate.modals

/**
 * Subscription information for a merchant
 * Path: merchants/{merchantId}/subscription
 * 
 * Issue #97: Track subscription plan and billing
 */
data class SubscriptionInfo(
    val plan: String = PLAN_FREE,           // Current plan
    val status: String = STATUS_ACTIVE,     // Subscription status
    val monthlyDueDate: Int = 1             // Day of month payment is due (1-28)
) {
    fun toMap(): Map<String, Any> = mapOf(
        "plan" to plan,
        "status" to status,
        "monthlyDueDate" to monthlyDueDate
    )

    fun isActive(): Boolean = status == STATUS_ACTIVE

    fun isPaid(): Boolean = plan != PLAN_FREE

    companion object {
        // Plan types
        const val PLAN_FREE = "free"
        const val PLAN_BASIC = "basic"
        const val PLAN_PREMIUM = "premium"

        // Status types
        const val STATUS_ACTIVE = "active"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_PAST_DUE = "past_due"

        fun fromMap(map: Map<String, Any?>?): SubscriptionInfo {
            if (map == null) return SubscriptionInfo()
            return SubscriptionInfo(
                plan = map["plan"] as? String ?: PLAN_FREE,
                status = map["status"] as? String ?: STATUS_ACTIVE,
                monthlyDueDate = (map["monthlyDueDate"] as? Number)?.toInt() ?: 1
            )
        }

        /**
         * Check if newPlan is an upgrade from oldPlan
         */
        fun isUpgrade(oldPlan: String, newPlan: String): Boolean {
            val planRanking = mapOf(
                PLAN_FREE to 0,
                PLAN_BASIC to 1,
                PLAN_PREMIUM to 2
            )
            return (planRanking[newPlan] ?: 0) > (planRanking[oldPlan] ?: 0)
        }
    }
}

/**
 * Billing record for tracking payments
 * Path: merchants/{merchantId}/subscription/billingHistory/{paymentId}
 * 
 * Issue #97: Track monthly payments and late payments
 */
data class BillingRecord(
    val id: String = "",
    val amount: Double = 0.0,
    val dueDate: Long = 0,              // When payment was due
    val paidDate: Long? = null,         // When payment was made (null if unpaid)
    val status: String = STATUS_PENDING,
    val lateDays: Int = 0               // Days late (0 if on time)
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "amount" to amount,
        "dueDate" to dueDate,
        "paidDate" to paidDate,
        "status" to status,
        "lateDays" to lateDays
    )

    fun isPaid(): Boolean = status == STATUS_PAID

    fun isLate(): Boolean = lateDays > 0

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_PAID = "paid"
        const val STATUS_LATE = "late"
        const val STATUS_MISSED = "missed"

        fun fromMap(map: Map<String, Any?>?): BillingRecord? {
            if (map == null) return null
            return BillingRecord(
                id = map["id"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                dueDate = (map["dueDate"] as? Number)?.toLong() ?: 0,
                paidDate = (map["paidDate"] as? Number)?.toLong(),
                status = map["status"] as? String ?: STATUS_PENDING,
                lateDays = (map["lateDays"] as? Number)?.toInt() ?: 0
            )
        }

        fun generateId(): String = java.util.UUID.randomUUID().toString()
    }
}
