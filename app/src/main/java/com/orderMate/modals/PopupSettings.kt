package com.orderMate.modals

/**
 * Pop-up behavior settings
 * Path: merchants/{merchantId}/settings
 */
data class PopupSettings(
    var triggerOnItemAdd: Boolean = false,
    var triggerFromBasket: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "triggerOnItemAdd" to triggerOnItemAdd,
        "triggerFromBasket" to triggerFromBasket
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>?): PopupSettings {
            if (map == null) return PopupSettings()
            return PopupSettings(
                triggerOnItemAdd = map["triggerOnItemAdd"] as? Boolean ?: false,
                triggerFromBasket = map["triggerFromBasket"] as? Boolean ?: false
            )
        }
    }
}
