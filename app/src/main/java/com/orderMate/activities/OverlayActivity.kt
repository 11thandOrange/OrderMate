package com.orderMate.activities

import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.clover.sdk.v3.order.Order
import com.orderMate.communicators.ILineItemUpdateListener
import com.orderMate.databinding.ActivityOverlayBinding
import com.orderMate.fragment.orderDetail.ItemNoteDialogFragment
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.exceptionHandler
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

    private var widgetManager: WidgetManager? = null
    private var orderData: Order? = null
    private var lineItemId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        
        // Initialize WidgetManager
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId != null) {
            widgetManager = WidgetManager(merchantId)
        }
        
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
            Log.e("codeChecking", "parseIntentData: Code checking is above running ")
            updateOrderData(
                intent.getParcelableExtra(Constants.LINE_ITEM_ADDED_ORDER_DETAILS),
                intent.getStringExtra(Constants.LINE_ITEM_ADDED_ID),
                null
            )
        } else {
            Log.e("codeChecking", "parseIntentData: Code checking is above running lower $lineItem")
            CoroutineScope(Dispatchers.IO).launch {
                val result = MyApp.getInstance().getOrderConnector().getOrder(data)
                updateOrderData(result, lineItem, position)
            }
        }
    }


    fun updateOrderData(result: Order?, lineItem: String?, position: String?) {
        orderData = result
        lineItemId = lineItem
        
        // Load widgets then show dialog
        widgetManager?.load { success ->
            runOnUiThread {
                if (success) {
                    showItemNoteDialog()
                } else {
                    Log.e("OverlayActivity", "Failed to load widgets")
                    finish()
                }
            }
        } ?: run {
            // No widget manager, close activity
            Log.e("OverlayActivity", "No widget manager available")
            finish()
        }
    }
    
    private fun showItemNoteDialog() {
        val enabledWidgets = widgetManager?.enabledWidgets ?: emptyList()
        
        if (enabledWidgets.isEmpty()) {
            Log.d("OverlayActivity", "No enabled widgets, closing")
            finish()
            return
        }
        
        // Get existing note for this line item
        val existingNote = orderData?.lineItems?.find { 
            it?.item?.id == lineItemId 
        }?.note
        
        ItemNoteDialogFragment.newInstance(
            widgets = enabledWidgets,
            lineItemId = lineItemId,
            existingNote = existingNote
        ).apply {
            setListener(object : ItemNoteDialogFragment.ItemNoteListener {
                override fun onNoteSaved(itemId: String?, note: String) {
                    // Update line item note in Clover
                    CoroutineScope(Dispatchers.IO).launch {
                        exceptionHandler {
                            val orderId = orderData?.id ?: return@exceptionHandler
                            val allLineItems = orderData?.lineItems ?: return@exceptionHandler
                            
                            // Update note for matching line items
                            allLineItems.forEach { lineItem ->
                                if (lineItem?.item?.id == itemId) {
                                    lineItem.note = note
                                }
                            }
                            
                            // Save to Clover
                            MyApp.getInstance().getOrderConnector().updateLineItems(orderId, allLineItems)
                        }
                    }
                    finish()
                }
                
                override fun onNoteCancelled() {
                    finish()
                }
            })
        }.show(supportFragmentManager, ItemNoteDialogFragment.TAG)
    }


    override fun dismissDialog() {
        finish()
    }

    override fun updateLineItem(id: String?, list: String?, position: Int) {
        Constants.notImplementedLog
    }
}
