package com.orderMate.utils

import android.graphics.drawable.GradientDrawable
import com.orderMate.R
import com.orderMate.modals.WidgetType

/**
 * Centralized color utility for consistent color coding across the app.
 * ALL COLORS IN ONE PLACE - NO HARDCODING ELSEWHERE.
 * 
 * TWO CATEGORIES ONLY:
 * 
 * 1. Widget Types (Custom Notes - Order Level & Item Level):
 *    - Calendar/Date: Blue (#64B5F6)
 *    - Single Select: Purple (#CE93D8)
 *    - Multi Select: Green (#81C784)
 *    - Text Box: Brown (#A1887F)
 * 
 * 2. Clover Filters (Built-in Order Data):
 *    - Payment Status: Yellow (#FFB74D)
 *    - Order Status: Red (#EF5350)
 *    - Payment Type: Grey (#9E9E9E)
 *    - Employee: Purple (#CE93D8)
 *    - Order Date: Blue (#64B5F6)
 * 
 * PILL STYLING:
 *    - Background: 15% opacity
 *    - Border: 25% opacity, 1dp stroke
 *    - Text: Full color (100%)
 */
object WidgetColorUtils {
    
    // Widget type colors (full opacity)
    const val COLOR_CALENDAR = 0xFF64B5F6.toInt()      // Blue
    const val COLOR_SINGLE_SELECT = 0xFFCE93D8.toInt() // Purple
    const val COLOR_MULTI_SELECT = 0xFF81C784.toInt()  // Green
    const val COLOR_TEXT_BOX = 0xFF212121.toInt()      // Black (for description pills)
    const val COLOR_TEXT_BOX_TEXT = 0xFF9E9E9E.toInt() // Light grey text for description pills
    
    // Clover filter colors
    const val COLOR_PAYMENT_STATUS = 0xFFFFB74D.toInt()  // Yellow
    const val COLOR_ORDER_STATUS = 0xFFEF5350.toInt()    // Red
    const val COLOR_PAYMENT_TYPE = 0xFF9E9E9E.toInt()    // Grey
    const val COLOR_EMPLOYEE = 0xFFCE93D8.toInt()        // Purple (same as Single Select)
    const val COLOR_ORDER_DATE = 0xFF64B5F6.toInt()      // Blue (same as Calendar)
    
    /**
     * Get color for a widget type (CALENDAR, SINGLE_SELECT, MULTI_SELECT, TEXT_BOX)
     */
    fun getColorForWidgetType(type: WidgetType): Int {
        return when (type) {
            WidgetType.CALENDAR -> COLOR_CALENDAR
            WidgetType.SINGLE_SELECT -> COLOR_SINGLE_SELECT
            WidgetType.MULTI_SELECT -> COLOR_MULTI_SELECT
            WidgetType.TEXT_BOX -> COLOR_TEXT_BOX
        }
    }
    
    /**
     * Get text color for a widget type pill
     * TEXT_BOX uses light grey text, others use the widget color
     */
    fun getTextColorForWidgetType(type: WidgetType): Int {
        return when (type) {
            WidgetType.TEXT_BOX -> COLOR_TEXT_BOX_TEXT // Light grey for description
            else -> getColorForWidgetType(type)
        }
    }
    
    /**
     * Get icon resource for a widget type - centralized to avoid duplicate functions
     */
    fun getIconForWidgetType(type: WidgetType): Int {
        return when (type) {
            WidgetType.CALENDAR -> R.drawable.ic_calendar
            WidgetType.SINGLE_SELECT -> R.drawable.ic_check_box
            WidgetType.MULTI_SELECT -> R.drawable.ic_label
            WidgetType.TEXT_BOX -> R.drawable.ic_edit
        }
    }
    
    /**
     * Get color for Clover filter type
     */
    fun getColorForCloverFilter(filterType: String): Int {
        return when (filterType.lowercase()) {
            "payment_status", "paymentstatus" -> COLOR_PAYMENT_STATUS
            "order_status", "orderstatus" -> COLOR_ORDER_STATUS
            "payment_type", "paymenttype" -> COLOR_PAYMENT_TYPE
            "employee" -> COLOR_EMPLOYEE
            "order_date", "orderdate" -> COLOR_ORDER_DATE
            else -> COLOR_ORDER_STATUS // Default
        }
    }
    
    /**
     * Get background color with 15% opacity for pills
     */
    fun getBackgroundColor(color: Int): Int {
        return (color and 0x00FFFFFF) or 0x26000000
    }
    
    /**
     * Get border color with 25% opacity for pills
     */
    fun getBorderColor(color: Int): Int {
        return (color and 0x00FFFFFF) or 0x40000000
    }
    
    /**
     * Create a unified pill background drawable with 15% opacity bg + 25% border
     * This is the SINGLE source of truth for all pill styling across the app
     */
    fun createPillBackground(color: Int, cornerRadiusDp: Float, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            setColor(getBackgroundColor(color))
            setStroke((1 * density).toInt(), getBorderColor(color))
        }
    }
    
    /**
     * Create a chip background drawable for selected state
     */
    fun createChipBackground(color: Int, isSelected: Boolean, cornerRadiusDp: Float, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            if (isSelected) {
                setColor(getBackgroundColor(color))
                setStroke((1 * density).toInt(), color)
            } else {
                setColor(0x1AFFFFFF) // 10% white
                setStroke((1 * density).toInt(), 0x33FFFFFF) // 20% white border
            }
        }
    }
}
