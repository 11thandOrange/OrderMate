package com.orderMate.utils

import android.graphics.drawable.GradientDrawable
import com.orderMate.modals.WidgetType

/**
 * Centralized widget color utility for consistent color coding across the app.
 * 
 * Color scheme:
 * - Calendar/Date: Blue (#64B5F6)
 * - Single Select: Purple (#CE93D8)
 * - Multi Select: Green (#81C784)
 * - Text Box: Orange (#FFB74D)
 */
object WidgetColorUtils {
    
    // Widget type colors (full opacity)
    const val COLOR_CALENDAR = 0xFF64B5F6.toInt()    // Blue
    const val COLOR_SINGLE_SELECT = 0xFFCE93D8.toInt() // Purple
    const val COLOR_MULTI_SELECT = 0xFF81C784.toInt()  // Green
    const val COLOR_TEXT_BOX = 0xFFFFB74D.toInt()      // Orange
    
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
     * Matches existing logic in ItemAdapter, OrderCardRedesignAdapter, windowManager
     */
    fun getColorForLabel(label: String): Int {
        val lowerLabel = label.lowercase()
        return when {
            lowerLabel.contains("date") || lowerLabel.contains("pickup") || lowerLabel.contains("calendar") -> COLOR_CALENDAR
            lowerLabel.contains("type") || lowerLabel.contains("status") || lowerLabel.contains("select") -> COLOR_SINGLE_SELECT
            lowerLabel.contains("category") || lowerLabel.contains("tag") -> COLOR_MULTI_SELECT
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
     * Create a rounded background drawable with widget color
     */
    fun createPillBackground(color: Int, cornerRadiusDp: Float, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            setColor(getBackgroundColor(color))
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
