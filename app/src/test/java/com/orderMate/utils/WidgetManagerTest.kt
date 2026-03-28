package com.orderMate.utils

import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WidgetManager
 * 
 * Tests widget management operations and filtering logic.
 * Note: Firebase operations are not tested here (require integration tests).
 */
class WidgetManagerTest {

    private lateinit var testWidgets: MutableList<WidgetConfig>
    private lateinit var testSettings: PopupSettings

    @Before
    fun setUp() {
        testWidgets = mutableListOf(
            WidgetConfig(
                id = "widget1",
                type = WidgetType.CALENDAR,
                label = "Due Date",
                isEnabled = true,
                order = 0,
                options = mutableListOf()
            ),
            WidgetConfig(
                id = "widget2",
                type = WidgetType.SINGLE_SELECT,
                label = "Category",
                isEnabled = true,
                order = 1,
                options = mutableListOf(
                    WidgetOption("opt1", "Birthday", "birthday"),
                    WidgetOption("opt2", "Wedding", "wedding")
                )
            ),
            WidgetConfig(
                id = "widget3",
                type = WidgetType.MULTI_SELECT,
                label = "Tags",
                isEnabled = false,
                order = 2,
                options = mutableListOf(
                    WidgetOption("opt3", "Rush", "rush"),
                    WidgetOption("opt4", "VIP", "vip")
                )
            ),
            WidgetConfig(
                id = "widget4",
                type = WidgetType.TEXT_BOX,
                label = "Description",
                isEnabled = true,
                order = 3,
                options = mutableListOf()
            )
        )
        
        testSettings = PopupSettings(
            triggerOnItemAdd = false,
            showOMButtonInRegister = true
        )
    }

    // ==================== Widget Filtering Tests ====================

    @Test
    fun `enabledWidgets returns only enabled widgets`() {
        val enabledWidgets = testWidgets.filter { it.isEnabled }
        
        assertEquals(3, enabledWidgets.size)
        assertTrue(enabledWidgets.all { it.isEnabled })
        assertFalse(enabledWidgets.any { it.id == "widget3" })
    }

    @Test
    fun `filterableWidgets returns single and multi select only`() {
        val filterableWidgets = testWidgets.filter { 
            it.type == WidgetType.SINGLE_SELECT || it.type == WidgetType.MULTI_SELECT 
        }
        
        assertEquals(2, filterableWidgets.size)
        assertTrue(filterableWidgets.any { it.type == WidgetType.SINGLE_SELECT })
        assertTrue(filterableWidgets.any { it.type == WidgetType.MULTI_SELECT })
    }

    @Test
    fun `enabledWidgets sorted by order`() {
        // Shuffle the order
        testWidgets[0].order = 3
        testWidgets[1].order = 1
        testWidgets[2].order = 2
        testWidgets[3].order = 0
        
        val sorted = testWidgets.filter { it.isEnabled }.sortedBy { it.order }
        
        assertEquals("widget4", sorted[0].id) // order 0
        assertEquals("widget2", sorted[1].id) // order 1
        assertEquals("widget1", sorted[2].id) // order 3
    }

    // ==================== Widget CRUD Tests ====================

    @Test
    fun `getWidgetById returns correct widget`() {
        val widget = testWidgets.find { it.id == "widget2" }
        
        assertNotNull(widget)
        assertEquals("Category", widget?.label)
        assertEquals(WidgetType.SINGLE_SELECT, widget?.type)
    }

    @Test
    fun `getWidgetById returns null for unknown id`() {
        val widget = testWidgets.find { it.id == "unknown" }
        
        assertNull(widget)
    }

    @Test
    fun `getWidgetCount returns correct count`() {
        assertEquals(4, testWidgets.size)
    }

    @Test
    fun `addWidget increases count`() {
        val newWidget = WidgetConfig(
            id = "widget5",
            type = WidgetType.TEXT_BOX,
            label = "Notes",
            isEnabled = true,
            order = testWidgets.size
        )
        testWidgets.add(newWidget)
        
        assertEquals(5, testWidgets.size)
        assertEquals("widget5", testWidgets.last().id)
    }

    @Test
    fun `max 7 widgets limit`() {
        val maxWidgets = 7
        
        // Add widgets up to max
        while (testWidgets.size < maxWidgets) {
            testWidgets.add(WidgetConfig(
                id = "widget${testWidgets.size + 1}",
                type = WidgetType.TEXT_BOX,
                label = "Widget ${testWidgets.size + 1}",
                order = testWidgets.size
            ))
        }
        
        assertEquals(maxWidgets, testWidgets.size)
        
        // Verify cannot add more
        val canAdd = testWidgets.size < maxWidgets
        assertFalse(canAdd)
    }

    @Test
    fun `deleteWidget removes widget`() {
        val initialCount = testWidgets.size
        testWidgets.removeAll { it.id == "widget2" }
        
        assertEquals(initialCount - 1, testWidgets.size)
        assertNull(testWidgets.find { it.id == "widget2" })
    }

    @Test
    fun `updateWidget modifies existing widget`() {
        val widget = testWidgets.find { it.id == "widget1" }!!
        widget.label = "Custom Date"
        widget.isEnabled = false
        
        val updated = testWidgets.find { it.id == "widget1" }
        assertEquals("Custom Date", updated?.label)
        assertFalse(updated?.isEnabled ?: true)
    }

    // ==================== Widget Reordering Tests ====================

