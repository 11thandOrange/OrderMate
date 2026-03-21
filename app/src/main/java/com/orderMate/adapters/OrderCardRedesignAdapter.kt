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
import org.json.JSONObject

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
                        .ifEmpty { "Walk-in" }
                } else {
                    "Walk-in"
                }
            } catch (e: Exception) {
                "Walk-in"
            }
        }

        private fun getEmployeeName(order: Order): String {
            return try {
                order.employee?.jsonObject?.get(Constants.name)?.toString() ?: "Staff"
            } catch (e: Exception) {
                "Staff"
            }
        }

        private fun setupPaymentType(order: Order) {
            val context = binding.root.context
            val payments = order.payments

            if (payments.isNullOrEmpty()) {
                binding.paymentType.text = "Pending"
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
                    binding.paymentType.text = payments.firstOrNull()?.tender?.label ?: "Other"
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
            binding.notesChipsContainer.removeAllViews()
            
            val notes = mutableListOf<NoteItem>()
            
            // Extract notes from line items
            order.lineItems?.forEach { lineItem ->
                lineItem?.note?.let { note ->
                    if (note.isNotBlank()) {
                        parseNotes(note, notes)
                    }
                }
            }

            if (notes.isEmpty()) {
                binding.notesSection.visibility = View.GONE
                return
            }

            binding.notesSection.visibility = View.VISIBLE
            
            // Add chips for each note (limit to 5 to avoid overflow)
            notes.take(5).forEach { noteItem ->
                val chip = createNoteChip(noteItem)
                binding.notesChipsContainer.addView(chip)
            }

            // Add "+X more" if there are more notes
            if (notes.size > 5) {
                val moreChip = Chip(binding.root.context).apply {
                    text = "+${notes.size - 5} more"
                    setChipBackgroundColorResource(R.color.bg_glass)
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 11f
                    chipMinHeight = 24f.dpToPx()
                    isClickable = false
                }
                binding.notesChipsContainer.addView(moreChip)
            }
        }

        private fun parseNotes(noteString: String, notes: MutableList<NoteItem>) {
            // Try to parse as JSON first
            try {
                val json = JSONObject(noteString)
                json.keys().forEach { key ->
                    val value = json.optString(key)
                    if (value.isNotBlank()) {
                        // Determine note type based on key pattern
                        val type = when {
                            key.contains("category", ignoreCase = true) -> NoteType.CATEGORY
                            key.contains("tag", ignoreCase = true) -> NoteType.TAG
                            else -> NoteType.TEXT
                        }
                        notes.add(NoteItem(value, type))
                    }
                }
            } catch (e: Exception) {
                // Not JSON, treat as plain text
                if (noteString.length <= 30) {
                    notes.add(NoteItem(noteString, NoteType.TEXT))
                } else {
                    notes.add(NoteItem(noteString.take(27) + "...", NoteType.TEXT))
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
                
                // Glass-style background for all chips
                setChipBackgroundColorResource(R.color.note_chip_bg)
                setTextColor(ContextCompat.getColor(context, R.color.text_light))
            }
        }
    }

    private enum class NoteType {
        CATEGORY, TAG, TEXT
    }

    private data class NoteItem(val text: String, val type: NoteType)
}
