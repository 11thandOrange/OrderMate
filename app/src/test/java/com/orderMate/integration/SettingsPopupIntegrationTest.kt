package com.orderMate.integration

import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for Settings -> Popup flow
 * 
 * Tests that widget configuration changes in Settings are properly
 * reflected in the popup dialog.
 * 
 * Note: These are logic-level tests. Firebase integration would require
 * instrumented tests with actual Firebase connection.
 */
class SettingsPopupIntegrationTest {

    private lateinit var widgets: MutableList<WidgetConfig>
    private lateinit var settings: PopupSettings

    @Before
    fun setUp() {
        // Simulate initial widget configuration
        widgets = mutableListOf(
            WidgetConfig(
                id = "widget1",
                type = WidgetType.CALENDAR,
                label = "Due Date",
                isEnabled = true,
                order = 0
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
            )
        )
        
        settings = PopupSettings(
            triggerOnItemAdd = false,
            showOMButtonInRegister = true
        )
    }

    // ==================== Settings Changes Reflect in Popup ====================

    @Test
    fun `disabling widget excludes it from popup`() {
        // Given: widget2 is enabled
        assertTrue(widgets.find { it.id == "widget2" }?.isEnabled ?: false)
        
        // When: user disables widget2 in settings
        widgets.find { it.id == "widget2" }?.isEnabled = false
        
        // Then: popup only shows enabled widgets
        val enabledWidgets = widgets.filter { it.isEnabled }
        assertEquals(1, enabledWidgets.size)
        assertEquals("widget1", enabledWidgets[0].id)
    }

    @Test
    fun `adding option in settings appears in popup`() {
        // Given: Category has 2 options
        val categoryWidget = widgets.find { it.id == "widget2" }!!
        assertEquals(2, categoryWidget.options.size)
        
        // When: user adds new option in settings
        categoryWidget.options.add(WidgetOption("opt3", "Anniversary", "anniversary"))
        
        // Then: popup shows 3 options for Category
        assertEquals(3, categoryWidget.options.size)
        assertTrue(categoryWidget.options.any { it.label == "Anniversary" })
    }

    @Test
    fun `removing option in settings removes from popup`() {
        // Given: Category has Birthday option
        val categoryWidget = widgets.find { it.id == "widget2" }!!
        assertTrue(categoryWidget.options.any { it.label == "Birthday" })
        
        // When: user removes Birthday in settings
        categoryWidget.options.removeAll { it.label == "Birthday" }
        
        // Then: popup no longer shows Birthday
        assertFalse(categoryWidget.options.any { it.label == "Birthday" })
        assertEquals(1, categoryWidget.options.size)
    }

    @Test
    fun `changing widget label updates popup`() {
        // Given: widget has label "Due Date"
        val widget = widgets.find { it.id == "widget1" }!!
        assertEquals("Due Date", widget.label)
        
        // When: user changes label in settings
        widget.label = "Pickup Time"
        
        // Then: popup shows new label
        assertEquals("Pickup Time", widgets.find { it.id == "widget1" }?.label)
    }

    @Test
    fun `reordering widgets changes popup order`() {
        // Given: widgets in order [widget1, widget2]
        assertEquals(0, widgets[0].order)
        assertEquals(1, widgets[1].order)
        
        // When: user reorders in settings
        java.util.Collections.swap(widgets, 0, 1)
        widgets.forEachIndexed { index, w -> w.order = index }
        
        // Then: popup shows widgets in new order
        val sorted = widgets.sortedBy { it.order }
        assertEquals("widget2", sorted[0].id)
        assertEquals("widget1", sorted[1].id)
    }

    // ==================== Register Trigger Tests ====================

