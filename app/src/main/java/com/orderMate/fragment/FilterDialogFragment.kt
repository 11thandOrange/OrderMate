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
import com.orderMate.modals.NoteLevel
import com.orderMate.utils.FilterCategoryBuilder
import com.orderMate.utils.FilterCategoryBuilder.FilterCategory
import com.orderMate.utils.FilterCategoryBuilder.FilterOption
import com.orderMate.utils.FilterCategoryBuilder.FilterType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter dialog for order list
 * 
 * Dynamically builds filter sections from:
 * 1. Clover data (payment status, order status, payment type, employee)
 * 2. OrderMate widgets (where showInFilter=true)
 * 
 * Categories are passed via FilterCategoryBuilder.buildCategories()
 */
class FilterDialogFragment : DialogFragment() {

    private var _binding: DialogFiltersBinding? = null
    private val binding get() = _binding!!

    private var listener: FilterListener? = null

    // Dynamic filter categories (built from Clover + OrderMate widgets)
    private var filterCategories: List<FilterCategory> = emptyList()

    // Current filter selections: categoryId -> selected values
    private val selections = mutableMapOf<String, MutableSet<String>>()

    // Date selections: categoryId -> selected dates
    private val dateSelections = mutableMapOf<String, MutableList<Date>>()

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    interface FilterListener {
        fun onFiltersApplied(filters: FilterState)
        fun onFilterCleared()
    }

    /**
     * Filter state using dynamic category IDs
     */
    data class FilterState(
        val selections: Map<String, Set<String>> = emptyMap(),      // categoryId -> selected values
        val dateSelections: Map<String, List<Date>> = emptyMap()    // categoryId -> selected dates
    ) {
        fun hasActiveFilters(): Boolean {
            return selections.values.any { it.isNotEmpty() } ||
                   dateSelections.values.any { it.isNotEmpty() }
        }

        fun getActiveFilterCount(): Int {
            return selections.values.sumOf { it.size } +
                   dateSelections.values.sumOf { it.size }
        }
        
        fun getSelectedValues(categoryId: String): Set<String> {
            return selections[categoryId] ?: emptySet()
        }
        
        fun getSelectedDates(categoryId: String): List<Date> {
            return dateSelections[categoryId] ?: emptyList()
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

        // Build dynamic filter sections
        buildFilterSections()
    }

    /**
     * Build all filter sections dynamically from filterCategories
     * Adds section dividers between Clover filters, Order Level widgets, and Item Level widgets
     */
    private fun buildFilterSections() {
        // Clear existing dynamic sections (keep the static container)
        binding.filterSectionsContainer.removeAllViews()

        // Separate categories by source and level
        val cloverCategories = filterCategories.filter { it.source == FilterCategoryBuilder.FilterSource.CLOVER }
        val orderLevelCategories = filterCategories.filter { it.level == NoteLevel.ORDER }
        val itemLevelCategories = filterCategories.filter { it.level == NoteLevel.ITEM }

        // Build Clover filter sections
        cloverCategories.forEach { category ->
            addCategorySection(category)
        }

        // Build Order Level widget sections with divider
        if (orderLevelCategories.isNotEmpty()) {
            addSectionDivider("Order Level")
            orderLevelCategories.forEach { category ->
                addCategorySection(category)
            }
        }

        // Build Item Level widget sections with divider
        if (itemLevelCategories.isNotEmpty()) {
            addSectionDivider("Item Level")
            itemLevelCategories.forEach { category ->
                addCategorySection(category)
            }
        }
    }

    /**
     * Add a category section based on its type
     */
    private fun addCategorySection(category: FilterCategory) {
        when (category.type) {
            FilterType.MULTI_SELECT -> {
                if (category.options.isNotEmpty()) {
                    addMultiSelectSection(category)
                }
            }
            FilterType.DATE_PICKER -> {
                addDatePickerSection(category)
            }
        }
    }

    /**
     * Add a section divider with label (e.g., "Order Level" or "Item Level")
     */
    private fun addSectionDivider(label: String) {
        val dividerView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(16), 0, dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Left line
        val leftLine = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(1), 1f)
            setBackgroundColor(0x33FFFFFF)
        }
        dividerView.addView(leftLine)

        // Label
        val labelView = TextView(requireContext()).apply {
            text = label.uppercase()
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textSize = 11f
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
        }
        dividerView.addView(labelView)

