package com.orderMate.modals

/**
 * Merchant metadata
 * Path: merchants/{merchantId}/meta
 */
data class MerchantMeta(
    val schemaVersion: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
