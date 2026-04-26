package com.orderMate.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.printer.Category
import com.clover.sdk.v1.printer.job.StaticOrderPrintJob
import com.clover.sdk.v3.order.OrderConnector
import com.clover.sdk.v1.printer.PrinterConnector
import com.orderMate.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Scheduled Receipt Printing Service (#83 requirement)
 * 
 * Schedules receipt printing X days and Y minutes before order due date.
 * Uses AlarmManager for reliable background execution on Clover devices.
 * Prints to kitchen/order printer via Clover Printer SDK.
 */
object ReceiptPrintScheduler {
    
    private const val ACTION_PRINT_RECEIPT = "com.orderMate.ACTION_PRINT_RECEIPT"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_MERCHANT_ID = "merchant_id"
    
    /**
     * Schedule a receipt print for an order
     * 
     * @param context Application context
     * @param orderId Order ID
     * @param dueDate Due date timestamp (milliseconds)
     * @param merchantId Merchant ID for fetching settings
     */
    fun scheduleReceiptPrint(
        context: Context,
        orderId: String,
        dueDate: Long,
        merchantId: String
    ) {
        val settingsManager = SettingsManager(context)
        val receiptDays = settingsManager.getReceiptDays()
        val receiptMinutes = settingsManager.getReceiptMinutes()
        
        // Calculate print time
        val printTime = calculatePrintTime(dueDate, receiptDays, receiptMinutes)
        val now = System.currentTimeMillis()
        
        if (printTime <= now) {
            // Due date already passed or print time is in the past
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createPrintIntent(context, orderId, merchantId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "print_$orderId".hashCode(),
            intent,
            getPendingIntentFlags()
        )
        
        // Schedule alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                printTime,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                printTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                printTime,
                pendingIntent
            )
        }
    }
    
    /**
     * Cancel scheduled receipt print for an order
     */
    fun cancelReceiptPrint(context: Context, orderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReceiptPrintReceiver::class.java).apply {
            action = ACTION_PRINT_RECEIPT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "print_$orderId".hashCode(),
            intent,
            getPendingIntentFlags()
        )
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Calculate print time based on settings
     * 
     * @param dueDate Order due date timestamp
     * @param days Days before due date
     * @param minutes Minutes before due date
     * @return Print timestamp
     */
    fun calculatePrintTime(dueDate: Long, days: Int, minutes: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dueDate
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.add(Calendar.MINUTE, -minutes)
        return calendar.timeInMillis
    }
    
    private fun createPrintIntent(
        context: Context,
        orderId: String,
        merchantId: String
    ): Intent {
        return Intent(context, ReceiptPrintReceiver::class.java).apply {
            action = ACTION_PRINT_RECEIPT
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_MERCHANT_ID, merchantId)
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
 * BroadcastReceiver that handles scheduled receipt printing
 * Prints order receipt to kitchen/order printer via Clover SDK
 */
class ReceiptPrintReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val orderId = intent.getStringExtra(ReceiptPrintScheduler.EXTRA_ORDER_ID) ?: return
        
        // Print in background
        CoroutineScope(Dispatchers.IO).launch {
            printOrderReceipt(context, orderId)
        }
    }
    
    private suspend fun printOrderReceipt(context: Context, orderId: String) {
        try {
            val settingsManager = SettingsManager(context)
            
            // Get print settings to determine which printer to use
            val printToCustomer = settingsManager.getPrintNotesOnCustomerReceipts()
            val printToOrder = settingsManager.getPrintNotesOnOrderReceipts()
            
            val cloverAccount = CloverAccount.getAccount(context) ?: return
            
            // Get order to verify it exists
            val orderConnector = OrderConnector(context, cloverAccount, null)
            orderConnector.connect()
            val order = orderConnector.getOrder(orderId)
            orderConnector.disconnect()
            
            if (order == null) return
            
            // Get printer based on receipt settings preference
            val printerConnector = PrinterConnector(context, cloverAccount, null)
            printerConnector.connect()
            
            var printer: com.clover.sdk.v1.printer.Printer? = null
            
            // Try ORDER printer first if order receipts are enabled
            if (printToOrder) {
                printer = printerConnector.getPrinters(Category.ORDER)?.firstOrNull()
            }
            
            // Fall back to RECEIPT printer if customer receipts are enabled
            if (printer == null && printToCustomer) {
                printer = printerConnector.getPrinters(Category.RECEIPT)?.firstOrNull()
            }
            
            // If no preference set, default to ORDER printer then RECEIPT
            if (printer == null) {
                printer = printerConnector.getPrinters(Category.ORDER)?.firstOrNull()
                    ?: printerConnector.getPrinters(Category.RECEIPT)?.firstOrNull()
            }
            
            if (printer == null) {
                printerConnector.disconnect()
                return
            }
            
            // Create and execute print job
            val printJob = StaticOrderPrintJob.Builder()
                .order(order)
                .build()
            
            printJob.print(context, cloverAccount)
            
            printerConnector.disconnect()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
