package com.orderMate.utils

import android.accounts.Account
import android.app.Application
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.ForbiddenException
import com.clover.sdk.v1.merchant.MerchantConnector
import com.clover.sdk.v3.employees.EmployeeConnector
import com.clover.sdk.v3.inventory.InventoryConnector
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.order.OrderCalc
import com.clover.sdk.v3.order.OrderV31Connector
import com.google.firebase.FirebaseApp


class MyApp : Application() {
    private var cloverAccount: Account? = null
    private var orderConnector: OrderV31Connector? = null
    private var inventoryConnector: InventoryConnector? = null
    private var employeeConnector: EmployeeConnector? = null
    private var merchantConnector: MerchantConnector? = null

    companion object {
         var latestAxis: Pair<Int?, Int?>? = Pair(700, 700)

        /*
          * We will basically apply the filters on the basis of indexing
          *  index  1   > order status (Paid , Open etc)
          *  index 2   > order employee name (name of employee who made the order)
          *  index 3   > order tender type (cash , cheque)
          *  index 4   > order made mode (online , pos)
          * index 5   > order notes (status , category )
          * */
        val filterArray: MutableMap<String, Int> = mutableMapOf()

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

    fun getEmployeeConnector(): EmployeeConnector? {
        return try {
            employeeConnector ?: synchronized(this) {
                EmployeeConnector(applicationContext, getCloverAccount(), null).also {
                    employeeConnector = it
                    employeeOrderConnector()
                }
            }
        } catch (e: ForbiddenException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getEmployeeName(employeeId: String?): String? {

        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null
        }
        val id = employeeConnector?.getEmployee(employeeId)?.name
        return employeeConnector?.getEmployee(employeeId)?.name
    }


    private fun getMerchantConnector(): MerchantConnector {
        return merchantConnector ?: synchronized(this) {
            MerchantConnector(applicationContext, getCloverAccount(), null).also {
                merchantConnector = it
                merchantConnector()
            }
        }
    }

    private fun employeeOrderConnector() {
        if (employeeConnector?.isConnected == true) {
            return
        }
        employeeConnector?.connect()
    }

    private fun merchantConnector() {
        if (merchantConnector?.isConnected == true) {
            return
        }
        merchantConnector?.connect()
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

    fun getMerchantId(): String? {
        return try {
            val data = getMerchantConnector().merchant
            data.id
        }catch (e : Exception){
            e.printStackTrace()
            null
        }
    }

    fun orderTax(order: Order?): Long {
        return OrderCalc(order).tax
    }

    fun orderLineItemTotal(order: Order?): Long {
        return OrderCalc(order).getLineSubtotal(order?.lineItems)
    }

    fun orderDiscount(order: Order?): Long {
        return OrderCalc(order).getLineSubtotalWithoutDiscounts(order?.lineItems)
    }

    fun disconnectConnectors() {
        orderConnector?.disconnect()
        merchantConnector?.disconnect()
        inventoryConnector?.disconnect()
    }


    /*
    * The order of the below filterArray is important and change in the sequence lead to change in the sequence
    * a Filter values for the notes.
    * */
    private fun storeIntoPreference() {
        filterArray[FilterCategories.TenderType.name] = 0
        filterArray[FilterCategories.PaymentStatus.name] = 0
        filterArray[FilterCategories.EmployeeName.name] = 0
        filterArray[FilterCategories.OrderBookingType.name] = 0
        filterArray[ModalDialogCategories.OrderProgress.name] = 0
        filterArray[ModalDialogCategories.OrderType.name] = 0
        filterArray[ModalDialogCategories.OrderCategories.name] = 0
        filterArray[ModalDialogCategories.OrderSubCategories.name] = 0

    }

    override fun onTerminate() {
        super.onTerminate()
        disconnectConnectors()
    }


}
