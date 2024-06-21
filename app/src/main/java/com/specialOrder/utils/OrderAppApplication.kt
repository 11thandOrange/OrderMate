package com.specialOrder.utils

import android.accounts.Account
import android.app.Application
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.merchant.MerchantConnector
import com.clover.sdk.v3.inventory.InventoryConnector
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.order.OrderCalc
import com.clover.sdk.v3.order.OrderV31Connector
import com.google.firebase.FirebaseApp
import com.specialOrder.modals.FilterData


class MyApp : Application() {
    private var cloverAccount: Account? = null
    private var orderConnector: OrderV31Connector? = null
    private var inventoryConnector: InventoryConnector? = null
    private var merchantConnector: MerchantConnector? = null

    companion object {

        /*
          * We will basically apply the filters on the basis of indexing
          *  index  1 --> order status (Paid , Open etc)
          *  index 2 --> order employee name (name of employee who made the order)
          *  index 3 --> order tender type (cash , cheque)
          *  index 4 --> order made mode (online , pos)
          * */
        val filterArray: MutableList<FilterData> = mutableListOf()

        private var instance: MyApp? = null


        fun getInstance(): MyApp {
            return instance ?: synchronized(this) {
                MyApp().also {
                    instance = it
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(applicationContext)
        storeIntoPreference()
    }


    fun getOrderConnector(): OrderV31Connector {
        return orderConnector ?: synchronized(this) {
            OrderV31Connector(applicationContext, getCloverAccount(), null).also {
                orderConnector = it
                connectOrderConnector()
            }
        }
    }

    private fun connectOrderConnector() {
        if (orderConnector?.isConnected == true) {
            return
        }
        orderConnector?.connect()
    }

    fun getCloverAccount(): Account {
        return cloverAccount ?: synchronized(this) {
            CloverAccount.getAccount(applicationContext).also { cloverAccount = it }
        }
    }

    fun orderTax(order: Order?): Long {
        return OrderCalc(order).tax
    }

    fun orderLineItemTotal(order: Order?): Long {
        return OrderCalc(order).getLineSubtotal(order?.lineItems)
    }

    fun orderDiscount(order: Order?): Long {
        return OrderCalc(order).getLineSubtotalWithoutDiscounts(order?.lineItems) - OrderCalc(order).getDiscountedSubtotal(order?.lineItems)
    }

    private fun disconnectConnectors() {
        orderConnector?.disconnect()
        merchantConnector?.disconnect()
        inventoryConnector?.disconnect()
    }

    private fun storeIntoPreference() {
        filterArray.add(FilterData(0, FilterCategories.TenderType.name))
        filterArray.add(FilterData(0, FilterCategories.PaymentStatus.name))
        filterArray.add(FilterData(0, FilterCategories.EmployeeName.name))
        filterArray.add(FilterData(0, FilterCategories.OrderBookingType.name))
    }

    override fun onTerminate() {
        super.onTerminate()
        disconnectConnectors()
    }


}
