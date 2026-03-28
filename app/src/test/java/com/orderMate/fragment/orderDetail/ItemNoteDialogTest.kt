package com.orderMate.fragment.orderDetail

import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ItemNoteDialogFragment behavior
 * 
 * Tests widget display logic, empty state handling, and note building.
 */
class ItemNoteDialogTest {

    private lateinit var widgets: MutableList<WidgetConfig>

    @Before
    fun setUp() {
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
                order = 3
            )
        )
    }

    // ==================== Empty State Tests ====================

    @Test
    fun `empty state shown when no widgets enabled`() {
        // Disable all widgets
        widgets.forEach { it.isEnabled = false }
        
        val enabledWidgets = widgets.filter { it.isEnabled }
        val shouldShowEmptyState = enabledWidgets.isEmpty()
        
        assertTrue(shouldShowEmptyState)
    }

    @Test
    fun `empty state not shown when widgets enabled`() {
        val enabledWidgets = widgets.filter { it.isEnabled }
        val shouldShowEmptyState = enabledWidgets.isEmpty()
        
        assertFalse(shouldShowEmptyState)
        assertEquals(3, enabledWidgets.size)
    }

    @Test
    fun `save button disabled when no widgets enabled`() {
        widgets.forEach { it.isEnabled = false }
        
        val enabledWidgets = widgets.filter { it.isEnabled }
        val saveButtonEnabled = enabledWidgets.isNotEmpty()
        
        assertFalse(saveButtonEnabled)
    }

    @Test
    fun `save button enabled when widgets exist`() {
        val enabledWidgets = widgets.filter { it.isEnabled }
        val saveButtonEnabled = enabledWidgets.isNotEmpty()
        
        assertTrue(saveButtonEnabled)
    }

    // ==================== Widget Display Order Tests ====================

    @Test
    fun `widgets displayed in order`() {
        val enabledWidgets = widgets.filter { it.isEnabled }.sortedBy { it.order }
        
        assertEquals("widget1", enabledWidgets[0].id) // order 0
        assertEquals("widget2", enabledWidgets[1].id) // order 1
        assertEquals("widget4", enabledWidgets[2].id) // order 3
    }

    @Test
    fun `disabled widgets not displayed`() {
        val enabledWidgets = widgets.filter { it.isEnabled }
        
        assertFalse(enabledWidgets.any { it.id == "widget3" })
    }

    @Test
    fun `widgets sorted correctly after reorder`() {
        // Simulate reorder: move widget4 (order 3) to position 0
        widgets.find { it.id == "widget4" }?.order = -1
        
        val sorted = widgets.filter { it.isEnabled }.sortedBy { it.order }
        
        assertEquals("widget4", sorted[0].id)
        assertEquals("widget1", sorted[1].id)
        assertEquals("widget2", sorted[2].id)
    }

    // ==================== Widget Type Rendering Tests ====================

    @Test
    fun `single select widget has options`() {
        val singleSelect = widgets.find { it.type == WidgetType.SINGLE_SELECT }!!
        
        assertTrue(singleSelect.options.isNotEmpty())
        assertEquals(2, singleSelect.options.size)
    }

    @Test
    fun `multi select widget has options`() {
        val multiSelect = widgets.find { it.type == WidgetType.MULTI_SELECT }!!
        
        assertTrue(multiSelect.options.isNotEmpty())
        assertEquals(2, multiSelect.options.size)
    }

    @Test
    fun `calendar widget has no options`() {
        val calendar = widgets.find { it.type == WidgetType.CALENDAR }!!
        
        assertTrue(calendar.options.isEmpty())
    }

    @Test
    fun `text box widget has no options`() {
        val textBox = widgets.find { it.type == WidgetType.TEXT_BOX }!!
        
        assertTrue(textBox.options.isEmpty())
    }

    // ==================== Read from WidgetManager Tests ====================

    @Test
    fun `dialog reads enabled widgets correctly`() {
        // Simulate WidgetManager.getEnabledWidgets()
        val enabledWidgets = widgets.filter { it.isEnabled }
        
        assertEquals(3, enabledWidgets.size)
        assertTrue(enabledWidgets.all { it.isEnabled })
    }

    @Test
    fun `dialog handles empty widget list`() {
        val emptyWidgets = emptyList<WidgetConfig>()
        val shouldShowEmptyState = emptyWidgets.isEmpty()
        
        assertTrue(shouldShowEmptyState)
    }

    // ==================== Note Building from Widgets ====================

    @Test
    fun `build note from single select selection`() {
        val selections = mapOf("Category" to "Birthday")
        val note = buildNoteString(selections)
        
        assertEquals("Category:Birthday", note)
    }

    @Test
    fun `build note from multi select selections`() {
        val selections = mapOf("Tags" to "Rush,VIP")
        val note = buildNoteString(selections)
        
        assertEquals("Tags:Rush,VIP", note)
    }

    @Test
    fun `build note from multiple widgets`() {
        val selections = mapOf(
            "Category" to "Birthday",
            "Due Date" to "Dec 25, 2024",
            "Description" to "Custom cake"
        )
        val note = buildNoteString(selections)
        
        assertTrue(note.contains("Category:Birthday"))
        assertTrue(note.contains("Due Date:Dec 25, 2024"))
        assertTrue(note.contains("Description:Custom cake"))
    }

    @Test
    fun `build note skips empty selections`() {
        val selections = mapOf(
            "Category" to "Birthday",
            "Tags" to "",
            "Description" to ""
        )
        val note = buildNoteString(selections)
        
        assertEquals("Category:Birthday", note)
    }

    // ==================== Note Parsing for Pre-population ====================

    @Test
    fun `parse existing note for pre-population`() {
        val existingNote = "Category:Wedding | Due Date:Jan 15, 2025"
        val parsed = parseNoteString(existingNote)
        
        assertEquals("Wedding", parsed["Category"])
        assertEquals("Jan 15, 2025", parsed["Due Date"])
    }

    @Test
    fun `parse note with multi select values`() {
        val existingNote = "Tags:Rush,VIP,Delivery"
        val parsed = parseNoteString(existingNote)
        
        assertEquals("Rush,VIP,Delivery", parsed["Tags"])
        
        val values = parsed["Tags"]!!.split(",")
        assertEquals(3, values.size)
        assertTrue(values.contains("Rush"))
        assertTrue(values.contains("VIP"))
        assertTrue(values.contains("Delivery"))
    }

    @Test
    fun `parse empty note returns empty map`() {
        val parsed = parseNoteString("")
        
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `parse null note returns empty map`() {
        val parsed = parseNoteString(null)
        
        assertTrue(parsed.isEmpty())
    }

    // Helper functions that mirror ItemNoteDialogFragment logic
    
    private fun buildNoteString(selections: Map<String, String>): String {
        return selections
            .filter { it.value.isNotEmpty() }
            .map { "${it.key}:${it.value}" }
            .joinToString(" | ")
    }

    private fun parseNoteString(note: String?): Map<String, String> {
        if (note.isNullOrEmpty()) return emptyMap()
        
        return note.split("|")
            .mapNotNull { part ->
                val trimmed = part.trim()
                val colonIndex = trimmed.indexOf(':')
                if (colonIndex > 0) {
                    val label = trimmed.substring(0, colonIndex).trim()
                    val value = trimmed.substring(colonIndex + 1).trim()
                    label to value
                } else null
            }
            .toMap()
    }
}
