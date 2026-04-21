package com.orderMate.services

import android.content.Context
import com.clover.sdk.v3.order.Order
import com.orderMate.modals.WidgetConfig
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderDueDateResolver
import com.orderMate.utils.WidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages scheduled tasks for orders (notifications and receipt printing)
 * 
 * Call scheduleTasksForOrder() after an order note is saved to:
 * 1. Schedule email notification if enabled in Advanced Settings
 * 2. Schedule receipt print if enabled in Advanced Settings
 * 
 * Both use the order's due date (resolved via OrderDueDateResolver)
 */
object ScheduledTaskManager {
    
    /**
     * Schedule notification and print tasks for an order based on settings
     * 
     * @param context Application context
     * @param order The order to schedule tasks for
     */
    fun scheduleTasksForOrder(context: Context, order: Order) {
        val orderId = order.id ?: return
        val merchantId = MyApp.getInstance().getMerchantId() ?: return
        
        // Get widgets for due date resolution
        val widgets = WidgetManager.getCachedWidgets()
        
        // Resolve due date using the 3-priority system
        val dueDate = OrderDueDateResolver.resolveDueDate(order, widgets) ?: return
        val dueDateMillis = dueDate.time
        
        // Check settings and schedule tasks
        FirebaseConfigManager.getInstance().getAdvancedSettings(merchantId) { settings ->
            CoroutineScope(Dispatchers.Main).launch {
                // Schedule email notification if enabled
                if (settings.scheduledNotificationsEnabled) {
                    NotificationScheduler.scheduleOrderNotification(
                        context = context,
                        orderId = orderId,
                        dueDate = dueDateMillis,
                        merchantId = merchantId
                    )
                }
                
                // Schedule receipt print if enabled
                if (settings.scheduledReceiptEnabled) {
                    ReceiptPrintScheduler.scheduleReceiptPrint(
                        context = context,
                        orderId = orderId,
                        dueDate = dueDateMillis,
                        merchantId = merchantId
                    )
                }
            }
        }
    }
    
    /**
     * Schedule tasks for an order by ID (fetches order from Clover)
     * 
     * @param context Application context
     * @param orderId Order ID
     */
    fun scheduleTasksForOrder(context: Context, orderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val orderConnector = MyApp.getInstance().getOrderConnector()
                val order = orderConnector.getOrder(orderId) ?: return@launch
                scheduleTasksForOrder(context, order)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Cancel all scheduled tasks for an order
     * 
     * @param context Application context
     * @param orderId Order ID
     */
    fun cancelTasksForOrder(context: Context, orderId: String) {
        NotificationScheduler.cancelOrderNotification(context, orderId)
        ReceiptPrintScheduler.cancelReceiptPrint(context, orderId)
    }
    
    /**
     * Reschedule tasks for an order (cancel existing + schedule new)
     * Call this when order due date is updated
     * 
     * @param context Application context
     * @param order The updated order
     */
    fun rescheduleTasksForOrder(context: Context, order: Order) {
        val orderId = order.id ?: return
        cancelTasksForOrder(context, orderId)
        scheduleTasksForOrder(context, order)
    }
}
