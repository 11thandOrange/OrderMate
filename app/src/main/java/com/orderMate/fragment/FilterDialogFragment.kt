package com.orderMate.fragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.databinding.DialogFiltersBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter dialog for order list (#80 requirement)
 * Pill-style filter options for payment status, order status, payment type, etc.
 */
class FilterDialogFragment : DialogFragment() {

    private var _binding: DialogFiltersBinding? = null
    private val binding get() = _binding!!

    private var listener: FilterListener? = null

    // Current filter selections (multi-select)
    private val selectedPaymentStatuses = mutableSetOf<String>()
    private val selectedOrderStatuses = mutableSetOf<String>()
    private val selectedPaymentTypes = mutableSetOf<String>()
    private val selectedCategories = mutableSetOf<String>()
    private val selectedEmployees = mutableSetOf<String>()

    // Date filters (can have multiple)
    private val selectedOrderDates = mutableListOf<Date>()
    private val selectedPickupDates = mutableListOf<Date>()

    // Available options (passed from fragment)
    private var availableEmployees: List<String> = emptyList()
    private var availableCategories: List<String> = emptyList()

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    interface FilterListener {
        fun onFiltersApplied(filters: FilterState)
        fun onFilterCleared()
    }

    data class FilterState(
        val paymentStatuses: Set<String> = emptySet(),
        val orderStatuses: Set<String> = emptySet(),
        val paymentTypes: Set<String> = emptySet(),
        val categories: Set<String> = emptySet(),
        val employees: Set<String> = emptySet(),
        val orderDates: List<Date> = emptyList(),
        val pickupDates: List<Date> = emptyList()
    ) {
        fun hasActiveFilters(): Boolean {
            return paymentStatuses.isNotEmpty() || orderStatuses.isNotEmpty() || 
                   paymentTypes.isNotEmpty() || categories.isNotEmpty() || employees.isNotEmpty() ||
                   orderDates.isNotEmpty() || pickupDates.isNotEmpty()
        }

        fun getActiveFilterCount(): Int {
            return paymentStatuses.size + orderStatuses.size + paymentTypes.size + 
                   categories.size + employees.size + orderDates.size + pickupDates.size
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        // Close on outside click
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen, max 520dp
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val maxWidthPx = (520 * displayMetrics.density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            val maxHeightPx = (displayMetrics.heightPixels * 0.85).toInt()
            
            window.setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            
            // Also set max height via layout params
            window.attributes = window.attributes.apply {
                height = minOf(height, maxHeightPx)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore any passed filter state
        arguments?.let { args ->
            args.getStringArrayList(ARG_PAYMENT_STATUSES)?.let { selectedPaymentStatuses.addAll(it) }
            args.getStringArrayList(ARG_ORDER_STATUSES)?.let { selectedOrderStatuses.addAll(it) }
            args.getStringArrayList(ARG_PAYMENT_TYPES)?.let { selectedPaymentTypes.addAll(it) }
            args.getStringArrayList(ARG_CATEGORIES_SELECTED)?.let { selectedCategories.addAll(it) }
            args.getStringArrayList(ARG_EMPLOYEES_SELECTED)?.let { selectedEmployees.addAll(it) }
            availableEmployees = args.getStringArrayList(ARG_EMPLOYEES) ?: emptyList()
            availableCategories = args.getStringArrayList(ARG_CATEGORIES) ?: emptyList()
        }

        setupDateInputs()
        setupFilterOptions()
    }

    private fun setupDateInputs() {
        // Order Date input
        binding.orderDateInput.setOnClickListener { showDatePicker(true) }
        binding.orderDatePickerLabel.setOnClickListener { showDatePicker(true) }

        // Pickup Date input
        binding.pickupDateInput.setOnClickListener { showDatePicker(false) }
        binding.pickupDatePickerLabel.setOnClickListener { showDatePicker(false) }

        // Update chips
        updateDateChips()
    }

    private fun showDatePicker(isOrderDate: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_DatePicker,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val date = calendar.time

                if (isOrderDate) {
                    if (!selectedOrderDates.any { isSameDay(it, date) }) {
                        selectedOrderDates.add(date)
                        binding.orderDateInput.setText(dateFormat.format(date))
                    }
                } else {
                    if (!selectedPickupDates.any { isSameDay(it, date) }) {
                        selectedPickupDates.add(date)
                        binding.pickupDateInput.setText(dateFormat.format(date))
                    }
                }
                updateDateChips()
                // Apply filters immediately
                applyFiltersImmediately()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateDateChips() {
        // Order date chips
        binding.orderDateChips.removeAllViews()
        selectedOrderDates.forEach { date ->
            val chip = createDateChip(dateFormat.format(date)) {
                selectedOrderDates.remove(date)
                updateDateChips()
                if (selectedOrderDates.isEmpty()) {
                    binding.orderDateInput.text?.clear()
                }
                // Apply filters immediately
                applyFiltersImmediately()
            }
            binding.orderDateChips.addView(chip)
        }

        // Pickup date chips
        binding.pickupDateChips.removeAllViews()
        selectedPickupDates.forEach { date ->
            val chip = createDateChip(dateFormat.format(date)) {
                selectedPickupDates.remove(date)
                updateDateChips()
                if (selectedPickupDates.isEmpty()) {
                    binding.pickupDateInput.text?.clear()
                }
                // Apply filters immediately
                applyFiltersImmediately()
            }
            binding.pickupDateChips.addView(chip)
        }
    }

    private fun createDateChip(text: String, onRemove: () -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_date_chip)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6))

            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(6), dpToPx(6))
            layoutParams = lp

            // Date text
            val textView = TextView(context).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(context, R.color.text_light))
                textSize = 12f
            }
            addView(textView)

            // Remove button
            val removeBtn = TextView(context).apply {
                this.text = "✕"
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                textSize = 12f
                setPadding(dpToPx(6), 0, 0, 0)
                setOnClickListener { onRemove() }
            }
            addView(removeBtn)
        }
    }

    private fun setupFilterOptions() {
        // Payment Status options (excluding "All" - multi-select doesn't need it)
        setupMultiSelectSection(
            binding.paymentStatusOptions,
            listOf("Paid" to "paid", "Unpaid" to "unpaid", "Refunded" to "refunded", "Partial" to "partial"),
            selectedPaymentStatuses
        )

        // Order Status options
        setupMultiSelectSection(
            binding.orderStatusOptions,
            listOf("Open" to "open", "Closed" to "closed"),
            selectedOrderStatuses
        )

        // Payment Type options
        setupMultiSelectSection(
            binding.paymentTypeOptions,
            listOf("Card" to "card", "Cash" to "cash"),
            selectedPaymentTypes
        )

        // Category options
        val categoryOptions = availableCategories.map { it to it }
        setupMultiSelectSection(
            binding.categoryOptions,
            categoryOptions,
            selectedCategories
        )

        // Employee options
        val employeeOptions = availableEmployees.map { it to it }
        setupMultiSelectSection(
            binding.employeeOptions,
            employeeOptions,
            selectedEmployees
        )
    }

    private fun setupMultiSelectSection(
        container: FlexboxLayout,
        options: List<Pair<String, String>>,
        selectedValues: MutableSet<String>
    ) {
        container.removeAllViews()

        options.forEach { (label, value) ->
            val isSelected = selectedValues.contains(value)
            val chip = createFilterChip(label, value, isSelected)
            chip.setOnClickListener {
                if (selectedValues.contains(value)) {
                    // Deselect
                    selectedValues.remove(value)
                    updateChipState(chip, false)
                } else {
                    // Select
                    selectedValues.add(value)
                    updateChipState(chip, true)
                }
                // Apply filters immediately
                applyFiltersImmediately()
            }
            container.addView(chip)
        }
    }

    private fun applyFiltersImmediately() {
        val filterState = FilterState(
            paymentStatuses = selectedPaymentStatuses.toSet(),
            orderStatuses = selectedOrderStatuses.toSet(),
            paymentTypes = selectedPaymentTypes.toSet(),
            categories = selectedCategories.toSet(),
            employees = selectedEmployees.toSet(),
            orderDates = selectedOrderDates.toList(),
            pickupDates = selectedPickupDates.toList()
        )
        listener?.onFiltersApplied(filterState)
    }

    private fun createFilterChip(label: String, value: String, isSelected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = label
            tag = value
            setPadding(
                dpToPx(16), dpToPx(8),
                dpToPx(16), dpToPx(8)
            )
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(8), dpToPx(8))
            layoutParams = lp
            textSize = 13f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)

            updateChipState(this, isSelected)
        }
    }

    private fun updateChipState(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_filter_option_selected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_option)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setFilterListener(listener: FilterListener) {
        this.listener = listener
    }

    companion object {
        const val TAG = "FilterDialogFragment"

        private const val ARG_PAYMENT_STATUSES = "payment_statuses"
        private const val ARG_ORDER_STATUSES = "order_statuses"
        private const val ARG_PAYMENT_TYPES = "payment_types"
        private const val ARG_CATEGORIES_SELECTED = "categories_selected"
        private const val ARG_EMPLOYEES_SELECTED = "employees_selected"
        private const val ARG_EMPLOYEES = "employees"
        private const val ARG_CATEGORIES = "categories"

        fun newInstance(
            currentFilters: FilterState = FilterState(),
            employees: List<String> = emptyList(),
            categories: List<String> = emptyList()
        ): FilterDialogFragment {
            return FilterDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_PAYMENT_STATUSES, ArrayList(currentFilters.paymentStatuses))
                    putStringArrayList(ARG_ORDER_STATUSES, ArrayList(currentFilters.orderStatuses))
                    putStringArrayList(ARG_PAYMENT_TYPES, ArrayList(currentFilters.paymentTypes))
                    putStringArrayList(ARG_CATEGORIES_SELECTED, ArrayList(currentFilters.categories))
                    putStringArrayList(ARG_EMPLOYEES_SELECTED, ArrayList(currentFilters.employees))
                    putStringArrayList(ARG_EMPLOYEES, ArrayList(employees))
                    putStringArrayList(ARG_CATEGORIES, ArrayList(categories))
                }
            }
        }
    }
}
