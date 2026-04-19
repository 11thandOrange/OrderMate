package com.orderMate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderCardRedesignBinding
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.utils.Constants
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.formatPaymentState
import com.orderMate.utils.toDoubleFloatPoint

/**
 * iOS-style Order Card Adapter (#80, #81 requirement)
 * 
 * Displays orders as glassmorphic cards with:
 * - Status badges (order status + payment status)
 * - Customer & employee info
 * - Payment type with icon
 * - Custom notes as pills
 * - Unpaid indicator (red left border)
 */
class OrderCardRedesignAdapter(
    private val orders: ArrayList<Order?>,
    private val listener: IOrderItemClickListener
) : RecyclerView.Adapter<OrderCardRedesignAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderCardRedesignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        orders.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun getItemCount(): Int = orders.size

    inner class ViewHolder(
        private val binding: ItemOrderCardRedesignBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listener on the card root (LinearLayout)
            binding.cardRoot.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onOrderItemClick(adapterPosition, null)
                }
            }
        }

        fun bind(order: Order) {
            val context = binding.root.context

            // Order Number
            binding.orderNumber.text = "#${order.id?.takeLast(8) ?: "---"}"

            // Order Status Badge
            setupOrderStatusBadge(order)

            // Payment Status Badge
            setupPaymentStatusBadge(order)

            // Order Date (Task 19)
            binding.orderDate.text = getOrderDate(order)

            // Customer Name
            binding.customerName.text = getCustomerName(order)

            // Employee Name
            binding.employeeName.text = getEmployeeName(order)

            // Payment Type
            setupPaymentType(order)

            // Total
            binding.orderTotal.text = formatTotal(order)

            // Order Description (#18)
            setupOrderDescription(order)

            // (#18, #19) Custom Order Tags - all order-level tags as color-coded pills
            setupCustomTags(order)
            
            // Item-level Notes Pills (in notes section)
            setupNotesPills(order)

            // Left Status Indicator (green for paid, red for unpaid)
            val paymentState = order.paymentState?.name ?: "NOT_PAID"
            val indicatorColor = when (paymentState) {
                "PAID" -> ContextCompat.getColor(context, R.color.paid_status_color)
                "NOT_PAID" -> ContextCompat.getColor(context, R.color.unpaid_status_color)
                "PARTIALLY_PAID" -> ContextCompat.getColor(context, R.color.orange_accent)
                "REFUNDED" -> ContextCompat.getColor(context, R.color.orange_accent)
                else -> ContextCompat.getColor(context, R.color.paid_status_color)
            }
            binding.unpaidIndicator.setBackgroundColor(indicatorColor)
            binding.unpaidIndicator.visibility = View.VISIBLE
        }

        /**
         * (#18) Setup order description from line item notes
         * Extracts "description:" field and displays it
         */
        private fun setupOrderDescription(order: Order) {
            val description = getOrderDescription(order)
            if (description.isNullOrBlank()) {
                binding.orderDescription.visibility = View.GONE
            } else {
                binding.orderDescription.text = description
                binding.orderDescription.visibility = View.VISIBLE
            }
        }

        /**
         * (#18) Extract order-level description from order.note
         * Uses widget-based parsing for ORDER-level TEXT widgets.
         * Falls back to legacy parsing if no widgets configured.
         */
        private fun getOrderDescription(order: Order): String? {
            val orderNote = order.note
            if (orderNote.isNullOrBlank()) return null
            
            // (#18) Use widget-based parsing for ORDER-level TEXT widgets
            val widgets = WidgetManager.getCachedWidgets()
            if (widgets.isNotEmpty()) {
                val textWidgets = widgets.filter { 
                    it.level == NoteLevel.ORDER && it.type == com.orderMate.modals.WidgetType.TEXT_BOX
                }
                for (widget in textWidgets) {
                    val label = widget.label
                    if (label.lowercase() == "description") {
                        val tags = OrderNoteParser.extractTagsFromNote(orderNote, listOf(widget), NoteLevel.ORDER)
                        tags.firstOrNull()?.let { return it.value }
                    }
                }
            }
            
            // Legacy fallback: parse "description:" from order.note
            val delimiter = if (orderNote.contains("•")) "•" else "|"
            val parts = orderNote.split(delimiter).map { it.trim() }
            
            for (part in parts) {
                val colonIndex = part.indexOf(':')
                if (colonIndex > 0) {
                    val label = part.substring(0, colonIndex).trim().lowercase()
                    if (label == "description") {
                        val value = part.substring(colonIndex + 1).trim()
                        if (value.isNotBlank()) {
                            return value
                        }
                    }
                }
            }
            return null
        }

        /**
         * (#19) Setup custom order tags from line item notes
         * Task 15: Uses WidgetColorUtils for consistent color coding across app
         * Task 20: Includes CALENDAR type widgets
         */
        private fun setupCustomTags(order: Order) {
            val context = binding.root.context
            val tagsContainer = binding.tagsContainer
            val density = context.resources.displayMetrics.density
            
            // Remove any previously added custom tags (keep first 2 - order status and payment status)
            while (tagsContainer.childCount > 2) {
                tagsContainer.removeViewAt(2)
            }
            
            val customTags = getCustomTags(order)
            if (customTags.isEmpty()) return
            
            customTags.forEach { tag ->
                val tagView = android.widget.TextView(context).apply {
                    text = tag.value
                    textSize = 11f
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    
                    // Task 15: Use WidgetColorUtils for consistent colors based on widget type
                    val tagColor = if (tag.widgetType != null) {
                        WidgetColorUtils.getColorForWidgetType(tag.widgetType)
                    } else {
                        WidgetColorUtils.getColorForLabel(tag.type)
                    }
                    
                    // Use widget color for text, slightly dimmed background
                    setTextColor(tagColor)
                    
                    // Create rounded background with 15% opacity of the tag color
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(WidgetColorUtils.getBackgroundColor(tagColor))
                        setStroke((1 * density).toInt(), (tagColor and 0x00FFFFFF) or 0x40000000)
                    }
                    background = bgDrawable
                    
                    // Padding and margin
                    val paddingH = (12 * density).toInt()
                    val paddingV = (4 * density).toInt()
                    setPadding(paddingH, paddingV, paddingH, paddingV)
                    
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = (8 * density).toInt()
                    }
                    layoutParams = params
                }
                
                tagsContainer.addView(tagView)
            }
        }

        /**
         * (#19) Extract custom tags from order.note using widget-based parsing.
         * Task 20: Reads ORDER-level SINGLE_SELECT, MULTI_SELECT and CALENDAR widgets.
         * Returns list of tag type, value, and widget type for color coding.
         */
        private fun getCustomTags(order: Order): List<CustomTag> {
            val orderNote = order.note
            if (orderNote.isNullOrBlank()) return emptyList()
            
            val tags = mutableListOf<CustomTag>()
            val seenValues = mutableSetOf<String>()
            
            // Task 20: Use widget-based parsing for ORDER-level SELECT and CALENDAR widgets
            val widgets = WidgetManager.getCachedWidgets()
            if (widgets.isNotEmpty()) {
                val displayWidgets = widgets.filter { 
                    it.level == NoteLevel.ORDER && 
                    (it.type == com.orderMate.modals.WidgetType.SINGLE_SELECT || 
                     it.type == com.orderMate.modals.WidgetType.MULTI_SELECT ||
                     it.type == com.orderMate.modals.WidgetType.CALENDAR)
                }
                
                val parsedTags = OrderNoteParser.extractTagsFromNote(orderNote, displayWidgets, NoteLevel.ORDER)
                parsedTags.forEach { tag ->
                    val uniqueKey = "${tag.label.lowercase()}:${tag.value}"
                    if (!seenValues.contains(uniqueKey)) {
                        seenValues.add(uniqueKey)
                        // Task 15: Store widget type for color coding
                        tags.add(CustomTag(tag.label.lowercase(), tag.value, tag.widgetType))
                    }
                }
                
                if (tags.isNotEmpty()) {
                    return tags.take(3)
                }
            }
            
            // Legacy fallback: parse from order.note directly
            val tagLabels = setOf("category", "type", "status", "sub-category", "subcategory")
            val delimiter = if (orderNote.contains("•")) "•" else "|"
            val parts = orderNote.split(delimiter).map { it.trim() }
            
            for (part in parts) {
                val colonIndex = part.indexOf(':')
                if (colonIndex > 0) {
                    val label = part.substring(0, colonIndex).trim().lowercase()
                    if (tagLabels.contains(label)) {
                        val rawValue = part.substring(colonIndex + 1).trim()
                        
                        // Handle comma-separated values for multi-select
                        val values = rawValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        values.forEach { value ->
                            val uniqueKey = "$label:$value"
                            if (!seenValues.contains(uniqueKey)) {
                                seenValues.add(uniqueKey)
                                tags.add(CustomTag(label, value, null))
                            }
                        }
                    }
                }
            }
            
            // Limit to 3 tags to avoid overflow
            return tags.take(3)
        }

        /**
         * (#19) Get background and text colors for a tag type
         */
        private fun getTagColors(tagType: String): Pair<Int, Int> {
            return when (tagType) {
                "category", "sub-category", "subcategory" -> 
                    Pair(R.color.tag_category_bg, R.color.tag_category_text)
                "type" -> 
                    Pair(R.color.tag_type_bg, R.color.tag_type_text)
                "status" -> 
                    Pair(R.color.tag_status_bg, R.color.tag_status_text)
                else -> 
                    Pair(R.color.tag_default_bg, R.color.tag_default_text)
            }
        }

        private fun Float.dpToPx(): Float {
            return this * binding.root.context.resources.displayMetrics.density
        }

        private fun setupOrderStatusBadge(order: Order) {
            val context = binding.root.context
            val state = order.state?.toString() ?: "OPEN"
            val displayText = formatPaymentState(state)
            
            val (bgRes, textColorRes) = when (state.uppercase()) {
                "LOCKED" -> Pair(R.drawable.bg_badge_closed, R.color.closed_status_color)
                else -> Pair(R.drawable.bg_badge_open, R.color.open_status_color)
            }
            
            binding.orderStatusBadge.text = displayText
            binding.orderStatusBadge.setBackgroundResource(bgRes)
            binding.orderStatusBadge.setTextColor(ContextCompat.getColor(context, textColorRes))
        }

        private fun setupPaymentStatusBadge(order: Order) {
            val context = binding.root.context
            val paymentState = order.paymentState?.name ?: "NOT_PAID"
            val displayText = formatPaymentState(paymentState)

            val (bgRes, textColorRes) = when (paymentState.uppercase()) {
                "PAID" -> Pair(R.drawable.bg_badge_paid, R.color.paid_status_color)
                "NOT_PAID" -> Pair(R.drawable.bg_badge_unpaid, R.color.unpaid_status_color)
                "PARTIALLY_PAID" -> Pair(R.drawable.bg_badge_unpaid, R.color.orange_accent)
                "REFUNDED", "PARTIALLY_REFUNDED" -> Pair(R.drawable.bg_badge_closed, R.color.closed_status_color)
                else -> Pair(R.drawable.bg_badge_open, R.color.text_secondary)
            }

            binding.paymentStatusBadge.text = displayText
            binding.paymentStatusBadge.setBackgroundResource(bgRes)
            binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, textColorRes))
        }

        private fun getCustomerName(order: Order): String {
            return try {
                val customer = order.customers?.firstOrNull()
                val fullName = if (customer != null) {
                    "${customer.firstName ?: ""} ${customer.lastName ?: ""}".trim()
                        .ifEmpty { "-" }
                } else {
                    "-"
                }
                // Task 16: Truncate customer name at 20 chars
                if (fullName.length > 20) {
                    fullName.take(20) + "…"
                } else {
                    fullName
                }
            } catch (e: Exception) {
                "-"
            }
        }

        private fun getEmployeeName(order: Order): String {
            return try {
                order.employee?.jsonObject?.get(Constants.name)?.toString() ?: "-"
            } catch (e: Exception) {
                "-"
            }
        }

        // Task 19: Get order date formatted
        private fun getOrderDate(order: Order): String {
            return try {
                val createdTime = order.createdTime
                if (createdTime != null) {
                    val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                    dateFormat.format(java.util.Date(createdTime))
                } else {
                    "-"
                }
            } catch (e: Exception) {
                "-"
            }
        }

        private fun setupPaymentType(order: Order) {
            val context = binding.root.context
            val payments = order.payments

            if (payments.isNullOrEmpty()) {
                binding.paymentType.text = "-"
                binding.paymentIcon.setImageResource(R.drawable.ic_credit_card)
                return
            }

            val tenderLabel = payments.firstOrNull()?.tender?.label?.lowercase() ?: ""
            
            when {
                tenderLabel.contains("cash") -> {
                    binding.paymentType.text = "Cash"
                    binding.paymentIcon.setImageResource(R.drawable.ic_cash)
                }
                tenderLabel.contains("card") || tenderLabel.contains("credit") || tenderLabel.contains("debit") -> {
                    binding.paymentType.text = "Card"
                    binding.paymentIcon.setImageResource(R.drawable.ic_credit_card)
                }
                else -> {
                    binding.paymentType.text = payments.firstOrNull()?.tender?.label ?: "-"
                    binding.paymentIcon.setImageResource(R.drawable.ic_credit_card)
                }
            }
        }

        private fun formatTotal(order: Order): String {
            return try {
                val total = (order.total ?: 0L) / 100.0
                "$${total.toDoubleFloatPoint()}"
            } catch (e: Exception) {
                "$0.00"
            }
        }

        private fun setupNotesPills(order: Order) {
            val notes = mutableListOf<NoteItem>()
            
            // Extract notes from line items
            order.lineItems?.forEach { lineItem ->
                lineItem?.note?.let { note ->
                    if (note.isNotBlank()) {
                        parseNotes(note, notes)
                    }
                }
            }

            val container = binding.notesChipsContainer
            
            if (notes.isEmpty()) {
                binding.notesSection.visibility = View.GONE
                container.removeAllViews()
                return
            }

            binding.notesSection.visibility = View.VISIBLE
            container.removeAllViews()
            
            val context = binding.root.context
            notes.forEach { noteItem ->
                val pillView = LayoutInflater.from(context)
                    .inflate(R.layout.item_note_pill, container, false) as LinearLayout
                
                val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                val pillText = pillView.findViewById<TextView>(R.id.pillText)
                
                // Light bg with dark text, icon with widget color
                val iconRes = getIconForLabel(noteItem.label)
                val iconColor = getColorForLabel(noteItem.label)
                
                // Truncate to 12 chars, single line, no newlines
                val displayText = noteItem.text.replace("\n", " ").take(12).let {
                    if (noteItem.text.length > 12) "$it..." else it
                }
                pillText.text = displayText
                pillText.maxLines = 1
                pillText.setTextColor(ContextCompat.getColor(context, R.color.list_chip_text))
                pillIcon.setImageResource(iconRes)
                pillIcon.setColorFilter(iconColor)
                
                // Set solid background color
                val bg = android.graphics.drawable.GradientDrawable()
                bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                bg.cornerRadius = 10f * context.resources.displayMetrics.density
                bg.setColor(ContextCompat.getColor(context, R.color.list_chip_bg))
                pillView.background = bg
                
                container.addView(pillView)
            }
        }
        
        private fun getIconForLabel(label: String): Int {
            return when {
                label.contains("date") || label.contains("pickup") -> R.drawable.ic_calendar
                label.contains("type") || label.contains("status") -> R.drawable.ic_check_box
                label.contains("category") || label.contains("tag") -> R.drawable.ic_label
                else -> R.drawable.ic_edit
            }
        }
        
        private fun getColorForLabel(label: String): Int {
            return when {
                label.contains("date") || label.contains("pickup") -> 0xFF64B5F6.toInt()
                label.contains("type") || label.contains("status") -> 0xFFCE93D8.toInt()
                label.contains("category") || label.contains("tag") -> 0xFF81C784.toInt()
                else -> 0xFFFFB74D.toInt()
            }
        }

        private fun parseNotes(noteString: String, notes: MutableList<NoteItem>) {
            // Parse format: "Label:Value • Label:Value" (or legacy "|" delimiter)
            val delimiter = if (noteString.contains("•")) "•" else "|"
            val parts = noteString.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
            
            parts.forEach { part ->
                val colonIndex = part.indexOf(':')
                if (colonIndex > 0) {
                    val label = part.substring(0, colonIndex).trim().lowercase()
                    val rawValue = part.substring(colonIndex + 1).trim()
                    
                    // Only split multi-select fields (category/tag) by comma
                    val isMultiSelect = label.contains("category") || label.contains("tag")
                    if (isMultiSelect) {
                        rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { value ->
                            notes.add(NoteItem(value, label))
                        }
                    } else if (rawValue.isNotBlank()) {
                        notes.add(NoteItem(rawValue, label))
                    }
                } else if (part.isNotBlank()) {
                    notes.add(NoteItem(part, ""))
                }
            }
        }
    }

    private data class NoteItem(val text: String, val label: String)
    
    // (#19) Custom tag data class - Task 15: Added widgetType for proper color coding
    private data class CustomTag(
        val type: String, 
        val value: String,
        val widgetType: com.orderMate.modals.WidgetType? = null
    )
}