        // Right line
        val rightLine = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(1), 1f)
            setBackgroundColor(0x33FFFFFF)
        }
        dividerView.addView(rightLine)

        binding.filterSectionsContainer.addView(dividerView)
    }

    /**
     * Add a multi-select filter section
     */
    private fun addMultiSelectSection(category: FilterCategory) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.filter_section_multiselect, binding.filterSectionsContainer, false)

        // Set section label
        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = category.label

        // Setup options
        val optionsContainer = sectionView.findViewById<FlexboxLayout>(R.id.optionsContainer)
        setupMultiSelectOptions(optionsContainer, category)

        binding.filterSectionsContainer.addView(sectionView)
    }

    /**
     * Add a date picker filter section
     */
    private fun addDatePickerSection(category: FilterCategory) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.filter_section_date, binding.filterSectionsContainer, false)

        // Set section label
        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = category.label

        // Setup date input
        val dateInput = sectionView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateInput)
        val dateChips = sectionView.findViewById<FlexboxLayout>(R.id.dateChips)

        dateInput.setOnClickListener { showDatePicker(category.id, dateInput, dateChips) }

        // Show existing selections
        updateDateChipsForCategory(category.id, dateChips, dateInput)

        binding.filterSectionsContainer.addView(sectionView)
    }

    /**
     * Setup multi-select options in a FlexboxLayout
     */
    private fun setupMultiSelectOptions(container: FlexboxLayout, category: FilterCategory) {
        container.removeAllViews()

        // Initialize selections set for this category if not exists
        if (!selections.containsKey(category.id)) {
            selections[category.id] = mutableSetOf()
        }
        val selectedValues = selections[category.id]!!

        category.options.forEach { option ->
            val isSelected = selectedValues.contains(option.value)
            val chip = createFilterChip(option.label, option.value, isSelected)
            
            chip.setOnClickListener {
                if (selectedValues.contains(option.value)) {
                    selectedValues.remove(option.value)
                    updateChipState(chip, false)
                } else {
                    selectedValues.add(option.value)
                    updateChipState(chip, true)
                }
                applyFiltersImmediately()
            }
            
            container.addView(chip)
        }
    }

    /**
     * Show date picker for a specific category
     */
    private fun showDatePicker(
        categoryId: String, 
        dateInput: com.google.android.material.textfield.TextInputEditText,
        dateChips: FlexboxLayout
    ) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_DatePicker,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val date = calendar.time

                // Initialize date list for this category if not exists
                if (!dateSelections.containsKey(categoryId)) {
                    dateSelections[categoryId] = mutableListOf()
                }
                val dates = dateSelections[categoryId]!!

                if (!dates.any { isSameDay(it, date) }) {
                    dates.add(date)
                    dateInput.setText(dateFormat.format(date))
                }
                
                updateDateChipsForCategory(categoryId, dateChips, dateInput)
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

    /**
     * Update date chips for a specific category
     */
    private fun updateDateChipsForCategory(
        categoryId: String,
        dateChips: FlexboxLayout,
        dateInput: com.google.android.material.textfield.TextInputEditText
    ) {
        dateChips.removeAllViews()
        
        val dates = dateSelections[categoryId] ?: return

        dates.forEach { date ->
            val chip = createDateChip(dateFormat.format(date)) {
                dates.remove(date)
                updateDateChipsForCategory(categoryId, dateChips, dateInput)
                if (dates.isEmpty()) {
                    dateInput.text?.clear()
                }
                applyFiltersImmediately()
            }
            dateChips.addView(chip)
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

    private fun applyFiltersImmediately() {
        val filterState = FilterState(
            selections = selections.mapValues { it.value.toSet() },
            dateSelections = dateSelections.mapValues { it.value.toList() }
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

    /**
     * Set the filter categories (must be called before showing dialog)
     */
    fun setFilterCategories(categories: List<FilterCategory>) {
        this.filterCategories = categories
    }

    /**
     * Restore previous filter selections
     */
    fun setCurrentFilters(filterState: FilterState) {
        selections.clear()
        filterState.selections.forEach { (key, values) ->
            selections[key] = values.toMutableSet()
        }
        
        dateSelections.clear()
        filterState.dateSelections.forEach { (key, dates) ->
            dateSelections[key] = dates.toMutableList()
        }
    }

    companion object {
        const val TAG = "FilterDialogFragment"

        /**
         * Create a new filter dialog with dynamic categories
         * 
         * @param categories Filter categories built from FilterCategoryBuilder
         * @param currentFilters Current filter state to restore
         */
        fun newInstance(
            categories: List<FilterCategory>,
            currentFilters: FilterState = FilterState()
        ): FilterDialogFragment {
            return FilterDialogFragment().apply {
                filterCategories = categories
                
                // Restore selections
                currentFilters.selections.forEach { (key, values) ->
                    selections[key] = values.toMutableSet()
                }
                currentFilters.dateSelections.forEach { (key, dates) ->
                    dateSelections[key] = dates.toMutableList()
                }
            }
        }
    }
}
