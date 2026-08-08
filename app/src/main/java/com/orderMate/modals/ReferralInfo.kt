package com.orderMate.modals

import java.util.UUID

/**
 * Referral partner information (#81)
 * Stored at: merchants/{merchantId}/referrals/{referralId}/
 * Also indexed at: referralPartners/{partnerKey}/{referralId}/ for cross-merchant
 * partner lookups, since Firebase Realtime Database can't query across merchants'
 * separate referral subtrees.
 *
 * Only Owner can submit referral info via the Profile page
 */
data class ReferralInfo(
    val id: String = "",
    val merchantId: String = "",
    val partnerName: String = "",
    val submittedAt: Long = 0,
    val submittedBy: String = ""    // Employee ID who submitted
) {
    companion object {
        fun generateId(): String = "ref_${UUID.randomUUID().toString().take(8)}"

        fun create(partnerName: String, employeeId: String, merchantId: String): ReferralInfo {
            return ReferralInfo(
                id = generateId(),
                merchantId = merchantId,
                partnerName = partnerName,
                submittedAt = System.currentTimeMillis(),
                submittedBy = employeeId
            )
        }

        /**
         * Normalizes a partner name into a stable Firebase key so name variants
         * ("Jane's Co", " jane's co ") tally under the same partner in the
         * referralPartners index. Strips characters Firebase keys disallow (.#$[]/).
         */
        fun normalizePartnerKey(partnerName: String): String {
            return partnerName.trim().lowercase().replace(Regex("[.#$\\[\\]/]"), "_")
        }
    }

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "merchantId" to merchantId,
        "partnerName" to partnerName,
        "submittedAt" to submittedAt,
        "submittedBy" to submittedBy
    )
}
