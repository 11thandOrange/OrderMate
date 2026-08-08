package com.orderMate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.orderMate.fragment.FloatingWidgetService
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.order.Order
import com.orderMate.communicators.ILineItemUpdateListener
import com.orderMate.databinding.ActivityOverlayBinding
import com.orderMate.fragment.orderDetail.ItemNoteDialogFragment
import com.orderMate.fragment.orderDetail.OrderNoteDialogFragment
import com.orderMate.repository.CloverRepository
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.isOrderOpenForEditing
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
        
        // Static flag to track if popup is active (for FloatingWidgetService to check)
        @Volatile
        var isActive: Boolean = false
            private set
    }

    private val binding: ActivityOverlayBinding by lazy {
        ActivityOverlayBinding.inflate(layoutInflater)
    }

    private var orderData: Order? = null
    private var lineItemId: String? = null
    private var overlayMode: String = OVERLAY_MODE_ITEM_NOTE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true
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
                var result = MyApp.getInstance().getOrderConnector().getOrder(data)
                
                // #78: Enrich order with full customer data (phone/email)
                result = CloverRepository.getInstance(this@OverlayActivity).enrichOrderWithFullCustomer(result)
                
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
        // Get line item data
        val lineItem = orderData?.lineItems?.find { it?.item?.id == lineItemId }
        val existingNote = lineItem?.note
        val itemName = lineItem?.getName()
        // Quantity is "how many separate line items share this catalog item" (see
        // CommonFunctions.countElementsByUniqueKeys) - unitQty is a different, weight-based
        // Clover concept and isn't what's shown/edited as quantity anywhere else in the app (#139).
        val itemQuantity = orderData?.lineItems?.count { it?.item?.id == lineItemId } ?: 1
        
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
            itemQuantity = itemQuantity,
            isOrderEditable = isOrderOpenForEditing(orderData)
        ).apply {
            setListener(object : ItemNoteDialogFragment.ItemNoteListener {
                override fun onNoteSaved(itemId: String?, note: String, quantity: Int) {
                    // Update line item note and quantity in Clover
                    CoroutineScope(Dispatchers.IO).launch {
                        exceptionHandler {
                            val orderId = orderData?.id ?: return@exceptionHandler
                            val allLineItems = orderData?.lineItems ?: return@exceptionHandler
                            val orderConnector = MyApp.getInstance().getOrderConnector()

                            // Update note first, only on line items we know currently exist -
                            // avoids touching anything that's about to be deleted below (#139).
                            val existingGroupItems = allLineItems.filter { it?.item?.id == itemId }
                            if (existingGroupItems.isNotEmpty()) {
                                existingGroupItems.forEach { it?.note = note }
                                orderConnector.updateLineItems(orderId, existingGroupItems)
                            }

                            // Quantity here means "how many separate line items represent this
                            // product" - Clover has no per-line-item count field (unitQty is a
                            // different, weight-based concept), so changing it means actually
                            // adding or deleting line items (#139).
                            val delta = quantity - itemQuantity
                            if (delta > 0) {
                                // Duplicate an actual existing (already note-updated, already
                                // grouped) line item via createLineItemsFrom rather than
                                // reconstructing one from catalog defaults - see
                                // OrderDetailFragment.onOrderItemClick for why (#139).
                                val sourceId = existingGroupItems.firstOrNull()?.id
                                if (!sourceId.isNullOrEmpty()) {
                                    repeat(delta) {
                                        orderConnector.createLineItemsFrom(orderId, orderId, listOf(sourceId))
                                    }
                                }
                            } else if (delta < 0) {
                                val idsToRemove = existingGroupItems.mapNotNull { it?.id }.take(-delta)
                                if (idsToRemove.isNotEmpty()) {
                                    orderConnector.deleteLineItems(orderId, idsToRemove)
                                }
                            }
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
        val existingNote = orderData?.note
        
        OrderNoteDialogFragment.newInstance(
            orderId = orderData?.id,
            existingNote = existingNote
        ).apply {
            setCurrentCustomer(orderData?.customers?.firstOrNull())
            setListener(object : OrderNoteDialogFragment.OrderNoteListener {
                override fun onOrderNoteSaved(orderId: String?, note: String, customer: Customer?) {
                    if (orderId != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val repository = CloverRepository.getInstance(this@OverlayActivity)
                            if (customer != null) {
                                repository.assignCustomerToOrder(orderId, customer)
                            }
                            repository.saveOrderNote(orderId, note)
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
    
    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        // Notify FloatingWidgetService that popup is closed
        sendBroadcast(Intent(FloatingWidgetService.ACTION_POPUP_CLOSED))
    }
}
