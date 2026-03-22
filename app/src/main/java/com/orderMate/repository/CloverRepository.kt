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
     */
    suspend fun searchCustomers(query: String): List<Customer> = withContext(Dispatchers.IO) {
        try {
            val customerConnector = myApp.getCustomerConnector()
                ?: return@withContext emptyList()

            // Get all customers and filter locally
            // Note: Clover SDK's getCustomers doesn't support direct search
            val allCustomers = customerConnector.customers ?: return@withContext emptyList()

            val lowerQuery = query.lowercase()
            allCustomers.filter { customer ->
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

            // Create customer object
            val customer = Customer().apply {
                this.firstName = firstName
                this.lastName = lastName
            }

            // Create customer in Clover
            val createdCustomer = customerConnector.createCustomer(customer)
                ?: return@withContext null

            // Add phone number if provided
            if (!phone.isNullOrBlank()) {
                val phoneNumber = PhoneNumber().apply {
                    this.phoneNumber = phone
                }
                customerConnector.createPhoneNumber(createdCustomer.id, phoneNumber)
            }

            // Add email if provided
            if (!email.isNullOrBlank()) {
                val emailAddress = EmailAddress().apply {
                    this.emailAddress = email
                }
                customerConnector.createEmailAddress(createdCustomer.id, emailAddress)
            }

            // Return the updated customer
            customerConnector.getCustomer(createdCustomer.id)
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

            // Get existing customer
            val existingCustomer = customerConnector.getCustomer(customerId)
                ?: return@withContext null

            // Update name fields
            val updatedCustomer = Customer().apply {
                this.id = customerId
                this.firstName = firstName ?: existingCustomer.firstName
                this.lastName = lastName ?: existingCustomer.lastName
            }
            customerConnector.updateCustomer(updatedCustomer)

            // Update phone number
            if (!phone.isNullOrBlank()) {
                val existingPhone = existingCustomer.phoneNumbers?.firstOrNull()
                if (existingPhone != null) {
                    // Update existing phone
                    val phoneNumber = PhoneNumber().apply {
                        this.id = existingPhone.id
                        this.phoneNumber = phone
                    }
                    customerConnector.updatePhoneNumber(customerId, phoneNumber)
                } else {
                    // Create new phone
                    val phoneNumber = PhoneNumber().apply {
                        this.phoneNumber = phone
                    }
                    customerConnector.createPhoneNumber(customerId, phoneNumber)
                }
            }

            // Update email
            if (!email.isNullOrBlank()) {
                val existingEmail = existingCustomer.emailAddresses?.firstOrNull()
                if (existingEmail != null) {
                    // Update existing email
                    val emailAddress = EmailAddress().apply {
                        this.id = existingEmail.id
                        this.emailAddress = email
                    }
                    customerConnector.updateEmailAddress(customerId, emailAddress)
                } else {
                    // Create new email
                    val emailAddress = EmailAddress().apply {
                        this.emailAddress = email
                    }
                    customerConnector.createEmailAddress(customerId, emailAddress)
                }
            }

            // Return the updated customer
            customerConnector.getCustomer(customerId)
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
            val customerConnector = myApp.getCustomerConnector()
                ?: return@withContext false

            // Get the customer
            val customer = customerConnector.getCustomer(customerId)
                ?: return@withContext false

            // Get current order
            val order = orderConnector.getOrder(orderId)
                ?: return@withContext false

            // Add customer to order
            orderConnector.addCustomer(orderId, customer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}