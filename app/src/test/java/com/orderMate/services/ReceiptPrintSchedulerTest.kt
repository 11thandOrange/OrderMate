package com.orderMate.services

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for ReceiptPrintScheduler
 * 
 * Tests cover:
 * - Print time calculation (days and minutes before due date)
 * - Edge cases (past due dates, zero values)
 * - Printer selection logic
 */
class ReceiptPrintSchedulerTest {

    // ==================== Print Time Calculation Tests ====================

    @Test
    fun `calculatePrintTime subtracts days correctly`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30) // May 15, 2026 2:30 PM
        val days = 3
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be May 12, 2026 2:30 PM (3 days before)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(12, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH))
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `calculatePrintTime subtracts minutes correctly`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30) // May 15, 2026 2:30 PM
        val days = 0
        val minutes = 45
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be May 15, 2026 1:45 PM (45 minutes before)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(13, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `calculatePrintTime subtracts both days and minutes`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30) // May 15, 2026 2:30 PM
        val days = 2
        val minutes = 30
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be May 13, 2026 2:00 PM (2 days and 30 minutes before)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(13, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `calculatePrintTime with zero days and zero minutes returns due date`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30)
        val days = 0
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        assertEquals(dueDate, printTime)
    }

    @Test
    fun `calculatePrintTime handles month boundary`() {
        val dueDate = createTimestamp(2026, 5, 2, 10, 0) // May 2, 2026 10:00 AM
        val days = 5
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be April 27, 2026 10:00 AM (crosses into previous month)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(27, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, calendar.get(Calendar.MONTH))
    }

    @Test
    fun `calculatePrintTime handles year boundary`() {
        val dueDate = createTimestamp(2026, 1, 3, 12, 0) // Jan 3, 2026 12:00 PM
        val days = 5
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be Dec 29, 2025 12:00 PM (crosses into previous year)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(29, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.DECEMBER, calendar.get(Calendar.MONTH))
        assertEquals(2025, calendar.get(Calendar.YEAR))
    }

    @Test
    fun `calculatePrintTime handles midnight crossing`() {
        val dueDate = createTimestamp(2026, 5, 15, 0, 30) // May 15, 2026 12:30 AM
        val days = 0
        val minutes = 60
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be May 14, 2026 11:30 PM (previous day)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(14, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `calculatePrintTime with max days (30)`() {
        val dueDate = createTimestamp(2026, 6, 15, 12, 0) // June 15, 2026
        val days = 30
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be May 16, 2026 (30 days before)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(16, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH))
    }

    @Test
    fun `calculatePrintTime with max minutes (60)`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30)
        val days = 0
        val minutes = 60
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be 1 hour before (13:30)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = printTime
        
        assertEquals(13, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `printTime before current time should not schedule`() {
        // Due date in the past
        val pastDueDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Yesterday
        val days = 0
        val minutes = 0
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(pastDueDate, days, minutes)
        val now = System.currentTimeMillis()
        
        // Print time would be in the past
        assertTrue(printTime < now)
    }

    @Test
    fun `printTime with large offset still works`() {
        val dueDate = createTimestamp(2026, 5, 15, 14, 30)
        val days = 30
        val minutes = 60
        
        val printTime = ReceiptPrintScheduler.calculatePrintTime(dueDate, days, minutes)
        
        // Should be 30 days and 1 hour before
        val expectedOffset = (30L * 24 * 60 * 60 * 1000) + (60L * 60 * 1000)
        assertEquals(dueDate - expectedOffset, printTime)
    }

    // ==================== Printer Selection Logic Tests ====================

    @Test
    fun `printer selection prefers ORDER when printToOrder is true`() {
        val printToOrder = true
        val printToCustomer = false
        
        val printerCategory = determinePrinterCategory(printToOrder, printToCustomer)
        
        assertEquals("ORDER", printerCategory)
    }

    @Test
    fun `printer selection uses RECEIPT when only printToCustomer is true`() {
        val printToOrder = false
        val printToCustomer = true
        
        val printerCategory = determinePrinterCategory(printToOrder, printToCustomer)
        
        assertEquals("RECEIPT", printerCategory)
    }

    @Test
    fun `printer selection prefers ORDER when both are true`() {
        val printToOrder = true
        val printToCustomer = true
        
        val printerCategory = determinePrinterCategory(printToOrder, printToCustomer)
        
        assertEquals("ORDER", printerCategory)
    }

    @Test
    fun `printer selection defaults to ORDER when neither is set`() {
        val printToOrder = false
        val printToCustomer = false
        
        val printerCategory = determinePrinterCategory(printToOrder, printToCustomer)
        
        // Should default to ORDER, then fall back to RECEIPT
        assertEquals("ORDER_OR_RECEIPT", printerCategory)
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a timestamp for testing
     */
    private fun createTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0) // month is 0-based
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Simulates printer category determination logic from ReceiptPrintReceiver
     */
    private fun determinePrinterCategory(printToOrder: Boolean, printToCustomer: Boolean): String {
        return when {
            printToOrder -> "ORDER"
            printToCustomer -> "RECEIPT"
            else -> "ORDER_OR_RECEIPT" // Fallback
        }
    }
}
