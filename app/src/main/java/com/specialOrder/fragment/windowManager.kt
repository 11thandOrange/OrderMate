package com.specialOrder.fragment


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
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.order.Order
import com.specialOrder.R
import com.specialOrder.activities.OverlayActivity
import com.specialOrder.adapters.ItemAdapter
import com.specialOrder.communicators.IOrderItemClickListener
import com.specialOrder.databinding.OrdermateBasketLayoutBinding
import com.specialOrder.fragment.orderDetail.OrderDetailFragment
import com.specialOrder.modals.ItemModal
import com.specialOrder.utils.Constants
import com.specialOrder.utils.MyApp
import com.specialOrder.utils.PreferenceManager
import com.specialOrder.utils.countElementsByUniqueKeys
import com.specialOrder.utils.exceptionHandlerWithReturn
import com.specialOrder.utils.getCustomerName
import com.specialOrder.utils.hideView
import com.specialOrder.utils.showView
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
    private var latestAxis: Pair<Int?, Int?>? = Pair(700, 700)
    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private val binding: OrdermateBasketLayoutBinding? by lazy {
        OrdermateBasketLayoutBinding.inflate(LayoutInflater.from(this))
    }

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
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        latestAxis = Pair(params?.x, params?.y)
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
            if (result?.isEmpty() == false) {
                val data = result[0]
                orderId.text = binding?.root?.context?.getString(
                    R.string.order_id_value,
                    data.order?.id.toString()
                )
                getCustomerName(binding?.root?.context, order, binding?.customerName)
                itemCount.text =
                    binding?.root?.context?.getString(R.string.order, getItemCount(result))
            }
            itemRecycler.layoutManager = LinearLayoutManager(this@FloatingWidgetService)
            val adapter = ItemAdapter(lineItems, this@FloatingWidgetService)
            itemRecycler.adapter = adapter
            itemRecycler.showView()
            itemCount.showView()
            orderId.showView()
        }
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
                        cartEmpty.showView()
                        cartEmptyText.showView()
                        itemRecycler.hideView()
                        progressLayout.hideView()
                        itemCount.hideView()
                        orderId.hideView()
                        customerName.hideView()
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
            binding?.orderMateButton?.hideView()
            // when the cart is empty
            if (prefManager.getString((Constants.isOrderSaved)) == Constants.isTrue && OrderDetailFragment.orderIdForReopen == null) {
                binding?.itemRecycler?.hideView()
                binding?.itemCount?.hideView()
                binding?.orderId?.hideView()
                binding?.customerName?.hideView()
                binding?.progressLayout?.hideView()
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

            binding?.container?.showView()
        }
        binding?.cancelButton?.setOnClickListener {
            closeHandler()
        }

        binding?.transparentContainer?.setOnClickListener {
            closeHandler()
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
                latestAxis?.first,
                latestAxis?.second
            )
        )
        binding?.container?.hideView()
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
        params?.x = xAxis
        params?.y = yAxis
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
