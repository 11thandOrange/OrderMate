package com.orderMate.repository

import android.content.Context
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.customers.EmailAddress
import com.clover.sdk.v3.customers.PhoneNumber
import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal
import com.orderMate.networkManager.RetrofitInstanceWithAuth
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Repository for Clover API operations including:
 * - Bird Messaging API (SMS/Email)
 * - Customer management (create, update, assign to order)
 */
class CloverRepository private constructor(private val context: Context) {

    private val myApp = MyApp.getInstance()

    companion object {
        @Volatile
        private var instance: CloverRepository? = null

        fun getInstance(context: Context): CloverRepository {
            return instance ?: synchronized(this) {
                instance ?: CloverRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Legacy getInstance for backward compatibility
        fun getInstance(): CloverRepository {
            return instance ?: throw IllegalStateException(
                "CloverRepository not initialized. Call getInstance(context) first."
            )
        }

        private val apiWithAuth = RetrofitInstanceWithAuth.getApiService()
    }

    // ==================== Messaging API ====================

    suspend fun sendEmail(data: ShareMessageJson) =
        apiWithAuth.shareEmail(Constants.workSpaceId, Constants.channelId, data)

    suspend fun sendSms(data: ShareSmsModal) =
        apiWithAuth.shareSms(Constants.workSpaceId, Constants.SMSChannelId, data)

    // ==================== Customer Management ====================

    /**
     * Search for customers - returns customers from recent orders
     * Note: Full customer search requires Clover Customers app integration
     */
    suspend fun searchCustomers(query: String): List<Customer> = withContext(Dispatchers.IO) {
        try {
            // Get customers from recent orders as a search source
            val orderConnector = myApp.getOrderConnector()
            val orders = orderConnector.getOrders(mutableListOf()) ?: return@withContext emptyList()
            
            val lowerQuery = query.lowercase()
            val customersMap = mutableMapOf<String, Customer>()
            
            // Collect unique customers from orders
            orders.forEach { order ->
                order?.customers?.forEach { customer ->
                    if (customer?.id != null && !customersMap.containsKey(customer.id)) {
                        val firstName = customer.firstName?.lowercase() ?: ""
                        val lastName = customer.lastName?.lowercase() ?: ""
                        val phone = customer.phoneNumbers?.firstOrNull()?.phoneNumber?.lowercase() ?: ""
                        val email = customer.emailAddresses?.firstOrNull()?.emailAddress?.lowercase() ?: ""
                        
                        if (firstName.contains(lowerQuery) ||
                            lastName.contains(lowerQuery) ||
                            "$firstName $lastName".contains(lowerQuery) ||
                            phone.contains(lowerQuery) ||
                            email.contains(lowerQuery)) {
                            customersMap[customer.id] = customer
                        }
                    }
                }
            }
            
            customersMap.values.take(20).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Create a new customer object (in-memory)
     * The customer will be persisted when assigned to an order
     */
    suspend fun createCustomer(
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            // Create customer object with contact info
            val customer = Customer()
            customer.setFirstName(firstName)
            customer.setLastName(lastName)
            
            // Add phone number if provided
            if (!phone.isNullOrBlank()) {
                val phoneNumber = PhoneNumber()
                phoneNumber.setPhoneNumber(phone)
                customer.setPhoneNumbers(listOf(phoneNumber))
            }

            // Add email if provided
            if (!email.isNullOrBlank()) {
                val emailAddress = EmailAddress()
                emailAddress.setEmailAddress(email)
                customer.setEmailAddresses(listOf(emailAddress))
            }

            customer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Update an existing customer object
     */
    suspend fun updateCustomer(
        customerId: String,
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            // Create updated customer object
            val customer = Customer()
            customer.setId(customerId)
            customer.setFirstName(firstName)
            customer.setLastName(lastName)

            // Add phone number if provided
            if (!phone.isNullOrBlank()) {
                val phoneNumber = PhoneNumber()
                phoneNumber.setPhoneNumber(phone)
                customer.setPhoneNumbers(listOf(phoneNumber))
            }

            // Add email if provided
            if (!email.isNullOrBlank()) {
                val emailAddress = EmailAddress()
                emailAddress.setEmailAddress(email)
                customer.setEmailAddresses(listOf(emailAddress))
            }

            customer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Assign a customer to an order
     * Uses OrderConnector.addCustomer to link customer to order in Clover
     */
    suspend fun assignCustomerToOrder(orderId: String, customerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()

            // Create a customer reference with the ID
            val customer = Customer()
            customer.setId(customerId)

            // Add customer to order - this persists in Clover
            orderConnector.addCustomer(orderId, customer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Assign a full customer object to an order
     * This creates/updates the customer in Clover and links to the order
     */
    suspend fun assignCustomerToOrder(orderId: String, customer: Customer): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()
            
            // Add customer to order - Clover will create/update the customer
            orderConnector.addCustomer(orderId, customer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}