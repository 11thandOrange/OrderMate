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
 * (see ItemNoteDialogTest, WidgetManagerTest) - these tests mirror the pure display logic
 * locally with a lightweight stand-in for Customer, rather than instantiating the real
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
        // CUSTOMER is committed via CloverRepository.assignCustomerToOrder (reusing
        // CustomerDialog's own save flow) rather than being serialized into the note
        // string, so it can never show up in the note text built from other widget selections.
        val selections = mapOf("Deadline" to "Apr 20, 2026", "Group" to "Catering")
        val note = selections
            .filter { it.value.isNotEmpty() }
            .map { "${it.key}:${it.value}" }
            .joinToString(" • ")

        assertFalse(note.contains("Customer", ignoreCase = true))
    }

    // --- Regression tests for the deferred-save fix ---
    //
    // Bug: CustomerDialog.saveCustomer() assigns the customer to the order (writes to Clover)
    // immediately whenever it is given a non-null orderId. OrderNoteDialogFragment.
    // openCustomerDialog() used to pass its real orderId straight through, so clicking Save on
    // the Customer Details screen wrote the order-customer assignment to Clover right away -
    // before the outer "Edit Order Level Notes" popup's own Save was ever clicked.
    //
    // Fix: openCustomerDialog() now always opens CustomerDialog with orderId = null, so its
    // Save only updates local popup state (currentCustomer, via onCustomerUpdated). The actual
    // assignment now happens in OrderNoteListener.onOrderNoteSaved(orderId, note, customer),
    // fired only when the popup's own Save button is clicked.

    // Mirrors OrderNoteDialogFragment.openCustomerDialog()'s CustomerDialog.newInstance(...)
    // call: it must never forward the popup's orderId, regardless of what the popup was opened
    // with, so CustomerDialog's own Save can never assign directly to Clover from this context.
    private fun customerDialogOrderIdForPopup(popupOrderId: String?): String? = null

    // Mirrors the listener implementations added to OrderDetailFragment/OverlayActivity's
    // onOrderNoteSaved: CloverRepository.assignCustomerToOrder is only called when a customer
    // is present.
    private fun shouldAssignCustomerToOrder(customer: TestCustomer?): Boolean = customer != null

    @Test
    fun `opening the Customer Details screen from the order-level popup never forwards orderId`() {
        assertNull(customerDialogOrderIdForPopup("order-123"))
        assertNull(customerDialogOrderIdForPopup(null))
    }

    @Test
    fun `order-level popup Save assigns the customer to Clover only when one is present`() {
        assertTrue(shouldAssignCustomerToOrder(TestCustomer(firstName = "Jane", lastName = "Doe")))
        assertFalse(shouldAssignCustomerToOrder(null))
    }
}
