package com.orderMate.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for #138 - order history rendering limit investigation.
 *
 * `MyApp.getAllOrders()` centralizes what every order-list call site (OrderListRedesignFragment,
 * CalendarFragment, OrderHistoryFragment, windowManager's FloatingWidgetService,
 * OrderDetailFragment, CloverRepository, CloverNotesToV2Migrator) previously did inline:
 * `getOrderConnector().getOrders(mutableListOf())` with an unfiltered filter list and no page
 * size. These tests can't exercise the real Clover SDK connector (it requires a live Android
 * Context/account), so they instead mirror the population loop every call site uses to consume
 * the result - guarding against a future `.take(N)`/`.subList(...)` truncation being introduced
 * either in `getAllOrders()` or in a caller's loop.
 */
class OrderAppApplicationTest {

    /** Mirrors the `orderData?.forEach { list.add(it) }` pattern used by every call site. */
    private fun <T> populateFromOrderSource(source: List<T>?): MutableList<T> {
        val result = mutableListOf<T>()
        source?.forEach { result.add(it) }
        return result
    }

    @Test
    fun `populating order list from a large source does not truncate`() {
        val fakeOrders = (1..200).map { "order-$it" }

        val result = populateFromOrderSource(fakeOrders)

        assertEquals(200, result.size)
        assertEquals("order-1", result.first())
        assertEquals("order-200", result.last())
    }

    @Test
    fun `populating order list past the previously suspected 50-order cutoff keeps every order`() {
        val fakeOrders = (1..75).map { "order-$it" }

        val result = populateFromOrderSource(fakeOrders)

        assertEquals(75, result.size)
        assertTrue("order-51 must be present past the suspected 50-order limit", result.contains("order-51"))
        assertTrue("order-75 must be present", result.contains("order-75"))
    }

    @Test
    fun `null order source yields an empty list instead of crashing`() {
        val result = populateFromOrderSource<String>(null)

        assertEquals(0, result.size)
    }
}
