package com.orderMate.fragment

import android.annotation.SuppressLint
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
import com.orderMate.utils.DefaultWidgetFactory
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.WidgetType
import com.orderMate.utils.WidgetColorUtils
import java.util.Collections
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetType as FirebaseWidgetType
import com.orderMate.utils.EmployeeRoleUtils
import com.orderMate.utils.MyApp
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import androidx.navigation.fragment.findNavController

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
    private val firebase = FirebaseConfigManager.getInstance()
    private var merchantId: String? = null
    
    private val widgetManager: WidgetManager
        get() = WidgetManager.getInstance(requireContext())
    
    // Sub-tabs
    private var tabGeneral: TextView? = null
    private var tabItemLevelNotes: TextView? = null
    private var tabOrderLevelNotes: TextView? = null
    private var tabFilter: TextView? = null
    private var tabNotification: TextView? = null
    private var tabAdvanced: TextView? = null
    
    // Panels
    private var panelGeneral: View? = null
    private var panelItemLevelNotes: View? = null
    private var panelOrderLevelNotes: View? = null
    private var panelFilter: View? = null
    private var panelNotification: View? = null
    private var panelAdvanced: View? = null
    
    // General Panel
    private var switchUseInCloverRegister: Switch? = null
    private var switchUseOrderMateInstead: Switch? = null
    private var switchItemNotesEnabledGeneral: Switch? = null
    private var switchOrderNotesEnabledGeneral: Switch? = null
    
    // Filter Panel
    private var filterItemLevelHeader: View? = null
    private var filterOrderLevelHeader: View? = null
    private var filterItemLevelWidgetsContainer: LinearLayout? = null
    private var filterOrderLevelWidgetsContainer: LinearLayout? = null
    private var filterItemLevelRecyclerView: RecyclerView? = null
    private var filterOrderLevelRecyclerView: RecyclerView? = null
    private var filterItemLevelAdapter: FilterWidgetAdapter? = null
    private var filterOrderLevelAdapter: FilterWidgetAdapter? = null
    private var filterEmptyState: TextView? = null
    
    // Clover Filter Cards (expandable)
    private var switchFilterOrderDate: Switch? = null
    private var switchFilterPaymentStatus: Switch? = null
    private var switchFilterOrderStatus: Switch? = null
    private var switchFilterPaymentType: Switch? = null
    private var switchFilterEmployee: Switch? = null
    private var paymentStatusHeader: View? = null
    private var paymentStatusBody: View? = null
    private var paymentStatusChevron: ImageView? = null
    private var paymentStatusOptions: com.google.android.flexbox.FlexboxLayout? = null
    private var orderStatusHeader: View? = null
    private var orderStatusBody: View? = null
    private var orderStatusChevron: ImageView? = null
    private var orderStatusOptions: com.google.android.flexbox.FlexboxLayout? = null
    private var paymentTypeHeader: View? = null
    private var paymentTypeBody: View? = null
    private var paymentTypeChevron: ImageView? = null
    private var paymentTypeOptions: com.google.android.flexbox.FlexboxLayout? = null
    
    // Item Level Notes Panel (#34)
    private var switchItemNotesEnabled: Switch? = null
    private var itemLevelEditorCard: View? = null
    private var itemLevelWidgetRecyclerView: RecyclerView? = null
    private var btnAddItemLevelWidget: View? = null
    private var btnResetItemLevelWidgets: View? = null
    
    // Order Level Notes Panel (#34)
    private var switchOrderNotesEnabled: Switch? = null
    private var orderLevelEditorCard: View? = null
    private var orderLevelWidgetRecyclerView: RecyclerView? = null
    private var btnAddOrderLevelWidget: View? = null
    private var btnResetOrderLevelWidgets: View? = null
    
    // Notification Panel
    private var templateRecyclerView: RecyclerView? = null
    private var btnAddTemplate: View? = null
    private var templateAdapter: FirebaseTemplateAdapter? = null
    
    // Advanced Panel
    private var switchScheduledNotifications: Switch? = null
    private var notificationInputsContainer: View? = null
    private var inputNotificationDays: EditText? = null
    private var inputNotificationMinutes: EditText? = null
    private var switchScheduledReceipt: Switch? = null
    private var receiptInputsContainer: View? = null
    private var inputReceiptDays: EditText? = null
    private var inputReceiptMinutes: EditText? = null
    
    // Receipt Settings
    private var switchPrintNotesCustomer: Switch? = null
    private var switchPrintNotesOrder: Switch? = null
    
    // #79: Permission Settings
    private var switchAllowAdminSettings: Switch? = null
    private var switchAllowManagersSettings: Switch? = null
    private var switchAllowEmployeesSettings: Switch? = null
    
    // #81: Permission Settings Card container (owner-only visibility)
    private var permissionSettingsCard: View? = null
    private var isOwner: Boolean = false
    
    // Loading
    private var loadingOverlay: View? = null
    
    // Widget adapters for both levels
    private var itemLevelWidgetAdapter: FirebaseWidgetEditorAdapter? = null
    private var orderLevelWidgetAdapter: FirebaseWidgetEditorAdapter? = null
    
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
        
        // Get merchantId and employee info from MyApp (Clover SDK) on background thread
        runOnBackgroundThread {
            try {
                val app = requireContext().applicationContext as? MyApp
                val mid = app?.getMerchantId()
                val employee = app?.getCurrentEmployee()
                
                // #81: Check if current user is owner for permission settings visibility
                isOwner = EmployeeRoleUtils.isOwner(employee)
                
                if (!mid.isNullOrEmpty()) {
                    merchantId = mid
                    
                    // #81: Verify user has settings access (entry guard/failsafe)
                    // If a non-permitted user somehow navigates here, redirect them
                    firebase.getAdvancedSettings(mid) { settings ->
                        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, settings)
                        
                        runOnMainThread {
                            if (isAdded) {
                                if (!canAccess) {
                                    // User doesn't have settings access - navigate back to order list
                                    try {
                                        findNavController().navigate(R.id.orderListRedesignFragment)
                                    } catch (e: Exception) {
                                        // Fallback: pop back if navigate fails
                                        findNavController().popBackStack()
                                    }
                                    return@runOnMainThread
                                }
                                
                                widgetManager.setMerchantId(mid)
                                // Now that we have merchantId, reload widgets from Firebase (not cache)
                                loadAllWidgetsFromFirebase()
                                // Also load templates
                                loadTemplatesFromFirebase()
                                
                                // #81: Hide permission settings card if not owner
                                // Only owners should see and modify permission settings
                                updatePermissionSettingsVisibility()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        initViews(view)
        setupSubTabs()
        setupGeneralPanel()
        setupItemLevelNotesPanel()
        setupOrderLevelNotesPanel()
        setupNotificationPanel()
        setupAdvancedPanel()
        loadSettings()
        
        // Show general panel by default
        switchToTab("general")
    }
    
    /**
     * #81: Update permission settings card visibility based on user role.
     * Only owners should see the Permission Settings card in Advanced tab.
     * This prevents non-owners from changing their own or others' permissions.
     */
    private fun updatePermissionSettingsVisibility() {
        permissionSettingsCard?.visibility = if (isOwner) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Load from local cache (not Firebase) to preserve recent changes
        // Firebase sync happens in onViewCreated for initial load
        android.util.Log.d("WidgetDragDebug", "onResume: loading from cache")
        loadWidgetsFromCache()
    }
    
    private fun loadWidgetsFromCache() {
        val itemWidgets = widgetManager.getItemWidgets()
        val orderWidgets = widgetManager.getOrderWidgets()
        android.util.Log.d("WidgetDragDebug", "loadWidgetsFromCache: ${itemWidgets.size} item, ${orderWidgets.size} order widgets")
        itemLevelWidgetAdapter?.setWidgets(itemWidgets.toMutableList())
        orderLevelWidgetAdapter?.setWidgets(orderWidgets.toMutableList())
    }

    private fun initViews(view: View) {
        // Sub-tabs
        tabGeneral = view.findViewById(R.id.tabGeneral)
        tabItemLevelNotes = view.findViewById(R.id.tabItemLevelNotes)
        tabOrderLevelNotes = view.findViewById(R.id.tabOrderLevelNotes)
        tabFilter = view.findViewById(R.id.tabFilter)
        tabNotification = view.findViewById(R.id.tabNotification)
        tabAdvanced = view.findViewById(R.id.tabAdvanced)
        
        // Panels
        panelGeneral = view.findViewById(R.id.panelGeneral)
        panelItemLevelNotes = view.findViewById(R.id.panelItemLevelNotes)
        panelOrderLevelNotes = view.findViewById(R.id.panelOrderLevelNotes)
        panelFilter = view.findViewById(R.id.panelFilter)
        panelNotification = view.findViewById(R.id.panelNotification)
        panelAdvanced = view.findViewById(R.id.panelAdvanced)
        
        // General Panel
        switchUseInCloverRegister = view.findViewById(R.id.switchUseInCloverRegister)
        switchUseOrderMateInstead = view.findViewById(R.id.switchUseOrderMateInstead)
        switchItemNotesEnabledGeneral = view.findViewById(R.id.switchItemNotesEnabledGeneral)
        switchOrderNotesEnabledGeneral = view.findViewById(R.id.switchOrderNotesEnabledGeneral)
        
        // Filter Panel
        filterItemLevelHeader = view.findViewById(R.id.filterItemLevelHeader)
        filterOrderLevelHeader = view.findViewById(R.id.filterOrderLevelHeader)
        filterItemLevelWidgetsContainer = view.findViewById(R.id.filterItemLevelWidgetsContainer)
        filterOrderLevelWidgetsContainer = view.findViewById(R.id.filterOrderLevelWidgetsContainer)
        filterItemLevelRecyclerView = view.findViewById(R.id.filterItemLevelRecyclerView)
        filterOrderLevelRecyclerView = view.findViewById(R.id.filterOrderLevelRecyclerView)
        filterEmptyState = view.findViewById(R.id.filterEmptyState)
        
        // Clover Filter Cards (expandable)
        switchFilterOrderDate = view.findViewById(R.id.switchFilterOrderDate)
        switchFilterPaymentStatus = view.findViewById(R.id.switchFilterPaymentStatus)
        switchFilterOrderStatus = view.findViewById(R.id.switchFilterOrderStatus)
        switchFilterPaymentType = view.findViewById(R.id.switchFilterPaymentType)
        switchFilterEmployee = view.findViewById(R.id.switchFilterEmployee)
        paymentStatusHeader = view.findViewById(R.id.paymentStatusHeader)
        paymentStatusBody = view.findViewById(R.id.paymentStatusBody)
        paymentStatusChevron = view.findViewById(R.id.paymentStatusChevron)
        paymentStatusOptions = view.findViewById(R.id.paymentStatusOptions)
        orderStatusHeader = view.findViewById(R.id.orderStatusHeader)
        orderStatusBody = view.findViewById(R.id.orderStatusBody)
        orderStatusChevron = view.findViewById(R.id.orderStatusChevron)
        orderStatusOptions = view.findViewById(R.id.orderStatusOptions)
        paymentTypeHeader = view.findViewById(R.id.paymentTypeHeader)
        paymentTypeBody = view.findViewById(R.id.paymentTypeBody)
        paymentTypeChevron = view.findViewById(R.id.paymentTypeChevron)
        paymentTypeOptions = view.findViewById(R.id.paymentTypeOptions)
        
        // Item Level Notes Panel (#34)
        switchItemNotesEnabled = view.findViewById(R.id.switchItemNotesEnabled)
        itemLevelEditorCard = view.findViewById(R.id.itemLevelEditorCard)
        itemLevelWidgetRecyclerView = view.findViewById(R.id.itemLevelWidgetRecyclerView)
        btnAddItemLevelWidget = view.findViewById(R.id.btnAddItemLevelWidget)
        btnResetItemLevelWidgets = view.findViewById(R.id.btnResetItemLevelWidgets)
        
        // Order Level Notes Panel (#34)
        switchOrderNotesEnabled = view.findViewById(R.id.switchOrderNotesEnabled)
        orderLevelEditorCard = view.findViewById(R.id.orderLevelEditorCard)
        orderLevelWidgetRecyclerView = view.findViewById(R.id.orderLevelWidgetRecyclerView)
        btnAddOrderLevelWidget = view.findViewById(R.id.btnAddOrderLevelWidget)
        btnResetOrderLevelWidgets = view.findViewById(R.id.btnResetOrderLevelWidgets)
        
        // Notification Panel
        templateRecyclerView = view.findViewById(R.id.templateRecyclerView)
        btnAddTemplate = view.findViewById(R.id.btnAddTemplate)
        
        // Advanced Panel
        switchScheduledNotifications = view.findViewById(R.id.switchScheduledNotifications)
        notificationInputsContainer = view.findViewById(R.id.notificationInputsContainer)
        inputNotificationDays = view.findViewById(R.id.inputNotificationDays)
        inputNotificationMinutes = view.findViewById(R.id.inputNotificationMinutes)
        switchScheduledReceipt = view.findViewById(R.id.switchScheduledReceipt)
        receiptInputsContainer = view.findViewById(R.id.receiptInputsContainer)
        inputReceiptDays = view.findViewById(R.id.inputReceiptDays)
        inputReceiptMinutes = view.findViewById(R.id.inputReceiptMinutes)
        
        // Receipt Settings
        switchPrintNotesCustomer = view.findViewById(R.id.switchPrintNotesCustomer)
        switchPrintNotesOrder = view.findViewById(R.id.switchPrintNotesOrder)
        
        // #79: Permission Settings
        switchAllowAdminSettings = view.findViewById(R.id.switchAllowAdminSettings)
        switchAllowManagersSettings = view.findViewById(R.id.switchAllowManagersSettings)
        switchAllowEmployeesSettings = view.findViewById(R.id.switchAllowEmployeesSettings)
        
        // #81: Permission Settings Card container (owner-only visibility)
        permissionSettingsCard = view.findViewById(R.id.permissionSettingsCard)
    }

    private fun setupSubTabs() {
        tabGeneral?.setOnClickListener { switchToTab("general") }
        tabItemLevelNotes?.setOnClickListener { switchToTab("item_level") }
        tabOrderLevelNotes?.setOnClickListener { switchToTab("order_level") }
        tabFilter?.setOnClickListener { switchToTab("filter") }
        tabNotification?.setOnClickListener { switchToTab("notification") }
        tabAdvanced?.setOnClickListener { switchToTab("advanced") }
    }

    private fun switchToTab(tab: String) {
        currentTab = tab
        
        // All tabs save immediately on user change via their own listeners
        // No need to save on tab leave
        
        // Update tab appearance
        val tabs = listOf(tabGeneral, tabItemLevelNotes, tabOrderLevelNotes, tabFilter, tabNotification, tabAdvanced)
        val tabNames = listOf("general", "item_level", "order_level", "filter", "notification", "advanced")
        
        tabs.forEachIndexed { index, textView ->
            val isSelected = tabNames.getOrNull(index) == tab
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
        panelItemLevelNotes?.visibility = if (tab == "item_level") View.VISIBLE else View.GONE
        panelOrderLevelNotes?.visibility = if (tab == "order_level") View.VISIBLE else View.GONE
        panelFilter?.visibility = if (tab == "filter") View.VISIBLE else View.GONE
        panelNotification?.visibility = if (tab == "notification") View.VISIBLE else View.GONE
        panelAdvanced?.visibility = if (tab == "advanced") View.VISIBLE else View.GONE
        
        // Load filter widgets and Clover filter toggles when switching to filter tab
        if (tab == "filter") {
            loadFilterWidgetToggles()
            setupCloverFilterToggles()
        }
    }
    
    // ==================== General Panel ====================
    
    private fun setupGeneralPanel() {
        // "Use OrderMate In Clover's Register" toggle (shows floating button)
        switchUseInCloverRegister?.setOnCheckedChangeListener { _, isChecked ->
            // Save to local settings (mutual exclusion handled in SettingsManager)
            settingsManager.setUseOrderMateRegister(isChecked)
            
            // Mutual exclusion: disable "Instead" toggle if this is enabled
            if (isChecked) {
                switchUseOrderMateInstead?.isChecked = false
            }
            
            // Save to Firebase V2 PopupSettings
            widgetManager.setShowOMButtonInRegister(isChecked) { success ->
                if (!success) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // "Use OrderMate Register Instead Of Clover Register" toggle (permanent overlay)
        switchUseOrderMateInstead?.setOnCheckedChangeListener { _, isChecked ->
            // Save to local settings (mutual exclusion handled in SettingsManager)
            settingsManager.setUseOrderMateRegisterInstead(isChecked)
            
            // Mutual exclusion: disable regular toggle if this is enabled
            if (isChecked) {
                switchUseInCloverRegister?.isChecked = false
            }
        }
        
        // Item Level Notes toggle in General panel - disables ALL item level widgets when off
        switchItemNotesEnabledGeneral?.setOnCheckedChangeListener { _, isChecked ->
            widgetManager.setItemNotesEnabled(isChecked) { success ->
                if (success) {
                    activity?.runOnUiThread {
                        // Also update the Item Level Notes tab toggle if visible
                        switchItemNotesEnabled?.isChecked = isChecked
                        itemLevelEditorCard?.alpha = if (isChecked) 1.0f else 0.5f
                        btnAddItemLevelWidget?.isEnabled = isChecked
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Order Level Notes toggle in General panel - disables ALL order level widgets when off
        switchOrderNotesEnabledGeneral?.setOnCheckedChangeListener { _, isChecked ->
            widgetManager.setOrderNotesEnabled(isChecked) { success ->
                if (success) {
                    activity?.runOnUiThread {
                        // Also update the Order Level Notes tab toggle if visible
                        switchOrderNotesEnabled?.isChecked = isChecked
                        orderLevelEditorCard?.alpha = if (isChecked) 1.0f else 0.5f
                        btnAddOrderLevelWidget?.isEnabled = isChecked
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ==================== Item Level Notes Panel ====================
    
    private fun setupItemLevelNotesPanel() {
        // Setup enable/disable toggle (#34)
        switchItemNotesEnabled?.setOnCheckedChangeListener { _, isChecked ->
            // Update editor card visibility based on toggle
            itemLevelEditorCard?.alpha = if (isChecked) 1.0f else 0.5f
            btnAddItemLevelWidget?.isEnabled = isChecked
            
            // Also update the General panel toggle
            switchItemNotesEnabledGeneral?.isChecked = isChecked
            
            // Save to Firebase
            widgetManager.setItemNotesEnabled(isChecked) { success ->
                if (!success) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        itemLevelWidgetAdapter = FirebaseWidgetEditorAdapter(
            onWidgetUpdate = { widget -> saveItemWidget(widget) },
            onWidgetDelete = { widget -> showDeleteItemWidgetDialog(widget) }
        )
        
        itemLevelWidgetRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemLevelWidgetAdapter
        }
        
        // Setup drag-and-drop for item level widgets
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            private var dragFrom = -1
            private var dragTo = -1
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                android.util.Log.d("WidgetDragDebug", "ITEM onMove: fromPos=$fromPos, toPos=$toPos, dragFrom=$dragFrom")
                
                // Track the original position
                if (dragFrom == -1) dragFrom = fromPos
                dragTo = toPos
                
                // Update UI immediately
                itemLevelWidgetAdapter?.moveWidget(fromPos, toPos)
                return true
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                android.util.Log.d("WidgetDragDebug", "ITEM clearView: dragFrom=$dragFrom, dragTo=$dragTo")
                // Save to Firebase only when drag is complete
                if (dragFrom != -1 && dragFrom != dragTo) {
                    android.util.Log.d("WidgetDragDebug", "ITEM clearView: calling saveItemWidgetOrder()")
                    saveItemWidgetOrder()
                } else {
                    android.util.Log.d("WidgetDragDebug", "ITEM clearView: SKIPPED save (no change or dragFrom=-1)")
                }
                dragFrom = -1
                dragTo = -1
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        })
        
        itemTouchHelper.attachToRecyclerView(itemLevelWidgetRecyclerView)
        itemLevelWidgetAdapter?.setDragHelper(itemTouchHelper)
        
        btnAddItemLevelWidget?.setOnClickListener {
            showAddItemWidgetDialog()
        }
        
        btnResetItemLevelWidgets?.setOnClickListener {
            showResetItemWidgetsConfirmDialog()
        }
    }
    
    // ==================== Order Level Notes Panel ====================
    
    private fun setupOrderLevelNotesPanel() {
        // Setup enable/disable toggle (#34)
        switchOrderNotesEnabled?.setOnCheckedChangeListener { _, isChecked ->
            // Update editor card visibility based on toggle
            orderLevelEditorCard?.alpha = if (isChecked) 1.0f else 0.5f
            btnAddOrderLevelWidget?.isEnabled = isChecked
            
            // Also update the General panel toggle
            switchOrderNotesEnabledGeneral?.isChecked = isChecked
            
            // Save to Firebase
            widgetManager.setOrderNotesEnabled(isChecked) { success ->
                if (!success) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        orderLevelWidgetAdapter = FirebaseWidgetEditorAdapter(
            onWidgetUpdate = { widget -> saveOrderWidget(widget) },
            onWidgetDelete = { widget -> showDeleteOrderWidgetDialog(widget) }
        )
        
        orderLevelWidgetRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderLevelWidgetAdapter
        }
        
        // Setup drag-and-drop for order level widgets
        val orderTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            private var dragFrom = -1
            private var dragTo = -1
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                android.util.Log.d("WidgetDragDebug", "ORDER onMove: fromPos=$fromPos, toPos=$toPos, dragFrom=$dragFrom")
                
                // Track the original position
                if (dragFrom == -1) dragFrom = fromPos
                dragTo = toPos
                
                // Update UI immediately
                orderLevelWidgetAdapter?.moveWidget(fromPos, toPos)
                return true
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                android.util.Log.d("WidgetDragDebug", "ORDER clearView: dragFrom=$dragFrom, dragTo=$dragTo")
                // Save to Firebase only when drag is complete
                if (dragFrom != -1 && dragFrom != dragTo) {
                    android.util.Log.d("WidgetDragDebug", "ORDER clearView: calling saveOrderWidgetOrder()")
                    saveOrderWidgetOrder()
                } else {
                    android.util.Log.d("WidgetDragDebug", "ORDER clearView: SKIPPED save (no change or dragFrom=-1)")
                }
                dragFrom = -1
                dragTo = -1
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        })
        
        orderTouchHelper.attachToRecyclerView(orderLevelWidgetRecyclerView)
        orderLevelWidgetAdapter?.setDragHelper(orderTouchHelper)
        
        btnAddOrderLevelWidget?.setOnClickListener {
            showAddOrderWidgetDialog()
        }
        
        btnResetOrderLevelWidgets?.setOnClickListener {
            showResetOrderWidgetsConfirmDialog()
        }
        
        // Load widgets AFTER both adapters are created
        loadAllWidgetsFromFirebase()
    }
    
    // ==================== Level-Specific Widget Methods ====================
    
    /**
     * Load item widgets from Firebase and update UI.
     * Uses level-specific fetch to prevent cross-contamination.
     */
    private fun loadItemWidgetsFromFirebase() {
        val merchantId = widgetManager.getMerchantId()
        if (merchantId == null) {
            android.util.Log.d("SettingsFragment", "loadItemWidgetsFromFirebase: merchantId is null, using cache")
            val itemWidgets = widgetManager.getItemWidgets()
            itemLevelWidgetAdapter?.setWidgets(itemWidgets.toMutableList())
            return
        }
        
        firebase.getItemWidgets(merchantId) { widgets ->
            android.util.Log.d("SettingsFragment", "loadItemWidgetsFromFirebase: fetched ${widgets.size} item widgets")
            widgets.forEach { w ->
                android.util.Log.d("SettingsFragment", "  Item Widget: ${w.label}, id=${w.id}")
            }
            
            activity?.runOnUiThread {
                widgetManager.saveItemWidgets(widgets)
                itemLevelWidgetAdapter?.setWidgets(widgets.toMutableList())
            }
        }
    }
    
    /**
     * Load order widgets from Firebase and update UI.
     * Uses level-specific fetch to prevent cross-contamination.
     */
    private fun loadOrderWidgetsFromFirebase() {
        val merchantId = widgetManager.getMerchantId()
        if (merchantId == null) {
            android.util.Log.d("SettingsFragment", "loadOrderWidgetsFromFirebase: merchantId is null, using cache")
            val orderWidgets = widgetManager.getOrderWidgets()
            orderLevelWidgetAdapter?.setWidgets(orderWidgets.toMutableList())
            return
        }
        
        firebase.getOrderWidgets(merchantId) { widgets ->
            android.util.Log.d("SettingsFragment", "loadOrderWidgetsFromFirebase: fetched ${widgets.size} order widgets")
            widgets.forEach { w ->
                android.util.Log.d("SettingsFragment", "  Order Widget: ${w.label}, id=${w.id}")
            }
            
            activity?.runOnUiThread {
                widgetManager.saveOrderWidgets(widgets)
                orderLevelWidgetAdapter?.setWidgets(widgets.toMutableList())
            }
        }
    }
    
    /**
     * Load both item and order widgets from Firebase.
     */
    private fun loadAllWidgetsFromFirebase() {
        loadItemWidgetsFromFirebase()
        loadOrderWidgetsFromFirebase()
    }
    
    /**
     * Save item-level widget using level-specific method.
     * Only updates the single widget, does not affect order widgets.
     * Validates unique label before saving.
     */
    private fun saveItemWidget(widget: WidgetConfig) {
        android.util.Log.d("SettingsFragment", "saveItemWidget: ${widget.label}, id=${widget.id}")
        
        // Validate unique label before saving
        val validationError = widgetManager.validateWidgetLabel(widget)
        if (validationError != null) {
            activity?.runOnUiThread {
                Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
            }
            return
        }
        
        widgetManager.updateItemWidget(widget) { success ->
            android.util.Log.d("SettingsFragment", "saveItemWidget result: success=$success")
            if (!success) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to save widget", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Save order-level widget using level-specific method.
     * Only updates the single widget, does not affect item widgets.
     * Validates unique label before saving.
     */
    private fun saveOrderWidget(widget: WidgetConfig) {
        android.util.Log.d("SettingsFragment", "saveOrderWidget: ${widget.label}, id=${widget.id}")
        
        // Validate unique label before saving
        val validationError = widgetManager.validateWidgetLabel(widget)
        if (validationError != null) {
            activity?.runOnUiThread {
                Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
            }
            return
        }
        
        widgetManager.updateOrderWidget(widget) { success ->
            android.util.Log.d("SettingsFragment", "saveOrderWidget result: success=$success")
            if (!success) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to save widget", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Save item-level widget order after drag-and-drop.
     * Gets current order from adapter and saves all widgets with updated order field.
     */
    private fun saveItemWidgetOrder() {
        val widgets = itemLevelWidgetAdapter?.getWidgets()?.toMutableList()
        if (widgets == null) {
            android.util.Log.e("WidgetDragDebug", "saveItemWidgetOrder: widgets is NULL!")
            return
        }
        android.util.Log.d("WidgetDragDebug", "saveItemWidgetOrder: saving ${widgets.size} widgets")
        // Update order field based on current position
        widgets.forEachIndexed { index, widget -> 
            android.util.Log.d("WidgetDragDebug", "  ITEM[$index]: ${widget.label} (id=${widget.id})")
            widget.order = index 
        }
        
        widgetManager.updateItemWidgets(widgets) { success ->
            android.util.Log.d("WidgetDragDebug", "saveItemWidgetOrder: Firebase callback success=$success")
            if (!success) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to save widget order", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Save order-level widget order after drag-and-drop.
     * Gets current order from adapter and saves all widgets with updated order field.
     */
    private fun saveOrderWidgetOrder() {
        val widgets = orderLevelWidgetAdapter?.getWidgets()?.toMutableList()
        if (widgets == null) {
            android.util.Log.e("WidgetDragDebug", "saveOrderWidgetOrder: widgets is NULL!")
            return
        }
        android.util.Log.d("WidgetDragDebug", "saveOrderWidgetOrder: saving ${widgets.size} widgets")
        // Update order field based on current position
        widgets.forEachIndexed { index, widget -> 
            android.util.Log.d("WidgetDragDebug", "  ORDER[$index]: ${widget.label} (id=${widget.id})")
            widget.order = index 
        }
        
        widgetManager.updateOrderWidgets(widgets) { success ->
            android.util.Log.d("WidgetDragDebug", "saveOrderWidgetOrder: Firebase callback success=$success")
            if (!success) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to save widget order", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddItemWidgetDialog() {
        if (!widgetManager.canAddItemWidget()) {
            Toast.makeText(requireContext(), "Maximum 7 item-level widgets allowed", Toast.LENGTH_SHORT).show()
            return
        }
        showAddWidgetDialogInternal(NoteLevel.ITEM)
    }
    
    private fun showAddOrderWidgetDialog() {
        if (!widgetManager.canAddOrderWidget()) {
            Toast.makeText(requireContext(), "Maximum 7 order-level widgets allowed", Toast.LENGTH_SHORT).show()
            return
        }
        showAddWidgetDialogInternal(NoteLevel.ORDER)
    }

    private fun showAddWidgetDialogInternal(level: NoteLevel) {
        // Create styled dialog matching filter modal (#36)
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_widget, null)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // (#81 QA) Apply smooth fade animation to prevent blink
        dialog.window?.setWindowAnimations(R.style.Animation_OrderMate_Dialog)
        
        // Set title based on level
        val title = if (level == NoteLevel.ITEM) "Add Item Widget" else "Add Order Widget"
        dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle)?.text = title
        
        // Helper function to add widget and dismiss dialog
        fun addWidgetOfType(type: FirebaseWidgetType) {
            // Generate unique label (e.g., "Calendar", "Calendar 2", "Calendar 3")
            val uniqueLabel = widgetManager.generateUniqueLabel(type.displayName)
            
            // Create widget with unique label
            val widget = DefaultWidgetFactory.createWidget(
                type = type,
                label = uniqueLabel,
                level = level,
                isEnabled = true,
                order = if (level == NoteLevel.ITEM) {
                    widgetManager.getItemWidgets().size
                } else {
                    widgetManager.getOrderWidgets().size
                }
            )
            
            if (level == NoteLevel.ITEM) {
                widgetManager.addItemWidget(widget) { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            itemLevelWidgetAdapter?.addWidget(widget)
                        } else {
                            Toast.makeText(context, "Failed to add widget", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                widgetManager.addOrderWidget(widget) { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            orderLevelWidgetAdapter?.addWidget(widget)
                        } else {
                            Toast.makeText(context, "Failed to add widget", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.dismiss()
        }
        
        // Setup click listeners for widget type options
        dialogView.findViewById<View>(R.id.optionCalendar)?.setOnClickListener {
            addWidgetOfType(FirebaseWidgetType.CALENDAR)
        }
        dialogView.findViewById<View>(R.id.optionSingleSelect)?.setOnClickListener {
            addWidgetOfType(FirebaseWidgetType.SINGLE_SELECT)
        }
        dialogView.findViewById<View>(R.id.optionMultiSelect)?.setOnClickListener {
            addWidgetOfType(FirebaseWidgetType.MULTI_SELECT)
        }
        dialogView.findViewById<View>(R.id.optionTextBox)?.setOnClickListener {
            addWidgetOfType(FirebaseWidgetType.TEXT_BOX)
        }
        dialogView.findViewById<View>(R.id.btnCancel)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteItemWidgetDialog(widget: WidgetConfig) {
        showDeleteConfirmationDialog(
            title = "Delete Widget?",
            message = "Are you sure you want to delete \"${widget.label}\"? This action cannot be undone.",
            onConfirm = {
                widgetManager.deleteItemWidget(widget.id) { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            itemLevelWidgetAdapter?.removeWidget(widget)
                            Toast.makeText(context, "Widget deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete widget", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
    
    private fun showDeleteOrderWidgetDialog(widget: WidgetConfig) {
        showDeleteConfirmationDialog(
            title = "Delete Widget?",
            message = "Are you sure you want to delete \"${widget.label}\"? This action cannot be undone.",
            onConfirm = {
                widgetManager.deleteOrderWidget(widget.id) { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            orderLevelWidgetAdapter?.removeWidget(widget)
                            Toast.makeText(context, "Widget deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete widget", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
    
    private fun showDeleteWidgetDialog(widget: PopUpWidget) {
        showDeleteConfirmationDialog(
            title = "Delete Widget?",
            message = "Are you sure you want to delete \"${widget.label}\"? This action cannot be undone.",
            onConfirm = {
                settingsManager.removeWidget(widget.id)
                Toast.makeText(context, "Widget deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    /**
     * Show confirmation dialog to reset item widgets to defaults
     */
    private fun showResetItemWidgetsConfirmDialog() {
        showDeleteConfirmationDialog(
            title = "Reset to Default Widgets?",
            message = "This will replace all Item Level widgets with the default widgets. This action cannot be undone.",
            onConfirm = {
                resetItemWidgetsToDefaults()
            }
        )
    }
    
    /**
     * Show confirmation dialog to reset order widgets to defaults
     */
    private fun showResetOrderWidgetsConfirmDialog() {
        showDeleteConfirmationDialog(
            title = "Reset to Default Widgets?",
            message = "This will replace all Order Level widgets with the default widgets. This action cannot be undone.",
            onConfirm = {
                resetOrderWidgetsToDefaults()
            }
        )
    }
    
    /**
     * Reset item widgets to defaults using atomic operation.
     */
    private fun resetItemWidgetsToDefaults() {
        android.util.Log.d("SettingsFragment", "resetItemWidgetsToDefaults: starting atomic reset")
        
        widgetManager.resetItemWidgetsToDefaults { success ->
            activity?.runOnUiThread {
                if (success) {
                    android.util.Log.d("SettingsFragment", "resetItemWidgetsToDefaults: success, updating UI")
                    val defaults = widgetManager.getItemWidgets()
                    itemLevelWidgetAdapter?.setWidgets(defaults.toMutableList())
                    Toast.makeText(context, "Item widgets reset to defaults", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.e("SettingsFragment", "resetItemWidgetsToDefaults: failed")
                    Toast.makeText(context, "Failed to reset widgets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Reset order widgets to defaults using atomic operation.
     */
    private fun resetOrderWidgetsToDefaults() {
        android.util.Log.d("SettingsFragment", "resetOrderWidgetsToDefaults: starting atomic reset")
        
        widgetManager.resetOrderWidgetsToDefaults { success ->
            activity?.runOnUiThread {
                if (success) {
                    android.util.Log.d("SettingsFragment", "resetOrderWidgetsToDefaults: success, updating UI")
                    val defaults = widgetManager.getOrderWidgets()
                    orderLevelWidgetAdapter?.setWidgets(defaults.toMutableList())
                    Toast.makeText(context, "Order widgets reset to defaults", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.e("SettingsFragment", "resetOrderWidgetsToDefaults: failed")
                    Toast.makeText(context, "Failed to reset widgets", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        
        // #78: Removed duplicate loadTemplatesFromFirebase() call here
        // Templates are loaded in onViewCreated after merchantId is retrieved (line 181)
        // Having it here caused race condition creating duplicate default templates
        
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
                            content = "Your order from {{merchant_name}} is ready for pickup!",
                            subject = "Hello, {{customer_name}}! Your Order Is Ready 🔥"
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
        
        fun updateInputsVisibility() {
            val notificationEnabled = switchScheduledNotifications?.isChecked ?: false
            notificationInputsContainer?.alpha = if (notificationEnabled) 1.0f else 0.5f
            inputNotificationDays?.isEnabled = notificationEnabled
            inputNotificationMinutes?.isEnabled = notificationEnabled
            
            val receiptEnabled = switchScheduledReceipt?.isChecked ?: false
            receiptInputsContainer?.alpha = if (receiptEnabled) 1.0f else 0.5f
            inputReceiptDays?.isEnabled = receiptEnabled
            inputReceiptMinutes?.isEnabled = receiptEnabled
        }
        
        // Toggle listeners for scheduled notifications and receipts
        switchScheduledNotifications?.setOnCheckedChangeListener { _, _ ->
            updateInputsVisibility()
            scheduleAdvancedSettingsSave()
        }
        
        switchScheduledReceipt?.setOnCheckedChangeListener { _, _ ->
            updateInputsVisibility()
            scheduleAdvancedSettingsSave()
        }
        
        // Receipt Settings toggles
        switchPrintNotesCustomer?.setOnCheckedChangeListener { _, _ ->
            scheduleAdvancedSettingsSave()
        }
        
        switchPrintNotesOrder?.setOnCheckedChangeListener { _, _ ->
            scheduleAdvancedSettingsSave()
        }
        
        // #79: Permission Settings toggles
        switchAllowAdminSettings?.setOnCheckedChangeListener { _, _ ->
            scheduleAdvancedSettingsSave()
        }
        
        switchAllowManagersSettings?.setOnCheckedChangeListener { _, _ ->
            scheduleAdvancedSettingsSave()
        }
        
        switchAllowEmployeesSettings?.setOnCheckedChangeListener { _, _ ->
            scheduleAdvancedSettingsSave()
        }
        
        // Initial visibility state
        updateInputsVisibility()
        
        // Notification time listeners (days/hours and minutes) with validation (#40, #41)
        inputNotificationDays?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull()
                if (value != null && (value < 0 || value > 30)) {
                    inputNotificationDays?.error = "Must be 0-30"
                    return
                }
                inputNotificationDays?.error = null
                value?.let { 
                    settingsManager.setNotificationDays(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        inputNotificationMinutes?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull()
                if (value != null && (value < 0 || value > 60)) {
                    inputNotificationMinutes?.error = "Must be 0-60"
                    return
                }
                inputNotificationMinutes?.error = null
                value?.let { 
                    settingsManager.setNotificationMinutes(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Receipt time listeners (days/hours and minutes) with validation (#40, #41)
        inputReceiptDays?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull()
                if (value != null && (value < 0 || value > 30)) {
                    inputReceiptDays?.error = "Must be 0-30"
                    return
                }
                inputReceiptDays?.error = null
                value?.let { 
                    settingsManager.setReceiptDays(it)
                    scheduleAdvancedSettingsSave()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        inputReceiptMinutes?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull()
                if (value != null && (value < 0 || value > 60)) {
                    inputReceiptMinutes?.error = "Must be 0-60"
                    return
                }
                inputReceiptMinutes?.error = null
                value?.let { 
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
                useOrderMateInRegister = switchUseInCloverRegister?.isChecked ?: false,
                useOrderMateRegisterInstead = switchUseOrderMateInstead?.isChecked ?: true,
                scheduledNotificationsEnabled = switchScheduledNotifications?.isChecked ?: false,
                notificationDays = settingsManager.getNotificationDays(),
                notificationMinutes = settingsManager.getNotificationMinutes(),
                scheduledReceiptEnabled = switchScheduledReceipt?.isChecked ?: false,
                receiptDays = settingsManager.getReceiptDays(),
                receiptMinutes = settingsManager.getReceiptMinutes(),
                printNotesOnCustomerReceipts = switchPrintNotesCustomer?.isChecked ?: true,
                printNotesOnOrderReceipts = switchPrintNotesOrder?.isChecked ?: true,
                // #79: Permission Settings
                allowAdminUpdateSettings = switchAllowAdminSettings?.isChecked ?: true,
                allowManagersUpdateSettings = switchAllowManagersSettings?.isChecked ?: true,
                allowEmployeesUpdateSettings = switchAllowEmployeesSettings?.isChecked ?: true
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
        switchUseOrderMateInstead?.isChecked = settingsManager.getUseOrderMateRegisterInstead()
        inputNotificationDays?.setText(settingsManager.getNotificationDays().toString())
        inputNotificationMinutes?.setText(settingsManager.getNotificationMinutes().toString())
        inputReceiptDays?.setText(settingsManager.getReceiptDays().toString())
        inputReceiptMinutes?.setText(settingsManager.getReceiptMinutes().toString())
        
        // Then sync from Firebase (authoritative)
        loadAdvancedSettingsFromFirebase()
        loadPopupSettingsFromFirebase()
    }
    
    private fun loadPopupSettingsFromFirebase() {
        // Load PopupSettings from WidgetManager (which loads from Firebase)
        widgetManager.reloadAll { success ->
            if (success) {
                activity?.runOnUiThread {
                    val settings = widgetManager.getSettings()
                    if (settings != null) {
                        // Update switch based on Firebase value
                        switchUseInCloverRegister?.isChecked = settings.showOMButtonInRegister
                        settingsManager.setUseOrderMateRegister(settings.showOMButtonInRegister)
                        
                        // Update item/order notes toggles (#34) - both in dedicated tabs and General panel
                        switchItemNotesEnabled?.isChecked = settings.itemNotesEnabled
                        switchOrderNotesEnabled?.isChecked = settings.orderNotesEnabled
                        switchItemNotesEnabledGeneral?.isChecked = settings.itemNotesEnabled
                        switchOrderNotesEnabledGeneral?.isChecked = settings.orderNotesEnabled
                        
                        // Update editor card visibility based on toggle state
                        itemLevelEditorCard?.alpha = if (settings.itemNotesEnabled) 1.0f else 0.5f
                        btnAddItemLevelWidget?.isEnabled = settings.itemNotesEnabled
                        orderLevelEditorCard?.alpha = if (settings.orderNotesEnabled) 1.0f else 0.5f
                        btnAddOrderLevelWidget?.isEnabled = settings.orderNotesEnabled
                    }
                }
            }
        }
    }
    
    private fun loadAdvancedSettingsFromFirebase() {
        merchantId?.let { mid ->
            firebase.getAdvancedSettings(mid) { settings ->
                activity?.runOnUiThread {
                    // Update local settings manager - sync all settings from Firebase
                    settingsManager.setNotificationDays(settings.notificationDays)
                    settingsManager.setNotificationMinutes(settings.notificationMinutes)
                    settingsManager.setReceiptDays(settings.receiptDays)
                    settingsManager.setReceiptMinutes(settings.receiptMinutes)
                    settingsManager.setUseOrderMateRegister(settings.useOrderMateInRegister)
                    settingsManager.setUseOrderMateRegisterInstead(settings.useOrderMateRegisterInstead)
                    settingsManager.setScheduledNotificationsEnabled(settings.scheduledNotificationsEnabled)
                    settingsManager.setScheduledReceiptEnabled(settings.scheduledReceiptEnabled)
                    settingsManager.setPrintNotesOnCustomerReceipts(settings.printNotesOnCustomerReceipts)
                    settingsManager.setPrintNotesOnOrderReceipts(settings.printNotesOnOrderReceipts)
                    
                    // Update UI - toggles
                    switchUseOrderMateInstead?.isChecked = settings.useOrderMateRegisterInstead
                    switchScheduledNotifications?.isChecked = settings.scheduledNotificationsEnabled
                    switchScheduledReceipt?.isChecked = settings.scheduledReceiptEnabled
                    
                    // Update UI - inputs
                    inputNotificationDays?.setText(settings.notificationDays.toString())
                    inputNotificationMinutes?.setText(settings.notificationMinutes.toString())
                    inputReceiptDays?.setText(settings.receiptDays.toString())
                    inputReceiptMinutes?.setText(settings.receiptMinutes.toString())
                    
                    // Update input visibility based on toggle state
                    val notificationEnabled = settings.scheduledNotificationsEnabled
                    notificationInputsContainer?.alpha = if (notificationEnabled) 1.0f else 0.5f
                    inputNotificationDays?.isEnabled = notificationEnabled
                    inputNotificationMinutes?.isEnabled = notificationEnabled
                    
                    val receiptEnabled = settings.scheduledReceiptEnabled
                    receiptInputsContainer?.alpha = if (receiptEnabled) 1.0f else 0.5f
                    inputReceiptDays?.isEnabled = receiptEnabled
                    inputReceiptMinutes?.isEnabled = receiptEnabled
                    
                    // Update Receipt Settings toggles
                    switchPrintNotesCustomer?.isChecked = settings.printNotesOnCustomerReceipts
                    switchPrintNotesOrder?.isChecked = settings.printNotesOnOrderReceipts
                    
                    // #79: Update Permission Settings toggles
                    switchAllowAdminSettings?.isChecked = settings.allowAdminUpdateSettings
                    switchAllowManagersSettings?.isChecked = settings.allowManagersUpdateSettings
                    switchAllowEmployeesSettings?.isChecked = settings.allowEmployeesUpdateSettings
                }
            }
        }
    }
    
    // ==================== Filter Panel ====================
    
    /**
     * Load all enabled widgets (Item + Order level) and display toggles for showInFilter
     * Only shows widgets that are enabled - TEXT_BOX types excluded since they can't be filtered
     * Uses expandable card layout matching Item/Order Level settings pages
     */
    private fun loadFilterWidgetToggles() {
        // Get Item Level widgets (enabled, not TEXT_BOX)
        val itemLevelWidgets = widgetManager.getFilterableItemWidgets()
        
        // Get Order Level widgets (enabled, not TEXT_BOX)
        val orderLevelWidgets = widgetManager.getFilterableOrderWidgets()
        
        // DEBUG: Log widget counts
        android.util.Log.d("FilterSettingsDebug", "=== loadFilterWidgetToggles ===")
        android.util.Log.d("FilterSettingsDebug", "itemLevelWidgets count: ${itemLevelWidgets.size}")
        android.util.Log.d("FilterSettingsDebug", "orderLevelWidgets count: ${orderLevelWidgets.size}")
        orderLevelWidgets.forEach { w ->
            android.util.Log.d("FilterSettingsDebug", "  ORDER: ${w.label}, enabled=${w.isEnabled}, type=${w.type}")
        }
        
        val hasItemWidgets = itemLevelWidgets.isNotEmpty()
        val hasOrderWidgets = orderLevelWidgets.isNotEmpty()
        
        // Show/hide section headers
        filterItemLevelHeader?.visibility = if (hasItemWidgets) View.VISIBLE else View.GONE
        filterOrderLevelHeader?.visibility = if (hasOrderWidgets) View.VISIBLE else View.GONE
        
        // Show empty state if no widgets
        if (!hasItemWidgets && !hasOrderWidgets) {
            filterEmptyState?.visibility = View.VISIBLE
            filterItemLevelRecyclerView?.visibility = View.GONE
            filterOrderLevelRecyclerView?.visibility = View.GONE
            return
        }
        filterEmptyState?.visibility = View.GONE
        
        // Setup Item Level RecyclerView with adapter
        if (hasItemWidgets) {
            filterItemLevelRecyclerView?.visibility = View.VISIBLE
            filterItemLevelAdapter = FilterWidgetAdapter { widget ->
                widgetManager.updateItemWidget(widget) { success ->
                    if (!success) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            filterItemLevelAdapter?.setAdapterName("ITEM")
            filterItemLevelAdapter?.setWidgets(itemLevelWidgets.toMutableList())
            filterItemLevelRecyclerView?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = filterItemLevelAdapter
            }
        } else {
            filterItemLevelRecyclerView?.visibility = View.GONE
        }
        
        // Setup Order Level - use LinearLayout with dynamic views instead of RecyclerView
        filterOrderLevelRecyclerView?.visibility = View.GONE  // Hide RecyclerView
        if (hasOrderWidgets) {
            filterOrderLevelWidgetsContainer?.visibility = View.VISIBLE
            filterOrderLevelWidgetsContainer?.removeAllViews()
            orderLevelWidgets.forEach { widget ->
                val itemView = layoutInflater.inflate(R.layout.item_filter_widget, filterOrderLevelWidgetsContainer, false)
                bindFilterWidgetView(itemView, widget) { updatedWidget ->
                    widgetManager.updateOrderWidget(updatedWidget) { success ->
                        if (!success) {
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                filterOrderLevelWidgetsContainer?.addView(itemView)
            }
        } else {
            filterOrderLevelWidgetsContainer?.visibility = View.GONE
        }
    }
    
    // Track expanded state for order-level filter widgets
    private val orderFilterExpandedIds = mutableSetOf<String>()
    
    private fun bindFilterWidgetView(itemView: View, widget: WidgetConfig, onUpdate: (WidgetConfig) -> Unit) {
        val widgetIconContainer: View = itemView.findViewById(R.id.filterWidgetIconContainer)
        val widgetIcon: ImageView = itemView.findViewById(R.id.filterWidgetIcon)
        val widgetTitle: TextView = itemView.findViewById(R.id.filterWidgetTitle)
        val widgetTypeView: TextView = itemView.findViewById(R.id.filterWidgetType)
        val widgetToggle: Switch = itemView.findViewById(R.id.filterWidgetToggle)
        val expandChevron: ImageView = itemView.findViewById(R.id.filterExpandChevron)
        val widgetHeader: View = itemView.findViewById(R.id.filterWidgetHeader)
        val widgetBody: View = itemView.findViewById(R.id.filterWidgetBody)
        val optionsLabel: TextView = itemView.findViewById(R.id.filterOptionsLabel)
        val valuesContainer: FlexboxLayout = itemView.findViewById(R.id.filterValuesContainer)
        
        // Set title and type
        widgetTitle.text = widget.label
        widgetTypeView.text = widget.type.displayName
        
        // Set icon and colors based on type - uses centralized WidgetColorUtils
        val iconRes = WidgetColorUtils.getIconForWidgetType(widget.type)
        val tintColor = WidgetColorUtils.getColorForWidgetType(widget.type)
        val bgRes = WidgetColorUtils.getIconBackgroundForWidgetType(widget.type)
        widgetIcon.setImageResource(iconRes)
        widgetIcon.setColorFilter(tintColor)
        widgetIconContainer.setBackgroundResource(bgRes)
        
        // Set toggle state
        widgetToggle.isChecked = widget.showInFilter
        widgetToggle.setOnCheckedChangeListener { _, isChecked ->
            widget.showInFilter = isChecked
            onUpdate(widget)
        }
        
        // (#77) Show/hide options based on widget type - same logic as FilterWidgetAdapter
        val hasDropdown = widget.type == com.orderMate.modals.WidgetType.SINGLE_SELECT || 
                          widget.type == com.orderMate.modals.WidgetType.MULTI_SELECT ||
                          widget.type == com.orderMate.modals.WidgetType.TEXT_BOX
        
        if (hasDropdown && widget.type != com.orderMate.modals.WidgetType.TEXT_BOX && widget.options.isNotEmpty()) {
            optionsLabel.visibility = View.VISIBLE
            setupOrderFilterOptionsDisplay(valuesContainer, widget, tintColor)
            expandChevron.visibility = View.VISIBLE
        } else if (widget.type == com.orderMate.modals.WidgetType.TEXT_BOX) {
            optionsLabel.text = "Type: Free Text"
            optionsLabel.visibility = View.VISIBLE
            valuesContainer.removeAllViews()
            expandChevron.visibility = View.VISIBLE
        } else {
            // CALENDAR - no dropdown needed
            optionsLabel.visibility = View.GONE
            valuesContainer.removeAllViews()
            expandChevron.visibility = View.GONE
        }
        
        // Expand/collapse state - only for types with dropdown
        if (hasDropdown) {
            val isExpanded = orderFilterExpandedIds.contains(widget.id)
            widgetBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandChevron.rotation = if (isExpanded) 180f else 0f
            
            // Header click to expand/collapse
            widgetHeader.setOnClickListener {
                val expanding = !orderFilterExpandedIds.contains(widget.id)
                if (expanding) {
                    orderFilterExpandedIds.add(widget.id)
                } else {
                    orderFilterExpandedIds.remove(widget.id)
                }
                // Animate
                widgetBody.visibility = if (expanding) View.VISIBLE else View.GONE
                expandChevron.animate().rotation(if (expanding) 180f else 0f).setDuration(200).start()
            }
        } else {
            // No dropdown - hide body, no click handler
            widgetBody.visibility = View.GONE
            widgetHeader.setOnClickListener(null)
        }
    }
    
    private fun setupOrderFilterOptionsDisplay(container: FlexboxLayout, widget: WidgetConfig, tintColor: Int) {
        container.removeAllViews()
        
        widget.options.forEach { option ->
            // Use shared filter chip function for consistent styling
            val chip = WidgetColorUtils.createFilterTabChip(requireContext(), option.label, tintColor)
            container.addView(chip)
        }
    }
    
    /**
     * Setup Clover filter cards - load saved state, add listeners, populate options
     */
    private fun setupCloverFilterToggles() {
        // Load saved states
        switchFilterOrderDate?.isChecked = settingsManager.getShowFilterOrderDate()
        switchFilterPaymentStatus?.isChecked = settingsManager.getShowFilterPaymentStatus()
        switchFilterOrderStatus?.isChecked = settingsManager.getShowFilterOrderStatus()
        switchFilterPaymentType?.isChecked = settingsManager.getShowFilterPaymentType()
        switchFilterEmployee?.isChecked = settingsManager.getShowFilterEmployee()
        
        // Set up toggle listeners
        switchFilterOrderDate?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowFilterOrderDate(isChecked)
        }
        switchFilterPaymentStatus?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowFilterPaymentStatus(isChecked)
        }
        switchFilterOrderStatus?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowFilterOrderStatus(isChecked)
        }
        switchFilterPaymentType?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowFilterPaymentType(isChecked)
        }
        switchFilterEmployee?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowFilterEmployee(isChecked)
        }
        
        // Set up expand/collapse for Payment Status
        paymentStatusHeader?.setOnClickListener {
            toggleCloverFilterExpansion(paymentStatusBody, paymentStatusChevron)
        }
        
        // Set up expand/collapse for Order Status
        orderStatusHeader?.setOnClickListener {
            toggleCloverFilterExpansion(orderStatusBody, orderStatusChevron)
        }
        
        // Set up expand/collapse for Payment Type
        paymentTypeHeader?.setOnClickListener {
            toggleCloverFilterExpansion(paymentTypeBody, paymentTypeChevron)
        }
        
        // Populate options
        populateCloverFilterOptions()
    }
    
    /**
     * Toggle expand/collapse state for a Clover filter card
     */
    private fun toggleCloverFilterExpansion(body: View?, chevron: ImageView?) {
        val isExpanded = body?.visibility == View.VISIBLE
        body?.visibility = if (isExpanded) View.GONE else View.VISIBLE
        chevron?.animate()?.rotation(if (isExpanded) 0f else 180f)?.setDuration(200)?.start()
    }
    
    /**
     * Populate the options tags for each Clover filter - uses WidgetColorUtils for consistency
     * Payment Status uses Clover SDK PaymentState enum: PAID, PARTIALLY_PAID, REFUNDED, PARTIALLY_REFUNDED, CREDITED
     * Note: OPEN/Unpaid is excluded - order status filter handles open/closed status
     * (#76) Updated to use title case (only capitalize first letter) per text changes requirement
     */
    private fun populateCloverFilterOptions() {
        // Payment Status options (Yellow) - display names for Clover PaymentState enum
        // (#76) Title case: only first letter capitalized
        // Note: Unpaid is excluded - order status (Open/Closed) handles this
        val paymentStatusValues = listOf("Paid", "Partially paid", "Refunded", "Partially refunded", "Credited")
        populateOptionsContainer(paymentStatusOptions, paymentStatusValues, WidgetColorUtils.COLOR_PAYMENT_STATUS)
        
        // Order Status options (Red)
        // (#76) Title case: only first letter capitalized
        val orderStatusValues = listOf("Open", "Closed")
        populateOptionsContainer(orderStatusOptions, orderStatusValues, WidgetColorUtils.COLOR_ORDER_STATUS)
        
        // Payment Type options (Grey) - already in title case
        val paymentTypeValues = listOf("Cash", "Credit Card", "Debit Card", "Check", "Gift Card", "Other")
        populateOptionsContainer(paymentTypeOptions, paymentTypeValues, WidgetColorUtils.COLOR_PAYMENT_TYPE)
    }
    
    /**
     * Populate a FlexboxLayout with option tags using shared filter chip styling
     */
    private fun populateOptionsContainer(
        container: com.google.android.flexbox.FlexboxLayout?, 
        values: List<String>,
        tintColor: Int
    ) {
        container?.removeAllViews()
        values.forEach { value ->
            // Use shared filter chip function for consistent styling
            val chip = WidgetColorUtils.createFilterTabChip(requireContext(), value, tintColor)
            container?.addView(chip)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
        // (#81 QA) Apply smooth fade animation to prevent blink
        dialog.window?.setWindowAnimations(R.style.Animation_OrderMate_Dialog)
        
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
        tabItemLevelNotes = null
        tabOrderLevelNotes = null
        tabFilter = null
        tabNotification = null
        tabAdvanced = null
        panelGeneral = null
        panelItemLevelNotes = null
        panelOrderLevelNotes = null
        panelFilter = null
        panelNotification = null
        panelAdvanced = null
        switchUseInCloverRegister = null
        switchUseOrderMateInstead = null
        switchItemNotesEnabledGeneral = null
        switchOrderNotesEnabledGeneral = null
        filterItemLevelHeader = null
        filterOrderLevelHeader = null
        filterItemLevelWidgetsContainer = null
        filterOrderLevelWidgetsContainer = null
        filterItemLevelRecyclerView = null
        filterOrderLevelRecyclerView = null
        filterItemLevelAdapter = null
        filterOrderLevelAdapter = null
        filterEmptyState = null
        switchFilterOrderDate = null
        switchFilterPaymentStatus = null
        switchFilterOrderStatus = null
        switchFilterPaymentType = null
        switchFilterEmployee = null
        paymentStatusHeader = null
        paymentStatusBody = null
        paymentStatusChevron = null
        paymentStatusOptions = null
        orderStatusHeader = null
        orderStatusBody = null
        orderStatusChevron = null
        orderStatusOptions = null
        paymentTypeHeader = null
        paymentTypeBody = null
        paymentTypeChevron = null
        paymentTypeOptions = null
        itemLevelWidgetRecyclerView = null
        orderLevelWidgetRecyclerView = null
        btnAddItemLevelWidget = null
        btnResetItemLevelWidgets = null
        btnAddOrderLevelWidget = null
        btnResetOrderLevelWidgets = null
        templateRecyclerView = null
        btnAddTemplate = null
        inputNotificationDays = null
        inputNotificationMinutes = null
        inputReceiptDays = null
        inputReceiptMinutes = null
        switchPrintNotesCustomer = null
        switchPrintNotesOrder = null
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
        private var currentWidgetBorderColor: Int = 0xFFFFFFFF.toInt()

        fun bind(widget: PopUpWidget, position: Int) {
            widgetTitle.text = widget.label
            widgetType.text = widget.type.defaultLabel
            widgetToggle.isChecked = widget.enabled
            inputWidgetLabel.setText(widget.label)

            // Set icon and colors based on type - uses centralized WidgetColorUtils
            val iconRes = WidgetColorUtils.getIconForWidgetType(widget.type)
            val tintColor = WidgetColorUtils.getColorForWidgetType(widget.type)
            val borderColor = WidgetColorUtils.getBgColorForWidgetType(widget.type)
            val bgRes = WidgetColorUtils.getIconBackgroundForWidgetType(widget.type)
            widgetIcon.setImageResource(iconRes)
            widgetIcon.setColorFilter(tintColor)
            widgetIconContainer.setBackgroundResource(bgRes)
            
            // Store colors for value pills
            currentWidgetTintColor = tintColor
            currentWidgetBorderColor = borderColor

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
            // Use shared editable pill function for consistent styling
            return WidgetColorUtils.createEditableValuePill(
                itemView.context, text, currentWidgetTintColor, currentWidgetBorderColor, onRemove
            )
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

// ==================== Firebase-backed Widget Editor Adapter ====================

class FirebaseWidgetEditorAdapter(
    private val onWidgetUpdate: (WidgetConfig) -> Unit,
    private val onWidgetDelete: (WidgetConfig) -> Unit
) : RecyclerView.Adapter<FirebaseWidgetEditorAdapter.ViewHolder>() {

    private var itemTouchHelper: ItemTouchHelper? = null
    private val widgets = mutableListOf<WidgetConfig>()
    private val expandedIds = mutableSetOf<String>()

    fun setDragHelper(helper: ItemTouchHelper) {
        itemTouchHelper = helper
    }
    
    fun setWidgets(newWidgets: MutableList<WidgetConfig>) {
        widgets.clear()
        widgets.addAll(newWidgets)
        notifyDataSetChanged()
    }

    fun addWidget(widget: WidgetConfig) {
        widgets.add(widget)
        notifyItemInserted(widgets.size - 1)
    }

    fun removeWidget(widget: WidgetConfig) {
        val index = widgets.indexOfFirst { it.id == widget.id }
        if (index >= 0) {
            widgets.removeAt(index)
            expandedIds.remove(widget.id)
            notifyItemRemoved(index)
        }
    }

    fun moveWidget(from: Int, to: Int) {
        Collections.swap(widgets, from, to)
        notifyItemMoved(from, to)
    }
    
    fun getWidgets(): List<WidgetConfig> {
        return widgets.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_widget_editor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(widgets[position])
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
        private var currentWidgetBorderColor: Int = 0xFFFFFFFF.toInt()
        private var saveHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private var saveRunnable: Runnable? = null
        private var labelWatcher: TextWatcher? = null

        @SuppressLint("ClickableViewAccessibility")
        fun bind(widget: WidgetConfig) {
            // Remove old watcher BEFORE setText to prevent triggering save
            labelWatcher?.let { inputWidgetLabel.removeTextChangedListener(it) }
            
            widgetTitle.text = widget.label
            widgetType.text = widget.type.displayName
            widgetToggle.isChecked = widget.isEnabled
            inputWidgetLabel.setText(widget.label)

            // Set icon and colors based on type - uses centralized WidgetColorUtils
            val iconRes = WidgetColorUtils.getIconForWidgetType(widget.type)
            val tintColor = WidgetColorUtils.getColorForWidgetType(widget.type)
            val borderColor = WidgetColorUtils.getBgColorForWidgetType(widget.type)
            val bgRes = WidgetColorUtils.getIconBackgroundForWidgetType(widget.type)
            widgetIcon.setImageResource(iconRes)
            widgetIcon.setColorFilter(tintColor)
            widgetIconContainer.setBackgroundResource(bgRes)
            currentWidgetTintColor = tintColor
            currentWidgetBorderColor = borderColor

            // Show/hide options for select types
            val hasOptions = widget.type == com.orderMate.modals.WidgetType.SINGLE_SELECT || 
                            widget.type == com.orderMate.modals.WidgetType.MULTI_SELECT
            optionsContainer.visibility = if (hasOptions) View.VISIBLE else View.GONE

            // Setup values
            setupValues(widget)

            // Expand/collapse using widget ID
            val isExpanded = expandedIds.contains(widget.id)
            widgetBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandChevron.rotation = if (isExpanded) 180f else 0f

            // Header click to expand/collapse
            widgetHeader.setOnClickListener {
                val expanding = !expandedIds.contains(widget.id)
                if (expanding) {
                    expandedIds.add(widget.id)
                } else {
                    expandedIds.remove(widget.id)
                }
                animateExpand(expanding)
            }

            // Toggle - setOnClickListener only fires on USER click, not when setting isChecked
            widgetToggle.setOnClickListener {
                widget.isEnabled = widgetToggle.isChecked
                scheduleSave(widget)
            }

            // Label change - add new watcher (old one removed at start of bind)
            labelWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    widget.label = s?.toString() ?: ""
                    widgetTitle.text = widget.label
                    scheduleSave(widget)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            inputWidgetLabel.addTextChangedListener(labelWatcher)

            // Add option
            inputAddOption.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || 
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    val value = inputAddOption.text.toString().trim()
                    if (value.isNotEmpty()) {
                        val newOption = com.orderMate.modals.WidgetOption(
                            id = java.util.UUID.randomUUID().toString(),
                            label = value,
                            value = value
                        )
                        widget.options.add(newOption)
                        inputAddOption.text.clear()
                        setupValues(widget)
                        scheduleSave(widget)
                    }
                    true
                } else false
            }

            // Delete widget - disable if last widget
            btnDeleteWidget.isEnabled = widgets.size > 1
            btnDeleteWidget.alpha = if (widgets.size > 1) 1.0f else 0.3f
            btnDeleteWidget.setOnClickListener {
                onWidgetDelete(widget)
            }

            // Drag handle
            dragHandle.setOnTouchListener { _, event ->
                android.util.Log.d("WidgetDragDebug", "dragHandle touched: action=${event.actionMasked}")
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    android.util.Log.d("WidgetDragDebug", "dragHandle ACTION_DOWN: calling startDrag, itemTouchHelper=$itemTouchHelper")
                    itemTouchHelper?.startDrag(this)
                }
                false
            }
        }
        
        private fun scheduleSave(widget: WidgetConfig) {
            saveRunnable?.let { saveHandler.removeCallbacks(it) }
            saveRunnable = Runnable { onWidgetUpdate(widget) }
            saveHandler.postDelayed(saveRunnable!!, 500) // Debounce 500ms
        }

        private fun setupValues(widget: WidgetConfig) {
            valuesContainer.removeAllViews()
            val density = itemView.context.resources.displayMetrics.density

            widget.options.forEach { option ->
                val pill = createValuePill(option.label, density) {
                    widget.options.removeAll { it.id == option.id }
                    setupValues(widget)
                    scheduleSave(widget)
                }
                valuesContainer.addView(pill)
            }
        }

        private fun createValuePill(text: String, density: Float, onRemove: () -> Unit): LinearLayout {
            // Use shared editable pill function for consistent styling
            return WidgetColorUtils.createEditableValuePill(
                itemView.context, text, currentWidgetTintColor, currentWidgetBorderColor, onRemove
            )
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
        private val inputTemplateSubject: EditText = itemView.findViewById(R.id.inputTemplateSubject)  // #64
        private val inputTemplateContent: EditText = itemView.findViewById(R.id.inputTemplateContent)
        private val charCount: TextView = itemView.findViewById(R.id.charCount)
        private val btnDeleteTemplate: View = itemView.findViewById(R.id.btnDeleteTemplate)
        
        private var currentTextWatcher: TextWatcher? = null
        private var currentNameWatcher: TextWatcher? = null
        private var currentSubjectWatcher: TextWatcher? = null  // #64
        private var saveHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private var saveRunnable: Runnable? = null

        fun bind(template: NotificationTemplate) {
            templateName.text = template.name
            templatePreview.text = if (template.content.length > 50) 
                "${template.content.take(50)}..." else template.content
            
            // Remove previous watchers
            currentTextWatcher?.let { inputTemplateContent.removeTextChangedListener(it) }
            currentNameWatcher?.let { inputTemplateName.removeTextChangedListener(it) }
            currentSubjectWatcher?.let { inputTemplateSubject.removeTextChangedListener(it) }  // #64
            
            inputTemplateName.setText(template.name)
            inputTemplateSubject.setText(template.subject)  // #64
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

            // #64: Debounced save for subject changes
            currentSubjectWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val subject = s?.toString() ?: ""
                    template.subject = subject
                    scheduleSave(template)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            inputTemplateSubject.addTextChangedListener(currentSubjectWatcher)

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

// ==================== Filter Widget Adapter ====================

/**
 * Adapter for displaying filter widgets in an expandable card format.
 * Read-only version of FirebaseWidgetEditorAdapter - no editing or deleting.
 * Shows toggle for showInFilter and expandable options view.
 */
class FilterWidgetAdapter(
    private val onWidgetUpdate: (WidgetConfig) -> Unit
) : RecyclerView.Adapter<FilterWidgetAdapter.ViewHolder>() {

    private val widgets = mutableListOf<WidgetConfig>()
    private val expandedIds = mutableSetOf<String>()

    private var adapterName = "unknown"
    
    fun setAdapterName(name: String) {
        adapterName = name
    }
    
    fun setWidgets(newWidgets: MutableList<WidgetConfig>) {
        android.util.Log.d("FilterAdapterDebug", "[$adapterName] setWidgets called with ${newWidgets.size} widgets")
        widgets.clear()
        widgets.addAll(newWidgets)
        android.util.Log.d("FilterAdapterDebug", "[$adapterName] After addAll, widgets.size = ${widgets.size}")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        android.util.Log.d("FilterAdapterDebug", "[$adapterName] onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_widget, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        android.util.Log.d("FilterAdapterDebug", "[$adapterName] onBindViewHolder position=$position, widget=${widgets[position].label}")
        holder.bind(widgets[position])
    }

    override fun getItemCount(): Int {
        android.util.Log.d("FilterAdapterDebug", "[$adapterName] getItemCount called, returning ${widgets.size}")
        return widgets.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val widgetIconContainer: View = itemView.findViewById(R.id.filterWidgetIconContainer)
        private val widgetIcon: ImageView = itemView.findViewById(R.id.filterWidgetIcon)
        private val widgetTitle: TextView = itemView.findViewById(R.id.filterWidgetTitle)
        private val widgetType: TextView = itemView.findViewById(R.id.filterWidgetType)
        private val widgetToggle: Switch = itemView.findViewById(R.id.filterWidgetToggle)
        private val expandChevron: ImageView = itemView.findViewById(R.id.filterExpandChevron)
        private val widgetHeader: View = itemView.findViewById(R.id.filterWidgetHeader)
        private val widgetBody: View = itemView.findViewById(R.id.filterWidgetBody)
        private val optionsLabel: TextView = itemView.findViewById(R.id.filterOptionsLabel)
        private val valuesContainer: com.google.android.flexbox.FlexboxLayout = itemView.findViewById(R.id.filterValuesContainer)

        fun bind(widget: WidgetConfig) {
            widgetTitle.text = widget.label
            widgetType.text = widget.type.displayName
            widgetToggle.isChecked = widget.showInFilter

            // Set icon and colors based on type - uses centralized WidgetColorUtils
            val iconRes = WidgetColorUtils.getIconForWidgetType(widget.type)
            val tintColor = WidgetColorUtils.getColorForWidgetType(widget.type)
            val bgRes = WidgetColorUtils.getIconBackgroundForWidgetType(widget.type)
            widgetIcon.setImageResource(iconRes)
            widgetIcon.setColorFilter(tintColor)
            widgetIconContainer.setBackgroundResource(bgRes)

            // (#77) Show/hide options based on widget type
            // Types that need dropdown: SINGLE_SELECT, MULTI_SELECT, TEXT_BOX
            // Types that DON'T need dropdown: CALENDAR (and Employee for Clover filters)
            val hasDropdown = widget.type == com.orderMate.modals.WidgetType.SINGLE_SELECT || 
                              widget.type == com.orderMate.modals.WidgetType.MULTI_SELECT ||
                              widget.type == com.orderMate.modals.WidgetType.TEXT_BOX
            
            if (hasDropdown && widget.type != com.orderMate.modals.WidgetType.TEXT_BOX && widget.options.isNotEmpty()) {
                optionsLabel.visibility = View.VISIBLE
                setupOptionsDisplay(widget, tintColor)
                expandChevron.visibility = View.VISIBLE
            } else if (widget.type == com.orderMate.modals.WidgetType.TEXT_BOX) {
                optionsLabel.text = "Type: Free Text"
                optionsLabel.visibility = View.VISIBLE
                valuesContainer.removeAllViews()
                expandChevron.visibility = View.VISIBLE
            } else {
                // CALENDAR - no dropdown needed
                optionsLabel.visibility = View.GONE
                valuesContainer.removeAllViews()
                expandChevron.visibility = View.GONE
            }

            // Expand/collapse state - only for types with dropdown
            if (hasDropdown) {
                val isExpanded = expandedIds.contains(widget.id)
                widgetBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
                expandChevron.rotation = if (isExpanded) 180f else 0f

                // Header click to expand/collapse
                widgetHeader.setOnClickListener {
                    val expanding = !expandedIds.contains(widget.id)
                    if (expanding) {
                        expandedIds.add(widget.id)
                    } else {
                        expandedIds.remove(widget.id)
                    }
                    animateExpand(expanding)
                }
            } else {
                // No dropdown - hide body, no click handler
                widgetBody.visibility = View.GONE
                widgetHeader.setOnClickListener(null)
            }

            // Toggle showInFilter
            widgetToggle.setOnCheckedChangeListener { _, isChecked ->
                widget.showInFilter = isChecked
                onWidgetUpdate(widget)
            }
        }

        private fun setupOptionsDisplay(widget: WidgetConfig, tintColor: Int) {
            valuesContainer.removeAllViews()
            
            widget.options.forEach { option ->
                // Use shared filter chip function for consistent styling
                val chip = WidgetColorUtils.createFilterTabChip(itemView.context, option.label, tintColor)
                valuesContainer.addView(chip)
            }
        }

        private fun animateExpand(expand: Boolean) {
            if (expand) {
                widgetBody.visibility = View.VISIBLE
                widgetBody.alpha = 0f
                widgetBody.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            } else {
                widgetBody.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { widgetBody.visibility = View.GONE }
                    .start()
            }
            expandChevron.animate()
                .rotation(if (expand) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }
}
