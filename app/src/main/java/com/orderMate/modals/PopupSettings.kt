package com.orderMate.modals

/**
 * Pop-up behavior settings
 * Path: merchants/{merchantId}/settings
 * 
 * @property triggerOnItemAdd - Show popup when item is added to cart (future use)
 * @property showOMButtonInRegister - Show OrderMate button in Clover Register app
 * @property itemNotesEnabled - Enable item-level notes functionality (#34)
 * @property orderNotesEnabled - Enable order-level notes functionality (#34)
 */
data class PopupSettings(
    var triggerOnItemAdd: Boolean = false,
    var showOMButtonInRegister: Boolean = true,
    var itemNotesEnabled: Boolean = true,
    var orderNotesEnabled: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "triggerOnItemAdd" to triggerOnItemAdd,
        "showOMButtonInRegister" to showOMButtonInRegister,
        "itemNotesEnabled" to itemNotesEnabled,
        "orderNotesEnabled" to orderNotesEnabled
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>?): PopupSettings {
            if (map == null) return PopupSettings()
            return PopupSettings(
                triggerOnItemAdd = map["triggerOnItemAdd"] as? Boolean ?: false,
                // Support both old and new field names for backward compatibility
                showOMButtonInRegister = map["showOMButtonInRegister"] as? Boolean 
                    ?: map["triggerFromBasket"] as? Boolean 
                    ?: true,
                itemNotesEnabled = map["itemNotesEnabled"] as? Boolean ?: true,
                orderNotesEnabled = map["orderNotesEnabled"] as? Boolean ?: true
            )
        }
    }
}
