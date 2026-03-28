package com.orderMate.modals

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WidgetConfig and related V2 models
 */
class WidgetConfigTest {

    @Test
    fun `widget type displayName is correct`() {
        assertEquals("Due Date", WidgetType.CALENDAR.displayName)
        assertEquals("Category", WidgetType.SINGLE_SELECT.displayName)
        assertEquals("Tags", WidgetType.MULTI_SELECT.displayName)
        assertEquals("Description", WidgetType.TEXT_BOX.displayName)
    }

    @Test
    fun `widget type fromString parses correctly`() {
        assertEquals(WidgetType.CALENDAR, WidgetType.fromString("CALENDAR"))
        assertEquals(WidgetType.SINGLE_SELECT, WidgetType.fromString("SINGLE_SELECT"))
        assertEquals(WidgetType.MULTI_SELECT, WidgetType.fromString("MULTI_SELECT"))
        assertEquals(WidgetType.TEXT_BOX, WidgetType.fromString("TEXT_BOX"))
    }

    @Test
    fun `widget type fromString defaults to TEXT_BOX for unknown`() {
        assertEquals(WidgetType.TEXT_BOX, WidgetType.fromString("UNKNOWN_TYPE"))
        assertEquals(WidgetType.TEXT_BOX, WidgetType.fromString(""))
    }

    @Test
    fun `widget option toMap serializes correctly`() {
        val option = WidgetOption(
            id = "opt1",
            label = "Birthday",
            value = "birthday"
        )
        
        val map = option.toMap()
        
        assertEquals("opt1", map["id"])
        assertEquals("Birthday", map["label"])
        assertEquals("birthday", map["value"])
    }

    @Test
    fun `widget option fromMap deserializes correctly`() {
        val map = mapOf<String, Any?>(
            "id" to "opt2",
            "label" to "Wedding",
            "value" to "wedding"
        )
        
        val option = WidgetOption.fromMap(map)
        
        assertEquals("opt2", option.id)
        assertEquals("Wedding", option.label)
        assertEquals("wedding", option.value)
    }

    @Test
    fun `widget config toMap serializes correctly`() {
        val widget = WidgetConfig(
            id = "widget1",
            type = WidgetType.SINGLE_SELECT,
            label = "Category",
            isEnabled = true,
            order = 0,
            options = mutableListOf(
                WidgetOption("opt1", "Birthday", "birthday")
            )
        )
        
        val map = widget.toMap()
        
        assertEquals("widget1", map["id"])
        assertEquals("SINGLE_SELECT", map["type"])
        assertEquals("Category", map["label"])
        assertEquals(true, map["isEnabled"])
        assertEquals(0L, map["order"])
        
        @Suppress("UNCHECKED_CAST")
        val options = map["options"] as List<Map<String, Any>>
        assertEquals(1, options.size)
        assertEquals("Birthday", options[0]["label"])
    }

    @Test
    fun `widget config fromMap deserializes correctly`() {
        val map = mapOf<String, Any?>(
            "id" to "widget2",
            "type" to "CALENDAR",
            "label" to "Due Date",
            "isEnabled" to true,
            "order" to 1L,
            "options" to emptyList<Map<String, Any>>()
        )
        
        val widget = WidgetConfig.fromMap(map)
        
        assertEquals("widget2", widget.id)
        assertEquals(WidgetType.CALENDAR, widget.type)
        assertEquals("Due Date", widget.label)
        assertTrue(widget.isEnabled)
        assertEquals(1, widget.order)
        assertTrue(widget.options.isEmpty())
    }

    @Test
    fun `widget config handles missing id with generated UUID`() {
        val map = mapOf<String, Any?>(
            "type" to "TEXT_BOX",
            "label" to "Notes"
        )
        
        val widget = WidgetConfig.fromMap(map)
        
        assertNotNull(widget.id)
        assertTrue(widget.id.isNotEmpty())
    }

    @Test
    fun `widget config handles options correctly`() {
        val map = mapOf<String, Any?>(
            "id" to "widget3",
            "type" to "MULTI_SELECT",
            "label" to "Tags",
            "isEnabled" to true,
            "order" to 2L,
            "options" to listOf(
                mapOf("id" to "t1", "label" to "Rush", "value" to "rush"),
                mapOf("id" to "t2", "label" to "VIP", "value" to "vip")
            )
        )
        
        val widget = WidgetConfig.fromMap(map)
        
        assertEquals(2, widget.options.size)
        assertEquals("Rush", widget.options[0].label)
        assertEquals("VIP", widget.options[1].label)
    }

    @Test
    fun `widget config roundtrip works`() {
        val original = WidgetConfig(
            id = "test-widget",
            type = WidgetType.MULTI_SELECT,
            label = "Test Widget",
            isEnabled = false,
            order = 5,
            options = mutableListOf(
                WidgetOption("o1", "Option 1", "opt1"),
                WidgetOption("o2", "Option 2", "opt2")
            )
        )
        
        val map = original.toMap()
        val restored = WidgetConfig.fromMap(map)
        
        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.label, restored.label)
        assertEquals(original.isEnabled, restored.isEnabled)
        assertEquals(original.order, restored.order)
        assertEquals(original.options.size, restored.options.size)
    }

    @Test
    fun `isEnabled defaults to true`() {
        val map = mapOf<String, Any?>(
            "id" to "w1",
            "type" to "CALENDAR",
            "label" to "Date"
            // isEnabled not specified
        )
        
        val widget = WidgetConfig.fromMap(map)
        
        assertTrue(widget.isEnabled)
    }

    @Test
    fun `order defaults to 0`() {
        val map = mapOf<String, Any?>(
            "id" to "w1",
            "type" to "CALENDAR",
            "label" to "Date"
            // order not specified
        )
        
        val widget = WidgetConfig.fromMap(map)
        
        assertEquals(0, widget.order)
    }
}
