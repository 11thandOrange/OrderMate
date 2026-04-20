package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.google.android.material.chip.Chip
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderCardBinding
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.toDoubleFloatPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * New card-based adapter for the redesigned List Tab (Issue #81)
 * Reuses existing business logic from OrderAdapter but with new card UI
 * 
 * Row sections displayed:
 * 1. Order number
 * 2. Order status (open, closed)
 * 3. Payment type (card, cash)
 * 4. Payment status (paid, unpaid, refunded, partially refunded)
 * 5. Customer name
 * 6. Employee name
 * 7. All custom notes
 */
class OrderCardAdapter(
    private val data: ArrayList<Order?>,
    private val listener: IOrderItemClickListener
) : RecyclerView.Adapter<OrderCardAdapter.OrderCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderCardViewHolder {
        val binding = ItemOrderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderCardViewHolder, position: Int) {
        if (position >= data.size) return
        val order = data[position] ?: return
        holder.bind(order)
    }

    override fun getItemCount(): Int = data.size

    inner class OrderCardViewHolder(
        private val binding: ItemOrderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onOrderItemClick(adapterPosition, null)
            }
        }

        fun bind(order: Order) {
            val context = binding.root.context

            // 1. Order Number
            binding.orderNumber.text = context.getString(R.string.orders_text, order.id?.takeLast(8) ?: "-")

            // 2. Order Status (Open/Closed)
            setupOrderStatusBadge(order, context)

            // 3 & 4. Payment Type and Payment Status
            setupPaymentInfo(order, context)

            // 5. Customer Name
            setupCustomerName(order, context)

            // 6. Employee Name
            setupEmployeeName(order, context)

            // 7. Custom Notes
            setupNotes(order, context)

            // Status indicator color
            setupStatusIndicator(order)

            // Total
            setupTotal(order, context)
        }

        private fun setupOrderStatusBadge(order: Order, context: Context) {
            val isOpen = order.state?.lowercase() == Constants.OPEN.lowercase()
            
            binding.orderStatusBadge.apply {
                text = if (isOpen) context.getString(R.string.open) else context.getString(R.string.closed)
                setBackgroundResource(
                    if (isOpen) R.drawable.badge_background_open else R.drawable.badge_background_closed
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (isOpen) R.color.open_status_color else R.color.closed_status_color
                    )
                )
            }
        }

        private fun setupPaymentInfo(order: Order, context: Context) {
            // Payment Status Badge
            val paymentState = order.paymentState?.name?.uppercase() ?: Constants.OPEN
            
            val (statusText, bgRes, textColorRes) = when (paymentState) {
                Constants.PAID -> Triple(
                    context.getString(R.string.paid),
                    R.drawable.badge_background_paid,
                    R.color.paid_status_color
                )
                Constants.REFUNDED -> Triple(
                    context.getString(R.string.refunded),
                    R.drawable.badge_background_unpaid,
                    R.color.other_order_payment_status
                )
                Constants.PARTIALLY_REFUNDED -> Triple(
                    context.getString(R.string.partially_refunded),
                    R.drawable.badge_background_unpaid,
                    R.color.other_order_payment_status
                )
                Constants.PARTIALLY_PAID -> Triple(
                    context.getString(R.string.partially_paid),
                    R.drawable.badge_background_open,
                    R.color.open_status_color
                )
                else -> Triple(
                    context.getString(R.string.unpaid),
                    R.drawable.badge_background_unpaid,
                    R.color.other_order_payment_status
                )
            }

            binding.paymentStatusBadge.apply {
                text = statusText
                setBackgroundResource(bgRes)
                setTextColor(ContextCompat.getColor(context, textColorRes))
            }

            // Payment Type (Card/Cash)
            val paymentType = getPaymentType(order.payments, context)
            binding.paymentType.text = paymentType

            // Payment icon
            val isCash = paymentType.lowercase().contains("cash")
            binding.paymentIcon.setImageResource(
                if (isCash) R.drawable.ic_cash else R.drawable.ic_credit_card
            )
        }

        private fun setupCustomerName(order: Order, context: Context) {
            val customerName = if (order.customers?.isNotEmpty() == true) {
                val customer = order.customers[0]
                val fullName = "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".trim()
                if (fullName.isEmpty() || fullName == "null null") {
                    context.getString(R.string.hypen_text)
                } else {
                    fullName
                }
            } else {
                context.getString(R.string.hypen_text)
            }
            binding.customerName.text = customerName
        }

        private fun setupEmployeeName(order: Order, context: Context) {
            // Get employee name - try jsonObject first, then use cached lookup
            val employeeName = order.employee?.jsonObject?.get(Constants.name)?.toString()
                ?: order.employee?.id?.let { MyApp.getInstance().getCachedEmployeeName(it) }
            binding.employeeName.text = employeeName ?: context.getString(R.string.hypen_text)
        }

        private fun setupNotes(order: Order, context: Context) {
            val notes = extractNotes(order.lineItems)
            
            if (notes.isEmpty()) {
                binding.notesSection.visibility = View.GONE
                return
            }

            binding.notesSection.visibility = View.VISIBLE
            binding.notesChipGroup.removeAllViews()

            // Add chips for each note category
            notes.forEach { noteItem ->
                val chip = Chip(context).apply {
                    text = noteItem
                    setChipBackgroundColorResource(R.color.header_blue)
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    textSize = 10f
                    isClickable = false
                    chipMinHeight = 0f
                    chipCornerRadius = 16f
                    chipStartPadding = 8f
                    chipEndPadding = 8f
                }
                binding.notesChipGroup.addView(chip)
            }
        }

        private fun setupStatusIndicator(order: Order) {
            val paymentState = order.paymentState?.name?.uppercase() ?: ""
            val colorRes = when (paymentState) {
                Constants.PAID -> R.color.paid_status_color
                Constants.REFUNDED, Constants.PARTIALLY_REFUNDED -> R.color.other_order_payment_status
                Constants.PARTIALLY_PAID, Constants.OPEN -> R.color.open_status_color
                else -> R.color.other_order_payment_status
            }
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
        }

        private fun setupTotal(order: Order, context: Context) {
            val currencySymbol = order.currency?.convertToSymbol() ?: "$"
            val total = order.total?.toInt()?.toDoubleFloatPoint() ?: "0.00"
            binding.orderTotal.text = context.getString(R.string.priceString, currencySymbol, total)
        }

        /**
         * Extracts payment type label from payments list
         * Reuses logic from original OrderAdapter
         */
        private fun getPaymentType(payments: List<Payment>?, context: Context): String {
            if (payments.isNullOrEmpty()) return context.getString(R.string.hypen_text)
            
            val types = payments
                .mapNotNull { it.tender?.label }
                .filter { it.isNotEmpty() }
                .distinct()
            
            return if (types.isEmpty()) {
                context.getString(R.string.hypen_text)
            } else {
                types.joinToString(", ")
            }
        }

        /**
         * Extracts and parses notes from line items
         * Returns list of formatted note strings for chips
         */
        private fun extractNotes(lineItems: List<LineItem>?): List<String> {
            if (lineItems.isNullOrEmpty()) return emptyList()

            val notesList = mutableListOf<String>()
            
            lineItems.forEach { item ->
                val note = item.note?.trim()
                if (!note.isNullOrEmpty()) {
                    // Parse the structured note format: "pickup date:XX • category:YY • ..."
                    val parts = note.split("•")
                    parts.forEach { part ->
                        val trimmed = part.trim()
                        if (trimmed.isNotEmpty() && trimmed != "-") {
                            // Format: "key:value" -> "📋 Key: Value"
                            val emoji = getEmojiForNoteType(trimmed)
                            notesList.add("$emoji $trimmed")
                        }
                    }
                }
            }

            return notesList.distinct().take(5) // Limit to 5 chips
        }

        private fun getEmojiForNoteType(note: String): String {
            return when {
                note.lowercase().contains("pickup") || note.lowercase().contains("date") -> "📅"
                note.lowercase().contains("category") -> "🏷️"
                note.lowercase().contains("status") -> "📊"
                note.lowercase().contains("type") -> "📝"
                note.lowercase().contains("description") -> "💬"
                note.lowercase().contains("subcategory") -> "📁"
                else -> "📋"
            }
        }
    }
}
