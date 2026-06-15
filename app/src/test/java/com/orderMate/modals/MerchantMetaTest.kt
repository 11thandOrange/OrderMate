package com.orderMate.modals

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MerchantMeta and MerchantTier (#85)
 * 
 * Tests cover:
 * - MerchantMeta data class with tier field
 * - Default tier value
 * - Schema version
 * - Data class equality
 * - MerchantTier enum values
 */
class MerchantMetaTest {

    // ==================== MerchantMeta Default Values Tests ====================

    @Test
    fun `default tier is FREE`() {
        val meta = MerchantMeta()
        
        assertEquals(MerchantTier.DEFAULT_TIER, meta.tier)
        assertEquals("FREE", meta.tier)
    }

    @Test
    fun `default schema version is 2`() {
        val meta = MerchantMeta()
        
        assertEquals(MerchantMeta.CURRENT_SCHEMA_VERSION, meta.schemaVersion)
        assertEquals(2, meta.schemaVersion)
    }

    @Test
    fun `default createdAt is current time`() {
        val before = System.currentTimeMillis()
        val meta = MerchantMeta()
        val after = System.currentTimeMillis()
        
        assertTrue(meta.createdAt in before..after)
    }

    @Test
    fun `default updatedAt is current time`() {
        val before = System.currentTimeMillis()
        val meta = MerchantMeta()
        val after = System.currentTimeMillis()
        
        assertTrue(meta.updatedAt in before..after)
    }

    // ==================== MerchantMeta Tier Field Tests ====================

    @Test
    fun `can set tier to STARTER`() {
        val meta = MerchantMeta(tier = MerchantTier.STARTER.name)
        
        assertEquals("STARTER", meta.tier)
    }

    @Test
    fun `can set tier to PROFESSIONAL`() {
        val meta = MerchantMeta(tier = MerchantTier.PROFESSIONAL.name)
        
        assertEquals("PROFESSIONAL", meta.tier)
    }

    @Test
    fun `can set tier to ENTERPRISE`() {
        val meta = MerchantMeta(tier = MerchantTier.ENTERPRISE.name)
        
        assertEquals("ENTERPRISE", meta.tier)
    }

    @Test
    fun `can set tier to FREE`() {
        val meta = MerchantMeta(tier = MerchantTier.FREE.name)
        
        assertEquals("FREE", meta.tier)
    }

    @Test
    fun `can set all meta fields together`() {
        val customCreatedAt = 1609459200000L // 2021-01-01
        val customUpdatedAt = 1609545600000L // 2021-01-02
        
        val meta = MerchantMeta(
            schemaVersion = 2,
            createdAt = customCreatedAt,
            updatedAt = customUpdatedAt,
            tier = "PROFESSIONAL"
        )
        
        assertEquals(2, meta.schemaVersion)
        assertEquals(customCreatedAt, meta.createdAt)
        assertEquals(customUpdatedAt, meta.updatedAt)
        assertEquals("PROFESSIONAL", meta.tier)
    }

    // ==================== MerchantTier Tier Field Tests ====================

    @Test
    fun `tier field is mutable`() {
        val meta = MerchantMeta()
        assertEquals("FREE", meta.tier)
        
        meta.tier = "STARTER"
        assertEquals("STARTER", meta.tier)
        
        meta.tier = "ENTERPRISE"
        assertEquals("ENTERPRISE", meta.tier)
    }

    // ==================== Data Class Equality Tests ====================

    @Test
    fun `metas with same values are equal`() {
        val meta1 = MerchantMeta(tier = "STARTER")
        val meta2 = MerchantMeta(tier = "STARTER")
        
        assertEquals(meta1, meta2)
    }

    @Test
    fun `metas with different tiers are not equal`() {
        val meta1 = MerchantMeta(tier = "FREE")
        val meta2 = MerchantMeta(tier = "ENTERPRISE")
        
        assertNotEquals(meta1, meta2)
    }

    @Test
    fun `meta copy preserves tier`() {
        val original = MerchantMeta(tier = "PROFESSIONAL")
        val copy = original.copy()
        
        assertEquals(original.tier, copy.tier)
    }

    @Test
    fun `meta copy can override tier`() {
        val original = MerchantMeta(tier = "FREE")
        val copy = original.copy(tier = "ENTERPRISE")
        
        assertEquals("FREE", original.tier)
        assertEquals("ENTERPRISE", copy.tier)
    }

    // ==================== CURRENT_SCHEMA_VERSION Tests ====================

    @Test
    fun `CURRENT_SCHEMA_VERSION is 2`() {
        assertEquals(2, MerchantMeta.CURRENT_SCHEMA_VERSION)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles empty tier string`() {
        val meta = MerchantMeta(tier = "")
        assertEquals("", meta.tier)
    }

    @Test
    fun `handles custom tier string`() {
        val meta = MerchantMeta(tier = "CUSTOM_TIER")
        assertEquals("CUSTOM_TIER", meta.tier)
    }

    @Test
    fun `handles lowercase tier string`() {
        val meta = MerchantMeta(tier = "free")
        assertEquals("free", meta.tier)
    }
}
