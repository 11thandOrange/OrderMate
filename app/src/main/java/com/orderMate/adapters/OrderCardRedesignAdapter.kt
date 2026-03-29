package com.orderMate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.google.android.material.chip.Chip
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderCardRedesignBinding
import com.orderMate.utils.Constants
import com.orderMate.utils.exceptionHandler
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

            // Customer Name
            binding.customerName.text = getCustomerName(order)

            // Employee Name
            binding.employeeName.text = getEmployeeName(order)

            // Payment Type
            setupPaymentType(order)

            // Total
            binding.orderTotal.text = formatTotal(order)

            // Custom Notes Pills
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

        private fun Float.dpToPx(): Float {
            return this * binding.root.context.resources.displayMetrics.density
        }

        private fun setupOrderStatusBadge(order: Order) {
            val context = binding.root.context
            val state = order.state?.toString() ?: "OPEN"
            
            when (state.uppercase()) {
                "OPEN" -> {
                    binding.orderStatusBadge.text = "Open"
                    binding.orderStatusBadge.setBackgroundResource(R.drawable.bg_badge_open)
                    binding.orderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.open_status_color))
                }
                "LOCKED" -> {
                    binding.orderStatusBadge.text = "Locked"
                    binding.orderStatusBadge.setBackgroundResource(R.drawable.bg_badge_closed)
                    binding.orderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.closed_status_color))
                }
                else -> {
                    binding.orderStatusBadge.text = state.lowercase().replaceFirstChar { it.uppercase() }
                    binding.orderStatusBadge.setBackgroundResource(R.drawable.bg_badge_open)
                    binding.orderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.open_status_color))
                }
            }
        }

        private fun setupPaymentStatusBadge(order: Order) {
            val context = binding.root.context
            val paymentState = order.paymentState?.name ?: "NOT_PAID"

            when (paymentState) {
                "PAID" -> {
                    binding.paymentStatusBadge.text = "Paid"
                    binding.paymentStatusBadge.setBackgroundResource(R.drawable.bg_badge_paid)
                    binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.paid_status_color))
                }
                "NOT_PAID" -> {
                    binding.paymentStatusBadge.text = "Unpaid"
                    binding.paymentStatusBadge.setBackgroundResource(R.drawable.bg_badge_unpaid)
                    binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.unpaid_status_color))
                }
                "PARTIALLY_PAID" -> {
                    binding.paymentStatusBadge.text = "Partial"
                    binding.paymentStatusBadge.setBackgroundResource(R.drawable.bg_badge_unpaid)
                    binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.orange_accent))
                }
                "REFUNDED" -> {
                    binding.paymentStatusBadge.text = "Refunded"
                    binding.paymentStatusBadge.setBackgroundResource(R.drawable.bg_badge_closed)
                    binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.closed_status_color))
                }
                else -> {
                    binding.paymentStatusBadge.text = paymentState.replace("_", " ")
                        .lowercase().replaceFirstChar { it.uppercase() }
                    binding.paymentStatusBadge.setBackgroundResource(R.drawable.bg_badge_open)
                    binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }
        }

        private fun getCustomerName(order: Order): String {
            return try {
                val customer = order.customers?.firstOrNull()
                if (customer != null) {
                    "${customer.firstName ?: ""} ${customer.lastName ?: ""}".trim()
                        .ifEmpty { "-" }
                } else {
                    "-"
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
            
            val displayNotes = notes.take(5)
            val hasMoreChip = notes.size > 5
            val targetCount = displayNotes.size + if (hasMoreChip) 1 else 0
            
            // Remove excess views from the end
            while (container.childCount > targetCount) {
                container.removeViewAt(container.childCount - 1)
            }
            
            // Update existing or add new chips
            displayNotes.forEachIndexed { index, noteItem ->
                if (index < container.childCount) {
                    // Update existing chip
                    (container.getChildAt(index) as? Chip)?.apply {
                        text = noteItem.text
                        val (iconRes, tintColor) = getIconAndColorForLabel(noteItem.label)
                        if (iconRes != 0) {
                            chipIcon = ContextCompat.getDrawable(context, iconRes)
                            chipIconTint = android.content.res.ColorStateList.valueOf(tintColor)
                        }
                    }
                } else {
                    // Add new chip
                    container.addView(createNoteChip(noteItem))
                }
            }
            
            // Handle "+X more" chip
            if (hasMoreChip) {
                val moreIndex = displayNotes.size
                if (moreIndex < container.childCount) {
                    (container.getChildAt(moreIndex) as? Chip)?.text = "+${notes.size - 5} more"
                } else {
                    container.addView(Chip(binding.root.context).apply {
                        text = "+${notes.size - 5} more"
                        setChipBackgroundColorResource(R.color.bg_glass)
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        textSize = 11f
                        chipMinHeight = 24f.dpToPx()
                        isClickable = false
                    })
                }
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
                    val value = part.substring(colonIndex + 1).trim()
                    
                    if (value.isNotBlank()) {
                        notes.add(NoteItem(value, label))
                    }
                } else if (part.isNotBlank()) {
                    // No colon, treat as plain text
                    notes.add(NoteItem(part, ""))
                }
            }
        }

        private fun createNoteChip(noteItem: NoteItem): Chip {
            return Chip(binding.root.context).apply {
                text = noteItem.text
                textSize = 12f
                chipMinHeight = 28f.dpToPx()
                chipCornerRadius = 6f.dpToPx()
                chipStrokeWidth = 0f
                isClickable = false
                
                // Light bg with dark text (matches design)
                setChipBackgroundColorResource(R.color.list_chip_bg)
                setTextColor(ContextCompat.getColor(context, R.color.list_chip_text))
                
                // Set icon and color based on widget type mapping
                val (iconRes, tintColor) = getIconAndColorForLabel(noteItem.label)
                if (iconRes != 0) {
                    chipIcon = ContextCompat.getDrawable(context, iconRes)
                    chipIconTint = android.content.res.ColorStateList.valueOf(tintColor)
                    chipIconSize = 16f.dpToPx()
                    iconStartPadding = 4f.dpToPx()
                }
                
                // Add spacing between chips
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 8f.dpToPx().toInt(), 8f.dpToPx().toInt())
                layoutParams = params
                
                // Truncate text widgets to single line
                val isTextWidget = noteItem.label.contains("description") || noteItem.label.contains("note")
                if (isTextWidget) {
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
            }
        }
        
        /**
         * Maps label to icon and color based on widget type:
         * - CALENDAR (date/pickup): ic_calendar, #64B5F6 (blue)
         * - SINGLE_SELECT (type/status): ic_check_box, #CE93D8 (purple)
         * - MULTI_SELECT (category/tag): ic_label, #81C784 (green)
         * - TEXT_BOX (description/note): ic_edit, #FFB74D (orange)
         */
        private fun getIconAndColorForLabel(label: String): Pair<Int, Int> {
            return when {
                // Calendar widget - blue
                label.contains("date") || label.contains("pickup") -> 
                    Pair(R.drawable.ic_calendar, 0xFF64B5F6.toInt())
                // Single select widget - purple (checkbox icon)
                label.contains("type") || label.contains("status") -> 
                    Pair(R.drawable.ic_check_box, 0xFFCE93D8.toInt())
                // Multi select widget - green (tag icon)
                label.contains("category") || label.contains("tag") -> 
                    Pair(R.drawable.ic_label, 0xFF81C784.toInt())
                // Text widget - orange (pencil icon)
                label.contains("description") || label.contains("note") -> 
                    Pair(R.drawable.ic_edit, 0xFFFFB74D.toInt())
                // Default - orange (text)
                else -> Pair(R.drawable.ic_edit, 0xFFFFB74D.toInt())
            }
        }
    }

    private data class NoteItem(val text: String, val label: String)
}
