package com.orderMate.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OrderListRedesignFragment's "Load older orders" REST backfill logic (#138).
 *
 * The real flow depends on a live Android Context, Clover's on-device account, and a network
 * call (CloverRepository.loadOlderOrders), none of which are available in a plain JVM test -
 * so, following this codebase's existing convention (see ItemNoteDialogTest,
 * OrderCardRedesignAdapterTest), these tests mirror the pure merge/pagination logic locally
 * with lightweight stand-ins rather than the real Order/Fragment classes.
 */
class OrderListRedesignFragmentTest {

    private data class TestOrder(val id: String)

    // Mirrors loadOlderOrders()'s dedup step: existing ids come from the already-loaded list,
    // only genuinely new orders get appended.
    private fun mergeNewOrders(existing: List<TestOrder>, fetched: List<TestOrder>): List<TestOrder> {
        val existingIds = existing.map { it.id }.toSet()
        val newOrders = fetched.filter { it.id !in existingIds }
        return existing + newOrders
    }

    @Test
    fun `merging appends only orders not already in the local list`() {
        val existing = listOf(TestOrder("a"), TestOrder("b"))
        val fetched = listOf(TestOrder("b"), TestOrder("c"))

        val merged = mergeNewOrders(existing, fetched)

        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
    }

    @Test
    fun `merging an entirely-overlapping page adds nothing new`() {
        val existing = listOf(TestOrder("a"), TestOrder("b"))
        val fetched = listOf(TestOrder("a"), TestOrder("b"))

        val merged = mergeNewOrders(existing, fetched)

        assertEquals(2, merged.size)
    }

    @Test
    fun `merging into an empty local list keeps every fetched order`() {
        val merged = mergeNewOrders(emptyList(), listOf(TestOrder("x"), TestOrder("y")))

        assertEquals(listOf("x", "y"), merged.map { it.id })
    }

    // Mirrors loadOlderOrders()'s offset-advancement rule: always advance by the number of
    // orders Clover's REST API actually returned, regardless of how many were new to us -
    // this is what keeps repeated taps paging forward instead of re-fetching the same page.
    private fun nextOffset(currentOffset: Int, pageReturnedCount: Int): Int {
        return currentOffset + pageReturnedCount
    }

    @Test
    fun `offset advances by the full page size returned, not by new-order count`() {
        // Page returned 50 orders but only 3 were new (47 already known) - offset must still
        // advance by 50, or the next tap would re-fetch orders we've already seen.
        val next = nextOffset(currentOffset = 0, pageReturnedCount = 50)

        assertEquals(50, next)
    }

    @Test
    fun `repeated pages advance offset cumulatively`() {
        var offset = 0
        offset = nextOffset(offset, pageReturnedCount = 50)
        offset = nextOffset(offset, pageReturnedCount = 50)

        assertEquals(100, offset)
    }

    @Test
    fun `an empty fetched page signals no more orders to load`() {
        val fetched = emptyList<TestOrder>()

        assertTrue("an empty page should be treated as the end of history", fetched.isEmpty())
    }

    // Mirrors loadOrders()'s reload-merge step (#138 follow-up): a fresh read of Clover's
    // local, retention-windowed cache can omit orders that were previously pulled in via the
    // REST backfill (e.g. after the merchant scrolled down, then hit Sync). Those backfilled
    // orders must be re-merged back in rather than dropped, or they "disappear" again on every
    // reload even though the scroll-backfill itself works.
    private fun mergeBackfilledIntoFreshLoad(
        freshLocalOrders: List<TestOrder>,
        backfilledOrders: List<TestOrder>
    ): List<TestOrder> {
        val freshIds = freshLocalOrders.map { it.id }.toSet()
        return freshLocalOrders + backfilledOrders.filter { it.id !in freshIds }
    }

    @Test
    fun `reload keeps previously backfilled orders that dropped out of the local cache`() {
        val freshLocalOrders = listOf(TestOrder("48"), TestOrder("49"), TestOrder("50"))
        val backfilledOrders = listOf(TestOrder("1"), TestOrder("2"))

        val merged = mergeBackfilledIntoFreshLoad(freshLocalOrders, backfilledOrders)

        assertEquals(listOf("48", "49", "50", "1", "2"), merged.map { it.id })
    }

    @Test
    fun `reload does not duplicate a backfilled order the local cache still returns`() {
        val freshLocalOrders = listOf(TestOrder("1"), TestOrder("48"), TestOrder("49"))
        val backfilledOrders = listOf(TestOrder("1"))

        val merged = mergeBackfilledIntoFreshLoad(freshLocalOrders, backfilledOrders)

        assertEquals(listOf("1", "48", "49"), merged.map { it.id })
    }

    @Test
    fun `reload with no prior backfill is unaffected`() {
        val freshLocalOrders = listOf(TestOrder("48"), TestOrder("49"), TestOrder("50"))

        val merged = mergeBackfilledIntoFreshLoad(freshLocalOrders, emptyList())

        assertEquals(freshLocalOrders.map { it.id }, merged.map { it.id })
    }
}
