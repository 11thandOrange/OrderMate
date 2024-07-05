package com.specialOrder.activities

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.clover.sdk.v3.order.Order
import com.specialOrder.communicators.ILineItemUpdateListener
import com.specialOrder.databinding.ActivityOverlayBinding
import com.specialOrder.fragment.orderDetail.CustomModalDialog
import com.specialOrder.utils.Constants
import com.specialOrder.utils.MyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *  This is the activity class having dialog as theme which is used to show the overlay
 *  on the clover register app when any line item is added to update the notes for the item.
 */
class OverlayActivity : AppCompatActivity(), ILineItemUpdateListener {

    private val binding: ActivityOverlayBinding by lazy {
        ActivityOverlayBinding.inflate(layoutInflater)
    }

    private var customModalDialog: CustomModalDialog? = null

    private var orderData: Order? = null
    private var lineItemId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        parseIntentData()
    }

    /**
     *  function to parse the intent to get the data from the line item added broadcast.
     */
    private fun parseIntentData() {
        if (intent == null) return

        val data = intent.getStringExtra(Constants.overlayIntentExtraOrder)
        val lineItem = intent.getStringExtra(Constants.overlayIntentExtraLineItemId)
        val position = intent.getStringExtra(Constants.overlayIntentExtraLinePosition)
        if (data == null) {
            updateOrderData(
                intent.getParcelableExtra(Constants.LINE_ITEM_ADDED_ORDER_DETAILS),
                intent.getStringExtra(Constants.LINE_ITEM_ADDED_ID),
                null
            )
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val result = MyApp.getInstance().getOrderConnector().getOrder(data)
                updateOrderData(result, lineItem, position)
            }
        }
    }


    @Synchronized
    fun updateOrderData(result: Order?, lineItem: String?, position: String?) {
        orderData = result
        lineItemId = lineItem
        customModalDialog = CustomModalDialog(lineItemId, orderData , orderData?.id, position?.toInt(),
            this@OverlayActivity)
        customModalDialog?.show(
            supportFragmentManager,
            Constants.defaultString,
        )
    }


    override fun dismissDialog() {
        finish()
    }

    override fun updateLineItem(id: String?, list: String?, position: Int) {
        Constants.notImplementedLog
    }
}
