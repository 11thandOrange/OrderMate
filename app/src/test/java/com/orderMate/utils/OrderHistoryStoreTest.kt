package com.orderMate.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OrderHistoryStore's merge/cap logic (#138).
 *
 * The real class depends on a live Android Context/SharedPreferences and the Clover SDK's
 * JSONObject-backed Order class, neither available in a plain JVM test - so, following this
 * codebase's existing convention (see WidgetManagerTest, OrderFilterUtilsTest), these tests
 * mirror the pure merge/eviction logic locally with a lightweight stand-in.
 */
class OrderHistoryStoreTest {

    private data class TestOrder(val id: String, val createdTime: Long?)

    // Mirrors remember()'s upsert: new/updated entries overwrite by id, everything else is kept.
    private fun mergeById(
        stored: Map<String, TestOrder>,
        incoming: List<TestOrder>
    ): Map<String, TestOrder> {
        val result = stored.toMutableMap()
        incoming.forEach { result[it.id] = it }
        return result
    }

    // Mirrors remember()'s cap: once over the limit, keep the most-recently-created orders.
    private fun trimToLimit(stored: Map<String, TestOrder>, limit: Int): Map<String, TestOrder> {
        if (stored.size <= limit) return stored
        return stored.values
            .sortedByDescending { it.createdTime ?: 0L }
            .take(limit)
            .associateBy { it.id }
    }

    @Test
    fun `remembering a new order adds it to the store`() {
        val stored = mergeById(emptyMap(), listOf(TestOrder("a", 1L)))

        assertEquals(setOf("a"), stored.keys)
    }

    @Test
    fun `remembering an already-known order overwrites rather than duplicates`() {
        val stored = mergeById(
            mapOf("a" to TestOrder("a", 1L)),
            listOf(TestOrder("a", 2L))
        )

        assertEquals(1, stored.size)
        assertEquals(2L, stored.getValue("a").createdTime)
    }

    @Test
    fun `previously remembered orders survive when they are not in the latest batch`() {
        val stored = mergeById(
            mapOf("a" to TestOrder("a", 1L), "b" to TestOrder("b", 2L)),
            listOf(TestOrder("c", 3L))
        )

        assertEquals(setOf("a", "b", "c"), stored.keys)
    }

    @Test
    fun `trimming under the limit is a no-op`() {
        val stored = mapOf("a" to TestOrder("a", 1L), "b" to TestOrder("b", 2L))

        val trimmed = trimToLimit(stored, limit = 5)

        assertEquals(stored, trimmed)
    }

    @Test
    fun `trimming over the limit keeps the most recently created orders`() {
        val stored = mapOf(
            "old" to TestOrder("old", 1L),
            "mid" to TestOrder("mid", 2L),
            "new" to TestOrder("new", 3L)
        )

        val trimmed = trimToLimit(stored, limit = 2)

        assertEquals(setOf("mid", "new"), trimmed.keys)
        assertTrue("the oldest-created order should be evicted first", "old" !in trimmed.keys)
    }
}
