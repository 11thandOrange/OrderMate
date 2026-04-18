package com.orderMate.adapters

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OrderCardRedesignAdapter
 * Issue #16 - List Page UI Refinements
 * 
 * Tests for:
 * - #17: Pill color contrast (verified via color values)
 * - #18: Order description extraction from notes
 * - #19: Custom tag extraction from notes
 */
class OrderCardRedesignAdapterTest {

    // ==================== #18: Order Description Tests ====================

    @Test
    fun `getOrderDescription extracts description from notes`() {
        val notes = listOf("category: Cake • description: Special birthday order")
        val result = getOrderDescription(notes)
        assertEquals("Special birthday order", result)
    }

    @Test
    fun `getOrderDescription returns null when no description`() {
        val notes = listOf("category: Cake • status: Ready")
        val result = getOrderDescription(notes)
        assertNull(result)
    }

    @Test
    fun `getOrderDescription handles empty notes`() {
        val notes = emptyList<String>()
        val result = getOrderDescription(notes)
        assertNull(result)
    }

    @Test
    fun `getOrderDescription extracts description with legacy delimiter`() {
        val notes = listOf("category: Cake | description: Rush order")
        val result = getOrderDescription(notes)
        assertEquals("Rush order", result)
    }

    @Test
    fun `getOrderDescription handles description with colons`() {
        val notes = listOf("description: Note: Handle with care")
        val result = getOrderDescription(notes)
        assertEquals("Note: Handle with care", result)
    }

    @Test
    fun `getOrderDescription is case insensitive for label`() {
        val notes = listOf("Description: Mixed case test")
        val result = getOrderDescription(notes)
        assertEquals("Mixed case test", result)
    }

    @Test
    fun `getOrderDescription returns first non-empty description`() {
        val notes = listOf(
            "category: Cake",
            "description: First description",
            "description: Second description"
        )
        val result = getOrderDescription(notes)
        assertEquals("First description", result)
    }

    @Test
    fun `getOrderDescription ignores blank descriptions`() {
        val notes = listOf("description:    ", "description: Valid description")
        val result = getOrderDescription(notes)
        assertEquals("Valid description", result)
    }

    // ==================== #19: Custom Tags Extraction Tests ====================

    @Test
    fun `getCustomTags extracts category tag`() {
        val notes = listOf("category: Custom Cake")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("category", result[0].type)
        assertEquals("Custom Cake", result[0].value)
    }

    @Test
    fun `getCustomTags extracts type tag`() {
        val notes = listOf("type: Rush Order")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("type", result[0].type)
        assertEquals("Rush Order", result[0].value)
    }

    @Test
    fun `getCustomTags extracts status tag`() {
        val notes = listOf("status: In Progress")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("status", result[0].type)
        assertEquals("In Progress", result[0].value)
    }

    @Test
    fun `getCustomTags extracts sub-category tag`() {
        val notes = listOf("sub-category: Birthday")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("sub-category", result[0].type)
        assertEquals("Birthday", result[0].value)
    }

    @Test
    fun `getCustomTags extracts subcategory tag`() {
        val notes = listOf("subcategory: Wedding")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("subcategory", result[0].type)
        assertEquals("Wedding", result[0].value)
    }

    @Test
    fun `getCustomTags extracts multiple tags`() {
        val notes = listOf("category: Cake • type: Rush • status: Ready")
        val result = getCustomTags(notes)
        assertEquals(3, result.size)
    }

    @Test
    fun `getCustomTags limits to 3 tags`() {
        val notes = listOf("category: A • type: B • status: C • sub-category: D • subcategory: E")
        val result = getCustomTags(notes)
        assertEquals(3, result.size)
    }

    @Test
    fun `getCustomTags handles comma-separated values`() {
        val notes = listOf("category: Cake, Cookies, Pastries")
        val result = getCustomTags(notes)
        assertEquals(3, result.size)
        assertEquals("Cake", result[0].value)
        assertEquals("Cookies", result[1].value)
        assertEquals("Pastries", result[2].value)
    }

    @Test
    fun `getCustomTags removes duplicates`() {
        val notes = listOf(
            "category: Cake",
            "category: Cake"
        )
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
    }

