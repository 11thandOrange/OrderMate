package com.orderMate.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.hideView
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
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
 * 
 * Reuses backend logic from OrderHistoryFragment.
 */
class OrderListRedesignFragment : Fragment(), IOrderItemClickListener {

    companion object {
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

    // Filter data
    private var orderPaymentStatusType: MutableSet<String> = mutableSetOf(Constants.all_orders)
    private var orderEmployeeType: MutableSet<String> = mutableSetOf(Constants.all_employee)
    private var orderTenderType: MutableSet<String> = mutableSetOf(Constants.all_tenders)
    private var orderBookingType: MutableSet<String> = mutableSetOf(Constants.all_booking_type, Constants.pos, Constants.online)

    // Order lists
    private var orderItems: ArrayList<Order?> = ArrayList()
    private var filterData: ArrayList<Order?> = ArrayList()
    private var allItemList: ArrayList<Order?> = ArrayList()

    // Selected date filter
    private var selectedDateFilter: Date? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListRedesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeRecyclerView()
        setupClickListeners()
        setupSearchListener()
        observeSharedState()
    }
    
    /**
     * Observe shared ViewModel for filter/search state changes
     * Restores state when navigating back to this tab
     */
    private fun observeSharedState() {
        // Observe filter state - update pills and apply if orders loaded
        sharedFilterViewModel.filterState.observe(viewLifecycleOwner) { state ->
            // Skip if state hasn't changed (prevents flash on back navigation)
            if (state == currentFilterState) {
                return@observe
            }
            currentFilterState = state
            // Always update pills for visual consistency
            if (_binding != null) {
                updateFilterPills(state)
            }
            // Only apply filters if orders are loaded
            if (allItemList.isNotEmpty()) {
                applyDialogFilters(state)
            }
        }
        
        // Observe search query
        sharedFilterViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (query != currentSearchQuery) {
                currentSearchQuery = query
                // Update search input without triggering listener
                if (_binding != null) {
                    binding.searchInput.apply {
                        if (text.toString() != query) {
                            setText(query)
                            setSelection(query.length)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
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
        super.onDestroyView()
        _binding = null
    }

    // ==================== Initialization ====================

    private fun initializeRecyclerView() {
        binding.ordersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // Reuse existing adapter to avoid rebinding all items on back navigation
            if (orderAdapter == null) {
                orderAdapter = OrderCardRedesignAdapter(orderItems, this@OrderListRedesignFragment)
            }
            adapter = orderAdapter
        }
    }

    private fun setupClickListeners() {
        // Filter button
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        // Reset button
        binding.resetButton.setOnClickListener {
            resetFilters()
        }

        // Calendar icon for date picker
        binding.calendarIcon.setOnClickListener {
            showDatePicker()
        }
    }

    private fun resetFilters() {
        // Clear all filters
        filterArray.keys.forEach { filterArray[it] = 0 }
        selectedDateFilter = null
        currentFilterState = FilterDialogFragment.FilterState()
        currentSearchQuery = ""
        
        // Sync reset to shared ViewModel for cross-tab persistence
        sharedFilterViewModel.resetAll()
        
        binding.searchInput.text?.clear()
        binding.searchInput.hint = getString(R.string.search_orders)
        
        // Hide pills
        binding.filterPillsScroll.visibility = View.GONE
        binding.filterPillsContainer.removeAllViews()
        
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
        binding.searchInput.doAfterTextChanged { text ->
            if (this::searchRunnable.isInitialized) {
                handler.removeCallbacks(searchRunnable)
            }
            searchRunnable = Runnable {
                currentSearchQuery = text.toString().trim()
                // Sync to shared ViewModel for cross-tab persistence
                sharedFilterViewModel.setSearchQuery(currentSearchQuery)
                searchOrders(currentSearchQuery)
            }
            handler.postDelayed(searchRunnable, Constants.debouncingTime)
        }
    }

    // ==================== Data Loading ====================

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders() {
        showLoading(true)
        
        runOnBackgroundThread {
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderItems.clear()
                allItemList.clear()
                
                orderData?.forEach {
                    allItemList.add(it)
                    orderItems.add(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                debugSnackBar(getString(R.string.there_is_issue_with_your_account))
            }

            runOnMainThread {
                showLoading(false)
                
                // Apply any pending shared state after orders are loaded
                val sharedState = sharedFilterViewModel.filterState.value
                if (sharedState != null && sharedState.hasActiveFilters()) {
                    currentFilterState = sharedState
                    applyDialogFilters(sharedState)
                } else {
                    updateResultsInfo()
                    orderAdapter?.notifyDataSetChanged()
                    updateEmptyState()
                }
            }

            // Update filter options
            CoroutineScope(Dispatchers.Default).launch {
                updateFilterOptions()
            }
        }
    }

    private fun syncOrders() {
        isSyncing = true
        binding.searchInput.text?.clear()
        selectedDateFilter = null
        
        // Show syncing indicator
        binding.syncingContainer.showView()

        runOnBackgroundThread {
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderItems.clear()
                allItemList.clear()
                filterData.clear()
                
                orderData?.forEach {
                    allItemList.add(it)
                    orderItems.add(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnMainThread {
                isSyncing = false
                binding.syncingContainer.hideView()
                
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
                
                Toast.makeText(requireContext(), "Orders synced", Toast.LENGTH_SHORT).show()
            }

            CoroutineScope(Dispatchers.Default).launch {
                updateFilterOptions()
            }
        }
    }

    // ==================== Search ====================

    private fun searchOrders(query: String?) {
        runOnBackgroundThread(Dispatchers.Default) {
            val isFilterApplied = isFilterActive()

            if (query.isNullOrEmpty()) {
                // Empty search - show all or filtered
                orderItems.clear()
                if (isFilterApplied) {
                    filterData.forEach { orderItems.add(it) }
                } else {
                    allItemList.forEach { orderItems.add(it) }
                }
            } else {
                // Search in data
                orderItems.clear()
                val sourceList = if (isFilterApplied) filterData else allItemList
                
                for (order in sourceList) {
                    if (matchesSearch(order, query.lowercase())) {
                        orderItems.add(order)
                    }
                }

                // If no results and query looks like order ID, try direct lookup
                if (orderItems.isEmpty() && query.length > 7) {
                    exceptionHandler {
                        val result = myApp.getOrderConnector().getOrder(query)
                        if (result != null) {
                            orderItems.add(result)
                        }
                    }
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun matchesSearch(order: Order?, query: String): Boolean {
        // Use shared search logic
        return OrderSearchFilter.matchesSearch(order, query)
    }
    // ==================== Date Filter ====================

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
            binding.searchInput.hint = "Filtering: ${dateFormat.format(it)}"
        }
    }

    // ==================== Filters ====================

    private fun showFilterDialog() {
        // Build dynamic filter categories from Clover orders + OrderMate widgets
        val filterableWidgets = WidgetManager.getInstance(requireContext()).getFilterableWidgets() ?: emptyList()
        val categories = FilterCategoryBuilder.buildCategories(allItemList, filterableWidgets)
        
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
        runOnBackgroundThread {
            orderItems.clear()
            filterData.clear()

            allItemList.forEach { order ->
                if (orderMatchesFilters(order, filters)) {
                    orderItems.add(order)
                    filterData.add(order)
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
                updateFilterPills(filters)
            }
        }
    }

    private fun orderMatchesFilters(order: Order?, filters: FilterDialogFragment.FilterState): Boolean {
        if (order == null) return false

        // Check each category in selections
        for ((categoryId, selectedValues) in filters.selections) {
            if (selectedValues.isEmpty()) continue
            
            when {
                // Clover filters
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
                // OrderMate widget filters
                FilterCategoryBuilder.isWidgetFilter(categoryId) -> {
                    val widgetId = FilterCategoryBuilder.getWidgetId(categoryId)
                    val widget = WidgetManager.getInstance(requireContext()).getWidgetById(widgetId ?: "") ?: continue
                    val orderValues = extractWidgetValuesFromNotes(order.lineItems, widget.label)
                    if (!selectedValues.any { it in orderValues }) return false
                }
            }
        }

        // Check date filters
        for ((categoryId, dates) in filters.dateSelections) {
            if (dates.isEmpty()) continue
            
            when {
                // Order Date filter (Clover createdTime)
                categoryId == FilterCategoryBuilder.CLOVER_ORDER_DATE -> {
                    val orderDate = order.createdTime?.let { java.util.Date(it) }
                    if (orderDate == null) return false
                    
                    val matchesAny = dates.any { filterDate ->
                        isSameDay(orderDate, filterDate)
                    }
                    if (!matchesAny) return false
                }
                
                // Widget date filters (like Pickup Date from OrderMate widgets)
                FilterCategoryBuilder.isWidgetFilter(categoryId) -> {
                    val widgetId = FilterCategoryBuilder.getWidgetId(categoryId)
                    val widget = WidgetManager.getInstance(requireContext()).getWidgetById(widgetId ?: "") ?: continue
                    val orderDateValues = extractWidgetValuesFromNotes(order.lineItems, widget.label)
                    
                    val matchesAny = dates.any { filterDate ->
                        val dateStr = java.text.SimpleDateFormat("M/d/yy", java.util.Locale.getDefault()).format(filterDate)
                        orderDateValues.any { it.contains(dateStr) }
                    }
                    if (!matchesAny) return false
                }
            }
        }

        return true
    }
    
    /**
     * Extract widget values from line item notes
     * Notes format: "Label: Value • Label2: Value2"
     */
    private fun extractWidgetValuesFromNotes(lineItems: MutableList<LineItem?>?, widgetLabel: String): Set<String> {
        val values = mutableSetOf<String>()
        lineItems?.forEach { item ->
            item?.note?.let { note ->
                // Parse note format: "Label: Value • Label2: Value2"
                val regex = "$widgetLabel:\\s*([^•]+)".toRegex(RegexOption.IGNORE_CASE)
                regex.find(note)?.groupValues?.getOrNull(1)?.trim()?.let { 
                    values.add(it) 
                }
            }
        }
        return values
    }

    private fun isSameDay(d1: java.util.Date, d2: java.util.Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun updateFilterPills(filters: FilterDialogFragment.FilterState) {
        binding.filterPillsContainer.removeAllViews()

        val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        var hasPills = false
        
        // Add selection filters as pills
        filters.selections.forEach { (categoryId, values) ->
            values.forEach { value ->
                hasPills = true
                val displayValue = when (categoryId) {
                    FilterCategoryBuilder.CLOVER_PAYMENT_STATUS -> formatPaymentStatus(value)
                    FilterCategoryBuilder.CLOVER_ORDER_STATUS -> formatOrderStatus(value)
                    else -> value
                }
                val pill = createFilterPillWithClose(displayValue) {
                    removeSelectionFilter(categoryId, value)
                }
                binding.filterPillsContainer.addView(pill)
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
                binding.filterPillsContainer.addView(pill)
            }
        }

        binding.filterPillsScroll.visibility = if (hasPills) View.VISIBLE else View.GONE
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
    
    private fun formatPaymentStatus(status: String): String {
        return when (status.uppercase()) {
            "PAID" -> "Paid"
            "NOT_PAID" -> "Unpaid"
            "PARTIALLY_PAID" -> "Partial"
            "REFUNDED" -> "Refunded"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun formatOrderStatus(status: String): String {
        return when (status.lowercase()) {
            "open" -> "Open"
            "locked" -> "Closed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
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
