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
 *   ├── merchantInfo/                    (#97 - merchant data for analytics)
 *   │   ├── merchantId
 *   │   ├── name
 *   │   ├── email
 *   │   ├── storeName
 *   │   ├── installDate
 *   │   ├── uninstallDate
 *   │   └── lastActiveDate
 *   ├── subscription/                    (#97 - subscription & billing)
 *   │   ├── plan
 *   │   ├── status
 *   │   ├── monthlyDueDate
 *   │   └── billingHistory/{paymentId}/
 *   │       ├── amount
 *   │       ├── dueDate
 *   │       ├── paidDate
 *   │       ├── status
 *   │       └── lateDays
 *   ├── events/{eventId}/                (#97/#98 - lifecycle events)
 *   │   ├── type
 *   │   ├── timestamp
 *   │   ├── details
 *   │   └── processed
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

    // #97/#98: Merchant info, subscription, and events
    const val MERCHANT_INFO = "merchantInfo"
    const val SUBSCRIPTION = "subscription"
    const val BILLING_HISTORY = "billingHistory"
    const val EVENTS = "events"
    
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

    // ==================== #97/#98: Merchant Info ====================

    fun merchantInfo(merchantId: String) = "${merchant(merchantId)}/$MERCHANT_INFO"

    // ==================== #97/#98: Subscription & Billing ====================

    fun subscription(merchantId: String) = "${merchant(merchantId)}/$SUBSCRIPTION"

    fun billingHistory(merchantId: String) = "${subscription(merchantId)}/$BILLING_HISTORY"

    fun billingRecord(merchantId: String, paymentId: String) =
        "${billingHistory(merchantId)}/$paymentId"

    // ==================== #97/#98: Lifecycle Events ====================

    fun events(merchantId: String) = "${merchant(merchantId)}/$EVENTS"

    fun event(merchantId: String, eventId: String) = "${events(merchantId)}/$eventId"
    
    // ==================== Legacy (Deprecated) ====================
    
    @Deprecated("Use employeeProfile() instead", ReplaceWith("employeeProfile(merchantId, employeeId)"))
    fun profileSettings(merchantId: String) = "${merchant(merchantId)}/$PROFILE_SETTINGS"
}
