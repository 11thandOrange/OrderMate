package com.orderMate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.model.ScheduledEvent
import com.orderMate.utils.CalendarManager
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
 */
class CalendarFragment : Fragment() {

    private lateinit var calendarManager: CalendarManager
    private var currentDate: Calendar = Calendar.getInstance()
    private var currentViewMode: String = "month"
    
    // Views
    private var monthYearTitle: TextView? = null
    private var calendarGrid: RecyclerView? = null
    private var btnPrev: View? = null
    private var btnNext: View? = null
    private var btnDay: TextView? = null
    private var btnWeek: TextView? = null
    private var btnMonth: TextView? = null
    private var btnToday: TextView? = null
    private var btnNextFulfillment: TextView? = null
    
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
        setupCalendarNavigation()
        setupViewToggle()
        setupActionButtons()
        renderCalendar()
    }
    
    private fun initViews(view: View) {
        monthYearTitle = view.findViewById(R.id.monthYearTitle)
        calendarGrid = view.findViewById(R.id.calendarGrid)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnDay = view.findViewById(R.id.btnDay)
        btnWeek = view.findViewById(R.id.btnWeek)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnToday = view.findViewById(R.id.btnToday)
        btnNextFulfillment = view.findViewById(R.id.btnNextFulfillment)
        
        // Setup grid layout manager
        calendarGrid?.layoutManager = GridLayoutManager(requireContext(), 7)
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
        
        // Get events for the current month
        val events = calendarManager.getEventsForMonth(year, month)
        
        // Generate calendar days
        val days = generateCalendarDays(year, month, events)
        
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
        
        // Get the day of week for the first day of month
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Add empty days for padding
        for (i in 0 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, emptyList()))
        }
        
        // Add days of month
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayEvents = events.filter { event ->
                val eventCal = Calendar.getInstance()
                eventCal.time = event.dueDate
                eventCal.get(Calendar.DAY_OF_MONTH) == day
            }
            days.add(CalendarDay(day, dayEvents.isNotEmpty(), dayEvents))
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
        val events: List<ScheduledEvent>
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
            private val eventIndicator: View = itemView.findViewById(R.id.eventIndicator)
            private val eventPreview: TextView = itemView.findViewById(R.id.eventPreview)
            private val dayContainer: View = itemView.findViewById(R.id.dayContainer)
            
            fun bind(day: CalendarDay) {
                if (day.dayNumber == 0) {
                    dayNumber.text = ""
                    eventIndicator.visibility = View.GONE
                    eventPreview.visibility = View.GONE
                    dayContainer.setOnClickListener(null)
                } else {
                    dayNumber.text = day.dayNumber.toString()
                    
                    if (day.hasEvents) {
                        eventIndicator.visibility = View.VISIBLE
                        eventPreview.visibility = View.VISIBLE
                        eventPreview.text = day.events.first().customerName
                        dayContainer.setOnClickListener { onDayClick(day) }
                    } else {
                        eventIndicator.visibility = View.GONE
                        eventPreview.visibility = View.GONE
                        dayContainer.setOnClickListener(null)
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
