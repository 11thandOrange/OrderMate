package com.orderMate.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.orderMate.fragment.FilterDialogFragment
import com.orderMate.utils.FilterCategoryBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

/**
 * Unit tests for SharedFilterViewModel
 * Tests filter state management across List and Calendar tabs
 */
class SharedFilterViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: SharedFilterViewModel

    @Before
    fun setUp() {
        viewModel = SharedFilterViewModel()
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial filterState is empty`() {
        val state = viewModel.filterState.value
        assertNotNull(state)
        assertFalse(state!!.hasActiveFilters())
    }

    @Test
    fun `initial searchQuery is empty`() {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `initial highlightedDates is empty`() {
        assertTrue(viewModel.highlightedDates.value?.isEmpty() ?: true)
    }

    @Test
    fun `initial calendarViewMode is month`() {
        assertEquals("month", viewModel.calendarViewMode.value)
    }

    @Test
    fun `initial selectedDate is null`() {
        assertNull(viewModel.selectedDate.value)
    }

    // ==================== setFilterState Tests ====================

    @Test
    fun `setFilterState updates filterState LiveData`() {
        val newState = FilterDialogFragment.FilterState(
            selections = mapOf(
                FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID")
            )
        )

        viewModel.setFilterState(newState)

        assertEquals(newState, viewModel.filterState.value)
    }

    @Test
    fun `setFilterState with date selections updates highlightedDates`() {
        val date = Date()
        val newState = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date)
            )
        )

        viewModel.setFilterState(newState)

        val highlighted = viewModel.highlightedDates.value
        assertNotNull(highlighted)
        assertEquals(1, highlighted!!.size)
    }

    @Test
    fun `setFilterState with multiple date categories updates highlightedDates with all dates`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000) // +1 day
        
        val newState = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2)
            )
        )

        viewModel.setFilterState(newState)

        val highlighted = viewModel.highlightedDates.value
        assertNotNull(highlighted)
        assertEquals(2, highlighted!!.size)
    }

    @Test
    fun `setFilterState with same date in multiple categories deduplicates by day`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 2, 10, 0, 0)
        val may2Morning = cal.time
        
        cal.set(2026, Calendar.MAY, 2, 15, 0, 0)
        val may2Afternoon = cal.time
        
        val newState = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(may2Morning),
                "widget_deadline_order" to listOf(may2Afternoon)
            )
        )

        viewModel.setFilterState(newState)

        val highlighted = viewModel.highlightedDates.value
        assertNotNull(highlighted)
        // Both are same day, should be deduplicated to 1
        assertEquals(1, highlighted!!.size)
    }

    @Test
    fun `setFilterState preserves both date filter categories`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        
        val newState = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_bc396383-96c1-4940-b91d-2966b7e478b3_order" to listOf(date2)
            )
        )

        viewModel.setFilterState(newState)

        val state = viewModel.filterState.value
        assertNotNull(state)
        assertEquals(2, state!!.dateSelections.size)
        assertTrue(state.dateSelections.containsKey(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertTrue(state.dateSelections.containsKey("widget_bc396383-96c1-4940-b91d-2966b7e478b3_order"))
    }

    // ==================== setSearchQuery Tests ====================

    @Test
    fun `setSearchQuery updates searchQuery LiveData`() {
        viewModel.setSearchQuery("test query")
        assertEquals("test query", viewModel.searchQuery.value)
    }

    @Test
    fun `setSearchQuery with empty string clears query`() {
        viewModel.setSearchQuery("test")
        viewModel.setSearchQuery("")
        assertEquals("", viewModel.searchQuery.value)
    }

    // ==================== setHighlightedDates Tests ====================

    @Test
    fun `setHighlightedDates updates highlightedDates LiveData`() {
        val dates = listOf(Date(), Date())
        viewModel.setHighlightedDates(dates)
        assertEquals(dates, viewModel.highlightedDates.value)
    }

    @Test
    fun `setHighlightedDates with empty list clears dates`() {
        viewModel.setHighlightedDates(listOf(Date()))
        viewModel.setHighlightedDates(emptyList())
        assertTrue(viewModel.highlightedDates.value?.isEmpty() ?: true)
    }

    // ==================== setCalendarViewMode Tests ====================

    @Test
    fun `setCalendarViewMode updates to day`() {
        viewModel.setCalendarViewMode("day")
        assertEquals("day", viewModel.calendarViewMode.value)
    }

    @Test
    fun `setCalendarViewMode updates to week`() {
        viewModel.setCalendarViewMode("week")
        assertEquals("week", viewModel.calendarViewMode.value)
    }

    @Test
    fun `setCalendarViewMode updates to month`() {
        viewModel.setCalendarViewMode("month")
        assertEquals("month", viewModel.calendarViewMode.value)
    }

    // ==================== setSelectedDate Tests ====================

    @Test
    fun `setSelectedDate updates selectedDate LiveData`() {
        val date = Date()
        viewModel.setSelectedDate(date)
        assertEquals(date, viewModel.selectedDate.value)
    }

    @Test
    fun `setSelectedDate with null clears date`() {
        viewModel.setSelectedDate(Date())
        viewModel.setSelectedDate(null)
        assertNull(viewModel.selectedDate.value)
    }

    // ==================== resetAll Tests ====================

    @Test
    fun `resetAll clears filterState`() {
        viewModel.setFilterState(
            FilterDialogFragment.FilterState(
                selections = mapOf(FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID"))
            )
        )
        
        viewModel.resetAll()
        
        assertFalse(viewModel.filterState.value?.hasActiveFilters() ?: true)
    }

    @Test
    fun `resetAll clears searchQuery`() {
        viewModel.setSearchQuery("test")
        viewModel.resetAll()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `resetAll clears highlightedDates`() {
        viewModel.setHighlightedDates(listOf(Date()))
        viewModel.resetAll()
        assertTrue(viewModel.highlightedDates.value?.isEmpty() ?: true)
    }

    @Test
    fun `resetAll preserves calendarViewMode`() {
        viewModel.setCalendarViewMode("day")
        viewModel.resetAll()
        assertEquals("day", viewModel.calendarViewMode.value)
    }

    // ==================== hasActiveFilters Tests ====================

    @Test
    fun `hasActiveFilters returns false when empty`() {
        assertFalse(viewModel.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true with selections`() {
        viewModel.setFilterState(
            FilterDialogFragment.FilterState(
                selections = mapOf(FilterCategoryBuilder.CLOVER_PAYMENT_STATUS to setOf("PAID"))
            )
        )
        assertTrue(viewModel.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true with dateSelections`() {
        viewModel.setFilterState(
            FilterDialogFragment.FilterState(
                dateSelections = mapOf(FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(Date()))
            )
        )
        assertTrue(viewModel.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true with searchQuery`() {
        viewModel.setSearchQuery("test")
        assertTrue(viewModel.hasActiveFilters())
    }

    // ==================== hasSelectedDates Tests ====================

    @Test
    fun `hasSelectedDates returns false when empty`() {
        assertFalse(viewModel.hasSelectedDates())
    }

    @Test
    fun `hasSelectedDates returns true with highlightedDates`() {
        viewModel.setHighlightedDates(listOf(Date()))
        assertTrue(viewModel.hasSelectedDates())
    }

    // ==================== triggerRefresh Tests ====================

    @Test
    fun `triggerRefresh increments refreshTrigger`() {
        val initialValue = viewModel.refreshTrigger.value ?: 0
        viewModel.triggerRefresh()
        assertEquals(initialValue + 1, viewModel.refreshTrigger.value)
    }

    @Test
    fun `triggerRefresh increments multiple times`() {
        val initialValue = viewModel.refreshTrigger.value ?: 0
        viewModel.triggerRefresh()
        viewModel.triggerRefresh()
        viewModel.triggerRefresh()
        assertEquals(initialValue + 3, viewModel.refreshTrigger.value)
    }

    // ==================== Filter State Persistence Tests ====================

    @Test
    fun `filter state with two date categories persists after multiple setFilterState calls`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        
        // Set initial state with two date categories
        val state1 = FilterDialogFragment.FilterState(
            dateSelections = mapOf(
                FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                "widget_deadline_order" to listOf(date2)
            )
        )
        viewModel.setFilterState(state1)
        
        // Simulate saving same state again (like saveCurrentStateToViewModel does)
        viewModel.setFilterState(state1)
        
        // Verify both categories still exist
        val currentState = viewModel.filterState.value
        assertNotNull(currentState)
        assertEquals(2, currentState!!.dateSelections.size)
    }

    @Test
    fun `removing one date category preserves the other`() {
        val date1 = Date()
        val date2 = Date(date1.time + 86400000)
        
        // Set state with two categories
        viewModel.setFilterState(
            FilterDialogFragment.FilterState(
                dateSelections = mapOf(
                    FilterCategoryBuilder.CLOVER_ORDER_DATE to listOf(date1),
                    "widget_deadline_order" to listOf(date2)
                )
            )
        )
        
        // Remove one category (simulate removeDateFilter)
        val newDateSelections = viewModel.filterState.value!!.dateSelections.toMutableMap()
        newDateSelections.remove(FilterCategoryBuilder.CLOVER_ORDER_DATE)
        val modifiedState = viewModel.filterState.value!!.copy(dateSelections = newDateSelections)
        viewModel.setFilterState(modifiedState)
        
        // Verify only widget filter remains
        val currentState = viewModel.filterState.value
        assertNotNull(currentState)
        assertEquals(1, currentState!!.dateSelections.size)
        assertFalse(currentState.dateSelections.containsKey(FilterCategoryBuilder.CLOVER_ORDER_DATE))
        assertTrue(currentState.dateSelections.containsKey("widget_deadline_order"))
        
        // Verify highlightedDates updated to only contain the remaining date
        val highlighted = viewModel.highlightedDates.value
        assertNotNull(highlighted)
        assertEquals(1, highlighted!!.size)
    }
}
