package com.orderMate.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.clover.sdk.v1.Intents.ACTION_ACTIVE_REGISTER_ORDER
import com.clover.sdk.v1.Intents.ACTION_LINE_ITEM_ADDED
import com.clover.sdk.v1.Intents.ACTION_LINE_ITEM_DELETED
import com.clover.sdk.v1.Intents.ACTION_ORDER_CREATED
import com.clover.sdk.v1.Intents.ACTION_ORDER_SAVED
import com.clover.sdk.v1.Intents.ACTION_PAYMENT_PROCESSED
import com.clover.sdk.v1.Intents.ACTION_V1_ORDER_BUILD_START
import com.clover.sdk.v1.Intents.ACTION_V1_ORDER_BUILD_STOP
import com.clover.sdk.v1.Intents.EXTRA_CLOVER_ITEM_ID
import com.clover.sdk.v1.Intents.EXTRA_CLOVER_ORDER_ID
import com.clover.sdk.v3.order.Order
import com.orderMate.activities.OverlayActivity
import com.orderMate.fragment.FloatingWidgetService
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *  This Class will listen to the events happened in the register app :
 *  Operation : When the line item is added by the user this class will get the callback for it.
 *
 *  We will get -> order id , line item id , item id.
 *
 *  This class will work in multiple  steps and all the case should work in this described manner-->
 *  Step1 : Check for the intent if the intent is null then don't do anything
 *  Step2 : Check for the intent if it is the relevant intent then perform the action else not
 *  Step3 : Basket Case : if the basket option is enabled then show the OrderMate icon on the register app.
 *  Step4 : if the show modal option is available then show the modal else not
 *  Step5 : if all the fields are disabled of the modal then don't do anything.
 */


class LineItemAddedReceiver : BroadcastReceiver() {


    private var prefManager: PreferenceManager? = null
    private var isItemAlreadyThere = false

    // please add the intent here if you want to listen that and perform the operation on that.
    private val requiredIntentArray = arrayOf(
        ACTION_LINE_ITEM_ADDED,
        ACTION_LINE_ITEM_DELETED,
        ACTION_ORDER_SAVED,
        ACTION_ACTIVE_REGISTER_ORDER,
        ACTION_V1_ORDER_BUILD_START,
        ACTION_ORDER_CREATED,
        ACTION_V1_ORDER_BUILD_STOP,
        ACTION_PAYMENT_PROCESSED
    )


