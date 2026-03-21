package com.orderMate.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.model.EventType
import com.orderMate.model.ScheduledEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Event Preview Dialog (#82 CAL-4)
 * 
 * Shows scheduled events for a selected day.
 * Allows tapping an event to view full order details.
 */
class EventPreviewDialog : DialogFragment() {

    private var events: List<ScheduledEvent> = emptyList()
    private var selectedDate: Date = Date()
    private var onEventClick: ((ScheduledEvent) -> Unit)? = null
    private var onViewAllClick: (() -> Unit)? = null

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
        
        val eventDate = view.findViewById<TextView>(R.id.eventDate)
        val eventCount = view.findViewById<TextView>(R.id.eventCount)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val eventsList = view.findViewById<RecyclerView>(R.id.eventsList)
        val btnViewAll = view.findViewById<TextView>(R.id.btnViewAll)

        // Set date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        eventDate.text = dateFormat.format(selectedDate)

        // Set event count
        val countText = if (events.size == 1) "1 scheduled order" else "${events.size} scheduled orders"
        eventCount.text = countText

        // Setup close button
        btnClose.setOnClickListener { dismiss() }

        // Setup events list
        eventsList.layoutManager = LinearLayoutManager(requireContext())
        eventsList.adapter = EventPreviewAdapter(events) { event ->
            onEventClick?.invoke(event)
            dismiss()
        }

        // Show "View All" if more than 5 events
        if (events.size > 5) {
            btnViewAll.visibility = View.VISIBLE
            btnViewAll.setOnClickListener {
                onViewAllClick?.invoke()
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setEvents(events: List<ScheduledEvent>, date: Date) {
        this.events = events
        this.selectedDate = date
    }

    fun setOnEventClickListener(listener: (ScheduledEvent) -> Unit) {
        this.onEventClick = listener
    }

    fun setOnViewAllClickListener(listener: () -> Unit) {
        this.onViewAllClick = listener
    }

    /**
     * Adapter for events in the preview modal
     */
    inner class EventPreviewAdapter(
        private val events: List<ScheduledEvent>,
        private val onClick: (ScheduledEvent) -> Unit
    ) : RecyclerView.Adapter<EventPreviewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(events[position])
        }

        override fun getItemCount(): Int = minOf(events.size, 5)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val eventTypeIndicator: View = itemView.findViewById(R.id.eventTypeIndicator)
            private val customerName: TextView = itemView.findViewById(R.id.customerName)
            private val eventType: TextView = itemView.findViewById(R.id.eventType)
            private val itemCount: TextView = itemView.findViewById(R.id.itemCount)
            private val orderTotal: TextView = itemView.findViewById(R.id.orderTotal)

            fun bind(event: ScheduledEvent) {
                val context = itemView.context

                customerName.text = event.customerName
                eventType.text = event.type.getDisplayName()
                itemCount.text = "${event.itemCount} items"
                orderTotal.text = "$${String.format("%.2f", event.total)}"

                // Set colors based on event type
                val color = when (event.type) {
                    EventType.PICKUP -> ContextCompat.getColor(context, R.color.event_pickup)
                    EventType.DELIVERY -> ContextCompat.getColor(context, R.color.event_delivery)
                    EventType.PREORDER -> ContextCompat.getColor(context, R.color.event_preorder)
                }
                eventTypeIndicator.setBackgroundColor(color)
                eventType.setTextColor(color)

                itemView.setOnClickListener { onClick(event) }
            }
        }
    }

    companion object {
        const val TAG = "EventPreviewDialog"

        fun newInstance(events: List<ScheduledEvent>, date: Date): EventPreviewDialog {
            return EventPreviewDialog().apply {
                setEvents(events, date)
            }
        }
    }
}
