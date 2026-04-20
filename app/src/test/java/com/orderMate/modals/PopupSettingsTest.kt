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
    
    // ==================== Item/Order Notes Enabled Tests ====================
    
    @Test
    fun `itemNotesEnabled defaults to true`() {
        val settings = PopupSettings()
        assertTrue(settings.itemNotesEnabled)
    }
    
    @Test
    fun `orderNotesEnabled defaults to true`() {
        val settings = PopupSettings()
        assertTrue(settings.orderNotesEnabled)
    }
    
    @Test
    fun `itemNotesEnabled can be disabled`() {
        val settings = PopupSettings(itemNotesEnabled = false)
        assertFalse(settings.itemNotesEnabled)
    }
    
    @Test
    fun `orderNotesEnabled can be disabled`() {
        val settings = PopupSettings(orderNotesEnabled = false)
        assertFalse(settings.orderNotesEnabled)
    }
    
    @Test
    fun `toMap includes itemNotesEnabled`() {
        val settings = PopupSettings(itemNotesEnabled = false)
        val map = settings.toMap()
        assertEquals(false, map["itemNotesEnabled"])
    }
    
    @Test
    fun `toMap includes orderNotesEnabled`() {
        val settings = PopupSettings(orderNotesEnabled = false)
        val map = settings.toMap()
        assertEquals(false, map["orderNotesEnabled"])
    }
    
    @Test
    fun `fromMap reads itemNotesEnabled`() {
        val map = mapOf<String, Any?>("itemNotesEnabled" to false)
        val settings = PopupSettings.fromMap(map)
        assertFalse(settings.itemNotesEnabled)
    }
    
    @Test
    fun `fromMap reads orderNotesEnabled`() {
        val map = mapOf<String, Any?>("orderNotesEnabled" to false)
        val settings = PopupSettings.fromMap(map)
        assertFalse(settings.orderNotesEnabled)
    }
    
    @Test
    fun `fromMap defaults itemNotesEnabled to true when missing`() {
        val map = mapOf<String, Any?>()
        val settings = PopupSettings.fromMap(map)
        assertTrue(settings.itemNotesEnabled)
    }
    
    @Test
    fun `fromMap defaults orderNotesEnabled to true when missing`() {
        val map = mapOf<String, Any?>()
        val settings = PopupSettings.fromMap(map)
        assertTrue(settings.orderNotesEnabled)
    }
    
    @Test
    fun `item and order notes can be independently controlled`() {
        val settings = PopupSettings(
            itemNotesEnabled = false,
            orderNotesEnabled = true
        )
        assertFalse(settings.itemNotesEnabled)
        assertTrue(settings.orderNotesEnabled)
        
        val settings2 = PopupSettings(
            itemNotesEnabled = true,
            orderNotesEnabled = false
        )
        assertTrue(settings2.itemNotesEnabled)
        assertFalse(settings2.orderNotesEnabled)
    }
    
    @Test
    fun `roundtrip serialization works for notes settings`() {
        val original = PopupSettings(
            itemNotesEnabled = false,
            orderNotesEnabled = false
        )
        
        val map = original.toMap()
        val restored = PopupSettings.fromMap(map)
        
        assertEquals(original.itemNotesEnabled, restored.itemNotesEnabled)
        assertEquals(original.orderNotesEnabled, restored.orderNotesEnabled)
    }
}
