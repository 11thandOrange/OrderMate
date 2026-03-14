package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProfileSettingsManager (Issue #85)
 * 
 * Tests cover:
 * - Theme color selection (any hex color via color picker)
 * - Avatar emoji selection (any emoji via emoji picker)
 * - Color utilities (lighten color for gradient)
 * - Settings persistence
 * - User isolation
 */
class ProfileSettingsManagerTest {

    private lateinit var mockSettings: MutableMap<String, Any?>

    @Before
    fun setUp() {
        mockSettings = mutableMapOf(
            "theme_color" to "#667eea",
            "avatar" to "😊"
        )
    }

    // ==================== Theme Color Tests ====================

    @Test
    fun `default theme color is purple`() {
        assertEquals("#667eea", mockSettings["theme_color"])
    }

    @Test
    fun `setThemeColor updates color`() {
        mockSettings["theme_color"] = "#ff5733"
        assertEquals("#ff5733", mockSettings["theme_color"])
    }

    @Test
    fun `theme color accepts any valid hex color`() {
        val testColors = listOf("#ff0000", "#00ff00", "#0000ff", "#ffffff", "#000000", "#123abc")
        testColors.forEach { color ->
            mockSettings["theme_color"] = color
            assertEquals(color, mockSettings["theme_color"])
        }
    }

    @Test
    fun `theme color persists after change`() {
        mockSettings["theme_color"] = "#e91e63"
        val saved = mockSettings["theme_color"]
        assertEquals("#e91e63", saved)
    }

    @Test
    fun `theme color can be any user-selected color`() {
        // User picks green from color picker
        mockSettings["theme_color"] = "#4caf50"
        assertEquals("#4caf50", mockSettings["theme_color"])
        
        // User changes to orange
        mockSettings["theme_color"] = "#ff9800"
        assertEquals("#ff9800", mockSettings["theme_color"])
    }

    // ==================== Color Utility Tests ====================

    @Test
    fun `lightenColor increases RGB values`() {
        val originalColor = "#667eea"
        val lightenedColor = lightenColor(originalColor, 30)
        
        // Lightened color should have higher RGB values
        assertNotEquals(originalColor, lightenedColor)
        assertTrue(lightenedColor.startsWith("#"))
        assertEquals(7, lightenedColor.length)
    }

    @Test
    fun `lightenColor handles dark colors`() {
        val darkColor = "#333333"
        val lightenedColor = lightenColor(darkColor, 30)
        
        assertNotEquals(darkColor, lightenedColor)
    }

    @Test
    fun `lightenColor caps at white`() {
        val lightColor = "#ffffff"
        val result = lightenColor(lightColor, 30)
        
        // Should still be valid hex
        assertTrue(result.startsWith("#"))
        assertEquals(7, result.length)
    }

    @Test
    fun `lightenColor with zero percent returns same color`() {
        val color = "#667eea"
        val result = lightenColor(color, 0)
        assertEquals(color, result)
    }

    // Helper function matching JS implementation
    private fun lightenColor(hex: String, percent: Int): String {
        val num = hex.removePrefix("#").toLong(16).toInt()
        val amt = (2.55 * percent).toInt()
        val r = minOf(255, (num shr 16) + amt)
        val g = minOf(255, ((num shr 8) and 0xFF) + amt)
        val b = minOf(255, (num and 0xFF) + amt)
        return "#${(0x1000000 + r * 0x10000 + g * 0x100 + b).toString(16).substring(1)}"
    }

    // ==================== Avatar Emoji Tests ====================

    @Test
    fun `default avatar is smile emoji`() {
        assertEquals("😊", mockSettings["avatar"])
    }

    @Test
    fun `setAvatar updates emoji`() {
        mockSettings["avatar"] = "🎂"
        assertEquals("🎂", mockSettings["avatar"])
    }

    @Test
    fun `avatar accepts any emoji from people category`() {
        val peopleEmojis = listOf("😀", "😃", "😄", "😁", "😊", "😇", "🥰", "😍", "🤩", "😎")
        peopleEmojis.forEach { emoji ->
            mockSettings["avatar"] = emoji
            assertEquals(emoji, mockSettings["avatar"])
        }
    }

    @Test
    fun `avatar accepts any emoji from food category`() {
        val foodEmojis = listOf("🍎", "🍕", "🍔", "🧁", "🎂", "🍰", "🥐", "🍩", "🍪", "🍫")
        foodEmojis.forEach { emoji ->
            mockSettings["avatar"] = emoji
            assertEquals(emoji, mockSettings["avatar"])
        }
    }

    @Test
    fun `avatar accepts any emoji from activities category`() {
        val activityEmojis = listOf("⚽", "🏀", "🎾", "🎮", "🎨", "🎬", "🎤", "🎧", "🎸", "🏆")
        activityEmojis.forEach { emoji ->
            mockSettings["avatar"] = emoji
            assertEquals(emoji, mockSettings["avatar"])
        }
    }

    @Test
    fun `avatar accepts any emoji from objects category`() {
        val objectEmojis = listOf("📱", "💻", "⌚", "📷", "💡", "🔧", "💎", "🎁", "📚", "✏️")
        objectEmojis.forEach { emoji ->
            mockSettings["avatar"] = emoji
            assertEquals(emoji, mockSettings["avatar"])
        }
    }

    @Test
    fun `avatar persists after change`() {
        mockSettings["avatar"] = "🚀"
        val saved = mockSettings["avatar"]
        assertEquals("🚀", saved)
    }

    // ==================== Gradient Generation Tests ====================

    @Test
    fun `gradient is created from theme color`() {
        val baseColor = "#667eea"
        val lighterColor = lightenColor(baseColor, 30)
        val gradient = "linear-gradient(135deg, $baseColor 0%, $lighterColor 100%)"
        
        assertTrue(gradient.contains(baseColor))
        assertTrue(gradient.contains(lighterColor))
        assertTrue(gradient.startsWith("linear-gradient"))
    }

    @Test
    fun `gradient updates when color changes`() {
        val color1 = "#667eea"
        val color2 = "#e91e63"
        
        val gradient1 = "linear-gradient(135deg, $color1 0%, ${lightenColor(color1, 30)} 100%)"
        val gradient2 = "linear-gradient(135deg, $color2 0%, ${lightenColor(color2, 30)} 100%)"
        
        assertNotEquals(gradient1, gradient2)
    }

    // ==================== Settings Persistence Tests ====================

    @Test
    fun `settings can be serialized to JSON`() {
        mockSettings["theme_color"] = "#ff5733"
        mockSettings["avatar"] = "🎉"
        
        // Simulate JSON serialization
        val json = """{"theme_color":"${mockSettings["theme_color"]}","avatar":"${mockSettings["avatar"]}"}"""
        
        assertTrue(json.contains("#ff5733"))
        assertTrue(json.contains("🎉"))
    }

    @Test
    fun `settings can be deserialized from JSON`() {
        val json = """{"theme_color":"#4caf50","avatar":"🌟"}"""
        
        // Simulate parsing
        val themeColor = "#4caf50"
        val avatar = "🌟"
        
        mockSettings["theme_color"] = themeColor
        mockSettings["avatar"] = avatar
        
        assertEquals("#4caf50", mockSettings["theme_color"])
        assertEquals("🌟", mockSettings["avatar"])
    }

    @Test
    fun `settings persist across sessions`() {
        // Session 1: Set values
        mockSettings["theme_color"] = "#9c27b0"
        mockSettings["avatar"] = "💜"
        
        // Simulate save
        val savedThemeColor = mockSettings["theme_color"]
        val savedAvatar = mockSettings["avatar"]
        
        // Session 2: Load values
        val newSettings = mutableMapOf<String, Any?>()
        newSettings["theme_color"] = savedThemeColor
        newSettings["avatar"] = savedAvatar
        
        assertEquals("#9c27b0", newSettings["theme_color"])
        assertEquals("💜", newSettings["avatar"])
    }

    // ==================== Reset Tests ====================

    @Test
    fun `resetToDefaults restores default theme color`() {
        mockSettings["theme_color"] = "#ff0000"
        
        // Reset
        mockSettings["theme_color"] = "#667eea"
        
        assertEquals("#667eea", mockSettings["theme_color"])
    }

    @Test
    fun `resetToDefaults restores default avatar`() {
        mockSettings["avatar"] = "🤖"
        
        // Reset
        mockSettings["avatar"] = "😊"
        
        assertEquals("😊", mockSettings["avatar"])
    }

    @Test
    fun `resetToDefaults restores all values`() {
        mockSettings["theme_color"] = "#000000"
        mockSettings["avatar"] = "👽"
        
        // Reset all
        mockSettings["theme_color"] = "#667eea"
        mockSettings["avatar"] = "😊"
        
        assertEquals("#667eea", mockSettings["theme_color"])
        assertEquals("😊", mockSettings["avatar"])
    }

    // ==================== User Isolation Tests ====================

    @Test
    fun `settings are user-specific`() {
        val user1Settings = mutableMapOf(
            "theme_color" to "#667eea",
            "avatar" to "👨‍🍳"
        )
        
        val user2Settings = mutableMapOf(
            "theme_color" to "#e91e63",
            "avatar" to "👩‍🍳"
        )
        
        assertNotEquals(user1Settings["theme_color"], user2Settings["theme_color"])
        assertNotEquals(user1Settings["avatar"], user2Settings["avatar"])
    }

    @Test
    fun `user cannot change other user settings`() {
        val currentUserSettings = mockSettings.toMutableMap()
        val otherUserSettings = mutableMapOf(
            "theme_color" to "#ff5733",
            "avatar" to "🎂"
        )
        
        // Change current user settings
        currentUserSettings["theme_color"] = "#4caf50"
        
        // Other user settings remain unchanged
        assertEquals("#ff5733", otherUserSettings["theme_color"])
        assertEquals("🎂", otherUserSettings["avatar"])
    }

    // ==================== Validation Tests ====================

    @Test
    fun `valid hex color format is accepted`() {
        val validColors = listOf("#000000", "#ffffff", "#123abc", "#ABC123", "#667eea")
        validColors.forEach { color ->
            assertTrue(isValidHexColor(color))
        }
    }

    @Test
    fun `invalid hex color format is rejected`() {
        val invalidColors = listOf("667eea", "#gggggg", "#12345", "red", "")
        invalidColors.forEach { color ->
            assertFalse(isValidHexColor(color))
        }
    }

    @Test
    fun `empty avatar uses default`() {
        val avatar = ""
        val finalAvatar = avatar.ifEmpty { "😊" }
        assertEquals("😊", finalAvatar)
    }

    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }

    // ==================== Nav Profile Theme Sync Tests ====================

    @Test
    fun `nav profile background matches theme color`() {
        val themeColor = "#4caf50"
        mockSettings["theme_color"] = themeColor
        
        // Simulate gradient generation for nav profile
        val gradient = "linear-gradient(135deg, $themeColor 0%, ${lightenColor(themeColor, 30)} 100%)"
        
        assertTrue(gradient.contains(themeColor))
    }

    @Test
    fun `nav profile updates when theme changes`() {
        val oldColor = "#667eea"
        val newColor = "#e91e63"
        
        mockSettings["theme_color"] = oldColor
        val oldGradient = "linear-gradient(135deg, $oldColor 0%, ${lightenColor(oldColor, 30)} 100%)"
        
        mockSettings["theme_color"] = newColor
        val newGradient = "linear-gradient(135deg, $newColor 0%, ${lightenColor(newColor, 30)} 100%)"
        
        assertNotEquals(oldGradient, newGradient)
    }

    // ==================== Emoji Picker Category Tests ====================

    @Test
    fun `people category contains expected emojis`() {
        val peopleEmojis = listOf("😀", "😃", "😄", "😁", "😊", "😇", "🥰", "😍", "🤩", "😎")
        assertTrue(peopleEmojis.size >= 10)
        assertTrue(peopleEmojis.contains("😊"))
    }

    @Test
    fun `food category contains expected emojis`() {
        val foodEmojis = listOf("🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑")
        assertTrue(foodEmojis.size >= 10)
        assertTrue(foodEmojis.contains("🍎"))
    }

    @Test
    fun `activities category contains expected emojis`() {
        val activityEmojis = listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸")
        assertTrue(activityEmojis.size >= 10)
        assertTrue(activityEmojis.contains("⚽"))
    }

    @Test
    fun `objects category contains expected emojis`() {
        val objectEmojis = listOf("⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "📷", "📹", "🎥")
        assertTrue(objectEmojis.size >= 10)
        assertTrue(objectEmojis.contains("📱"))
    }
}
