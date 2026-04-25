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
 * 
 * IMPORTANT: Widgets are stored separately by level (ITEM vs ORDER) to prevent
 * cross-contamination bugs. Use level-specific methods for all operations.
 */
class WidgetManager private constructor(private val context: Context) {
    
    private val firebase = FirebaseConfigManager.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var merchantId: String? = null
    
    companion object {
        private const val PREFS_NAME = "widget_config_v2"
        private const val KEY_ITEM_WIDGETS = "widgets_item"
        private const val KEY_ORDER_WIDGETS = "widgets_order"
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
         * Get all cached widgets (both levels) without requiring instance context.
         * Returns empty list if WidgetManager not initialized.
         */
        fun getCachedWidgets(): List<WidgetConfig> {
            return instance?.getAllWidgets() ?: emptyList()
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
    
    // ==================== Item Level Cache (Private) ====================
    
    private fun getItemWidgetsFromCache(): List<WidgetConfig> {
        return try {
            val json = prefs.getString(KEY_ITEM_WIDGETS, null)
            android.util.Log.d("WidgetCacheDebug", "getItemWidgetsFromCache: json=${if (json != null) "${json.take(100)}..." else "NULL"}")
            if (json != null) {
                val type = object : TypeToken<List<WidgetConfig>>() {}.type
                val widgets = gson.fromJson<List<WidgetConfig>>(json, type)
                val result = widgets?.sortedBy { it.order } ?: emptyList()
                android.util.Log.d("WidgetCacheDebug", "getItemWidgetsFromCache: parsed ${result.size} widgets")
                result.forEach { w ->
                    android.util.Log.d("WidgetCacheDebug", "  CACHED ITEM: id=${w.id}, label=${w.label}")
                }
                result
            } else {
                android.util.Log.d("WidgetCacheDebug", "getItemWidgetsFromCache: CACHE IS EMPTY/NULL")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetCacheDebug", "getItemWidgetsFromCache: EXCEPTION", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun saveItemWidgetsToCache(widgets: List<WidgetConfig>) {
        android.util.Log.d("WidgetCacheDebug", "saveItemWidgetsToCache: saving ${widgets.size} widgets")
        widgets.forEach { w ->
            android.util.Log.d("WidgetCacheDebug", "  SAVING ITEM: id=${w.id}, label=${w.label}")
        }
        prefs.edit().putString(KEY_ITEM_WIDGETS, gson.toJson(widgets)).apply()
    }
    
    // ==================== Order Level Cache (Private) ====================
    
    private fun getOrderWidgetsFromCache(): List<WidgetConfig> {
        return try {
            val json = prefs.getString(KEY_ORDER_WIDGETS, null)
            android.util.Log.d("WidgetCacheDebug", "getOrderWidgetsFromCache: json=${if (json != null) "${json.take(100)}..." else "NULL"}")
            if (json != null) {
                val type = object : TypeToken<List<WidgetConfig>>() {}.type
                val widgets = gson.fromJson<List<WidgetConfig>>(json, type)
                val result = widgets?.sortedBy { it.order } ?: emptyList()
                android.util.Log.d("WidgetCacheDebug", "getOrderWidgetsFromCache: parsed ${result.size} widgets")
                result.forEach { w ->
                    android.util.Log.d("WidgetCacheDebug", "  CACHED ORDER: id=${w.id}, label=${w.label}")
                }
                result
            } else {
                android.util.Log.d("WidgetCacheDebug", "getOrderWidgetsFromCache: CACHE IS EMPTY/NULL")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetCacheDebug", "getOrderWidgetsFromCache: EXCEPTION", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun saveOrderWidgetsToCache(widgets: List<WidgetConfig>) {
        android.util.Log.d("WidgetCacheDebug", "saveOrderWidgetsToCache: saving ${widgets.size} widgets")
        widgets.forEach { w ->
            android.util.Log.d("WidgetCacheDebug", "  SAVING ORDER: id=${w.id}, label=${w.label}")
        }
        prefs.edit().putString(KEY_ORDER_WIDGETS, gson.toJson(widgets)).apply()
    }
    
    // ==================== Public Read Methods ====================
    
    /**
     * Get all widgets (both levels combined).
     * Use level-specific methods when possible.
     */
    fun getAllWidgets(): List<WidgetConfig> {
        return getItemWidgetsFromCache() + getOrderWidgetsFromCache()
    }
    
    // ==================== Cache-Only Methods (for pill rendering) ====================
    // These methods return ONLY cached/stored widgets, never defaults.
    // Use for rendering pills where we only want to show stored widget values.
    
    /**
     * Get cached item-level widgets only (no defaults).
     * For pill rendering - only show pills for stored widgets.
     */
    fun getCachedItemWidgets(): List<WidgetConfig> {
        return getItemWidgetsFromCache().filter { it.isEnabled }
    }
    
    /**
     * Get cached order-level widgets only (no defaults).
     * For pill rendering - only show pills for stored widgets.
     */
    fun getCachedOrderWidgets(): List<WidgetConfig> {
        return getOrderWidgetsFromCache().filter { it.isEnabled }
    }
    
    /**
     * Get item-level widgets (all, including disabled) sorted by order.
     */
    fun getItemWidgets(): List<WidgetConfig> {
        val cached = getItemWidgetsFromCache()
        val result = if (cached.isEmpty()) {
            android.util.Log.d("WidgetGetDebug", "getItemWidgets: CACHE EMPTY - creating new defaults!")
            DefaultWidgetFactory.createItemLevelDefaults()
        } else {
            android.util.Log.d("WidgetGetDebug", "getItemWidgets: returning ${cached.size} cached widgets")
            cached
        }
        return result
    }
    
    /**
     * Get order-level widgets (all, including disabled) sorted by order.
     */
    fun getOrderWidgets(): List<WidgetConfig> {
        val cached = getOrderWidgetsFromCache()
        val result = if (cached.isEmpty()) {
            android.util.Log.d("WidgetGetDebug", "getOrderWidgets: CACHE EMPTY - creating new defaults!")
            DefaultWidgetFactory.createOrderLevelDefaults()
        } else {
            android.util.Log.d("WidgetGetDebug", "getOrderWidgets: returning ${cached.size} cached widgets")
            cached
        }
        return result
    }
    
    /**
     * Get enabled item-level widgets sorted by order.
     */
    fun getEnabledItemWidgets(): List<WidgetConfig> {
        val widgets = getItemWidgets().filter { it.isEnabled }
        android.util.Log.d("WidgetGetDebug", "getEnabledItemWidgets: returning ${widgets.size} enabled widgets")
        widgets.forEach { w ->
            android.util.Log.d("WidgetGetDebug", "  Widget: id=${w.id}, label=${w.label}")
        }
        return widgets
    }
    
    /**
     * Get enabled order-level widgets sorted by order.
     */
    fun getEnabledOrderWidgets(): List<WidgetConfig> {
        return getOrderWidgets().filter { it.isEnabled }
    }
    
    /**
     * Get filterable item-level widgets (enabled, excluding TEXT_BOX).
     */
    fun getFilterableItemWidgets(): List<WidgetConfig> {
        return getItemWidgets().filter { it.isEnabled && it.type != WidgetType.TEXT_BOX }
    }
    
    /**
     * Get filterable order-level widgets (enabled, excluding TEXT_BOX).
     */
    fun getFilterableOrderWidgets(): List<WidgetConfig> {
        return getOrderWidgets().filter { it.isEnabled && it.type != WidgetType.TEXT_BOX }
    }
    
    /**
     * Check if can add more item-level widgets (max 7).
     */
    fun canAddItemWidget(): Boolean {
        return getItemWidgetsFromCache().size < MAX_WIDGETS_PER_LEVEL
    }
    
    /**
     * Check if can add more order-level widgets (max 7).
     */
    fun canAddOrderWidget(): Boolean {
        return getOrderWidgetsFromCache().size < MAX_WIDGETS_PER_LEVEL
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
     * Check if any item-level widgets are enabled.
     */
    fun hasEnabledItemWidgets(): Boolean {
        return getEnabledItemWidgets().isNotEmpty()
    }
    
    /**
     * Check if any order-level widgets are enabled.
     */
    fun hasEnabledOrderWidgets(): Boolean {
        return getEnabledOrderWidgets().isNotEmpty()
    }
    
    // ==================== Save to Cache ====================
    
    /**
     * Save item widgets to cache.
     */
    fun saveItemWidgets(widgets: List<WidgetConfig>) {
        saveItemWidgetsToCache(widgets)
    }
    
    /**
     * Save order widgets to cache.
     */
    fun saveOrderWidgets(widgets: List<WidgetConfig>) {
        saveOrderWidgetsToCache(widgets)
    }
    
    /**
     * Save settings to cache.
     */
    fun saveSettings(settings: PopupSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }
    
    // ==================== Item Level CRUD ====================
    
    /**
     * Add a new item-level widget.
     */
    fun addItemWidget(type: WidgetType, callback: (WidgetConfig?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        
        if (!canAddItemWidget()) {
            callback(null)
            return
        }
        
        val currentWidgets = getItemWidgetsFromCache()
        val order = (currentWidgets.maxOfOrNull { it.order } ?: -1) + 1
        val widget = DefaultWidgetFactory.createEmpty(type, order, NoteLevel.ITEM)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = currentWidgets.toMutableList()
                widgets.add(widget)
                saveItemWidgetsToCache(widgets)
                callback(widget)
            } else {
                callback(null)
            }
        }
    }
    
    /**
     * Add a pre-configured item-level widget.
     */
    fun addItemWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        if (!canAddItemWidget()) {
            callback(false)
            return
        }
        
        widget.level = NoteLevel.ITEM
        val currentWidgets = getItemWidgetsFromCache()
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = currentWidgets.toMutableList()
                widgets.add(widget)
                saveItemWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    /**
     * Update a single item-level widget by ID.
     * Only updates the specified widget, does not affect others.
     */
    fun updateItemWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        widget.level = NoteLevel.ITEM
        val currentWidgets = getItemWidgetsFromCache().toMutableList()
        val index = currentWidgets.indexOfFirst { it.id == widget.id }
        
        if (index < 0) {
            callback(false)
            return
        }
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                currentWidgets[index] = widget
                saveItemWidgetsToCache(currentWidgets)
            }
            callback(success)
        }
    }
    
    /**
     * Delete a single item-level widget by ID.
     */
    fun deleteItemWidget(widgetId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        val currentWidgets = getItemWidgetsFromCache().toMutableList()
        
        firebase.deleteWidget(mid, widgetId) { success ->
            if (success) {
                currentWidgets.removeAll { it.id == widgetId }
                currentWidgets.forEachIndexed { idx, w -> w.order = idx }
                saveItemWidgetsToCache(currentWidgets)
            }
            callback(success)
        }
    }
    
    /**
     * Reset item-level widgets to defaults.
     * Atomically replaces all item widgets with default set.
     */
    fun resetItemWidgetsToDefaults(callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val defaults = DefaultWidgetFactory.createItemLevelDefaults()
        val currentOrderWidgets = getOrderWidgetsFromCache()
        
        android.util.Log.d("WidgetResetDebug", "========== RESET ITEM WIDGETS ==========")
        android.util.Log.d("WidgetResetDebug", "merchantId: $mid")
        android.util.Log.d("WidgetResetDebug", "NEW defaults being created:")
        defaults.forEach { w ->
            android.util.Log.d("WidgetResetDebug", "  Widget: id=${w.id}, label=${w.label}, type=${w.type}, level=${w.level}")
        }
        android.util.Log.d("WidgetResetDebug", "=========================================")
        
        firebase.replaceAllWidgets(mid, defaults + currentOrderWidgets) { success ->
            android.util.Log.d("WidgetResetDebug", "Firebase replaceAllWidgets success: $success")
            if (success) {
                saveItemWidgetsToCache(defaults)
                android.util.Log.d("WidgetResetDebug", "Saved to cache")
            }
            callback(success)
        }
    }
    
    /**
     * Reorder item-level widgets.
     */
    fun reorderItemWidgets(fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getItemWidgetsFromCache().toMutableList()
        
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= widgets.size || toIndex >= widgets.size) {
            callback(false)
            return
        }
        
        val widget = widgets.removeAt(fromIndex)
        widgets.add(toIndex, widget)
        widgets.forEachIndexed { index, w -> w.order = index }
        
        firebase.saveWidgetsBatch(mid, widgets) { success ->
            if (success) {
                saveItemWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    // ==================== Order Level CRUD ====================
    
    /**
     * Add a new order-level widget.
     */
    fun addOrderWidget(type: WidgetType, callback: (WidgetConfig?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        
        if (!canAddOrderWidget()) {
            callback(null)
            return
        }
        
        val currentWidgets = getOrderWidgetsFromCache()
        val order = (currentWidgets.maxOfOrNull { it.order } ?: -1) + 1
        val widget = DefaultWidgetFactory.createEmpty(type, order, NoteLevel.ORDER)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = currentWidgets.toMutableList()
                widgets.add(widget)
                saveOrderWidgetsToCache(widgets)
                callback(widget)
            } else {
                callback(null)
            }
        }
    }
    
    /**
     * Add a pre-configured order-level widget.
     */
    fun addOrderWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        if (!canAddOrderWidget()) {
            callback(false)
            return
        }
        
        widget.level = NoteLevel.ORDER
        val currentWidgets = getOrderWidgetsFromCache()
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                val widgets = currentWidgets.toMutableList()
                widgets.add(widget)
                saveOrderWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    /**
     * Update a single order-level widget by ID.
     * Only updates the specified widget, does not affect others.
     */
    fun updateOrderWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        widget.level = NoteLevel.ORDER
        val currentWidgets = getOrderWidgetsFromCache().toMutableList()
        val index = currentWidgets.indexOfFirst { it.id == widget.id }
        
        if (index < 0) {
            callback(false)
            return
        }
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                currentWidgets[index] = widget
                saveOrderWidgetsToCache(currentWidgets)
            }
            callback(success)
        }
    }
    
    /**
     * Delete a single order-level widget by ID.
     */
    fun deleteOrderWidget(widgetId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        
        val currentWidgets = getOrderWidgetsFromCache().toMutableList()
        
        firebase.deleteWidget(mid, widgetId) { success ->
            if (success) {
                currentWidgets.removeAll { it.id == widgetId }
                currentWidgets.forEachIndexed { idx, w -> w.order = idx }
                saveOrderWidgetsToCache(currentWidgets)
            }
            callback(success)
        }
    }
    
    /**
     * Reset order-level widgets to defaults.
     * Atomically replaces all order widgets with default set.
     */
    fun resetOrderWidgetsToDefaults(callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val defaults = DefaultWidgetFactory.createOrderLevelDefaults()
        val currentItemWidgets = getItemWidgetsFromCache()
        
        firebase.replaceAllWidgets(mid, currentItemWidgets + defaults) { success ->
            if (success) {
                saveOrderWidgetsToCache(defaults)
            }
            callback(success)
        }
    }
    
    /**
     * Reorder order-level widgets.
     */
    fun reorderOrderWidgets(fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getOrderWidgetsFromCache().toMutableList()
        
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= widgets.size || toIndex >= widgets.size) {
            callback(false)
            return
        }
        
        val widget = widgets.removeAt(fromIndex)
        widgets.add(toIndex, widget)
        widgets.forEachIndexed { index, w -> w.order = index }
        
        firebase.saveWidgetsBatch(mid, widgets) { success ->
            if (success) {
                saveOrderWidgetsToCache(widgets)
            }
            callback(success)
        }
    }
    