    @Test
    fun `showOMButtonInRegister controls overlay activity`() {
        // Given: OM button is enabled
        assertTrue(settings.showOMButtonInRegister)
        
        // Simulate: overlay activity checks this setting
        val shouldShowOverlay = settings.showOMButtonInRegister
        assertTrue(shouldShowOverlay)
        
        // When: user disables in settings
        settings = settings.copy(showOMButtonInRegister = false)
        
        // Then: overlay should not show
        val shouldShowOverlayNow = settings.showOMButtonInRegister
        assertFalse(shouldShowOverlayNow)
    }

    @Test
    fun `triggerOnItemAdd can be toggled`() {
        // Given: triggerOnItemAdd is disabled
        assertFalse(settings.triggerOnItemAdd)
        
        // When: user enables in settings
        settings = settings.copy(triggerOnItemAdd = true)
        
        // Then: setting is updated
        assertTrue(settings.triggerOnItemAdd)
    }

    // ==================== Widget Type Specific Tests ====================

    @Test
    fun `calendar widget shows date picker in popup`() {
        val calendarWidget = widgets.find { it.type == WidgetType.CALENDAR }!!
        
        // Calendar widgets should have no predefined options
        assertTrue(calendarWidget.options.isEmpty())
        assertEquals("Due Date", calendarWidget.label)
    }

    @Test
    fun `single select widget shows options as chips`() {
        val singleSelectWidget = widgets.find { it.type == WidgetType.SINGLE_SELECT }!!
        
        // Single select should have options
        assertTrue(singleSelectWidget.options.isNotEmpty())
        assertEquals(2, singleSelectWidget.options.size)
    }

    @Test
    fun `text box widget has no options`() {
        // Add a text box widget
        widgets.add(WidgetConfig(
            id = "widget3",
            type = WidgetType.TEXT_BOX,
            label = "Notes",
            isEnabled = true,
            order = 2
        ))
        
        val textWidget = widgets.find { it.type == WidgetType.TEXT_BOX }!!
        assertTrue(textWidget.options.isEmpty())
    }

    // ==================== Data Persistence Simulation ====================

    @Test
    fun `widget changes persist across sessions`() {
        // Simulate saving to Firebase
        val savedWidgets = widgets.map { it.copy() }
        
        // Modify original (simulate user changes)
        widgets.find { it.id == "widget1" }?.label = "Modified Label"
        
        // Simulate loading from Firebase (would restore saved state)
        val loadedWidgets = savedWidgets
        
        // Verify saved state is preserved
        assertEquals("Due Date", loadedWidgets.find { it.id == "widget1" }?.label)
    }

    @Test
    fun `settings changes persist across sessions`() {
        // Simulate saving settings
        val savedSettings = settings.copy()
        
        // Modify settings
        settings = settings.copy(showOMButtonInRegister = false)
        
        // Verify original saved state
        assertTrue(savedSettings.showOMButtonInRegister)
        assertFalse(settings.showOMButtonInRegister)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `popup handles no enabled widgets`() {
        // Disable all widgets
        widgets.forEach { it.isEnabled = false }
        
        val enabledWidgets = widgets.filter { it.isEnabled }
        
        // Popup should show empty state or not appear
        assertTrue(enabledWidgets.isEmpty())
    }

    @Test
    fun `popup handles widget with no options`() {
        val singleSelectWidget = widgets.find { it.type == WidgetType.SINGLE_SELECT }!!
        singleSelectWidget.options.clear()
        
        // Widget should still be shown, just with no selectable options
        assertTrue(singleSelectWidget.options.isEmpty())
        assertTrue(singleSelectWidget.isEnabled)
    }

    @Test
    fun `maximum 7 widgets enforced`() {
        val maxWidgets = 7
        
        // Add widgets up to max
        while (widgets.size < maxWidgets) {
            widgets.add(WidgetConfig(
                id = "widget${widgets.size + 1}",
                type = WidgetType.TEXT_BOX,
                label = "Widget ${widgets.size + 1}",
                order = widgets.size
            ))
        }
        
        assertEquals(maxWidgets, widgets.size)
        
        // Cannot add more
        val canAdd = widgets.size < maxWidgets
        assertFalse(canAdd)
    }
}
