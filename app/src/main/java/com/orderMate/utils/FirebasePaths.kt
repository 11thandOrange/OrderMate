package com.orderMate.utils

/**
 * Firebase Realtime Database path builders
 * 
 * New Structure:
 * merchants/{merchantId}/
 *   ├── meta/
 *   │   ├── schemaVersion
 *   │   ├── createdAt
 *   │   └── updatedAt
 *   ├── settings/
 *   │   ├── triggerOnItemAdd
 *   │   └── triggerFromBasket
 *   └── widgets/{widgetId}/
 *       ├── type
 *       ├── label
 *       ├── isEnabled
 *       ├── isRequired
 *       ├── showInFilter
 *       ├── order
 *       └── options/{optionId}/
 *           ├── label
 *           ├── value
 *           ├── isDefault
 *           └── color
 */
object FirebasePaths {
    
    // Root collections
    const val MERCHANTS = "merchants"
    
    // Sub-paths
    const val META = "meta"
    const val SETTINGS = "settings"
    const val WIDGETS = "widgets"
    const val OPTIONS = "options"
    
    // Legacy (for migration)
    const val LEGACY_ROOT = "customData"
    const val LEGACY_DATA = "data"
    
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
    
    // Legacy paths
    fun legacyData(merchantId: String) = "$LEGACY_ROOT/$merchantId/$LEGACY_DATA"
    
    fun legacyRoot(merchantId: String) = "$LEGACY_ROOT/$merchantId"
}
