package com.orderMate.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType

/**
 * Business logic for widget operations
 * Loads from local cache synchronously, syncs with Firebase in background
 */
class WidgetManager(private val merchantId: String) {
    
    private val firebase = FirebaseConfigManager.getInstance()
    private var _widgets = mutableListOf<WidgetConfig>()
    private var _settings = PopupSettings()
    private var _isLoaded = false
    
    companion object {
        private const val PREFS_NAME = "widget_cache"
        private const val KEY_WIDGETS = "cached_widgets"
        private const val KEY_SETTINGS = "cached_settings"
        private val gson = Gson()
        
        private var prefs: SharedPreferences? = null
        
        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    init {
        // Load from local cache synchronously on init
        loadFromCache()
    }
    
    // ==================== Getters ====================
    
    val widgets: List<WidgetConfig> 
        get() = _widgets.toList()
    
    val enabledWidgets: List<WidgetConfig> 
        get() = _widgets.filter { it.isEnabled }.sortedBy { it.order }
    
    // All enabled widgets appear in filters (except TEXT_BOX which can't be filtered)
    val filterableWidgets: List<WidgetConfig>
        get() = _widgets
            .filter { it.isEnabled && it.type != WidgetType.TEXT_BOX }
            .sortedBy { it.order }
    
    val settings: PopupSettings 
        get() = _settings
    
    val isLoaded: Boolean 
        get() = _isLoaded
    
    // ==================== Local Cache ====================
    
    private fun loadFromCache() {
        try {
            val widgetsJson = prefs?.getString(KEY_WIDGETS, null)
            val settingsJson = prefs?.getString(KEY_SETTINGS, null)
            
            if (widgetsJson != null) {
                val type = object : TypeToken<List<WidgetConfig>>() {}.type
                _widgets = gson.fromJson<List<WidgetConfig>>(widgetsJson, type)?.toMutableList() 
                    ?: mutableListOf()
            }
            
            if (settingsJson != null) {
                _settings = gson.fromJson(settingsJson, PopupSettings::class.java) ?: PopupSettings()
            }
            
            _isLoaded = _widgets.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveToCache() {
        try {
            prefs?.edit()?.apply {
                putString(KEY_WIDGETS, gson.toJson(_widgets))
                putString(KEY_SETTINGS, gson.toJson(_settings))
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ==================== Load from Firebase ====================
    
    /**
     * Load from Firebase and update local cache
     * Call this in background to sync with server
     */
    fun load(callback: (Boolean) -> Unit) {
        firebase.getWidgets(merchantId) { widgets ->
            firebase.getSettings(merchantId) { settings ->
                _widgets = widgets.toMutableList()
                _settings = settings
                _isLoaded = true
                saveToCache()
                callback(true)
            }
        }
    }
    
    fun reload(callback: (Boolean) -> Unit) {
        _isLoaded = false
        load(callback)
    }
    
    // ==================== Widget CRUD ====================
    
    fun addWidget(type: WidgetType, callback: (WidgetConfig?) -> Unit) {
        val order = (_widgets.maxOfOrNull { it.order } ?: -1) + 1
        val widget = DefaultWidgetFactory.createEmpty(type, order)
        
        firebase.saveWidget(merchantId, widget) { success ->
            if (success) {
                _widgets.add(widget)
                saveToCache()
                callback(widget)
            } else {
                callback(null)
            }
        }
    }
    
    fun addWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        firebase.saveWidget(merchantId, widget) { success ->
            if (success) {
                _widgets.add(widget)
                saveToCache()
            }
            callback(success)
        }
    }
    
    fun updateWidget(widget: WidgetConfig, callback: (Boolean) -> Unit) {
        firebase.saveWidget(merchantId, widget) { success ->
            if (success) {
                val index = _widgets.indexOfFirst { it.id == widget.id }
                if (index >= 0) {
                    _widgets[index] = widget
                }
                saveToCache()
            }
            callback(success)
        }
    }
    
    fun updateWidgetLabel(widgetId: String, newLabel: String, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        widget.label = newLabel
        updateWidget(widget, callback)
    }
    
    fun toggleWidgetEnabled(widgetId: String, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        widget.isEnabled = !widget.isEnabled
        updateWidget(widget, callback)
    }
    
    fun toggleShowInFilter(widgetId: String, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        widget.showInFilter = !widget.showInFilter
        updateWidget(widget, callback)
    }
    
    fun deleteWidget(widgetId: String, callback: (Boolean) -> Unit) {
        firebase.deleteWidget(merchantId, widgetId) { success ->
            if (success) {
                _widgets.removeAll { it.id == widgetId }
                reorderWidgetsInternal()
                saveToCache()
            }
            callback(success)
        }
    }
    
    fun reorderWidgets(fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= _widgets.size || toIndex >= _widgets.size) {
            callback(false)
            return
        }
        
        val widget = _widgets.removeAt(fromIndex)
        _widgets.add(toIndex, widget)
        reorderWidgetsInternal()
        saveToCache()
        
        firebase.saveWidgetsBatch(merchantId, _widgets, callback)
    }
    
    private fun reorderWidgetsInternal() {
        _widgets.forEachIndexed { index, widget -> widget.order = index }
    }
    
    // ==================== Option CRUD ====================
    
    fun addOption(widgetId: String, label: String, value: String = label, callback: (WidgetOption?) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(null)
            return
        }
        
        val option = DefaultWidgetFactory.createOption(label, value)
        widget.options.add(option)
        
        firebase.saveWidget(merchantId, widget) { success ->
            if (success) {
                callback(option)
            } else {
                widget.options.removeAll { it.id == option.id }
                callback(null)
            }
        }
    }
    
    fun updateOption(widgetId: String, option: WidgetOption, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val index = widget.options.indexOfFirst { it.id == option.id }
        if (index >= 0) {
            widget.options[index] = option
            firebase.saveWidget(merchantId, widget, callback)
        } else {
            callback(false)
        }
    }
    
    fun deleteOption(widgetId: String, optionId: String, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null) {
            callback(false)
            return
        }
        
        val removed = widget.options.removeAll { it.id == optionId }
        if (removed) {
            firebase.saveWidget(merchantId, widget, callback)
        } else {
            callback(false)
        }
    }
    
    fun reorderOptions(widgetId: String, fromIndex: Int, toIndex: Int, callback: (Boolean) -> Unit) {
        val widget = _widgets.find { it.id == widgetId }
        if (widget == null || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= widget.options.size || toIndex >= widget.options.size) {
            callback(false)
            return
        }
        
        val option = widget.options.removeAt(fromIndex)
        widget.options.add(toIndex, option)
        firebase.saveWidget(merchantId, widget, callback)
    }
    
    // ==================== Settings ====================
    
    fun updateSettings(settings: PopupSettings, callback: (Boolean) -> Unit) {
        firebase.saveSettings(merchantId, settings) { success ->
            if (success) {
                _settings = settings
            }
            callback(success)
        }
    }
    
    fun setTriggerOnItemAdd(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = _settings.copy(triggerOnItemAdd = enabled)
        updateSettings(newSettings, callback)
    }
    
    fun setShowOMButtonInRegister(enabled: Boolean, callback: (Boolean) -> Unit) {
        val newSettings = _settings.copy(showOMButtonInRegister = enabled)
        updateSettings(newSettings, callback)
    }
    
    // ==================== Helpers ====================
    
    fun getWidgetById(widgetId: String): WidgetConfig? {
        return _widgets.find { it.id == widgetId }
    }
    
    fun getWidgetByLabel(label: String): WidgetConfig? {
        return _widgets.find { it.label.equals(label, ignoreCase = true) }
    }
    
    fun getOptionsForWidget(widgetId: String): List<WidgetOption> {
        return getWidgetById(widgetId)?.options ?: emptyList()
    }
    
    fun hasWidgets(): Boolean = _widgets.isNotEmpty()
    
    fun getWidgetCount(): Int = _widgets.size
    
    fun getEnabledWidgetCount(): Int = _widgets.count { it.isEnabled }
}
