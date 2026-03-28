package com.orderMate.fragment.orderDetail

import android.app.DatePickerDialog
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
import com.google.android.material.textfield.TextInputEditText
import com.orderMate.R
import com.orderMate.databinding.DialogItemNoteBinding
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog for adding/editing notes on line items
 * 
 * Displays widgets dynamically based on WidgetConfig list:
 * - SINGLE_SELECT: Chips with radio-like behavior (one selection)
 * - MULTI_SELECT: Chips with checkbox-like behavior (multiple selections)
 * - CALENDAR: Date picker input
 * - TEXT_BOX: Free-form text input
 */
class ItemNoteDialogFragment : DialogFragment() {

    private var _binding: DialogItemNoteBinding? = null
    private val binding get() = _binding!!

    private var widgets: List<WidgetConfig> = emptyList()
    private var listener: ItemNoteListener? = null
    private var lineItemId: String? = null
    private var existingNote: String? = null

    // Selections: widgetId -> selected values
    private val singleSelections = mutableMapOf<String, String?>()
    private val multiSelections = mutableMapOf<String, MutableSet<String>>()
    private val dateSelections = mutableMapOf<String, String?>()
    private val textSelections = mutableMapOf<String, String?>()

    // Track view references for collecting data
    private val textInputViews = mutableMapOf<String, TextInputEditText>()

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    interface ItemNoteListener {
        fun onNoteSaved(lineItemId: String?, note: String)
        fun onNoteCancelled()
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
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val maxWidthPx = (520 * displayMetrics.density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            val maxHeightPx = (displayMetrics.heightPixels * 0.85).toInt()

            window.setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
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
        _binding = DialogItemNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        buildNoteSections()
        parseExistingNote()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            listener?.onNoteCancelled()
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val note = buildNoteString()
            listener?.onNoteSaved(lineItemId, note)
            dismiss()
        }
    }

    /**
     * Build all widget sections dynamically
     */
    private fun buildNoteSections() {
        binding.noteSectionsContainer.removeAllViews()

        widgets.filter { it.isEnabled }.sortedBy { it.order }.forEach { widget ->
            when (widget.type) {
                WidgetType.SINGLE_SELECT -> addSingleSelectSection(widget)
                WidgetType.MULTI_SELECT -> addMultiSelectSection(widget)
                WidgetType.CALENDAR -> addCalendarSection(widget)
                WidgetType.TEXT_BOX -> addTextBoxSection(widget)
            }
        }
    }

    /**
     * Add SINGLE_SELECT section - radio-like chip selection
     */
    private fun addSingleSelectSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_select, binding.noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val optionsContainer = sectionView.findViewById<FlexboxLayout>(R.id.optionsContainer)
        setupSingleSelectOptions(optionsContainer, widget)

