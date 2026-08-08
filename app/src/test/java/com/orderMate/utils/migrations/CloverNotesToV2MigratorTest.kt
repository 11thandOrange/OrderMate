package com.orderMate.utils.migrations

import com.orderMate.modals.WidgetType
import com.orderMate.utils.OrderNoteParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for CloverNotesToV2Migrator (#151).
 *
 * The real object depends on a live Android Context, the Clover SDK's Order/LineItem
 * classes, and Firebase - none available in a plain JVM test - so, following this
 * codebase's convention (see OrderHistoryStoreTest, WidgetManagerTest), these tests mirror
 * the migrator's pure decision logic locally.
 *
 * The note-parsing itself is NOT mirrored: it calls the real OrderNoteParser.parseNoteToMap,
 * since that's what the migrator actually relies on to understand both note formats, and it's
 * plain JVM-testable already.
 */
class CloverNotesToV2MigratorTest {

    private val labelTypeMap = mapOf(
        "pickup date" to WidgetType.CALENDAR,
        "due date" to WidgetType.CALENDAR,
        "status" to WidgetType.SINGLE_SELECT,
        "category" to WidgetType.SINGLE_SELECT,
        "subcategory" to WidgetType.MULTI_SELECT,
        "type" to WidgetType.MULTI_SELECT,
        "description" to WidgetType.TEXT_BOX,
        "notes" to WidgetType.TEXT_BOX
    )

    private fun inferWidgetType(label: String): WidgetType {
        return labelTypeMap[label.lowercase().trim()] ?: WidgetType.SINGLE_SELECT
    }

    // Mirrors analyzeNotes()'s "unmigrated" filter: only labels not already backed by a widget.
    private fun unmigratedLabels(note: String, knownLabels: Set<String>): Map<String, String> {
        return OrderNoteParser.parseNoteToMap(note).filterKeys { it.trim().lowercase() !in knownLabels }
    }

    // ==================== Note format handling ====================

    @Test
    fun `current-format note with no existing widgets is entirely unmigrated`() {
        val note = "Category:Custom Cake • Status:In Progress"

        val result = unmigratedLabels(note, knownLabels = emptySet())

        assertEquals(mapOf("Category" to "Custom Cake", "Status" to "In Progress"), result)
    }

    @Test
    fun `legacy bracket-tagged note is treated the same as a bare label once widget-id is stripped`() {
        val note = "[abc-123]Category:Custom Cake"

        val result = unmigratedLabels(note, knownLabels = emptySet())

        assertEquals(mapOf("Category" to "Custom Cake"), result)
    }

    @Test
    fun `pipe-delimited legacy note (pre-widget system) is parsed the same as bullet-delimited`() {
        val note = "Category:Custom Cake|Status:In Progress"

        val result = unmigratedLabels(note, knownLabels = emptySet())

        assertEquals(mapOf("Category" to "Custom Cake", "Status" to "In Progress"), result)
    }

    @Test
    fun `labels already backed by an existing widget are excluded, regardless of note format`() {
        val note = "Category:Custom Cake • Status:In Progress"

        val result = unmigratedLabels(note, knownLabels = setOf("category"))

        assertEquals(mapOf("Status" to "In Progress"), result)
    }

    @Test
    fun `label matching is case-insensitive against known widgets`() {
        val note = "CATEGORY:Custom Cake"

        val result = unmigratedLabels(note, knownLabels = setOf("category"))

        assertTrue("a label already covered by a widget should never be reported as unmigrated", result.isEmpty())
    }

    @Test
    fun `note where every label already has a widget yields nothing to migrate`() {
        val note = "Category:Custom Cake • Status:In Progress"

        val result = unmigratedLabels(note, knownLabels = setOf("category", "status"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank note yields nothing to migrate`() {
        val result = unmigratedLabels("", knownLabels = emptySet())

        assertTrue(result.isEmpty())
    }

    // ==================== Widget type inference ====================

    @Test
    fun `known labels infer their documented widget type`() {
        assertEquals(WidgetType.CALENDAR, inferWidgetType("Due Date"))
        assertEquals(WidgetType.SINGLE_SELECT, inferWidgetType("Category"))
        assertEquals(WidgetType.MULTI_SELECT, inferWidgetType("Subcategory"))
        assertEquals(WidgetType.TEXT_BOX, inferWidgetType("Description"))
    }

    @Test
    fun `label inference is case-insensitive`() {
        assertEquals(WidgetType.CALENDAR, inferWidgetType("PICKUP DATE"))
    }

    @Test
    fun `unrecognized labels default to single select`() {
        assertEquals(WidgetType.SINGLE_SELECT, inferWidgetType("Some Brand New Label"))
    }

    // ==================== Validation against existing widgets ====================

    // Mirrors validateWidgets()'s label-collision checks.
    private fun collisionErrors(proposedLabels: List<String>, knownLabels: Set<String>): List<String> {
        val errors = mutableListOf<String>()
        val seenLabels = mutableSetOf<String>()

        for (label in proposedLabels) {
            val normalized = label.lowercase()
            if (normalized in seenLabels) {
                errors.add("Duplicate widget label: $label")
            }
            if (normalized in knownLabels) {
                errors.add("Widget label collides with an existing widget: $label")
            }
            seenLabels.add(normalized)
        }

        return errors
    }

    @Test
    fun `a synthesized widget colliding with an existing widget label is rejected`() {
        val errors = collisionErrors(listOf("Category"), knownLabels = setOf("category"))

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("collides"))
    }

    @Test
    fun `two synthesized widgets with the same label are rejected as duplicates`() {
        val errors = collisionErrors(listOf("Rush Order", "rush order"), knownLabels = emptySet())

        assertTrue(errors.any { it.contains("Duplicate widget label") })
    }

    @Test
    fun `synthesized widgets with distinct, non-colliding labels pass validation cleanly`() {
        val errors = collisionErrors(listOf("Rush Order", "Gift Wrap"), knownLabels = setOf("category", "status"))

        assertTrue(errors.isEmpty())
    }
}
