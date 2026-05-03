package com.orderMate.utils

/**
 * Firebase Realtime Database path builders
 * 
 * Structure:
 * merchants/{merchantId}/
 *   ├── meta/
 *   │   ├── schemaVersion
 *   │   ├── createdAt
 *   │   └── updatedAt
 *   ├── settings/
 *   │   ├── triggerOnItemAdd
 *   │   ├── triggerFromBasket
 *   │   ├── useOrderMateInRegister
 *   │   ├── notificationDays
 *   │   ├── notificationMinutes
 *   │   ├── receiptDays
 *   │   └── receiptMinutes
 *   ├── widgets/{widgetId}/
 *   │   ├── type
 *   │   ├── label
 *   │   ├── isEnabled
 *   │   ├── isRequired
 *   │   ├── showInFilter
 *   │   ├── order
 *   │   └── options/{optionId}/
 *   │       ├── label
 *   │       ├── value
 *   │       ├── isDefault
 *   │       └── color
 *   ├── templates/{templateId}/
 *   │   ├── name
 *   │   └── content
 *   ├── profiles/{employeeId}/           (#81)
 *   │   ├── color
 *   │   └── avatar
 *   ├── referrals/{referralId}/          (#81)
 *   │   ├── id
 *   │   ├── partnerName
 *   │   ├── submittedAt
 *   │   └── submittedBy
 *   └── discounts/{discountId}/          (#81 - admin only)
 *       ├── id
 *       ├── amount
 *       ├── startDate
 *       ├── endDate
 *       ├── discountCode
 *       ├── createdAt
 *       └── isActive
 */
object FirebasePaths {
    
    // Root collections
    const val MERCHANTS = "merchants"
    
    // Sub-paths
    const val META = "meta"
    const val SETTINGS = "settings"
    const val WIDGETS = "widgets"
    const val OPTIONS = "options"
    const val TEMPLATES = "templates"
    
    // #81: Per-employee profiles, referrals, and discounts
    const val PROFILES = "profiles"
    const val REFERRALS = "referrals"
    const val DISCOUNTS = "discounts"
    
    // Legacy - kept for backward compatibility
    @Deprecated("Use PROFILES instead", ReplaceWith("PROFILES"))
    const val PROFILE_SETTINGS = "profileSettings"
    
    // Meta fields
    const val SCHEMA_VERSION = "schemaVersion"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    
    // ==================== Path Builders ====================
    
    fun merchant(merchantId: String) = "$MERCHANTS/$merchantId"
    
    fun meta(merchantId: String) = "${merchant(merchantId)}/$META"
    
    fun settings(merchantId: String) = "${merchant(merchantId)}/$SETTINGS"
    
    fun widgets(merchantId: String) = "${merchant(merchantId)}/$WIDGETS"
    
    fun widget(merchantId: String, widgetId: String) = "${widgets(merchantId)}/$widgetId"
    
    fun options(merchantId: String, widgetId: String) = "${widget(merchantId, widgetId)}/$OPTIONS"
    
    fun option(merchantId: String, widgetId: String, optionId: String) = 
        "${options(merchantId, widgetId)}/$optionId"
    
    fun templates(merchantId: String) = "${merchant(merchantId)}/$TEMPLATES"
    
    fun template(merchantId: String, templateId: String) = "${templates(merchantId)}/$templateId"
    
    // ==================== #81: Employee Profiles ====================
    
    fun profiles(merchantId: String) = "${merchant(merchantId)}/$PROFILES"
    
    fun employeeProfile(merchantId: String, employeeId: String) = 
        "${profiles(merchantId)}/$employeeId"
    
    // ==================== #81: Referrals ====================
    
    fun referrals(merchantId: String) = "${merchant(merchantId)}/$REFERRALS"
    
    fun referral(merchantId: String, referralId: String) = 
        "${referrals(merchantId)}/$referralId"
    
    // ==================== #81: Discounts ====================
    
    fun discounts(merchantId: String) = "${merchant(merchantId)}/$DISCOUNTS"
    
    fun discount(merchantId: String, discountId: String) = 
        "${discounts(merchantId)}/$discountId"
    
    // ==================== Legacy (Deprecated) ====================
    
    @Deprecated("Use employeeProfile() instead", ReplaceWith("employeeProfile(merchantId, employeeId)"))
    fun profileSettings(merchantId: String) = "${merchant(merchantId)}/$PROFILE_SETTINGS"
}
