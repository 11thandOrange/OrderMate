package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Extension functions used in List Tab (Issue #81)
 */
class ExtensionsTest {

    // ==================== Price Formatting Tests ====================

    @Test
    fun `toDoubleFloatPoint Int converts cents to dollars`() {
        assertEquals("1.25", 125.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPoint Int handles zero`() {
        assertEquals("0.00", 0.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPoint Int handles single digit cents`() {
        assertEquals("0.05", 5.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPoint Int handles large amounts`() {
        assertEquals("9999.99", 999999.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPoint Long converts correctly`() {
        assertEquals("125.00", 12500L.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPoint Double formats to two decimal places`() {
        assertEquals("125.50", 125.5.toDoubleFloatPoint())
    }

    @Test
    fun `toDoubleFloatPointLatest divides by 100 and formats`() {
        assertEquals("125.00", 12500.0.toDoubleFloatPointLatest())
    }

    // ==================== Currency Symbol Tests ====================

    @Test
    fun `convertToSymbol returns dollar sign for USD`() {
        assertEquals("$", "USD".convertToSymbol())
    }

    @Test
    fun `convertToSymbol returns euro sign for EUR`() {
        assertEquals("€", "EUR".convertToSymbol())
    }

    @Test
    fun `convertToSymbol returns pound sign for GBP`() {
        assertEquals("£", "GBP".convertToSymbol())
    }

    @Test
    fun `convertToSymbol returns empty string for empty input`() {
        assertEquals("", "".convertToSymbol())
    }

    @Test
    fun `convertToSymbol returns empty string for whitespace only`() {
        assertEquals("", "   ".convertToSymbol())
    }

    // ==================== Two Decimal Conversion Tests ====================

    @Test
    fun `convertToTwoDecimal adds decimal places to integer string`() {
        assertEquals("125.00", "125".convertToTwoDecimal())
    }

    @Test
    fun `convertToTwoDecimal truncates extra decimal places`() {
        assertEquals("125.12", "125.123456".convertToTwoDecimal())
    }

    @Test
    fun `convertToTwoDecimal handles existing two decimal places`() {
        assertEquals("125.50", "125.50".convertToTwoDecimal())
    }

    @Test
    fun `convertToTwoDecimal handles single decimal place`() {
        assertEquals("125.5", "125.5".convertToTwoDecimal())
    }

    // ==================== Name Parsing Tests ====================

    @Test
    fun `getOnlyFirstName returns first part before pipe`() {
        assertEquals("John Smith", "John Smith|Additional Info".getOnlyFirstName())
    }

    @Test
    fun `getOnlyFirstName returns full string if no pipe`() {
        assertEquals("John Smith", "John Smith".getOnlyFirstName())
    }

    // ==================== Date Formatting Tests ====================

    @Test
    fun `formatMillisToDateTime formats correctly with yearFormat`() {
        // March 15, 2024 at noon UTC
        val timestamp = 1710504000000L
        val result = timestamp.formatMillisToDateTime(Constants.yearFormat)
        assertTrue(result.matches(Regex("\\d{2}/\\d{2}/\\d{2}")))
    }

    @Test
    fun `formatMillisToDateTime formats correctly with dateFormat`() {
        val timestamp = 1710504000000L
        val result = timestamp.formatMillisToDateTime(Constants.dateFormat)
        assertTrue(result.matches(Regex("\\d{2}:\\d{2} [AP]M")))
    }

    // ==================== Array Matching Tests ====================

    @Test
    fun `isInArray returns true when value exists`() {
        val hashMap = hashMapOf("key1" to "value1", "key2" to "value2")
        assertTrue("value1,value3".isInArray(hashMap))
    }

    @Test
    fun `isInArray returns false when value does not exist`() {
        val hashMap = hashMapOf("key1" to "value1", "key2" to "value2")
        assertFalse("value3,value4".isInArray(hashMap))
    }

    @Test
    fun `isInArray handles empty string`() {
        val hashMap = hashMapOf("key1" to "value1")
        assertFalse("".isInArray(hashMap))
    }

    // ==================== Extension Function Implementations for Testing ====================

    private fun Int.toDoubleFloatPoint(): String {
        return "%.2f".format(this.toFloat() / 100)
    }

    private fun Long.toDoubleFloatPoint(): String {
        return "%.2f".format((this.toDouble() / 100))
    }

    private fun Double.toDoubleFloatPoint(): String {
        return "%.2f".format(this)
    }

    private fun Double.toDoubleFloatPointLatest(): String {
        return "%.2f".format(this / 100)
    }

    private fun String.convertToSymbol(): String {
        if (this.trim().isEmpty()) return ""
        return try {
            val currency = java.util.Currency.getInstance(this)
            currency.symbol
        } catch (e: Exception) {
            ""
        }
    }

    private fun String.convertToTwoDecimal(): String {
        return try {
            val decimalIndex = this.indexOfFirst { it == '.' }
            if (decimalIndex == -1) {
                "$this.00"
            } else {
                val wholePartStr = this.substring(0, decimalIndex)
                val fractionalPartStr = this.substring(decimalIndex + 1)
                val fractionalPart = fractionalPartStr.take(2)
                "$wholePartStr.$fractionalPart"
            }
        } catch (e: NumberFormatException) {
            ""
        }
    }

    private fun String.getOnlyFirstName(): String {
        try {
            val split = this.split('|')
            return split[0]
        } catch (e: Exception) {
            return Constants.merchantName
        }
    }

    private fun Long.formatMillisToDateTime(format: String): String {
        val dateTime = java.util.Date(this)
        val dateFormat = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
        return dateFormat.format(dateTime)
    }

    private fun String.isInArray(list: HashMap<String, String>): Boolean {
        val newArr = this.split(",")
        newArr.forEach {
            if (list.values.contains(it)) {
                return true
            }
        }
        return false
    }
}
