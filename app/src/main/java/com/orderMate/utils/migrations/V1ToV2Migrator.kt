package com.orderMate.utils.migrations

import com.orderMate.modals.*
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.ModalDialogCategories
import java.util.UUID

/**
 * Migrates from legacy v1 schema to v2
 * 
 * V1 Structure (customData/{merchantId}/data):
 * - Single JSON string with types array
 * - Each type has: name, type (enum), hasDropDown, isActive, list (string array)
 * 
 * V2 Structure (merchants/{merchantId}/):
 * - Separate collections: meta, settings, widgets
 * - Widgets have UUIDs and structured options
 */
object V1ToV2Migrator {
    
    private const val TAG = "V1ToV2Migrator"
    
    /**
     * Perform migration from v1 to v2
     */
    fun migrate(merchantId: String, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        firebase.getLegacyData(merchantId) { legacyData ->
            if (legacyData == null) {
                android.util.Log.e(TAG, "No legacy data found to migrate")
                callback(false)
                return@getLegacyData
            }
            
            try {
                android.util.Log.d(TAG, "Converting ${legacyData.types.size} legacy types")
                
                // Convert to new format
                val widgets = convertWidgets(legacyData)
                val settings = extractSettings(legacyData)
                
                android.util.Log.d(TAG, "Converted to ${widgets.size} widgets")
                
                // Save to new structure
                firebase.initializeMerchant(merchantId, widgets, settings) { success ->
                    if (success) {
                        android.util.Log.d(TAG, "Migration completed successfully")
                    } else {
                        android.util.Log.e(TAG, "Failed to save migrated data")
                    }
                    callback(success)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Migration error: ${e.message}")
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * Convert legacy ModalData items to WidgetConfig list
     */
    private fun convertWidgets(legacy: LegacyCustomItemJson): List<WidgetConfig> {
        val widgets = mutableListOf<WidgetConfig>()
        var order = 0
        
        legacy.types.forEach { modalData ->
            // Skip trigger settings - they go to PopupSettings
            if (modalData.type == ModalDialogCategories.ModalShown ||
                modalData.type == ModalDialogCategories.BasketShown) {
                return@forEach
            }
            
            val widgetType = mapLegacyType(modalData.type)
            
            val widget = WidgetConfig(
                id = UUID.randomUUID().toString(),
                type = widgetType,
                label = modalData.name,
                isEnabled = modalData.isActive,
                isRequired = false,
                showInFilter = modalData.hasDropDown && widgetType != WidgetType.TEXT_BOX,
                order = order++,
                options = convertOptions(modalData.list)
            )
            
            widgets.add(widget)
            android.util.Log.d(TAG, "Converted widget: ${widget.label} (${widget.type})")
        }
        
        return widgets
    }
    
    /**
     * Map legacy ModalDialogCategories to new WidgetType
     */
    private fun mapLegacyType(legacyType: ModalDialogCategories): WidgetType {
        return when (legacyType) {
            ModalDialogCategories.PickUpDate -> WidgetType.CALENDAR
            ModalDialogCategories.Description -> WidgetType.TEXT_BOX
            ModalDialogCategories.OrderCategories -> WidgetType.MULTI_SELECT
            ModalDialogCategories.OrderSubCategories -> WidgetType.MULTI_SELECT
            ModalDialogCategories.OrderType -> WidgetType.SINGLE_SELECT
            ModalDialogCategories.OrderProgress -> WidgetType.SINGLE_SELECT
            else -> WidgetType.SINGLE_SELECT
        }
    }
    
    /**
     * Convert string list to WidgetOption list
     */
    private fun convertOptions(legacyList: MutableList<String>): MutableList<WidgetOption> {
        return legacyList.mapIndexed { index, value ->
            WidgetOption(
                id = UUID.randomUUID().toString(),
                label = value.trim(),
                value = value.trim(),
                isDefault = index == 0 // First option is default
            )
        }.toMutableList()
    }
    
    /**
     * Extract popup settings from legacy data
     */
    private fun extractSettings(legacy: LegacyCustomItemJson): PopupSettings {
        var triggerOnAdd = false
        var triggerFromBasket = false
        
        legacy.types.forEach { modalData ->
            when (modalData.type) {
                ModalDialogCategories.ModalShown -> {
                    triggerOnAdd = modalData.isActive
                }
                ModalDialogCategories.BasketShown -> {
                    triggerFromBasket = modalData.isActive
                }
                else -> { /* Skip other types */ }
            }
        }
        
        android.util.Log.d(TAG, "Extracted settings: triggerOnAdd=$triggerOnAdd, triggerFromBasket=$triggerFromBasket")
        
        return PopupSettings(
            triggerOnItemAdd = triggerOnAdd,
            triggerFromBasket = triggerFromBasket
        )
    }
}
