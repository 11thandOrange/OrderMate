package com.orderMate.modals

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ReferralInfo, including the partner-key normalization added for the
 * referralPartners/{partnerKey} cross-merchant index (#142).
 */
class ReferralInfoTest {

    @Test
    fun `create populates merchantId, partnerName, and submittedBy`() {
        val referral = ReferralInfo.create(
            partnerName = "Jane's Referral Co",
            employeeId = "emp-1",
            merchantId = "merchant-1"
        )

        assertEquals("Jane's Referral Co", referral.partnerName)
        assertEquals("emp-1", referral.submittedBy)
        assertEquals("merchant-1", referral.merchantId)
        assertTrue(referral.id.startsWith("ref_"))
        assertTrue(referral.submittedAt > 0)
    }

    @Test
    fun `toMap includes merchantId`() {
        val referral = ReferralInfo.create("Partner Co", "emp-1", "merchant-1")

        val map = referral.toMap()

        assertEquals("merchant-1", map["merchantId"])
        assertEquals("Partner Co", map["partnerName"])
        assertEquals("emp-1", map["submittedBy"])
    }

    @Test
    fun `normalizePartnerKey lowercases and trims`() {
        assertEquals("jane's co", ReferralInfo.normalizePartnerKey("  Jane's Co  "))
        assertEquals("partner", ReferralInfo.normalizePartnerKey("PARTNER"))
    }

    @Test
    fun `normalizePartnerKey treats case and whitespace variants as the same partner`() {
        val a = ReferralInfo.normalizePartnerKey("Jane's Co")
        val b = ReferralInfo.normalizePartnerKey(" jane's co ")
        val c = ReferralInfo.normalizePartnerKey("JANE'S CO")

        assertEquals(a, b)
        assertEquals(a, c)
    }

    @Test
    fun `normalizePartnerKey strips characters Firebase keys disallow`() {
        val key = ReferralInfo.normalizePartnerKey("A.B#C\$D[E]F/G")

        assertFalse(key.contains("."))
        assertFalse(key.contains("#"))
        assertFalse(key.contains("$"))
        assertFalse(key.contains("["))
        assertFalse(key.contains("]"))
        assertFalse(key.contains("/"))
    }

    @Test
    fun `normalizePartnerKey is stable for distinct partner names`() {
        val a = ReferralInfo.normalizePartnerKey("Acme Referrals")
        val b = ReferralInfo.normalizePartnerKey("Beta Partners")

        assertNotEquals(a, b)
    }
}
