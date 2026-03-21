package com.orderMate.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.model.ScheduledEvent
import com.orderMate.model.EventType
import com.orderMate.utils.CalendarManager
import com.orderMate.utils.Constants
import com.orderMate.utils.FilterCategoryBuilder
import com.orderMate.utils.FilterCategoryBuilder.FilterCategory
import com.orderMate.utils.FilterCategoryBuilder.FilterType
import java.text.SimpleDateFormat
import java.util.*

/**
 * iOS-style Calendar Tab Fragment (#82 requirement)
 * 
 * Displays scheduled orders as calendar events for:
 * - Pickup orders
 * - Delivery orders
 * - Preorders
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
    
    // All events and filtered events
    private var allEvents: List<ScheduledEvent> = emptyList()
    private var filteredEvents: List<ScheduledEvent> = emptyList()
    
    // Filter state
    private var currentFilterState = FilterDialogFragment.FilterState()
    private var currentSearchQuery: String = ""
    
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
    
    // Header Views
    private var searchInput: EditText? = null
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
        loadEvents()
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
        
        // Header views
        searchInput = view.findViewById(R.id.searchInput)
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
    }
    
    private fun setupSearchListener() {
        searchInput?.doAfterTextChanged { text ->
            if (this::searchRunnable.isInitialized) {
                handler.removeCallbacks(searchRunnable)
            }
            searchRunnable = Runnable {
                currentSearchQuery = text.toString().trim()
                applyFilters()
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
    
    private fun loadEvents() {
        // Load all events from calendar manager
        allEvents = calendarManager.getAllEvents()
        filteredEvents = allEvents
        renderCalendar()
    }
    
    private fun showFilterDialog() {
        val categories = buildCalendarFilterCategories()
        
        val dialog = FilterDialogFragment.newInstance(categories, currentFilterState)
        dialog.setFilterListener(object : FilterDialogFragment.FilterListener {
            override fun onFiltersApplied(filterState: FilterDialogFragment.FilterState) {
                currentFilterState = filterState
                applyFilters()
                updateFilterPills()
            }
            
            override fun onFiltersCleared() {
                resetFilters()
            }
        })
        dialog.show(parentFragmentManager, "CalendarFilterDialog")
    }
    
    private fun buildCalendarFilterCategories(): List<FilterCategory> {
        val categories = mutableListOf<FilterCategory>()
        
        // Event Type filter
        categories.add(FilterCategory(
            id = "event_type",
            title = "Event Type",
            type = FilterType.MULTI_SELECT,
            options = listOf("Pickup", "Delivery", "Preorder")
        ))
        
        // Date filter
        categories.add(FilterCategory(
            id = FilterCategoryBuilder.CLOVER_ORDER_DATE,
            title = "Due Date",
            type = FilterType.DATE_PICKER,
            options = emptyList()
        ))
        
        return categories
    }
    
    private fun applyFilters() {
        filteredEvents = allEvents.filter { event ->
            matchesSearch(event) && matchesFilters(event)
        }
        renderCalendar()
    }
    
    private fun matchesSearch(event: ScheduledEvent): Boolean {
        if (currentSearchQuery.isEmpty()) return true
        
        val query = currentSearchQuery.lowercase()
        return event.customerName.lowercase().contains(query) ||
               event.orderId.lowercase().contains(query) ||
               event.type.getDisplayName().lowercase().contains(query) ||
               (event.note?.lowercase()?.contains(query) == true)
    }
    
    private fun matchesFilters(event: ScheduledEvent): Boolean {
        // Check event type filter
        val eventTypeSelections = currentFilterState.selections["event_type"]
        if (!eventTypeSelections.isNullOrEmpty()) {
            val eventTypeName = event.type.getDisplayName()
            if (!eventTypeSelections.any { it.equals(eventTypeName, ignoreCase = true) }) {
                return false
            }
        }
        
        // Check date filter
        val dateSelections = currentFilterState.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE]
        if (!dateSelections.isNullOrEmpty()) {
            val matchesAnyDate = dateSelections.any { selectedDate ->
                isSameDay(event.dueDate, selectedDate)
            }
            if (!matchesAnyDate) return false
        }
        
        return true
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
        searchInput?.text?.clear()
        filteredEvents = allEvents
        updateFilterPills()
        renderCalendar()
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateFilterPills() {
        filterPillsContainer?.removeAllViews()
        
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        var hasPills = false
        
        // Add event type filter pills
        currentFilterState.selections["event_type"]?.forEach { value ->
            hasPills = true
            val pill = createFilterPillWithClose(value) {
                removeSelectionFilter("event_type", value)
            }
            filterPillsContainer?.addView(pill)
        }
        
        // Add date filter pills
        currentFilterState.dateSelections[FilterCategoryBuilder.CLOVER_ORDER_DATE]?.forEachIndexed { index, date ->
            hasPills = true
            val pill = createFilterPillWithClose("Due: ${dateFormat.format(date)}") {
                removeDateFilter(FilterCategoryBuilder.CLOVER_ORDER_DATE, index)
            }
            filterPillsContainer?.addView(pill)
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
        applyFilters()
        updateFilterPills()
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
        applyFilters()
        updateFilterPills()
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
            
            // Text label
            val textView = TextView(context).apply {
                this.text = text
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            addView(textView)
            
            // Close button
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
    
    private fun setViewMode(mode: String) {
        currentViewMode = mode
        
        // Reset all buttons
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        btnDay?.setBackgroundResource(0)
        btnDay?.setTextColor(inactiveColor)
        btnWeek?.setBackgroundResource(0)
        btnWeek?.setTextColor(inactiveColor)
        btnMonth?.setBackgroundResource(0)
        btnMonth?.setTextColor(inactiveColor)
        
        // Set active button (orange background, dark text)
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
        
        // Re-render calendar with new view mode
        renderCalendar()
    }
    
    fun renderCalendar() {
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        
        // Update title
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTitle?.text = dateFormat.format(currentDate.time)
        
        // Get filtered events for the current month
        val monthEvents = filteredEvents.filter { event ->
            val eventCal = Calendar.getInstance()
            eventCal.time = event.dueDate
            eventCal.get(Calendar.YEAR) == year && eventCal.get(Calendar.MONTH) == month
        }
        
        // Generate calendar days
        val days = generateCalendarDays(year, month, monthEvents)
        
        // Set adapter
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
