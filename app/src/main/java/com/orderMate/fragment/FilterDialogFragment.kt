package com.orderMate.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.databinding.DialogFiltersBinding

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
    private var selectedBookingType: String = "all"
    private var selectedEmployee: String = "all"

    // Available employees (passed from fragment)
    private var availableEmployees: List<String> = emptyList()

    interface FilterListener {
        fun onFiltersApplied(filters: FilterState)
        fun onFilterCleared()
    }

    data class FilterState(
        val paymentStatus: String = "all",
        val orderStatus: String = "all",
        val paymentType: String = "all",
        val bookingType: String = "all",
        val employee: String = "all"
    ) {
        fun hasActiveFilters(): Boolean {
            return paymentStatus != "all" || orderStatus != "all" || 
                   paymentType != "all" || bookingType != "all" || employee != "all"
        }

        fun getActiveFilterCount(): Int {
            var count = 0
            if (paymentStatus != "all") count++
            if (orderStatus != "all") count++
            if (paymentType != "all") count++
            if (bookingType != "all") count++
            if (employee != "all") count++
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
            selectedBookingType = args.getString(ARG_BOOKING_TYPE, "all")
            selectedEmployee = args.getString(ARG_EMPLOYEE, "all")
            availableEmployees = args.getStringArrayList(ARG_EMPLOYEES) ?: emptyList()
        }

        setupFilterOptions()
        setupButtons()
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

        // Booking Type options
        setupFilterSection(
            binding.bookingTypeOptions,
            listOf("All" to "all", "Pickup" to "pickup", "Delivery" to "delivery", "Preorder" to "preorder"),
            selectedBookingType
        ) { value -> selectedBookingType = value }

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
        selectedBookingType = "all"
        selectedEmployee = "all"

        // Reset UI
        setupFilterOptions()

        listener?.onFilterCleared()
        dismiss()
    }

    private fun applyFilters() {
        val filterState = FilterState(
            paymentStatus = selectedPaymentStatus,
            orderStatus = selectedOrderStatus,
            paymentType = selectedPaymentType,
            bookingType = selectedBookingType,
            employee = selectedEmployee
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
        private const val ARG_BOOKING_TYPE = "booking_type"
        private const val ARG_EMPLOYEE = "employee"
        private const val ARG_EMPLOYEES = "employees"

        fun newInstance(
            currentFilters: FilterState = FilterState(),
            employees: List<String> = emptyList()
        ): FilterDialogFragment {
            return FilterDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PAYMENT_STATUS, currentFilters.paymentStatus)
                    putString(ARG_ORDER_STATUS, currentFilters.orderStatus)
                    putString(ARG_PAYMENT_TYPE, currentFilters.paymentType)
                    putString(ARG_BOOKING_TYPE, currentFilters.bookingType)
                    putString(ARG_EMPLOYEE, currentFilters.employee)
                    putStringArrayList(ARG_EMPLOYEES, ArrayList(employees))
                }
            }
        }
    }
}
