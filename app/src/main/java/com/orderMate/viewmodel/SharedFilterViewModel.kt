package com.orderMate.viewmodel

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

    // Filter state
    private val _filterState = MutableLiveData(FilterDialogFragment.FilterState())
    val filterState: LiveData<FilterDialogFragment.FilterState> = _filterState

    // Search query
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // Dates parsed from search or filter (for calendar highlighting)
    private val _searchedDates = MutableLiveData<List<Date>>(emptyList())
    val searchedDates: LiveData<List<Date>> = _searchedDates

    /**
     * Update the filter state
     */
    fun setFilterState(state: FilterDialogFragment.FilterState) {
        _filterState.value = state
        updateSearchedDatesFromFilters()
    }

    /**
     * Update the search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update searched dates (combines filter dates and parsed search dates)
     */
    fun setSearchedDates(dates: List<Date>) {
        _searchedDates.value = dates
    }

    /**
     * Reset all filters and search
     */
    fun resetAll() {
        _filterState.value = FilterDialogFragment.FilterState()
        _searchQuery.value = ""
        _searchedDates.value = emptyList()
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
        return !_searchedDates.value.isNullOrEmpty()
    }

    /**
     * Update searched dates from current filter state
     */
    private fun updateSearchedDatesFromFilters() {
        val filterDates = _filterState.value?.dateSelections?.values?.flatten() ?: emptyList()
        if (filterDates.isNotEmpty()) {
            val currentSearchDates = _searchedDates.value ?: emptyList()
            val combined = (filterDates + currentSearchDates).distinctBy { 
                java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(it)
            }.sortedBy { it.time }
            _searchedDates.value = combined
        }
    }
}
