package com.orderMate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.orderMate.utils.getPaymentTypeLabel
import com.orderMate.utils.setupPaymentStatusPill
import com.orderMate.utils.setupPaymentTypePill
// formatOrderState removed - only using payment status now
import com.orderMate.utils.getPaymentStateFromOrder
import com.orderMate.utils.MyApp
import com.orderMate.utils.toDoubleFloatPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

            // Order Status Badge - REMOVED (only using payment status now)
            binding.orderStatusBadge.visibility = View.GONE

            // Payment Status Badge - show for ALL payment states including OPEN/UNPAID
            setupPaymentStatusBadge(order)

            // Order Date (Task 19)
            binding.orderDate.text = getOrderDate(order)

            // Customer Name
            binding.customerName.text = getCustomerName(order)

            // Employee Name (#81 QA) - use same async pattern as OrderDetailFragment
            setupEmployeeName(order)

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

            // Left Status Indicator (green for paid, red for unpaid/open, orange for partial)
            val paymentState = getPaymentStateFromOrder(order)
            val indicatorColor = when (paymentState) {
                "PAID" -> ContextCompat.getColor(context, R.color.paid_status_color)
                "PARTIALLY_PAID" -> ContextCompat.getColor(context, R.color.orange_accent)
                "REFUNDED", "PARTIALLY_REFUNDED" -> ContextCompat.getColor(context, R.color.orange_accent)
                "CREDITED" -> ContextCompat.getColor(context, R.color.paid_status_color)
                // null or OPEN means unpaid - show red indicator
                else -> ContextCompat.getColor(context, R.color.unpaid_status_color)
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
         * Uses shared pill utility for consistent styling
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
                val pillView = WidgetColorUtils.createPillView(
                    context, tagsContainer, tag.value, tag.widgetType, 
                    cornerRadiusDp = 12f, truncate = false
                )
                
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
                    // (#77) Truncate all pill values consistently
                    val displayValue = WidgetColorUtils.truncateForPill(tag.value)
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

        // setupOrderStatusBadge REMOVED - only using payment status now
        // Order status badge is hidden in bind() method

        private fun setupPaymentStatusBadge(order: Order) {
            // Use shared function from CommonFunctions.kt
            setupPaymentStatusPill(binding.paymentStatusBadge, order)
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

        /**
         * (#81 QA) Setup employee name using same pattern as OrderDetailFragment.
         * 
         * Clover's getOrders() may return partial employee data (only ID, no name).
         * This method:
         * 1. Tries direct access to employee.jsonObject.name (sync)
         * 2. Falls back to MyApp.getEmployeeName(employeeId) on IO thread (async)
         * 3. Updates UI on Main thread after async fetch
         */
        private fun setupEmployeeName(order: Order) {
            val employee = order.employee
            
            // Try direct access first (same as OrderDetailFragment line 294-297)
            exceptionHandler({
                val employeeName = employee?.jsonObject?.get(Constants.name)?.toString()
                binding.employeeName.text = if (!employeeName.isNullOrBlank() && employeeName != "null") {
                    employeeName
                } else {
                    "-"
                }
            })
            // Fallback: async fetch using coroutine (same as OrderDetailFragment line 300-308)
            {
                CoroutineScope(Dispatchers.IO).launch {
                    val value = MyApp.getInstance().getEmployeeName(employee?.id)
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.employeeName.text = value ?: "-"
                    }
                }
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
            val orderId = order.id?.takeLast(8) ?: "null"
            android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - setupPaymentType CALLED")
            
            // Use shared function for pill badge
            android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - calling setupPaymentTypePill...")
            setupPaymentTypePill(binding.paymentTypeBadge, order)
            android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - pill visibility after: ${binding.paymentTypeBadge.visibility} (0=VISIBLE, 4=INVISIBLE, 8=GONE)")
            
            // Also update the payment type column (text + icon)
            android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - calling getPaymentTypeLabel...")
            val displayLabel = getPaymentTypeLabel(order)
            android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - displayLabel from getPaymentTypeLabel: '$displayLabel'")
            
            if (displayLabel != null) {
                binding.paymentType.text = displayLabel
                binding.paymentIcon.setImageResource(
                    if (displayLabel.lowercase().contains("cash")) R.drawable.ic_cash 
                    else R.drawable.ic_credit_card
                )
                android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - text column set to: '$displayLabel'")
            } else {
                binding.paymentType.text = "-"
                binding.paymentIcon.setImageResource(R.drawable.ic_credit_card)
                android.util.Log.d("SetupPaymentTypeDebug", "Order #$orderId - text column set to: '-'")
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
                                // (#77) Truncate all pill values consistently
                                val displayValue = WidgetColorUtils.truncateForPill(tag.value)
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
            
            notes.forEach { noteItem ->
                // (#77) Text already truncated in extraction, use shared pill utility
                WidgetColorUtils.addPillToContainer(
                    context, container, noteItem.text, noteItem.widgetType, truncate = false
                )
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
