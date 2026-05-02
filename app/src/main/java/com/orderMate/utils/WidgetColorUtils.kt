package com.orderMate.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
 *    - Dark container background (#CC292D3E) - same as filter modal
 *    - Colored overlay: 15% opacity on top
 *    - Border: 25% opacity, 1dp stroke
 *    - Text: Full color (100%)
 */
object WidgetColorUtils {
    
    // Consistent pill text truncation length (#77)
    const val PILL_TRUNCATE_LENGTH = 20
    
    // Dark container background color (matches filter modal bg_dialog.xml)
    const val COLOR_PILL_CONTAINER = 0xCC292D3E.toInt()
    
    // Widget type colors (full opacity)
    const val COLOR_CALENDAR = 0xFF64B5F6.toInt()      // Blue
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
     * Get icon resource for a widget type - centralized to avoid duplicate functions
     * (#77) Single Select = checkmark, Multi Select = double checkmark, Text Box = 'A' icon
     */
    fun getIconForWidgetType(type: WidgetType): Int {
        return when (type) {
            WidgetType.CALENDAR -> R.drawable.ic_calendar
            WidgetType.SINGLE_SELECT -> R.drawable.ic_check_box
            WidgetType.MULTI_SELECT -> R.drawable.ic_check_double
            WidgetType.TEXT_BOX -> R.drawable.ic_text_format
        }
    }
    
    /**
     * Get icon resource for utils WidgetType (used in SettingsFragment)
     * Overload for compatibility with com.orderMate.utils.WidgetType
     */
    fun getIconForWidgetType(type: com.orderMate.utils.WidgetType): Int {
        return when (type) {
            com.orderMate.utils.WidgetType.CALENDAR -> R.drawable.ic_calendar
            com.orderMate.utils.WidgetType.SINGLE_SELECT -> R.drawable.ic_check_box
            com.orderMate.utils.WidgetType.MULTI_SELECT -> R.drawable.ic_check_double
            com.orderMate.utils.WidgetType.TEXT_BOX -> R.drawable.ic_text_format
        }
    }
    
    /**
     * Get color for utils WidgetType (used in SettingsFragment)
     * Overload for compatibility with com.orderMate.utils.WidgetType
     */
    fun getColorForWidgetType(type: com.orderMate.utils.WidgetType): Int {
        return when (type) {
            com.orderMate.utils.WidgetType.CALENDAR -> COLOR_CALENDAR
            com.orderMate.utils.WidgetType.SINGLE_SELECT -> COLOR_SINGLE_SELECT
            com.orderMate.utils.WidgetType.MULTI_SELECT -> COLOR_MULTI_SELECT
            com.orderMate.utils.WidgetType.TEXT_BOX -> COLOR_TEXT_BOX
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
     * Create a unified pill background drawable with dark container + colored overlay
     * This is the SINGLE source of truth for all pill styling across the app
     * 
     * Layers (bottom to top):
     * 1. Dark container (#CC292D3E) - matches filter modal background
     * 2. Colored overlay (15% opacity of pill color)
     * 3. Border (25% opacity of pill color)
     */
    fun createPillBackground(color: Int, cornerRadiusDp: Float, density: Float): LayerDrawable {
        val cornerRadius = cornerRadiusDp * density
        
        // Bottom layer: dark container background
        val containerDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(COLOR_PILL_CONTAINER)
        }
        
        // Top layer: colored overlay with border
        val colorOverlayDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(getBackgroundColor(color))
            setStroke((1 * density).toInt(), getBorderColor(color))
        }
        
        return LayerDrawable(arrayOf(containerDrawable, colorOverlayDrawable))
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
    
    /**
     * Truncate text for pill display with consistent length (#77)
     * Removes newlines and truncates to PILL_TRUNCATE_LENGTH chars with ellipsis
     */
    fun truncateForPill(text: String, maxLength: Int = PILL_TRUNCATE_LENGTH): String {
        val cleaned = text.replace("\n", " ")
        return if (cleaned.length > maxLength) {
            cleaned.take(maxLength) + "..."
        } else {
            cleaned
        }
    }
    
    /**
     * Create and style a pill view with consistent styling across the app.
     * This is the SINGLE source of truth for all pill rendering.
     * 
     * @param context Context for inflation
     * @param container Parent container to attach pill to (not added automatically)
     * @param text Text to display (will be truncated)
     * @param widgetType Widget type for color and icon
     * @param cornerRadiusDp Corner radius in dp (default 10f)
     * @param truncate Whether to truncate text (default true)
     * @return Styled LinearLayout pill view (not attached to container)
     */
    fun createPillView(
        context: Context,
        container: ViewGroup,
        text: String,
        widgetType: WidgetType,
        cornerRadiusDp: Float = 10f,
        truncate: Boolean = true
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        val color = getColorForWidgetType(widgetType)
        val iconRes = getIconForWidgetType(widgetType)
        
        val pillView = LayoutInflater.from(context)
            .inflate(R.layout.item_note_pill, container, false) as LinearLayout
        
        val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
        val pillText = pillView.findViewById<TextView>(R.id.pillText)
        
        pillText.text = if (truncate) truncateForPill(text) else text
        pillText.maxLines = 1
        pillText.setTextColor(color)
        
        pillIcon.setImageResource(iconRes)
        pillIcon.setColorFilter(color)
        
        pillView.background = createPillBackground(color, cornerRadiusDp, density)
        
        return pillView
    }
    
    /**
     * Create and add a pill view to a container.
     * Convenience method that creates and immediately adds the pill.
     */
    fun addPillToContainer(
        context: Context,
        container: ViewGroup,
        text: String,
        widgetType: WidgetType,
        cornerRadiusDp: Float = 10f,
        truncate: Boolean = true
    ) {
        val pillView = createPillView(context, container, text, widgetType, cornerRadiusDp, truncate)
        container.addView(pillView)
    }
}
