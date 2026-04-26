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
            val paymentState = order.paymentState?.name ?: "OPEN"
            val indicatorColor = when (paymentState) {
                "PAID" -> ContextCompat.getColor(context, R.color.paid_status_color)
                "OPEN" -> ContextCompat.getColor(context, R.color.unpaid_status_color)
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
            
            // (#18) Use cached widgets only for parsing (never defaults, enabled only)
            val cachedWidgets = WidgetManager.getInstance(binding.root.context).getCachedOrderWidgets()
            if (cachedWidgets.isNotEmpty()) {
                val textWidgets = cachedWidgets.filter { 
                    it.type == com.orderMate.modals.WidgetType.TEXT_BOX
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
         * Uses item_note_pill layout with icons matching item-level pills
         */
        private fun setupCustomTags(order: Order) {
            val context = binding.root.context
            val tagsContainer = binding.tagsContainer
            val density = context.resources.displayMetrics.density
            val inflater = LayoutInflater.from(context)
            
            // Remove any previously added custom tags (keep first 2 - order status and payment status)
            while (tagsContainer.childCount > 2) {
                tagsContainer.removeViewAt(2)
            }
            
            val customTags = getCustomTags(order)
            if (customTags.isEmpty()) return
            
            customTags.forEach { tag ->
                // Use WidgetColorUtils for consistent colors and icons based on widget type
                val tagColor = WidgetColorUtils.getColorForWidgetType(tag.widgetType)
                val iconRes = WidgetColorUtils.getIconForWidgetType(tag.widgetType)
                
                // Inflate the same pill layout used by item-level widgets
                val pillView = inflater.inflate(R.layout.item_note_pill, tagsContainer, false) as LinearLayout
                
                val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                val pillText = pillView.findViewById<TextView>(R.id.pillText)
                
                pillText.text = tag.value
                pillText.textSize = 11f
                pillText.setTextColor(tagColor)
                
                pillIcon.setImageResource(iconRes)
                pillIcon.setColorFilter(tagColor)
                
                // Unified pill background: 15% opacity + 25% border
                pillView.background = WidgetColorUtils.createPillBackground(tagColor, 12f, density)
                
                // Margin between pills
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (8 * density).toInt()
                }
                pillView.layoutParams = params
                
                tagsContainer.addView(pillView)
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
            
            // Use cached widgets only for pill rendering (never defaults, enabled only)
            val displayWidgets = WidgetManager.getInstance(binding.root.context).getCachedOrderWidgets()
            
            // Include all 4 widget types, TEXT_BOX included with truncation
            val parsedTags = OrderNoteParser.extractTagsFromNote(orderNote, displayWidgets, NoteLevel.ORDER, includeTextBox = true)
            parsedTags.forEach { tag ->
                val uniqueKey = "${tag.label.lowercase()}:${tag.value}"
                if (!seenValues.contains(uniqueKey)) {
                    seenValues.add(uniqueKey)
                    // Truncate TEXT_BOX values to 20 chars for list page
                    val displayValue = if (tag.widgetType == com.orderMate.modals.WidgetType.TEXT_BOX && tag.value.length > 20) {
                        tag.value.take(20) + "..."
                    } else {
                        tag.value
                    }
                    tags.add(CustomTag(tag.label.lowercase(), displayValue, tag.widgetType))
                }
            }
            
            return tags
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
            val state = order.state?.toString() ?: "OPEN"
            val displayText = formatPaymentState(state)
            val density = binding.root.context.resources.displayMetrics.density
            
            // Use WidgetColorUtils for consistent colors - ORDER_STATUS = Red
            val color = WidgetColorUtils.COLOR_ORDER_STATUS
            
            binding.orderStatusBadge.text = displayText
            binding.orderStatusBadge.background = WidgetColorUtils.createPillBackground(color, 12f, density)
            binding.orderStatusBadge.setTextColor(color)
        }

        private fun setupPaymentStatusBadge(order: Order) {
            val paymentState = order.paymentState?.name ?: "OPEN"
            val displayText = formatPaymentState(paymentState)
            val density = binding.root.context.resources.displayMetrics.density
            
            // Use WidgetColorUtils for consistent colors - PAYMENT_STATUS = Yellow
            val color = WidgetColorUtils.COLOR_PAYMENT_STATUS
            
            binding.paymentStatusBadge.text = displayText
            binding.paymentStatusBadge.background = WidgetColorUtils.createPillBackground(color, 12f, density)
            binding.paymentStatusBadge.setTextColor(color)
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
            // Deduplicate: Legacy behavior saves same note to all line items of same product,
            // so we only show one pill per unique label:value combination
            val seenPills = mutableSetOf<String>()
            
            // Use cached widgets only for pill rendering (never defaults, enabled only)
            val itemLevelWidgets = WidgetManager.getInstance(binding.root.context).getCachedItemWidgets()
            
            order.lineItems?.forEach { lineItem ->
                lineItem?.note?.let { note ->
                    if (note.isNotBlank()) {
                        val parsedTags = OrderNoteParser.extractTagsFromNote(note, itemLevelWidgets, NoteLevel.ITEM, includeTextBox = true)
                        parsedTags.forEach { tag ->
                            val pillKey = "${tag.label.lowercase()}:${tag.value}"
                            if (pillKey !in seenPills) {
                                seenPills.add(pillKey)
                                // Truncate TEXT_BOX values for list page item pills
                                val displayValue = if (tag.widgetType == com.orderMate.modals.WidgetType.TEXT_BOX && tag.value.length > 20) {
                                    tag.value.take(20) + "..."
                                } else {
                                    tag.value
                                }
                                notes.add(NoteItem(tag.label.lowercase(), displayValue, tag.widgetType))
                            }
                        }
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
            val density = context.resources.displayMetrics.density
            
            notes.forEach { noteItem ->
                val pillView = LayoutInflater.from(context)
                    .inflate(R.layout.item_note_pill, container, false) as LinearLayout
                
                val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                val pillText = pillView.findViewById<TextView>(R.id.pillText)
                
                // Get widget color from widgetType
                val pillColor = WidgetColorUtils.getColorForWidgetType(noteItem.widgetType)
                
                val iconRes = WidgetColorUtils.getIconForWidgetType(noteItem.widgetType)
                
                // Truncate to 12 chars, single line, no newlines
                val displayText = noteItem.text.replace("\n", " ").take(12).let {
                    if (noteItem.text.length > 12) "$it..." else it
                }
                pillText.text = displayText
                pillText.maxLines = 1
                
                // Use widget color for text and icon (same as order details page)
                pillText.setTextColor(pillColor)
                pillIcon.setImageResource(iconRes)
                pillIcon.setColorFilter(pillColor)
                
                // Unified pill background: 15% opacity + 25% border
                pillView.background = WidgetColorUtils.createPillBackground(pillColor, 10f, density)
                
                container.addView(pillView)
            }
        }
    }

    private data class NoteItem(
        val label: String, 
        val text: String,
        val widgetType: com.orderMate.modals.WidgetType
    )
    
    private data class CustomTag(
        val type: String, 
        val value: String,
        val widgetType: com.orderMate.modals.WidgetType
    )
}
