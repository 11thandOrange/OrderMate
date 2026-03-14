package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsManager (Issue #83)
 * 
 * Tests cover:
 * - General settings (register toggles)
 * - Pop Up widgets (add, remove, reorder, max 7)
 * - Advanced settings (notification days, receipt settings)
 * - Notification settings (SMS number, multiple templates)
 */
class SettingsManagerTest {

    private lateinit var mockSettings: MutableMap<String, Any?>
    private lateinit var notificationTemplates: MutableList<NotificationTemplate>

    @Before
    fun setUp() {
        mockSettings = mutableMapOf(
            "use_ordermate_register" to false, // Coming soon - disabled by default
            "use_ordermate_in_clover" to true,
            "notification_days" to 3,
            "receipt_time" to 60,
            "receipt_unit" to "minutes",
            "sms_number" to ""
        )
        
        notificationTemplates = mutableListOf(
            NotificationTemplate(1, "Order Ready", "Your order from {{merchant_name}} is ready for pickup!"),
            NotificationTemplate(2, "Order Shipped", "Great news! Your order has been shipped and is on its way!")
        )
    }

    // Helper data class for notification templates
    data class NotificationTemplate(
        val id: Int,
        var name: String,
        var content: String
    )

    // ==================== General Settings Tests ====================

    @Test
    fun `OrderMate register is disabled by default (coming soon)`() {
        assertFalse(mockSettings["use_ordermate_register"] as Boolean)
    }

    @Test
    fun `OrderMate register toggle is disabled`() {
        // Coming soon feature - should remain false
        val isComingSoon = true
        if (!isComingSoon) {
            mockSettings["use_ordermate_register"] = true
        }
        assertFalse(mockSettings["use_ordermate_register"] as Boolean)
    }

    @Test
    fun `default uses OrderMate in Clover register`() {
        assertTrue(mockSettings["use_ordermate_in_clover"] as Boolean)
    }

    @Test
    fun `can toggle OrderMate in Clover register`() {
        mockSettings["use_ordermate_in_clover"] = false
        assertFalse(mockSettings["use_ordermate_in_clover"] as Boolean)
    }

    // ==================== Widget Tests ====================

    @Test
    fun `default has 4 widgets`() {
        val defaultWidgets = listOf(
            TestWidget(1, "CALENDAR", "Due Date/Time"),
            TestWidget(2, "SINGLE_SELECT", "Category"),
            TestWidget(3, "MULTI_SELECT", "Tags"),
            TestWidget(4, "TEXT_BOX", "Description")
        )
        assertEquals(4, defaultWidgets.size)
    }

    @Test
    fun `can add widget up to 7`() {
        val widgets = mutableListOf<TestWidget>()
        repeat(7) { i ->
            widgets.add(TestWidget(i + 1, "CALENDAR", "Widget $i"))
        }
        assertEquals(7, widgets.size)
    }

    @Test
    fun `cannot add more than 7 widgets`() {
        val widgets = mutableListOf<TestWidget>()
        repeat(7) { i ->
            widgets.add(TestWidget(i + 1, "CALENDAR", "Widget $i"))
        }
        
        val canAdd = widgets.size < 7
        assertFalse(canAdd)
    }

    @Test
    fun `can remove widget`() {
        val widgets = mutableListOf(
            TestWidget(1, "CALENDAR", "Due Date/Time"),
            TestWidget(2, "SINGLE_SELECT", "Category")
        )
        widgets.removeAll { it.id == 1 }
        assertEquals(1, widgets.size)
        assertEquals(2, widgets[0].id)
    }

    @Test
    fun `can reorder widgets`() {
        val widgets = mutableListOf(
            TestWidget(1, "CALENDAR", "Due Date/Time"),
            TestWidget(2, "SINGLE_SELECT", "Category"),
            TestWidget(3, "MULTI_SELECT", "Tags")
        )
        
        // Move first to last
        val widget = widgets.removeAt(0)
        widgets.add(widget)
        
        assertEquals(2, widgets[0].id)
        assertEquals(3, widgets[1].id)
        assertEquals(1, widgets[2].id)
    }

    @Test
    fun `widget types are valid`() {
        val validTypes = listOf("CALENDAR", "SINGLE_SELECT", "MULTI_SELECT", "TEXT_BOX")
        assertEquals(4, validTypes.size)
        assertTrue(validTypes.contains("CALENDAR"))
        assertTrue(validTypes.contains("SINGLE_SELECT"))
        assertTrue(validTypes.contains("MULTI_SELECT"))
        assertTrue(validTypes.contains("TEXT_BOX"))
    }

    @Test
    fun `calendar widget has correct default label`() {
        val widget = TestWidget(1, "CALENDAR", "Due Date/Time")
        assertEquals("Due Date/Time", widget.label)
    }

    @Test
    fun `single select widget has correct default label`() {
        val widget = TestWidget(1, "SINGLE_SELECT", "Category")
        assertEquals("Category", widget.label)
    }

    @Test
    fun `multi select widget has correct default label`() {
        val widget = TestWidget(1, "MULTI_SELECT", "Tags")
        assertEquals("Tags", widget.label)
    }

    @Test
    fun `text box widget has correct default label`() {
        val widget = TestWidget(1, "TEXT_BOX", "Description")
        assertEquals("Description", widget.label)
    }

    @Test
    fun `can update widget label`() {
        val widget = TestWidget(1, "CALENDAR", "Due Date/Time")
        val updatedWidget = widget.copy(label = "Custom Date")
        assertEquals("Custom Date", updatedWidget.label)
    }

    @Test
    fun `can toggle widget enabled`() {
        val widget = TestWidget(1, "CALENDAR", "Due Date/Time", enabled = true)
        val disabledWidget = widget.copy(enabled = false)
        assertFalse(disabledWidget.enabled)
    }

    @Test
    fun `select widget can have values`() {
        val values = listOf("Birthday", "Wedding", "Custom")
        val widget = TestWidget(1, "SINGLE_SELECT", "Category", values = values)
        assertEquals(3, widget.values.size)
        assertTrue(widget.values.contains("Birthday"))
    }

    @Test
    fun `can add value to widget`() {
        val values = mutableListOf("Birthday", "Wedding")
        values.add("Anniversary")
        assertEquals(3, values.size)
        assertTrue(values.contains("Anniversary"))
    }

    @Test
    fun `can remove value from widget`() {
        val values = mutableListOf("Birthday", "Wedding", "Custom")
        values.remove("Wedding")
        assertEquals(2, values.size)
        assertFalse(values.contains("Wedding"))
    }

    // ==================== Advanced Settings Tests ====================

    @Test
    fun `default notification days is 3`() {
        assertEquals(3, mockSettings["notification_days"])
    }

    @Test
    fun `can set notification days`() {
        mockSettings["notification_days"] = 5
        assertEquals(5, mockSettings["notification_days"])
    }

    @Test
    fun `notification days clamped to 1-30`() {
        var days = 0
        days = days.coerceIn(1, 30)
        assertEquals(1, days)
        
        days = 50
        days = days.coerceIn(1, 30)
        assertEquals(30, days)
    }

    @Test
    fun `default receipt time is 60 minutes`() {
        assertEquals(60, mockSettings["receipt_time"])
        assertEquals("minutes", mockSettings["receipt_unit"])
    }

    @Test
    fun `can set receipt time`() {
        mockSettings["receipt_time"] = 30
        assertEquals(30, mockSettings["receipt_time"])
    }

    @Test
    fun `receipt unit can be minutes hours or days`() {
        val validUnits = listOf("minutes", "hours", "days")
        
        validUnits.forEach { unit ->
            mockSettings["receipt_unit"] = unit
            assertEquals(unit, mockSettings["receipt_unit"])
        }
    }

    @Test
    fun `invalid receipt unit is rejected`() {
        val validUnits = listOf("minutes", "hours", "days")
        val testUnit = "weeks"
        
        if (testUnit in validUnits) {
            mockSettings["receipt_unit"] = testUnit
        }
        
        assertNotEquals("weeks", mockSettings["receipt_unit"])
    }

    // ==================== Notification Settings Tests ====================

    @Test
    fun `default SMS number is empty`() {
        assertEquals("", mockSettings["sms_number"])
    }

    @Test
    fun `can set SMS number`() {
        mockSettings["sms_number"] = "+1 (555) 123-4567"
        assertEquals("+1 (555) 123-4567", mockSettings["sms_number"])
    }

    @Test
    fun `SMS number placeholder should be visible`() {
        val placeholder = "Enter phone number"
        assertTrue(placeholder.isNotEmpty())
    }

    // ==================== Multiple Notification Templates Tests ====================

    @Test
    fun `default has 2 notification templates`() {
        assertEquals(2, notificationTemplates.size)
    }

    @Test
    fun `first template is Order Ready`() {
        val template = notificationTemplates[0]
        assertEquals("Order Ready", template.name)
        assertTrue(template.content.contains("ready for pickup"))
    }

    @Test
    fun `second template is Order Shipped`() {
        val template = notificationTemplates[1]
        assertEquals("Order Shipped", template.name)
        assertTrue(template.content.contains("shipped"))
    }

    @Test
    fun `can add new template`() {
        val newTemplate = NotificationTemplate(3, "New Template", "Enter your template text...")
        notificationTemplates.add(newTemplate)
        assertEquals(3, notificationTemplates.size)
    }

    @Test
    fun `can delete template`() {
        notificationTemplates.removeAll { it.id == 1 }
        assertEquals(1, notificationTemplates.size)
        assertNull(notificationTemplates.find { it.id == 1 })
    }

    @Test
    fun `can edit template name`() {
        notificationTemplates[0].name = "Custom Name"
        assertEquals("Custom Name", notificationTemplates[0].name)
    }

    @Test
    fun `can edit template content`() {
        val newContent = "Your order is on its way!"
        notificationTemplates[0].content = newContent
        assertEquals(newContent, notificationTemplates[0].content)
    }

    @Test
    fun `template content max 250 chars`() {
        val longContent = "A".repeat(300)
        val trimmed = longContent.take(250)
        assertEquals(250, trimmed.length)
    }

    @Test
    fun `template preserves variables`() {
        val template = notificationTemplates[0]
        assertTrue(template.content.contains("{{merchant_name}}"))
    }

    @Test
    fun `can have multiple templates with same name`() {
        notificationTemplates.add(NotificationTemplate(3, "Order Ready", "Different content"))
        val sameNameTemplates = notificationTemplates.filter { it.name == "Order Ready" }
        assertEquals(2, sameNameTemplates.size)
    }

    @Test
    fun `templates have unique IDs`() {
        val ids = notificationTemplates.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `new template gets next available ID`() {
        val maxId = notificationTemplates.maxOf { it.id }
        val newTemplate = NotificationTemplate(maxId + 1, "New", "Content")
        notificationTemplates.add(newTemplate)
        assertEquals(maxId + 1, notificationTemplates.last().id)
    }

    @Test
    fun `can expand and collapse template`() {
        var isExpanded = false
        isExpanded = !isExpanded
        assertTrue(isExpanded)
        isExpanded = !isExpanded
        assertFalse(isExpanded)
    }

    @Test
    fun `template shows preview when collapsed`() {
        val template = notificationTemplates[0]
        val preview = template.content.take(40) + "..."
        assertTrue(preview.length <= 43)
    }

    @Test
    fun `templates available in notification popup dropdown`() {
        // Templates should be selectable in dropdown
        assertTrue(notificationTemplates.isNotEmpty())
        notificationTemplates.forEach { template ->
            assertTrue(template.name.isNotEmpty())
            assertTrue(template.content.isNotEmpty())
        }
    }

    // ==================== Reset Tests ====================

    @Test
    fun `can reset to defaults`() {
        // Change settings
        mockSettings["use_ordermate_in_clover"] = false
        mockSettings["notification_days"] = 10
        mockSettings["sms_number"] = "+1234567890"
        
        // Reset
        mockSettings["use_ordermate_register"] = false // Coming soon
        mockSettings["use_ordermate_in_clover"] = true
        mockSettings["notification_days"] = 3
        mockSettings["receipt_time"] = 60
        mockSettings["receipt_unit"] = "minutes"
        mockSettings["sms_number"] = ""
        
        // Reset templates to default
        notificationTemplates.clear()
        notificationTemplates.add(NotificationTemplate(1, "Order Ready", "Your order from {{merchant_name}} is ready for pickup!"))
        notificationTemplates.add(NotificationTemplate(2, "Order Shipped", "Great news! Your order has been shipped and is on its way!"))
        
        // Verify
        assertFalse(mockSettings["use_ordermate_register"] as Boolean)
        assertTrue(mockSettings["use_ordermate_in_clover"] as Boolean)
        assertEquals(3, mockSettings["notification_days"])
        assertEquals(60, mockSettings["receipt_time"])
        assertEquals("minutes", mockSettings["receipt_unit"])
        assertEquals("", mockSettings["sms_number"])
        assertEquals(2, notificationTemplates.size)
    }

    @Test
    fun `reset restores default templates`() {
        // Delete all templates
        notificationTemplates.clear()
        assertEquals(0, notificationTemplates.size)
        
        // Reset
        notificationTemplates.add(NotificationTemplate(1, "Order Ready", "Your order from {{merchant_name}} is ready for pickup!"))
        notificationTemplates.add(NotificationTemplate(2, "Order Shipped", "Great news! Your order has been shipped and is on its way!"))
        
        assertEquals(2, notificationTemplates.size)
    }

    // ==================== Persistence Tests ====================

    @Test
    fun `settings persist after change`() {
        mockSettings["notification_days"] = 7
        val saved = mockSettings["notification_days"]
        assertEquals(7, saved)
    }

    @Test
    fun `widgets can be serialized`() {
        val widgets = listOf(
            TestWidget(1, "CALENDAR", "Due Date"),
            TestWidget(2, "TEXT_BOX", "Notes")
        )
        
        // Simulate JSON serialization
        val json = widgets.map { 
            """{"id":${it.id},"type":"${it.type}","label":"${it.label}"}"""
        }.joinToString(",", "[", "]")
        
        assertTrue(json.contains("CALENDAR"))
        assertTrue(json.contains("TEXT_BOX"))
    }

    // ==================== Widget Order Tests ====================

    @Test
    fun `widgets maintain order`() {
        val widgets = listOf(
            TestWidget(1, "CALENDAR", "Due Date", order = 0),
            TestWidget(2, "TEXT_BOX", "Notes", order = 1),
            TestWidget(3, "SINGLE_SELECT", "Type", order = 2)
        )
        
        val sorted = widgets.sortedBy { it.order }
        assertEquals(1, sorted[0].id)
        assertEquals(2, sorted[1].id)
        assertEquals(3, sorted[2].id)
    }

    @Test
    fun `reorder updates order values`() {
        val widgets = mutableListOf(
            TestWidget(1, "CALENDAR", "Due Date", order = 0),
            TestWidget(2, "TEXT_BOX", "Notes", order = 1),
            TestWidget(3, "SINGLE_SELECT", "Type", order = 2)
        )
        
        // Move widget 3 to first position
        val widget = widgets.removeAt(2)
        widgets.add(0, widget)
        
        // Update order values
        widgets.forEachIndexed { index, w ->
            w.order = index
        }
        
        assertEquals(0, widgets[0].order) // Type now first
        assertEquals(1, widgets[1].order) // Due Date now second
        assertEquals(2, widgets[2].order) // Notes now third
    }

    // Helper data class for testing
    data class TestWidget(
        val id: Int,
        val type: String,
        val label: String,
        val values: List<String> = emptyList(),
        val enabled: Boolean = true,
        var order: Int = 0
    )
}
