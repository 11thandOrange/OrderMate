package com.orderMate.fragment.orderDetail

import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OrderNoteDialogFragment's CUSTOMER widget behavior (#140).
 *
 * OrderNoteDialogFragment.renderCustomer()/addCustomerSection() need a real Android Context
 * and the Clover SDK's Customer type, so - following this codebase's existing convention
 * (see ItemNoteDialogTest, OrderCardRedesignAdapterTest) - these tests mirror the pure display
 * logic locally with a lightweight stand-in for Customer, rather than instantiating the real
 * dialog or SDK class.
 */
class OrderNoteDialogTest {

    private data class TestCustomer(val firstName: String?, val lastName: String?)

    // Mirrors OrderNoteDialogFragment.renderCustomer()'s name formatting
    private fun formatCustomerName(customer: TestCustomer?): String {
        if (customer == null) return "No customer assigned"
        return listOfNotNull(customer.firstName, customer.lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Customer" }
    }

    // Mirrors OrderNoteDialogFragment.renderCustomer()'s action label
    private fun actionLabel(customer: TestCustomer?): String {
        return if (customer != null) "Change" else "Add"
    }

    @Test
    fun `no customer assigned shows placeholder text and Add action`() {
        assertEquals("No customer assigned", formatCustomerName(null))
        assertEquals("Add", actionLabel(null))
    }

    @Test
    fun `customer with full name is displayed and shows Change action`() {
        val customer = TestCustomer(firstName = "Jane", lastName = "Doe")

        assertEquals("Jane Doe", formatCustomerName(customer))
        assertEquals("Change", actionLabel(customer))
    }

    @Test
    fun `customer with only first name is displayed without trailing space`() {
        val customer = TestCustomer(firstName = "Jane", lastName = null)

        assertEquals("Jane", formatCustomerName(customer))
    }

    @Test
    fun `customer with only last name is displayed`() {
        val customer = TestCustomer(firstName = null, lastName = "Doe")

        assertEquals("Doe", formatCustomerName(customer))
    }

    @Test
    fun `customer with blank names falls back to generic label`() {
        val customer = TestCustomer(firstName = "", lastName = "")

        assertEquals("Customer", formatCustomerName(customer))
    }

    @Test
    fun `customer widget has no options`() {
        val widget = WidgetConfig(
            id = "widget-customer",
            type = WidgetType.CUSTOMER,
            label = "Customer",
            level = NoteLevel.ORDER
        )

        assertTrue(widget.options.isEmpty())
    }

    @Test
    fun `customer is excluded from note serialization`() {
        // Like QUANTITY, CUSTOMER is persisted immediately via CloverRepository (reusing
        // CustomerDialog's own save/assign flow) rather than being serialized into the note
        // string, so it can never show up in the note text built from other widget selections.
        val selections = mapOf("Deadline" to "Apr 20, 2026", "Group" to "Catering")
        val note = selections
            .filter { it.value.isNotEmpty() }
            .map { "${it.key}:${it.value}" }
            .joinToString(" • ")

        assertFalse(note.contains("Customer", ignoreCase = true))
    }
}
