package com.orderMate.utils

import android.content.Context
import com.orderMate.modals.MerchantTier

/**
 * Utility for checking merchant tier and feature whitelisting
 * #85: Implement Free Tier Whitelisting
 * 
 * Usage:
 * - TierChecker.isFeatureAllowed(context, "advanced_widgets") { allowed -> ... }
 * - TierChecker.isFreeTier(context) { isFree -> ... }
 */
object TierChecker {
    
    // Feature whitelist per tier
    // Format: "feature_name" to minimum required tier
    private val FEATURE_WHITELIST = mapOf(
        // All tiers
        "basic_widgets" to MerchantTier.FREE.name,
        "order_notes" to MerchantTier.FREE.name,
        "customer_contact" to MerchantTier.FREE.name,
        "simple_notifications" to MerchantTier.FREE.name,
        
        // Starter tier and above
        "calendar_view" to MerchantTier.STARTER.name,
        "custom_widgets" to MerchantTier.STARTER.name,
        "advanced_notifications" to MerchantTier.STARTER.name,
        
        // Professional tier and above
        "analytics" to MerchantTier.PROFESSIONAL.name,
        "bulk_operations" to MerchantTier.PROFESSIONAL.name,
        "priority_support" to MerchantTier.PROFESSIONAL.name,
        
        // Enterprise tier only
        "api_access" to MerchantTier.ENTERPRISE.name,
        "custom_integrations" to MerchantTier.ENTERPRISE.name,
        "dedicated_support" to MerchantTier.ENTERPRISE.name
    )
    
    /**
     * Check if a feature is allowed for the current merchant
     * @param context Android context
     * @param featureName The feature identifier (e.g., "calendar_view", "analytics")
     * @param callback Returns true if feature is allowed, false otherwise
     */
    fun isFeatureAllowed(context: Context, featureName: String, callback: (Boolean) -> Unit) {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId == null) {
            callback(false)
            return
        }
        
        FirebaseConfigManager.getInstance().getMerchantTier(merchantId) { tier ->
            val isAllowed = isFeatureAllowedForTier(featureName, tier)
            callback(isAllowed)
        }
    }
    
    /**
     * Check if a feature is allowed for a specific tier (synchronous)
     * @param featureName The feature identifier
     * @param tier The merchant tier string
     * @return true if the tier meets the minimum requirement for the feature
     */
    fun isFeatureAllowedForTier(featureName: String, tier: String): Boolean {
        val requiredTier = FEATURE_WHITELIST[featureName] ?: return true // Unknown features are allowed
        val requiredPriority = MerchantTier.getTierPriority(requiredTier)
        val currentPriority = MerchantTier.getTierPriority(tier)
        return currentPriority >= requiredPriority
    }
    
    /**
     * Check if the current merchant is on the free tier
     * @param context Android context
     * @param callback Returns true if free tier, false otherwise
     */
    fun isFreeTier(context: Context, callback: (Boolean) -> Unit) {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId == null) {
            callback(true) // Default to free tier if no merchant ID
            return
        }
        
        FirebaseConfigManager.getInstance().getMerchantTier(merchantId) { tier ->
            callback(tier.equals(MerchantTier.FREE.name, ignoreCase = true))
        }
    }
    
    /**
     * Get the current merchant tier
     * @param context Android context
     * @param callback Returns the tier string
     */
    fun getCurrentTier(context: Context, callback: (String) -> Unit) {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId == null) {
            callback(MerchantTier.DEFAULT_TIER)
            return
        }
        
        FirebaseConfigManager.getInstance().getMerchantTier(merchantId, callback)
    }
    
    /**
     * Check if merchant has access to a tier or higher
     * @param context Android context
     * @param requiredTier Minimum required tier
     * @param callback Returns true if merchant meets requirement
     */
    fun hasTierAccess(context: Context, requiredTier: String, callback: (Boolean) -> Unit) {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId == null) {
            callback(false)
            return
        }
        
        FirebaseConfigManager.getInstance().getMerchantTier(merchantId) { tier ->
            val requiredPriority = MerchantTier.getTierPriority(requiredTier)
            val currentPriority = MerchantTier.getTierPriority(tier)
            callback(currentPriority >= requiredPriority)
        }
    }
    
    /**
     * Get list of features blocked for current tier
     * @param context Android context
     * @param callback Returns list of blocked feature names
     */
    fun getBlockedFeatures(context: Context, callback: (List<String>) -> Unit) {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId == null) {
            callback(FEATURE_WHITELIST.keys.toList())
            return
        }
        
        FirebaseConfigManager.getInstance().getMerchantTier(merchantId) { tier ->
            val blockedFeatures = FEATURE_WHITELIST.filter { (feature, requiredTier) ->
                !isFeatureAllowedForTier(feature, tier)
            }.keys.toList()
            callback(blockedFeatures)
        }
    }
    
    /**
     * Get all features with their tier requirements
     */
    fun getFeatureRequirements(): Map<String, String> = FEATURE_WHITELIST
}
