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
import com.orderMate.utils.Constants
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
}
