package com.orderMate.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType

/**
 * Singleton manager for widget operations.
 * Follows production pattern: Firebase listener saves to SharedPreferences,
 * UI reads from SharedPreferences synchronously.
 */
class WidgetManager private constructor(private val context: Context) {
    
    private val firebase = FirebaseConfigManager.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var merchantId: String? = null
    
    companion object {
        private const val PREFS_NAME = "widget_config_v2"
        private const val KEY_WIDGETS = "widgets"
        private const val KEY_SETTINGS = "settings"
        const val MAX_WIDGETS_PER_LEVEL = 7
        
        @Volatile
        private var instance: WidgetManager? = null
        
        fun getInstance(context: Context): WidgetManager {
            return instance ?: synchronized(this) {
                instance ?: WidgetManager(context.applicationContext).also { instance = it }
            }
        }
        
        fun getInstance(): WidgetManager {
            return instance ?: throw IllegalStateException("WidgetManager not initialized. Call getInstance(context) first.")
        }

        /**
         * Get cached widgets without requiring instance context.
         * Returns empty list if WidgetManager not initialized.
         */
        fun getCachedWidgets(): List<WidgetConfig> {
            return instance?.getWidgets() ?: emptyList()
        }
    }
    
    // ==================== Initialization ====================
    
    /**
     * Set merchant ID for CRUD operations.
     */
    fun setMerchantId(merchantId: String) {
        this.merchantId = merchantId
    }
    
    /**
     * Get merchant ID for direct Firebase queries.
     */
    fun getMerchantId(): String? = merchantId
    
    // ==================== Read from Cache (synchronous) ====================
    
