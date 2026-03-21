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

    // Current filter selections
    private var selectedPaymentStatus: String = "all"
    private var selectedOrderStatus: String = "all"
    private var selectedPaymentType: String = "all"
    private var selectedCategory: String = "all"
    private var selectedEmployee: String = "all"

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
        val paymentStatus: String = "all",
        val orderStatus: String = "all",
        val paymentType: String = "all",
        val category: String = "all",
        val employee: String = "all",
        val orderDates: List<Date> = emptyList(),
        val pickupDates: List<Date> = emptyList()
    ) {
        fun hasActiveFilters(): Boolean {
            return paymentStatus != "all" || orderStatus != "all" || 
                   paymentType != "all" || category != "all" || employee != "all" ||
                   orderDates.isNotEmpty() || pickupDates.isNotEmpty()
        }

        fun getActiveFilterCount(): Int {
            var count = 0
            if (paymentStatus != "all") count++
            if (orderStatus != "all") count++
            if (paymentType != "all") count++
            if (category != "all") count++
            if (employee != "all") count++
            count += orderDates.size
            count += pickupDates.size
            return count
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
            selectedPaymentStatus = args.getString(ARG_PAYMENT_STATUS, "all")
            selectedOrderStatus = args.getString(ARG_ORDER_STATUS, "all")
            selectedPaymentType = args.getString(ARG_PAYMENT_TYPE, "all")
            selectedCategory = args.getString(ARG_CATEGORY, "all")
            selectedEmployee = args.getString(ARG_EMPLOYEE, "all")
            availableEmployees = args.getStringArrayList(ARG_EMPLOYEES) ?: emptyList()
            availableCategories = args.getStringArrayList(ARG_CATEGORIES) ?: emptyList()
        }

        setupDateInputs()
        setupFilterOptions()
        setupButtons()
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
            R.style.Theme_OrderMate_Dialog,
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
        // Payment Status options
        setupFilterSection(
            binding.paymentStatusOptions,
            listOf("All" to "all", "Paid" to "paid", "Unpaid" to "unpaid", "Refunded" to "refunded", "Partial" to "partial"),
            selectedPaymentStatus
        ) { value -> selectedPaymentStatus = value }

        // Order Status options
        setupFilterSection(
            binding.orderStatusOptions,
            listOf("All" to "all", "Open" to "open", "Closed" to "closed"),
            selectedOrderStatus
        ) { value -> selectedOrderStatus = value }

        // Payment Type options
        setupFilterSection(
            binding.paymentTypeOptions,
            listOf("All" to "all", "Card" to "card", "Cash" to "cash"),
            selectedPaymentType
        ) { value -> selectedPaymentType = value }

        // Category options
        val categoryOptions = mutableListOf("All" to "all")
        availableCategories.forEach { name ->
            categoryOptions.add(name to name)
        }
        setupFilterSection(
            binding.categoryOptions,
            categoryOptions,
            selectedCategory
        ) { value -> selectedCategory = value }

        // Employee options
        val employeeOptions = mutableListOf("All" to "all")
        availableEmployees.forEach { name ->
            employeeOptions.add(name to name)
        }
        setupFilterSection(
            binding.employeeOptions,
            employeeOptions,
            selectedEmployee
        ) { value -> selectedEmployee = value }
    }

    private fun setupFilterSection(
        container: FlexboxLayout,
        options: List<Pair<String, String>>,
        selectedValue: String,
        onSelect: (String) -> Unit
    ) {
        container.removeAllViews()

        options.forEach { (label, value) ->
            val chip = createFilterChip(label, value, value == selectedValue)
            chip.setOnClickListener {
                // Deselect all in this container
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i) as? TextView
                    child?.let { updateChipState(it, false) }
                }
                // Select this one
                updateChipState(chip, true)
                onSelect(value)
            }
            container.addView(chip)
        }
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

    private fun setupButtons() {
        binding.btnClearAll.setOnClickListener {
            clearAllFilters()
        }

        binding.btnApply.setOnClickListener {
            applyFilters()
        }
    }

    private fun clearAllFilters() {
        selectedPaymentStatus = "all"
        selectedOrderStatus = "all"
        selectedPaymentType = "all"
        selectedCategory = "all"
        selectedEmployee = "all"
        selectedOrderDates.clear()
        selectedPickupDates.clear()

        // Reset UI
        binding.orderDateInput.text?.clear()
        binding.pickupDateInput.text?.clear()
        setupFilterOptions()
        updateDateChips()

        listener?.onFilterCleared()
        dismiss()
    }

    private fun applyFilters() {
        val filterState = FilterState(
            paymentStatus = selectedPaymentStatus,
            orderStatus = selectedOrderStatus,
            paymentType = selectedPaymentType,
            category = selectedCategory,
            employee = selectedEmployee,
            orderDates = selectedOrderDates.toList(),
            pickupDates = selectedPickupDates.toList()
        )
        listener?.onFiltersApplied(filterState)
        dismiss()
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

        private const val ARG_PAYMENT_STATUS = "payment_status"
        private const val ARG_ORDER_STATUS = "order_status"
        private const val ARG_PAYMENT_TYPE = "payment_type"
        private const val ARG_CATEGORY = "category"
        private const val ARG_EMPLOYEE = "employee"
        private const val ARG_EMPLOYEES = "employees"
        private const val ARG_CATEGORIES = "categories"

        fun newInstance(
            currentFilters: FilterState = FilterState(),
            employees: List<String> = emptyList(),
            categories: List<String> = emptyList()
        ): FilterDialogFragment {
            return FilterDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PAYMENT_STATUS, currentFilters.paymentStatus)
                    putString(ARG_ORDER_STATUS, currentFilters.orderStatus)
                    putString(ARG_PAYMENT_TYPE, currentFilters.paymentType)
                    putString(ARG_CATEGORY, currentFilters.category)
                    putString(ARG_EMPLOYEE, currentFilters.employee)
                    putStringArrayList(ARG_EMPLOYEES, ArrayList(employees))
                    putStringArrayList(ARG_CATEGORIES, ArrayList(categories))
                }
            }
        }
    }
}