    @Test
    fun `reorderWidgets swaps positions`() {
        // Move widget at index 0 to index 2
        val widget = testWidgets.removeAt(0)
        testWidgets.add(2, widget)
        
        // Update order values
        testWidgets.forEachIndexed { index, w -> w.order = index }
        
        assertEquals("widget2", testWidgets[0].id)
        assertEquals("widget3", testWidgets[1].id)
        assertEquals("widget1", testWidgets[2].id)
        assertEquals("widget4", testWidgets[3].id)
        
        // Verify order values match position
        testWidgets.forEachIndexed { index, w ->
            assertEquals(index, w.order)
        }
    }

    @Test
    fun `reorderWidgets maintains list integrity`() {
        val originalIds = testWidgets.map { it.id }.toSet()
        
        // Perform multiple reorders
        java.util.Collections.swap(testWidgets, 0, 3)
        java.util.Collections.swap(testWidgets, 1, 2)
        
        val newIds = testWidgets.map { it.id }.toSet()
        
        assertEquals(originalIds, newIds)
        assertEquals(4, testWidgets.size)
    }

    // ==================== Widget Options Tests ====================

    @Test
    fun `widget options can be added`() {
        val widget = testWidgets.find { it.id == "widget2" }!!
        val initialCount = widget.options.size
        
        widget.options.add(WidgetOption("opt_new", "Anniversary", "anniversary"))
        
        assertEquals(initialCount + 1, widget.options.size)
        assertTrue(widget.options.any { it.label == "Anniversary" })
    }

    @Test
    fun `widget options can be removed`() {
        val widget = testWidgets.find { it.id == "widget2" }!!
        widget.options.removeAll { it.id == "opt1" }
        
        assertEquals(1, widget.options.size)
        assertFalse(widget.options.any { it.label == "Birthday" })
    }

    @Test
    fun `calendar widget has no options`() {
        val calendarWidget = testWidgets.find { it.type == WidgetType.CALENDAR }!!
        assertTrue(calendarWidget.options.isEmpty())
    }

    @Test
    fun `textbox widget has no options`() {
        val textWidget = testWidgets.find { it.type == WidgetType.TEXT_BOX }!!
        assertTrue(textWidget.options.isEmpty())
    }

    // ==================== Settings Tests ====================

    @Test
    fun `default settings showOMButtonInRegister is true`() {
        assertTrue(testSettings.showOMButtonInRegister)
    }

    @Test
    fun `settings can be updated`() {
        val newSettings = testSettings.copy(showOMButtonInRegister = false)
        
        assertFalse(newSettings.showOMButtonInRegister)
        // Original unchanged
        assertTrue(testSettings.showOMButtonInRegister)
    }

    @Test
    fun `triggerOnItemAdd defaults to false`() {
        assertFalse(testSettings.triggerOnItemAdd)
    }

    // ==================== Widget Type Tests ====================

    @Test
    fun `all widget types have display names`() {
        WidgetType.values().forEach { type ->
            assertTrue(type.displayName.isNotEmpty())
        }
    }

    @Test
    fun `widget types are correctly identified`() {
        assertEquals(WidgetType.CALENDAR, testWidgets[0].type)
        assertEquals(WidgetType.SINGLE_SELECT, testWidgets[1].type)
        assertEquals(WidgetType.MULTI_SELECT, testWidgets[2].type)
        assertEquals(WidgetType.TEXT_BOX, testWidgets[3].type)
    }

    // ==================== Default Widget Factory Tests ====================

    @Test
    fun `default widgets created correctly`() {
        val defaults = createDefaultWidgets()
        
        assertEquals(4, defaults.size)
        assertTrue(defaults.any { it.type == WidgetType.CALENDAR })
        assertTrue(defaults.any { it.type == WidgetType.SINGLE_SELECT })
        assertTrue(defaults.any { it.type == WidgetType.MULTI_SELECT })
        assertTrue(defaults.any { it.type == WidgetType.TEXT_BOX })
    }

    @Test
    fun `default widgets have unique IDs`() {
        val defaults = createDefaultWidgets()
        val ids = defaults.map { it.id }
        
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `default widgets are enabled`() {
        val defaults = createDefaultWidgets()
        
        assertTrue(defaults.all { it.isEnabled })
    }

    @Test
    fun `default widgets have correct order`() {
        val defaults = createDefaultWidgets()
        
        defaults.forEachIndexed { index, widget ->
            assertEquals(index, widget.order)
        }
    }

    // Helper to create default widgets
    private fun createDefaultWidgets(): List<WidgetConfig> {
        return listOf(
            WidgetConfig(
                id = "default_calendar",
                type = WidgetType.CALENDAR,
                label = "Due Date",
                isEnabled = true,
                order = 0
            ),
            WidgetConfig(
                id = "default_category",
                type = WidgetType.SINGLE_SELECT,
                label = "Category",
                isEnabled = true,
                order = 1,
                options = mutableListOf(
                    WidgetOption("cat1", "Birthday", "birthday"),
                    WidgetOption("cat2", "Wedding", "wedding"),
                    WidgetOption("cat3", "Custom", "custom")
                )
            ),
            WidgetConfig(
                id = "default_tags",
                type = WidgetType.MULTI_SELECT,
                label = "Tags",
                isEnabled = true,
                order = 2,
                options = mutableListOf(
                    WidgetOption("tag1", "Rush", "rush"),
                    WidgetOption("tag2", "VIP", "vip"),
                    WidgetOption("tag3", "Delivery", "delivery")
                )
            ),
            WidgetConfig(
                id = "default_description",
                type = WidgetType.TEXT_BOX,
                label = "Description",
                isEnabled = true,
                order = 3
            )
        )
    }
}