        binding.noteSectionsContainer.addView(sectionView)
    }

    /**
     * Add MULTI_SELECT section - checkbox-like chip selection
     */
    private fun addMultiSelectSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_select, binding.noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val optionsContainer = sectionView.findViewById<FlexboxLayout>(R.id.optionsContainer)
        setupMultiSelectOptions(optionsContainer, widget)

        binding.noteSectionsContainer.addView(sectionView)
    }

    /**
     * Add CALENDAR section - date picker
     */
    private fun addCalendarSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_calendar, binding.noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val dateInput = sectionView.findViewById<TextInputEditText>(R.id.dateInput)
        dateInput.setOnClickListener { showDatePicker(widget.id, dateInput) }

        // Restore existing selection
        dateSelections[widget.id]?.let { dateInput.setText(it) }

        binding.noteSectionsContainer.addView(sectionView)
    }

    /**
     * Add TEXT_BOX section - free-form text input
     */
    private fun addTextBoxSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_textbox, binding.noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val textInput = sectionView.findViewById<TextInputEditText>(R.id.textInput)
        textInputViews[widget.id] = textInput

        // Restore existing text
        textSelections[widget.id]?.let { textInput.setText(it) }

        binding.noteSectionsContainer.addView(sectionView)
    }

    /**
     * Setup SINGLE_SELECT chips - only one can be selected
     */
    private fun setupSingleSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()

        // Initialize selection
        if (!singleSelections.containsKey(widget.id)) {
            singleSelections[widget.id] = null
        }

        val chipViews = mutableListOf<TextView>()

        widget.options.forEach { option ->
            val isSelected = singleSelections[widget.id] == option.value
            val chip = createChip(option.label, option.value, isSelected)
            chipViews.add(chip)

            chip.setOnClickListener {
                // Deselect all other chips
                chipViews.forEach { other -> updateChipState(other, false) }
                
                // Toggle this chip
                val wasSelected = singleSelections[widget.id] == option.value
                if (wasSelected) {
                    singleSelections[widget.id] = null
                    updateChipState(chip, false)
                } else {
                    singleSelections[widget.id] = option.value
                    updateChipState(chip, true)
                }
            }

            container.addView(chip)
        }
    }

    /**
     * Setup MULTI_SELECT chips - multiple can be selected
     */
    private fun setupMultiSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()

        // Initialize selections set
        if (!multiSelections.containsKey(widget.id)) {
            multiSelections[widget.id] = mutableSetOf()
        }
        val selectedValues = multiSelections[widget.id]!!

        widget.options.forEach { option ->
            val isSelected = selectedValues.contains(option.value)
            val chip = createChip(option.label, option.value, isSelected)

            chip.setOnClickListener {
                if (selectedValues.contains(option.value)) {
                    selectedValues.remove(option.value)
                    updateChipState(chip, false)
                } else {
                    selectedValues.add(option.value)
                    updateChipState(chip, true)
                }
            }

            container.addView(chip)
        }
    }

    /**
     * Show date picker dialog
     */
    private fun showDatePicker(widgetId: String, dateInput: TextInputEditText) {
        val calendar = Calendar.getInstance()

        // If there's an existing date, parse it
        dateSelections[widgetId]?.let { existing ->
            try {
                dateFormat.parse(existing)?.let { calendar.time = it }
            } catch (e: Exception) { /* ignore */ }
        }

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_DatePicker,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val formattedDate = dateFormat.format(calendar.time)
                dateSelections[widgetId] = formattedDate
                dateInput.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Create a chip view
     */
    private fun createChip(label: String, value: String, isSelected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = label
            tag = value
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            
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

    /**
     * Update chip visual state
     */
    private fun updateChipState(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_filter_option_selected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_option)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light))
        }
    }

    /**
     * Build the note string from all selections
     */
    private fun buildNoteString(): String {
        val parts = mutableListOf<String>()

        widgets.filter { it.isEnabled }.sortedBy { it.order }.forEach { widget ->
            when (widget.type) {
                WidgetType.SINGLE_SELECT -> {
                    singleSelections[widget.id]?.let { value ->
                        parts.add("${widget.label}:$value")
                    }
                }
                WidgetType.MULTI_SELECT -> {
                    multiSelections[widget.id]?.let { values ->
                        if (values.isNotEmpty()) {
                            parts.add("${widget.label}:${values.joinToString(",")}")
                        }
                    }
                }
                WidgetType.CALENDAR -> {
                    dateSelections[widget.id]?.let { value ->
                        if (value.isNotEmpty()) {
                            parts.add("${widget.label}:$value")
                        }
                    }
                }
                WidgetType.TEXT_BOX -> {
                    val value = textInputViews[widget.id]?.text?.toString()?.trim()
                    if (!value.isNullOrEmpty()) {
                        parts.add("${widget.label}:$value")
                    }
                }
            }
        }

        return parts.joinToString(" | ")
    }

    /**
     * Parse existing note and populate selections
     */
    private fun parseExistingNote() {
        if (existingNote.isNullOrEmpty()) return

        // Parse format: "Label:Value | Label:Value1,Value2 | ..."
        existingNote?.split("|")?.forEach { part ->
            val trimmed = part.trim()
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val label = trimmed.substring(0, colonIndex).trim()
                val value = trimmed.substring(colonIndex + 1).trim()

                // Find widget by label
                val widget = widgets.find { it.label.equals(label, ignoreCase = true) }
                widget?.let {
                    when (it.type) {
                        WidgetType.SINGLE_SELECT -> {
                            singleSelections[it.id] = value
                        }
                        WidgetType.MULTI_SELECT -> {
                            val values = value.split(",").map { v -> v.trim() }.toMutableSet()
                            multiSelections[it.id] = values
                        }
                        WidgetType.CALENDAR -> {
                            dateSelections[it.id] = value
                        }
                        WidgetType.TEXT_BOX -> {
                            textSelections[it.id] = value
                        }
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setListener(listener: ItemNoteListener) {
        this.listener = listener
    }

    fun setWidgets(widgets: List<WidgetConfig>) {
        this.widgets = widgets
    }

    fun setLineItemId(id: String?) {
        this.lineItemId = id
    }

    fun setExistingNote(note: String?) {
        this.existingNote = note
    }

    companion object {
        const val TAG = "ItemNoteDialogFragment"

        fun newInstance(
            widgets: List<WidgetConfig>,
            lineItemId: String? = null,
            existingNote: String? = null
        ): ItemNoteDialogFragment {
            return ItemNoteDialogFragment().apply {
                this.widgets = widgets
                this.lineItemId = lineItemId
                this.existingNote = existingNote
            }
        }
    }
}
