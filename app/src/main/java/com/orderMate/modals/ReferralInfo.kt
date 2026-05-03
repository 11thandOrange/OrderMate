package com.orderMate.modals

import java.util.UUID

/**
 * Referral partner information (#81)
 * Stored at: merchants/{merchantId}/referrals/{referralId}/
 * 
 * Only Owner can submit referral info via the Profile page
 */
data class ReferralInfo(
    val id: String = "",
    val partnerName: String = "",
    val submittedAt: Long = 0,
    val submittedBy: String = ""    // Employee ID who submitted
) {
    companion object {
        fun generateId(): String = "ref_${UUID.randomUUID().toString().take(8)}"
        
        fun create(partnerName: String, employeeId: String): ReferralInfo {
            return ReferralInfo(
                id = generateId(),
                partnerName = partnerName,
                submittedAt = System.currentTimeMillis(),
                submittedBy = employeeId
            )
        }
    }
    
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "partnerName" to partnerName,
        "submittedAt" to submittedAt,
        "submittedBy" to submittedBy
    )
}
