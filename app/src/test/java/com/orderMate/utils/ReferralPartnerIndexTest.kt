package com.orderMate.utils

import com.orderMate.modals.ReferralInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the multi-path update FirebaseConfigManager.saveReferral() builds (#142).
 *
 * FirebaseConfigManager needs a real FirebaseDatabase instance, so - following this
 * codebase's convention (see WidgetManagerTest, OrderNoteDialogTest) - this mirrors the
 * pure map-construction logic locally rather than instantiating the real manager.
 */
class ReferralPartnerIndexTest {

    // Mirrors FirebaseConfigManager.saveReferral()'s update map construction
    private fun buildReferralUpdates(merchantId: String, referral: ReferralInfo): Map<String, Any?> {
        val partnerKey = ReferralInfo.normalizePartnerKey(referral.partnerName)
        return mapOf(
            FirebasePaths.referral(merchantId, referral.id) to referral.toMap(),
            FirebasePaths.referralPartnerEntry(partnerKey, referral.id) to referral.toMap()
        )
    }

    @Test
    fun `saving a referral writes both the merchant-scoped path and the partner index path`() {
        val referral = ReferralInfo.create("Acme Referrals", "emp-1", "merchant-1")

        val updates = buildReferralUpdates("merchant-1", referral)

        assertEquals(2, updates.size)
        assertTrue(updates.containsKey("merchants/merchant-1/referrals/${referral.id}"))
        assertTrue(updates.containsKey("referralPartners/acme referrals/${referral.id}"))
    }

    @Test
    fun `both write locations carry the same referral data`() {
        val referral = ReferralInfo.create("Acme Referrals", "emp-1", "merchant-1")

        val updates = buildReferralUpdates("merchant-1", referral)
        val values = updates.values.toList()

        assertEquals(values[0], values[1])
    }

    @Test
    fun `referrals from different merchants for the same partner land under the same partner key`() {
        val referralFromA = ReferralInfo.create("Acme Referrals", "emp-1", "merchant-A")
        val referralFromB = ReferralInfo.create("acme referrals", "emp-2", "merchant-B")

        val updatesA = buildReferralUpdates("merchant-A", referralFromA)
        val updatesB = buildReferralUpdates("merchant-B", referralFromB)

        val partnerPathA = updatesA.keys.first { it.startsWith("referralPartners/") }
        val partnerPathB = updatesB.keys.first { it.startsWith("referralPartners/") }

        assertEquals(
            partnerPathA.substringBeforeLast("/"),
            partnerPathB.substringBeforeLast("/")
        )
    }
}
