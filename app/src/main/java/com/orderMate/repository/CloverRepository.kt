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
import com.clover.sdk.v1.customer.Customer as V1Customer


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
     * Search for customers in the full Clover customer database
     * Uses CustomerConnector to search all merchant customers
     */
    suspend fun searchCustomers(query: String): List<Customer> = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector()
            
            if (customerConnector != null) {
                // Search full Clover customer database
                val v1Customers: List<V1Customer> = if (query.isBlank()) {
                    customerConnector.getCustomers() ?: emptyList()
                } else {
                    customerConnector.getCustomers(query) ?: emptyList()
                }
                
                // Convert v1 customers to v3 customers (used by rest of app)
                v1Customers.take(50).mapNotNull { v1Customer ->
                    convertV1ToV3Customer(v1Customer)
                }
            } else {
                // Fallback: search from recent orders if connector unavailable
                searchCustomersFromOrders(query)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to order-based search on error
            searchCustomersFromOrders(query)
        }
    }

    /**
     * Convert v1 Customer to v3 Customer for compatibility with rest of app
     */
    private fun convertV1ToV3Customer(v1Customer: V1Customer): Customer? {
        return try {
            val customer = Customer()
            customer.setId(v1Customer.id)
            customer.setFirstName(v1Customer.firstName)
            customer.setLastName(v1Customer.lastName)
            
            // Convert phone numbers
            val phoneNumbers = v1Customer.phoneNumbers?.mapNotNull { v1Phone ->
                PhoneNumber().apply {
                    setId(v1Phone.id)
                    setPhoneNumber(v1Phone.phoneNumber)
                }
            }
            if (!phoneNumbers.isNullOrEmpty()) {
                customer.setPhoneNumbers(phoneNumbers)
            }
            
            // Convert email addresses
            val emailAddresses = v1Customer.emailAddresses?.mapNotNull { v1Email ->
                EmailAddress().apply {
                    setId(v1Email.id)
                    setEmailAddress(v1Email.emailAddress)
                }
            }
            if (!emailAddresses.isNullOrEmpty()) {
                customer.setEmailAddresses(emailAddresses)
            }
            
            customer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fallback: Search customers from recent orders
     */
    private fun searchCustomersFromOrders(query: String): List<Customer> {
        return try {
            val orderConnector = myApp.getOrderConnector()
            val orders = orderConnector.getOrders(mutableListOf()) ?: return emptyList()
            
            val lowerQuery = query.lowercase()
            val customersMap = mutableMapOf<String, Customer>()
            
            orders.forEach { order ->
                order?.customers?.forEach { customer ->
                    if (customer?.id != null && !customersMap.containsKey(customer.id)) {
                        val firstName = customer.firstName?.lowercase() ?: ""
                        val lastName = customer.lastName?.lowercase() ?: ""
                        val phone = customer.phoneNumbers?.firstOrNull()?.phoneNumber?.lowercase() ?: ""
                        val email = customer.emailAddresses?.firstOrNull()?.emailAddress?.lowercase() ?: ""
                        
                        if (query.isBlank() ||
                            firstName.contains(lowerQuery) ||
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
     * #48: Create and save a new customer to Clover
     * Persists the customer to Clover's customer database
     */
    suspend fun createAndSaveCustomerToClover(
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector() ?: return@withContext null
            
            // Create v1 customer (required for CustomerConnector.createCustomer)
            val v1Customer = V1Customer()
            v1Customer.firstName = firstName ?: ""
            v1Customer.lastName = lastName ?: ""
            
            // Create customer in Clover
            val createdV1Customer = customerConnector.createCustomer(v1Customer) ?: return@withContext null
            
            // Add phone number if provided
            if (!phone.isNullOrBlank()) {
                val v1Phone = com.clover.sdk.v1.customer.PhoneNumber()
                v1Phone.phoneNumber = phone
                customerConnector.addPhoneNumber(createdV1Customer.id, v1Phone)
            }
            
            // Add email if provided
            if (!email.isNullOrBlank()) {
                val v1Email = com.clover.sdk.v1.customer.EmailAddress()
                v1Email.emailAddress = email
                customerConnector.addEmailAddress(createdV1Customer.id, v1Email)
            }
            
            // Convert to v3 Customer for return
            convertV1ToV3Customer(createdV1Customer)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to in-memory customer if Clover save fails
            createCustomer(firstName, lastName, phone, email)
        }
    }
    
    /**
     * #48: Get customer by ID from Clover
     */
    suspend fun getCustomerById(customerId: String): Customer? = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector() ?: return@withContext null
            val v1Customer = customerConnector.getCustomer(customerId) ?: return@withContext null
            convertV1ToV3Customer(v1Customer)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * #48: Update an existing customer in Clover
     */
    suspend fun updateCustomerInClover(
        customerId: String,
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector() ?: return@withContext null
            
            // Get existing customer
            val existingCustomer = customerConnector.getCustomer(customerId) ?: return@withContext null
            
            // Update basic info
            existingCustomer.firstName = firstName ?: ""
            existingCustomer.lastName = lastName ?: ""
            
            // Update customer in Clover
            customerConnector.setName(customerId, existingCustomer.firstName, existingCustomer.lastName)
            
            // Update phone - delete existing and add new
            if (!phone.isNullOrBlank()) {
                existingCustomer.phoneNumbers?.firstOrNull()?.let { existingPhone ->
                    customerConnector.deletePhoneNumber(customerId, existingPhone.id)
                }
                val v1Phone = com.clover.sdk.v1.customer.PhoneNumber()
                v1Phone.phoneNumber = phone
                customerConnector.addPhoneNumber(customerId, v1Phone)
            }
            
            // Update email - delete existing and add new
            if (!email.isNullOrBlank()) {
                existingCustomer.emailAddresses?.firstOrNull()?.let { existingEmail ->
                    customerConnector.deleteEmailAddress(customerId, existingEmail.id)
                }
                val v1Email = com.clover.sdk.v1.customer.EmailAddress()
                v1Email.emailAddress = email
                customerConnector.addEmailAddress(customerId, v1Email)
            }
            
            // Get updated customer and convert
            val updatedCustomer = customerConnector.getCustomer(customerId)
            updatedCustomer?.let { convertV1ToV3Customer(it) }
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
     * Assign a customer to an order by customer ID
     * Gets the order and updates it with the customer
     */
    suspend fun assignCustomerToOrder(orderId: String, customerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()
            
            // Get current order
            val order = orderConnector.getOrder(orderId) ?: return@withContext false
            
            // Create a customer reference with the ID
            val customer = Customer()
            customer.setId(customerId)
            
            // Update order with customer - use updateOrder or similar
            order.setCustomers(listOf(customer))
            orderConnector.updateOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Assign a full customer object to an order
     * Updates the order with the new customer info
     */
    suspend fun assignCustomerToOrder(orderId: String, customer: Customer): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()
            
            // Get current order
            val order = orderConnector.getOrder(orderId) ?: return@withContext false
            
            // Update order with the customer
            order.setCustomers(listOf(customer))
            orderConnector.updateOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ==================== Order Notes (#93) ====================
    
    /**
     * Save order-level note to Order.note field
     * Format: "Label:Value • Label:Value"
     */
    suspend fun saveOrderNote(orderId: String, note: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()
            
            // Get current order
            val order = orderConnector.getOrder(orderId) ?: return@withContext false
            
            // Update the note field
            order.setNote(note)
            orderConnector.updateOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get order-level note from Order.note field
     */
    fun getOrderNote(orderId: String): String? {
        return try {
            val orderConnector = myApp.getOrderConnector()
            val order = orderConnector.getOrder(orderId)
            order?.note
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get order-level note from an Order object
     */
    fun getOrderNote(order: com.clover.sdk.v3.order.Order): String? {
        return order.note
    }
    
    /**
     * Clear order-level note
     */
    suspend fun clearOrderNote(orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()
            val order = orderConnector.getOrder(orderId) ?: return@withContext false
            
            order.setNote(null)
            orderConnector.updateOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}