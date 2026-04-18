package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unit tests for date formatting utilities used in date pills
 * Issue #11 - Main Header Refinements
 * Issue #12 - Separate Order Date and Due Date Pills
 */
class DateFormattingTest {

    // ==================== Date Display Format Tests ====================

    @Test
    fun `formatDateForPill formats date correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15)
        val date = calendar.time
        
        val result = formatDateForPill(date)
        assertEquals("Mar 15, 2024", result)
    }

    @Test
    fun `formatDateForPill handles single digit day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 5)
        val date = calendar.time
        
        val result = formatDateForPill(date)
        assertEquals("Jan 5, 2024", result)
    }

    @Test
    fun `formatDateForPill handles December`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.DECEMBER, 25)
        val date = calendar.time
        
        val result = formatDateForPill(date)
        assertEquals("Dec 25, 2024", result)
    }

    // ==================== Date Comparison Format Tests ====================

    @Test
    fun `formatDateForComparison creates yyyyMMdd format`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15)
        val date = calendar.time
        
        val result = formatDateForComparison(date)
        assertEquals("20240315", result)
    }

    @Test
    fun `formatDateForComparison pads single digit month`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 15)
        val date = calendar.time
        
        val result = formatDateForComparison(date)
        assertEquals("20240115", result)
    }

    @Test
    fun `formatDateForComparison pads single digit day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 5)
        val date = calendar.time
        
        val result = formatDateForComparison(date)
        assertEquals("20240305", result)
    }

    // ==================== Same Day Comparison Tests ====================

    @Test
    fun `isSameDay returns true for same day`() {
        val calendar1 = Calendar.getInstance()
        calendar1.set(2024, Calendar.MARCH, 15, 10, 30, 0)
        
        val calendar2 = Calendar.getInstance()
        calendar2.set(2024, Calendar.MARCH, 15, 14, 45, 0)
        
        assertTrue(isSameDay(calendar1.time, calendar2.time))
    }

    @Test
    fun `isSameDay returns false for different days`() {
        val calendar1 = Calendar.getInstance()
        calendar1.set(2024, Calendar.MARCH, 15)
        
        val calendar2 = Calendar.getInstance()
        calendar2.set(2024, Calendar.MARCH, 16)
        
        assertFalse(isSameDay(calendar1.time, calendar2.time))
    }

    @Test
    fun `isSameDay returns false for different months`() {
        val calendar1 = Calendar.getInstance()
        calendar1.set(2024, Calendar.MARCH, 15)
        
        val calendar2 = Calendar.getInstance()
        calendar2.set(2024, Calendar.APRIL, 15)
        
        assertFalse(isSameDay(calendar1.time, calendar2.time))
    }

    @Test
    fun `isSameDay returns false for different years`() {
        val calendar1 = Calendar.getInstance()
        calendar1.set(2024, Calendar.MARCH, 15)
        
        val calendar2 = Calendar.getInstance()
        calendar2.set(2025, Calendar.MARCH, 15)
        
        assertFalse(isSameDay(calendar1.time, calendar2.time))
    }

    // ==================== Date Filter Match Tests (#12) ====================

    @Test
    fun `matchesOrderDate returns true when dates match`() {
        val orderCreatedTime = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15, 10, 30, 0)
        }.timeInMillis
        
        val filterDate = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15, 0, 0, 0)
        }.time
        
        assertTrue(matchesOrderDate(orderCreatedTime, filterDate))
    }

    @Test
    fun `matchesOrderDate returns false when dates differ`() {
        val orderCreatedTime = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15, 10, 30, 0)
        }.timeInMillis
        
        val filterDate = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 16, 0, 0, 0)
        }.time
        
        assertFalse(matchesOrderDate(orderCreatedTime, filterDate))
    }

    @Test
    fun `matchesDueDate returns true when date string contains target`() {
        val dueDateValues = setOf("Mar 15, 2024", "Pickup: March 15")
        val filterDate = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.time
        
        assertTrue(matchesDueDate(dueDateValues, filterDate))
    }

    @Test
    fun `matchesDueDate returns false when no match`() {
        val dueDateValues = setOf("Mar 16, 2024", "Pickup: March 16")
        val filterDate = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.time
        
        assertFalse(matchesDueDate(dueDateValues, filterDate))
    }

    @Test
    fun `matchesDueDate returns false for empty values`() {
        val dueDateValues = emptySet<String>()
        val filterDate = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.time
        
        assertFalse(matchesDueDate(dueDateValues, filterDate))
    }

    // ==================== Independent Date Filter Tests (#12) ====================

    @Test
    fun `order date filter works independently of due date`() {
        // Order created on March 15, due on March 20
        val orderCreatedTime = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.timeInMillis
        
        // Filter by order date (March 15) - should match
        val orderDateFilter = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.time
        
        // Filter by due date (March 20) - independent
        val dueDateFilter = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 20)
        }.time
        
        assertTrue(matchesOrderDate(orderCreatedTime, orderDateFilter))
        // Due date filter doesn't affect order date match
        assertTrue(matchesOrderDate(orderCreatedTime, orderDateFilter))
    }

    @Test
    fun `due date filter works independently of order date`() {
        val dueDateValues = setOf("Mar 20, 2024")
        
        // Filter by due date (March 20) - should match
        val dueDateFilter = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 20)
        }.time
        
        assertTrue(matchesDueDate(dueDateValues, dueDateFilter))
        
        // Different due date filter - should not match
        val differentFilter = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }.time
        
        assertFalse(matchesDueDate(dueDateValues, differentFilter))
    }

    // ==================== Helper Functions ====================

    private fun formatDateForPill(date: Date): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun formatDateForComparison(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(date1) == dateFormat.format(date2)
    }

    private fun matchesOrderDate(orderCreatedTime: Long, filterDate: Date): Boolean {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val orderDateStr = dateFormat.format(Date(orderCreatedTime))
        val filterDateStr = dateFormat.format(filterDate)
        return orderDateStr == filterDateStr
    }

    private fun matchesDueDate(dueDateValues: Set<String>, filterDate: Date): Boolean {
        if (dueDateValues.isEmpty()) return false
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val filterDateStr = dateFormat.format(filterDate)
        val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val filterDateDisplay = displayFormat.format(filterDate)
        
        return dueDateValues.any { 
            it.contains(filterDateStr) || it.contains(filterDateDisplay)
        }
    }
}
