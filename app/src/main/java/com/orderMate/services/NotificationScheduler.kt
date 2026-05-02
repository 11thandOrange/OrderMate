package com.orderMate.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.merchant.MerchantConnector
import com.clover.sdk.v3.order.OrderConnector
import com.orderMate.modals.Body
import com.orderMate.modals.Contact
import com.orderMate.modals.Html
import com.orderMate.modals.MessageMeta
import com.orderMate.modals.Metadata
import com.orderMate.modals.Receiver
import com.orderMate.modals.ShareMessageJson
import com.orderMate.repository.CloverRepository
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
 * Sends email to merchant via Bird API when alarm fires
 */
class OrderNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val orderId = intent.getStringExtra(NotificationScheduler.EXTRA_ORDER_ID) ?: return
        val merchantId = intent.getStringExtra(NotificationScheduler.EXTRA_MERCHANT_ID) ?: return
        val dueDate = intent.getLongExtra(NotificationScheduler.EXTRA_DUE_DATE, 0)
        
        // Send the notification/email in background
        CoroutineScope(Dispatchers.IO).launch {
            sendOrderNotification(context, orderId, merchantId, dueDate)
        }
    }
    
    private suspend fun sendOrderNotification(
        context: Context,
        orderId: String,
        merchantId: String,
        dueDate: Long
    ) {
        try {
            // 1. Get merchant email from Clover
            val cloverAccount = CloverAccount.getAccount(context) ?: return
            val merchantConnector = MerchantConnector(context, cloverAccount, null)
            merchantConnector.connect()
            val merchant = merchantConnector.merchant
            val merchantEmail = merchant?.supportEmail
            val merchantName = merchant?.name ?: "OrderMate"
            merchantConnector.disconnect()
            
            if (merchantEmail.isNullOrBlank()) {
                // No merchant email configured, cannot send notification
                return
            }
            
            // 2. Get order details from Clover
            val orderConnector = OrderConnector(context, cloverAccount, null)
            orderConnector.connect()
            var order = orderConnector.getOrder(orderId)
            orderConnector.disconnect()
            
            if (order == null) return
            
            // #78: Enrich order with full customer data (phone/email)
            order = CloverRepository.getInstance(context).enrichOrderWithFullCustomer(order)
            
            // 3. Build email content
            val customerName = order.customers?.firstOrNull()?.let { customer ->
                listOfNotNull(customer.firstName, customer.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            } ?: "Customer"
            
            val orderTotal = order.total?.let { "$${String.format("%.2f", it / 100.0)}" } ?: "$0.00"
            val itemCount = order.lineItems?.size ?: 0
            
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dueDateObj = Date(dueDate)
            val dueDateStr = dateFormat.format(dueDateObj)
            val dueTimeStr = timeFormat.format(dueDateObj)
            
            // Build line items HTML
            val lineItemsHtml = order.lineItems?.joinToString("") { lineItem ->
                val name = lineItem.name ?: "Item"
                val qty = lineItem.unitQty?.toInt() ?: 1
                val price = lineItem.price?.let { "$${String.format("%.2f", it / 100.0)}" } ?: ""
                "<tr><td style='padding:8px;border-bottom:1px solid #eee;'>$name</td><td style='padding:8px;border-bottom:1px solid #eee;text-align:center;'>$qty</td><td style='padding:8px;border-bottom:1px solid #eee;text-align:right;'>$price</td></tr>"
            } ?: ""
            
            // Build order notes section
            val orderNotesHtml = order.note?.let { note ->
                if (note.isNotBlank()) {
                    "<p style='margin-top:15px;padding:10px;background:#f5f5f5;border-radius:5px;'><strong>Notes:</strong> $note</p>"
                } else ""
            } ?: ""
            
            val shortOrderId = orderId.takeLast(4).uppercase()
            
            // Email HTML template (Google Calendar invite style)
            val emailHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(135deg, #FF9F43 0%, #FF6B35 100%); padding: 20px; border-radius: 10px 10px 0 0;">
                        <h1 style="color: white; margin: 0;">📅 Upcoming Order Reminder</h1>
                    </div>
                    <div style="background: white; padding: 20px; border: 1px solid #eee; border-top: none; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #333; margin-top: 0;">Order #$shortOrderId</h2>
                        
                        <table style="width: 100%; margin-bottom: 15px;">
                            <tr><td style="color: #666; padding: 5px 0;"><strong>Customer:</strong></td><td>$customerName</td></tr>
                            <tr><td style="color: #666; padding: 5px 0;"><strong>Due Date:</strong></td><td>$dueDateStr</td></tr>
                            <tr><td style="color: #666; padding: 5px 0;"><strong>Due Time:</strong></td><td>$dueTimeStr</td></tr>
                            <tr><td style="color: #666; padding: 5px 0;"><strong>Total:</strong></td><td style="font-weight: bold; color: #FF9F43;">$orderTotal</td></tr>
                        </table>
                        
                        <h3 style="color: #333; border-bottom: 2px solid #FF9F43; padding-bottom: 5px;">Items ($itemCount)</h3>
                        <table style="width: 100%; border-collapse: collapse;">
                            <thead>
                                <tr style="background: #f9f9f9;">
                                    <th style="padding: 8px; text-align: left;">Item</th>
                                    <th style="padding: 8px; text-align: center;">Qty</th>
                                    <th style="padding: 8px; text-align: right;">Price</th>
                                </tr>
                            </thead>
                            <tbody>$lineItemsHtml</tbody>
                        </table>
                        
                        $orderNotesHtml
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px; text-align: center;">
                            Sent by OrderMate for $merchantName
                        </p>
                    </div>
                </div>
            """.trimIndent()
            
            val plainText = """
                Upcoming Order Reminder
                
                Order #$shortOrderId
                Customer: $customerName
                Due Date: $dueDateStr at $dueTimeStr
                Total: $orderTotal
                Items: $itemCount
                
                ${order.note ?: ""}
                
                Sent by OrderMate for $merchantName
            """.trimIndent()
            
            // 4. Build ShareMessageJson for Bird API
            val html = Html(
                html = emailHtml,
                metadata = Metadata(subject = "📅 Order #$shortOrderId Due: $dueDateStr at $dueTimeStr"),
                text = plainText
            )
            val body = Body(html, Constants.html)
            val receiver = Receiver(listOf(
                Contact(identifierKey = "emailaddress", identifierValue = merchantEmail)
            ))
            val reference = "scheduled-notification-$orderId"
            val meta = MessageMeta(
                extraInformation = mapOf(
                    "orderId" to orderId,
                    "type" to "scheduled_notification"
                )
            )
            val emailData = ShareMessageJson(body, receiver, reference, meta)
            
            // 5. Send via Bird API
            val repository = CloverRepository.getInstance(context)
            repository.sendEmail(emailData)
            
        } catch (e: Exception) {
            e.printStackTrace()
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
