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
import com.google.android.flexbox.FlexboxLayout
import com.orderMate.R
import com.orderMate.model.CustomNote
import com.orderMate.model.EventType
import com.orderMate.model.LineItemPreview
import com.orderMate.model.ScheduledEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Event Preview Dialog (#82 CAL-4)
 * Matches HTML preview - dismissible by clicking outside
 */
class EventPreviewDialog : DialogFragment() {

    private var event: ScheduledEvent? = null
    private var onFullDetailsClick: ((ScheduledEvent) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true) // Click outside to dismiss like HTML
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
        val orderTitle = view.findViewById<TextView>(R.id.orderTitle)
        val btnFullDetails = view.findViewById<View>(R.id.btnFullDetails)
        
        // Details
        val customerName = view.findViewById<TextView>(R.id.customerName)
        val dueDate = view.findViewById<TextView>(R.id.dueDate)
        val orderTotal = view.findViewById<TextView>(R.id.orderTotal)
        val itemCount = view.findViewById<TextView>(R.id.itemCount)
        val itemsList = view.findViewById<RecyclerView>(R.id.itemsList)

        // Set order title
        val shortId = currentEvent.orderId.takeLast(4).uppercase()
        orderTitle.text = "Order #$shortId"

        // Setup order-level notes pills (#93)
        setupOrderNotesPills(view, currentEvent)

        // Set details
        customerName.text = if (currentEvent.customerName.isBlank()) "-" else currentEvent.customerName
        
        val dateTimeFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        dueDate.text = dateTimeFormat.format(currentEvent.dueDate)
        
        orderTotal.text = "$${String.format("%.2f", currentEvent.total)}"
        itemCount.text = "${currentEvent.itemCount} items"

        // Items list (no limit - show all items like HTML)
        if (currentEvent.lineItems.isNotEmpty()) {
            itemsList.visibility = View.VISIBLE
            itemsList.layoutManager = LinearLayoutManager(requireContext())
            itemsList.adapter = LineItemAdapter(currentEvent.lineItems)
            
            // Setup scroll indicator
            setupScrollIndicator(view, itemsList)
        } else {
            itemsList.visibility = View.GONE
        }

        // Full Details button
        btnFullDetails.setOnClickListener {
            onFullDetailsClick?.invoke(currentEvent)
            dismiss()
        }
    }
    
    private fun setupScrollIndicator(view: View, recyclerView: RecyclerView) {
        val scrollIndicator = view.findViewById<View>(R.id.scrollIndicator)
        scrollIndicator?.let { indicator ->
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    updateScrollIndicator(rv, indicator)
                }
            })
            // Initial check
            recyclerView.post { updateScrollIndicator(recyclerView, indicator) }
        }
    }
    
    private fun updateScrollIndicator(recyclerView: RecyclerView, indicator: View) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        val hasMoreContent = lastVisibleItem < itemCount - 1
        indicator.visibility = if (hasMoreContent) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        // Set max width to 400dp (matches HTML max-width: 400px)
        val displayMetrics = resources.displayMetrics
        val maxWidthPx = (400 * displayMetrics.density).toInt()
        val screenWidth = displayMetrics.widthPixels
        val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
        
        dialog?.window?.setLayout(
            targetWidth,
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
     * (#30) Setup order-level notes pills display.
     * Uses pre-parsed customTags from ScheduledEvent (widget-based parsing done in CalendarFragment).
     * Falls back to legacy parsing if customTags is empty.
     */
    private fun setupOrderNotesPills(view: View, event: ScheduledEvent) {
        val container = view.findViewById<FlexboxLayout>(R.id.orderNotesPillsContainer)
        container.removeAllViews()
        
        // Only show pills if there are widget values on the order
        if (event.customTags.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        
        // Add order-level widget pills (CALENDAR, SINGLE_SELECT, MULTI_SELECT)
        // All use WidgetColorUtils for consistent color coding
        event.customTags.forEach { tag ->
            val color = com.orderMate.utils.WidgetColorUtils.getColorForWidgetType(tag.widgetType)
            
            val pill = TextView(requireContext()).apply {
                text = tag.text
                textSize = 11f
                setTextColor(color)
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                // Same pill styling as everywhere else: 15% opacity bg + 25% border
                background = com.orderMate.utils.WidgetColorUtils.createPillBackground(color, 10f, density)
                
                val lp = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
                layoutParams = lp
            }
            container.addView(pill)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

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

        // No limit - show all items (matches HTML behavior)
        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val itemName: TextView = itemView.findViewById(R.id.itemName)
            private val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
            private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
            private val notesPillsContainer: FlexboxLayout = itemView.findViewById(R.id.notesPillsContainer)

            fun bind(item: LineItemPreview) {
                itemName.text = item.name
                itemPrice.text = "$${String.format("%.2f", item.price)}"
                itemQuantity.text = "x${item.quantity}"
                
                // Render custom notes as pills (matches HTML)
                renderNotePills(item.customNotes)
            }
            
            private fun renderNotePills(customNotes: List<CustomNote>) {
                notesPillsContainer.removeAllViews()
                
                if (customNotes.isEmpty()) {
                    notesPillsContainer.visibility = View.GONE
                    return
                }
                
                notesPillsContainer.visibility = View.VISIBLE
                
                customNotes.forEach { note ->
                    when (note.type) {
                        "select" -> {
                            // Category style pill (purple)
                            note.getStringValue()?.let { value ->
                                addPill(value, R.drawable.bg_note_pill_category, R.color.note_pill_category_text)
                            }
                        }
                        "multiselect" -> {
                            // Multiple tag pills (green)
                            note.getListValue().forEach { tag ->
                                addPill(tag, R.drawable.bg_note_pill_tag, R.color.note_pill_tag_text)
                            }
                        }
                        "text" -> {
                            // Text description pill (orange) - truncate if >30 chars
                            note.getStringValue()?.let { value ->
                                if (value.isNotBlank()) {
                                    val displayText = if (value.length > 30) {
                                        value.take(30) + "..."
                                    } else {
                                        value
                                    }
                                    addPill(displayText, R.drawable.bg_note_pill_text, R.color.note_pill_text_text)
                                }
                            }
                        }
                    }
                }
            }
            
            private fun addPill(text: String, backgroundRes: Int, textColorRes: Int) {
                val context = itemView.context
                val pill = TextView(context).apply {
                    this.text = text
                    setBackgroundResource(backgroundRes)
                    setTextColor(ContextCompat.getColor(context, textColorRes))
                    textSize = 10f
                    setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                    
                    val lp = FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, dpToPx(4), dpToPx(4))
                    layoutParams = lp
                }
                notesPillsContainer.addView(pill)
            }
            
            private fun dpToPx(dp: Int): Int {
                return (dp * itemView.resources.displayMetrics.density).toInt()
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
        
        fun newInstance(events: List<ScheduledEvent>, date: Date): EventPreviewDialog {
            return EventPreviewDialog().apply {
                if (events.isNotEmpty()) {
                    setEvent(events.first())
                }
            }
        }
    }
}
