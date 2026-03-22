package com.orderMate.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.utils.PopUpWidget
import com.orderMate.utils.SettingsManager
import com.orderMate.utils.WidgetType
import java.util.Collections

/**
 * Settings Fragment with Sub-tabs (#83 requirement)
 * 
 * Features:
 * - General: Enable OrderMate in Clover Register
 * - Pop Up: Widget editor with drag-and-drop
 * - Notification: Templates management
 * - Advanced: Scheduled notifications, receipt settings
 */
class SettingsFragment : Fragment() {

    private lateinit var settingsManager: SettingsManager
    
    // Sub-tabs
    private var tabGeneral: TextView? = null
    private var tabPopUp: TextView? = null
    private var tabNotification: TextView? = null
    private var tabAdvanced: TextView? = null
    
    // Panels
    private var panelGeneral: View? = null
    private var panelPopUp: View? = null
    private var panelNotification: View? = null
    private var panelAdvanced: View? = null
    
    // General Panel
    private var switchUseInCloverRegister: Switch? = null
    
    // Pop Up Panel
    private var widgetRecyclerView: RecyclerView? = null
    private var btnAddWidget: View? = null
    private var widgetAdapter: WidgetEditorAdapter? = null
    
    // Notification Panel
    private var templateRecyclerView: RecyclerView? = null
    private var btnAddTemplate: View? = null
    private var templateAdapter: NotificationTemplateAdapter? = null
    
    // Advanced Panel
    private var inputNotificationTime: EditText? = null
    private var spinnerNotificationUnit: Spinner? = null
    private var inputReceiptTime: EditText? = null
    private var spinnerReceiptUnit: Spinner? = null
    
    private var currentTab = "general"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_redesign, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsManager = SettingsManager(requireContext())
        
        initViews(view)
        setupSubTabs()
        setupGeneralPanel()
        setupPopUpPanel()
        setupNotificationPanel()
        setupAdvancedPanel()
        loadSettings()
        
