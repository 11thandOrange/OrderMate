package com.orderMate.fragment.orderDetail

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ItemNoteDialogFragment note building/parsing logic
 * 
 * Tests the note format: "Label:Value | Label:Value1,Value2 | ..."
 */
class ItemNoteBuilderTest {

    // Note format constants
    private val SEPARATOR = " | "
    private val LABEL_VALUE_DELIMITER = ":"
    private val MULTI_VALUE_DELIMITER = ","

    /**
     * Build a note string from parts
     */
    private fun buildNote(parts: List<Pair<String, String>>): String {
        return parts
            .filter { it.second.isNotEmpty() }
            .map { "${it.first}${LABEL_VALUE_DELIMITER}${it.second}" }
            .joinToString(SEPARATOR)
    }

    /**
     * Parse a note string into parts
     */
    private fun parseNote(note: String): Map<String, String> {
        if (note.isEmpty()) return emptyMap()
        
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

    /**
     * Parse multi-select values
     */
    private fun parseMultiValues(value: String): List<String> {
        return value.split(MULTI_VALUE_DELIMITER).map { it.trim() }
    }

    // ==================== Build Note Tests ====================

    @Test
    fun `buildNote with single value`() {
        val parts = listOf("Category" to "Birthday")
        val note = buildNote(parts)
        assertEquals("Category:Birthday", note)
    }

    @Test
    fun `buildNote with multiple values`() {
        val parts = listOf(
            "Category" to "Birthday",
            "Due Date" to "2024-12-25"
        )
        val note = buildNote(parts)
        assertEquals("Category:Birthday | Due Date:2024-12-25", note)
    }

    @Test
    fun `buildNote filters empty values`() {
        val parts = listOf(
            "Category" to "Birthday",
            "Tags" to "",
            "Due Date" to "2024-12-25"
        )
        val note = buildNote(parts)
        assertEquals("Category:Birthday | Due Date:2024-12-25", note)
    }

    @Test
    fun `buildNote with multi-select values joined by comma`() {
        val multiValues = listOf("Rush", "VIP", "Delivery")
        val parts = listOf(
            "Tags" to multiValues.joinToString(MULTI_VALUE_DELIMITER)
        )
        val note = buildNote(parts)
        assertEquals("Tags:Rush,VIP,Delivery", note)
    }

    @Test
    fun `buildNote empty when all values empty`() {
        val parts = listOf(
            "Category" to "",
            "Tags" to ""
        )
        val note = buildNote(parts)
        assertEquals("", note)
    }

    @Test
    fun `buildNote preserves spaces in values`() {
        val parts = listOf("Description" to "Happy Birthday John!")
        val note = buildNote(parts)
        assertEquals("Description:Happy Birthday John!", note)
    }

    // ==================== Parse Note Tests ====================

    @Test
    fun `parseNote single value`() {
        val note = "Category:Birthday"
        val parsed = parseNote(note)
        assertEquals(1, parsed.size)
        assertEquals("Birthday", parsed["Category"])
    }

    @Test
    fun `parseNote multiple values`() {
        val note = "Category:Birthday | Due Date:2024-12-25"
        val parsed = parseNote(note)
        assertEquals(2, parsed.size)
        assertEquals("Birthday", parsed["Category"])
        assertEquals("2024-12-25", parsed["Due Date"])
    }

    @Test
    fun `parseNote handles multi-select value`() {
        val note = "Tags:Rush,VIP,Delivery"
        val parsed = parseNote(note)
        assertEquals("Rush,VIP,Delivery", parsed["Tags"])
        
        // Parse individual multi-values
        val values = parseMultiValues(parsed["Tags"]!!)
        assertEquals(3, values.size)
        assertTrue(values.contains("Rush"))
        assertTrue(values.contains("VIP"))
        assertTrue(values.contains("Delivery"))
    }

    @Test
    fun `parseNote empty string returns empty map`() {
        val parsed = parseNote("")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `parseNote handles extra spaces`() {
        val note = "  Category : Birthday  |  Due Date : 2024-12-25  "
        val parsed = parseNote(note)
        assertEquals(2, parsed.size)
        assertEquals("Birthday", parsed["Category"])
        assertEquals("2024-12-25", parsed["Due Date"])
    }

    @Test
    fun `parseNote handles colons in value`() {
        val note = "Time:10:30 AM"
        val parsed = parseNote(note)
        assertEquals("10:30 AM", parsed["Time"])
    }

    @Test
    fun `parseNote ignores malformed parts`() {
        val note = "Category:Birthday | InvalidPart | Due Date:2024-12-25"
        val parsed = parseNote(note)
        assertEquals(2, parsed.size)
        assertEquals("Birthday", parsed["Category"])
        assertEquals("2024-12-25", parsed["Due Date"])
    }

    // ==================== Roundtrip Tests ====================

    @Test
    fun `roundtrip single select`() {
        val original = listOf("Category" to "Wedding")
        val note = buildNote(original)
        val parsed = parseNote(note)
        assertEquals("Wedding", parsed["Category"])
    }

    @Test
    fun `roundtrip multi select`() {
        val values = listOf("Rush", "VIP")
        val original = listOf("Tags" to values.joinToString(","))
        val note = buildNote(original)
        val parsed = parseNote(note)
        val restoredValues = parseMultiValues(parsed["Tags"]!!)
        assertEquals(2, restoredValues.size)
        assertTrue(restoredValues.containsAll(values))
    }

    @Test
    fun `roundtrip full note`() {
        val original = listOf(
            "Category" to "Birthday",
            "Tags" to "Rush,VIP",
            "Due Date" to "2024-12-25 10:00",
            "Description" to "Custom cake for John"
        )
        
        val note = buildNote(original)
        val parsed = parseNote(note)
        
        assertEquals(4, parsed.size)
        assertEquals("Birthday", parsed["Category"])
        assertEquals("Rush,VIP", parsed["Tags"])
        assertEquals("2024-12-25 10:00", parsed["Due Date"])
        assertEquals("Custom cake for John", parsed["Description"])
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `note with special characters`() {
        val parts = listOf("Description" to "Price: \$50 (10% off!)")
        val note = buildNote(parts)
        val parsed = parseNote(note)
        assertEquals("Price: \$50 (10% off!)", parsed["Description"])
    }

    @Test
    fun `note with unicode characters`() {
        val parts = listOf("Description" to "Happy Birthday! 🎂🎉")
        val note = buildNote(parts)
        val parsed = parseNote(note)
        assertEquals("Happy Birthday! 🎂🎉", parsed["Description"])
    }

    @Test
    fun `multi select with single value`() {
        val values = parseMultiValues("OnlyOne")
        assertEquals(1, values.size)
        assertEquals("OnlyOne", values[0])
    }

    @Test
    fun `multi select with empty string`() {
        val values = parseMultiValues("")
        assertEquals(1, values.size)
        assertEquals("", values[0])
    }
}
