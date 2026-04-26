package com.orderMate.fragment


import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.order.Order
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.activities.OverlayActivity
import com.orderMate.adapters.DrawerItemAdapter
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.OrdermateBasketLayoutBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.modals.ItemModal
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetType
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.SettingsManager
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.countElementsByUniqueKeys
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerName
import com.orderMate.utils.hideView
import com.orderMate.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloatingWidgetService : Service(), IOrderItemClickListener {

    companion object {
        var isShowing: Boolean = false
        var lastOrder: Order? = null
        var instance: FloatingWidgetService? = null
        const val EXTRA_PERMANENT_MODE = "permanent_mode"
        const val ACTION_POPUP_CLOSED = "com.orderMate.ACTION_POPUP_CLOSED"
    }

    private lateinit var windowManager: WindowManager
    private var params: WindowManager.LayoutParams? = null
    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private val binding: OrdermateBasketLayoutBinding? by lazy {
        OrdermateBasketLayoutBinding.inflate(LayoutInflater.from(this))
    }
    
    // Flag to prevent immediate close when opening drawer
    private var isDrawerOpening = false
    
    // Flag for permanent overlay mode (Use OrderMate Register Instead)
    private var isPermanentMode: Boolean = false
    
    // Flag to prevent duplicate data fetching
    @Volatile
    private var isFetchingData = false
    
    // Flag to track if a refresh was requested while fetching (prevents race conditions)
    @Volatile
    private var pendingRefresh = false
    
    // Flag to track when popup is open (prevents drawer close in permanent mode)
    private var isPopupOpen = false
    
    // Broadcast receiver to listen for popup closed events
    private val popupClosedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_POPUP_CLOSED) {
                isPopupOpen = false
            }
        }
    }

    private val prefManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(applicationContext)
    }
    
    private val settingsManager: SettingsManager by lazy {
        SettingsManager(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isShowing = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Register broadcast receiver for popup closed events
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(popupClosedReceiver, IntentFilter(ACTION_POPUP_CLOSED), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(popupClosedReceiver, IntentFilter(ACTION_POPUP_CLOSED))
        }
        
        // Register OnOrderChangedListener once to handle any order changes
        // This ensures drawer stays up-to-date with real-time order modifications
        try {
            MyApp.getInstance().getOrderConnector().addOnOrderChangedListener { orderId, _ ->
                Log.d("DrawerState", "OnOrderChangedListener triggered for order: $orderId")
                if (isShowing) {
                    getTheOrderData()
                }
            }
        } catch (e: Exception) {
            Log.e("DrawerState", "Failed to register OnOrderChangedListener: ${e.message}")
        }
        
        // Check if we're in permanent mode from settings
        isPermanentMode = settingsManager.getUseOrderMateRegisterInstead()
        
        if (isPermanentMode) {
            // Permanent mode: start with drawer expanded, positioned over Clover register item list
            windowManager.addView(binding?.root, setTheWindowParamsForPermanentOverlay())
            binding?.orderMateButton?.hideView()
            binding?.permanentModeBackground?.showView()  // Show opaque dark background
            // Use no-rounded-corners background for permanent mode
            binding?.container?.setBackgroundResource(R.drawable.bg_drawer_permanent)
            // Set container to fill the window in permanent mode
            binding?.container?.layoutParams = binding?.container?.layoutParams?.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            binding?.container?.showView()
            binding?.transparentContainer?.visibility = View.GONE  // No dimming overlay needed
            getTheOrderData()
        } else {
            // Normal mode: show floating button
            windowManager.addView(binding?.root, setTheWindowParams())
            binding?.permanentModeBackground?.hideView()  // Ensure background is hidden
        }

        setupClickListener()
        setUpTouchListener()
    }
    
    /**
     * WindowManager params for permanent overlay mode.
     * Positions the drawer over Clover register item list (left panel):
     * - Top aligned with bottom border of "Register (DEV)" header
     * - Bottom aligned with top of Subtotal/Tax/Total footer section
     * - Covers the full width of the left panel
     * 
     * Uses percentage-based positioning to work across different Clover devices:
     * - Flex: 720x1280 @ 320dpi (xhdpi)
     * - Mini: 1280x800 @ 213dpi (tvdpi)
     * - Station 2018: 1920x1080 @ 213dpi (tvdpi)
     * - Station: 1366x768 @ 160dpi (mdpi)
     */
    private fun setTheWindowParamsForPermanentOverlay(): WindowManager.LayoutParams {
        // Get real screen dimensions including nav bar
        val realMetrics = android.util.DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            realMetrics.widthPixels = bounds.width()
            realMetrics.heightPixels = bounds.height()
            realMetrics.density = resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(realMetrics)
        }
        
        val screenWidth = realMetrics.widthPixels
        val screenHeight = realMetrics.heightPixels
        
        // Clover register left panel width: 50% of screen width (flush with right edge of item list)
        val drawerWidth = (screenWidth * 0.50).toInt()
        
        // Top offset: approximately 11% of screen height (status bar + Register header) - already flush
        val topOffset = (screenHeight * 0.11).toInt()
        
        // Bottom offset: approximately 7% of screen height (Save/Pay footer container only)
        val bottomOffset = (screenHeight * 0.07).toInt()
        
        val drawerHeight = screenHeight - topOffset - bottomOffset
        
        val permanentParams = WindowManager.LayoutParams(
            drawerWidth,
            drawerHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        // Position at left side (START), below header
        permanentParams.gravity = Gravity.START or Gravity.TOP
        permanentParams.x = 0
        permanentParams.y = topOffset
        return permanentParams
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTouchListener() {
        binding?.container1?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX =params?.x ?: (MyApp.latestAxis!!.first?:0)
                        initialY = params?.y ?: (MyApp.latestAxis!!.second ?:0)
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        MyApp.latestAxis = Pair(params?.x, params?.y)
                        windowManager.updateViewLayout(binding?.root, params)
                        return false
                    }


                }
                return false
            }
        })
    }

    @Synchronized
    private fun setupRecyclerView(result: List<ItemModal>?, order: Order? ) {
        binding?.apply {
            // Update order title (#42)
            val shortId = order?.id?.takeLast(4)?.uppercase() ?: ""
            orderTitle.text = "Order #$shortId"
            
            // Update customer name (#42)
            val customerNameStr = order?.customers?.firstOrNull()?.let { customer ->
                listOfNotNull(customer.firstName, customer.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { null }
            } ?: "-"
            customerName.text = customerNameStr
            
            // Update order total (#42)
            val totalCents = order?.total ?: 0L
            orderTotal.text = "$${String.format("%.2f", totalCents / 100.0)}"
            
            // Update item count (#42)
            val itemCountNum = result?.sumOf { it.itemCount } ?: 0
            itemCount.text = "$itemCountNum items"
            
            // Setup order notes pills (#42)
            setupOrderNotesPills(order)
            
            // Hide empty state, show items
            emptyStateContainer.hideView()
            progressLayout.hideView()
            
            itemRecycler.layoutManager = LinearLayoutManager(this@FloatingWidgetService)
            val adapter = DrawerItemAdapter(lineItems, this@FloatingWidgetService)
            itemRecycler.adapter = adapter
            itemRecycler.showView()
        }
    }
    
    /**
     * Setup order-level notes pills (#42)
     * Parses order note and displays as pills with widget-specific color coding
     * Pills: SINGLE_SELECT, MULTI_SELECT, CALENDAR only
     * TEXT_BOX gets dedicated rows (wrapped, not truncated)
     */
    private fun setupOrderNotesPills(order: Order?) {
        val container = binding?.orderNotesPillsContainer ?: return
        val dynamicRowsContainer = binding?.dynamicWidgetRowsContainer
        container.removeAllViews()
        dynamicRowsContainer?.removeAllViews()
        
        val orderNote = order?.note
        if (orderNote.isNullOrBlank()) {
            container.visibility = View.GONE
            dynamicRowsContainer?.visibility = View.GONE
            return
        }
        
        // Use widget-based parsing for ORDER-level widgets
        val widgets = WidgetManager.getCachedWidgets()
        val orderLevelWidgets = widgets.filter { it.level == NoteLevel.ORDER }
        val density = resources.displayMetrics.density
        
        // Get all tags including TEXT_BOX for dedicated rows
        val allTags = OrderNoteParser.extractTagsFromNote(orderNote, orderLevelWidgets, NoteLevel.ORDER, includeTextBox = true)
        
        // Separate pills (single, multi, calendar) from dedicated rows (TEXT_BOX)
        val pillTags = allTags.filter { 
            it.widgetType == WidgetType.SINGLE_SELECT || 
            it.widgetType == WidgetType.MULTI_SELECT || 
            it.widgetType == WidgetType.CALENDAR 
        }
        val textBoxTags = allTags.filter { it.widgetType == WidgetType.TEXT_BOX }
        
        // Setup pill container
        if (pillTags.isEmpty()) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            pillTags.forEach { tag ->
                addPill(container, tag.value, tag.widgetType, density)
            }
        }
        
        // Setup dedicated TEXT_BOX rows (wrapped, not truncated)
        if (textBoxTags.isEmpty()) {
            dynamicRowsContainer?.visibility = View.GONE
        } else {
            dynamicRowsContainer?.visibility = View.VISIBLE
            textBoxTags.forEach { tag ->
                // Find the widget config to get the label
                val widget = orderLevelWidgets.find { 
                    it.type == WidgetType.TEXT_BOX && it.label.equals(tag.label, ignoreCase = true)
                }
                val label = widget?.label ?: tag.label
                addDynamicWidgetRow(dynamicRowsContainer!!, label, tag.value, tag.widgetType, density)
            }
        }
    }
    
    /**
     * Add a dedicated row for TEXT_BOX widgets (wrapped, not truncated)
     * Matches the style of Order Details card rows
     */
    private fun addDynamicWidgetRow(container: android.widget.LinearLayout, label: String, value: String, widgetType: WidgetType, density: Float) {
        val color = WidgetColorUtils.getColorForWidgetType(widgetType)
        val iconRes = WidgetColorUtils.getIconForWidgetType(widgetType)
        
        // Row container
        val rowLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        
        // Icon container (matches Order Details style)
        val iconFrame = android.widget.FrameLayout(this).apply {
            val size = dpToPx(32)
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size)
            setBackgroundResource(R.drawable.bg_detail_icon)
        }
        
        val icon = android.widget.ImageView(this).apply {
            val iconSize = dpToPx(16)
            layoutParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(iconRes)
            setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.text_muted))
        }
        iconFrame.addView(icon)
        rowLayout.addView(iconFrame)
        
        // Text container (label + value)
        val textLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
            }
        }
        
        // Label
        val labelView = TextView(this).apply {
            text = label.uppercase()
            setTextColor(ContextCompat.getColor(this@FloatingWidgetService, R.color.text_muted))
            textSize = 11f
        }
        textLayout.addView(labelView)
        
        // Value - wrapped, not truncated
        val valueView = TextView(this).apply {
            text = value
            setTextColor(ContextCompat.getColor(this@FloatingWidgetService, R.color.text_light))
            textSize = 14f
            maxLines = 3  // Allow wrapping up to 3 lines
        }
        textLayout.addView(valueView)
        
        rowLayout.addView(textLayout)
        
        // Add divider
        val divider = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                dpToPx(1)
            )
            setBackgroundColor(0x1AFFFFFF.toInt())
        }
        
        container.addView(rowLayout)
        container.addView(divider)
    }
    
    private fun addPill(container: FlexboxLayout, text: String, widgetType: WidgetType, density: Float) {
        val color = WidgetColorUtils.getColorForWidgetType(widgetType)
        
        val pill = TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(color)
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            
            // Unified pill background: 15% opacity + 25% border
            background = WidgetColorUtils.createPillBackground(color, 12f, density)
            
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
            layoutParams = lp
        }
        container.addView(pill)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getItemCount(list: List<ItemModal>): Int {
        var result = 0
        list.forEach {
            result += it.itemCount
        }
        return result
    }


    fun updateData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = exceptionHandlerWithReturn {
                MyApp.getInstance().getOrderConnector()
                    .getOrder(OrderDetailFragment.orderIdForReopen)
            }
            if (data == null) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding?.apply {
                        // Show empty state (#42)
                        emptyStateContainer.showView()
                        itemRecycler.hideView()
                        progressLayout.hideView()
                        OrderDetailFragment.orderIdForReopen = null
                        OrderDetailFragment.isReOpenBtnClicked = false
                    }
                }
            } else {
                // Always refresh the order data when data exists
                getTheOrderData()
            }
        }

    }


    private fun setupClickListener() {
        binding?.progressLayout?.setOnClickListener {
            Constants.notImplementedLog
        }
        if (OrderDetailFragment.isReOpenBtnClicked) {
            getTheOrderData()
        }

        binding?.container1?.setOnClickListener {
            // Set flag to prevent immediate close from transparentContainer click
            isDrawerOpening = true
            
            binding?.orderMateButton?.hideView()
            // when the cart is empty (#42 - use new emptyStateContainer)
            if (prefManager.getString((Constants.isOrderSaved)) == Constants.isTrue && OrderDetailFragment.orderIdForReopen == null) {
                binding?.itemRecycler?.hideView()
                binding?.progressLayout?.hideView()
                binding?.emptyStateContainer?.showView()
            }
            if (prefManager.getString(Constants.isOrderSaved) == Constants.isFalse) {
                getTheOrderData()
            }
            setupMinWidth(500)

            if (isPermanentMode) {
                // In permanent mode, expand to cover Clover register item list
                windowManager.updateViewLayout(
                    binding?.root,
                    setTheWindowParamsForPermanentOverlay()
                )
                // Show opaque background and drawer, no transparent dimming
                binding?.permanentModeBackground?.showView()
                // Use no-rounded-corners background for permanent mode
                binding?.container?.setBackgroundResource(R.drawable.bg_drawer_permanent)
                // Set container to fill the window in permanent mode
                binding?.container?.layoutParams = binding?.container?.layoutParams?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                binding?.container?.showView()
                binding?.transparentContainer?.visibility = View.GONE
            } else {
                // Normal mode: expand to full screen with dimming
                windowManager.updateViewLayout(
                    binding?.root,
                    setTheWindowParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )
                )
                // Show drawer and overlay with rounded corners
                binding?.permanentModeBackground?.hideView()
                binding?.container?.setBackgroundResource(R.drawable.bg_drawer_glass)
                binding?.container?.showView()
                binding?.transparentContainer?.showView()
            }
            
            // Reset flag after a short delay to allow layout to settle
            binding?.root?.postDelayed({
                isDrawerOpening = false
            }, 300)
        }
        binding?.cancelButton?.setOnClickListener {
            closeHandler()
        }

        binding?.transparentContainer?.setOnClickListener {
            // Ignore clicks if drawer is still opening (prevents accidental close)
            if (isDrawerOpening) {
                return@setOnClickListener
            }
            closeHandler()
        }
        
        // Edit Order Notes button (#42) - opens order-level notes popup
        binding?.btnEditOrderNotes?.setOnClickListener {
            // Check if order-level notes are enabled
            if (WidgetManager.getInstance(applicationContext).isOrderNotesEnabled()) {
                // In permanent mode, set flag to prevent drawer from closing
                if (isPermanentMode) {
                    isPopupOpen = true
                }
                
                val data = Intent(applicationContext, OverlayActivity::class.java)
                data.putExtra(Constants.overlayIntentExtraOrder, lastOrder?.id)
                data.putExtra(OverlayActivity.EXTRA_OVERLAY_MODE, OverlayActivity.OVERLAY_MODE_ORDER_NOTE)
                data.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                applicationContext?.startActivity(data)
            }
        }
    }


    private fun closeHandler() {
        // In permanent mode, don't close drawer when popup is open
        if (isPermanentMode && (isPopupOpen || OverlayActivity.isActive)) {
            return
        }
        
        setupMinWidth(0)
        binding?.orderMateButton?.showView()
        
        if (isPermanentMode) {
            // In permanent mode, collapse to button but position at left edge for easy re-access
            // (near where the Clover item list was)
            val displayMetrics = resources.displayMetrics
            windowManager.updateViewLayout(
                binding?.root,
                setTheWindowParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    50,  // Left side position
                    displayMetrics.heightPixels / 2
                )
            )
        } else {
            // Normal mode: restore to last known position
            windowManager.updateViewLayout(
                binding?.root,
                setTheWindowParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    MyApp.latestAxis?.first,
                    MyApp.latestAxis?.second
                )
            )
        }
        binding?.permanentModeBackground?.hideView()
        binding?.container?.hideView()
        binding?.transparentContainer?.hideView()
    }

    fun visibleRecycler() {
        binding?.itemRecycler?.showView()
    }

    private fun setupMinWidth(width: Int) {
        binding?.parentContainer?.minWidth = width
    }

    fun getTheOrderData() {
        // If already fetching, mark that a refresh is pending and return
        if (isFetchingData) {
            Log.d("DrawerState", "getTheOrderData() called but fetch in progress, marking pendingRefresh")
            pendingRefresh = true
            return
        }
        isFetchingData = true
        pendingRefresh = false  // Clear any pending flag since we're starting a fresh fetch
        
        Log.d("DrawerState", "getTheOrderData() called")
        Log.d("DrawerState", "Expected order (orderIdForReopen): ${OrderDetailFragment.orderIdForReopen}")
        
        binding?.progressLayout?.showView()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data =
                    MyApp.getInstance().getOrderConnector().getOrders(mutableListOf())
                lineItems.clear()
                if (data?.isEmpty() == true) {
                    Log.d("DrawerState", "No orders available from getOrders()")
                    isFetchingData = false
                    checkPendingRefresh()
                    return@launch
                }
                
                Log.d("DrawerState", "First order from getOrders(): ${data?.get(0)?.id}")
                
                val requiredData = if (OrderDetailFragment.orderIdForReopen != null) {
                    Log.d("DrawerState", "Fetching specific order: ${OrderDetailFragment.orderIdForReopen}")
                    MyApp.getInstance().getOrderConnector()
                        .getOrder(OrderDetailFragment.orderIdForReopen)
                } else {
                    Log.d("DrawerState", "orderIdForReopen is null, will use first order")
                    null
                }

                val orderToDisplay = requiredData ?: data?.get(0)
                Log.d("DrawerState", "Actual order being displayed: ${orderToDisplay?.id}")
                
                updateOrder(orderToDisplay)
                val result = orderToDisplay?.lineItems?.let {
                    countElementsByUniqueKeys(
                        binding?.root?.context,
                        it
                    )
                }
                result?.forEach {
                    lineItems.add(it)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    setupRecyclerView(result, orderToDisplay)
                    isFetchingData = false
                    checkPendingRefresh()
                }
            } catch (e: Exception) {
                Log.e("DrawerState", "Error in getTheOrderData: ${e.message}")
                isFetchingData = false
                checkPendingRefresh()
            }
        }
    }
    
    /**
     * Check if a refresh was requested while we were fetching.
     * If so, trigger another fetch to get the latest data.
     */
    private fun checkPendingRefresh() {
        if (pendingRefresh) {
            Log.d("DrawerState", "Pending refresh detected, fetching latest data")
            pendingRefresh = false
            getTheOrderData()
        }
    }

    @Synchronized
    private fun updateOrder(order: Order?) {
        if (order == null) {
            return
        }
        lastOrder = order
    }


    private fun setTheWindowParams(
        height: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        width: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        xAxis: Int? = 700,
        yAxis: Int? = 700,
    ): WindowManager.LayoutParams? {
        params = WindowManager.LayoutParams(
            width, height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params?.x = MyApp.latestAxis?.first ?: xAxis
        params?.y = MyApp.latestAxis?.second ?: yAxis
        return params
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(popupClosedReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
        if (binding != null) windowManager.removeView(binding?.root)
        isShowing = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        // Check if item-level notes are enabled
        if (!WidgetManager.getInstance(applicationContext).isItemNotesEnabled()) {
            return // Item notes disabled, don't show popup
        }
        
        // In permanent mode, set flag to prevent drawer from closing
        if (isPermanentMode) {
            isPopupOpen = true
        }
        
        val data = Intent(applicationContext, OverlayActivity::class.java)
        data.putExtra(Constants.overlayIntentExtraOrder, lastOrder?.id)
        data.putExtra(Constants.overlayIntentExtraLinePosition, orderPosition.toString())
        data.putExtra(Constants.overlayIntentExtraLineItemId, lineItemId.toString())
        data.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext?.startActivity(data)
    }
}
