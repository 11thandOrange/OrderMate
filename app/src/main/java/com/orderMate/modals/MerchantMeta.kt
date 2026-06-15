package com.orderMate.modals

/**
 * Merchant tier levels for feature whitelisting
 * Used in Firebase: merchants/{merchantId}/meta/tier
 */
enum class MerchantTier(val displayName: String, val isFreeTier: Boolean = false) {
    FREE("Free", isFreeTier = true),
    STARTER("Starter"),
    PROFESSIONAL("Professional"),
    ENTERPRISE("Enterprise");

    companion object {
        /**
         * Default tier for new merchants
         */
        const val DEFAULT_TIER = "FREE"
        
        /**
         * Tier priority for comparison (higher = more features)
         */
        fun getTierPriority(tier: String): Int {
            return when (tier.uppercase()) {
                "FREE" -> 0
                "STARTER" -> 1
                "PROFESSIONAL" -> 2
                "ENTERPRISE" -> 3
                else -> 0
            }
        }
    }
}

/**
 * Merchant metadata
 * Path: merchants/{merchantId}/meta
 */
data class MerchantMeta(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var tier: String = MerchantTier.DEFAULT_TIER
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
