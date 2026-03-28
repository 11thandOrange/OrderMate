package com.orderMate.modals

/**
 * Pop-up behavior settings
 * Path: merchants/{merchantId}/settings
 * 
 * @property triggerOnItemAdd - Show popup when item is added to cart (future use)
 * @property showOMButtonInRegister - Show OrderMate button in Clover Register app
 */
data class PopupSettings(
    var triggerOnItemAdd: Boolean = false,
    var showOMButtonInRegister: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "triggerOnItemAdd" to triggerOnItemAdd,
        "showOMButtonInRegister" to showOMButtonInRegister
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>?): PopupSettings {
            if (map == null) return PopupSettings()
            return PopupSettings(
                triggerOnItemAdd = map["triggerOnItemAdd"] as? Boolean ?: false,
                // Support both old and new field names for backward compatibility
                showOMButtonInRegister = map["showOMButtonInRegister"] as? Boolean 
                    ?: map["triggerFromBasket"] as? Boolean 
                    ?: true
            )
        }
    }
}
