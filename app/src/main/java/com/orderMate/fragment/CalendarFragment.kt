package com.orderMate.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import com.orderMate.model.ScheduledEvent
import com.orderMate.model.EventType
import com.orderMate.model.LineItemPreview
import com.orderMate.utils.CalendarManager
import com.orderMate.utils.Constants
import com.orderMate.utils.FilterCategoryBuilder
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderDueDateResolver
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.OrderSearchFilter
import com.orderMate.utils.SettingsManager
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import com.orderMate.utils.hideView
import com.orderMate.modals.NoteLevel
import com.orderMate.viewmodel.SharedFilterViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * iOS-style Calendar Tab Fragment (#82 requirement)
 * 
 * Displays scheduled orders as calendar events.
 * Uses same search/filter logic as OrderListRedesignFragment.
 * 
 * Features:
 * - Monthly calendar grid view (default)
 * - Day/Week view modes
 * - Event preview modal on click
 * - Today button - jump to current date
 * - Next Fulfillment button - jump to next scheduled order
 * - Previous/Next navigation
 * - Search and filter (same as List tab)
 */
class CalendarFragment : Fragment() {

    private lateinit var calendarManager: CalendarManager
    private var currentDate: Calendar = Calendar.getInstance()
    private var currentViewMode: String = "month"
    
    // Shared ViewModel for filter/search state persistence across tabs
    private val sharedFilterViewModel: SharedFilterViewModel by activityViewModels()
    
    // Order data (same as List tab)
    private var allOrders: ArrayList<Order?> = ArrayList()
    private var filteredOrders: ArrayList<Order?> = ArrayList()
    
    // Events for calendar display
    private var allEvents: List<ScheduledEvent> = emptyList()
    private var filteredEvents: List<ScheduledEvent> = emptyList()
    
    // Filter state (synced with shared ViewModel)
    private var currentFilterState = FilterDialogFragment.FilterState()
    private var currentSearchQuery: String = ""
    private var selectedDateFilter: Date? = null
    
    // Flag to track if initial filters have been applied after load
    private var isApplyingFilters = false
    private var ordersLoaded = false
    
    private val myApp by lazy { MyApp.getInstance() }
    
    // Timeline views from XML
    private var timelineContainer: LinearLayout? = null
    private var timelineHeader: LinearLayout? = null
    private var timelineScrollView: ScrollView? = null
    private var timelineBody: LinearLayout? = null
    
    // Handler for search debouncing
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchRunnable: Runnable
    
    // Calendar Views
    private var monthYearTitle: TextView? = null
    private var calendarGrid: RecyclerView? = null
    private var weekdayHeaders: LinearLayout? = null
    private var btnPrev: View? = null
    private var btnNext: View? = null
    private var btnDay: TextView? = null
    private var btnWeek: TextView? = null
    private var btnMonth: TextView? = null
    private var btnToday: TextView? = null
    private var btnNextFulfillment: TextView? = null
    
    // Header Views (same as List tab)
    private var searchInput: EditText? = null
    private var calendarIcon: ImageView? = null
    private var calendarIconContainer: View? = null  // (#14) Expanded click area
    private var filterButton: View? = null
    private var resetButton: View? = null
    private var filterPillsScroll: HorizontalScrollView? = null
    private var filterPillsContainer: LinearLayout? = null
    private var syncButton: View? = null
    private var syncingContainer: View? = null
    private var isSyncing = false
    
    // Search pill views
    private var searchPillContainer: View? = null
    private var searchPillText: TextView? = null
    private var searchPillClose: View? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        calendarManager = CalendarManager(requireContext())
        return inflater.inflate(R.layout.fragment_calendar_redesign, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupHeaderActions()
        setupSearchListener()
        setupCalendarNavigation()
        setupViewToggle()
        setupActionButtons()
        observeSharedState()
        loadOrders()
        
        // Sync widgets from Firebase and refresh calendar to ensure pills display on first load
        WidgetManager.getInstance(requireContext()).reloadAll {
            activity?.runOnUiThread {
                if (allOrders.isNotEmpty()) {
                    allEvents = convertOrdersToEvents(allOrders)
                    filteredEvents = convertOrdersToEvents(filteredOrders)
                    renderCalendar()
                }
            }
        }
    }
    
    private fun initViews(view: View) {
        // Calendar views
        monthYearTitle = view.findViewById(R.id.monthYearTitle)
        calendarGrid = view.findViewById(R.id.calendarGrid)
        weekdayHeaders = view.findViewById(R.id.weekdayHeaders)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnDay = view.findViewById(R.id.btnDay)
        btnWeek = view.findViewById(R.id.btnWeek)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnToday = view.findViewById(R.id.btnToday)
        btnNextFulfillment = view.findViewById(R.id.btnNextFulfillment)
        
        // Timeline views (Day/Week view)
        timelineContainer = view.findViewById(R.id.timelineContainer)
        timelineHeader = view.findViewById(R.id.timelineHeader)
        timelineScrollView = view.findViewById(R.id.timelineScrollView)
        timelineBody = view.findViewById(R.id.timelineBody)
        
        // Header views (same as List tab)
        searchInput = view.findViewById(R.id.searchInput)
        calendarIcon = view.findViewById(R.id.calendarIcon)
        calendarIconContainer = view.findViewById(R.id.calendarIconContainer)  // (#14) Expanded click area
        filterButton = view.findViewById(R.id.filterButton)
        resetButton = view.findViewById(R.id.resetButton)
        filterPillsScroll = view.findViewById(R.id.filterPillsScroll)
        filterPillsContainer = view.findViewById(R.id.filterPillsContainer)
        syncButton = view.findViewById(R.id.syncButton)
        syncingContainer = view.findViewById(R.id.syncingContainer)
        
        // Search pill views
        searchPillContainer = view.findViewById(R.id.searchPillContainer)
        searchPillText = view.findViewById(R.id.searchPillText)
        searchPillClose = view.findViewById(R.id.searchPillClose)
        
        // Setup grid layout manager
        calendarGrid?.layoutManager = GridLayoutManager(requireContext(), 7)
    }
    
    /**
     * Observe shared ViewModel for filter/search state changes
     * Restores state when navigating back to this tab
     */
    private fun observeSharedState() {
        // Observe filter state - update pills and apply if orders loaded
        // Note: Initial filter application is handled by loadOrders() to avoid race conditions
        sharedFilterViewModel.filterState.observe(viewLifecycleOwner) { state ->
            currentFilterState = state
            // Always update pills for visual consistency
            updateFilterPills(state)
            // Don't apply filters here during initial load - loadOrders handles that
            // Only apply for subsequent filter changes (when user interacts with filters)
        }
        
        // Observe search query
        sharedFilterViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (query != currentSearchQuery) {
                currentSearchQuery = query
                // Update search input without triggering listener
                searchInput?.apply {
                    if (text.toString() != query) {
                        setText(query)
                        setSelection(query.length)
                    }
                }
                // Update search pill
                updateSearchPill(query)
            }
        }
        
        // Observe searched dates (affects view mode buttons)
        sharedFilterViewModel.searchedDates.observe(viewLifecycleOwner) { dates ->
            searchedDates = dates
            updateViewModeButtonsState()
            // Don't render here - the filter application will handle rendering
        }
        
        // Observe calendar view mode (persists across navigation)
        sharedFilterViewModel.calendarViewMode.observe(viewLifecycleOwner) { mode ->
            if (mode != currentViewMode) {
                currentViewMode = mode
                updateViewModeButtonVisuals(mode)
                // Don't render here - wait for filter application or loadOrders
            }
        }
    }
    
    /**
     * Update view mode button visuals without triggering renderCalendar
     */
    private fun updateViewModeButtonVisuals(mode: String) {
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        
        btnDay?.setBackgroundResource(0)
        btnDay?.setTextColor(inactiveColor)
        btnWeek?.setBackgroundResource(0)
        btnWeek?.setTextColor(inactiveColor)
        btnMonth?.setBackgroundResource(0)
        btnMonth?.setTextColor(inactiveColor)
        
        when (mode) {
            "day" -> {
                btnDay?.setBackgroundResource(R.drawable.bg_view_mode_active)
                btnDay?.setTextColor(activeColor)
            }
            "week" -> {
                btnWeek?.setBackgroundResource(R.drawable.bg_view_mode_active)
                btnWeek?.setTextColor(activeColor)
            }
            "month" -> {
                btnMonth?.setBackgroundResource(R.drawable.bg_view_mode_active)
                btnMonth?.setTextColor(activeColor)
            }
        }
    }
    
    /**
     * Update view mode buttons based on whether dates are selected
     * Disables Month/Week when dates are searched/filtered
     * Disables Today/Next Fulfillment when individual date is selected
     */
    private fun updateViewModeButtonsState() {
        val hasSelectedDates = searchedDates.isNotEmpty()
        val disabledAlpha = 0.4f
        val enabledAlpha = 1.0f
        
        btnMonth?.apply {
            isEnabled = !hasSelectedDates
            alpha = if (hasSelectedDates) disabledAlpha else enabledAlpha
        }
        btnWeek?.apply {
            isEnabled = !hasSelectedDates
            alpha = if (hasSelectedDates) disabledAlpha else enabledAlpha
        }
        // Day view is always enabled
        btnDay?.apply {
            isEnabled = true
            alpha = enabledAlpha
        }
        
        // Disable Today and Next Fulfillment when dates are selected
        btnToday?.apply {
            isEnabled = !hasSelectedDates
            alpha = if (hasSelectedDates) disabledAlpha else enabledAlpha
        }
        btnNextFulfillment?.apply {
            isEnabled = !hasSelectedDates
            alpha = if (hasSelectedDates) disabledAlpha else enabledAlpha
        }
    }
    
    private fun setupHeaderActions() {
        filterButton?.setOnClickListener { showFilterDialog() }
        resetButton?.setOnClickListener { resetFilters() }
        // (#14) Use container for expanded click area
        calendarIconContainer?.setOnClickListener { showDatePicker() }
        // Sync button
        syncButton?.setOnClickListener {
            if (!isSyncing) {
                syncOrders()
            }
        }
    }
    
    private fun syncOrders() {
        isSyncing = true
        searchInput?.text?.clear()
        
        // Show syncing indicator
        syncingContainer?.visibility = View.VISIBLE
        syncButton?.isEnabled = false
        syncButton?.alpha = 0.5f
        
        runOnBackgroundThread {
            try {
                val orderConnector = (requireActivity().application as MyApp).getOrderConnector()
                val orderData = orderConnector.getOrders(mutableListOf())
                allOrders.clear()
                orderData?.forEach { allOrders.add(it) }
                allEvents = convertOrdersToEvents(allOrders)
                filteredEvents = allEvents
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            runOnMainThread {
                isSyncing = false
                syncingContainer?.visibility = View.GONE
                syncButton?.isEnabled = true
                syncButton?.alpha = 1.0f
                
                renderCalendar()
                Toast.makeText(requireContext(), "Calendar synced", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupSearchListener() {
        searchInput?.doAfterTextChanged { text ->
            if (this::searchRunnable.isInitialized) {
                handler.removeCallbacks(searchRunnable)
            }
            searchRunnable = Runnable {
                currentSearchQuery = text.toString().trim()
                // Sync to shared ViewModel for cross-tab persistence
                sharedFilterViewModel.setSearchQuery(currentSearchQuery)
                // Update search pill immediately
                updateSearchPill(currentSearchQuery)
                searchOrders(currentSearchQuery)
            }
            handler.postDelayed(searchRunnable, Constants.debouncingTime)
        }
        
        // Search pill close button - clears search
        searchPillClose?.setOnClickListener {
            searchInput?.text?.clear()
            currentSearchQuery = ""
            sharedFilterViewModel.setSearchQuery("")
            updateSearchPill("")
            searchOrders("")
        }
    }
    
    /**
     * Update search pill visibility and text based on search query
     */
    private fun updateSearchPill(query: String) {
        if (query.isNotBlank()) {
            searchPillContainer?.showView()
            searchPillText?.text = query
        } else {
            searchPillContainer?.hideView()
        }
    }
    
    private fun setupCalendarNavigation() {
        btnPrev?.setOnClickListener { navigateMonth(-1) }
        btnNext?.setOnClickListener { navigateMonth(1) }
    }
    
    private fun setupViewToggle() {
        btnDay?.setOnClickListener { setViewMode("day") }
        btnWeek?.setOnClickListener { setViewMode("week") }
        btnMonth?.setOnClickListener { setViewMode("month") }
    }
    
    private fun setupActionButtons() {
        btnToday?.setOnClickListener { goToToday() }
        btnNextFulfillment?.setOnClickListener { goToNextFulfillment() }
    }
    
    /**
     * Load orders from Clover (same as List tab)
     */
    private fun loadOrders() {
        ordersLoaded = false
        
        runOnBackgroundThread {
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                allOrders.clear()
                filteredOrders.clear()
                
                orderData?.forEach {
                    allOrders.add(it)
                    filteredOrders.add(it)
                }
                
                // Convert orders to events for calendar display
                allEvents = convertOrdersToEvents(allOrders)
                filteredEvents = allEvents
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnMainThread {
                ordersLoaded = true
                
                // Restore selected date from shared state FIRST
                sharedFilterViewModel.selectedDate.value?.let { date ->
                    currentDate.time = date
                }
                
                // Restore searched dates from shared state
                val restoredSearchedDates = sharedFilterViewModel.searchedDates.value ?: emptyList()
                searchedDates = restoredSearchedDates
                
                // Restore view mode from shared state
                sharedFilterViewModel.calendarViewMode.value?.let { mode ->
                    currentViewMode = mode
                    updateViewModeButtonVisuals(mode)
                }
                
                updateViewModeButtonsState()
                
                // Apply any pending shared state after orders are loaded
                val sharedState = sharedFilterViewModel.filterState.value
                if (sharedState != null && sharedState.hasActiveFilters()) {
                    currentFilterState = sharedState
                    // applyFiltersSync handles pills and rendering
                    applyFiltersSync(sharedState)
                } else if (restoredSearchedDates.isNotEmpty()) {
                    // We have searched dates but no active filters - need to filter by those dates
                    currentFilterState = currentFilterState.copy(
                        dateSelections = mapOf(FilterCategoryBuilder.CLOVER_ORDER_DATE to restoredSearchedDates)
                    )
                    // applyFiltersSync handles pills and rendering
                    applyFiltersSync(currentFilterState)
                } else {
                    renderCalendar()
                }
            }
        }
    }
    
    /**
     * Apply filters synchronously (on current thread)
     * Used to avoid race conditions with async operations
     */
    private fun applyFiltersSync(filters: FilterDialogFragment.FilterState) {
        if (!isAdded || view == null) return  // Safety check
        
        filteredOrders.clear()
        
        allOrders.forEach { order ->
            if (orderMatchesFilters(order, filters)) {
                filteredOrders.add(order)
            }
        }
        
        filteredEvents = convertOrdersToEvents(filteredOrders)
        
        // Update searchedDates from filter state
        val filterDates = filters.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE] ?: emptyList()
        val queryDates = if (currentSearchQuery.isNotEmpty()) {
            OrderSearchFilter.parseSearchDates(currentSearchQuery, currentDate.get(Calendar.YEAR))
        } else {
            emptyList()
        }
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val combinedDates = (filterDates + queryDates)
            .distinctBy { dateFormat.format(it) }
            .sortedBy { it.time }
        
        searchedDates = combinedDates
        sharedFilterViewModel.setSearchedDates(combinedDates)
        
        updateViewModeButtonsState()
        updateFilterPills(filters)
        renderCalendar()
    }
    
    /**
     * Convert Clover orders to calendar events
     */
    private fun convertOrdersToEvents(orders: List<Order?>): List<ScheduledEvent> {
        // Get widgets for widget-based parsing (#29, #30)
        val allWidgets = WidgetManager.getCachedWidgets()
        // Get ORDER level widgets only for order-level tag extraction
        val orderLevelWidgets = allWidgets.filter { it.level == NoteLevel.ORDER && it.isEnabled }
        
        return orders.mapNotNull { order ->
            order ?: return@mapNotNull null
            
            // (#29) Use widget-based due date resolution
            val dueDate = if (allWidgets.isNotEmpty()) {
                OrderDueDateResolver.resolveDueDate(order, allWidgets)
            } else {
                OrderDueDateResolver.resolveDueDate(order)  // Legacy fallback
            } ?: return@mapNotNull null
            
            val customerName = try {
                val customer = order.customers?.firstOrNull()
                "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".trim().ifEmpty { "-" }
            } catch (e: Exception) { "-" }
            
            val total = try {
                (order.total ?: 0L) / 100.0
            } catch (e: Exception) { 0.0 }
            
            val itemCount = try {
                order.lineItems?.size ?: 0
            } catch (e: Exception) { 0 }
            
            // Get line items for preview (include note for widget-based parsing)
            val lineItems = try {
                order.lineItems?.mapNotNull { lineItem ->
                    lineItem?.let {
                        LineItemPreview(
                            name = it.name ?: "Unknown",
                            price = (it.price ?: 0L) / 100.0,
                            quantity = it.unitQty?.toInt() ?: 1,
                            note = it.note
                        )
                    }
                }?.take(5) ?: emptyList()
            } catch (e: Exception) { emptyList() }
            
            // Determine event type based on order data
            val eventType = determineEventType(order)
            
            // (#30) Extract order-level tags for event preview (ORDER level widgets only)
            val customTags = extractOrderLevelTags(order.note, orderLevelWidgets)
            
            ScheduledEvent(
                id = order.id?.hashCode()?.toLong() ?: System.currentTimeMillis(),
                orderId = order.id ?: "",
                customerName = customerName,
                type = eventType,
                dueDate = dueDate,
                total = total,
                itemCount = itemCount,
                note = null,
                lineItems = lineItems,
                orderNote = order.note,
                customTags = customTags  // (#30) Add custom tags for event preview
            )
        }
    }
    
    /**
     * (#30) Extract order-level tags from order.note using widget configuration.
     * Returns list of formatted "Label: Value" strings.
     * Includes all 4 widget types (SINGLE_SELECT, MULTI_SELECT, CALENDAR, TEXT_BOX).
     */
    private fun extractOrderLevelTags(orderNote: String?, widgets: List<com.orderMate.modals.WidgetConfig>): List<com.orderMate.model.EventTag> {
        if (orderNote.isNullOrBlank() || widgets.isEmpty()) return emptyList()
        
        // Include TEXT_BOX for event preview (will be truncated in EventPreviewDialog)
        val tags = OrderNoteParser.extractTagsFromNote(orderNote, widgets, NoteLevel.ORDER, includeTextBox = true)
        // Only return tags that have actual values (not empty)
        return tags
            .filter { it.value.isNotBlank() }
            .map { com.orderMate.model.EventTag("${it.label}: ${it.value}", it.widgetType) }
    }
    
    private fun determineEventType(order: Order): EventType {
        // Check line item notes for delivery/pickup indicators
        val hasDelivery = order.lineItems?.any { 
            it?.note?.lowercase()?.contains("delivery") == true 
        } == true
        
        if (hasDelivery) return EventType.DELIVERY
        
        // Default to pickup
        return EventType.PICKUP
    }

    // ==================== Search (matches HTML - includes date parsing) ====================
    
    // Dates parsed from search query (matches HTML searchedDates)
    private var searchedDates: List<Date> = emptyList()
    
    private fun searchOrders(query: String?) {
        runOnBackgroundThread {
            val isFilterApplied = currentFilterState.hasActiveFilters()
            
            // Parse dates from search query (matches HTML parseSearchDates)
            searchedDates = if (!query.isNullOrEmpty()) {
                OrderSearchFilter.parseSearchDates(query, currentDate.get(Calendar.YEAR))
            } else {
                emptyList()
            }
            
            // Also include dates from filter state (matches HTML behavior)
            val filterDates = currentFilterState.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE] ?: emptyList()
            if (filterDates.isNotEmpty()) {
                val combined = (searchedDates + filterDates).distinctBy { 
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it)
                }.sortedBy { it.time }
                searchedDates = combined
            }

            if (query.isNullOrEmpty()) {
                filteredOrders.clear()
                if (isFilterApplied) {
                    // Keep filtered results
                } else {
                    allOrders.forEach { filteredOrders.add(it) }
                }
            } else {
                filteredOrders.clear()
                val sourceList = allOrders
                
                for (order in sourceList) {
                    if (matchesSearch(order, query.lowercase())) {
                        filteredOrders.add(order)
                    }
                }
            }
            
            // Convert filtered orders to events
            filteredEvents = convertOrdersToEvents(filteredOrders)

            runOnMainThread {
                renderCalendar()
            }
        }
    }
    
    /**
     * Search matching logic (same as List tab)
     */
    private fun matchesSearch(order: Order?, query: String): Boolean {
        // Use shared search logic
        return OrderSearchFilter.matchesSearch(order, query)
    }
    
    /**
     * Get dates to highlight/filter on calendar
     * Combines dates from search query and filter state (matches HTML)
     */
    fun getSearchedDates(): List<Date> = searchedDates

    // ==================== Date Picker (matches HTML - multiple dates with pills) ====================
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        // Use last selected date or current date
        val existingDates = currentFilterState.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE]
        existingDates?.lastOrNull()?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_Dialog,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val selectedDate = calendar.time
                
                // Add date to filter state (supports multiple dates like HTML)
                addDateToFilterState(selectedDate)
                
                // Navigate calendar to this date and persist
                currentDate.set(year, month, day)
                sharedFilterViewModel.setSelectedDate(currentDate.time)
                
                // Sync filter state to shared ViewModel
                sharedFilterViewModel.setFilterState(currentFilterState)
                
                // Apply filters and update pills
                applyDialogFilters(currentFilterState)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    /**
     * Add a date to the current filter state (matches HTML behavior)
     * Creates a pill for the date in the filter pills row
     */
    private fun addDateToFilterState(date: Date) {
        val newDateSelections = currentFilterState.dateSelections.toMutableMap()
        val categoryId = FilterCategoryBuilder.CLOVER_ORDER_DATE
        
        val existingDates = newDateSelections[categoryId]?.toMutableList() ?: mutableListOf()
        
        // Check if date already exists (compare by day only)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val newDateStr = dateFormat.format(date)
        val alreadyExists = existingDates.any { dateFormat.format(it) == newDateStr }
        
        if (!alreadyExists) {
            existingDates.add(date)
            newDateSelections[categoryId] = existingDates
            currentFilterState = currentFilterState.copy(dateSelections = newDateSelections)
        }
    }
    
    private fun filterByDate(date: Date) {
        // Deprecated - now using addDateToFilterState + applyDialogFilters for consistency
        runOnBackgroundThread {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val targetDate = dateFormat.format(date)

            filteredOrders.clear()

            for (order in allOrders) {
                val orderDate = order?.createdTime?.let {
                    dateFormat.format(Date(it))
                }
                if (orderDate == targetDate) {
                    filteredOrders.add(order)
                }
            }
            
            filteredEvents = convertOrdersToEvents(filteredOrders)

            runOnMainThread {
                renderCalendar()
            }
        }
    }

    // ==================== Filters (same as List tab) ====================
    
    private fun showFilterDialog() {
        val filterableWidgets = WidgetManager.getInstance(requireContext()).getFilterableWidgets() ?: emptyList()
        val settingsManager = SettingsManager(requireContext())
        val categories = FilterCategoryBuilder.buildCategories(allOrders, filterableWidgets, settingsManager)
        
        val dialog = FilterDialogFragment.newInstance(
            categories = categories,
            currentFilters = currentFilterState
        )
        
        dialog.setFilterListener(object : FilterDialogFragment.FilterListener {
            override fun onFiltersApplied(filters: FilterDialogFragment.FilterState) {
                currentFilterState = filters
                // Sync to shared ViewModel for cross-tab persistence
                sharedFilterViewModel.setFilterState(filters)
                applyDialogFilters(filters)
            }

            override fun onFilterCleared() {
                currentFilterState = FilterDialogFragment.FilterState()
                sharedFilterViewModel.resetAll()
                resetFilters()
            }
        })
        
        dialog.show(childFragmentManager, FilterDialogFragment.TAG)
    }
    
    private fun applyDialogFilters(filters: FilterDialogFragment.FilterState) {
        // Use sync version to avoid race conditions
        applyFiltersSync(filters)
    }
    
    /**
     * Order filter matching logic (same as List tab)
     */
    private fun orderMatchesFilters(order: Order?, filters: FilterDialogFragment.FilterState): Boolean {
        if (order == null) return false

        for ((categoryId, selectedValues) in filters.selections) {
            if (selectedValues.isEmpty()) continue
            
            when {
                categoryId == FilterCategoryBuilder.CLOVER_PAYMENT_STATUS -> {
                    val orderPayment = order.paymentState?.name ?: ""
                    if (!selectedValues.contains(orderPayment)) return false
                }
                categoryId == FilterCategoryBuilder.CLOVER_ORDER_STATUS -> {
                    val orderState = order.state?.lowercase() ?: ""
                    if (!selectedValues.any { it.lowercase() == orderState }) return false
                }
                categoryId == FilterCategoryBuilder.CLOVER_PAYMENT_TYPE -> {
                    val paymentTypes = order.payments?.mapNotNull { it?.tender?.label?.lowercase() } ?: emptyList()
                    val selectedLower = selectedValues.map { it.lowercase() }
                    if (!paymentTypes.any { it in selectedLower }) return false
                }
                categoryId == FilterCategoryBuilder.CLOVER_EMPLOYEE -> {
                    val employeeName = try {
                        order.employee?.jsonObject?.getString("name") ?: ""
                    } catch (e: Exception) { "" }
                    if (!selectedValues.contains(employeeName)) return false
                }
                FilterCategoryBuilder.isWidgetFilter(categoryId) -> {
                    val widgetId = FilterCategoryBuilder.getWidgetId(categoryId)
                    val widget = WidgetManager.getInstance(requireContext()).getWidgetById(widgetId ?: "") ?: continue
                    val orderValues = extractWidgetValuesFromNotes(order.lineItems, widget.label)
                    if (!selectedValues.any { it in orderValues }) return false
                }
            }
        }

        for ((categoryId, dates) in filters.dateSelections) {
            if (dates.isEmpty()) continue
            
            when {
                categoryId == FilterCategoryBuilder.CLOVER_ORDER_DATE -> {
                    val orderDate = order.createdTime?.let { Date(it) }
                    if (orderDate == null) return false
                    
                    val matchesAny = dates.any { filterDate ->
                        isSameDay(orderDate, filterDate)
                    }
                    if (!matchesAny) return false
                }
            }
        }

        return true
    }
    
    private fun extractWidgetValuesFromNotes(lineItems: List<com.clover.sdk.v3.order.LineItem>?, widgetLabel: String): Set<String> {
        val values = mutableSetOf<String>()
        lineItems?.forEach { lineItem ->
            val note = lineItem?.note ?: return@forEach
            if (note.contains(widgetLabel, ignoreCase = true)) {
                val parts = note.split("\n")
                parts.forEach { part ->
                    if (part.contains(widgetLabel, ignoreCase = true)) {
                        val value = part.substringAfter(":").trim()
                        if (value.isNotEmpty()) values.add(value)
                    }
                }
            }
        }
        return values
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun resetFilters() {
        currentFilterState = FilterDialogFragment.FilterState()
        currentSearchQuery = ""
        selectedDateFilter = null
        searchedDates = emptyList()
        
        // Sync reset to shared ViewModel for cross-tab persistence
        sharedFilterViewModel.resetAll()
        
        searchInput?.text?.clear()
        searchInput?.hint = getString(R.string.search_orders)
        
        filterPillsScroll?.visibility = View.GONE
        filterPillsContainer?.removeAllViews()
        
        filteredOrders.clear()
        allOrders.forEach { filteredOrders.add(it) }
        filteredEvents = allEvents
        
        updateViewModeButtonsState()
        renderCalendar()
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateFilterPills(filters: FilterDialogFragment.FilterState) {
        filterPillsContainer?.removeAllViews()
        
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        var hasPills = false
        
        // Add selection filter pills with formatted display (matches List tab)
        for ((categoryId, values) in filters.selections) {
            values.forEach { value ->
                hasPills = true
                val displayValue = formatFilterValue(categoryId, value)
                val pill = createFilterPillWithClose(displayValue) {
                    removeSelectionFilter(categoryId, value)
                }
                filterPillsContainer?.addView(pill)
            }
        }
        
        // Add date filter pills (with "Order Date:" label like HTML)
        for ((categoryId, dates) in filters.dateSelections) {
            val label = when (categoryId) {
                FilterCategoryBuilder.CLOVER_ORDER_DATE -> "Order Date"
                else -> {
                    if (FilterCategoryBuilder.isWidgetFilter(categoryId)) {
                        val widgetId = FilterCategoryBuilder.getWidgetId(categoryId)
                        WidgetManager.getInstance(requireContext()).getWidgetById(widgetId ?: "")?.label ?: "Date"
                    } else "Date"
                }
            }
            dates.forEachIndexed { index, date ->
                hasPills = true
                val pill = createFilterPillWithClose("$label: ${dateFormat.format(date)}") {
                    removeDateFilter(categoryId, index)
                }
                filterPillsContainer?.addView(pill)
            }
        }
        
        filterPillsScroll?.visibility = if (hasPills) View.VISIBLE else View.GONE
    }
    
    /**
     * Format filter values for display (consistent with List tab)
     */
    private fun formatFilterValue(categoryId: String, value: String): String {
        return when (categoryId) {
            FilterCategoryBuilder.CLOVER_PAYMENT_STATUS -> formatPaymentStatus(value)
            FilterCategoryBuilder.CLOVER_ORDER_STATUS -> formatOrderStatus(value)
            else -> value
        }
    }
    
    private fun formatPaymentStatus(status: String): String {
        return when (status.uppercase()) {
            "PAID" -> "Paid"
            "NOT_PAID" -> "Unpaid"
            "PARTIALLY_PAID" -> "Partial"
            "REFUNDED" -> "Refunded"
            else -> status.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun formatOrderStatus(status: String): String {
        return when (status.lowercase()) {
            "open" -> "Open"
            "locked" -> "Closed"
            else -> status.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun removeSelectionFilter(categoryId: String, value: String) {
        val newSelections = currentFilterState.selections.toMutableMap()
        newSelections[categoryId]?.let { values ->
            val newValues = values.toMutableSet()
            newValues.remove(value)
            if (newValues.isEmpty()) {
                newSelections.remove(categoryId)
            } else {
                newSelections[categoryId] = newValues
            }
        }
        currentFilterState = currentFilterState.copy(selections = newSelections)
        // Sync to shared ViewModel for cross-tab consistency
        sharedFilterViewModel.setFilterState(currentFilterState)
        applyDialogFilters(currentFilterState)
    }
    
    private fun removeDateFilter(categoryId: String, index: Int) {
        val newDateSelections = currentFilterState.dateSelections.toMutableMap()
        newDateSelections[categoryId]?.let { dates ->
            val newDates = dates.toMutableList()
            if (index < newDates.size) {
                newDates.removeAt(index)
            }
            if (newDates.isEmpty()) {
                newDateSelections.remove(categoryId)
            } else {
                newDateSelections[categoryId] = newDates
            }
        }
        currentFilterState = currentFilterState.copy(dateSelections = newDateSelections)
        // Sync to shared ViewModel for cross-tab consistency
        sharedFilterViewModel.setFilterState(currentFilterState)
        applyDialogFilters(currentFilterState)
    }
    
    private fun createFilterPillWithClose(text: String, onClose: () -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_filter_pill)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6))
            
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dpToPx(8)
            lp.bottomMargin = dpToPx(4)
            layoutParams = lp
            
            val textView = TextView(context).apply {
                this.text = text
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            addView(textView)
            
            val closeBtn = android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
                val size = dpToPx(16)
                val closeLp = LinearLayout.LayoutParams(size, size)
                closeLp.marginStart = dpToPx(6)
                layoutParams = closeLp
                setOnClickListener { onClose() }
            }
            addView(closeBtn)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // ==================== View Mode ====================
    
    private fun setViewMode(mode: String) {
        currentViewMode = mode
        
        // Sync to shared ViewModel for persistence across navigation
        sharedFilterViewModel.setCalendarViewMode(mode)
        
        updateViewModeButtonVisuals(mode)
        renderCalendar()
    }

    // ==================== Calendar Rendering ====================
    
    fun renderCalendar() {
        // Check if we have searched dates (matches HTML behavior)
        // When dates are searched/filtered, show those specific dates
        val hasSearchedDates = searchedDates.isNotEmpty()
        
        // Update nav button visibility (matches HTML: hide when searching dates)
        val navPrev = view?.findViewById<View>(R.id.btnPrev)
        val navNext = view?.findViewById<View>(R.id.btnNext)
        val navVisibility = if (hasSearchedDates) View.INVISIBLE else View.VISIBLE
        navPrev?.visibility = navVisibility
        navNext?.visibility = navVisibility
        
        when {
            searchedDates.size > 1 -> {
                // Multiple dates searched - show side-by-side view (matches HTML renderMultipleDaysView)
                monthYearTitle?.text = "${searchedDates.size} Selected Dates"
                renderSearchedDatesView()
            }
            searchedDates.size == 1 -> {
                // Single date searched - show day view for that date (matches HTML renderSingleSearchedDay)
                val searchedDate = searchedDates.first()
                currentDate.time = searchedDate
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                monthYearTitle?.text = dateFormat.format(searchedDate)
                renderDayView()
            }
            else -> {
                // No searched dates - normal view mode
                when (currentViewMode) {
                    "day" -> renderDayView()
                    "week" -> renderWeekView()
                    else -> renderMonthView()
                }
            }
        }
    }
    
    /**
     * Render multiple searched dates side by side (matches HTML renderMultipleDaysView)
     */
    private fun renderSearchedDatesView() {
        if (searchedDates.isEmpty()) {
            renderMonthView()
            return
        }
        
        // Hide the grid and show timeline
        calendarGrid?.visibility = View.GONE
        weekdayHeaders?.visibility = View.GONE
        
        // Render timeline with searched dates
        renderTimelineViewForDates(searchedDates)
    }
    
    /**
     * Render timeline view for specific dates (used for searched dates)
     * Uses XML-defined views for proper layout (fixed header height)
     */
    private fun renderTimelineViewForDates(dates: List<Date>) {
        if (dates.isEmpty()) return
        
        val context = context ?: return
        val today = Calendar.getInstance()
        val dayNamesShort = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val hourHeight = 60
        val density = resources.displayMetrics.density
        val hourHeightPx = (hourHeight * density).toInt()
        
        // Show timeline container (XML-defined)
        timelineContainer?.visibility = View.VISIBLE
        
        // Clear and populate header (XML-defined, fixed 40dp height)
        timelineHeader?.removeAllViews()
        
        // Gutter space for hour labels
        timelineHeader?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams((50 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        })
        
        // Day headers - single line format "SAT - 21" (matching HTML and renderTimelineView)
        dates.forEach { date ->
            val cal = Calendar.getInstance().apply { time = date }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                          cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            
            val headerView = TextView(context).apply {
                val dayName = dayNamesShort[dayOfWeek]
                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                text = "$dayName - $dayNum"
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                letterSpacing = 0.05f
                setTypeface(null, android.graphics.Typeface.BOLD)
                
                if (isToday) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.today_highlight_bg))
                    setTextColor(ContextCompat.getColor(context, R.color.accent_orange))
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }
            }
            timelineHeader?.addView(headerView)
        }
        
        // Clear and populate body (XML-defined ScrollView)
        timelineBody?.removeAllViews()
        
        // Hours column
        val hoursColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams((50 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        for (hour in 0..23) {
            val hourView = TextView(context).apply {
                text = if (hour == 0) "12 AM" else if (hour < 12) "$hour AM" else if (hour == 12) "12 PM" else "${hour - 12} PM"
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    hourHeightPx
                )
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setPadding(0, 0, (8 * density).toInt(), 0)
            }
            hoursColumn.addView(hourView)
        }
        timelineBody?.addView(hoursColumn)
        
        // Day columns with events
        val columnsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        dates.forEach { date ->
            val cal = Calendar.getInstance().apply { time = date }
            val dayEvents = filteredEvents.filter { event ->
                val eventCal = Calendar.getInstance().apply { time = event.dueDate }
                eventCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                eventCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }
            
            val dayColumn = android.widget.FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 24 * hourHeightPx, 1f)
                setBackgroundResource(R.drawable.bg_timeline_column)
            }
            
            // Group events by hour to handle overlapping events horizontally (like iCal)
            val eventsByHour = dayEvents.groupBy { event ->
                val eventCal = Calendar.getInstance().apply { time = event.dueDate }
                eventCal.get(Calendar.HOUR_OF_DAY)
            }
            
            // Add events to this day column with horizontal stacking for same-time events
            eventsByHour.forEach { (_, eventsAtSameHour) ->
                val eventCount = eventsAtSameHour.size
                
                eventsAtSameHour.forEachIndexed { index, event ->
                    val eventCal = Calendar.getInstance().apply { time = event.dueDate }
                    val hour = eventCal.get(Calendar.HOUR_OF_DAY)
                    val minute = eventCal.get(Calendar.MINUTE)
                    val topPx = (hour * hourHeightPx + minute * hourHeightPx / 60)
                    val eventHeight = (hourHeightPx * 0.8).toInt()
                    
                    // Calculate horizontal position for side-by-side stacking
                    val columnWidthFraction = 1f / eventCount
                    val leftOffsetFraction = index * columnWidthFraction
                    
                    val eventView = TextView(context).apply {
                        text = "${event.customerName}\n${event.type.getDisplayName()}"
                        textSize = if (eventCount > 2) 8f else 10f
                        setTextColor(ContextCompat.getColor(context, R.color.text_light))
                        setPadding((2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt())
                        maxLines = if (eventCount > 2) 1 else 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        
                        val bgRes = when (event.type) {
                            EventType.PICKUP -> R.drawable.bg_calendar_event_pickup
                            EventType.DELIVERY -> R.drawable.bg_calendar_event_delivery
                            EventType.PREORDER -> R.drawable.bg_calendar_event_preorder
                        }
                        setBackgroundResource(bgRes)
                        
                        setOnClickListener {
                            // Show event preview dialog
                            val dialog = EventPreviewDialog.newInstance(listOf(event), event.dueDate)
                            dialog.setOnEventClickListener { e ->
                                viewFullOrderDetails(e.orderId)
                            }
                            dialog.show(childFragmentManager, EventPreviewDialog.TAG)
                        }
                    }
                    
                    // Use custom layout params to position horizontally
                    eventView.layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        eventHeight
                    ).apply {
                        topMargin = topPx
                        marginStart = (2 * density).toInt()
                        marginEnd = (2 * density).toInt()
                    }
                    
                    // Post to apply horizontal positioning after layout
                    eventView.post {
                        val parent = eventView.parent as? android.widget.FrameLayout ?: return@post
                        val parentWidth = parent.width - (4 * density).toInt() // account for margins
                        val eventWidth = (parentWidth * columnWidthFraction).toInt()
                        val leftOffset = (parentWidth * leftOffsetFraction).toInt()
                        
                        eventView.layoutParams = android.widget.FrameLayout.LayoutParams(
                            eventWidth,
                            eventHeight
                        ).apply {
                            topMargin = topPx
                            marginStart = leftOffset + (2 * density).toInt()
                        }
                    }
                    
                    dayColumn.addView(eventView)
                }
            }
            
            columnsContainer.addView(dayColumn)
        }
        
        timelineBody?.addView(columnsContainer)
        
        // Auto-scroll to first event (1 hour before) or current time
        if (dates.isNotEmpty()) {
            val startDate = Calendar.getInstance().apply { time = dates.first() }
            timelineScrollView?.post {
                val scrollPosition = calculateAutoScrollPosition(startDate, dates.size, hourHeightPx)
                timelineScrollView?.scrollTo(0, scrollPosition)
            }
        }
    }
    
    private fun renderMonthView() {
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTitle?.text = dateFormat.format(currentDate.time)
        
        // Hide timeline, show grid and weekday headers
        timelineContainer?.visibility = View.GONE
        calendarGrid?.visibility = View.VISIBLE
        weekdayHeaders?.visibility = View.VISIBLE
        
        val monthEvents = filteredEvents.filter { event ->
            val eventCal = Calendar.getInstance()
            eventCal.time = event.dueDate
            eventCal.get(Calendar.YEAR) == year && eventCal.get(Calendar.MONTH) == month
        }
        
        // Show grid with 7 columns for month view
        calendarGrid?.layoutManager = GridLayoutManager(requireContext(), 7)
        
        val days = generateCalendarDays(year, month, monthEvents)
        
        calendarGrid?.adapter = CalendarDayAdapter(days) { day ->
            if (day.hasEvents) {
                // (#26, #27) Multi-event day opens day view, single event opens preview
                if (day.events.size > 1) {
                    // (#26) Multi-event: switch to day view
                    currentDate.set(Calendar.DAY_OF_MONTH, day.dayNumber)
                    setViewMode("day")
                } else {
                    // (#27) Single event: open preview modal directly
                    showDayEventsDialog(day)
                }
            }
        }
    }
    
    private fun renderWeekView() {
        val year = currentDate.get(Calendar.YEAR)
        
        // Get week start (Sunday)
        val weekStart = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val weekEnd = Calendar.getInstance().apply {
            time = weekStart.time
            add(Calendar.DAY_OF_MONTH, 6)
        }
        monthYearTitle?.text = "${dateFormat.format(weekStart.time)} - ${dateFormat.format(weekEnd.time)}, ${year}"
        
        // Hide grid and weekday headers, show timeline
        calendarGrid?.visibility = View.GONE
        weekdayHeaders?.visibility = View.GONE
        
        // Show timeline view with 7 days
        renderTimelineView(weekStart, 7)
    }
    
    private fun renderDayView() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        monthYearTitle?.text = dateFormat.format(currentDate.time)
        
        // Hide grid and weekday headers, show timeline
        calendarGrid?.visibility = View.GONE
        weekdayHeaders?.visibility = View.GONE
        
        // Show timeline view with 1 day
        renderTimelineView(currentDate, 1)
    }
    
    /**
     * Renders the timeline view with hours on the left and day columns
     * Uses XML-defined views for proper layout (fixed header height)
     */
    private fun renderTimelineView(startDate: Calendar, numDays: Int) {
        val context = context ?: return
        val today = Calendar.getInstance()
        val dayNamesShort = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val hourHeight = 60 // dp per hour
        val density = resources.displayMetrics.density
        val hourHeightPx = (hourHeight * density).toInt()
        
        // Show timeline container (XML-defined)
        timelineContainer?.visibility = View.VISIBLE
        
        // Clear and populate header (XML-defined, fixed 40dp height)
        timelineHeader?.removeAllViews()
        
        // Gutter space for hour labels
        val gutter = View(context).apply {
            layoutParams = LinearLayout.LayoutParams((50 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        }
        timelineHeader?.addView(gutter)
        
        // Day headers - single line format "SAT - 21" (matching HTML)
        for (i in 0 until numDays) {
            val dayCalendar = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.DAY_OF_MONTH, i)
            }
            
            val isToday = dayCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                         dayCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            
            // Single line header: "SAT - 21" (matching weekdayHeaders XML style)
            val dayHeader = TextView(context).apply {
                val dayName = dayNamesShort[dayCalendar.get(Calendar.DAY_OF_WEEK) - 1]
                val dayNum = dayCalendar.get(Calendar.DAY_OF_MONTH)
                text = "$dayName - $dayNum"
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                letterSpacing = 0.05f
                setTypeface(null, android.graphics.Typeface.BOLD)
                
                if (isToday) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.today_highlight_bg))
                    setTextColor(ContextCompat.getColor(context, R.color.accent_orange))
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }
            }
            
            timelineHeader?.addView(dayHeader)
        }
        
        // Clear and populate body (XML-defined ScrollView)
        timelineBody?.removeAllViews()
        
        // Hour labels column
        val hoursColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams((50 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        for (hour in 0..23) {
            val label = when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
            
            val hourLabel = TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                gravity = android.view.Gravity.END or android.view.Gravity.TOP
                setPadding(0, (2 * density).toInt(), (8 * density).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    hourHeightPx
                )
            }
            hoursColumn.addView(hourLabel)
        }
        
        timelineBody?.addView(hoursColumn)
        
        // Day columns container
        val columnsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        for (i in 0 until numDays) {
            val dayCalendar = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.DAY_OF_MONTH, i)
            }
            
            val isToday = dayCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                         dayCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            
            // Get events for this day
            val dayEvents = filteredEvents.filter { event ->
                val eventCal = Calendar.getInstance()
                eventCal.time = event.dueDate
                eventCal.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                eventCal.get(Calendar.DAY_OF_YEAR) == dayCalendar.get(Calendar.DAY_OF_YEAR)
            }
            
            val column = android.widget.FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 24 * hourHeightPx, 1f)
                if (isToday) {
                    setBackgroundResource(R.drawable.bg_timeline_column_today)
                } else {
                    setBackgroundResource(R.drawable.bg_timeline_column)
                }
            }
            
            // Hour row dividers
            val rowsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            for (hour in 0..23) {
                val row = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        hourHeightPx
                    )
                    setBackgroundResource(R.drawable.bg_timeline_row)
                }
                rowsContainer.addView(row)
            }
            column.addView(rowsContainer)
            
            // Group events by hour to handle overlapping events horizontally (like iCal)
            val eventsByHour = dayEvents.groupBy { event ->
                val eventCal = Calendar.getInstance().apply { time = event.dueDate }
                eventCal.get(Calendar.HOUR_OF_DAY)
            }
            
            // Add events with horizontal stacking for same-time events
            eventsByHour.forEach { (_, eventsInHour) ->
                val eventCount = eventsInHour.size
                eventsInHour.forEachIndexed { index, event ->
                    val eventCal = Calendar.getInstance().apply { time = event.dueDate }
                    val eventHour = eventCal.get(Calendar.HOUR_OF_DAY)
                    val eventMinute = eventCal.get(Calendar.MINUTE)
                    val topPx = ((eventHour * 60 + eventMinute) * density).toInt()
                    val heightPx = (50 * density).toInt()
                    
                    // Calculate horizontal position for side-by-side stacking
                    val columnWidthFraction = 1f / eventCount
                    val leftOffsetFraction = index * columnWidthFraction
                    
                    val eventView = createTimelineEventView(context, event)
                    
                    // Position horizontally after layout
                    eventView.post {
                        val parent = eventView.parent as? android.widget.FrameLayout ?: return@post
                        val parentWidth = parent.width - (8 * density).toInt()
                        val eventWidth = (parentWidth * columnWidthFraction).toInt()
                        val leftOffset = (parentWidth * leftOffsetFraction).toInt()
                        
                        eventView.layoutParams = android.widget.FrameLayout.LayoutParams(
                            eventWidth,
                            heightPx
                        ).apply {
                            topMargin = topPx
                            marginStart = leftOffset + (4 * density).toInt()
                        }
                    }
                    
                    // Initial layout params
                    eventView.layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        heightPx
                    ).apply {
                        topMargin = topPx
                        marginStart = (4 * density).toInt()
                        marginEnd = (4 * density).toInt()
                    }
                    
                    column.addView(eventView)
                }
            }
            
            columnsContainer.addView(column)
        }
        
        timelineBody?.addView(columnsContainer)
        
        // (#21) Auto-scroll to first event of the selected day, or current time if no events
        timelineScrollView?.post {
            val scrollPosition = calculateAutoScrollPosition(startDate, numDays, hourHeightPx)
            timelineScrollView?.scrollTo(0, scrollPosition)
        }
    }
    
    /**
     * (#21) Calculate scroll position for timeline view.
     * Priority:
     * 1. First event of the selected day(s)
     * 2. Current time if viewing today
     * 3. Start of business hours (8 AM)
     */
    private fun calculateAutoScrollPosition(startDate: Calendar, numDays: Int, hourHeightPx: Int): Int {
        // Get events for the visible date range
        // Reset startDate to midnight for correct filtering
        val rangeStart = Calendar.getInstance().apply {
            time = startDate.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rangeEnd = Calendar.getInstance().apply {
            time = startDate.time
            add(Calendar.DAY_OF_MONTH, numDays - 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        
        val visibleEvents = filteredEvents.filter { event ->
            event.dueDate >= rangeStart.time && event.dueDate <= rangeEnd.time
        }.sortedBy { it.dueDate }
        
        // If there are events, scroll to the first one
        if (visibleEvents.isNotEmpty()) {
            val firstEvent = visibleEvents.first()
            val eventCal = Calendar.getInstance().apply { time = firstEvent.dueDate }
            val eventHour = eventCal.get(Calendar.HOUR_OF_DAY)
            // Scroll to 1 hour before the event to show context
            return ((eventHour - 1).coerceAtLeast(0) * hourHeightPx)
        }
        
        // If viewing today and no events, scroll to current time
        val today = Calendar.getInstance()
        val isViewingToday = (startDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                startDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                startDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
        
        if (isViewingToday) {
            val nowHour = today.get(Calendar.HOUR_OF_DAY)
            return ((nowHour - 1).coerceAtLeast(0) * hourHeightPx)
        }
        
        // Default: scroll to 8 AM (typical business hours start)
        return (7 * hourHeightPx)
    }
    
    private fun createTimelineEventView(context: android.content.Context, event: ScheduledEvent): View {
        val density = resources.displayMetrics.density
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            
            val bgRes = when (event.type) {
                EventType.PICKUP -> R.drawable.bg_timeline_event_pickup
                EventType.DELIVERY -> R.drawable.bg_timeline_event_delivery
                EventType.PREORDER -> R.drawable.bg_timeline_event_preorder
            }
            setBackgroundResource(bgRes)
            
            setOnClickListener { viewFullOrderDetails(event.orderId) }
        }
        
        val textColor = when (event.type) {
            EventType.PICKUP -> ContextCompat.getColor(context, R.color.event_pickup)
            EventType.DELIVERY -> ContextCompat.getColor(context, R.color.event_delivery)
            EventType.PREORDER -> ContextCompat.getColor(context, R.color.event_preorder)
        }
        
        // Time - Customer name (or order number if no customer)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(event.dueDate)
        val displayName = if (event.customerName.isNotBlank() && event.customerName != "-") {
            event.customerName
        } else {
            "#${event.orderId}"
        }
        
        val eventLabel = TextView(context).apply {
            text = "$timeStr - $displayName"
            textSize = 11f
            setTextColor(textColor)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(eventLabel)
        
        return container
    }
    
    data class WeekDay(
        val dayName: String,
        val dayNumber: Int,
        val date: Date,
        val events: List<ScheduledEvent>,
        val isToday: Boolean
    )
    
    /**
     * Adapter for week view
     */
    inner class WeekDayAdapter(
        private val weekDays: List<WeekDay>,
        private val onDayClick: (WeekDay) -> Unit
    ) : RecyclerView.Adapter<WeekDayAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_week_day, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(weekDays[position])
        }
        
        override fun getItemCount(): Int = weekDays.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dayName: TextView = itemView.findViewById(R.id.dayName)
            private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)
            private val eventCount: TextView = itemView.findViewById(R.id.eventCount)
            private val eventsContainer: LinearLayout = itemView.findViewById(R.id.eventsContainer)
            
            fun bind(weekDay: WeekDay) {
                val context = itemView.context
                
                dayName.text = weekDay.dayName
                dayNumber.text = weekDay.dayNumber.toString()
                
                // Highlight today
                if (weekDay.isToday) {
                    dayNumber.setBackgroundResource(R.drawable.bg_today_circle)
                    dayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                } else {
                    dayNumber.setBackgroundResource(0)
                    dayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_light))
                }
                
                // Show events
                if (weekDay.events.isNotEmpty()) {
                    eventCount.text = "${weekDay.events.size} orders"
                    eventCount.visibility = View.VISIBLE
                    
                    eventsContainer.removeAllViews()
                    eventsContainer.visibility = View.VISIBLE
                    
                    // Show up to 3 events
                    weekDay.events.take(3).forEach { event ->
                        val eventView = createEventRow(context, event)
                        eventsContainer.addView(eventView)
                    }
                    
                    itemView.setOnClickListener { onDayClick(weekDay) }
                } else {
                    eventCount.visibility = View.GONE
                    eventsContainer.visibility = View.GONE
                    itemView.setOnClickListener(null)
                }
            }
            
            private fun createEventRow(context: android.content.Context, event: ScheduledEvent): View {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_calendar_day_event, eventsContainer, false)
                
                val colorBar = view.findViewById<View>(R.id.eventColorBar)
                val customerName = view.findViewById<TextView>(R.id.customerName)
                val eventDetails = view.findViewById<TextView>(R.id.eventDetails)
                val orderTotal = view.findViewById<TextView>(R.id.orderTotal)
                
                customerName.text = event.customerName
                eventDetails.text = "${event.type.getDisplayName()} • ${event.itemCount} items"
                orderTotal.text = "$${String.format("%.2f", event.total)}"
                
                val color = when (event.type) {
                    EventType.PICKUP -> ContextCompat.getColor(context, R.color.event_pickup)
                    EventType.DELIVERY -> ContextCompat.getColor(context, R.color.event_delivery)
                    EventType.PREORDER -> ContextCompat.getColor(context, R.color.event_preorder)
                }
                colorBar.setBackgroundColor(color)
                
                view.setOnClickListener { viewFullOrderDetails(event.orderId) }
                
                return view
            }
        }
    }
    
    /**
     * Adapter for day view (full event list)
     */
    inner class DayEventAdapter(
        private val events: List<ScheduledEvent>,
        private val onClick: (ScheduledEvent) -> Unit
    ) : RecyclerView.Adapter<DayEventAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day_event, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(events[position])
        }
        
        override fun getItemCount(): Int = events.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorBar: View = itemView.findViewById(R.id.eventColorBar)
            private val customerName: TextView = itemView.findViewById(R.id.customerName)
            private val eventDetails: TextView = itemView.findViewById(R.id.eventDetails)
            private val orderTotal: TextView = itemView.findViewById(R.id.orderTotal)
            
            fun bind(event: ScheduledEvent) {
                val context = itemView.context
                
                customerName.text = event.customerName
                eventDetails.text = "${event.type.getDisplayName()} • ${event.itemCount} items"
                orderTotal.text = "$${String.format("%.2f", event.total)}"
                
                val color = when (event.type) {
                    EventType.PICKUP -> ContextCompat.getColor(context, R.color.event_pickup)
                    EventType.DELIVERY -> ContextCompat.getColor(context, R.color.event_delivery)
                    EventType.PREORDER -> ContextCompat.getColor(context, R.color.event_preorder)
                }
                colorBar.setBackgroundColor(color)
                
                itemView.setOnClickListener { onClick(event) }
            }
        }
    }
    
    /**
     * Show dialog with all events for a selected day
     */
    private fun showDayEventsDialog(day: CalendarDay) {
        val dayDate = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_MONTH, day.dayNumber)
        }.time
        
        val dialog = EventPreviewDialog.newInstance(day.events, dayDate)
        dialog.setOnEventClickListener { event ->
            viewFullOrderDetails(event.orderId)
        }
        dialog.show(childFragmentManager, EventPreviewDialog.TAG)
    }
    
    private fun generateCalendarDays(year: Int, month: Int, events: List<ScheduledEvent>): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        // Get today's date for comparison
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        
        // Get the day of week for the first day of month
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Add empty days for padding
        for (i in 0 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, emptyList(), isToday = false, isOtherMonth = true))
        }
        
        // Add days of month
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayEvents = events.filter { event ->
                val eventCal = Calendar.getInstance()
                eventCal.time = event.dueDate
                eventCal.get(Calendar.DAY_OF_MONTH) == day &&
                eventCal.get(Calendar.MONTH) == month &&
                eventCal.get(Calendar.YEAR) == year
            }
            
            val isToday = (year == todayYear && month == todayMonth && day == todayDay)
            
            days.add(CalendarDay(
                dayNumber = day,
                hasEvents = dayEvents.isNotEmpty(),
                events = dayEvents,
                isToday = isToday,
                isOtherMonth = false
            ))
        }
        
        return days
    }
    
    /**
     * (#23) Navigate calendar based on current view mode.
     * - Day view: +/- 1 day
     * - Week view: +/- 1 week
     * - Month view: +/- 1 month
     */
    fun navigateMonth(direction: Int) {
        when (currentViewMode) {
            "day" -> currentDate.add(Calendar.DAY_OF_MONTH, direction)
            "week" -> currentDate.add(Calendar.WEEK_OF_YEAR, direction)
            "month" -> currentDate.add(Calendar.MONTH, direction)
            else -> currentDate.add(Calendar.MONTH, direction)
        }
        sharedFilterViewModel.setSelectedDate(currentDate.time)
        renderCalendar()
    }
    
    fun goToToday() {
        currentDate = Calendar.getInstance()
        sharedFilterViewModel.setSelectedDate(currentDate.time)
        renderCalendar()
    }
    
    fun goToNextFulfillment() {
        // Find next event from filtered events
        val now = Date()
        val nextEvent = filteredEvents
            .filter { it.dueDate.after(now) }
            .minByOrNull { it.dueDate }
        
        nextEvent?.let { event ->
            currentDate.time = event.dueDate
            sharedFilterViewModel.setSelectedDate(currentDate.time)
            renderCalendar()
            
            // Show single event in dialog
            val dialog = EventPreviewDialog.newInstance(listOf(event), event.dueDate)
            dialog.setOnEventClickListener { e ->
                viewFullOrderDetails(e.orderId)
            }
            dialog.show(childFragmentManager, EventPreviewDialog.TAG)
        } ?: run {
            showNoUpcomingEventsMessage()
        }
    }
    
    fun viewFullOrderDetails(orderId: String) {
        // Save current state before navigating
        saveCurrentStateToViewModel()
        
        // Navigate to order details using nav controller
        try {
            val order = allOrders.find { it?.id == orderId }
            if (order != null) {
                OrderListRedesignFragment.userClickedData = order
                
                val navController = androidx.navigation.Navigation.findNavController(requireView())
                val bundle = Bundle().apply {
                    putParcelable("orderData", order)
                }
                navController.navigate(R.id.action_calendarFragment_to_orderDetailFragment, bundle)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open order details", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Save all current state to ViewModel before navigation
     */
    private fun saveCurrentStateToViewModel() {
        sharedFilterViewModel.setSelectedDate(currentDate.time)
        sharedFilterViewModel.setCalendarViewMode(currentViewMode)
        sharedFilterViewModel.setFilterState(currentFilterState)
        sharedFilterViewModel.setSearchedDates(searchedDates)
    }
    
    private fun showNoUpcomingEventsMessage() {
        Toast.makeText(
            requireContext(),
            "No upcoming fulfillment dates scheduled.",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    data class CalendarDay(
        val dayNumber: Int,
        val hasEvents: Boolean,
        val events: List<ScheduledEvent>,
        val isToday: Boolean = false,
        val isOtherMonth: Boolean = false
    )
    
    inner class CalendarDayAdapter(
        private val days: List<CalendarDay>,
        private val onDayClick: (CalendarDay) -> Unit
    ) : RecyclerView.Adapter<CalendarDayAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(days[position])
        }
        
        override fun getItemCount(): Int = days.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)
            private val dayContainer: View = itemView.findViewById(R.id.dayContainer)
            private val eventsContainer: View = itemView.findViewById(R.id.eventsContainer)
            private val event1: TextView = itemView.findViewById(R.id.event1)
            private val event2: TextView = itemView.findViewById(R.id.event2)
            private val moreEvents: TextView = itemView.findViewById(R.id.moreEvents)
            private val eventIndicator: View = itemView.findViewById(R.id.eventIndicator)
            
            fun bind(day: CalendarDay) {
                val context = itemView.context
                
                if (day.dayNumber == 0) {
                    // Empty cell (padding)
                    dayNumber.text = ""
                    eventsContainer.visibility = View.GONE
                    eventIndicator.visibility = View.GONE
                    dayContainer.setOnClickListener(null)
                    dayContainer.alpha = 0f
                    return
                }
                
                dayContainer.alpha = if (day.isOtherMonth) 0.4f else 1f
                dayNumber.text = day.dayNumber.toString()
                
                // Today highlight
                if (day.isToday) {
                    dayNumber.setBackgroundResource(R.drawable.bg_today_circle)
                    dayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                } else {
                    dayNumber.setBackgroundResource(0)
                    dayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_light))
                }
                
                // Events
                if (day.hasEvents && day.events.isNotEmpty()) {
                    eventsContainer.visibility = View.VISIBLE
                    eventIndicator.visibility = View.GONE
                    
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    
                    // Show first event with time: "9:30 AM - John Smith"
                    val firstEvent = day.events[0]
                    val time1 = timeFormat.format(firstEvent.dueDate)
                    event1.text = "$time1 - ${firstEvent.customerName}"
                    event1.visibility = View.VISIBLE
                    setEventStyle(event1, firstEvent.type)
                    
                    // Show second event if exists
                    if (day.events.size > 1) {
                        val secondEvent = day.events[1]
                        val time2 = timeFormat.format(secondEvent.dueDate)
                        event2.text = "$time2 - ${secondEvent.customerName}"
                        event2.visibility = View.VISIBLE
                        setEventStyle(event2, secondEvent.type)
                    } else {
                        event2.visibility = View.GONE
                    }
                    
                    // Show more indicator if > 2 events
                    if (day.events.size > 2) {
                        moreEvents.text = "+${day.events.size - 2} more"
                        moreEvents.visibility = View.VISIBLE
                    } else {
                        moreEvents.visibility = View.GONE
                    }
                    
                    dayContainer.setOnClickListener { onDayClick(day) }
                } else {
                    eventsContainer.visibility = View.GONE
                    event1.visibility = View.GONE
                    event2.visibility = View.GONE
                    moreEvents.visibility = View.GONE
                    eventIndicator.visibility = View.GONE
                    dayContainer.setOnClickListener(null)
                }
            }
            
            private fun setEventStyle(textView: TextView, eventType: com.orderMate.model.EventType) {
                val context = textView.context
                when (eventType) {
                    com.orderMate.model.EventType.PICKUP -> {
                        textView.setBackgroundResource(R.drawable.bg_calendar_event_pickup)
                        textView.setTextColor(ContextCompat.getColor(context, R.color.event_pickup))
                    }
                    com.orderMate.model.EventType.DELIVERY -> {
                        textView.setBackgroundResource(R.drawable.bg_calendar_event_delivery)
                        textView.setTextColor(ContextCompat.getColor(context, R.color.event_delivery))
                    }
                    com.orderMate.model.EventType.PREORDER -> {
                        textView.setBackgroundResource(R.drawable.bg_calendar_event_preorder)
                        textView.setTextColor(ContextCompat.getColor(context, R.color.event_preorder))
                    }
                }
            }
        }
    }
    
    companion object {
        const val TAG = "CalendarFragment"
        
        fun newInstance(): CalendarFragment {
            return CalendarFragment()
        }
    }
}
