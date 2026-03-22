package com.orderMate.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.SettingsManager
import java.util.Calendar

/**
 * Scheduled Order Notification Service (#83 requirement)
 * 
 * Schedules email notifications X days and Y minutes before order due date.
 * Uses AlarmManager for reliable background execution on Clover devices.
 * 
 * Features:
 * - Schedule notification based on order due date
 * - Template variable substitution
 * - Cancel/reschedule when order is updated
 */
object NotificationScheduler {
    
    private const val ACTION_ORDER_NOTIFICATION = "com.orderMate.ACTION_ORDER_NOTIFICATION"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_MERCHANT_ID = "merchant_id"
    const val EXTRA_DUE_DATE = "due_date"
    
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
        val settingsManager = SettingsManager(context)
        val notificationDays = settingsManager.getNotificationDays()
        val notificationMinutes = settingsManager.getNotificationMinutes()
        
        // Calculate notification time
        val notificationTime = calculateNotificationTime(dueDate, notificationDays, notificationMinutes)
        val now = System.currentTimeMillis()
        
        if (notificationTime <= now) {
            // Due date already passed or notification time is in the past
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createNotificationIntent(context, orderId, merchantId, dueDate)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            orderId.hashCode(),
            intent,
            getPendingIntentFlags()
        )
        
        // Schedule alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }
    
    /**
     * Cancel scheduled notification for an order
     */
    fun cancelOrderNotification(context: Context, orderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, OrderNotificationReceiver::class.java).apply {
            action = ACTION_ORDER_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            orderId.hashCode(),
            intent,
            getPendingIntentFlags()
        )
        alarmManager.cancel(pendingIntent)
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
    
    private fun createNotificationIntent(
        context: Context,
        orderId: String,
        merchantId: String,
        dueDate: Long
    ): Intent {
        return Intent(context, OrderNotificationReceiver::class.java).apply {
            action = ACTION_ORDER_NOTIFICATION
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_MERCHANT_ID, merchantId)
            putExtra(EXTRA_DUE_DATE, dueDate)
        }
    }
    
    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}

/**
 * BroadcastReceiver that handles scheduled notifications
 */
class OrderNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val orderId = intent.getStringExtra(NotificationScheduler.EXTRA_ORDER_ID) ?: return
        val merchantId = intent.getStringExtra(NotificationScheduler.EXTRA_MERCHANT_ID) ?: return
        val dueDate = intent.getLongExtra(NotificationScheduler.EXTRA_DUE_DATE, 0)
        
        // Send the notification/email
        sendOrderNotification(context, orderId, merchantId, dueDate)
    }
    
    private fun sendOrderNotification(
        context: Context,
        orderId: String,
        merchantId: String,
        dueDate: Long
    ) {
        // This would integrate with your email service
        // For now, we'll create the notification content using template substitution
        
        val firebase = FirebaseConfigManager.getInstance()
        
        // Get templates and send notification
        firebase.getTemplates(merchantId) { templates ->
            if (templates.isNotEmpty()) {
                val template = templates.first()
                // Process template and send email
                // EmailService.send(customerEmail, "Order Reminder", processedContent)
            }
        }
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
