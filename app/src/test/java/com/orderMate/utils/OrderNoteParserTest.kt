package com.orderMate.utils

import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OrderNoteParser
 * 
 * Tests note parsing with label-based matching (unique labels enforced).
 * Verifies case-insensitive matching and legacy format support.
 */
class OrderNoteParserTest {

    private lateinit var itemWidgets: List<WidgetConfig>
    private lateinit var orderWidgets: List<WidgetConfig>
    private lateinit var allWidgets: List<WidgetConfig>

    @Before
    fun setUp() {
        itemWidgets = listOf(
            WidgetConfig(
                id = "item_calendar",
                type = WidgetType.CALENDAR,
                label = "Due Date",
                isEnabled = true,
                level = NoteLevel.ITEM,
                order = 0
            ),
            WidgetConfig(
                id = "item_category",
                type = WidgetType.SINGLE_SELECT,
                label = "Category",
                isEnabled = true,
                level = NoteLevel.ITEM,
                order = 1
            ),
            WidgetConfig(
                id = "item_tags",
                type = WidgetType.MULTI_SELECT,
                label = "Tags",
                isEnabled = true,
                level = NoteLevel.ITEM,
                order = 2
            ),
            WidgetConfig(
                id = "item_desc",
                type = WidgetType.TEXT_BOX,
                label = "Description",
                isEnabled = true,
                level = NoteLevel.ITEM,
                order = 3
            )
        )
        
        orderWidgets = listOf(
            WidgetConfig(
                id = "order_deadline",
                type = WidgetType.CALENDAR,
                label = "Deadline",
                isEnabled = true,
                level = NoteLevel.ORDER,
                order = 0
            ),
            WidgetConfig(
                id = "order_group",
                type = WidgetType.SINGLE_SELECT,
                label = "Group",
                isEnabled = true,
                level = NoteLevel.ORDER,
                order = 1
            )
        )
        
        allWidgets = itemWidgets + orderWidgets
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `parseNoteToMap extracts label value pairs`() {
        val note = "Category:Birthday • Tags:Rush,VIP • Description:Special order"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals(3, result.size)
        assertEquals("Birthday", result["Category"])
        assertEquals("Rush,VIP", result["Tags"])
        assertEquals("Special order", result["Description"])
    }

    @Test
    fun `parseNoteToMap handles pipe separator`() {
        val note = "Category:Birthday | Tags:Rush | Description:Test"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals(3, result.size)
        assertEquals("Birthday", result["Category"])
        assertEquals("Rush", result["Tags"])
        assertEquals("Test", result["Description"])
    }

    @Test
    fun `parseNoteToMap handles empty note`() {
        val result = OrderNoteParser.parseNoteToMap("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseNoteToMap handles null note`() {
        val result = OrderNoteParser.parseNoteToMap(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseNoteToMap trims whitespace`() {
        val note = "  Category : Birthday  •  Tags : Rush  "
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals("Birthday", result["Category"])
        assertEquals("Rush", result["Tags"])
    }

    // ==================== Legacy Format Support ====================

    @Test
    fun `parseNoteToMap handles legacy widgetId format`() {
        val note = "[abc123]Category:Birthday • [def456]Tags:Rush"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals(2, result.size)
        assertEquals("Birthday", result["Category"])
        assertEquals("Rush", result["Tags"])
    }

    @Test
    fun `parseNoteToMap strips widgetId from legacy format`() {
        val note = "[widget_id_12345]Due Date:Apr 20, 2026"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals("Apr 20, 2026", result["Due Date"])
        assertNull(result["[widget_id_12345]Due Date"])
    }

    @Test
    fun `parseNoteToMap handles mixed legacy and new format`() {
        val note = "[abc]Category:Birthday • Tags:VIP • [def]Description:Test"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals(3, result.size)
        assertEquals("Birthday", result["Category"])
        assertEquals("VIP", result["Tags"])
        assertEquals("Test", result["Description"])
    }

    // ==================== Widget Matching Tests ====================

    @Test
    fun `parseNotesByWidgetType matches widgets by label`() {
        val note = "Category:Birthday • Tags:Rush,VIP"
        val result = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ITEM)
        
        assertEquals(2, result.size)
        
        val categoryWidget = result.keys.find { it.label == "Category" }
        assertNotNull(categoryWidget)
        assertEquals("Birthday", result[categoryWidget])
        
        val tagsWidget = result.keys.find { it.label == "Tags" }
        assertNotNull(tagsWidget)
        assertEquals("Rush,VIP", result[tagsWidget])
    }

    @Test
    fun `parseNotesByWidgetType is case insensitive`() {
        val note = "category:Birthday • TAGS:Rush"
        val result = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ITEM)
        
        assertEquals(2, result.size)
        assertTrue(result.keys.any { it.label == "Category" })
        assertTrue(result.keys.any { it.label == "Tags" })
    }

    @Test
    fun `parseNotesByWidgetType filters by level`() {
        val note = "Category:Birthday • Deadline:Apr 20, 2026"
        
        // ITEM level should only match Category
        val itemResult = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ITEM)
        assertEquals(1, itemResult.size)
        assertTrue(itemResult.keys.any { it.label == "Category" })
        
        // ORDER level should only match Deadline
        val orderResult = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ORDER)
        assertEquals(1, orderResult.size)
        assertTrue(orderResult.keys.any { it.label == "Deadline" })
    }

    @Test
    fun `parseNotesByWidgetType only matches enabled widgets`() {
        val disabledWidgets = itemWidgets.map { 
            it.copy(isEnabled = if (it.label == "Category") false else true)
        }
        
        val note = "Category:Birthday • Tags:Rush"
        val result = OrderNoteParser.parseNotesByWidgetType(note, disabledWidgets, NoteLevel.ITEM)
        
        assertEquals(1, result.size)
        assertTrue(result.keys.any { it.label == "Tags" })
        assertFalse(result.keys.any { it.label == "Category" })
    }

    @Test
    fun `parseNotesByWidgetType returns empty for unmatched labels`() {
        val note = "Unknown:Value • NonExistent:Test"
        val result = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ITEM)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseNotesByWidgetType handles legacy format with widget matching`() {
        val note = "[old_id]Category:Birthday • [another_id]Tags:Rush"
        val result = OrderNoteParser.parseNotesByWidgetType(note, allWidgets, NoteLevel.ITEM)
        
        assertEquals(2, result.size)
        assertEquals("Birthday", result[itemWidgets.find { it.label == "Category" }])
        assertEquals("Rush", result[itemWidgets.find { it.label == "Tags" }])
    }

    // ==================== Tag Extraction Tests ====================

    @Test
    fun `extractTagsFromNote returns parsed tags`() {
        val note = "Category:Birthday • Tags:Rush,VIP"
        val tags = OrderNoteParser.extractTagsFromNote(note, allWidgets, NoteLevel.ITEM)
        
        assertTrue(tags.isNotEmpty())
        assertTrue(tags.any { it.label == "Category" && it.value == "Birthday" })
    }

    @Test
    fun `extractTagsFromNote splits multi-select values`() {
        val note = "Tags:Rush,VIP,Delivery"
        val tags = OrderNoteParser.extractTagsFromNote(note, allWidgets, NoteLevel.ITEM)
        
        val tagValues = tags.filter { it.label == "Tags" }.map { it.value }
        assertEquals(3, tagValues.size)
        assertTrue(tagValues.contains("Rush"))
        assertTrue(tagValues.contains("VIP"))
        assertTrue(tagValues.contains("Delivery"))
    }

    @Test
    fun `extractTagsFromNote excludes textbox when specified`() {
        val note = "Category:Birthday • Description:Some text"
        val tags = OrderNoteParser.extractTagsFromNote(note, allWidgets, NoteLevel.ITEM, includeTextBox = false)
        
        assertTrue(tags.any { it.label == "Category" })
        assertFalse(tags.any { it.label == "Description" })
    }

    @Test
    fun `extractTagsFromNote includes textbox by default`() {
        val note = "Category:Birthday • Description:Some text"
        val tags = OrderNoteParser.extractTagsFromNote(note, allWidgets, NoteLevel.ITEM)
        
        assertTrue(tags.any { it.label == "Category" })
        assertTrue(tags.any { it.label == "Description" })
    }

    // ==================== Date Extraction Tests ====================

    @Test
    fun `extractDateFromNote returns date for calendar widget`() {
        val note = "Due Date:Apr 20, 2026 • Category:Birthday"
        val date = OrderNoteParser.extractDateFromNote(note, allWidgets, NoteLevel.ITEM)
        
        assertNotNull(date)
    }

    @Test
    fun `extractDateFromNote returns null when no calendar widget`() {
        val note = "Category:Birthday • Tags:Rush"
        val date = OrderNoteParser.extractDateFromNote(note, allWidgets, NoteLevel.ITEM)
        
        assertNull(date)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `parseNoteToMap handles colon in value`() {
        val note = "Description:Time is 10:30 AM"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        assertEquals("Time is 10:30 AM", result["Description"])
    }

    @Test
    fun `parseNoteToMap handles empty value`() {
        val note = "Category: • Tags:Rush"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        // Empty values should be excluded
        assertNull(result["Category"])
        assertEquals("Rush", result["Tags"])
    }

    @Test
    fun `parseNoteToMap handles empty label`() {
        val note = ":Value • Tags:Rush"
        val result = OrderNoteParser.parseNoteToMap(note)
        
        // Empty labels should be excluded
        assertEquals(1, result.size)
        assertEquals("Rush", result["Tags"])
    }

    @Test
    fun `parseNotesByWidgetType handles empty widgets list`() {
        val note = "Category:Birthday"
        val result = OrderNoteParser.parseNotesByWidgetType(note, emptyList(), NoteLevel.ITEM)
        
        assertTrue(result.isEmpty())
    }
}