    /**
     * Get widgets from local cache.
     * Returns defaults if cache is empty - popup is never empty.
     */
    fun getWidgets(): List<WidgetConfig> {
        return try {
            val json = prefs.getString(KEY_WIDGETS, null)
            if (json != null) {
                val type = object : TypeToken<List<WidgetConfig>>() {}.type
                val widgets = gson.fromJson<List<WidgetConfig>>(json, type)
                if (widgets.isNullOrEmpty()) {
                    DefaultWidgetFactory.createDefaults()
                } else {
                    // Normalize: set level to ITEM if null (backward compatibility)
                    widgets.forEach { if (it.level == null) it.level = NoteLevel.ITEM }
                    widgets
                }
            } else {
                // Cache empty → return defaults
                DefaultWidgetFactory.createDefaults()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DefaultWidgetFactory.createDefaults()
        }
    }
    
    /**
     * Get enabled widgets sorted by order.
     */
    fun getEnabledWidgets(): List<WidgetConfig> {
        return getWidgets().filter { it.isEnabled }.sortedBy { it.order }
    }
    
    /**
     * Get filterable widgets (enabled, excluding TEXT_BOX).
     */
    fun getFilterableWidgets(): List<WidgetConfig> {
        return getWidgets()
            .filter { it.isEnabled && it.type != WidgetType.TEXT_BOX }
            .sortedBy { it.order }
    }
    
    // ==================== Level-based Filtering ====================
    
    /**
     * Get item-level widgets (enabled) sorted by order.
     */
    fun getItemLevelWidgets(): List<WidgetConfig> {
        return getWidgets()
            .filter { it.level == NoteLevel.ITEM && it.isEnabled }
            .sortedBy { it.order }
    }
    
    /**
     * Get order-level widgets (enabled) sorted by order.
     */
    fun getOrderLevelWidgets(): List<WidgetConfig> {
        return getWidgets()
            .filter { it.level == NoteLevel.ORDER && it.isEnabled }
            .sortedBy { it.order }
    }
    
    /**
     * Get all item-level widgets (including disabled) sorted by order.
     */
    fun getAllItemLevelWidgets(): List<WidgetConfig> {
        return getWidgets()
            .filter { it.level == NoteLevel.ITEM }
            .sortedBy { it.order }
    }
    
    /**
     * Get all order-level widgets (including disabled) sorted by order.
     */
    fun getAllOrderLevelWidgets(): List<WidgetConfig> {
        return getWidgets()
            .filter { it.level == NoteLevel.ORDER }
            .sortedBy { it.order }
    }
    
    /**
     * Get count of item-level widgets.
     */
    fun getItemLevelWidgetCount(): Int {
        return getWidgets().count { it.level == NoteLevel.ITEM }
    }
    
    /**
     * Get count of order-level widgets.
     */
    fun getOrderLevelWidgetCount(): Int {
        return getWidgets().count { it.level == NoteLevel.ORDER }
    }
    
    /**
     * Check if can add more widgets for a given level (max 7 per level).
     */
    fun canAddWidget(level: NoteLevel): Boolean {
        val count = if (level == NoteLevel.ITEM) getItemLevelWidgetCount() else getOrderLevelWidgetCount()
        return count < MAX_WIDGETS_PER_LEVEL
    }
    
    /**
     * Get settings from local cache.
     */
    fun getSettings(): PopupSettings {
        return try {
            val json = prefs.getString(KEY_SETTINGS, null)
            if (json != null) {
                gson.fromJson(json, PopupSettings::class.java) ?: PopupSettings()
            } else {
                PopupSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PopupSettings()
        }
    }
    
    /**
     * Check if any widgets are enabled.
     */
    fun hasEnabledWidgets(): Boolean {
        return getEnabledWidgets().isNotEmpty()
    }
    
    // ==================== Save to Cache ====================
    
    /**
     * Save widgets and settings to local cache.
     * Called from MainActivityRedesign after Firebase fetch.
     */
    fun saveToCache(widgets: List<WidgetConfig>, settings: PopupSettings) {
        prefs.edit().apply {
            putString(KEY_WIDGETS, gson.toJson(widgets))
            putString(KEY_SETTINGS, gson.toJson(settings))
            apply()
        }
    }
    
    private fun saveWidgetsToCache(widgets: List<WidgetConfig>) {
        prefs.edit().putString(KEY_WIDGETS, gson.toJson(widgets)).apply()
    }
    
    private fun saveSettingsToCache(settings: PopupSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }
    
    // ==================== Widget CRUD ====================
    
    fun addWidget(type: WidgetType, level: NoteLevel = NoteLevel.ITEM, callback: (WidgetConfig?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        
        // Check max widgets per level
        if (!canAddWidget(level)) {
            callback(null)
            return
        }
        
        val widgetsForLevel = if (level == NoteLevel.ITEM) getAllItemLevelWidgets() else getAllOrderLevelWidgets()
        val order = (widgetsForLevel.maxOfOrNull { it.order } ?: -1) + 1
        val widget = DefaultWidgetFactory.createEmpty(type, order, level)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = getWidgets().toMutableList()
                widgets.add(widget)
                saveWidgetsToCache(widgets)
                callback(widget)
            } else {
                callback(null)
            }
        }
    }
    
    fun addWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        // Check max widgets per level
        if (!canAddWidget(widget.level)) {
            callback(false)
            return
        }
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = getWidgets().toMutableList()
                widgets.add(widget)
                saveWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    fun updateWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        // Note: Level is now enforced by saveItemWidget/saveOrderWidget in SettingsFragment
        // The widget's level should already be correct when this method is called
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = getWidgets().toMutableList()
                val index = widgets.indexOfFirst { it.id == widget.id }
                if (index >= 0) {
                    widgets[index] = widget
                    saveWidgetsToCache(widgets)
                }
            }
            callback(success)
        }
    }
    
    fun updateWidgetLabel(widgetId: String, newLabel: String, callback: (Boolean) -> Unit) {
        val widget = getWidgetById(widgetId)
        if (widget == null) {
            callback(false)
            return
        }
        widget.label = newLabel
        updateWidget(widget, callback)
    }
    
    fun toggleWidgetEnabled(widgetId: String, callback: (Boolean) -> Unit) {
        val widget = getWidgetById(widgetId)
        if (widget == null) {
            callback(false)
            return
        }
        widget.isEnabled = !widget.isEnabled
        updateWidget(widget, callback)
    }
    
    fun toggleShowInFilter(widgetId: String, callback: (Boolean) -> Unit) {
        val widget = getWidgetById(widgetId)
        if (widget == null) {
            callback(false)
            return
        }
        widget.showInFilter = !widget.showInFilter
        updateWidget(widget, callback)
    }
    
    fun deleteWidget(widgetId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        firebase.deleteWidget(mid, widgetId) { success ->
            if (success) {
                val widgets = getWidgets().toMutableList()
                widgets.removeAll { it.id == widgetId }
                widgets.forEachIndexed { index, w -> w.order = index }
                saveWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    fun reorderWidgets(fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getWidgets().toMutableList()
        
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= widgets.size || toIndex >= widgets.size) {
            callback(false)
            return
        }
        
        val widget = widgets.removeAt(fromIndex)
        widgets.add(toIndex, widget)
        widgets.forEachIndexed { index, w -> w.order = index }
        saveWidgetsToCache(widgets)
        
        firebase.saveWidgetsBatch(mid, widgets, callback)
    }
    
    /**
     * Reorder widgets within a specific level only.
     * This prevents index mismatches when reordering in level-specific adapters.
     */
    fun reorderWidgetsForLevel(level: NoteLevel, fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        // Get widgets for this level only
        val levelWidgets = getWidgets().filter { it.level == level }.sortedBy { it.order }.toMutableList()
        
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= levelWidgets.size || toIndex >= levelWidgets.size) {
            callback(false)
            return
        }
        
        // Reorder within level
        val widget = levelWidgets.removeAt(fromIndex)
        levelWidgets.add(toIndex, widget)
        
        // Update order values for the level
        levelWidgets.forEachIndexed { index, w -> w.order = index }
        
        // Merge back with other level's widgets
        val otherLevelWidgets = getWidgets().filter { it.level != level }
        val allWidgets = (levelWidgets + otherLevelWidgets).toMutableList()
        
        saveWidgetsToCache(allWidgets)
        firebase.saveWidgetsBatch(mid, allWidgets, callback)
    }
    
    // ==================== Option CRUD ====================
    
    fun addOption(widgetId: String, label: String, value: String = label, callback: (WidgetOption?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        val widgets = getWidgets().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(null)
            return
        }
        
        val option = DefaultWidgetFactory.createOption(label, value)
        widget.options.add(option)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                saveWidgetsToCache(widgets)
                callback(option)
            } else {
                widget.options.removeAll { it.id == option.id }
                callback(null)
            }
        }
    }
    
    fun updateOption(widgetId: String, option: WidgetOption, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getWidgets().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val index = widget.options.indexOfFirst { it.id == option.id }
        if (index >= 0) {
            widget.options[index] = option
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun deleteOption(widgetId: String, optionId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getWidgets().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val removed = widget.options.removeAll { it.id == optionId }
        if (removed) {
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun reorderOptions(widgetId: String, fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getWidgets().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= widget.options.size || toIndex >= widget.options.size) {
            callback(false)
            return
        }
        
        val option = widget.options.removeAt(fromIndex)
        widget.options.add(toIndex, option)
        firebase.saveWidget(mid, widget) { success ->
            if (success) saveWidgetsToCache(widgets)
            callback(success)
        }
    }
    
    // ==================== Settings ====================
    
    fun updateSettings(settings: PopupSettings, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        firebase.saveSettings(mid, settings) { success ->
            if (success) {
                saveSettingsToCache(settings)
            }
            callback(success)
        }
    }
    
    fun setTriggerOnItemAdd(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = getSettings().copy(triggerOnItemAdd = enabled)
        updateSettings(newSettings, callback)
    }
    
    fun setShowOMButtonInRegister(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = getSettings().copy(showOMButtonInRegister = enabled)
        updateSettings(newSettings, callback)
    }
    
    /**
     * Enable/disable item-level notes (#34)
     */
    fun setItemNotesEnabled(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = getSettings().copy(itemNotesEnabled = enabled)
        updateSettings(newSettings, callback)
    }
    
    /**
     * Enable/disable order-level notes (#34)
     */
    fun setOrderNotesEnabled(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = getSettings().copy(orderNotesEnabled = enabled)
        updateSettings(newSettings, callback)
    }
    
    /**
     * Check if item-level notes are enabled (#34)
     */
    fun isItemNotesEnabled(): Boolean = getSettings().itemNotesEnabled
    
    /**
     * Check if order-level notes are enabled (#34)
     */
    fun isOrderNotesEnabled(): Boolean = getSettings().orderNotesEnabled
    
    // ==================== Helpers ====================
    
    fun getWidgetById(widgetId: String): WidgetConfig? {
        return getWidgets().find { it.id == widgetId }
    }
    
    fun getWidgetByLabel(label: String): WidgetConfig? {
        return getWidgets().find { it.label.equals(label, ignoreCase = true) }
    }
    
    fun getOptionsForWidget(widgetId: String): List<WidgetOption> {
        return getWidgetById(widgetId)?.options ?: emptyList()
    }
    
    fun getWidgetCount(): Int = getWidgets().size
    
    fun getEnabledWidgetCount(): Int = getWidgets().count { it.isEnabled }
    
    /**
     * Force reload from Firebase (e.g., after settings change)
     */
    fun reload(callback: ((Boolean) -> Unit)? = null) {
        val mid = merchantId ?: return
        firebase.getWidgets(mid) { widgets ->
            firebase.getSettings(mid) { settings ->
                saveToCache(widgets, settings)
                callback?.invoke(true)
            }
        }
    }
}
