package com.orderMate.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.utils.AdvancedSettings
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.NotificationTemplate
import com.orderMate.utils.PopUpWidget
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.SettingsManager
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.WidgetType
import java.util.Collections
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType as FirebaseWidgetType

/**
 * Settings Fragment with Sub-tabs (#83 requirement)
 * 
 * Features:
 * - General: Enable OrderMate in Clover Register
 * - Pop Up: Widget editor with drag-and-drop (Firebase persistence)
 * - Notification: Templates management (Firebase persistence)
 * - Advanced: Scheduled notifications, receipt settings
 */
class SettingsFragment : Fragment() {

    private lateinit var settingsManager: SettingsManager
    private var widgetManager: WidgetManager? = null
    private val firebase = FirebaseConfigManager.getInstance()
    private var merchantId: String? = null
    
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
    private var templateAdapter: FirebaseTemplateAdapter? = null
    
    // Advanced Panel
    private var inputNotificationDays: EditText? = null
    private var inputNotificationMinutes: EditText? = null
    private var inputReceiptDays: EditText? = null
    private var inputReceiptMinutes: EditText? = null
    
    // Loading
    private var loadingOverlay: View? = null
    
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
        
        // Get merchantId from PreferenceManager
        val prefManager = PreferenceManager.getInstance(requireContext())
        merchantId = prefManager.getString("merchantId")
        
        if (merchantId != null) {
            widgetManager = WidgetManager(merchantId!!)
        }
        
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
        inputNotificationDays = view.findViewById(R.id.inputNotificationDays)
        inputNotificationMinutes = view.findViewById(R.id.inputNotificationMinutes)
        inputReceiptDays = view.findViewById(R.id.inputReceiptDays)
        inputReceiptMinutes = view.findViewById(R.id.inputReceiptMinutes)
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
            onWidgetDelete = { widget -> showDeleteWidgetDialog(widget) }
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
    