    // ==================== Item Level Option CRUD ====================
    
    fun addItemWidgetOption(widgetId: String, label: String, value: String = label, callback: (WidgetOption?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        val widgets = getItemWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(null)
            return
        }
        
        val option = DefaultWidgetFactory.createOption(label, value)
        widget.options.add(option)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                saveItemWidgetsToCache(widgets)
                callback(option)
            } else {
                widget.options.removeAll { it.id == option.id }
                callback(null)
            }
        }
    }
    
    fun updateItemWidgetOption(widgetId: String, option: WidgetOption, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getItemWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val index = widget.options.indexOfFirst { it.id == option.id }
        if (index >= 0) {
            widget.options[index] = option
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveItemWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun deleteItemWidgetOption(widgetId: String, optionId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getItemWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val removed = widget.options.removeAll { it.id == optionId }
        if (removed) {
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveItemWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun reorderItemWidgetOptions(widgetId: String, fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getItemWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= widget.options.size || toIndex >= widget.options.size) {
            callback(false)
            return
        }
        
        val option = widget.options.removeAt(fromIndex)
        widget.options.add(toIndex, option)
        firebase.saveWidget(mid, widget) { success ->
            if (success) saveItemWidgetsToCache(widgets)
            callback(success)
        }
    }
    
    // ==================== Order Level Option CRUD ====================
    
    fun addOrderWidgetOption(widgetId: String, label: String, value: String = label, callback: (WidgetOption?) -> Unit) {
        val mid = merchantId ?: return callback(null)
        val widgets = getOrderWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(null)
            return
        }
        
        val option = DefaultWidgetFactory.createOption(label, value)
        widget.options.add(option)
        
        firebase.saveWidget(mid, widget) { success ->
            if (success) {
                saveOrderWidgetsToCache(widgets)
                callback(option)
            } else {
                widget.options.removeAll { it.id == option.id }
                callback(null)
            }
        }
    }
    
    fun updateOrderWidgetOption(widgetId: String, option: WidgetOption, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getOrderWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val index = widget.options.indexOfFirst { it.id == option.id }
        if (index >= 0) {
            widget.options[index] = option
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveOrderWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun deleteOrderWidgetOption(widgetId: String, optionId: String, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getOrderWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val removed = widget.options.removeAll { it.id == optionId }
        if (removed) {
            firebase.saveWidget(mid, widget) { success ->
                if (success) saveOrderWidgetsToCache(widgets)
                callback(success)
            }
        } else {
            callback(false)
        }
    }
    
    fun reorderOrderWidgetOptions(widgetId: String, fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        val widgets = getOrderWidgetsFromCache().toMutableList()
        val widget = widgets.find { it.id == widgetId }
        if (widget == null || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= widget.options.size || toIndex >= widget.options.size) {
            callback(false)
            return
        }
        
        val option = widget.options.removeAt(fromIndex)
        widget.options.add(toIndex, option)
        firebase.saveWidget(mid, widget) { success ->
            if (success) saveOrderWidgetsToCache(widgets)
            callback(success)
        }
    }
    
    // ==================== Settings ====================
    
    fun updateSettings(settings: PopupSettings, callback: (Boolean) -> Unit) {
        val mid = merchantId ?: return callback(false)
        firebase.saveSettings(mid, settings) { success ->
            if (success) {
                saveSettings(settings)
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
    
    fun getItemWidgetById(widgetId: String): WidgetConfig? {
        return getItemWidgetsFromCache().find { it.id == widgetId }
    }
    
    fun getOrderWidgetById(widgetId: String): WidgetConfig? {
        return getOrderWidgetsFromCache().find { it.id == widgetId }
    }
    
    fun getItemWidgetByLabel(label: String): WidgetConfig? {
        return getItemWidgetsFromCache().find { it.label.equals(label, ignoreCase = true) }
    }
    
    fun getOrderWidgetByLabel(label: String): WidgetConfig? {
        return getOrderWidgetsFromCache().find { it.label.equals(label, ignoreCase = true) }
    }
    
    fun getItemWidgetCount(): Int = getItemWidgetsFromCache().size
    
    fun getOrderWidgetCount(): Int = getOrderWidgetsFromCache().size
    
    fun getEnabledItemWidgetCount(): Int = getItemWidgetsFromCache().count { it.isEnabled }
    
    fun getEnabledOrderWidgetCount(): Int = getOrderWidgetsFromCache().count { it.isEnabled }
    
    // ==================== Cross-Level Convenience Methods ====================
    
    /**
     * Get all filterable widgets (both levels, enabled, excluding TEXT_BOX).
     * Used by filter dialogs that show widgets from both levels.
     */
    fun getFilterableWidgets(): List<WidgetConfig> {
        return getFilterableItemWidgets() + getFilterableOrderWidgets()
    }
    
    /**
     * Get widget by ID from either level.
     * Used when widget level is unknown.
     */
    fun getWidgetById(widgetId: String): WidgetConfig? {
        return getItemWidgetById(widgetId) ?: getOrderWidgetById(widgetId)
    }
    
    /**
     * Force reload item widgets from Firebase
     */
    fun reloadItemWidgets(callback: ((Boolean) -> Unit)? = null) {
        android.util.Log.d("WidgetReloadDebug", "reloadItemWidgets: starting...")
        val mid = merchantId ?: run {
            android.util.Log.w("WidgetReloadDebug", "reloadItemWidgets: merchantId is NULL!")
            return
        }
        firebase.getItemWidgets(mid) { widgets ->
            android.util.Log.d("WidgetReloadDebug", "reloadItemWidgets: received ${widgets.size} widgets from Firebase, saving to cache")
            saveItemWidgetsToCache(widgets)
            callback?.invoke(true)
        }
    }
    
    /**
     * Force reload order widgets from Firebase
     */
    fun reloadOrderWidgets(callback: ((Boolean) -> Unit)? = null) {
        android.util.Log.d("WidgetReloadDebug", "reloadOrderWidgets: starting...")
        val mid = merchantId ?: run {
            android.util.Log.w("WidgetReloadDebug", "reloadOrderWidgets: merchantId is NULL!")
            return
        }
        firebase.getOrderWidgets(mid) { widgets ->
            android.util.Log.d("WidgetReloadDebug", "reloadOrderWidgets: received ${widgets.size} widgets from Firebase, saving to cache")
            saveOrderWidgetsToCache(widgets)
            callback?.invoke(true)
        }
    }
    
    /**
     * Force reload all widgets and settings from Firebase
     */
    fun reloadAll(callback: ((Boolean) -> Unit)? = null) {
        android.util.Log.d("WidgetReloadDebug", "reloadAll: starting...")
        val mid = merchantId ?: run {
            android.util.Log.w("WidgetReloadDebug", "reloadAll: merchantId is NULL!")
            return
        }
        firebase.getItemWidgets(mid) { itemWidgets ->
            android.util.Log.d("WidgetReloadDebug", "reloadAll: received ${itemWidgets.size} ITEM widgets")
            saveItemWidgetsToCache(itemWidgets)
            firebase.getOrderWidgets(mid) { orderWidgets ->
                android.util.Log.d("WidgetReloadDebug", "reloadAll: received ${orderWidgets.size} ORDER widgets")
                saveOrderWidgetsToCache(orderWidgets)
                firebase.getSettings(mid) { settings ->
                    saveSettings(settings)
                    android.util.Log.d("WidgetReloadDebug", "reloadAll: COMPLETE")
                    callback?.invoke(true)
                }
            }
        }
    }
}
