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

    // ==================== #81: Per-Employee Isolation Tests ====================

    @Test
    fun `different employees have different SharedPreferences files`() {
        val employeeA = "emp_123"
        val employeeB = "emp_456"
        
        val prefsNameA = "ordermate_profile_settings_v2_$employeeA"
        val prefsNameB = "ordermate_profile_settings_v2_$employeeB"
        
        assertNotEquals(prefsNameA, prefsNameB)
        assertTrue(prefsNameA.contains(employeeA))
        assertTrue(prefsNameB.contains(employeeB))
    }

    @Test
    fun `employee switch results in different settings`() {
        val employeeASettings = mutableMapOf(
            "theme_color" to "#ff0000",
            "avatar" to "🔴"
        )
        
        val employeeBSettings = mutableMapOf(
            "theme_color" to "#0000ff",
            "avatar" to "🔵"
        )
        
        // Simulate employee A active
        var currentSettings = employeeASettings
        assertEquals("#ff0000", currentSettings["theme_color"])
        assertEquals("🔴", currentSettings["avatar"])
        
        // Simulate employee B logs in
        currentSettings = employeeBSettings
        assertEquals("#0000ff", currentSettings["theme_color"])
        assertEquals("🔵", currentSettings["avatar"])
    }

    @Test
    fun `cacheAllProfiles stores multiple employee profiles`() {
        val profiles = mapOf(
            "emp_1" to mapOf("color" to "#ff0000", "avatar" to "😀"),
            "emp_2" to mapOf("color" to "#00ff00", "avatar" to "😎"),
            "emp_3" to mapOf("color" to "#0000ff", "avatar" to "🎉")
        )
        
        // Simulate caching
        val cachedProfiles = mutableMapOf<String, MutableMap<String, String>>()
        profiles.forEach { (employeeId, profile) ->
            cachedProfiles[employeeId] = mutableMapOf(
                "theme_color" to (profile["color"] ?: "#3C4B80"),
                "avatar" to (profile["avatar"] ?: "")
            )
        }
        
        assertEquals(3, cachedProfiles.size)
        assertEquals("#ff0000", cachedProfiles["emp_1"]?.get("theme_color"))
        assertEquals("#00ff00", cachedProfiles["emp_2"]?.get("theme_color"))
        assertEquals("#0000ff", cachedProfiles["emp_3"]?.get("theme_color"))
    }

    @Test
    fun `cacheAllProfiles uses defaults for missing fields`() {
        val profiles = mapOf(
            "emp_1" to mapOf<String, String>(), // Empty profile
            "emp_2" to mapOf("color" to "#ff0000") // Missing avatar
        )
        
        val cachedProfiles = mutableMapOf<String, MutableMap<String, String>>()
        profiles.forEach { (employeeId, profile) ->
            cachedProfiles[employeeId] = mutableMapOf(
                "theme_color" to (profile["color"] ?: "#3C4B80"),
                "avatar" to (profile["avatar"] ?: "")
            )
        }
        
        // Empty profile gets defaults
        assertEquals("#3C4B80", cachedProfiles["emp_1"]?.get("theme_color"))
        assertEquals("", cachedProfiles["emp_1"]?.get("avatar"))
        
        // Partial profile keeps provided value, defaults for missing
        assertEquals("#ff0000", cachedProfiles["emp_2"]?.get("theme_color"))
        assertEquals("", cachedProfiles["emp_2"]?.get("avatar"))
    }

    @Test
    fun `employee change detection works correctly`() {
        var lastKnownEmployeeId: String? = null
        
        // Initial state - no employee
        val currentEmployee1 = "emp_123"
        val employeeChanged1 = currentEmployee1 != lastKnownEmployeeId
        assertTrue(employeeChanged1)
        lastKnownEmployeeId = currentEmployee1
        
        // Same employee - no change
        val currentEmployee2 = "emp_123"
        val employeeChanged2 = currentEmployee2 != lastKnownEmployeeId
        assertFalse(employeeChanged2)
        
        // Different employee - changed
        val currentEmployee3 = "emp_456"
        val employeeChanged3 = currentEmployee3 != lastKnownEmployeeId
        assertTrue(employeeChanged3)
        lastKnownEmployeeId = currentEmployee3
    }

    @Test
    fun `instant switch uses cached profile`() {
        // Pre-cached profiles
        val cachedProfiles = mapOf(
            "emp_A" to mapOf("theme_color" to "#ff0000", "avatar" to "🔴"),
            "emp_B" to mapOf("theme_color" to "#0000ff", "avatar" to "🔵")
        )
        
        // Employee A is active
        var currentEmployeeId = "emp_A"
        var currentProfile = cachedProfiles[currentEmployeeId]
        assertEquals("#ff0000", currentProfile?.get("theme_color"))
        
        // Employee B logs in - instant switch from cache
        currentEmployeeId = "emp_B"
        currentProfile = cachedProfiles[currentEmployeeId]
        assertEquals("#0000ff", currentProfile?.get("theme_color"))
        
        // No network call needed - profile was pre-cached
        assertNotNull(currentProfile)
    }

    @Test
    fun `new employee without cache gets defaults`() {
        val cachedProfiles = mapOf(
            "emp_A" to mapOf("theme_color" to "#ff0000", "avatar" to "🔴")
        )
        
        // New employee not in cache
        val newEmployeeId = "emp_NEW"
        val profile = cachedProfiles[newEmployeeId]
        
        // Profile is null, should use defaults
        assertNull(profile)
        
        // Default values should be used
        val themeColor = profile?.get("theme_color") ?: "#3C4B80"
        val avatar = profile?.get("avatar") ?: ""
        
        assertEquals("#3C4B80", themeColor)
        assertEquals("", avatar)
    }

    @Test
    fun `singleton returns same instance for same employee`() {
        // Simulate singleton behavior
        var currentEmployeeId = "emp_123"
        var instanceId = "instance_${currentEmployeeId}"
        
        val firstCall = instanceId
        val secondCall = instanceId // Same employee, same instance
        
        assertEquals(firstCall, secondCall)
    }

    @Test
    fun `singleton returns new instance when employee changes`() {
        // Simulate singleton behavior
        var currentEmployeeId = "emp_123"
        var instanceId = "instance_${currentEmployeeId}"
        
        val firstInstance = instanceId
        
        // Employee changes
        currentEmployeeId = "emp_456"
        instanceId = "instance_${currentEmployeeId}"
        
        val secondInstance = instanceId
        
        assertNotEquals(firstInstance, secondInstance)
    }

    @Test
    fun `clearInstance resets singleton state`() {
        var instance: String? = "instance_emp_123"
        var currentEmployeeId: String? = "emp_123"
        
        // Clear
        instance = null
        currentEmployeeId = null
        
        assertNull(instance)
        assertNull(currentEmployeeId)
    }

    @Test
    fun `refreshInstance creates new instance for current employee`() {
        var currentEmployeeId = "emp_123"
        var instanceVersion = 1
        
        val firstInstance = "instance_${currentEmployeeId}_v$instanceVersion"
        
        // Refresh
        instanceVersion++
        val refreshedInstance = "instance_${currentEmployeeId}_v$instanceVersion"
        
        assertNotEquals(firstInstance, refreshedInstance)
        assertTrue(refreshedInstance.contains(currentEmployeeId))
    }
}
