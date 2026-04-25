package com.orderMate.fragment.orderDetail

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
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
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
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
    private var itemName: String? = null
    private var itemModifiers: String? = null
    private var itemQuantity: Int = 1

    // Selections: widgetId -> selected values
    private val singleSelections = mutableMapOf<String, String?>()
    private val multiSelections = mutableMapOf<String, MutableSet<String>>()
    private val dateSelections = mutableMapOf<String, String?>()
    private val textSelections = mutableMapOf<String, String?>()

    // Track view references for collecting data
    private val textInputViews = mutableMapOf<String, TextInputEditText>()

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    interface ItemNoteListener {
        fun onNoteSaved(lineItemId: String?, note: String)
        fun onNoteCancelled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
        
        // Restore simple arguments
        arguments?.let { args ->
            lineItemId = args.getString(ARG_LINE_ITEM_ID)
            existingNote = args.getString(ARG_EXISTING_NOTE)
            itemName = args.getString(ARG_ITEM_NAME)
            itemModifiers = args.getString(ARG_ITEM_MODIFIERS)
            itemQuantity = args.getInt(ARG_ITEM_QUANTITY, 1)
        }
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

        // Read item-level widgets only (enabled widgets)
        widgets = WidgetManager.getInstance(requireContext()).getEnabledItemWidgets()
        
        setupHeader()
        setupButtons()
        // Task 10: Parse existing note BEFORE building UI so selections are pre-populated
        parseExistingNote()
        buildNoteSections()
    }
    
    private fun setupHeader() {
        // Set quantity badge
        binding.dialogQtyBadge.text = "x$itemQuantity"
        
        // Set item name
        binding.dialogTitle.text = itemName ?: "Item"
        
        // Show modifiers if available, hide subtitle if empty
        if (itemModifiers.isNullOrBlank()) {
            binding.dialogSubtitle.visibility = View.GONE
        } else {
            binding.dialogSubtitle.text = itemModifiers
            binding.dialogSubtitle.visibility = View.VISIBLE
        }
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

        val enabledWidgets = widgets.filter { it.isEnabled }.sortedBy { it.order }
        
        if (enabledWidgets.isEmpty()) {
            // Show empty state message and disable save button
            addEmptyStateMessage()
            binding.btnSave.isEnabled = false
            binding.btnSave.alpha = 0.5f
            return
        }
        
        enabledWidgets.forEach { widget ->
            when (widget.type) {
                WidgetType.SINGLE_SELECT -> addSingleSelectSection(widget)
                WidgetType.MULTI_SELECT -> addMultiSelectSection(widget)
                WidgetType.CALENDAR -> addCalendarSection(widget)
                WidgetType.TEXT_BOX -> addTextBoxSection(widget)
            }
        }
    }
    
    /**
     * Show empty state when no widgets are enabled
     */
    private fun addEmptyStateMessage() {
        val emptyView = TextView(requireContext()).apply {
            text = "No item-level widgets enabled. Update Item Level Notes settings."
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dpToPx(48), 0, dpToPx(48))
        }
        binding.noteSectionsContainer.addView(emptyView)
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

        // Set label from DB widget config
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

        // Set label from DB widget config
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
     * Uses purple color coding via WidgetColorUtils
     */
    private fun setupSingleSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()

        // Initialize selection
        if (!singleSelections.containsKey(widget.id)) {
            singleSelections[widget.id] = null
        }

        val chipViews = mutableListOf<TextView>()
        val widgetColor = WidgetColorUtils.getColorForWidgetType(widget.type)

        widget.options.forEach { option ->
            val isSelected = singleSelections[widget.id] == option.value
            val chip = createChip(option.label, option.value, isSelected, widgetColor)
            chipViews.add(chip)

            chip.setOnClickListener {
                // Deselect all other chips
                chipViews.forEach { other -> updateChipState(other, false, widgetColor) }
                
                // Toggle this chip
                val wasSelected = singleSelections[widget.id] == option.value
                if (wasSelected) {
                    singleSelections[widget.id] = null
                    updateChipState(chip, false, widgetColor)
                } else {
                    singleSelections[widget.id] = option.value
                    updateChipState(chip, true, widgetColor)
                }
            }

            container.addView(chip)
        }
    }

    /**
     * Setup MULTI_SELECT chips - multiple can be selected
     * Uses green color coding via WidgetColorUtils
     */
    private fun setupMultiSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()

        // Initialize selections set
        if (!multiSelections.containsKey(widget.id)) {
            multiSelections[widget.id] = mutableSetOf()
        }
        val selectedValues = multiSelections[widget.id]!!
        val widgetColor = WidgetColorUtils.getColorForWidgetType(widget.type)

        widget.options.forEach { option ->
            val isSelected = selectedValues.contains(option.value)
            val chip = createChip(option.label, option.value, isSelected, widgetColor)

            chip.setOnClickListener {
                if (selectedValues.contains(option.value)) {
                    selectedValues.remove(option.value)
                    updateChipState(chip, false, widgetColor)
                } else {
                    selectedValues.add(option.value)
                    updateChipState(chip, true, widgetColor)
                }
            }

            container.addView(chip)
        }
    }

    /**
     * Show date and time picker dialog (date first, then time)
     */
    private fun showDatePicker(widgetId: String, dateInput: TextInputEditText) {
        val calendar = Calendar.getInstance()

        // If there's an existing datetime, parse it
        dateSelections[widgetId]?.let { existing ->
            try {
                dateTimeFormat.parse(existing)?.let { calendar.time = it }
            } catch (e: Exception) {
                try {
                    dateFormat.parse(existing)?.let { calendar.time = it }
                } catch (e2: Exception) { /* ignore */ }
            }
        }

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_DatePicker,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                // After date selection, show time picker
                showTimePicker(widgetId, dateInput, calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    /**
     * Show time picker dialog after date selection
     */
    private fun showTimePicker(widgetId: String, dateInput: TextInputEditText, calendar: Calendar) {
        TimePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_DatePicker,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                
                val formattedDateTime = dateTimeFormat.format(calendar.time)
                dateSelections[widgetId] = formattedDateTime
                dateInput.setText(formattedDateTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // 12-hour format
        ).show()
    }

    /**
     * Create a chip view with widget-specific color coding
     */
    private fun createChip(label: String, value: String, isSelected: Boolean, widgetColor: Int): TextView {
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

            updateChipState(this, isSelected, widgetColor)
        }
    }

    /**
     * Update chip visual state with widget-specific color coding
     */
    private fun updateChipState(chip: TextView, isSelected: Boolean, widgetColor: Int) {
        val density = resources.displayMetrics.density
        if (isSelected) {
            chip.background = WidgetColorUtils.createChipBackground(widgetColor, true, 8f, density)
            chip.setTextColor(widgetColor)
        } else {
            chip.background = WidgetColorUtils.createChipBackground(widgetColor, false, 8f, density)
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

        return parts.joinToString(" • ")
    }

    /**
     * Parse existing note and populate selections
     */
    private fun parseExistingNote() {
        if (existingNote.isNullOrEmpty()) return

        // Parse format: "Label:Value • Label:Value" (or legacy "|" delimiter)
        val delimiter = if (existingNote!!.contains("•")) "•" else "|"
        existingNote?.split(delimiter)?.forEach { part ->
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

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        // Called when dialog is dismissed by clicking outside or pressing back
        listener?.onNoteCancelled()
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
    
    fun setItemName(name: String?) {
        this.itemName = name
    }
    
    fun setItemModifiers(modifiers: String?) {
        this.itemModifiers = modifiers
    }
    
    fun setItemQuantity(quantity: Int) {
        this.itemQuantity = quantity
    }

    companion object {
        const val TAG = "ItemNoteDialogFragment"
        private const val ARG_LINE_ITEM_ID = "arg_line_item_id"
        private const val ARG_EXISTING_NOTE = "arg_existing_note"
        private const val ARG_ITEM_NAME = "arg_item_name"
        private const val ARG_ITEM_MODIFIERS = "arg_item_modifiers"
        private const val ARG_ITEM_QUANTITY = "arg_item_quantity"

        fun newInstance(
            lineItemId: String? = null,
            existingNote: String? = null,
            itemName: String? = null,
            itemModifiers: String? = null,
            itemQuantity: Int = 1
        ): ItemNoteDialogFragment {
            return ItemNoteDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LINE_ITEM_ID, lineItemId)
                    putString(ARG_EXISTING_NOTE, existingNote)
                    putString(ARG_ITEM_NAME, itemName)
                    putString(ARG_ITEM_MODIFIERS, itemModifiers)
                    putInt(ARG_ITEM_QUANTITY, itemQuantity)
                }
            }
        }
    }
}
