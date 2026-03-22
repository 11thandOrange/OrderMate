package com.orderMate.services

import android.content.Context
import androidx.work.*
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.NotificationTemplate
import com.orderMate.utils.PreferenceManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Scheduled Order Notification Service (#83 requirement)
 * 
 * Schedules email notifications X days and Y minutes before order due date.
 * Uses WorkManager for reliable background execution.
 * 
 * Features:
 * - Schedule notification based on order due date
 * - Template variable substitution
 * - Cancel/reschedule when order is updated
 */
object NotificationScheduler {
    
    private const val NOTIFICATION_WORK_PREFIX = "order_notification_"
    
    /**
     * Schedule a notification for an order
     * 
     * @param context Application context
     * @param orderId Order ID
     * @param dueDate Due date timestamp (milliseconds)
     * @param merchantId Merchant ID for fetching settings
     */
    fun scheduleOrderNotification(
        context: Context,
        orderId: String,
        dueDate: Long,
        merchantId: String
    ) {
        val prefManager = PreferenceManager.getInstance(context)
        val notificationDays = prefManager.getInt("notification_days", 3)
        val notificationMinutes = prefManager.getInt("notification_minutes", 0)
        
        // Calculate notification time
        val notificationTime = calculateNotificationTime(dueDate, notificationDays, notificationMinutes)
        val delay = notificationTime - System.currentTimeMillis()
        
        if (delay <= 0) {
            // Due date already passed or notification time is in the past
            return
        }
        
        // Build work request
        val workRequest = OneTimeWorkRequestBuilder<OrderNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    OrderNotificationWorker.KEY_ORDER_ID to orderId,
                    OrderNotificationWorker.KEY_MERCHANT_ID to merchantId,
                    OrderNotificationWorker.KEY_DUE_DATE to dueDate
                )
            )
            .addTag(getWorkTag(orderId))
            .build()
        
        // Enqueue with unique work to replace any existing notification for this order
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                getWorkTag(orderId),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
    
    /**
     * Cancel scheduled notification for an order
     */
    fun cancelOrderNotification(context: Context, orderId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(getWorkTag(orderId))
    }
    
    /**
     * Calculate notification time based on settings
     * 
     * @param dueDate Order due date timestamp
     * @param days Days before due date
     * @param minutes Minutes before due date
     * @return Notification timestamp
     */
    fun calculateNotificationTime(dueDate: Long, days: Int, minutes: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dueDate
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.add(Calendar.MINUTE, -minutes)
        return calendar.timeInMillis
    }
    
    private fun getWorkTag(orderId: String) = "$NOTIFICATION_WORK_PREFIX$orderId"
}

/**
 * Worker that sends the actual notification/email
 */
class OrderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_ORDER_ID = "order_id"
        const val KEY_MERCHANT_ID = "merchant_id"
        const val KEY_DUE_DATE = "due_date"
    }
    
    override suspend fun doWork(): Result {
        val orderId = inputData.getString(KEY_ORDER_ID) ?: return Result.failure()
        val merchantId = inputData.getString(KEY_MERCHANT_ID) ?: return Result.failure()
        val dueDate = inputData.getLong(KEY_DUE_DATE, 0)
        
        return try {
            // Fetch templates and order data, then send notification
            sendOrderNotification(orderId, merchantId, dueDate)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private suspend fun sendOrderNotification(orderId: String, merchantId: String, dueDate: Long) {
        // This would integrate with your email service
        // For now, we'll create the notification content using template substitution
        
        val firebase = FirebaseConfigManager.getInstance()
        
        // Get templates (synchronous wrapper needed for coroutine context)
        // In production, use suspendCoroutine or Flow
        
        // Create notification content
        val templateContent = "Your order is due soon!"
        
        // Send via your email/notification service
        // EmailService.send(customerEmail, subject, processedContent)
    }
}

/**
 * Template variable substitution utility
 */
object TemplateProcessor {
    
    // Available template variables
    private val VARIABLES = mapOf(
        "{{merchant_name}}" to "merchant_name",
        "{{order_id}}" to "order_id",
        "{{customer_name}}" to "customer_name",
        "{{order_total}}" to "order_total",
        "{{due_date}}" to "due_date",
        "{{due_time}}" to "due_time",
        "{{item_count}}" to "item_count",
        "{{order_notes}}" to "order_notes"
    )
    
    /**
     * Process a template string, replacing variables with actual values
     * 
     * @param template Template string with {{variable}} placeholders
     * @param values Map of variable names to their values
     * @return Processed string with variables replaced
     */
    fun process(template: String, values: Map<String, String>): String {
        var result = template
        
        VARIABLES.forEach { (placeholder, key) ->
            val value = values[key] ?: ""
            result = result.replace(placeholder, value)
        }
        
        return result
    }
    
    /**
     * Process template with order data
     */
    fun processForOrder(
        template: String,
        merchantName: String,
        orderId: String,
        customerName: String = "",
        orderTotal: String = "",
        dueDate: String = "",
        dueTime: String = "",
        itemCount: Int = 0,
        orderNotes: String = ""
    ): String {
        return process(template, mapOf(
            "merchant_name" to merchantName,
            "order_id" to orderId,
            "customer_name" to customerName,
            "order_total" to orderTotal,
            "due_date" to dueDate,
            "due_time" to dueTime,
            "item_count" to itemCount.toString(),
            "order_notes" to orderNotes
        ))
    }
    
    /**
     * Get list of available template variables for UI display
     */
    fun getAvailableVariables(): List<String> = VARIABLES.keys.toList()
    
    /**
     * Preview template with sample data
     */
    fun preview(template: String): String {
        return processForOrder(
            template = template,
            merchantName = "Sample Bakery",
            orderId = "ORD-12345",
            customerName = "John Doe",
            orderTotal = "$45.99",
            dueDate = "March 25, 2026",
            dueTime = "2:30 PM",
            itemCount = 3,
            orderNotes = "Extra frosting"
        )
    }
}
