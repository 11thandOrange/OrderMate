package com.orderMate.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orderMate.fragment.FilterDialogFragment
import java.util.Date

/**
 * Shared ViewModel for filter and search state between List and Calendar tabs.
 * Scoped to Activity so state persists across fragment navigation.
 */
class SharedFilterViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SharedFilterVM_DEBUG"
    }

    // Filter state
    private val _filterState = MutableLiveData(FilterDialogFragment.FilterState())
    val filterState: LiveData<FilterDialogFragment.FilterState> = _filterState

    // Search query
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // Dates parsed from search or filter (for calendar highlighting)
    private val _highlightedDates = MutableLiveData<List<Date>>(emptyList())
    val highlightedDates: LiveData<List<Date>> = _highlightedDates
    
    // Calendar view mode (day/week/month) - persists across navigation
    private val _calendarViewMode = MutableLiveData("month")
    val calendarViewMode: LiveData<String> = _calendarViewMode
    
    // Selected calendar date - persists across navigation
    private val _selectedDate = MutableLiveData<Date?>(null)
    val selectedDate: LiveData<Date?> = _selectedDate
    
    // Refresh trigger - incremented to signal list fragments to refresh
    private val _refreshTrigger = MutableLiveData(0)
    val refreshTrigger: LiveData<Int> = _refreshTrigger

    /**
     * Update the filter state
     */
    fun setFilterState(state: FilterDialogFragment.FilterState) {
        Log.d(TAG, "setFilterState called:")
        Log.d(TAG, "  BEFORE _filterState = ${_filterState.value?.dateSelections?.map { "${it.key}: ${it.value.size} dates" }}")
        Log.d(TAG, "  NEW state = ${state.dateSelections.map { "${it.key}: ${it.value.size} dates" }}")
        _filterState.value = state
        Log.d(TAG, "  AFTER _filterState = ${_filterState.value?.dateSelections?.map { "${it.key}: ${it.value.size} dates" }}")
        updateHighlightedDatesFromFilters()
    }

    /**
     * Update the search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update highlighted dates (combines filter dates and parsed search dates)
     */
    fun setHighlightedDates(dates: List<Date>) {
        _highlightedDates.value = dates
    }
    
    /**
     * Update calendar view mode (day/week/month)
     */
    fun setCalendarViewMode(mode: String) {
        _calendarViewMode.value = mode
    }
    
    /**
     * Update selected calendar date (for day/week view persistence)
     */
    fun setSelectedDate(date: Date?) {
        _selectedDate.value = date
    }

    /**
     * Reset all filters and search
     */
    fun resetAll() {
        _filterState.value = FilterDialogFragment.FilterState()
        _searchQuery.value = ""
        _highlightedDates.value = emptyList()
        // Note: We don't reset view mode on filter clear
    }
    
    /**
     * Trigger a refresh of the order list (e.g., after deleting an order)
     */
    fun triggerRefresh() {
        _refreshTrigger.value = (_refreshTrigger.value ?: 0) + 1
    }

    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return _filterState.value?.hasActiveFilters() == true || 
               !_searchQuery.value.isNullOrEmpty()
    }

    /**
     * Check if dates are selected (disables month/week view)
     */
    fun hasSelectedDates(): Boolean {
        return !_highlightedDates.value.isNullOrEmpty()
    }

    /**
     * Update highlighted dates from current filter state
     */
    private fun updateHighlightedDatesFromFilters() {
        val filterDates = _filterState.value?.dateSelections?.values?.flatten() ?: emptyList()
        if (filterDates.isNotEmpty()) {
            val currentSearchDates = _highlightedDates.value ?: emptyList()
            val combined = (filterDates + currentSearchDates).distinctBy { 
                java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(it)
            }.sortedBy { it.time }
            _highlightedDates.value = combined
        }
    }
}