    override fun onReceive(p0: Context?, p1: Intent?) {
        // if the intent received is null
        if (p1 == null) {
            return
        }

        // Note: OnOrderChangedListener is registered once in FloatingWidgetService.onCreate()
        // to avoid duplicate listener registration on every broadcast

        // is this the required intent if yes then perform the next operation else not
        if (!isRequiredIntent(p1)) {
            return
        }

        prefManager = p0?.let { PreferenceManager.getInstance(it) }
        if (p1.action.equals(ACTION_ORDER_SAVED, true) || p1.action.equals(
                ACTION_PAYMENT_PROCESSED,
                true
            )
        ) {
            Log.d("DrawerState", "ORDER_SAVED or PAYMENT_PROCESSED received")
            prefManager?.saveString(Constants.isOrderSaved, Constants.isTrue)
            OrderDetailFragment.orderIdForReopen = null
            Log.d("DrawerState", "Cleared orderIdForReopen, drawer will use new active order")
            // Refresh the drawer to show the new order that starts after save/pay
            if (FloatingWidgetService.isShowing) {
                Log.d("DrawerState", "Refreshing drawer after save/pay")
                FloatingWidgetService.instance?.getTheOrderData("ORDER_SAVED_or_PAYMENT_PROCESSED")
            }
            return
        }
        // Task 14: Check V2 settings for OrderMate in Clover Register
        // Use the new SettingsManager which stores the setting under "use_ordermate_register" key
        val settingsManager = p0?.let { SettingsManager(it) }
        val isOrderMateRegisterEnabled = settingsManager?.getUseOrderMateRegister() ?: 
            prefManager?.getBoolean(Constants.isMenuBasketOptionEnabled) ?: false
        val isOrderMateRegisterInstead = settingsManager?.getUseOrderMateRegisterInstead() ?: false
        
        // Either mode triggers the service (floating button OR permanent overlay)
        if (isOrderMateRegisterEnabled || isOrderMateRegisterInstead) {
            val intent = Intent(p0, FloatingWidgetService::class.java)
            // Pass mode to service so it knows whether to show permanent overlay
            intent.putExtra(FloatingWidgetService.EXTRA_PERMANENT_MODE, isOrderMateRegisterInstead)
            
            when {
                (p1.action.equals(ACTION_V1_ORDER_BUILD_START)) -> {
                    if(FloatingWidgetService.isShowing){
                        FloatingWidgetService.instance?.updateData()
                        return
                    }
                    p0?.startService(intent)
                }

                (p1.action.equals(ACTION_V1_ORDER_BUILD_STOP)) -> {
                    p0?.stopService(intent)
                }

                (p1.action.equals(ACTION_ORDER_CREATED)) -> {
                    OrderDetailFragment.orderIdForReopen = null
                    prefManager?.saveString(Constants.isOrderSaved, Constants.isTrue)
                }
                
                (p1.action.equals(ACTION_ACTIVE_REGISTER_ORDER)) -> {
                    // When the active order changes in Register, update drawer to show that order
                    val previousOrderId = OrderDetailFragment.orderIdForReopen
                    Log.d("DrawerState", "ACTION_ACTIVE_REGISTER_ORDER received")
                    Log.d("DrawerState", "Previous orderIdForReopen: $previousOrderId")
                    
                    // Get the order ID - Clover uses "clover.intent.extra.ORDER_ID" key
                    // The value may be stored as Object, so use extras?.get() and convert to String
                    val activeOrderId = p1.extras?.get("clover.intent.extra.ORDER_ID")?.toString()
                        ?: p1.getStringExtra(EXTRA_CLOVER_ORDER_ID)
                        ?: p1.data?.lastPathSegment
                    
                    Log.d("DrawerState", "New active order from Register: $activeOrderId")
                    if (activeOrderId != null && activeOrderId != "null") {
                        OrderDetailFragment.orderIdForReopen = activeOrderId
                        prefManager?.saveString(Constants.isOrderSaved, Constants.isFalse)
                        Log.d("DrawerState", "Updated orderIdForReopen to: $activeOrderId")
                        // Refresh the drawer with the new active order
                        if (FloatingWidgetService.isShowing) {
                            Log.d("DrawerState", "FloatingWidgetService is showing, refreshing data")
                            FloatingWidgetService.instance?.getTheOrderData("ACTIVE_REGISTER_ORDER")
                        } else {
                            Log.d("DrawerState", "FloatingWidgetService is NOT showing")
                        }
                    } else {
                        Log.d("DrawerState", "activeOrderId is null, no update performed")
                    }
                }

                else -> {
                    Constants.notImplementedLog
                }
            }
        }



        when (p1.action) {
            ACTION_LINE_ITEM_ADDED -> {
                val orderId = p1.getStringExtra(EXTRA_CLOVER_ORDER_ID)
                val itemId = p1.getStringExtra(EXTRA_CLOVER_ITEM_ID)
                Log.d("DrawerState", "ACTION_LINE_ITEM_ADDED received - orderId: $orderId, itemId: $itemId")
                
                prefManager?.saveString(Constants.isOrderSaved, Constants.isFalse)
                FloatingWidgetService.instance?.visibleRecycler()
                
                // Refresh the drawer to reflect the added item (#80 fix)
                if (FloatingWidgetService.isShowing) {
                    Log.d("DrawerState", "Refreshing drawer after item added")
                    FloatingWidgetService.instance?.getTheOrderData("LINE_ITEM_ADDED")
                } else {
                    Log.d("DrawerState", "FloatingWidgetService not showing, skipping refresh")
                }
                
                performAddOperation(p1, p0)
            }

            ACTION_LINE_ITEM_DELETED -> {
                Log.d("DrawerState", "ACTION_LINE_ITEM_DELETED received")
                prefManager?.saveString(Constants.isOrderSaved, Constants.isFalse)
                // Refresh the drawer to reflect the deleted item
                if (FloatingWidgetService.isShowing) {
                    Log.d("DrawerState", "Refreshing drawer after item deleted")
                    FloatingWidgetService.instance?.getTheOrderData("LINE_ITEM_DELETED")
                }
            }
        }
    }