        // Show general panel by default
        switchToTab("general")
    }

    private fun initViews(view: View) {
        // Sub-tabs
        tabGeneral = view.findViewById(R.id.tabGeneral)
        tabPopUp = view.findViewById(R.id.tabPopUp)
        tabNotification = view.findViewById(R.id.tabNotification)
        tabAdvanced = view.findViewById(R.id.tabAdvanced)
        
        // Panels
        panelGeneral = view.findViewById(R.id.panelGeneral)
        panelPopUp = view.findViewById(R.id.panelPopUp)
        panelNotification = view.findViewById(R.id.panelNotification)
        panelAdvanced = view.findViewById(R.id.panelAdvanced)
        
        // General Panel
        switchUseInCloverRegister = view.findViewById(R.id.switchUseInCloverRegister)
        
        // Pop Up Panel
        widgetRecyclerView = view.findViewById(R.id.widgetRecyclerView)
        btnAddWidget = view.findViewById(R.id.btnAddWidget)
        
        // Notification Panel
        templateRecyclerView = view.findViewById(R.id.templateRecyclerView)
        btnAddTemplate = view.findViewById(R.id.btnAddTemplate)
        
        // Advanced Panel
        inputNotificationTime = view.findViewById(R.id.inputNotificationTime)
        spinnerNotificationUnit = view.findViewById(R.id.spinnerNotificationUnit)
        inputReceiptTime = view.findViewById(R.id.inputReceiptTime)
        spinnerReceiptUnit = view.findViewById(R.id.spinnerReceiptUnit)
    }

    private fun setupSubTabs() {
        tabGeneral?.setOnClickListener { switchToTab("general") }
        tabPopUp?.setOnClickListener { switchToTab("popup") }
        tabNotification?.setOnClickListener { switchToTab("notification") }
        tabAdvanced?.setOnClickListener { switchToTab("advanced") }
    }

    private fun switchToTab(tab: String) {
        currentTab = tab
        
        // Update tab appearance
        val tabs = listOf(tabGeneral, tabPopUp, tabNotification, tabAdvanced)
        val tabNames = listOf("general", "popup", "notification", "advanced")
        
        tabs.forEachIndexed { index, textView ->
            val isSelected = tabNames[index] == tab
            textView?.isSelected = isSelected
            textView?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.text_primary_dark else R.color.text_muted
                )
            )
        }
        
        // Show/hide panels
        panelGeneral?.visibility = if (tab == "general") View.VISIBLE else View.GONE
        panelPopUp?.visibility = if (tab == "popup") View.VISIBLE else View.GONE
        panelNotification?.visibility = if (tab == "notification") View.VISIBLE else View.GONE
        panelAdvanced?.visibility = if (tab == "advanced") View.VISIBLE else View.GONE
    }

    // ==================== General Panel ====================
    
    private fun setupGeneralPanel() {
        switchUseInCloverRegister?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setUseOrderMateRegister(isChecked)
            // This enables/disables the OrderMate button in Clover Register
            // When clicked, that button opens the OrderMate popup
        }
    }

    // ==================== Pop Up Panel ====================
    
    private fun setupPopUpPanel() {
        // Setup RecyclerView
        widgetAdapter = WidgetEditorAdapter(
            widgets = settingsManager.getWidgets().toMutableList(),
            onWidgetUpdate = { widget -> settingsManager.updateWidget(widget) },
            onWidgetDelete = { widget -> 
                settingsManager.removeWidget(widget.id)
                widgetAdapter?.removeWidget(widget)
            }
        )
        
        widgetRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = widgetAdapter
        }
        
        // Setup drag-and-drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                widgetAdapter?.moveWidget(fromPos, toPos)
                settingsManager.reorderWidgets(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            
            override fun isLongPressDragEnabled() = false
        })
        
        itemTouchHelper.attachToRecyclerView(widgetRecyclerView)
        widgetAdapter?.setDragHelper(itemTouchHelper)
        
        // Add widget button
        btnAddWidget?.setOnClickListener {
            showAddWidgetDialog()
        }
    }

    private fun showAddWidgetDialog() {
        val widgets = settingsManager.getWidgets()
        if (widgets.size >= SettingsManager.MAX_WIDGETS) {
            Toast.makeText(requireContext(), "Maximum ${SettingsManager.MAX_WIDGETS} widgets allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val types = arrayOf("Calendar", "Single Select", "Multi Select", "Text Box")
        val typeEnums = arrayOf(WidgetType.CALENDAR, WidgetType.SINGLE_SELECT, WidgetType.MULTI_SELECT, WidgetType.TEXT_BOX)
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Widget")
            .setItems(types) { _, which ->
                val newWidget = settingsManager.addWidget(typeEnums[which])
                if (newWidget != null) {
                    widgetAdapter?.addWidget(newWidget)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==================== Notification Panel ====================
    
    private fun setupNotificationPanel() {
        // Setup RecyclerView
        templateAdapter = NotificationTemplateAdapter(
            templates = getTemplates().toMutableList(),
            onTemplateUpdate = { template -> saveTemplate(template) },
            onTemplateDelete = { template -> 
                deleteTemplate(template)
                templateAdapter?.removeTemplate(template)
            }
        )
        
        templateRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = templateAdapter
        }
        
        // Add template button
        btnAddTemplate?.setOnClickListener {
            val newTemplate = NotificationTemplate(
                id = System.currentTimeMillis().toInt(),
                name = "New Template",
                content = ""
            )
            saveTemplate(newTemplate)
            templateAdapter?.addTemplate(newTemplate)
        }
    }

    // Template storage helpers (extend SettingsManager functionality)
    private fun getTemplates(): List<NotificationTemplate> {
        // For now, use single template from SettingsManager
        // TODO: Extend to support multiple templates
        val content = settingsManager.getNotificationTemplate()
        return listOf(
            NotificationTemplate(1, "Order Ready", content)
        )
    }

    private fun saveTemplate(template: NotificationTemplate) {
        // Save to settings (extend for multiple templates)
        settingsManager.setNotificationTemplate(template.content)
    }

    private fun deleteTemplate(template: NotificationTemplate) {
        // TODO: Implement multiple template deletion
    }

    // ==================== Advanced Panel ====================
    
    private fun setupAdvancedPanel() {
        // Setup unit spinners
        val units = arrayOf("days", "hours", "minutes")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        spinnerNotificationUnit?.adapter = spinnerAdapter
        spinnerReceiptUnit?.adapter = spinnerAdapter
        
        // Notification time listeners
        inputNotificationTime?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setNotificationDays(it) 
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        spinnerNotificationUnit?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Save notification unit
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Receipt time listeners
        inputReceiptTime?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setReceiptTime(it) 
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        spinnerReceiptUnit?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsManager.setReceiptUnit(units[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ==================== Load Settings ====================
    
    private fun loadSettings() {
        // General
        switchUseInCloverRegister?.isChecked = settingsManager.getUseOrderMateRegister()
        
        // Advanced
        inputNotificationTime?.setText(settingsManager.getNotificationDays().toString())
        inputReceiptTime?.setText(settingsManager.getReceiptTime().toString())
        
        // Set spinner positions
        val units = arrayOf("days", "hours", "minutes")
        val receiptUnit = settingsManager.getReceiptUnit()
        spinnerReceiptUnit?.setSelection(units.indexOf(receiptUnit).coerceAtLeast(0))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabGeneral = null
        tabPopUp = null
        tabNotification = null
        tabAdvanced = null
        panelGeneral = null
        panelPopUp = null
        panelNotification = null
        panelAdvanced = null
        switchUseInCloverRegister = null
        widgetRecyclerView = null
        btnAddWidget = null
        templateRecyclerView = null
        btnAddTemplate = null
        inputNotificationTime = null
        spinnerNotificationUnit = null
        inputReceiptTime = null
        spinnerReceiptUnit = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}

// ==================== Data Classes ====================

data class NotificationTemplate(
    val id: Int,
    var name: String,
    var content: String
)

// ==================== Widget Editor Adapter ====================

class WidgetEditorAdapter(
    private val widgets: MutableList<PopUpWidget>,
    private val onWidgetUpdate: (PopUpWidget) -> Unit,
    private val onWidgetDelete: (PopUpWidget) -> Unit
) : RecyclerView.Adapter<WidgetEditorAdapter.ViewHolder>() {

    private var itemTouchHelper: ItemTouchHelper? = null
    private val expandedPositions = mutableSetOf<Int>()

    fun setDragHelper(helper: ItemTouchHelper) {
        itemTouchHelper = helper
    }

    fun addWidget(widget: PopUpWidget) {
        widgets.add(widget)
        notifyItemInserted(widgets.size - 1)
    }

    fun removeWidget(widget: PopUpWidget) {
        val index = widgets.indexOf(widget)
        if (index >= 0) {
            widgets.removeAt(index)
            expandedPositions.remove(index)
            notifyItemRemoved(index)
        }
    }

    fun moveWidget(from: Int, to: Int) {
        Collections.swap(widgets, from, to)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_widget_editor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(widgets[position], position)
    }

    override fun getItemCount() = widgets.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dragHandle: View = itemView.findViewById(R.id.dragHandle)
        private val widgetIconContainer: View = itemView.findViewById(R.id.widgetIconContainer)
        private val widgetIcon: ImageView = itemView.findViewById(R.id.widgetIcon)
        private val widgetTitle: TextView = itemView.findViewById(R.id.widgetTitle)
        private val widgetType: TextView = itemView.findViewById(R.id.widgetType)
        private val widgetToggle: Switch = itemView.findViewById(R.id.widgetToggle)
        private val expandChevron: ImageView = itemView.findViewById(R.id.expandChevron)
        private val widgetHeader: View = itemView.findViewById(R.id.widgetHeader)
        private val widgetBody: View = itemView.findViewById(R.id.widgetBody)
        private val inputWidgetLabel: EditText = itemView.findViewById(R.id.inputWidgetLabel)
        private val optionsContainer: View = itemView.findViewById(R.id.optionsContainer)
        private val inputAddOption: EditText = itemView.findViewById(R.id.inputAddOption)
        private val valuesContainer: FlexboxLayout = itemView.findViewById(R.id.valuesContainer)
        private val btnDeleteWidget: View = itemView.findViewById(R.id.btnDeleteWidget)

        fun bind(widget: PopUpWidget, position: Int) {
            widgetTitle.text = widget.label
            widgetType.text = widget.type.defaultLabel
            widgetToggle.isChecked = widget.enabled
            inputWidgetLabel.setText(widget.label)

            // Set icon and colors based on type
            val (iconRes, bgRes, tintColor) = when (widget.type) {
                WidgetType.CALENDAR -> Triple(R.drawable.ic_calendar, R.drawable.bg_widget_icon_calendar, 0xFF3B82F6.toInt())
                WidgetType.SINGLE_SELECT -> Triple(R.drawable.ic_list, R.drawable.bg_widget_icon_select, 0xFF8B5CF6.toInt())
                WidgetType.MULTI_SELECT -> Triple(R.drawable.ic_check_box, R.drawable.bg_widget_icon_multiselect, 0xFF10B981.toInt())
                WidgetType.TEXT_BOX -> Triple(R.drawable.ic_text_format, R.drawable.bg_widget_icon_text, 0xFFFF9F43.toInt())
            }
            widgetIcon.setImageResource(iconRes)
            widgetIcon.setColorFilter(tintColor)
            widgetIconContainer.setBackgroundResource(bgRes)

            // Show options for select types
            val hasOptions = widget.type == WidgetType.SINGLE_SELECT || widget.type == WidgetType.MULTI_SELECT
            optionsContainer.visibility = if (hasOptions) View.VISIBLE else View.GONE

            // Populate values
            if (hasOptions) {
                populateValues(widget)
            }

            // Expand/collapse - use widget ID for stable expand state
            val isExpanded = expandedPositions.contains(widget.id)
            widgetBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandChevron.rotation = if (isExpanded) 180f else 0f

            // Listeners - use bindingAdapterPosition for safe position access
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this)
                }
                false
            }

            widgetHeader.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                
                if (expandedPositions.contains(widget.id)) {
                    expandedPositions.remove(widget.id)
                } else {
                    expandedPositions.add(widget.id)
                }
                notifyItemChanged(currentPos)
            }

            widgetToggle.setOnCheckedChangeListener { _, isChecked ->
                widget.enabled = isChecked
                onWidgetUpdate(widget)
            }

            inputWidgetLabel.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    widget.label = inputWidgetLabel.text.toString()
                    onWidgetUpdate(widget)
                    // Update title text without full rebind
                    widgetTitle.text = widget.label
                }
            }

            inputAddOption.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || 
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    val newValue = inputAddOption.text.toString().trim()
                    if (newValue.isNotEmpty()) {
                        widget.values = widget.values + newValue
                        onWidgetUpdate(widget)
                        inputAddOption.text.clear()
                        populateValues(widget)
                    }
                    true
                } else false
            }

            btnDeleteWidget.setOnClickListener {
                onWidgetDelete(widget)
            }
        }

        private fun populateValues(widget: PopUpWidget) {
            valuesContainer.removeAllViews()
            widget.values.forEachIndexed { index, value ->
                val tag = createValueTag(value) {
                    widget.values = widget.values.filterIndexed { i, _ -> i != index }
                    onWidgetUpdate(widget)
                    populateValues(widget)
                }
                valuesContainer.addView(tag)
            }
        }

        private fun createValueTag(text: String, onRemove: () -> Unit): View {
            val context = itemView.context
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_value_tag)
                gravity = android.view.Gravity.CENTER_VERTICAL
                
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 8, 8)
                layoutParams = params

                addView(TextView(context).apply {
                    this.text = text
                    setTextColor(ContextCompat.getColor(context, R.color.text_light))
                    textSize = 12f
                })

                addView(TextView(context).apply {
                    this.text = "×"
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    textSize = 14f
                    setPadding(12, 0, 4, 0)
                    setOnClickListener { onRemove() }
                })
            }
        }
    }
}

