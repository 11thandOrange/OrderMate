package com.orderMate.utils

import android.graphics.drawable.GradientDrawable
import com.orderMate.modals.WidgetType

/**
 * Centralized widget color utility for consistent color coding across the app.
 * 
 * Widget Color Scheme:
 * - Calendar/Date: Blue (#64B5F6)
 * - Single Select: Purple (#CE93D8)
 * - Multi Select: Green (#81C784)
 * - Text Box: Brown (#A1887F)
 * 
 * Clover Filter Color Scheme:
 * - Payment Status: Yellow (#FFB74D)
 * - Order Status: Red (#EF5350)
 * - Payment Type: Grey (#9E9E9E)
 * - Employee: Purple (#CE93D8)
 * - Order Date: Blue (#64B5F6)
 */
object WidgetColorUtils {
    
    // Widget type colors (full opacity)
    const val COLOR_CALENDAR = 0xFF64B5F6.toInt()    // Blue
    const val COLOR_SINGLE_SELECT = 0xFFCE93D8.toInt() // Purple
    const val COLOR_MULTI_SELECT = 0xFF81C784.toInt()  // Green
    const val COLOR_TEXT_BOX = 0xFFA1887F.toInt()      // Brown
    
    // Clover filter colors
    const val COLOR_PAYMENT_STATUS = 0xFFFFB74D.toInt()  // Yellow
    const val COLOR_ORDER_STATUS = 0xFFEF5350.toInt()    // Red
    const val COLOR_PAYMENT_TYPE = 0xFF9E9E9E.toInt()    // Grey
    const val COLOR_EMPLOYEE = 0xFFCE93D8.toInt()        // Purple (same as Single Select)
    const val COLOR_ORDER_DATE = 0xFF64B5F6.toInt()      // Blue (same as Calendar)
    
    /**
     * Get color for a widget type
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
     * Get color for a label string (used when parsing notes)
     * Priority: Calendar > Multi-Select > Single-Select > Text
     * 
     * Note: This is a fallback for when widgetType is not available.
     * Always prefer getColorForWidgetType() when WidgetType is known.
     */
    fun getColorForLabel(label: String): Int {
        val lowerLabel = label.lowercase()
        return when {
            // Calendar keywords
            lowerLabel.contains("date") || lowerLabel.contains("pickup") || lowerLabel.contains("calendar") || lowerLabel.contains("due") -> COLOR_CALENDAR
            // Multi-select keywords (check BEFORE single-select to avoid "category" matching "type")
            lowerLabel.contains("category") || lowerLabel.contains("categories") || lowerLabel.contains("tags") -> COLOR_MULTI_SELECT
            // Single-select keywords
            lowerLabel.contains("type") || lowerLabel.contains("status") || lowerLabel.contains("select") -> COLOR_SINGLE_SELECT
            else -> COLOR_TEXT_BOX
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
