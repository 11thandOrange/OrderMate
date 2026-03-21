package com.orderMate.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.model.EventType
import com.orderMate.model.LineItemPreview
import com.orderMate.model.ScheduledEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Event Preview Dialog (#82 CAL-4)
 *
 * Shows order details for a scheduled event.
 * Matches HTML preview layout with:
 * - Type badge + Order title + Full Details button
 * - Customer, Due Date, Total, Items rows
 * - Line items list with price and quantity
 */
class EventPreviewDialog : DialogFragment() {

    private var event: ScheduledEvent? = null
    private var onFullDetailsClick: ((ScheduledEvent) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_event_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentEvent = event ?: return

        // Header
        val eventTypeBadge = view.findViewById<TextView>(R.id.eventTypeBadge)
        val orderTitle = view.findViewById<TextView>(R.id.orderTitle)
        val btnFullDetails = view.findViewById<View>(R.id.btnFullDetails)
        val btnClose = view.findViewById<TextView>(R.id.btnClose)
        
        // Details
        val customerName = view.findViewById<TextView>(R.id.customerName)
        val dueDate = view.findViewById<TextView>(R.id.dueDate)
        val orderTotal = view.findViewById<TextView>(R.id.orderTotal)
        val itemCount = view.findViewById<TextView>(R.id.itemCount)
        val itemsList = view.findViewById<RecyclerView>(R.id.itemsList)

        // Set type badge
        eventTypeBadge.text = currentEvent.type.getDisplayName().uppercase()
        when (currentEvent.type) {
            EventType.PICKUP -> {
                eventTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.event_pickup))
                eventTypeBadge.setBackgroundResource(R.drawable.bg_event_badge_pickup)
            }
            EventType.DELIVERY -> {
                eventTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.event_delivery))
                eventTypeBadge.setBackgroundResource(R.drawable.bg_event_badge_delivery)
            }
            EventType.PREORDER -> {
                eventTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.event_preorder))
                eventTypeBadge.setBackgroundResource(R.drawable.bg_event_badge_preorder)
            }
        }

        // Set order title (truncate ID for display)
        val shortId = currentEvent.orderId.takeLast(4).uppercase()
        orderTitle.text = "Order #$shortId"

        // Set details
        customerName.text = currentEvent.customerName
        
        val dateTimeFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        dueDate.text = dateTimeFormat.format(currentEvent.dueDate)
        
        orderTotal.text = "$${String.format("%.2f", currentEvent.total)}"
        itemCount.text = "${currentEvent.itemCount} items"

        // Items list
        if (currentEvent.lineItems.isNotEmpty()) {
            itemsList.visibility = View.VISIBLE
            itemsList.layoutManager = LinearLayoutManager(requireContext())
            itemsList.adapter = LineItemAdapter(currentEvent.lineItems)
        } else {
            itemsList.visibility = View.GONE
        }

        // Full Details button
        btnFullDetails.setOnClickListener {
            onFullDetailsClick?.invoke(currentEvent)
            dismiss()
        }

        // Close button
        btnClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setEvent(event: ScheduledEvent) {
        this.event = event
    }

    fun setOnEventClickListener(listener: (ScheduledEvent) -> Unit) {
        this.onFullDetailsClick = listener
    }

    /**
     * Adapter for line items with price and quantity
     */
    inner class LineItemAdapter(
        private val items: List<LineItemPreview>
    ) : RecyclerView.Adapter<LineItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_line_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = minOf(items.size, 5)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val itemName: TextView = itemView.findViewById(R.id.itemName)
            private val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
            private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)

            fun bind(item: LineItemPreview) {
                itemName.text = item.name
                itemPrice.text = "$${String.format("%.2f", item.price)}"
                itemQuantity.text = "x${item.quantity}"
            }
        }
    }

    companion object {
        const val TAG = "EventPreviewDialog"

        fun newInstance(event: ScheduledEvent): EventPreviewDialog {
            return EventPreviewDialog().apply {
                setEvent(event)
            }
        }
        
        // For backwards compatibility - shows first event
        fun newInstance(events: List<ScheduledEvent>, date: Date): EventPreviewDialog {
            return EventPreviewDialog().apply {
                if (events.isNotEmpty()) {
                    setEvent(events.first())
                }
            }
        }
    }
}
