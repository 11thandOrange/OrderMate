package com.orderMate.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.databinding.DialogDatetimePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Combined date and time picker dialog with side-by-side layout.
 * Replaces the sequential DatePickerDialog -> TimePickerDialog flow.
 */
class DateTimePickerDialog : DialogFragment() {

    private var _binding: DialogDatetimePickerBinding? = null
    private val binding get() = _binding!!

    private var widgetLabel: String = "Due Date"
    private var initialDateTime: Date? = null
    private var listener: OnDateTimeSelectedListener? = null

    private val calendar = Calendar.getInstance()
    private val displayCalendar = Calendar.getInstance() // For month navigation
    
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    interface OnDateTimeSelectedListener {
        fun onDateTimeSelected(dateTime: Date, formattedDateTime: String)
    }

    companion object {
        const val TAG = "DateTimePickerDialog"

        fun newInstance(
            widgetLabel: String,
            initialDateTime: Date? = null
        ): DateTimePickerDialog {
            return DateTimePickerDialog().apply {
                this.widgetLabel = widgetLabel
                this.initialDateTime = initialDateTime
            }
        }
    }

    fun setListener(listener: OnDateTimeSelectedListener): DateTimePickerDialog {
        this.listener = listener
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDatetimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize calendar with initial date or current time
        initialDateTime?.let { calendar.time = it }
        displayCalendar.time = calendar.time

        setupHeader()
        setupCalendar()
        setupTimePicker()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val density = displayMetrics.density
            val screenWidth = displayMetrics.widthPixels
            // Max width 420dp for compact datetime picker
            val maxWidthPx = (420 * density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            window.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }

    private fun setupHeader() {
        binding.dialogSubtitle.text = widgetLabel
    }

    private fun setupCalendar() {
        // Setup RecyclerView with GridLayoutManager (7 columns)
        binding.calendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        
        updateMonthYearDisplay()
        renderCalendarGrid()

        binding.btnPrevMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, -1)
            updateMonthYearDisplay()
            renderCalendarGrid()
        }

