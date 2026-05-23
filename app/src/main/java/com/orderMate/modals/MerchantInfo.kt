package com.orderMate.modals

/**
 * Merchant information for analytics and reporting
 * Path: merchants/{merchantId}/merchantInfo
 * 
 * Issue #97: Store all relevant merchant data
 */
data class MerchantInfo(
    val merchantId: String = "",
    val name: String = "",              // Owner/contact name
    val email: String = "",             // Contact email
    val storeName: String = "",         // Business name
    val installDate: Long = 0,          // App install timestamp
    val uninstallDate: Long? = null,    // Null if still active
    val lastActiveDate: Long = 0        // Last app usage timestamp
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "merchantId" to merchantId,
        "name" to name,
        "email" to email,
        "storeName" to storeName,
        "installDate" to installDate,
        "uninstallDate" to uninstallDate,
        "lastActiveDate" to lastActiveDate
    )

    fun isActive(): Boolean = uninstallDate == null

    companion object {
        fun fromMap(map: Map<String, Any?>?): MerchantInfo {
            if (map == null) return MerchantInfo()
            return MerchantInfo(
                merchantId = map["merchantId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                storeName = map["storeName"] as? String ?: "",
                installDate = (map["installDate"] as? Number)?.toLong() ?: 0,
                uninstallDate = (map["uninstallDate"] as? Number)?.toLong(),
                lastActiveDate = (map["lastActiveDate"] as? Number)?.toLong() ?: 0
            )
        }
    }
}
