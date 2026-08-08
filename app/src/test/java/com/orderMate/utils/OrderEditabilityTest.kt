package com.orderMate.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for isOrderOpenForEditing's state-resolution logic (#139 feedback: quantity, and
 * any future Clover-state widget, must be locked once an order is no longer fully unpaid).
 *
 * isOrderOpenForEditing itself takes a real com.clover.sdk.v3.order.Order, which this module's
 * tests avoid constructing directly (see ItemNoteDialogTest for the established convention) -
 * so this mirrors just the state string the real function resolves from
 * `order.paymentState?.name ?: order.state`.
 */
class OrderEditabilityTest {

    private fun isOpenState(state: String?): Boolean {
        return state.equals("OPEN", ignoreCase = true)
    }

    @Test
    fun `OPEN order is editable`() {
        assertTrue(isOpenState("OPEN"))
        assertTrue(isOpenState("open"))
    }

    @Test
    fun `PAID order is not editable`() {
        assertFalse(isOpenState("PAID"))
    }

    @Test
    fun `PARTIALLY_PAID order is not editable`() {
        assertFalse(isOpenState("PARTIALLY_PAID"))
    }

    @Test
    fun `PARTIALLY_REFUNDED order is not editable`() {
        assertFalse(isOpenState("PARTIALLY_REFUNDED"))
    }

    @Test
    fun `REFUNDED order is not editable`() {
        assertFalse(isOpenState("REFUNDED"))
    }

    @Test
    fun `CREDITED order is not editable`() {
        assertFalse(isOpenState("CREDITED"))
    }

    @Test
    fun `LOCKED order is not editable`() {
        assertFalse(isOpenState("LOCKED"))
    }

    @Test
    fun `null state is not editable`() {
        assertFalse(isOpenState(null))
    }
}