        binding.btnNextMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, 1)
            updateMonthYearDisplay()
            renderCalendarGrid()
        }
    }

    private fun updateMonthYearDisplay() {
        binding.monthYearText.text = monthYearFormat.format(displayCalendar.time)
    }

    private fun renderCalendarGrid() {
        val days = generateCalendarDays()
        binding.calendarGrid.adapter = CalendarDayAdapter(days) { day ->
            if (day.dayNumber > 0) {
                if (day.isOtherMonth) {
                    // Clicking on other month day - navigate to that month and select
                    if (day.isPreviousMonth) {
                        displayCalendar.add(Calendar.MONTH, -1)
                    } else {
                        displayCalendar.add(Calendar.MONTH, 1)
                    }
                    updateMonthYearDisplay()
                }
                // Set selected date
                calendar.set(Calendar.YEAR, displayCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, displayCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, day.dayNumber)
                renderCalendarGrid()
            }
        }
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        
        val today = Calendar.getInstance()
        val selectedYear = calendar.get(Calendar.YEAR)
        val selectedMonth = calendar.get(Calendar.MONTH)
        val selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        val displayYear = displayCalendar.get(Calendar.YEAR)
        val displayMonth = displayCalendar.get(Calendar.MONTH)

        // Get first day of month
        val monthCal = displayCalendar.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month days
        val prevMonthCal = monthCal.clone() as Calendar
        prevMonthCal.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Add previous month padding days
        for (i in 0 until firstDayOfWeek) {
            val day = daysInPrevMonth - firstDayOfWeek + i + 1
            days.add(CalendarDay(day, isToday = false, isSelected = false, isOtherMonth = true, isPreviousMonth = true))
        }

        // Add current month days
        for (day in 1..daysInMonth) {
            val isToday = displayYear == today.get(Calendar.YEAR) &&
                          displayMonth == today.get(Calendar.MONTH) &&
                          day == today.get(Calendar.DAY_OF_MONTH)
            
            val isSelected = displayYear == selectedYear &&
                             displayMonth == selectedMonth &&
                             day == selectedDay

            days.add(CalendarDay(day, isToday, isSelected, isOtherMonth = false, isPreviousMonth = false))
        }

        // Add next month padding days to fill 6 rows (42 cells)
        var nextMonthDay = 1
        while (days.size < 42) {
            days.add(CalendarDay(nextMonthDay++, isToday = false, isSelected = false, isOtherMonth = true, isPreviousMonth = false))
        }

        return days
    }

    // Data class for calendar day
    data class CalendarDay(
        val dayNumber: Int,
        val isToday: Boolean,
        val isSelected: Boolean,
        val isOtherMonth: Boolean,
        val isPreviousMonth: Boolean  // true = previous month, false = next month (only relevant if isOtherMonth)
    )

    // Adapter for calendar grid
    inner class CalendarDayAdapter(
        private val days: List<CalendarDay>,
        private val onDayClick: (CalendarDay) -> Unit
    ) : RecyclerView.Adapter<CalendarDayAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_datetime_picker_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(days[position])
        }

        override fun getItemCount(): Int = days.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dayText: TextView = itemView.findViewById(R.id.dayText)

            fun bind(day: CalendarDay) {
                val context = itemView.context
                dayText.text = day.dayNumber.toString()

                when {
                    day.isSelected -> {
                        dayText.setTextColor(Color.WHITE)
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                    }
                    day.isToday -> {
                        dayText.setTextColor(ContextCompat.getColor(context, R.color.orange_accent))
                        dayText.background = createTodayBackground()
                    }
                    day.isOtherMonth -> {
                        dayText.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                        dayText.setBackgroundResource(0)
                    }
                    else -> {
                        dayText.setTextColor(ContextCompat.getColor(context, R.color.text_light))
                        dayText.setBackgroundResource(0)
                    }
                }

                itemView.setOnClickListener {
                    onDayClick(day)
                }
            }

            private fun createTodayBackground(): GradientDrawable {
                val density = itemView.resources.displayMetrics.density
                return GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6 * density
                    setColor(Color.TRANSPARENT)
                    setStroke((1 * density).toInt(), ContextCompat.getColor(itemView.context, R.color.orange_accent))
                }
            }
        }
    }

    private var hourTouchStartY = 0f
    private var minuteTouchStartY = 0f
    private val SCROLL_THRESHOLD = 30f // Minimum scroll distance to trigger change

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupTimePicker() {
        updateTimeDisplay()
        updateAmPmButtons()

        // (#77) Touch scroll support for hour column
        binding.hourScrollContainer.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    hourTouchStartY = event.y
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = hourTouchStartY - event.y
                    if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                        if (deltaY > 0) {
                            // Scrolled up - increase hour
                            calendar.add(Calendar.HOUR_OF_DAY, 1)
                        } else {
                            // Scrolled down - decrease hour
                            calendar.add(Calendar.HOUR_OF_DAY, -1)
                        }
                        hourTouchStartY = event.y
                        updateTimeDisplay()
                        updateAmPmButtons()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    true
                }
                else -> false
            }
        }

        // (#77) Touch scroll support for minute column
        binding.minuteScrollContainer.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    minuteTouchStartY = event.y
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = minuteTouchStartY - event.y
                    if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                        if (deltaY > 0) {
                            // Scrolled up - increase minute
                            calendar.add(Calendar.MINUTE, 1)
                        } else {
                            // Scrolled down - decrease minute
                            calendar.add(Calendar.MINUTE, -1)
                        }
                        minuteTouchStartY = event.y
                        updateTimeDisplay()
                        updateAmPmButtons()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    true
                }
                else -> false
            }
        }

        // Also allow clicking on individual numbers to jump to that value
        setupHourClickListeners()
        setupMinuteClickListeners()

        // AM/PM toggle (simple text buttons, no background)
        binding.btnAM.setOnClickListener {
            if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                calendar.add(Calendar.HOUR_OF_DAY, -12)
                updateTimeDisplay()
                updateAmPmButtons()
            }
        }

        binding.btnPM.setOnClickListener {
            if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
                calendar.add(Calendar.HOUR_OF_DAY, 12)
                updateTimeDisplay()
                updateAmPmButtons()
            }
        }
    }

    private fun setupHourClickListeners() {
        binding.hourPrev3Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, -3)
            updateTimeDisplay()
            updateAmPmButtons()
        }
        binding.hourPrev2Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, -2)
            updateTimeDisplay()
            updateAmPmButtons()
        }
        binding.hourPrev1Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, -1)
            updateTimeDisplay()
            updateAmPmButtons()
        }
        binding.hourNext1Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            updateTimeDisplay()
            updateAmPmButtons()
        }
        binding.hourNext2Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            updateTimeDisplay()
            updateAmPmButtons()
        }
        binding.hourNext3Text.setOnClickListener {
            calendar.add(Calendar.HOUR_OF_DAY, 3)
            updateTimeDisplay()
            updateAmPmButtons()
        }
    }

    private fun setupMinuteClickListeners() {
        binding.minutePrev3Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, -3)
            updateTimeDisplay()
        }
        binding.minutePrev2Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, -2)
            updateTimeDisplay()
        }
        binding.minutePrev1Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, -1)
            updateTimeDisplay()
        }
        binding.minuteNext1Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, 1)
            updateTimeDisplay()
        }
        binding.minuteNext2Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, 2)
            updateTimeDisplay()
        }
        binding.minuteNext3Text.setOnClickListener {
            calendar.add(Calendar.MINUTE, 3)
            updateTimeDisplay()
        }
    }

    private fun updateTimeDisplay() {
        var hour = calendar.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = calendar.get(Calendar.MINUTE)

        // Current values
        binding.hourText.text = String.format("%02d", hour)
        binding.minuteText.text = String.format("%02d", minute)
        
        // (#77) Show 3 previous and 3 next hours with wrapping (1-12)
        binding.hourPrev1Text.text = String.format("%02d", wrapHour12(hour - 1))
        binding.hourPrev2Text.text = String.format("%02d", wrapHour12(hour - 2))
        binding.hourPrev3Text.text = String.format("%02d", wrapHour12(hour - 3))
        binding.hourNext1Text.text = String.format("%02d", wrapHour12(hour + 1))
        binding.hourNext2Text.text = String.format("%02d", wrapHour12(hour + 2))
        binding.hourNext3Text.text = String.format("%02d", wrapHour12(hour + 3))
        
        // (#77) Show 3 previous and 3 next minutes with wrapping (0-59)
        binding.minutePrev1Text.text = String.format("%02d", wrapMinute(minute - 1))
        binding.minutePrev2Text.text = String.format("%02d", wrapMinute(minute - 2))
        binding.minutePrev3Text.text = String.format("%02d", wrapMinute(minute - 3))
        binding.minuteNext1Text.text = String.format("%02d", wrapMinute(minute + 1))
        binding.minuteNext2Text.text = String.format("%02d", wrapMinute(minute + 2))
        binding.minuteNext3Text.text = String.format("%02d", wrapMinute(minute + 3))
    }

    // Wrap hour to 1-12 range
    private fun wrapHour12(hour: Int): Int {
        var h = hour % 12
        if (h <= 0) h += 12
        return h
    }

    // Wrap minute to 0-59 range
    private fun wrapMinute(minute: Int): Int {
        var m = minute % 60
        if (m < 0) m += 60
        return m
    }

    private fun updateAmPmButtons() {
        val isAM = calendar.get(Calendar.AM_PM) == Calendar.AM
        val context = requireContext()

        // (#77) Simple text styling - no background, just color change
        if (isAM) {
            binding.btnAM.setTextColor(ContextCompat.getColor(context, R.color.orange_accent))
            binding.btnPM.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        } else {
            binding.btnPM.setTextColor(ContextCompat.getColor(context, R.color.orange_accent))
            binding.btnAM.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val formattedDateTime = dateTimeFormat.format(calendar.time)
            listener?.onDateTimeSelected(calendar.time, formattedDateTime)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
