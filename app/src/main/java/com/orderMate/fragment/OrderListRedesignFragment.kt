package com.orderMate.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.google.gson.Gson
import com.orderMate.R
import com.orderMate.adapters.OrderCardRedesignAdapter
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.FragmentOrderListRedesignBinding
import com.orderMate.utils.Constants
import com.orderMate.utils.FilterCategories
import com.orderMate.utils.FilterCategoryBuilder
import com.orderMate.utils.ModalDialogCategories
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderSearchFilter
import com.orderMate.utils.MyApp.Companion.filterArray
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.OrderDueDateResolver
import com.orderMate.utils.OrderFilterUtils
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.hideView
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.SettingsManager
import com.orderMate.utils.showView
import com.orderMate.utils.formatPaymentState
import com.orderMate.utils.formatPaymentStateTitleCase
import com.orderMate.utils.formatOrderState
import com.orderMate.utils.formatOrderStateTitleCase
import com.orderMate.viewmodel.SharedFilterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * iOS-style Order List Fragment (#80, #81 requirement)
 * 
 * Redesigned list view with:
 * - Glassmorphic order cards
 * - Modern search with date picker
 * - Filter pills instead of spinners
 * - Sync button with animation
 */
class OrderListRedesignFragment : Fragment(), IOrderItemClickListener {

    companion object {
        private const val TAG = "ListFragment_DEBUG"
        private var instance: OrderListRedesignFragment? = null
        
        fun getInstance(): OrderListRedesignFragment {
            return instance ?: synchronized(this) {
                OrderListRedesignFragment().also { instance = it }
            }
        }
        
        val notesFilter: HashMap<String, MutableList<String>> = hashMapOf()
        var userSelectedPosition: Int = 0
        var userClickedData: Order? = null
    }

    private var _binding: FragmentOrderListRedesignBinding? = null
    private val binding get() = _binding!!
    
    // Debug: Track fragment instance ID for logging
    private val fragmentId = System.identityHashCode(this).toString(16)

    // Filter data
    private var orderPaymentStatusType: MutableSet<String> = mutableSetOf(Constants.all_orders)
    private var orderEmployeeType: MutableSet<String> = mutableSetOf(Constants.all_employee)
    private var orderTenderType: MutableSet<String> = mutableSetOf(Constants.all_tenders)
    private var orderBookingType: MutableSet<String> = mutableSetOf(Constants.all_booking_type, Constants.pos, Constants.online)

    // Order lists
    private var orderItems: ArrayList<Order?> = ArrayList()
    private var filterData: ArrayList<Order?> = ArrayList()
    private var allItemList: ArrayList<Order?> = ArrayList()

    // Selected date filters (#12 - separate order date and due date)
    private var selectedDateFilter: Date? = null
    private var selectedOrderDate: Date? = null
    private var selectedDueDate: Date? = null

    private val preferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val myApp by lazy {
        MyApp.getInstance()
    }
    
    // Shared ViewModel for filter/search state persistence across tabs
    private val sharedFilterViewModel: SharedFilterViewModel by activityViewModels()

    private var orderAdapter: OrderCardRedesignAdapter? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchRunnable: Runnable

    private var isSyncing = false

    // Current filter state for dialog (synced with shared ViewModel)
    private var currentFilterState = FilterDialogFragment.FilterState()
    private var currentSearchQuery = ""
    
    private fun logDebug(message: String) {
        Log.d(TAG, "[$fragmentId] $message | isAdded=$isAdded, binding=${_binding != null}, view=${view != null}")
    }
    
    private fun logFilterState(prefix: String) {
        val dateCount = currentFilterState.dateSelections.values.sumOf { it.size }
        val selectionCount = currentFilterState.selections.values.sumOf { it.size }
        Log.d(TAG, "[$fragmentId] $prefix | dates=$dateCount, selections=$selectionCount, search='$currentSearchQuery'")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[$fragmentId] onCreate")
        instance = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "[$fragmentId] onCreateView - inflating binding")
        _binding = FragmentOrderListRedesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "[$fragmentId] onViewCreated - setting up UI")
        initializeRecyclerView()
        setupClickListeners()
        setupSearchListener()
        observeSharedState()
        
        // Sync widgets from Firebase and refresh adapter to ensure pills display on first load
        WidgetManager.getInstance(requireContext()).reloadAll {
            Log.d(TAG, "[$fragmentId] WidgetManager.reloadAll callback - isAdded=$isAdded")
            activity?.runOnUiThread { orderAdapter?.notifyDataSetChanged() }
        }
    }
    
    /**
     * Observe shared ViewModel for filter/search state changes
     * Restores state when navigating back to this tab
     */
    private fun observeSharedState() {
        Log.d(TAG, "[$fragmentId] observeSharedState - setting up observers")
        
        // Observe filter state - update pills and apply if orders loaded
        sharedFilterViewModel.filterState.observe(viewLifecycleOwner) { state ->
            val dateCount = state.dateSelections.values.sumOf { it.size }
            Log.d(TAG, "[$fragmentId] OBSERVER filterState fired | dateCount=$dateCount, isAdded=$isAdded, binding=${_binding != null}")
            
            // Skip if state hasn't changed (prevents flash on back navigation)
            if (state == currentFilterState) {
                Log.d(TAG, "[$fragmentId] filterState unchanged, skipping")
                return@observe
            }
            currentFilterState = state
            logFilterState("filterState updated")
            
            // Always update pills for visual consistency
            if (_binding != null) {
                Log.d(TAG, "[$fragmentId] Updating filter pills")
                updateFilterPills(state)
            } else {
                Log.w(TAG, "[$fragmentId] WARNING: _binding is null in filterState observer!")
            }
            // Only apply filters if orders are loaded
            if (allItemList.isNotEmpty()) {
                Log.d(TAG, "[$fragmentId] Applying dialog filters (orders loaded: ${allItemList.size})")
                applyDialogFilters(state)
            }
        }
        
        // Observe search query
        sharedFilterViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            Log.d(TAG, "[$fragmentId] OBSERVER searchQuery fired | query='$query', isAdded=$isAdded, binding=${_binding != null}")
            
            if (query != currentSearchQuery) {
                currentSearchQuery = query
                // Update search input without triggering listener
                if (_binding != null) {
                    binding.header.searchInput.apply {
                        if (text.toString() != query) {
                            setText(query)
                            setSelection(query.length)
                        }
                    }
                    // Update search pill
                    updateSearchPill(query)
                } else {
                    Log.w(TAG, "[$fragmentId] WARNING: _binding is null in searchQuery observer!")
                }
            }
        }
        
        // Observe refresh trigger (e.g., after order deletion)
        sharedFilterViewModel.refreshTrigger.observe(viewLifecycleOwner) { trigger ->
            Log.d(TAG, "[$fragmentId] OBSERVER refreshTrigger fired | trigger=$trigger")
            // Refresh order list when triggered
            if (allItemList.isNotEmpty() || orderItems.isNotEmpty()) {
                loadOrders()
            }
        }
    }
    
    /**
     * Update search pill visibility and text based on search query
     */
    private fun updateSearchPill(query: String) {
        Log.d(TAG, "[$fragmentId] updateSearchPill | query='$query', binding=${_binding != null}")
        if (query.isNotBlank()) {
            binding.header.searchPillContainer.showView()
            binding.header.searchPillText.text = query
        } else {
            binding.header.searchPillContainer.hideView()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "[$fragmentId] onStart")
        if (orderItems.isEmpty()) {
            loadOrders()
        }
    }

    /**
     * Called from filter button or pull-to-refresh to sync orders
     */
    fun triggerSync() {
        syncOrders()
    }

    override fun onDestroyView() {
        Log.d(TAG, "[$fragmentId] onDestroyView - nullifying binding")
        logFilterState("onDestroyView state")
        handler.removeCallbacksAndMessages(null) // Cancel pending search runnables
        super.onDestroyView()
        _binding = null
    }
    
    override fun onDestroy() {
        Log.d(TAG, "[$fragmentId] onDestroy")
        super.onDestroy()
    }

    // ==================== Initialization ====================

    private fun initializeRecyclerView() {
        binding.ordersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // Reuse existing adapter to avoid rebinding all items on back navigation
            if (orderAdapter == null) {
                orderAdapter = OrderCardRedesignAdapter(orderItems, this@OrderListRedesignFragment)
                adapter = orderAdapter
            } else if (adapter !== orderAdapter) {
                adapter = orderAdapter
            }
        }
    }

    private fun setupClickListeners() {
        // Filter button
        binding.header.filterButton.setOnClickListener {
            showFilterDialog()
        }

        // Reset button
        binding.header.resetButton.setOnClickListener {
            resetFilters()
        }

        // Calendar icon for date picker (#14 - expanded click area)
        binding.header.calendarIconContainer.setOnClickListener {
            showDatePicker()
        }

        // Sync button (#15)
        binding.header.syncButton.setOnClickListener {
            if (!isSyncing) {
                syncOrders()
            }
        }

        // Date pills removed - date filtering now handled through Filter dialog
    }

    private fun resetFilters() {
        // Clear all filters
        filterArray.keys.forEach { filterArray[it] = 0 }
        selectedDateFilter = null
        currentFilterState = FilterDialogFragment.FilterState()
        currentSearchQuery = ""
        
        // Clear date selections
        selectedOrderDate = null
        selectedDueDate = null
        
        // Sync reset to shared ViewModel for cross-tab persistence
        sharedFilterViewModel.resetAll()
        
        binding.header.searchInput.text?.clear()
        binding.header.searchInput.hint = getString(R.string.search_orders)
        
        // Hide pills
        binding.header.filterPillsScroll.visibility = View.GONE
        binding.header.filterPillsContainer.removeAllViews()
        
        // Reload all orders
        orderItems.clear()
        allItemList.forEach { orderItems.add(it) }
        filterData.clear()
        
        updateResultsInfo()
        notifyAdapter()
        updateEmptyState()
        
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun setupSearchListener() {
        binding.header.searchInput.doAfterTextChanged { text ->
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
        binding.header.searchPillClose.setOnClickListener {
            binding.header.searchInput.text?.clear()
            currentSearchQuery = ""
            sharedFilterViewModel.setSearchQuery("")
            updateSearchPill("")
            searchOrders("")
        }
    }

    // ==================== Data Loading ====================

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "[$fragmentId] loadOrders START")
        showLoading(true)
        
        runOnBackgroundThread {
            Log.d(TAG, "[$fragmentId] loadOrders BACKGROUND THREAD START | isAdded=$isAdded")
            // Fetch data on background thread into temporary list
            val tempOrders = ArrayList<Order?>()
            var fetchError: Exception? = null
            
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderData?.forEach {
                    tempOrders.add(it)
                }
                Log.d(TAG, "[$fragmentId] loadOrders fetched ${tempOrders.size} orders")
            } catch (e: Exception) {
                e.printStackTrace()
                fetchError = e
                Log.e(TAG, "[$fragmentId] loadOrders ERROR: ${e.message}")
            }

            runOnMainThread {
                Log.d(TAG, "[$fragmentId] loadOrders MAIN THREAD CALLBACK | isAdded=$isAdded, binding=${_binding != null}")
                
                // Safety check before accessing fragment state
                if (!isAdded || _binding == null) {
                    Log.w(TAG, "[$fragmentId] loadOrders MAIN THREAD - Fragment not attached or binding null, aborting!")
                    return@runOnMainThread
                }
                
                // All list modifications on main thread to avoid RecyclerView inconsistency
                if (fetchError != null) {
                    debugSnackBar(getString(R.string.there_is_issue_with_your_account))
                } else {
                    orderItems.clear()
                    allItemList.clear()
                    allItemList.addAll(tempOrders)
                    orderItems.addAll(tempOrders)
                }
                
                showLoading(false)
                
                // Apply any pending shared state after orders are loaded
                val sharedState = sharedFilterViewModel.filterState.value
                val dateCount = sharedState?.dateSelections?.values?.sumOf { it.size } ?: 0
                Log.d(TAG, "[$fragmentId] loadOrders checking shared state | hasFilters=${sharedState?.hasActiveFilters()}, dateCount=$dateCount")
                
                if (sharedState != null && sharedState.hasActiveFilters()) {
                    currentFilterState = sharedState
                    Log.d(TAG, "[$fragmentId] loadOrders applying dialog filters")
                    applyDialogFilters(sharedState)
                } else if (currentSearchQuery.isNotEmpty()) {
                    // No dialog filters but search is active - apply search filter
                    Log.d(TAG, "[$fragmentId] loadOrders applying search: '$currentSearchQuery'")
                    searchOrders(currentSearchQuery)
                } else {
                    updateResultsInfo()
                    orderAdapter?.notifyDataSetChanged()
                    updateEmptyState()
                }
                
                onComplete?.invoke()
            }

            // Update filter options
            CoroutineScope(Dispatchers.Default).launch {
                updateFilterOptions()
            }
        }
    }

    private fun syncOrders() {
        // Change sync icon to orange and blink
        binding.header.syncIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.accent_orange)
        )
        binding.header.syncIcon.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.blink_sync)
        )
        binding.header.syncButton.isEnabled = false
        
        loadOrders {
            // Stop animation and restore icon color
            binding.header.syncIcon.clearAnimation()
            binding.header.syncIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.text_light)
            )
            binding.header.syncButton.isEnabled = true
            Toast.makeText(requireContext(), "Orders synced", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== Search ====================

    private fun searchOrders(query: String?) {
        Log.d(TAG, "[$fragmentId] searchOrders START | query='$query', isAdded=$isAdded")
        logFilterState("searchOrders filter state")
        
        runOnBackgroundThread(Dispatchers.Default) {
            Log.d(TAG, "[$fragmentId] searchOrders BACKGROUND THREAD | isAdded=$isAdded")
            
            // Safety check - get context safely
            val ctx = context
            if (ctx == null) {
                Log.e(TAG, "[$fragmentId] searchOrders BACKGROUND - context is NULL, aborting!")
                return@runOnBackgroundThread
            }
            
            // Use shared filter+search function for consistent behavior with Calendar page
            // Filter into temporary list on background thread
            val tempResults = ArrayList<Order?>()
            try {
                tempResults.addAll(OrderFilterUtils.filterAndSearchOrders(
                    allItemList, currentFilterState, query ?: "", ctx
                ))
                Log.d(TAG, "[$fragmentId] searchOrders filtered to ${tempResults.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "[$fragmentId] searchOrders ERROR during filter: ${e.message}", e)
            }

            // If no results and query looks like order ID, try direct lookup
            if (tempResults.isEmpty() && !query.isNullOrEmpty() && query.length > 7) {
                exceptionHandler {
                    val result = myApp.getOrderConnector().getOrder(query)
                    if (result != null) {
                        tempResults.add(result)
                    }
                }
            }

            runOnMainThread {
                Log.d(TAG, "[$fragmentId] searchOrders MAIN THREAD CALLBACK | isAdded=$isAdded, binding=${_binding != null}")
                
                // Safety check before modifying UI
                if (!isAdded || _binding == null) {
                    Log.w(TAG, "[$fragmentId] searchOrders MAIN THREAD - Fragment not attached or binding null, aborting!")
                    return@runOnMainThread
                }
                
                // All list modifications on main thread to avoid RecyclerView inconsistency
                orderItems.clear()
                orderItems.addAll(tempResults)
                
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }
    // ==================== Date Filter ====================

    private fun showDatePicker() {
        // Use last selected date or current date
        val existingDates = currentFilterState.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE]
        val initialDate = existingDates?.lastOrNull()

        DateTimePickerDialog.newInstance(
            widgetLabel = "Select Date",
            initialDateTime = initialDate
        ).apply {
            setListener(object : DateTimePickerDialog.OnDateTimeSelectedListener {
                override fun onDateTimeSelected(dateTime: java.util.Date, formattedDateTime: String) {
                    // Add date to filter state (supports multiple dates like HTML)
                    addDateToFilterState(dateTime)
                    
                    // Sync filter state to shared ViewModel (so Calendar page sees it too)
                    sharedFilterViewModel.setFilterState(currentFilterState)
                    
                    // Apply filters and update pills
                    applyDialogFilters(currentFilterState)
                }
            })
        }.show(childFragmentManager, "dateTimePicker")
    }

    // ==================== Separate Date Pills (#12) ====================

    // Date pill methods removed - date filtering now handled through Filter dialog
    // The applyDateFilters method is still used when filter dialog selections change

    /**
     * Apply date filters for Order Date and Due Date (#12)
     * Filters are independent - each filters orders that match that date
     */
    private fun applyDateFilters() {
        runOnBackgroundThread {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            
            // Filter into temporary list on background thread
            val tempResults = ArrayList<Order?>()
            val sourceList = if (isFilterActive()) filterData else allItemList

            for (order in sourceList) {
                var matchesOrderDate = true
                var matchesDueDate = true

                // Filter by Order Date (createdTime)
                if (selectedOrderDate != null) {
                    val orderCreatedDate = order?.createdTime?.let { dateFormat.format(Date(it)) }
                    val targetOrderDate = dateFormat.format(selectedOrderDate!!)
                    matchesOrderDate = orderCreatedDate == targetOrderDate
                }

                // (#12) Filter by Due Date using three-priority logic:
                // P1: Order-level CALENDAR from order.note
                // P2: Item-level CALENDAR from lineItem.note (earliest)
                // P3: order.createdTime
                if (selectedDueDate != null && order != null) {
                    val widgets = WidgetManager.getCachedWidgets()
                    val orderDueDate = if (widgets.isNotEmpty()) {
                        OrderDueDateResolver.resolveDueDate(order, widgets)
                    } else {
                        OrderDueDateResolver.resolveDueDate(order)
                    }
                    
                    matchesDueDate = if (orderDueDate != null) {
                        // Compare dates (day only)
                        val targetDateStr = dateFormat.format(selectedDueDate!!)
                        val orderDateStr = dateFormat.format(orderDueDate)
                        targetDateStr == orderDateStr
                    } else {
                        false
                    }
                }

                if (matchesOrderDate && matchesDueDate) {
                    tempResults.add(order)
                }
            }

            runOnMainThread {
                // All list modifications on main thread to avoid RecyclerView inconsistency
                orderItems.clear()
                orderItems.addAll(tempResults)
                
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
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

            orderItems.clear()
            val sourceList = if (isFilterActive()) filterData else allItemList

            for (order in sourceList) {
                val orderDate = order?.createdTime?.let {
                    dateFormat.format(Date(it))
                }
                if (orderDate == targetDate) {
                    orderItems.add(order)
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun updateDateFilterUI() {
        // Deprecated - now using pills instead
        selectedDateFilter?.let {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            binding.header.searchInput.hint = "Filtering: ${dateFormat.format(it)}"
        }
    }

    // ==================== Filters ====================

    private fun showFilterDialog() {
        // Build dynamic filter categories from Clover orders + OrderMate widgets
        val filterableWidgets = WidgetManager.getInstance(requireContext()).getFilterableWidgets() ?: emptyList()
        val settingsManager = SettingsManager(requireContext())
        val categories = FilterCategoryBuilder.buildCategories(allItemList, filterableWidgets, settingsManager)
        
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
        val dateCount = filters.dateSelections.values.sumOf { it.size }
        val selectionCount = filters.selections.values.sumOf { it.size }
        Log.d(TAG, "[$fragmentId] applyDialogFilters START | dates=$dateCount, selections=$selectionCount, isAdded=$isAdded")
        
        runOnBackgroundThread {
            Log.d(TAG, "[$fragmentId] applyDialogFilters BACKGROUND THREAD | isAdded=$isAdded")
            
            // Safety check - get context safely
            val ctx = context
            if (ctx == null) {
                Log.e(TAG, "[$fragmentId] applyDialogFilters BACKGROUND - context is NULL, aborting!")
                return@runOnBackgroundThread
            }
            
            // Use shared filter+search function for consistent behavior with Calendar page
            try {
                val results = OrderFilterUtils.filterAndSearchOrders(
                    allItemList, filters, currentSearchQuery, ctx
                )
                orderItems.clear()
                orderItems.addAll(results)
                Log.d(TAG, "[$fragmentId] applyDialogFilters filtered to ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "[$fragmentId] applyDialogFilters ERROR: ${e.message}", e)
            }

            runOnMainThread {
                Log.d(TAG, "[$fragmentId] applyDialogFilters MAIN THREAD CALLBACK | isAdded=$isAdded, binding=${_binding != null}")
                
                // Safety check before modifying UI
                if (!isAdded || _binding == null) {
                    Log.w(TAG, "[$fragmentId] applyDialogFilters MAIN THREAD - Fragment not attached or binding null, aborting!")
                    return@runOnMainThread
                }
                
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
                updateFilterPills(filters)
            }
        }
    }

    // Note: Filter matching logic moved to shared OrderFilterUtils.orderMatchesFilters()

    private fun updateFilterPills(filters: FilterDialogFragment.FilterState) {
        binding.header.filterPillsContainer.removeAllViews()

        val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        var hasPills = false
        
        // Add selection filters as pills
        // (#81 QA) Use title case for Clover status pills in header
        filters.selections.forEach { (categoryId, values) ->
            values.forEach { value ->
                hasPills = true
                val displayValue = when (categoryId) {
                    FilterCategoryBuilder.CLOVER_PAYMENT_STATUS -> formatPaymentStateTitleCase(value)
                    FilterCategoryBuilder.CLOVER_ORDER_STATUS -> formatOrderStateTitleCase(value)
                    else -> value
                }
                val pill = createFilterPillWithClose(displayValue) {
                    removeSelectionFilter(categoryId, value)
                }
                binding.header.filterPillsContainer.addView(pill)
            }
        }
        
        // Add date filters as pills
        filters.dateSelections.forEach { (categoryId, dates) ->
            val label = when {
                categoryId == FilterCategoryBuilder.CLOVER_ORDER_DATE -> "Order Date"
                FilterCategoryBuilder.isWidgetFilter(categoryId) -> {
                    val widgetId = FilterCategoryBuilder.getWidgetId(categoryId)
                    WidgetManager.getInstance(requireContext()).getWidgetById(widgetId ?: "")?.label ?: "Date"
                }
                else -> "Date"
            }
            dates.forEachIndexed { index, date ->
                hasPills = true
                val pill = createFilterPillWithClose("$label: ${dateFormat.format(date)}") {
                    removeDateFilter(categoryId, index)
                }
                binding.header.filterPillsContainer.addView(pill)
            }
        }

        binding.header.filterPillsScroll.visibility = if (hasPills) View.VISIBLE else View.GONE
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
        return android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_filter_pill)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6))
            
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dpToPx(8)
            lp.bottomMargin = dpToPx(4)
            layoutParams = lp
            
            // Text label
            val textView = android.widget.TextView(context).apply {
                this.text = text
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
            }
            addView(textView)
            
            // Close button
            val closeBtn = android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                val size = dpToPx(16)
                val closeLp = android.widget.LinearLayout.LayoutParams(size, size)
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

    private fun isFilterActive(): Boolean {
        // Check new filter state (from FilterDialogFragment)
        if (currentFilterState.hasActiveFilters()) return true
        // Also check legacy filterArray for backwards compatibility
        filterArray.forEach {
            if (it.value != 0) return true
        }
        return false
    }

    private fun updateFilterOptions() {
        orderPaymentStatusType.clear()
        orderPaymentStatusType.add(Constants.all_orders)
        
        orderEmployeeType.clear()
        orderEmployeeType.add(Constants.all_employee)
        
        orderTenderType.clear()
        orderTenderType.add(Constants.all_tenders)

        orderItems.forEach { order ->
            order?.paymentState?.name?.let { orderPaymentStatusType.add(it) }
            
            order?.payments?.forEach { payment ->
                orderTenderType.add(payment.tender.label)
            }
            
            val employeeName = try {
                order?.employee?.jsonObject?.get(Constants.name)?.toString()
            } catch (e: Exception) { null }
            
            employeeName?.let { orderEmployeeType.add(it) }
        }

        // Load custom notes filters from V2 WidgetManager
        loadNotesFiltersV2()
    }

    /**
     * Load notes filters from V2 WidgetManager (filterableWidgets)
     */
    private fun loadNotesFiltersV2() {
        notesFilter.clear()
        notesFilter[getString(R.string.all_orders)] = mutableListOf()
        
        val filterableWidgets = WidgetManager.getInstance(requireContext()).getFilterableWidgets() ?: emptyList()
        filterableWidgets.forEach { widget ->
            if (widget.options.isNotEmpty()) {
                val options = mutableListOf<String>()
                options.add(getString(R.string.all_orders))
                widget.options.forEach { option -> options.add(option.label) }
                notesFilter[widget.label.trim()] = options
            }
        }
    }

    fun applyFilters(matchArray: HashMap<String, String>) {
        runOnBackgroundThread {
            orderItems.clear()
            filterData.clear()

            allItemList.forEach { order ->
                val employeeName = try {
                    order?.employee?.jsonObject?.get(Constants.name).toString()
                } catch (e: Exception) { null }
                
                val booking = if (order?.isNotNullOnlineOrder == true) Constants.online else Constants.pos
                val paymentMode = getPaymentMode(order?.payments)

                val matchesPayment = filterArray[FilterCategories.PaymentStatus.name] == 0 ||
                        matchArray.values.contains(order?.paymentState?.name)
                val matchesEmployee = filterArray[FilterCategories.EmployeeName.name] == 0 ||
                        matchArray.values.contains(employeeName)
                val matchesBooking = filterArray[FilterCategories.OrderBookingType.name] == 0 ||
                        matchArray.values.contains(booking)
                val matchesTender = filterArray[FilterCategories.TenderType.name] == 0 ||
                        paymentMode.contains(matchArray.values.firstOrNull() ?: "")
                val matchesNotes = isNoteDataMatched(matchArray, order?.lineItems)

                if (matchesPayment && matchesEmployee && matchesBooking && matchesTender && matchesNotes) {
                    orderItems.add(order)
                    filterData.add(order)
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun getPaymentMode(payments: List<Payment>?): String {
        return payments?.joinToString(", ") { it.tender?.label ?: "" } ?: ""
    }

    private fun isNoteDataMatched(matchArray: HashMap<String, String>, lineItems: MutableList<LineItem?>?): Boolean {
        // If no note filters applied, match all
        if (filterArray[ModalDialogCategories.OrderCategories.name] == 0 &&
            filterArray[ModalDialogCategories.OrderSubCategories.name] == 0 &&
            filterArray[ModalDialogCategories.OrderType.name] == 0 &&
            filterArray[ModalDialogCategories.OrderProgress.name] == 0) {
            return true
        }

        val category = matchArray[ModalDialogCategories.OrderCategories.name]?.trim() ?: ""
        val subCategory = matchArray[ModalDialogCategories.OrderSubCategories.name]?.trim() ?: ""
        val type = matchArray[ModalDialogCategories.OrderType.name]?.trim() ?: ""
        val status = matchArray[ModalDialogCategories.OrderProgress.name]?.trim() ?: ""

        lineItems?.forEach { item ->
            val note = item?.note ?: return@forEach
            if ((category.isNotEmpty() && note.contains(category, true)) ||
                (subCategory.isNotEmpty() && note.contains(subCategory, true)) ||
                (type.isNotEmpty() && note.contains(type, true)) ||
                (status.isNotEmpty() && note.contains(status, true))) {
                return true
            }
        }

        return false
    }

    // ==================== UI Updates ====================

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.isVisible = show
        binding.ordersRecycler.isVisible = !show && orderItems.isNotEmpty()
        binding.emptyState.isVisible = !show && orderItems.isEmpty()
    }

    private fun updateEmptyState() {
        binding.emptyState.isVisible = orderItems.isEmpty() && !binding.loadingContainer.isVisible
        binding.ordersRecycler.isVisible = orderItems.isNotEmpty()
    }

    private fun updateResultsInfo() {
        val count = orderItems.size
        val total = orderItems.sumOf { (it?.total ?: 0L) } / 100.0
        val totalFormatted = String.format("$%.2f", total)
        
        val text = when {
            count == 0 -> "No orders found"
            count == 1 -> "Showing 1 order • Total: $totalFormatted"
            else -> "Showing $count orders • Total: $totalFormatted"
        }
        
        // Apply bold styling to numbers
        val spannable = android.text.SpannableString(text)
        val countStr = count.toString()
        val countStart = text.indexOf(countStr)
        if (countStart >= 0) {
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                countStart,
                countStart + countStr.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_light)
                ),
                countStart,
                countStart + countStr.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val totalStart = text.indexOf(totalFormatted)
        if (totalStart >= 0) {
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                totalStart,
                totalStart + totalFormatted.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_light)
                ),
                totalStart,
                totalStart + totalFormatted.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        binding.resultsInfo.text = spannable
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyAdapter() {
        orderAdapter?.notifyDataSetChanged()
    }

    // ==================== Order Click ====================

    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        if (orderPosition >= orderItems.size) return
        
        userSelectedPosition = orderPosition
        userClickedData = orderItems[orderPosition]
        
        val order = orderItems[orderPosition] ?: return
        
        // Navigate to order detail using nav controller
        try {
            val navController = androidx.navigation.Navigation.findNavController(requireView())
            val bundle = Bundle().apply {
                putParcelable("orderData", order)
            }
            navController.navigate(R.id.action_orderListRedesignFragment_to_orderDetailFragment, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== Public Methods ====================

    @SuppressLint("NotifyDataSetChanged")
    fun updateOrder(orderData: Order?) {
        runOnBackgroundThread {
            orderItems.forEachIndexed { index, order ->
                if (order?.id == orderData?.id) {
                    orderItems[index] = orderData
                    runOnMainThread {
                        orderAdapter?.notifyItemChanged(index)
                    }
                    return@runOnBackgroundThread
                }
            }
        }
    }

    fun refreshOrders() {
        loadOrders()
    }
}
