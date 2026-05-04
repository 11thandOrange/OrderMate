package com.orderMate.utils

import com.orderMate.fragment.FilterDialogFragment
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for OrderFilterUtils
 * Tests the shared filtering logic used by both List and Calendar pages
 */
class OrderFilterUtilsTest {

    // ==================== FilterState Helper Tests ====================

    @Test
    fun `FilterState hasActiveFilters returns false when empty`() {
        val state = FilterDialogFragment.FilterState()
        assertFalse(state.hasActiveFilters())
    }

    @Test
    fun `FilterState hasActiveFilters returns true with date selections`() {
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(Date())
            )
        )
        assertTrue(state.hasActiveFilters())
    }

    @Test
    fun `FilterState hasActiveFilters returns true with selections`() {
        val state = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID")
            )
        )
        assertTrue(state.hasActiveFilters())
    }

    @Test
    fun `FilterState hasActiveFilters returns true with both selections and date selections`() {
        val state = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID")
            ),
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(Date())
            )
        )
        assertTrue(state.hasActiveFilters())
    }

    @Test
    fun `FilterState hasActiveFilters returns false with empty collections`() {
        val state = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to emptySet()
            ),
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to emptyList()
            )
        )
        assertFalse(state.hasActiveFilters())
    }

    @Test
    fun `FilterState getActiveFilterCount returns correct count`() {
        val state = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID", "OPEN")
            ),
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(Date(), Date())
            )
        )
        assertEquals(4, state.getActiveFilterCount())
    }

    @Test
    fun `FilterState getSelectedValues returns values for category`() {
        val state = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID", "OPEN")
            )
        )
        assertEquals(setOf("PAID", "OPEN"), state.getSelectedValues(FilterCategoryBuilder.CLOVER_PAYMENT_STATUS))
    }

    @Test
    fun `FilterState getSelectedValues returns empty set for missing category`() {
        val state = FilterDialogFragment.FilterState()
        assertEquals(emptySet<String>(), state.getSelectedValues(FilterCategoryBuilder.CLOVER_PAYMENT_STATUS))
    }

    @Test
    fun `FilterState getSelectedDates returns dates for category`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000) // +1 day
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1, date2)
            )
        )
        assertEquals(listOf(date1, date2), state.getSelectedDates(FilterCategoryBuilder.CLOVER_ORDER_DATE))
    }

    @Test
    fun `FilterState getSelectedDates returns empty list for missing category`() {
        val state = FilterDialogFragment.FilterState()
        assertEquals(emptyList<Date>(), state.getSelectedDates(FilterCategoryBuilder.CLOVER_ORDER_DATE))
    }

    // ==================== Multiple Date Filter Categories Tests ====================

    @Test
    fun `FilterState supports multiple date filter categories`() {
        val orderDate = Date()
        val widgetDate = Date(orderDate.time + 86400000)
        
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(orderDate),
                "widget_deadline_order" to listOf(widgetDate)
            )
        )
        
        assertEquals(2, state.dateSelections.size)
        assertEquals(listOf(orderDate), state.getSelectedDates(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertEquals(listOf(widgetDate), state.getSelectedDates("widget_deadline_order"))
    }

    @Test
    fun `FilterState dateSelections values flatten returns all dates`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        val date3 = Date(date1.time + 172800000)
        
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2),
                "widget_pickup_order" to listOf(date3)
            )
        )
        
        val allDates = state.dateSelections.values.flatten()
        assertEquals(3, allDates.size)
        assertTrue(allDates.contains(date1))
        assertTrue(allDates.contains(date2))
        assertTrue(allDates.contains(date3))
    }

    @Test
    fun `FilterState copy preserves all date categories`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        
        val original = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2)
            )
        )
        
        val copy = original.copy()
        
        assertEquals(original.dateSelections, copy.dateSelections)
        assertEquals(2, copy.dateSelections.size)
    }

    @Test
    fun `FilterState copy with modified dateSelections removes one category`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        
        val original = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2)
            )
        )
        
        // Simulate removing one filter category
        val newDateSelections = original.dateSelections.toMutableMap()
        newDateSelections.remove(FilterCategoryBuilder.CLOVER_ORDER_DATE)
        
        val modified = original.copy(dateSelections = newDateSelections)
        
        assertEquals(1, modified.dateSelections.size)
        assertFalse(modified.dateSelections.containsKey(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertTrue(modified.dateSelections.containsKey("widget_deadline_order"))
    }

    @Test
    fun `FilterState removing one date category preserves other categories`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        val date3 = Date(date1.time + 172800000)
        
        val original = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2),
                "widget_pickup_order" to listOf(date3)
            )
        )
        
        // Remove middle category
        val newDateSelections = original.dateSelections.toMutableMap()
        newDateSelections.remove("widget_deadline_order")
        val modified = original.copy(dateSelections = newDateSelections)
        
        assertEquals(2, modified.dateSelections.size)
        assertTrue(modified.dateSelections.containsKey(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertFalse(modified.dateSelections.containsKey("widget_deadline_order"))
        assertTrue(modified.dateSelections.containsKey("widget_pickup_order"))
    }

    // ==================== Highlighted Dates Calculation Tests ====================
    
    @Test
    fun `flattening dateSelections includes all categories`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 2, 0, 0, 0)
        val may2 = cal.time
        
        cal.set(2026, Calendar.MAY, 4, 0, 0, 0)
        val may4 = cal.time
        
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(may2),
                "widget_bc396383-96c1-4940-b91d-2966b7e478b3_order" to listOf(may2)
            )
        )
        
        val allDates = state.dateSelections.values.flatten()
        
        // Both dates are May 2, so flatten gives 2 items (before dedup)
        assertEquals(2, allDates.size)
    }

    @Test
    fun `flattening and deduplicating dateSelections by day`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 2, 10, 0, 0)
        val may2Morning = cal.time
        
        cal.set(2026, Calendar.MAY, 2, 15, 0, 0)
        val may2Afternoon = cal.time
        
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(may2Morning),
                "widget_deadline_order" to listOf(may2Afternoon)
            )
        )
        
        val allDates = state.dateSelections.values.flatten()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val uniqueDates = allDates.distinctBy { dateFormat.format(it) }
        
        // Both are same day, so after dedup should be 1
        assertEquals(1, uniqueDates.size)
    }

    @Test
    fun `flattening dateSelections with different dates keeps all`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 2, 0, 0, 0)
        val may2 = cal.time
        
        cal.set(2026, Calendar.MAY, 4, 0, 0, 0)
        val may4 = cal.time
        
        val state = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(may2),
                "widget_deadline_order" to listOf(may4)
            )
        )
        
        val allDates = state.dateSelections.values.flatten()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val uniqueDates = allDates.distinctBy { dateFormat.format(it) }
        
        // Different days, so both kept
        assertEquals(2, uniqueDates.size)
    }

    // ==================== Widget Filter Category ID Tests ====================

    @Test
    fun `widget filter category ID with item suffix is recognized`() {
        val categoryId = "widget_abc123_item"
        assertTrue(FilterCategoryBuilder.isWidgetFilter(categoryId))
    }

    @Test
    fun `widget filter category ID with order suffix is recognized`() {
        val categoryId = "widget_abc123_order"
        assertTrue(FilterCategoryBuilder.isWidgetFilter(categoryId))
    }

    @Test
    fun `getWidgetId extracts ID from item-level widget filter`() {
        val categoryId = "widget_abc123_item"
        assertEquals("abc123", FilterCategoryBuilder.getWidgetId(categoryId))
    }

    @Test
    fun `getWidgetId extracts ID from order-level widget filter`() {
        val categoryId = "widget_abc123_order"
        assertEquals("abc123", FilterCategoryBuilder.getWidgetId(categoryId))
    }

    @Test
    fun `getWidgetId extracts UUID from item-level widget filter`() {
        val categoryId = "widget_bc396383-96c1-4940-b91d-2966b7e478b3_item"
        assertEquals("bc396383-96c1-4940-b91d-2966b7e478b3", FilterCategoryBuilder.getWidgetId(categoryId))
    }

    @Test
    fun `getWidgetId extracts UUID from order-level widget filter`() {
        val categoryId = "widget_bc396383-96c1-4940-b91d-2966b7e478b3_order"
        assertEquals("bc396383-96c1-4940-b91d-2966b7e478b3", FilterCategoryBuilder.getWidgetId(categoryId))
    }
}