    private fun showDeleteWidgetDialog(widget: PopUpWidget) {
        showDeleteConfirmationDialog(
            title = "Delete Widget?",
            message = "Are you sure you want to delete \"${widget.label}\"? This action cannot be undone.",
            onConfirm = {
                settingsManager.removeWidget(widget.id)
                widgetAdapter?.removeWidget(widget)
                Toast.makeText(context, "Widget deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ==================== Notification Panel ====================
    
    private fun setupNotificationPanel() {
        // Setup RecyclerView with Firebase-backed adapter
        templateAdapter = FirebaseTemplateAdapter(
            onTemplateUpdate = { template -> saveTemplateToFirebase(template) },
            onTemplateDelete = { template -> showDeleteTemplateDialog(template) }
        )
        
        templateRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = templateAdapter
        }
        
        // Load templates from Firebase
        loadTemplatesFromFirebase()
        
        // Add template button
        btnAddTemplate?.setOnClickListener {
            val newTemplate = NotificationTemplate.create(
                name = "New Template",
                content = ""
            )
            saveTemplateToFirebase(newTemplate)
            templateAdapter?.addTemplateExpanded(newTemplate)
        }
    }
    
    private fun loadTemplatesFromFirebase() {
        merchantId?.let { mid ->
            firebase.getTemplates(mid) { templates ->
                activity?.runOnUiThread {
                    if (templates.isEmpty()) {
                        // Create default template if none exist
                        val defaultTemplate = NotificationTemplate.create(
                            name = "Order Ready",
                            content = "Your order from {{merchant_name}} is ready for pickup!"
                        )
                        saveTemplateToFirebase(defaultTemplate)
                        templateAdapter?.setTemplates(listOf(defaultTemplate))
                    } else {
                        templateAdapter?.setTemplates(templates)
                    }
                }
            }
        }
    }
    
    private fun saveTemplateToFirebase(template: NotificationTemplate) {
        merchantId?.let { mid ->
            firebase.saveTemplate(mid, template) { success ->
                activity?.runOnUiThread {
                    if (!success) {
                        Toast.makeText(context, "Failed to save template", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun showDeleteTemplateDialog(template: NotificationTemplate) {
        showDeleteConfirmationDialog(
            title = "Delete Template?",
            message = "Are you sure you want to delete \"${template.name}\"? This action cannot be undone.",
            onConfirm = {
                merchantId?.let { mid ->
                    firebase.deleteTemplate(mid, template.id) { success ->
                        activity?.runOnUiThread {
                            if (success) {
                                templateAdapter?.removeTemplate(template)
                                Toast.makeText(context, "Template deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete template", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
    }

    // ==================== Advanced Panel ====================
    
    private fun setupAdvancedPanel() {
        // Debounce timer for saving settings
        var saveJob: Runnable? = null
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        fun scheduleAdvancedSettingsSave() {
            saveJob?.let { handler.removeCallbacks(it) }
            saveJob = Runnable { saveAdvancedSettingsToFirebase() }
            handler.postDelayed(saveJob!!, 500) // Debounce 500ms
        }
        
        // Notification time listeners (days and minutes)
        inputNotificationDays?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setNotificationDays(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        inputNotificationMinutes?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setNotificationMinutes(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Receipt time listeners (days and minutes)
        inputReceiptDays?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setReceiptDays(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        inputReceiptMinutes?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { 
                    settingsManager.setReceiptMinutes(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    
    private fun saveAdvancedSettingsToFirebase() {
        merchantId?.let { mid ->
            val settings = AdvancedSettings(
                useOrderMateInRegister = switchUseInCloverRegister?.isChecked ?: true,
                notificationDays = settingsManager.getNotificationDays(),
                notificationMinutes = settingsManager.getNotificationMinutes(),
                receiptDays = settingsManager.getReceiptDays(),
                receiptMinutes = settingsManager.getReceiptMinutes()
            )
            firebase.saveAdvancedSettings(mid, settings) { success ->
                // Silent save - no toast on success
                if (!success) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ==================== Load Settings ====================
    
    private fun loadSettings() {
        // Load from local SettingsManager first (fast)
        switchUseInCloverRegister?.isChecked = settingsManager.getUseOrderMateRegister()
        inputNotificationDays?.setText(settingsManager.getNotificationDays().toString())
        inputNotificationMinutes?.setText(settingsManager.getNotificationMinutes().toString())
        inputReceiptDays?.setText(settingsManager.getReceiptDays().toString())
        inputReceiptMinutes?.setText(settingsManager.getReceiptMinutes().toString())
        
        // Then sync from Firebase (authoritative)
        loadAdvancedSettingsFromFirebase()
    }
    
    private fun loadAdvancedSettingsFromFirebase() {
        merchantId?.let { mid ->
            firebase.getAdvancedSettings(mid) { settings ->
                activity?.runOnUiThread {
                    // Update local settings manager
                    settingsManager.setNotificationDays(settings.notificationDays)
                    settingsManager.setNotificationMinutes(settings.notificationMinutes)
                    settingsManager.setReceiptDays(settings.receiptDays)
                    settingsManager.setReceiptMinutes(settings.receiptMinutes)
                    settingsManager.setUseOrderMateRegister(settings.useOrderMateInRegister)
                    
                    // Update UI
                    switchUseInCloverRegister?.isChecked = settings.useOrderMateInRegister
                    inputNotificationDays?.setText(settings.notificationDays.toString())
                    inputNotificationMinutes?.setText(settings.notificationMinutes.toString())
                    inputReceiptDays?.setText(settings.receiptDays.toString())
                    inputReceiptMinutes?.setText(settings.receiptMinutes.toString())
                }
            }
        }
    }
    
    // ==================== Delete Confirmation Dialog ====================
    
    private fun showDeleteConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_confirmation, null)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        dialogView.findViewById<TextView>(R.id.dialogTitle)?.text = title
        dialogView.findViewById<TextView>(R.id.dialogMessage)?.text = message
        
        dialogView.findViewById<View>(R.id.btnCancel)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnDelete)?.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        
        dialog.show()
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
        inputNotificationDays = null
        inputNotificationMinutes = null
        inputReceiptDays = null
        inputReceiptMinutes = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}

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
        private val widgetCard: CardView = itemView.findViewById(R.id.widgetCard)
        private val widgetContainer: View = itemView.findViewById(R.id.widgetContainer)
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
        
        private var currentWidgetTintColor: Int = 0xFFFFFFFF.toInt()

        fun bind(widget: PopUpWidget, position: Int) {
            widgetTitle.text = widget.label
            widgetType.text = widget.type.defaultLabel
            widgetToggle.isChecked = widget.enabled
            inputWidgetLabel.setText(widget.label)

            // Set icon and colors based on type - matches HTML widget icon colors
            val (iconRes, bgRes, tintColor) = when (widget.type) {
                WidgetType.CALENDAR -> Triple(R.drawable.ic_calendar, R.drawable.bg_widget_icon_calendar, 0xFF64B5F6.toInt())
                WidgetType.SINGLE_SELECT -> Triple(R.drawable.ic_list, R.drawable.bg_widget_icon_select, 0xFFCE93D8.toInt())
                WidgetType.MULTI_SELECT -> Triple(R.drawable.ic_check_box, R.drawable.bg_widget_icon_multiselect, 0xFF81C784.toInt())
                WidgetType.TEXT_BOX -> Triple(R.drawable.ic_text_format, R.drawable.bg_widget_icon_text, 0xFFFFB74D.toInt())
            }
            widgetIcon.setImageResource(iconRes)
            widgetIcon.setColorFilter(tintColor)
            widgetIconContainer.setBackgroundResource(bgRes)
            
            // Store tint color for value pills
            currentWidgetTintColor = tintColor

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

            // Listeners - use adapterPosition for safe position access
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this)
                }
                false
            }

            widgetHeader.setOnClickListener {
                val currentPos = adapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                
                val expanding = !expandedPositions.contains(widget.id)
                if (expanding) {
                    expandedPositions.add(widget.id)
                } else {
                    expandedPositions.remove(widget.id)
                }
                
                // Smooth animation for expand/collapse
                animateExpand(expanding)
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
            val density = context.resources.displayMetrics.density
            
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
                
                // Create pill background with widget-specific border color
                val pillBg = GradientDrawable().apply {
                    setColor(0x33000000) // Dark semi-transparent background
                    cornerRadius = 16 * density
                    setStroke((1 * density).toInt(), currentWidgetTintColor)
                }
                background = pillBg
                
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, (8 * density).toInt(), (8 * density).toInt())
                layoutParams = params

                addView(TextView(context).apply {
                    this.text = text
                    setTextColor(ContextCompat.getColor(context, R.color.text_light))
                    textSize = 12f
                })

                addView(TextView(context).apply {
                    this.text = "×"
                    setTextColor(currentWidgetTintColor)
                    textSize = 14f
                    setPadding((8 * density).toInt(), 0, (2 * density).toInt(), 0)
                    setOnClickListener { onRemove() }
                })
            }
        }
        
        private fun animateExpand(expanding: Boolean) {
            if (expanding) {
                widgetBody.visibility = View.VISIBLE
                widgetBody.alpha = 0f
                widgetBody.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            } else {
                widgetBody.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction { widgetBody.visibility = View.GONE }
                    .start()
            }
            
            // Animate chevron rotation
            expandChevron.animate()
                .rotation(if (expanding) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }
}

// ==================== Firebase-backed Template Adapter ====================

class FirebaseTemplateAdapter(
    private val onTemplateUpdate: (NotificationTemplate) -> Unit,
    private val onTemplateDelete: (NotificationTemplate) -> Unit
) : RecyclerView.Adapter<FirebaseTemplateAdapter.ViewHolder>() {

    private val templates = mutableListOf<NotificationTemplate>()
    private val expandedIds = mutableSetOf<String>()
    
    fun setTemplates(newTemplates: List<NotificationTemplate>) {
        templates.clear()
        templates.addAll(newTemplates)
        notifyDataSetChanged()
    }

    fun addTemplate(template: NotificationTemplate) {
        templates.add(template)
        notifyItemInserted(templates.size - 1)
    }
    
    fun addTemplateExpanded(template: NotificationTemplate) {
        templates.add(template)
        expandedIds.add(template.id)
        notifyItemInserted(templates.size - 1)
    }

    fun removeTemplate(template: NotificationTemplate) {
        val index = templates.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            templates.removeAt(index)
            expandedIds.remove(template.id)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(templates[position])
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
        private var currentNameWatcher: TextWatcher? = null
        private var saveHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private var saveRunnable: Runnable? = null

        fun bind(template: NotificationTemplate) {
            templateName.text = template.name
            templatePreview.text = if (template.content.length > 50) 
                "${template.content.take(50)}..." else template.content
            
            // Remove previous watchers
            currentTextWatcher?.let { inputTemplateContent.removeTextChangedListener(it) }
            currentNameWatcher?.let { inputTemplateName.removeTextChangedListener(it) }
            
            inputTemplateName.setText(template.name)
            inputTemplateContent.setText(template.content)
            updateCharCount(template.content.length)

            // Expand/collapse using template ID
            val isExpanded = expandedIds.contains(template.id)
            templateBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandChevron.rotation = if (isExpanded) 180f else 0f

            templateHeader.setOnClickListener {
                val currentPos = adapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                
                val expanding = !expandedIds.contains(template.id)
                if (expanding) {
                    expandedIds.add(template.id)
                } else {
                    expandedIds.remove(template.id)
                }
                notifyItemChanged(currentPos)
            }

            // Debounced save for name changes
            currentNameWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val name = s?.toString() ?: ""
                    template.name = name
                    templateName.text = name
                    scheduleSave(template)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            inputTemplateName.addTextChangedListener(currentNameWatcher)

            // Debounced save for content changes
            currentTextWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val content = s?.toString() ?: ""
                    template.content = content
                    updateCharCount(content.length)
                    templatePreview.text = if (content.length > 50) "${content.take(50)}..." else content
                    scheduleSave(template)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            inputTemplateContent.addTextChangedListener(currentTextWatcher)

            btnDeleteTemplate.setOnClickListener {
                onTemplateDelete(template)
            }
        }
        
        private fun scheduleSave(template: NotificationTemplate) {
            saveRunnable?.let { saveHandler.removeCallbacks(it) }
            saveRunnable = Runnable { onTemplateUpdate(template) }
            saveHandler.postDelayed(saveRunnable!!, 500) // Debounce 500ms
        }

        private fun updateCharCount(count: Int) {
            charCount.text = "$count/250"
        }
    }
}
