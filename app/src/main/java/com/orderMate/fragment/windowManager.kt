package com.orderMate.fragment


import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.order.Order
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.activities.OverlayActivity
import com.orderMate.adapters.ItemAdapter
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.OrdermateBasketLayoutBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.modals.ItemModal
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.PreferenceManager
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
    }

    private lateinit var windowManager: WindowManager
    private var params: WindowManager.LayoutParams? = null
    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private val binding: OrdermateBasketLayoutBinding? by lazy {
        OrdermateBasketLayoutBinding.inflate(LayoutInflater.from(this))
    }
    
    // Flag to prevent immediate close when opening drawer
    private var isDrawerOpening = false

    private val prefManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isShowing = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(binding?.root, setTheWindowParams())

        setupClickListener()
        setUpTouchListener()


        // this is to handle the case when the dialog is open and user has added some items
        //some time it backfires by not providing the callback at correct time

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
            val adapter = ItemAdapter(lineItems, this@FloatingWidgetService)
            itemRecycler.adapter = adapter
            itemRecycler.showView()
        }
    }
    
    /**
     * Setup order-level notes pills (#42)
     * Parses order note and displays as pills with widget-specific color coding
     */
    private fun setupOrderNotesPills(order: Order?) {
        val container = binding?.orderNotesPillsContainer ?: return
        container.removeAllViews()
        
        val orderNote = order?.note
        if (orderNote.isNullOrBlank()) {
            container.visibility = View.GONE
            return
        }
        
        // Parse order note with labels preserved for color coding
        val parsedNotes = parseOrderNoteWithLabels(orderNote)
        
        if (parsedNotes.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        
        parsedNotes.forEach { (label, value) ->
            // Get widget-specific color based on label
            val color = getColorForLabel(label)
            val bgColor = (color and 0x00FFFFFF) or 0x26000000  // 15% opacity
            
            val pill = TextView(this).apply {
                text = value
                textSize = 11f
                setTextColor(color)
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                
                // Create background with widget-specific color
                val bg = android.graphics.drawable.GradientDrawable()
                bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                bg.cornerRadius = 12f * resources.displayMetrics.density
                bg.setColor(bgColor)
                background = bg
                
                val lp = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
                layoutParams = lp
            }
            container.addView(pill)
        }
    }
    
    /**
     * Parse order note preserving labels for color coding
     */
    private fun parseOrderNoteWithLabels(noteString: String): List<Pair<String, String>> {
        val notes = mutableListOf<Pair<String, String>>()
        val delimiter = if (noteString.contains("•")) "•" else "|"
        val parts = noteString.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
        
        parts.forEach { part ->
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val rawValue = part.substring(colonIndex + 1).trim()
                
                val isMultiSelect = label.contains("category") || label.contains("tag")
                if (isMultiSelect) {
                    rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { value ->
                        notes.add(label to value)
                    }
                } else if (rawValue.isNotBlank()) {
                    notes.add(label to rawValue)
                }
            } else if (part.isNotBlank()) {
                notes.add("text" to part)
            }
        }
        return notes
    }
    
    /**
     * Get color for widget label - uses WidgetColorUtils for consistency
     */
    private fun getColorForLabel(label: String): Int {
        return com.orderMate.utils.WidgetColorUtils.getColorForLabel(label)
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
                if (OrderDetailFragment.isReOpenBtnClicked) {
                    getTheOrderData()
                }
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

            windowManager.updateViewLayout(
                binding?.root,
                setTheWindowParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            )

            // Show drawer and overlay
            binding?.container?.showView()
            binding?.transparentContainer?.showView()
            
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
            val data = Intent(applicationContext, OverlayActivity::class.java)
            data.putExtra(Constants.overlayIntentExtraOrder, lastOrder?.id)
            data.putExtra(OverlayActivity.EXTRA_OVERLAY_MODE, OverlayActivity.OVERLAY_MODE_ORDER_NOTE)
            data.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            applicationContext?.startActivity(data)
        }
    }


    private fun closeHandler() {
        setupMinWidth(0)
        binding?.orderMateButton?.showView()
        windowManager.updateViewLayout(
            binding?.root,
            setTheWindowParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                MyApp.latestAxis?.first,
                MyApp.latestAxis?.second
            )
        )
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
        binding?.progressLayout?.showView()
        CoroutineScope(Dispatchers.IO).launch {

            val data =
                MyApp.getInstance().getOrderConnector().getOrders(mutableListOf())
            lineItems.clear()
            if (data?.isEmpty() == true) {
                return@launch
            }
            val requiredData = if (OrderDetailFragment.orderIdForReopen != null) {
                MyApp.getInstance().getOrderConnector()
                    .getOrder(OrderDetailFragment.orderIdForReopen)
            } else null


            updateOrder(requiredData ?: data?.get(0))
            val result = (requiredData ?: data?.get(0))?.lineItems?.let {
                countElementsByUniqueKeys(
                    binding?.root?.context,
                    it
                )
            }
            result?.forEach {
                lineItems.add(it)
            }
            CoroutineScope(Dispatchers.Main).launch {
                setupRecyclerView(result, requiredData ?: data?.get(0)  )
            }
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
        if (binding != null) windowManager.removeView(binding?.root)
        isShowing = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        val data = Intent(applicationContext, OverlayActivity::class.java)
        data.putExtra(Constants.overlayIntentExtraOrder, lastOrder?.id)
        data.putExtra(Constants.overlayIntentExtraLinePosition, orderPosition.toString())
        data.putExtra(Constants.overlayIntentExtraLineItemId, lineItemId.toString())
        data.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext?.startActivity(data)
    }
}
