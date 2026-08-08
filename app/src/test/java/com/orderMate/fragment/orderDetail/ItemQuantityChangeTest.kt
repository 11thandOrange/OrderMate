package com.orderMate.fragment.orderDetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the quantity-change decision logic in
 * OrderDetailFragment/OverlayActivity's onNoteSaved handlers (#139).
 *
 * The real code calls the live Clover OrderConnector (createLineItemsFrom to add,
 * deleteLineItems to remove), which needs a real device or Robolectric/instrumentation this
 * repo doesn't have wired up for this flow - so, following this codebase's existing convention
 * (see OrderListRedesignFragmentTest, OrderHistoryStoreTest), these tests mirror the pure
 * decision logic locally: how many copies to add or which ids to delete, not the SDK calls
 * themselves.
 */
class ItemQuantityChangeTest {

    // Mirrors `val delta = quantity - itemQuantity`
    private fun computeDelta(originalQuantity: Int, newQuantity: Int): Int {
        return newQuantity - originalQuantity
    }

    @Test
    fun `increasing quantity produces a positive delta equal to the difference`() {
        assertEquals(1, computeDelta(originalQuantity = 1, newQuantity = 2))
        assertEquals(5, computeDelta(originalQuantity = 1, newQuantity = 6))
    }

    @Test
    fun `decreasing quantity produces a negative delta equal to the difference`() {
        assertEquals(-1, computeDelta(originalQuantity = 2, newQuantity = 1))
        assertEquals(-4, computeDelta(originalQuantity = 6, newQuantity = 2))
    }

    @Test
    fun `unchanged quantity produces a zero delta`() {
        assertEquals(0, computeDelta(originalQuantity = 3, newQuantity = 3))
    }

    // Mirrors the `delta > 0` branch: how many createLineItemsFrom calls should fire.
    private fun addCallCount(delta: Int): Int {
        return if (delta > 0) delta else 0
    }

    @Test
    fun `increasing by N schedules exactly N duplicate calls`() {
        assertEquals(1, addCallCount(computeDelta(1, 2)))
        assertEquals(5, addCallCount(computeDelta(1, 6)))
    }

    @Test
    fun `decreasing or unchanged quantity schedules zero add calls`() {
        assertEquals(0, addCallCount(computeDelta(2, 1)))
        assertEquals(0, addCallCount(computeDelta(3, 3)))
    }

    // Mirrors the `delta < 0` branch: OrderDetailFragment.onOrderItemClick takes
    // -delta ids from the front of the group's known line item ids.
    private fun idsToRemove(groupLineItemIds: List<String>, delta: Int): List<String> {
        if (delta >= 0) return emptyList()
        return groupLineItemIds.take(-delta)
    }

    @Test
    fun `decreasing by N removes exactly N ids from the group`() {
        val ids = listOf("a", "b", "c", "d")

        val toRemove = idsToRemove(ids, computeDelta(4, 2))

        assertEquals(2, toRemove.size)
        assertEquals(listOf("a", "b"), toRemove)
    }

    @Test
    fun `decreasing to 1 removes all but one id`() {
        val ids = listOf("a", "b", "c")

        val toRemove = idsToRemove(ids, computeDelta(3, 1))

        assertEquals(listOf("a", "b"), toRemove)
        assertTrue("at least one id must survive so the group isn't fully deleted", toRemove.size < ids.size)
    }

    @Test
    fun `increasing or unchanged quantity removes nothing`() {
        val ids = listOf("a", "b", "c")

        assertEquals(emptyList<String>(), idsToRemove(ids, computeDelta(1, 2)))
        assertEquals(emptyList<String>(), idsToRemove(ids, computeDelta(3, 3)))
    }

    @Test
    fun `removing more than available still only takes what exists`() {
        // Defensive case - shouldn't happen if itemQuantity/lineItemDifferentId stay in sync,
        // but take() on a short list simply returns everything rather than throwing.
        val ids = listOf("a", "b")

        val toRemove = idsToRemove(ids, delta = -5)

        assertEquals(listOf("a", "b"), toRemove)
    }
}