// ==================== Notification Template Adapter ====================

class NotificationTemplateAdapter(
    private val templates: MutableList<NotificationTemplate>,
    private val onTemplateUpdate: (NotificationTemplate) -> Unit,
    private val onTemplateDelete: (NotificationTemplate) -> Unit
) : RecyclerView.Adapter<NotificationTemplateAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    fun addTemplate(template: NotificationTemplate) {
        templates.add(template)
        notifyItemInserted(templates.size - 1)
    }

    fun removeTemplate(template: NotificationTemplate) {
        val index = templates.indexOf(template)
        if (index >= 0) {
            templates.removeAt(index)
            expandedPositions.remove(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(templates[position], position)
    }

    override fun getItemCount() = templates.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val templateHeader: View = itemView.findViewById(R.id.templateHeader)
        private val templateName: TextView = itemView.findViewById(R.id.templateName)
        private val templatePreview: TextView = itemView.findViewById(R.id.templatePreview)
        private val expandChevron: ImageView = itemView.findViewById(R.id.expandChevron)
        private val templateBody: View = itemView.findViewById(R.id.templateBody)
        private val inputTemplateName: EditText = itemView.findViewById(R.id.inputTemplateName)
        private val inputTemplateContent: EditText = itemView.findViewById(R.id.inputTemplateContent)
        private val charCount: TextView = itemView.findViewById(R.id.charCount)
        private val btnDeleteTemplate: View = itemView.findViewById(R.id.btnDeleteTemplate)
        
        private var currentTextWatcher: TextWatcher? = null

        fun bind(template: NotificationTemplate, position: Int) {
            templateName.text = template.name
            templatePreview.text = if (template.content.length > 50) 
                "${template.content.take(50)}..." else template.content
            
            // Remove previous text watcher to avoid duplicate callbacks
            currentTextWatcher?.let { inputTemplateContent.removeTextChangedListener(it) }
            
            inputTemplateName.setText(template.name)
            inputTemplateContent.setText(template.content)
            updateCharCount(template.content.length)

            // Expand/collapse - use template ID for stable expand state
            val isExpanded = expandedPositions.contains(template.id)
            templateBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandChevron.rotation = if (isExpanded) 180f else 0f

            // Listeners - use bindingAdapterPosition for safe position access
            templateHeader.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                
                if (expandedPositions.contains(template.id)) {
                    expandedPositions.remove(template.id)
                } else {
                    expandedPositions.add(template.id)
                }
                notifyItemChanged(currentPos)
            }

            inputTemplateName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val currentPos = bindingAdapterPosition
                    if (currentPos == RecyclerView.NO_POSITION) return@setOnFocusChangeListener
                    
                    template.name = inputTemplateName.text.toString()
                    onTemplateUpdate(template)
                    // Update header text without full rebind
                    templateName.text = template.name
                }
            }

            currentTextWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val content = s?.toString() ?: ""
                    template.content = content
                    updateCharCount(content.length)
                    onTemplateUpdate(template)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            inputTemplateContent.addTextChangedListener(currentTextWatcher)

            btnDeleteTemplate.setOnClickListener {
                onTemplateDelete(template)
            }
        }

        private fun updateCharCount(count: Int) {
            charCount.text = "$count/250"
        }
    }
}
