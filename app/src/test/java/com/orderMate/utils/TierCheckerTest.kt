package com.orderMate.utils

import com.orderMate.modals.MerchantTier
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TierChecker and MerchantTier (#85)
 * 
 * Tests cover:
 * - MerchantTier enum values and properties
 * - Tier priority calculations
 * - Feature whitelist logic
 * - Tier comparison
 */
class TierCheckerTest {

    // ==================== MerchantTier Enum Tests ====================

    @Test
    fun `FREE tier has correct properties`() {
        val freeTier = MerchantTier.FREE
        
        assertEquals("Free", freeTier.displayName)
        assertTrue(freeTier.isFreeTier)
    }

    @Test
    fun `STARTER tier is not free tier`() {
        val starterTier = MerchantTier.STARTER
        
        assertEquals("Starter", starterTier.displayName)
        assertFalse(starterTier.isFreeTier)
    }

    @Test
    fun `PROFESSIONAL tier is not free tier`() {
        val proTier = MerchantTier.PROFESSIONAL
        
        assertEquals("Professional", proTier.displayName)
        assertFalse(proTier.isFreeTier)
    }

    @Test
    fun `ENTERPRISE tier is not free tier`() {
        val enterpriseTier = MerchantTier.ENTERPRISE
        
        assertEquals("Enterprise", enterpriseTier.displayName)
        assertFalse(enterpriseTier.isFreeTier)
    }

    @Test
    fun `all tier values are present`() {
        val expectedTiers = listOf("FREE", "STARTER", "PROFESSIONAL", "ENTERPRISE")
        val actualTiers = MerchantTier.values().map { it.name }
        
        assertEquals(expectedTiers, actualTiers)
    }

    // ==================== Tier Priority Tests ====================

    @Test
    fun `FREE tier has priority 0`() {
        assertEquals(0, MerchantTier.getTierPriority("FREE"))
    }

    @Test
    fun `STARTER tier has priority 1`() {
        assertEquals(1, MerchantTier.getTierPriority("STARTER"))
    }

    @Test
    fun `PROFESSIONAL tier has priority 2`() {
        assertEquals(2, MerchantTier.getTierPriority("PROFESSIONAL"))
    }

    @Test
    fun `ENTERPRISE tier has priority 3`() {
        assertEquals(3, MerchantTier.getTierPriority("ENTERPRISE"))
    }

    @Test
    fun `unknown tier defaults to priority 0`() {
        assertEquals(0, MerchantTier.getTierPriority("UNKNOWN"))
        assertEquals(0, MerchantTier.getTierPriority(""))
        assertEquals(0, MerchantTier.getTierPriority("invalid"))
    }

    @Test
    fun `tier priority is case insensitive`() {
        assertEquals(0, MerchantTier.getTierPriority("free"))
        assertEquals(1, MerchantTier.getTierPriority("starter"))
        assertEquals(2, MerchantTier.getTierPriority("professional"))
        assertEquals(3, MerchantTier.getTierPriority("enterprise"))
    }

    @Test
    fun `tier priorities are in ascending order`() {
        val freePriority = MerchantTier.getTierPriority("FREE")
        val starterPriority = MerchantTier.getTierPriority("STARTER")
        val proPriority = MerchantTier.getTierPriority("PROFESSIONAL")
        val enterprisePriority = MerchantTier.getTierPriority("ENTERPRISE")
        
        assertTrue(freePriority < starterPriority)
        assertTrue(starterPriority < proPriority)
        assertTrue(proPriority < enterprisePriority)
    }

    // ==================== DEFAULT_TIER Constant Tests ====================

    @Test
    fun `DEFAULT_TIER is FREE`() {
        assertEquals("FREE", MerchantTier.DEFAULT_TIER)
    }

    // ==================== TierChecker Feature Whitelist Tests ====================

    @Test
    fun `basic_widgets feature is available to all tiers`() {
        assertTrue(TierChecker.isFeatureAllowedForTier("basic_widgets", "FREE"))
        assertTrue(TierChecker.isFeatureAllowedForTier("basic_widgets", "STARTER"))
        assertTrue(TierChecker.isFeatureAllowedForTier("basic_widgets", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("basic_widgets", "ENTERPRISE"))
    }

    @Test
    fun `order_notes feature is available to all tiers`() {
        assertTrue(TierChecker.isFeatureAllowedForTier("order_notes", "FREE"))
        assertTrue(TierChecker.isFeatureAllowedForTier("order_notes", "STARTER"))
        assertTrue(TierChecker.isFeatureAllowedForTier("order_notes", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("order_notes", "ENTERPRISE"))
    }

    @Test
    fun `calendar_view feature requires STARTER tier`() {
        assertFalse(TierChecker.isFeatureAllowedForTier("calendar_view", "FREE"))
        assertTrue(TierChecker.isFeatureAllowedForTier("calendar_view", "STARTER"))
        assertTrue(TierChecker.isFeatureAllowedForTier("calendar_view", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("calendar_view", "ENTERPRISE"))
    }

    @Test
    fun `analytics feature requires PROFESSIONAL tier`() {
        assertFalse(TierChecker.isFeatureAllowedForTier("analytics", "FREE"))
        assertFalse(TierChecker.isFeatureAllowedForTier("analytics", "STARTER"))
        assertTrue(TierChecker.isFeatureAllowedForTier("analytics", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("analytics", "ENTERPRISE"))
    }

    @Test
    fun `api_access feature requires ENTERPRISE tier`() {
        assertFalse(TierChecker.isFeatureAllowedForTier("api_access", "FREE"))
        assertFalse(TierChecker.isFeatureAllowedForTier("api_access", "STARTER"))
        assertFalse(TierChecker.isFeatureAllowedForTier("api_access", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("api_access", "ENTERPRISE"))
    }

    @Test
    fun `unknown feature is allowed by default`() {
        assertTrue(TierChecker.isFeatureAllowedForTier("unknown_feature", "FREE"))
        assertTrue(TierChecker.isFeatureAllowedForTier("custom_feature", "FREE"))
        assertTrue(TierChecker.isFeatureAllowedForTier("", "FREE"))
    }

    // ==================== TierChecker getFeatureRequirements Tests ====================

    @Test
    fun `getFeatureRequirements returns all features`() {
        val requirements = TierChecker.getFeatureRequirements()
        
        assertFalse(requirements.isEmpty())
        assertTrue(requirements.containsKey("basic_widgets"))
        assertTrue(requirements.containsKey("calendar_view"))
        assertTrue(requirements.containsKey("analytics"))
        assertTrue(requirements.containsKey("api_access"))
    }

    @Test
    fun `getFeatureRequirements returns tier strings`() {
        val requirements = TierChecker.getFeatureRequirements()
        
        requirements.values.forEach { tier ->
            assertTrue(
                tier == "FREE" || tier == "STARTER" || 
                tier == "PROFESSIONAL" || tier == "ENTERPRISE"
            )
        }
    }

    // ==================== Tier Comparisons Tests ====================

    @Test
    fun `higher tier can access lower tier features`() {
        // Enterprise can access Starter features
        assertTrue(TierChecker.isFeatureAllowedForTier("calendar_view", "ENTERPRISE"))
        
        // Professional can access Free features
        assertTrue(TierChecker.isFeatureAllowedForTier("basic_widgets", "PROFESSIONAL"))
    }

    @Test
    fun `equal tier can access its own features`() {
        assertTrue(TierChecker.isFeatureAllowedForTier("calendar_view", "STARTER"))
        assertTrue(TierChecker.isFeatureAllowedForTier("analytics", "PROFESSIONAL"))
        assertTrue(TierChecker.isFeatureAllowedForTier("api_access", "ENTERPRISE"))
    }

    @Test
    fun `lower tier cannot access higher tier features`() {
        // Free cannot access Professional features
        assertFalse(TierChecker.isFeatureAllowedForTier("analytics", "FREE"))
        
        // Starter cannot access Enterprise features
        assertFalse(TierChecker.isFeatureAllowedForTier("api_access", "STARTER"))
    }
}
