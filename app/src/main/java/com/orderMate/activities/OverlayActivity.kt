package com.orderMate.activities

import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.clover.sdk.v3.order.Order
import com.orderMate.communicators.ILineItemUpdateListener
import com.orderMate.databinding.ActivityOverlayBinding
import com.orderMate.fragment.orderDetail.ItemNoteDialogFragment
import com.orderMate.fragment.orderDetail.OrderNoteDialogFragment
import com.orderMate.repository.CloverRepository
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
 *  
 *  (#93) Also supports order-level notes via OVERLAY_MODE_ORDER_NOTE mode.
 */
class OverlayActivity : AppCompatActivity(), ILineItemUpdateListener {

    companion object {
        const val OVERLAY_MODE_ITEM_NOTE = "item_note"
        const val OVERLAY_MODE_ORDER_NOTE = "order_note"
        const val EXTRA_OVERLAY_MODE = "overlay_mode"
    }

    private val binding: ActivityOverlayBinding by lazy {
        ActivityOverlayBinding.inflate(layoutInflater)
    }

    private var orderData: Order? = null
    private var lineItemId: String? = null
    private var overlayMode: String = OVERLAY_MODE_ITEM_NOTE


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
        
        // Check overlay mode (#93)
        overlayMode = intent.getStringExtra(EXTRA_OVERLAY_MODE) ?: OVERLAY_MODE_ITEM_NOTE
        
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
        runOnUiThread { 
            when (overlayMode) {
                OVERLAY_MODE_ORDER_NOTE -> showOrderNoteDialog()
                else -> showItemNoteDialog()
            }
        }
    }
    
    private fun showItemNoteDialog() {
        // Check if item-level notes are enabled in settings
        if (!WidgetManager.getInstance(this).isItemNotesEnabled()) {
            finish() // Item notes disabled, close overlay
            return
        }
        
        // Get line item data
        val lineItem = orderData?.lineItems?.find { it?.item?.id == lineItemId }
        val existingNote = lineItem?.note
        val itemName = lineItem?.getName()
        val itemQuantity = lineItem?.unitQty?.toInt() ?: 1
        
        // Build modifiers string from modifications
        val modifiersString = lineItem?.modifications?.mapNotNull { it?.name }
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
        
        // Dialog reads widgets from WidgetManager directly (like production)
        ItemNoteDialogFragment.newInstance(
            lineItemId = lineItemId,
            existingNote = existingNote,
            itemName = itemName,
            itemModifiers = modifiersString,
            itemQuantity = itemQuantity
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
    
    /**
     * Show order-level note dialog (#93)
     */
    private fun showOrderNoteDialog() {
        // Check if order-level notes are enabled in settings
        if (!WidgetManager.getInstance(this).isOrderNotesEnabled()) {
            finish() // Order notes disabled, close overlay
            return
        }
        
        val existingNote = orderData?.note
        
        OrderNoteDialogFragment.newInstance(
            orderId = orderData?.id,
            existingNote = existingNote
        ).apply {
            setListener(object : OrderNoteDialogFragment.OrderNoteListener {
                override fun onOrderNoteSaved(orderId: String?, note: String) {
                    if (orderId != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            CloverRepository.getInstance(this@OverlayActivity)
                                .saveOrderNote(orderId, note)
                        }
                    }
                    finish()
                }
                
                override fun onOrderNoteCancelled() {
                    finish()
                }
            })
        }.show(supportFragmentManager, OrderNoteDialogFragment.TAG)
    }


    override fun dismissDialog() {
        finish()
    }

    override fun updateLineItem(id: String?, list: String?, position: Int) {
        Constants.notImplementedLog
    }
}
