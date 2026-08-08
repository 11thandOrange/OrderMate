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
 *   │   ├── merchantId
 *   │   ├── partnerName
 *   │   ├── submittedAt
 *   │   └── submittedBy
 *   ├── discounts/{discountId}/          (#81 - admin only)
 *   │   ├── id
 *   │   ├── amount
 *   │   ├── startDate
 *   │   ├── endDate
 *   │   ├── discountCode
 *   │   ├── createdAt
 *   │   └── isActive
 *   └── orderConversations/{orderId}/{conversationId}: true  (#54)
 *       Bird conversation ids recorded against the order they were sent for.
 *       Bird itself has no server-side way to query conversations by order -
 *       its `resource` field has a fixed type enum with no value for an order
 *       and requires a UUID id, which Clover order ids never are - so
 *       OrderMate tracks this mapping itself to render notification history.
 *
 * referralPartners/{partnerKey}/{referralId}/  (#142 - top-level, NOT merchant-scoped)
 *   Denormalized copy of each referral, keyed by a normalized partner name, so
 *   "all referrals for one partner" can be read directly instead of scanning
 *   every merchant's referrals subtree. Same fields as merchants/{id}/referrals/{id}.
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

    // #54: Bird conversation ids recorded per order
    const val ORDER_CONVERSATIONS = "orderConversations"

    // Top-level (not merchant-scoped) index for cross-merchant partner lookups
    const val REFERRAL_PARTNERS = "referralPartners"

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

    // Top-level index: referralPartners/{partnerKey}/{referralId} - lets a query find
    // every referral for one partner across all merchants without scanning "merchants".
    fun referralPartner(partnerKey: String) = "$REFERRAL_PARTNERS/$partnerKey"

    fun referralPartnerEntry(partnerKey: String, referralId: String) =
        "${referralPartner(partnerKey)}/$referralId"
    
    // ==================== #81: Discounts ====================
    
    fun discounts(merchantId: String) = "${merchant(merchantId)}/$DISCOUNTS"

    fun discount(merchantId: String, discountId: String) =
        "${discounts(merchantId)}/$discountId"

    // ==================== #54: Order Conversations ====================

    fun orderConversations(merchantId: String, orderId: String) =
        "${merchant(merchantId)}/$ORDER_CONVERSATIONS/$orderId"

    fun orderConversation(merchantId: String, orderId: String, conversationId: String) =
        "${orderConversations(merchantId, orderId)}/$conversationId"

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