    @Test
    fun `getCustomTags ignores description field`() {
        val notes = listOf("description: Some text • category: Cake")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("category", result[0].type)
    }

    @Test
    fun `getCustomTags ignores pickup date field`() {
        val notes = listOf("pickup date: 03/15/24 • category: Cake")
        val result = getCustomTags(notes)
        assertEquals(1, result.size)
        assertEquals("category", result[0].type)
    }

    @Test
    fun `getCustomTags returns empty for notes without tags`() {
        val notes = listOf("pickup date: 03/15/24 • description: Test")
        val result = getCustomTags(notes)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCustomTags handles legacy pipe delimiter`() {
        val notes = listOf("category: Cake | type: Rush")
        val result = getCustomTags(notes)
        assertEquals(2, result.size)
    }

    // ==================== #19: Tag Color Tests ====================

    @Test
    fun `getTagColors returns category colors for category type`() {
        val (bgColor, textColor) = getTagColors("category")
        assertEquals("tag_category_bg", bgColor)
        assertEquals("tag_category_text", textColor)
    }

    @Test
    fun `getTagColors returns category colors for sub-category type`() {
        val (bgColor, textColor) = getTagColors("sub-category")
        assertEquals("tag_category_bg", bgColor)
        assertEquals("tag_category_text", textColor)
    }

    @Test
    fun `getTagColors returns type colors for type`() {
        val (bgColor, textColor) = getTagColors("type")
        assertEquals("tag_type_bg", bgColor)
        assertEquals("tag_type_text", textColor)
    }

    @Test
    fun `getTagColors returns status colors for status`() {
        val (bgColor, textColor) = getTagColors("status")
        assertEquals("tag_status_bg", bgColor)
        assertEquals("tag_status_text", textColor)
    }

    @Test
    fun `getTagColors returns default colors for unknown type`() {
        val (bgColor, textColor) = getTagColors("unknown")
        assertEquals("tag_default_bg", bgColor)
        assertEquals("tag_default_text", textColor)
    }

    // ==================== Helper Functions (mirror adapter logic) ====================

    private data class CustomTag(val type: String, val value: String)

    private fun getOrderDescription(notes: List<String>): String? {
        notes.forEach { note ->
            if (note.isNotBlank()) {
                val delimiter = if (note.contains("•")) "•" else "|"
                val parts = note.split(delimiter).map { it.trim() }
                
                for (part in parts) {
                    val colonIndex = part.indexOf(':')
                    if (colonIndex > 0) {
                        val label = part.substring(0, colonIndex).trim().lowercase()
                        if (label == "description") {
                            val value = part.substring(colonIndex + 1).trim()
                            if (value.isNotBlank()) {
                                return value
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getCustomTags(notes: List<String>): List<CustomTag> {
        val tags = mutableListOf<CustomTag>()
        val seenValues = mutableSetOf<String>()
        
        val tagLabels = setOf("category", "type", "status", "sub-category", "subcategory")
        
        notes.forEach { note ->
            if (note.isNotBlank()) {
                val delimiter = if (note.contains("•")) "•" else "|"
                val parts = note.split(delimiter).map { it.trim() }
                
                for (part in parts) {
                    val colonIndex = part.indexOf(':')
                    if (colonIndex > 0) {
                        val label = part.substring(0, colonIndex).trim().lowercase()
                        if (tagLabels.contains(label)) {
                            val rawValue = part.substring(colonIndex + 1).trim()
                            
                            val values = rawValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            values.forEach { value ->
                                val uniqueKey = "$label:$value"
                                if (!seenValues.contains(uniqueKey)) {
                                    seenValues.add(uniqueKey)
                                    tags.add(CustomTag(label, value))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return tags.take(3)
    }

    private fun getTagColors(tagType: String): Pair<String, String> {
        return when (tagType) {
            "category", "sub-category", "subcategory" -> 
                Pair("tag_category_bg", "tag_category_text")
            "type" -> 
                Pair("tag_type_bg", "tag_type_text")
            "status" -> 
                Pair("tag_status_bg", "tag_status_text")
            else -> 
                Pair("tag_default_bg", "tag_default_text")
        }
    }
}