    private fun isRequiredIntent(intent: Intent): Boolean {
        return requiredIntentArray.contains(intent.action)
    }


    private fun performAddOperation(p1: Intent, p0: Context?) {
        val orderId = p1.getStringExtra(EXTRA_CLOVER_ORDER_ID)
        val itemId = p1.getStringExtra(EXTRA_CLOVER_ITEM_ID)
        Log.d("DrawerState", "performAddOperation() called - orderId: $orderId, itemId: $itemId")
        
        addItemToPref(p1)
        getOrderData(orderId) { orderDetails ->

            if(orderDetails == null){
                Log.d("DrawerState", "performAddOperation: orderDetails is null, returning")
                return@getOrderData
            }

            if (isItemAlreadyThere) {
                Log.d("DrawerState", "performAddOperation: item already exists, skipping popup")
                return@getOrderData
            }

            // if the admin has disabled this then the modal will be disabled.
            if (prefManager?.getBoolean(Constants.isMenuOptionEnabled) == false) {
                Log.d("DrawerState", "performAddOperation: menu option disabled, skipping popup")
                return@getOrderData
            }

            // if all the fields are disabled then we will not show the dialog
            if (prefManager?.getBoolean(Constants.isAllFieldDisabled) == true) {
                Log.d("DrawerState", "performAddOperation: all fields disabled, skipping popup")
                return@getOrderData
            }
            
            Log.d("DrawerState", "performAddOperation: launching OverlayActivity for item: $itemId")
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(p0, OverlayActivity::class.java)
                intent.putExtra(Constants.LINE_ITEM_ADDED_ORDER_DETAILS, orderDetails)
                intent.putExtra(
                    Constants.LINE_ITEM_ADDED_ID,
                    itemId
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                p0?.startActivity(intent)
            }
        }
    }

    /**
     *  This function is used to make sure the dialog for the adding notes will show only one time for
     *  unique line item.
     *
     *  Logic -> When user add the line item then , broadcast receiver gives us
     *   -> Order id, item id , line item id
     *
     * There is a bit in shared preference which check if the order id is already present then , we
     * check if the item id is already present then we simply return
     *
     * if item id is not present then we add it and show the "ADD Notes" dialog.
     *
     * if there is no order id present , then we add both order id , item id and show the dialog.
     *
     * Special Case : ->
     * Let say if user save the one order and go to the second and start adding the item , we clear
     * the preference data for the key and restart the above process.
     *
     * Different Order Logic -> We check if the preference is not empty and the current order id is not
     * present then we refresh the preferences.
     */
    private fun addItemToPref(p1: Intent) {
        val orderId = p1.getStringExtra(EXTRA_CLOVER_ORDER_ID)
        val itemId = p1.getStringExtra(EXTRA_CLOVER_ITEM_ID)

        if (orderId == null || itemId == null) {
            isItemAlreadyThere = true
            return
        }



        if (prefManager?.getString(Constants.LINE_ITEM_ADDED_ORDER_DETAILS).toString()
                .contains(orderId) &&
            prefManager?.getString(Constants.LINE_ITEM_ADDED_ORDER_DETAILS).toString()
                .contains(itemId)
        ) {
            isItemAlreadyThere = true
            return
        }

        if (prefManager?.getString(Constants.LINE_ITEM_ADDED_ORDER_DETAILS).toString()
                .contains(orderId) &&
            !prefManager?.getString(Constants.LINE_ITEM_ADDED_ORDER_DETAILS).toString()
                .contains(itemId)
        ) {
            prefManager?.saveString(
                Constants.LINE_ITEM_ADDED_ORDER_DETAILS,
                StringBuilder(
                    prefManager?.getString(Constants.LINE_ITEM_ADDED_ORDER_DETAILS).toString()
                ).append(itemId).toString()
            )
            isItemAlreadyThere = false
            return
        }

        prefManager?.saveString(
            Constants.LINE_ITEM_ADDED_ORDER_DETAILS,
            StringBuilder(orderId).append(itemId).toString()
        )

        isItemAlreadyThere = false
    }

    /**
     *  Function to receive the order data for the respective line item id.
     */
    private fun getOrderData(orderId: String?, function: (Order?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try{
                function( try{MyApp.getInstance().getOrderConnector().getOrder(orderId)}catch (e :Exception){e.printStackTrace(); null} )
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
    }


}