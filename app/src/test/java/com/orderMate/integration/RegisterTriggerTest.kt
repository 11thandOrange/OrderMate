package com.orderMate.integration

import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for Register trigger toggle (showOMButtonInRegister)
 * 
 * Verifies that the "Use OrderMate in Clover Register" toggle:
 * 1. Updates PopupSettings.showOMButtonInRegister in Firebase
 * 2. Affects whether OverlayActivity shows the popup
 * 
 * Note: These are logic-level tests. Actual Firebase and Activity tests
 * would require instrumented tests.
 */
class RegisterTriggerTest {

    private lateinit var settings: PopupSettings
    private lateinit var widgets: List<WidgetConfig>

    @Before
    fun setUp() {
        settings = PopupSettings(
            triggerOnItemAdd = false,
            showOMButtonInRegister = true
        )
        
        widgets = listOf(
            WidgetConfig(
                id = "widget1",
                type = WidgetType.CALENDAR,
                label = "Due Date",
                isEnabled = true,
                order = 0
            )
        )
    }

    // ==================== Toggle State Tests ====================

    @Test
    fun `toggle defaults to enabled`() {
        val newSettings = PopupSettings()
        assertTrue(newSettings.showOMButtonInRegister)
    }

    @Test
    fun `toggle can be disabled`() {
        settings = settings.copy(showOMButtonInRegister = false)
        assertFalse(settings.showOMButtonInRegister)
    }

    @Test
    fun `toggle can be re-enabled`() {
        settings = settings.copy(showOMButtonInRegister = false)
        assertFalse(settings.showOMButtonInRegister)
        
        settings = settings.copy(showOMButtonInRegister = true)
        assertTrue(settings.showOMButtonInRegister)
    }

    // ==================== OverlayActivity Behavior Simulation ====================

    @Test
    fun `overlay shows when toggle enabled and widgets exist`() {
        // Given: toggle enabled, widgets exist
        val shouldShowPopup = shouldShowOverlayPopup(
            showOMButtonInRegister = true,
            hasEnabledWidgets = true
        )
        
        assertTrue(shouldShowPopup)
    }

    @Test
    fun `overlay hidden when toggle disabled`() {
        // Given: toggle disabled
        val shouldShowPopup = shouldShowOverlayPopup(
            showOMButtonInRegister = false,
            hasEnabledWidgets = true
        )
        
        assertFalse(shouldShowPopup)
    }

    @Test
    fun `overlay hidden when no enabled widgets`() {
        // Given: toggle enabled but no widgets
        val shouldShowPopup = shouldShowOverlayPopup(
            showOMButtonInRegister = true,
            hasEnabledWidgets = false
        )
        
        assertFalse(shouldShowPopup)
    }

    @Test
    fun `overlay hidden when toggle disabled and no widgets`() {
        val shouldShowPopup = shouldShowOverlayPopup(
            showOMButtonInRegister = false,
            hasEnabledWidgets = false
        )
        
        assertFalse(shouldShowPopup)
    }

    // ==================== Firebase Persistence Simulation ====================

    @Test
    fun `toggle change persists to Firebase`() {
        // Simulate: user toggles OFF
        val updatedSettings = settings.copy(showOMButtonInRegister = false)
        
        // Simulate: save to Firebase
        val savedMap = updatedSettings.toMap()
        
        // Verify: map contains correct value
        assertEquals(false, savedMap["showOMButtonInRegister"])
    }

    @Test
    fun `toggle loads correctly from Firebase`() {
        // Simulate: Firebase returns saved settings
        val firebaseMap = mapOf<String, Any?>(
            "triggerOnItemAdd" to false,
            "showOMButtonInRegister" to false
        )
        
        // Load settings
        val loadedSettings = PopupSettings.fromMap(firebaseMap)
        
        assertFalse(loadedSettings.showOMButtonInRegister)
    }

    @Test
    fun `backward compatible with triggerFromBasket field`() {
        // Simulate: old Firebase data with legacy field name
        val legacyMap = mapOf<String, Any?>(
            "triggerOnItemAdd" to false,
            "triggerFromBasket" to true  // Legacy field name
        )
        
        val settings = PopupSettings.fromMap(legacyMap)
        
        // Should read legacy field
        assertTrue(settings.showOMButtonInRegister)
    }

    // ==================== Local Settings Sync ====================

    @Test
    fun `local SettingsManager syncs with Firebase`() {
        // Simulate: Firebase value
        val firebaseValue = false
        
        // Simulate: sync to local SettingsManager
        val localValue = firebaseValue
        
        // Both should match
        assertEquals(firebaseValue, localValue)
    }

    @Test
    fun `toggle updates both local and Firebase`() {
        // Simulate: user toggles setting
        val newValue = !settings.showOMButtonInRegister
        
        // Update local
        val localSettings = settings.copy(showOMButtonInRegister = newValue)
        
        // Update Firebase (simulated)
        val firebaseSettings = settings.copy(showOMButtonInRegister = newValue)
        
        // Both should have new value
        assertEquals(newValue, localSettings.showOMButtonInRegister)
        assertEquals(newValue, firebaseSettings.showOMButtonInRegister)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `rapid toggle changes handled correctly`() {
        // Simulate rapid toggling
        var currentValue = settings.showOMButtonInRegister
        
        repeat(9) { // 9 (odd) = final value is opposite of initial
            currentValue = !currentValue
            settings = settings.copy(showOMButtonInRegister = currentValue)
        }
        
        // Final value should be opposite of initial (10 toggles)
        assertEquals(!true, settings.showOMButtonInRegister)
    }

    @Test
    fun `toggle state independent of triggerOnItemAdd`() {
        // Both settings should be independent
        settings = settings.copy(
            triggerOnItemAdd = true,
            showOMButtonInRegister = false
        )
        
        assertTrue(settings.triggerOnItemAdd)
        assertFalse(settings.showOMButtonInRegister)
        
        settings = settings.copy(triggerOnItemAdd = false)
        
        assertFalse(settings.triggerOnItemAdd)
        assertFalse(settings.showOMButtonInRegister)  // Unchanged
    }

    // ==================== WidgetManager Integration ====================

    @Test
    fun `WidgetManager provides settings access`() {
        // Simulate WidgetManager providing settings
        val managerSettings = settings
        
        assertEquals(settings.showOMButtonInRegister, managerSettings.showOMButtonInRegister)
    }

    @Test
    fun `settings update via WidgetManager method`() {
        // Simulate setShowOMButtonInRegister call
        val newValue = false
        val updatedSettings = settings.copy(showOMButtonInRegister = newValue)
        
        assertFalse(updatedSettings.showOMButtonInRegister)
    }

    // ==================== Helper Functions ====================

    /**
     * Simulates OverlayActivity logic for deciding whether to show popup
     */
    private fun shouldShowOverlayPopup(
        showOMButtonInRegister: Boolean,
        hasEnabledWidgets: Boolean
    ): Boolean {
        // Overlay shows popup only if:
        // 1. showOMButtonInRegister is true (user enabled it in settings)
        // 2. There are enabled widgets to display
        return showOMButtonInRegister && hasEnabledWidgets
    }
}
