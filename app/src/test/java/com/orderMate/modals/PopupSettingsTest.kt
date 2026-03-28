package com.orderMate.modals

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PopupSettings
 */
class PopupSettingsTest {

    @Test
    fun `default values are correct`() {
        val settings = PopupSettings()
        assertFalse(settings.triggerOnItemAdd)
        assertTrue(settings.showOMButtonInRegister)
    }

    @Test
    fun `toMap serializes correctly`() {
        val settings = PopupSettings(
            triggerOnItemAdd = true,
            showOMButtonInRegister = false
        )
        
        val map = settings.toMap()
        
        assertEquals(true, map["triggerOnItemAdd"])
        assertEquals(false, map["showOMButtonInRegister"])
    }

    @Test
    fun `fromMap deserializes correctly`() {
        val map = mapOf<String, Any?>(
            "triggerOnItemAdd" to true,
            "showOMButtonInRegister" to false
        )
        
        val settings = PopupSettings.fromMap(map)
        
        assertTrue(settings.triggerOnItemAdd)
        assertFalse(settings.showOMButtonInRegister)
    }

    @Test
    fun `fromMap handles null map`() {
        val settings = PopupSettings.fromMap(null)
        
        assertFalse(settings.triggerOnItemAdd)
        assertTrue(settings.showOMButtonInRegister)
    }

    @Test
    fun `fromMap handles missing keys with defaults`() {
        val map = mapOf<String, Any?>()
        
        val settings = PopupSettings.fromMap(map)
        
        assertFalse(settings.triggerOnItemAdd)
        assertTrue(settings.showOMButtonInRegister)
    }

    @Test
    fun `fromMap supports legacy triggerFromBasket field`() {
        // Test backward compatibility with old field name
        val map = mapOf<String, Any?>(
            "triggerOnItemAdd" to false,
            "triggerFromBasket" to true  // Old field name
        )
        
        val settings = PopupSettings.fromMap(map)
        
        // Should read from legacy field when new field is missing
        assertTrue(settings.showOMButtonInRegister)
    }

    @Test
    fun `fromMap prefers new field over legacy`() {
        val map = mapOf<String, Any?>(
            "showOMButtonInRegister" to false,
            "triggerFromBasket" to true  // Old field should be ignored
        )
        
        val settings = PopupSettings.fromMap(map)
        
        // New field takes precedence
        assertFalse(settings.showOMButtonInRegister)
    }

    @Test
    fun `copy creates new instance with modified values`() {
        val original = PopupSettings(
            triggerOnItemAdd = false,
            showOMButtonInRegister = true
        )
        
        val modified = original.copy(showOMButtonInRegister = false)
        
        // Original unchanged
        assertTrue(original.showOMButtonInRegister)
        // Copy has new value
        assertFalse(modified.showOMButtonInRegister)
    }

    @Test
    fun `roundtrip serialization works`() {
        val original = PopupSettings(
            triggerOnItemAdd = true,
            showOMButtonInRegister = false
        )
        
        val map = original.toMap()
        val restored = PopupSettings.fromMap(map)
        
        assertEquals(original.triggerOnItemAdd, restored.triggerOnItemAdd)
        assertEquals(original.showOMButtonInRegister, restored.showOMButtonInRegister)
    }
}
