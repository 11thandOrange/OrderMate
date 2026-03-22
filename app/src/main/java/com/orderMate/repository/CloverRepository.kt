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
 * - Customer management (search, create, update)
 * - Order-customer assignment
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
     * Search for customers by name, phone, or email
     * Uses CustomerConnector.getCustomers() and filters locally
     */
    suspend fun searchCustomers(query: String): List<Customer> = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector()
                ?: return@withContext emptyList()

            // Get all customers using the connector's getCustomers method
            val allCustomers: List<Customer> = customerConnector.getCustomers() ?: return@withContext emptyList()

            val lowerQuery = query.lowercase()
            allCustomers.filter { customer: Customer ->
                val firstName = customer.firstName?.lowercase() ?: ""
                val lastName = customer.lastName?.lowercase() ?: ""
                val phone = customer.phoneNumbers?.firstOrNull()?.phoneNumber?.lowercase() ?: ""
                val email = customer.emailAddresses?.firstOrNull()?.emailAddress?.lowercase() ?: ""

                firstName.contains(lowerQuery) ||
                lastName.contains(lowerQuery) ||
                "$firstName $lastName".contains(lowerQuery) ||
                phone.contains(lowerQuery) ||
                email.contains(lowerQuery)
            }.take(20) // Limit results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Create a new customer in Clover
     * Uses CustomerConnector to create customer and add contact info
     */
    suspend fun createCustomer(
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector()
                ?: return@withContext null

            // Create customer object with basic info
            val customer = Customer().apply {
                setFirstName(firstName)
                setLastName(lastName)
            }

            // Create customer in Clover
            val createdCustomer = customerConnector.createCustomer(customer)
                ?: return@withContext null

            // Add phone number if provided
            if (!phone.isNullOrBlank()) {
                try {
                    val phoneNumber = PhoneNumber().apply {
                        setPhoneNumber(phone)
                    }
                    customerConnector.addPhoneNumber(createdCustomer.id, phoneNumber)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Add email if provided
            if (!email.isNullOrBlank()) {
                try {
                    val emailAddress = EmailAddress().apply {
                        setEmailAddress(email)
                    }
                    customerConnector.addEmailAddress(createdCustomer.id, emailAddress)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Return the created customer (may not have updated contact info)
            createdCustomer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Update an existing customer in Clover
     */
    suspend fun updateCustomer(
        customerId: String,
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): Customer? = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector()
                ?: return@withContext null

            // Create updated customer object
            val updatedCustomer = Customer().apply {
                setId(customerId)
                setFirstName(firstName)
                setLastName(lastName)
            }
            
            // Update the customer
            customerConnector.setCustomer(updatedCustomer)

            // Handle phone number update
            if (!phone.isNullOrBlank()) {
                try {
                    val phoneNumber = PhoneNumber().apply {
                        setPhoneNumber(phone)
                    }
                    // Try to add new phone number (Clover may handle duplicates)
                    customerConnector.addPhoneNumber(customerId, phoneNumber)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Handle email update
            if (!email.isNullOrBlank()) {
                try {
                    val emailAddress = EmailAddress().apply {
                        setEmailAddress(email)
                    }
                    // Try to add new email (Clover may handle duplicates)
                    customerConnector.addEmailAddress(customerId, emailAddress)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            updatedCustomer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Assign a customer to an order
     */
    suspend fun assignCustomerToOrder(orderId: String, customerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderConnector = myApp.getOrderConnector()

            // Create a customer reference with just the ID
            val customer = Customer().apply {
                setId(customerId)
            }

            // Add customer to order using OrderConnector
            orderConnector.setCustomer(orderId, customer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}