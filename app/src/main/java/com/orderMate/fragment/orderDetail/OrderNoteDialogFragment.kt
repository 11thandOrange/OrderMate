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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.textfield.TextInputEditText
import com.orderMate.R
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog for adding/editing notes at the order level (#93 requirement)
 * 
 * Similar to ItemNoteDialogFragment but:
 * - Uses order-level widgets (level = ORDER)
 * - Saves to Order.note field (not LineItem.note)
 * - Different color scheme (purple accent)
 */
class OrderNoteDialogFragment : DialogFragment() {

    private var noteSectionsContainer: LinearLayout? = null
    private var btnCancel: View? = null
    private var btnSave: View? = null

    private var widgets: List<WidgetConfig> = emptyList()
    private var listener: OrderNoteListener? = null
    private var orderId: String? = null
    private var existingNote: String? = null

    private val singleSelections = mutableMapOf<String, String?>()
    private val multiSelections = mutableMapOf<String, MutableSet<String>>()
    private val dateSelections = mutableMapOf<String, String?>()
    private val textSelections = mutableMapOf<String, String?>()
    private val textInputViews = mutableMapOf<String, TextInputEditText>()

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    interface OrderNoteListener {
        fun onOrderNoteSaved(orderId: String?, note: String)
        fun onOrderNoteCancelled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
        
        arguments?.let { args ->
            orderId = args.getString(ARG_ORDER_ID)
            existingNote = args.getString(ARG_EXISTING_NOTE)
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
    ): View? {
        return inflater.inflate(R.layout.dialog_order_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteSectionsContainer = view.findViewById(R.id.noteSectionsContainer)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)

        // Read order-level widgets only
        widgets = WidgetManager.getInstance(requireContext()).getOrderLevelWidgets()
        
        setupButtons()
        // Task 10: Parse existing note BEFORE building UI so selections are pre-populated
        parseExistingNote()
        buildNoteSections()
    }

    private fun setupButtons() {
        btnCancel?.setOnClickListener {
            listener?.onOrderNoteCancelled()
            dismiss()
        }

        btnSave?.setOnClickListener {
            val note = buildNoteString()
            listener?.onOrderNoteSaved(orderId, note)
            dismiss()
        }
    }

    private fun buildNoteSections() {
        noteSectionsContainer?.removeAllViews()

        val enabledWidgets = widgets.filter { it.isEnabled }.sortedBy { it.order }
        
        if (enabledWidgets.isEmpty()) {
            addEmptyStateMessage()
            btnSave?.isEnabled = false
            btnSave?.alpha = 0.5f
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
    
    private fun addEmptyStateMessage() {
        val emptyView = TextView(requireContext()).apply {
            text = "No order-level widgets enabled. Update Order Level Notes settings."
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dpToPx(48), 0, dpToPx(48))
        }
        noteSectionsContainer?.addView(emptyView)
    }

    private fun addSingleSelectSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_select, noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val optionsContainer = sectionView.findViewById<FlexboxLayout>(R.id.optionsContainer)
        setupSingleSelectOptions(optionsContainer, widget)

        noteSectionsContainer?.addView(sectionView)
    }

    private fun addMultiSelectSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_select, noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val optionsContainer = sectionView.findViewById<FlexboxLayout>(R.id.optionsContainer)
        setupMultiSelectOptions(optionsContainer, widget)

        noteSectionsContainer?.addView(sectionView)
    }

    private fun addCalendarSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_calendar, noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val dateInput = sectionView.findViewById<TextInputEditText>(R.id.dateInput)
        dateSelections[widget.id]?.let { dateInput.setText(it) }
        dateInput.setOnClickListener { showDatePicker(widget.id, dateInput) }

        noteSectionsContainer?.addView(sectionView)
    }

    private fun addTextBoxSection(widget: WidgetConfig) {
        val sectionView = LayoutInflater.from(requireContext())
            .inflate(R.layout.note_section_textbox, noteSectionsContainer, false)

        val labelView = sectionView.findViewById<TextView>(R.id.sectionLabel)
        labelView.text = widget.label

        val textInput = sectionView.findViewById<TextInputEditText>(R.id.textInput)
        textSelections[widget.id]?.let { textInput.setText(it) }
        textInputViews[widget.id] = textInput

        noteSectionsContainer?.addView(sectionView)
    }

    private fun setupSingleSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()
        val widgetColor = WidgetColorUtils.getColorForWidgetType(widget.type)

        widget.options.forEach { option ->
            val isSelected = singleSelections[widget.id] == option.value
            val chip = createChip(option.label, option.value, isSelected, widgetColor)

            chip.setOnClickListener {
                container.children.forEach { child ->
                    if (child is TextView) updateChipState(child, false, widgetColor)
                }
                
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

    private fun setupMultiSelectOptions(container: FlexboxLayout, widget: WidgetConfig) {
        container.removeAllViews()

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

    private fun showDatePicker(widgetId: String, dateInput: TextInputEditText) {
        val calendar = Calendar.getInstance()

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

    private fun parseExistingNote() {
        if (existingNote.isNullOrEmpty()) return

        val delimiter = if (existingNote!!.contains("•")) "•" else "|"
        existingNote?.split(delimiter)?.forEach { part ->
            val trimmed = part.trim()
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val label = trimmed.substring(0, colonIndex).trim()
                val value = trimmed.substring(colonIndex + 1).trim()

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

    private val ViewGroup.children: Sequence<View>
        get() = (0 until childCount).asSequence().map { getChildAt(it) }

    override fun onDestroyView() {
        super.onDestroyView()
        noteSectionsContainer = null
        btnCancel = null
        btnSave = null
    }

    fun setListener(listener: OrderNoteListener) {
        this.listener = listener
    }

    fun setOrderId(id: String?) {
        this.orderId = id
    }

    fun setExistingNote(note: String?) {
        this.existingNote = note
    }

    companion object {
        const val TAG = "OrderNoteDialogFragment"
        private const val ARG_ORDER_ID = "arg_order_id"
        private const val ARG_EXISTING_NOTE = "arg_existing_note"

        fun newInstance(
            orderId: String? = null,
            existingNote: String? = null
        ): OrderNoteDialogFragment {
            return OrderNoteDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_EXISTING_NOTE, existingNote)
                }
            }
        }
    }
}
