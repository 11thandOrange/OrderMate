package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FilterCategoryBuilder
 * Tests filter category constants and helper functions
 * Issue #11 - Main Header Refinements
 */
class FilterCategoryBuilderTest {

    // ==================== Filter ID Constants Tests ====================

    @Test
    fun `CLOVER_ORDER_DATE constant has correct value`() {
        assertEquals("clover_order_date", FilterCategoryBuilder.CLOVER_ORDER_DATE)
    }

    @Test
    fun `CLOVER_DUE_DATE constant has correct value`() {
        // #12 - Separate order date and due date
        assertEquals("clover_due_date", FilterCategoryBuilder.CLOVER_DUE_DATE)
    }

    @Test
    fun `CLOVER_PAYMENT_STATUS constant has correct value`() {
        assertEquals("clover_payment_status", FilterCategoryBuilder.CLOVER_PAYMENT_STATUS)
    }

    @Test
    fun `CLOVER_ORDER_STATUS constant has correct value`() {
        assertEquals("clover_order_status", FilterCategoryBuilder.CLOVER_ORDER_STATUS)
    }

    @Test
    fun `CLOVER_PAYMENT_TYPE constant has correct value`() {
        assertEquals("clover_payment_type", FilterCategoryBuilder.CLOVER_PAYMENT_TYPE)
    }

    @Test
    fun `CLOVER_EMPLOYEE constant has correct value`() {
        assertEquals("clover_employee", FilterCategoryBuilder.CLOVER_EMPLOYEE)
    }

    @Test
    fun `WIDGET_PREFIX constant has correct value`() {
        assertEquals("widget_", FilterCategoryBuilder.WIDGET_PREFIX)
    }

    // ==================== isCloverFilter Tests ====================

    @Test
    fun `isCloverFilter returns true for clover order date`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_ORDER_DATE))
    }

    @Test
    fun `isCloverFilter returns true for clover due date`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_DUE_DATE))
    }

    @Test
    fun `isCloverFilter returns true for clover payment status`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_PAYMENT_STATUS))
    }

    @Test
    fun `isCloverFilter returns true for clover order status`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_ORDER_STATUS))
    }

    @Test
    fun `isCloverFilter returns true for clover payment type`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_PAYMENT_TYPE))
    }

    @Test
    fun `isCloverFilter returns true for clover employee`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_EMPLOYEE))
    }

    @Test
    fun `isCloverFilter returns false for widget filter`() {
        assertFalse(FilterCategoryBuilder.isCloverFilter("widget_123"))
    }

    @Test
    fun `isCloverFilter returns false for random string`() {
        assertFalse(FilterCategoryBuilder.isCloverFilter("random_filter"))
    }

    // ==================== isWidgetFilter Tests ====================

    @Test
    fun `isWidgetFilter returns true for widget filter`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_123"))
    }

    @Test
    fun `isWidgetFilter returns true for widget with uuid`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_abc-def-123"))
    }

    @Test
    fun `isWidgetFilter returns false for clover filter`() {
        assertFalse(FilterCategoryBuilder.isWidgetFilter(FilterCategoryBuilder.CLOVER_ORDER_DATE))
    }

    @Test
    fun `isWidgetFilter returns false for random string`() {
        assertFalse(FilterCategoryBuilder.isWidgetFilter("random_filter"))
    }

    // ==================== getWidgetId Tests ====================

    @Test
    fun `getWidgetId extracts id from widget filter`() {
        assertEquals("123", FilterCategoryBuilder.getWidgetId("widget_123"))
    }

    @Test
    fun `getWidgetId extracts uuid from widget filter`() {
        assertEquals("abc-def-456", FilterCategoryBuilder.getWidgetId("widget_abc-def-456"))
    }

    @Test
    fun `getWidgetId returns null for clover filter`() {
        assertNull(FilterCategoryBuilder.getWidgetId(FilterCategoryBuilder.CLOVER_ORDER_DATE))
    }

    @Test
    fun `getWidgetId returns null for non-widget string`() {
        assertNull(FilterCategoryBuilder.getWidgetId("random_string"))
    }

    // ==================== getWidgetId with Level Suffix Tests ====================

    @Test
    fun `getWidgetId extracts id from item-level widget filter`() {
        assertEquals("abc123", FilterCategoryBuilder.getWidgetId("widget_abc123_item"))
    }

    @Test
    fun `getWidgetId extracts id from order-level widget filter`() {
        assertEquals("abc123", FilterCategoryBuilder.getWidgetId("widget_abc123_order"))
    }

    @Test
    fun `getWidgetId extracts UUID from item-level widget filter`() {
        assertEquals(
            "bc396383-96c1-4940-b91d-2966b7e478b3",
            FilterCategoryBuilder.getWidgetId("widget_bc396383-96c1-4940-b91d-2966b7e478b3_item")
        )
    }

    @Test
    fun `getWidgetId extracts UUID from order-level widget filter`() {
        assertEquals(
            "bc396383-96c1-4940-b91d-2966b7e478b3",
            FilterCategoryBuilder.getWidgetId("widget_bc396383-96c1-4940-b91d-2966b7e478b3_order")
        )
    }

    @Test
    fun `isWidgetFilter returns true for item-level widget`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_abc123_item"))
    }

    @Test
    fun `isWidgetFilter returns true for order-level widget`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_abc123_order"))
    }

    @Test
    fun `isWidgetFilter returns true for UUID item-level widget`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_bc396383-96c1-4940-b91d-2966b7e478b3_item"))
    }

    @Test
    fun `isWidgetFilter returns true for UUID order-level widget`() {
        assertTrue(FilterCategoryBuilder.isWidgetFilter("widget_bc396383-96c1-4940-b91d-2966b7e478b3_order"))
    }

    // ==================== Filter Type Enum Tests ====================

    @Test
    fun `FilterType has MULTI_SELECT value`() {
        assertNotNull(FilterCategoryBuilder.FilterType.MULTI_SELECT)
    }

    @Test
    fun `FilterType has DATE_PICKER value`() {
        assertNotNull(FilterCategoryBuilder.FilterType.DATE_PICKER)
    }

    // ==================== Filter Source Enum Tests ====================

    @Test
    fun `FilterSource has CLOVER value`() {
        assertNotNull(FilterCategoryBuilder.FilterSource.CLOVER)
    }

    @Test
    fun `FilterSource has ORDERMATE value`() {
        assertNotNull(FilterCategoryBuilder.FilterSource.ORDERMATE)
    }

    // ==================== Date Filter Differentiation Tests (#12) ====================

    @Test
    fun `order date and due date have different IDs`() {
        assertNotEquals(
            FilterCategoryBuilder.CLOVER_ORDER_DATE,
            FilterCategoryBuilder.CLOVER_DUE_DATE
        )
    }

    @Test
    fun `both date filters are clover filters`() {
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertTrue(FilterCategoryBuilder.isCloverFilter(FilterCategoryBuilder.CLOVER_DUE_DATE))
    }

    @Test
    fun `date filters are not widget filters`() {
        assertFalse(FilterCategoryBuilder.isWidgetFilter(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertFalse(FilterCategoryBuilder.isWidgetFilter(FilterCategoryBuilder.CLOVER_DUE_DATE))
    }
}
