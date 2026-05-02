package com.orderMate.utils

import android.accounts.Account
import android.app.Application
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.ForbiddenException
import com.clover.sdk.v1.customer.CustomerConnector
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
    private var customerConnector: CustomerConnector? = null

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
        // Connect CustomerConnector early so it's ready when needed
        initializeCustomerConnector()
    }
    
    /**
     * Initialize CustomerConnector early in app startup.
     * This ensures the connection is established before any customer operations are attempted.
     */
    private fun initializeCustomerConnector() {
        try {
            getCustomerConnector()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    /**
     * Get the CustomerConnector for searching/managing Clover customers
     */
    fun getCustomerConnector(): CustomerConnector? {
        return try {
            customerConnector ?: synchronized(this) {
                CustomerConnector(applicationContext, getCloverAccount(), null).also {
                    customerConnector = it
                    connectCustomerConnector()
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

    private fun connectCustomerConnector() {
        if (customerConnector?.isConnected == true) {
            return
        }
        customerConnector?.connect()
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
    
    fun getMerchantName(): String? {
        return try {
            val data = getMerchantConnector().merchant
            data.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==================== Order Calculations (using Clover OrderCalc) ====================
    
    /**
     * #78: Get tax amount from Clover OrderCalc
     */
    fun orderTax(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getTax(order.lineItems)
    }

    /**
     * #78: Get subtotal BEFORE discounts from Clover OrderCalc
     */
    fun orderLineItemTotal(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getLineSubtotalWithoutDiscounts(order.lineItems)
    }

    /**
     * #78: Get subtotal AFTER discounts from Clover OrderCalc
     */
    fun orderSubtotalAfterDiscounts(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getLineSubtotal(order.lineItems)
    }

    /**
     * #78: Get discount amount from Clover OrderCalc
     * Calculated as: subtotal before discounts - subtotal after discounts
     */
    fun orderDiscount(order: Order?): Long {
        if (order == null) return 0L
        val calc = OrderCalc(order)
        val beforeDiscount = calc.getLineSubtotalWithoutDiscounts(order.lineItems)
        val afterDiscount = calc.getLineSubtotal(order.lineItems)
        return beforeDiscount - afterDiscount
    }

    /**
     * #78: Get service charge from Clover OrderCalc
     */
    fun orderServiceCharge(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getServiceCharge(order.lineItems)
    }

    /**
     * #78: Get order fees from Clover OrderCalc
     * Uses getTotalOrderFeeFromLineItems if available, otherwise returns 0
     */
    fun orderFees(order: Order?): Long {
        if (order == null) return 0L
        return try {
            OrderCalc(order).getTotalOrderFeeFromLineItems(order.lineItems)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * #78: Get total (subtotal + tax + service charge) from Clover OrderCalc
     * Note: Does NOT include tip
     */
    fun orderTotal(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getTotal(order.lineItems)
    }

    /**
     * #78: Get total with tip from Clover OrderCalc
     */
    fun orderTotalWithTip(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).getTotalWithTip(order.lineItems)
    }

    /**
     * #78: Get tip amount from Clover OrderCalc
     */
    fun orderTip(order: Order?): Long {
        if (order == null) return 0L
        return OrderCalc(order).tip
    }

    fun disconnectConnectors() {
        orderConnector?.disconnect()
        merchantConnector?.disconnect()
        inventoryConnector?.disconnect()
        customerConnector?.disconnect()
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
