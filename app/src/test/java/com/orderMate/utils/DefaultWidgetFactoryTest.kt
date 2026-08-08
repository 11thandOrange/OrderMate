package com.orderMate.utils

import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DefaultWidgetFactory's QUANTITY (#139) and CUSTOMER (#140) handling.
 *
 * Neither widget type has discrete options, so neither can usefully appear in the order-list
 * filter dialog the way SINGLE_SELECT/MULTI_SELECT/CALENDAR widgets do - both should default
 * to excluded from the filter, the same way TEXT_BOX already is.
 */
class DefaultWidgetFactoryTest {

    @Test
    fun `quantity and customer widgets default to excluded from filter, like text box`() {
        val quantity = DefaultWidgetFactory.createEmpty(WidgetType.QUANTITY, order = 0)
        val customer = DefaultWidgetFactory.createEmpty(WidgetType.CUSTOMER, order = 1, level = NoteLevel.ORDER)
        val textBox = DefaultWidgetFactory.createEmpty(WidgetType.TEXT_BOX, order = 2)

        assertFalse(quantity.showInFilter)
        assertFalse(customer.showInFilter)
        assertFalse(textBox.showInFilter)
    }

    @Test
    fun `other widget types default to included in filter`() {
        val calendar = DefaultWidgetFactory.createEmpty(WidgetType.CALENDAR, order = 0)
        val singleSelect = DefaultWidgetFactory.createEmpty(WidgetType.SINGLE_SELECT, order = 1)
        val multiSelect = DefaultWidgetFactory.createEmpty(WidgetType.MULTI_SELECT, order = 2)

        assertTrue(calendar.showInFilter)
        assertTrue(singleSelect.showInFilter)
        assertTrue(multiSelect.showInFilter)
    }

    @Test
    fun `createWidget for quantity has no options and is excluded from filter by default`() {
        val widget = DefaultWidgetFactory.createWidget(
            type = WidgetType.QUANTITY,
            label = "Quantity",
            level = NoteLevel.ITEM
        )

        assertTrue(widget.options.isEmpty())
        assertFalse(widget.showInFilter)
    }

    @Test
    fun `createWidget for customer has no options and is excluded from filter by default`() {
        val widget = DefaultWidgetFactory.createWidget(
            type = WidgetType.CUSTOMER,
            label = "Customer",
            level = NoteLevel.ORDER
        )

        assertTrue(widget.options.isEmpty())
        assertFalse(widget.showInFilter)
    }
}
