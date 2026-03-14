package com.orderMate.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

/**
 * Profile Settings Manager (Issue #85)
 * 
 * Manages user-specific profile settings stored in SharedPreferences.
 * 
 * Settings include:
 * - Theme color (any hex color via color picker)
 * - Avatar emoji (any emoji via emoji picker)
 * 
 * Each user's settings are stored separately.
 */
class ProfileSettingsManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    // ==================== Theme Color ====================

    /**
     * Get the current theme color
     * @return Hex color string (e.g., "#667eea")
     */
    fun getThemeColor(): String {
        return prefs.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR) ?: DEFAULT_THEME_COLOR
    }

    /**
     * Set the theme color
     * @param hexColor Hex color string (e.g., "#667eea")
     */
    fun setThemeColor(hexColor: String) {
        if (isValidHexColor(hexColor)) {
            prefs.edit { putString(KEY_THEME_COLOR, hexColor) }
        }
    }

    /**
     * Get the theme gradient based on the current color
     * @return Pair of colors (base color, lighter color) for gradient
     */
    fun getThemeGradient(): Pair<String, String> {
        val baseColor = getThemeColor()
        val lighterColor = lightenColor(baseColor, 30)
        return Pair(baseColor, lighterColor)
    }

    // ==================== Avatar ====================

    /**
     * Get the current avatar emoji
     * @return Avatar emoji string
     */
    fun getAvatar(): String {
        return prefs.getString(KEY_AVATAR, DEFAULT_AVATAR) ?: DEFAULT_AVATAR
    }

    /**
     * Set the avatar emoji
     * @param emoji Any emoji string
     */
    fun setAvatar(emoji: String) {
        if (emoji.isNotEmpty()) {
            prefs.edit { putString(KEY_AVATAR, emoji) }
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Lighten a hex color by a percentage
     * @param hex Hex color string
     * @param percent Percentage to lighten (0-100)
     * @return Lightened hex color string
     */
    fun lightenColor(hex: String, percent: Int): String {
        return try {
            val color = Color.parseColor(hex)
            val r = minOf(255, Color.red(color) + (255 * percent / 100))
            val g = minOf(255, Color.green(color) + (255 * percent / 100))
            val b = minOf(255, Color.blue(color) + (255 * percent / 100))
            String.format("#%02x%02x%02x", r, g, b)
        } catch (e: Exception) {
            hex
        }
    }

    /**
     * Validate hex color format
     * @param color Color string to validate
     * @return true if valid hex color
     */
    fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        prefs.edit {
            putString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR)
            putString(KEY_AVATAR, DEFAULT_AVATAR)
        }
    }

    /**
     * Export settings as a map (for backup/sync)
     * @return Map of all settings
     */
    fun exportSettings(): Map<String, String> {
        return mapOf(
            KEY_THEME_COLOR to getThemeColor(),
            KEY_AVATAR to getAvatar()
        )
    }

    /**
     * Import settings from a map (for restore/sync)
     * @param settings Map of settings to import
     */
    fun importSettings(settings: Map<String, String>) {
        prefs.edit {
            settings[KEY_THEME_COLOR]?.let { 
                if (isValidHexColor(it)) putString(KEY_THEME_COLOR, it) 
            }
            settings[KEY_AVATAR]?.let { 
                if (it.isNotEmpty()) putString(KEY_AVATAR, it) 
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "ordermate_profile_settings_v2"
        
        // Keys
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_AVATAR = "avatar"

        // Defaults
        private const val DEFAULT_THEME_COLOR = "#667eea"
        private const val DEFAULT_AVATAR = "😊"

        // Emoji categories for picker
        val EMOJI_PEOPLE = listOf(
            "😀", "😃", "😄", "😁", "😊", "😇", "🥰", "😍", "🤩", "😎",
            "🧐", "🤓", "😏", "😌", "😴", "🤤", "😋", "😛", "😜", "🤪",
            "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑"
        )

        val EMOJI_FOOD = listOf(
            "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑",
            "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒",
            "🍕", "🍔", "🍟", "🌭", "🍿", "🧁", "🍰", "🎂", "🍩", "🍪"
        )

        val EMOJI_ACTIVITIES = listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
            "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "⛳", "🏹", "🎣", "🥊",
            "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸"
        )

        val EMOJI_OBJECTS = listOf(
            "⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "📷", "📹", "🎥",
            "📺", "📻", "🎙️", "⏰", "⌛", "📡", "🔋", "💡", "🔦", "🕯️",
            "💎", "💰", "💳", "🔧", "🔨", "⚙️", "🔫", "💣", "🔮", "📿"
        )
    }
}
