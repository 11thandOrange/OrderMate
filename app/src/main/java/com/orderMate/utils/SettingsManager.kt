package com.orderMate.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Settings Manager (Issue #83)
 * 
 * Manages app settings stored in SharedPreferences.
 * 
 * Settings include:
 * - General: Register integration options
 * - Pop Up: Widget configuration (up to 7 widgets)
 * - Advanced: Notification frequency, receipt settings
 * - Notification: SMS number, templates
 */
class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    // ==================== General Settings ====================

    fun getUseOrderMateRegister(): Boolean {
        return prefs.getBoolean(KEY_USE_ORDERMATE_REGISTER, true)
    }

    fun setUseOrderMateRegister(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_USE_ORDERMATE_REGISTER, enabled) }
    }

    fun getUseBothRegisters(): Boolean {
        return prefs.getBoolean(KEY_USE_BOTH_REGISTERS, false)
    }

    fun setUseBothRegisters(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_USE_BOTH_REGISTERS, enabled) }
    }

    // ==================== Pop Up Widget Settings ====================

    fun getWidgets(): List<PopUpWidget> {
        val json = prefs.getString(KEY_WIDGETS, null) ?: return getDefaultWidgets()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PopUpWidget(
                    id = obj.getInt("id"),
                    type = WidgetType.valueOf(obj.getString("type")),
                    label = obj.getString("label"),
                    values = obj.optJSONArray("values")?.let { valArray ->
                        (0 until valArray.length()).map { valArray.getString(it) }
                    } ?: emptyList(),
                    enabled = obj.getBoolean("enabled"),
                    order = obj.getInt("order")
                )
            }
        } catch (e: Exception) {
            getDefaultWidgets()
        }
    }

    fun saveWidgets(widgets: List<PopUpWidget>) {
        val array = JSONArray()
        widgets.forEach { widget ->
            val obj = JSONObject().apply {
                put("id", widget.id)
                put("type", widget.type.name)
                put("label", widget.label)
                put("values", JSONArray(widget.values))
                put("enabled", widget.enabled)
                put("order", widget.order)
            }
            array.put(obj)
        }
        prefs.edit { putString(KEY_WIDGETS, array.toString()) }
    }

    fun addWidget(type: WidgetType): PopUpWidget? {
        val widgets = getWidgets().toMutableList()
        if (widgets.size >= MAX_WIDGETS) return null

        val newWidget = PopUpWidget(
            id = (widgets.maxOfOrNull { it.id } ?: 0) + 1,
            type = type,
            label = type.defaultLabel,
            values = emptyList(),
            enabled = true,
            order = widgets.size
        )
        widgets.add(newWidget)
        saveWidgets(widgets)
        return newWidget
    }

    fun updateWidget(widget: PopUpWidget) {
        val widgets = getWidgets().toMutableList()
        val index = widgets.indexOfFirst { it.id == widget.id }
        if (index >= 0) {
            widgets[index] = widget
            saveWidgets(widgets)
        }
    }

    fun removeWidget(widgetId: Int) {
        val widgets = getWidgets().toMutableList()
        widgets.removeAll { it.id == widgetId }
        // Reorder remaining widgets
        widgets.forEachIndexed { index, widget ->
            widget.order = index
        }
        saveWidgets(widgets)
    }

    fun reorderWidgets(fromPosition: Int, toPosition: Int) {
        val widgets = getWidgets().toMutableList()
        val widget = widgets.removeAt(fromPosition)
        widgets.add(toPosition, widget)
        widgets.forEachIndexed { index, w -> w.order = index }
        saveWidgets(widgets)
    }

    private fun getDefaultWidgets(): List<PopUpWidget> {
        return listOf(
            PopUpWidget(1, WidgetType.CALENDAR, "Due Date/Time", emptyList(), true, 0),
            PopUpWidget(2, WidgetType.SINGLE_SELECT, "Category", listOf("Birthday", "Wedding", "Custom"), true, 1),
            PopUpWidget(3, WidgetType.MULTI_SELECT, "Tags", listOf("Rush", "VIP", "Delivery"), true, 2),
            PopUpWidget(4, WidgetType.TEXT_BOX, "Description", emptyList(), true, 3)
        )
    }

    // ==================== Advanced Settings ====================

    fun getNotificationDays(): Int {
        return prefs.getInt(KEY_NOTIFICATION_DAYS, DEFAULT_NOTIFICATION_DAYS)
    }

    fun setNotificationDays(days: Int) {
        prefs.edit { putInt(KEY_NOTIFICATION_DAYS, days.coerceIn(1, 30)) }
    }

    fun getReceiptTime(): Int {
        return prefs.getInt(KEY_RECEIPT_TIME, DEFAULT_RECEIPT_TIME)
    }

    fun setReceiptTime(time: Int) {
        prefs.edit { putInt(KEY_RECEIPT_TIME, time.coerceIn(1, 999)) }
    }

    fun getReceiptUnit(): String {
        return prefs.getString(KEY_RECEIPT_UNIT, DEFAULT_RECEIPT_UNIT) ?: DEFAULT_RECEIPT_UNIT
    }

    fun setReceiptUnit(unit: String) {
        if (unit in listOf("minutes", "hours", "days")) {
            prefs.edit { putString(KEY_RECEIPT_UNIT, unit) }
        }
    }

    // ==================== Notification Settings ====================

    fun getSmsNumber(): String {
        return prefs.getString(KEY_SMS_NUMBER, "") ?: ""
    }

    fun setSmsNumber(number: String) {
        prefs.edit { putString(KEY_SMS_NUMBER, number) }
    }

    fun getNotificationTemplate(): String {
        return prefs.getString(KEY_NOTIFICATION_TEMPLATE, DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
    }

    fun setNotificationTemplate(template: String) {
        val trimmed = template.take(MAX_TEMPLATE_LENGTH)
        prefs.edit { putString(KEY_NOTIFICATION_TEMPLATE, trimmed) }
    }

    // ==================== iOS-style Redesign Settings (#80 requirement) ====================

    fun getNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun getAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC_ENABLED, enabled) }
    }

    fun getThemeColor(): Int {
        return prefs.getInt(KEY_THEME_COLOR, DEFAULT_THEME_COLOR)
    }

    fun setThemeColor(color: Int) {
        prefs.edit { putInt(KEY_THEME_COLOR, color) }
    }

    // ==================== Profile/Avatar Settings ====================

    fun setCustomAvatarUri(uri: Uri) {
        prefs.edit { putString(KEY_CUSTOM_AVATAR_URI, uri.toString()) }
    }

    fun getCustomAvatarUri(): Uri? {
        val uriString = prefs.getString(KEY_CUSTOM_AVATAR_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    fun clearCustomAvatarUri() {
        prefs.edit { remove(KEY_CUSTOM_AVATAR_URI) }
    }

    fun setAvatarEmoji(emoji: String) {
        prefs.edit { putString(KEY_AVATAR_EMOJI, emoji) }
    }

    fun getAvatarEmoji(): String? {
        return prefs.getString(KEY_AVATAR_EMOJI, null)
    }

    // ==================== Color Scheme Settings ====================

    fun getColorScheme(): String {
        return prefs.getString(KEY_COLOR_SCHEME, DEFAULT_COLOR_SCHEME) ?: DEFAULT_COLOR_SCHEME
    }

    fun setColorScheme(schemeId: String) {
        prefs.edit { putString(KEY_COLOR_SCHEME, schemeId) }
    }

    fun isThemeTargetEnabled(target: String): Boolean {
        return prefs.getBoolean("${KEY_THEME_TARGET_PREFIX}$target", true)
    }

    fun setThemeTargetEnabled(target: String, enabled: Boolean) {
        prefs.edit { putBoolean("${KEY_THEME_TARGET_PREFIX}$target", enabled) }
    }

    // ==================== Commit (apply pending changes) ====================

    fun commit() {
        // SharedPreferences with edit{} already applies changes
        // This method exists for explicit save semantics in UI
    }

    // ==================== Reset ====================

    fun resetToDefaults() {
        prefs.edit {
            putBoolean(KEY_USE_ORDERMATE_REGISTER, true)
            putBoolean(KEY_USE_BOTH_REGISTERS, false)
            remove(KEY_WIDGETS)
            putInt(KEY_NOTIFICATION_DAYS, DEFAULT_NOTIFICATION_DAYS)
            putInt(KEY_RECEIPT_TIME, DEFAULT_RECEIPT_TIME)
            putString(KEY_RECEIPT_UNIT, DEFAULT_RECEIPT_UNIT)
            putString(KEY_SMS_NUMBER, "")
            putString(KEY_NOTIFICATION_TEMPLATE, DEFAULT_TEMPLATE)
            putBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            putBoolean(KEY_AUTO_SYNC_ENABLED, true)
            putInt(KEY_THEME_COLOR, DEFAULT_THEME_COLOR)
            remove(KEY_CUSTOM_AVATAR_URI)
            remove(KEY_AVATAR_EMOJI)
            putString(KEY_COLOR_SCHEME, DEFAULT_COLOR_SCHEME)
        }
    }

    companion object {
        private const val PREFS_NAME = "ordermate_settings"

        // Keys
        private const val KEY_USE_ORDERMATE_REGISTER = "use_ordermate_register"
        private const val KEY_USE_BOTH_REGISTERS = "use_both_registers"
        private const val KEY_WIDGETS = "popup_widgets"
        private const val KEY_NOTIFICATION_DAYS = "notification_days"
        private const val KEY_RECEIPT_TIME = "receipt_time"
        private const val KEY_RECEIPT_UNIT = "receipt_unit"
        private const val KEY_SMS_NUMBER = "sms_number"
        private const val KEY_NOTIFICATION_TEMPLATE = "notification_template"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_CUSTOM_AVATAR_URI = "custom_avatar_uri"
        private const val KEY_AVATAR_EMOJI = "avatar_emoji"
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_THEME_TARGET_PREFIX = "theme_target_"

        // Defaults
        private const val DEFAULT_NOTIFICATION_DAYS = 3
        private const val DEFAULT_RECEIPT_TIME = 60
        private const val DEFAULT_RECEIPT_UNIT = "minutes"
        private const val DEFAULT_TEMPLATE = "Your order from {{merchant_name}} is ready for pickup!"
        private const val DEFAULT_THEME_COLOR = 0xFFFF9F43.toInt() // Orange accent
        private const val DEFAULT_COLOR_SCHEME = "purple"
        
        const val MAX_WIDGETS = 7
        const val MAX_TEMPLATE_LENGTH = 250
    }
}

/**
 * Widget types for Pop Up editor
 */
enum class WidgetType(val defaultLabel: String) {
    CALENDAR("Due Date/Time"),
    SINGLE_SELECT("Category"),
    MULTI_SELECT("Tags"),
    TEXT_BOX("Description")
}

/**
 * Pop Up Widget data class
 */
data class PopUpWidget(
    val id: Int,
    val type: WidgetType,
    var label: String,
    var values: List<String>,
    var enabled: Boolean,
    var order: Int
)
