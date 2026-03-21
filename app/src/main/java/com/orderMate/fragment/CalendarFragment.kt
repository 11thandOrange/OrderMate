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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import com.orderMate.model.ScheduledEvent
import com.orderMate.model.EventType
import com.orderMate.utils.CalendarManager
import com.orderMate.utils.Constants
import com.orderMate.utils.FilterCategoryBuilder
import com.orderMate.utils.MyApp
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.migrations.SchemaMigrator
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
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
    
    // Order data (same as List tab)
    private var allOrders: ArrayList<Order?> = ArrayList()
    private var filteredOrders: ArrayList<Order?> = ArrayList()
    
    // Events for calendar display
    private var allEvents: List<ScheduledEvent> = emptyList()
    private var filteredEvents: List<ScheduledEvent> = emptyList()
    
    // Filter state (same as List tab)
    private var currentFilterState = FilterDialogFragment.FilterState()
    private var currentSearchQuery: String = ""
    private var selectedDateFilter: Date? = null
    
    // Widget manager for dynamic filters (same as List tab)
    private var widgetManager: WidgetManager? = null
    
    private val myApp by lazy { MyApp.getInstance() }
    
    // Handler for search debouncing
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchRunnable: Runnable
    
    // Calendar Views
    private var monthYearTitle: TextView? = null
    private var calendarGrid: RecyclerView? = null
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
    private var filterButton: View? = null
    private var resetButton: View? = null
    private var filterPillsScroll: HorizontalScrollView? = null
    private var filterPillsContainer: LinearLayout? = null
    
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
        initWidgetManager()
        loadOrders()
    }
    
    private fun initViews(view: View) {
        // Calendar views
        monthYearTitle = view.findViewById(R.id.monthYearTitle)
        calendarGrid = view.findViewById(R.id.calendarGrid)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnDay = view.findViewById(R.id.btnDay)
        btnWeek = view.findViewById(R.id.btnWeek)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnToday = view.findViewById(R.id.btnToday)
        btnNextFulfillment = view.findViewById(R.id.btnNextFulfillment)
        
        // Header views (same as List tab)
        searchInput = view.findViewById(R.id.searchInput)
        calendarIcon = view.findViewById(R.id.calendarIcon)
        filterButton = view.findViewById(R.id.filterButton)
        resetButton = view.findViewById(R.id.resetButton)
        filterPillsScroll = view.findViewById(R.id.filterPillsScroll)
        filterPillsContainer = view.findViewById(R.id.filterPillsContainer)
        
        // Setup grid layout manager
        calendarGrid?.layoutManager = GridLayoutManager(requireContext(), 7)
    }
    
    private fun setupHeaderActions() {
        filterButton?.setOnClickListener { showFilterDialog() }
        resetButton?.setOnClickListener { resetFilters() }
        calendarIcon?.setOnClickListener { showDatePicker() }
    }
    
    private fun setupSearchListener() {
        searchInput?.doAfterTextChanged { text ->
            if (this::searchRunnable.isInitialized) {
                handler.removeCallbacks(searchRunnable)
            }
            searchRunnable = Runnable {
                currentSearchQuery = text.toString().trim()
                searchOrders(currentSearchQuery)
            }
            handler.postDelayed(searchRunnable, Constants.debouncingTime)
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
     * Initialize widget manager for dynamic filters (same as List tab)
     */
    private fun initWidgetManager() {
        val merchantId = myApp.getMerchantId() ?: return
        
        SchemaMigrator.migrateIfNeeded(merchantId) { success ->
            if (success) {
                widgetManager = WidgetManager(merchantId)
                widgetManager?.load { loaded ->
                    if (loaded) {
                        android.util.Log.d("CalendarFragment", "Widget manager loaded: ${widgetManager?.getWidgetCount()} widgets")
                    }
                }
            }
        }
    }
    
    /**
     * Load orders from Clover (same as List tab)
     */
    private fun loadOrders() {
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
                renderCalendar()
            }
        }
    }
    
    /**
     * Convert Clover orders to calendar events
     */
    private fun convertOrdersToEvents(orders: List<Order?>): List<ScheduledEvent> {
        return orders.mapNotNull { order ->
            order ?: return@mapNotNull null
            
            val dueDate = order.createdTime?.let { Date(it) } ?: return@mapNotNull null
            val customerName = try {
                val customer = order.customers?.firstOrNull()
                "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".trim().ifEmpty { "-" }
            } catch (e: Exception) { "-" }
            
            val total = try {
                (order.total ?: 0L) / 100.0
            } catch (e: Exception) { 0.0 }
            
            // Determine event type based on order data
            val eventType = determineEventType(order)
            
            ScheduledEvent(
                id = order.id?.hashCode()?.toLong() ?: System.currentTimeMillis(),
                orderId = order.id ?: "",
                customerName = customerName,
                dueDate = dueDate,
                type = eventType,
                total = total,
                note = null
            )
        }
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

    // ==================== Search (same as List tab) ====================
    
    private fun searchOrders(query: String?) {
        runOnBackgroundThread {
            val isFilterApplied = currentFilterState.hasActiveFilters()

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
        if (order == null) return false
        
        return exceptionHandlerWithReturn {
            val orderId = order.id?.lowercase() ?: ""
            
            val customerName = try {
                val customer = order.customers?.firstOrNull()
                "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".lowercase()
            } catch (e: Exception) { "" }
            
            val employeeName = try {
                order.employee?.jsonObject?.get(Constants.name)?.toString()?.lowercase() ?: ""
            } catch (e: Exception) { "" }
            
            val paymentStatus = order.paymentState?.name?.lowercase() ?: ""
            
            val customerContact = try {
                getCustomerContactDetails(order.customers?.firstOrNull())
            } catch (e: Exception) { Pair("", "") }

            val widgetDataMatch = order.lineItems?.any { lineItem ->
                val note = lineItem?.note?.lowercase() ?: ""
                val itemName = lineItem?.name?.lowercase() ?: ""
                note.contains(query) || itemName.contains(query)
            } ?: false

            orderId.contains(query) ||
            customerName.contains(query) ||
            employeeName.contains(query) ||
            paymentStatus.contains(query) ||
            customerContact.first.lowercase().contains(query) ||
            customerContact.second.lowercase().contains(query) ||
            widgetDataMatch
        } ?: false
    }

    // ==================== Date Picker (same as List tab) ====================
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDateFilter?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_Dialog,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateFilter = calendar.time
                currentDate.set(year, month, day)
                filterByDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun filterByDate(date: Date) {
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
        val filterableWidgets = widgetManager?.filterableWidgets ?: emptyList()
        val categories = FilterCategoryBuilder.buildCategories(allOrders, filterableWidgets)
        
        val dialog = FilterDialogFragment.newInstance(
            categories = categories,
            currentFilters = currentFilterState
        )
        
        dialog.setFilterListener(object : FilterDialogFragment.FilterListener {
            override fun onFiltersApplied(filters: FilterDialogFragment.FilterState) {
                currentFilterState = filters
                applyDialogFilters(filters)
            }

            override fun onFilterCleared() {
                currentFilterState = FilterDialogFragment.FilterState()
                resetFilters()
            }
        })
        
        dialog.show(childFragmentManager, FilterDialogFragment.TAG)
    }
    
    private fun applyDialogFilters(filters: FilterDialogFragment.FilterState) {
        runOnBackgroundThread {
            filteredOrders.clear()

            allOrders.forEach { order ->
                if (orderMatchesFilters(order, filters)) {
                    filteredOrders.add(order)
                }
            }
            
            filteredEvents = convertOrdersToEvents(filteredOrders)

            runOnMainThread {
                renderCalendar()
                updateFilterPills(filters)
            }
        }
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
                    val widget = widgetManager?.getWidgetById(widgetId ?: "") ?: continue
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
        searchInput?.text?.clear()
        searchInput?.hint = getString(R.string.search_orders)
        
        filterPillsScroll?.visibility = View.GONE
        filterPillsContainer?.removeAllViews()
        
        filteredOrders.clear()
        allOrders.forEach { filteredOrders.add(it) }
        filteredEvents = allEvents
        
        renderCalendar()
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateFilterPills(filters: FilterDialogFragment.FilterState) {
        filterPillsContainer?.removeAllViews()
        
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        var hasPills = false
        
        // Add selection filter pills
        for ((categoryId, values) in filters.selections) {
            values.forEach { value ->
                hasPills = true
                val pill = createFilterPillWithClose(value) {
                    removeSelectionFilter(categoryId, value)
                }
                filterPillsContainer?.addView(pill)
            }
        }
        
        // Add date filter pills
        for ((categoryId, dates) in filters.dateSelections) {
            dates.forEachIndexed { index, date ->
                hasPills = true
                val pill = createFilterPillWithClose(dateFormat.format(date)) {
                    removeDateFilter(categoryId, index)
                }
                filterPillsContainer?.addView(pill)
            }
        }
        
        filterPillsScroll?.visibility = if (hasPills) View.VISIBLE else View.GONE
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
        
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        btnDay?.setBackgroundResource(0)
        btnDay?.setTextColor(inactiveColor)
        btnWeek?.setBackgroundResource(0)
        btnWeek?.setTextColor(inactiveColor)
        btnMonth?.setBackgroundResource(0)
        btnMonth?.setTextColor(inactiveColor)
        
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
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
        
        renderCalendar()
    }

    // ==================== Calendar Rendering ====================
    
    fun renderCalendar() {
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTitle?.text = dateFormat.format(currentDate.time)
        
        val monthEvents = filteredEvents.filter { event ->
            val eventCal = Calendar.getInstance()
            eventCal.time = event.dueDate
            eventCal.get(Calendar.YEAR) == year && eventCal.get(Calendar.MONTH) == month
        }
        
        val days = generateCalendarDays(year, month, monthEvents)
        
        calendarGrid?.adapter = CalendarDayAdapter(days) { day ->
            if (day.hasEvents) {
                showEventPreview(day.events.first())
            }
        }
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
    
    fun navigateMonth(direction: Int) {
        currentDate.add(Calendar.MONTH, direction)
        renderCalendar()
    }
    
    fun goToToday() {
        currentDate = Calendar.getInstance()
        renderCalendar()
    }
    
    fun goToNextFulfillment() {
        val nextEvent = calendarManager.getNextScheduledEvent()
        nextEvent?.let { event ->
            currentDate.time = event.dueDate
            renderCalendar()
            showEventPreview(event)
        } ?: run {
            showNoUpcomingEventsMessage()
        }
    }
    
    fun showEventPreview(event: ScheduledEvent) {
        Toast.makeText(
            requireContext(),
            "Order: ${event.orderId}\n${event.customerName}\n$${event.total}",
            Toast.LENGTH_LONG
        ).show()
    }
    
    fun viewFullOrderDetails(orderId: String) {
        // Navigate to order details page
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
                    
                    // Show first event
                    val firstEvent = day.events[0]
                    event1.text = firstEvent.customerName
                    event1.visibility = View.VISIBLE
                    setEventStyle(event1, firstEvent.type)
                    
                    // Show second event if exists
                    if (day.events.size > 1) {
                        val secondEvent = day.events[1]
                        event2.text = secondEvent.customerName
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
