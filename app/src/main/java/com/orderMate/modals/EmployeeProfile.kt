package com.orderMate.modals

/**
 * Per-employee profile settings (#81)
 * Stored at: merchants/{merchantId}/profiles/{employeeId}/
 * 
 * Role is NOT stored here - fetched from Clover SDK at runtime
 */
data class EmployeeProfile(
    val color: String = DEFAULT_COLOR,
    val avatar: String = DEFAULT_AVATAR
) {
    companion object {
        const val DEFAULT_COLOR = "#3C4B80"
        const val DEFAULT_AVATAR = "😊"
    }
    
    fun toMap(): Map<String, Any> = mapOf(
        "color" to color,
        "avatar" to avatar
    )
}
