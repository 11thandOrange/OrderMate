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
import com.orderMate.modals.NoteLevel
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.createPaymentStatusPillView
import com.orderMate.utils.formatPaymentState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Event Preview Dialog (#82 CAL-4)
 * Matches HTML preview - dismissible by clicking outside
 */
class EventPreviewDialog : DialogFragment() {

    private var event: ScheduledEvent? = null
    private var onFullDetailsClick: ((ScheduledEvent) -> Unit)? = null

    // (#81 QA) Use OrderMate dialog theme to prevent blink
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }

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

        // Set order title (#76 - removed "Order" prefix, #81 QA - render full order ID)
        orderTitle.text = "#${currentEvent.orderId}"

        // (#76) Setup Clover default pill (payment status) above order level pills
        setupCloverDefaultPill(view, currentEvent)
        
        // Setup order-level notes pills (#93)
        setupOrderNotesPills(view, currentEvent)
        
        // (#77) Setup text box rows (rendered as rows, not pills)
        setupTextBoxRows(view, currentEvent)

        // (#81 QA) Conditionally render customer row - hide if no customer
        // Check both isBlank() and "-" fallback (matches CalendarFragment timeline line 1643)
        val customerRow = view.findViewById<View>(R.id.customerRow)
        val customerRowDivider = view.findViewById<View>(R.id.customerRowDivider)
        if (currentEvent.customerName.isBlank() || currentEvent.customerName == "-") {
            customerRow.visibility = View.GONE
            customerRowDivider.visibility = View.GONE
        } else {
            customerRow.visibility = View.VISIBLE
            customerRowDivider.visibility = View.VISIBLE
            customerName.text = currentEvent.customerName
        }
        
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
     * (#76) Setup Clover default pills (payment status + payment type) above order level pills.
     * Uses shared functions from CommonFunctions.kt for consistent styling.
     */
    private fun setupCloverDefaultPill(view: View, event: ScheduledEvent) {
        val container = view.findViewById<FlexboxLayout>(R.id.cloverDefaultPillContainer)
        container.removeAllViews()
        
        var hasAnyPill = false
        
        // Payment Status pill - use shared function
        createPaymentStatusPillView(requireContext(), event.paymentState)?.let { textView ->
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
            textView.layoutParams = lp
            container.addView(textView)
            hasAnyPill = true
        }
        
        // Payment Type pill - use shared function (need order for this, use paymentType from event if available)
        event.paymentType?.let { paymentType ->
            val density = resources.displayMetrics.density
            val textView = TextView(requireContext()).apply {
                text = paymentType.uppercase()
                setTextColor(WidgetColorUtils.COLOR_PAYMENT_TYPE)
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(
                    (10 * density).toInt(),
                    (4 * density).toInt(),
                    (10 * density).toInt(),
                    (4 * density).toInt()
                )
                background = WidgetColorUtils.createPillBackground(WidgetColorUtils.COLOR_PAYMENT_TYPE, 10f, density)
            }
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
            textView.layoutParams = lp
            container.addView(textView)
            hasAnyPill = true
        }
        
        container.visibility = if (hasAnyPill) View.VISIBLE else View.GONE
    }
    
    /**
     * (#77) Setup text box rows (rendered as dedicated rows, not pills)
     * Matches Register Drawer styling
     */
    private fun setupTextBoxRows(view: View, event: ScheduledEvent) {
        val container = view.findViewById<android.widget.LinearLayout>(R.id.textBoxRowsContainer)
        container.removeAllViews()
        
        val textBoxTags = event.customTags.filter { it.widgetType == com.orderMate.modals.WidgetType.TEXT_BOX }
        
        if (textBoxTags.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        
        textBoxTags.forEach { tag ->
            // EventTag text is in format "Label: Value", parse to extract both
            val parts = tag.text.split(": ", limit = 2)
            val label = if (parts.size > 1) parts[0] else "Note"
            val value = if (parts.size > 1) parts[1] else tag.text
            addTextBoxRow(container, label, value, density)
        }
    }
    
    /**
     * (#77) Add a dedicated row for TEXT_BOX widget (matches Settings tab style)
     */
    private fun addTextBoxRow(container: android.widget.LinearLayout, label: String, value: String, density: Float) {
        val context = requireContext()
        val iconRes = WidgetColorUtils.getIconForWidgetType(com.orderMate.modals.WidgetType.TEXT_BOX)
        val iconColor = WidgetColorUtils.getColorForWidgetType(com.orderMate.modals.WidgetType.TEXT_BOX)
        val iconBgRes = WidgetColorUtils.getIconBackgroundForWidgetType(com.orderMate.modals.WidgetType.TEXT_BOX)
        
        // Row container
        val rowLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        
        // Icon container - use widget-specific background color
        val iconFrame = android.widget.FrameLayout(context).apply {
            val size = dpToPx(32)
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size)
            setBackgroundResource(iconBgRes)
        }
        
        val icon = android.widget.ImageView(context).apply {
            val iconSize = dpToPx(16)
            layoutParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(iconRes)
            setColorFilter(iconColor)
        }
        iconFrame.addView(icon)
        rowLayout.addView(iconFrame)
        
        // Text container
        val textLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
            }
        }
        
        // Label
        val labelView = TextView(context).apply {
            text = label.uppercase()
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_muted))
            textSize = 11f
        }
        textLayout.addView(labelView)
        
        // Value
        val valueView = TextView(context).apply {
            text = value
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_light))
            textSize = 14f
            maxLines = 3
        }
        textLayout.addView(valueView)
        
        rowLayout.addView(textLayout)
        
        // Add divider
        val divider = View(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                dpToPx(1)
            )
            setBackgroundColor(0x1AFFFFFF.toInt())
        }
        
        container.addView(rowLayout)
        container.addView(divider)
    }
    
    /**
     * (#30) Setup order-level notes pills display.
     * Uses pre-parsed customTags from ScheduledEvent (widget-based parsing done in CalendarFragment).
     * (#77) TEXT_BOX rendered as rows, not pills - only SINGLE_SELECT, MULTI_SELECT, CALENDAR as pills.
     * Uses shared pill utility for consistent styling.
     */
    private fun setupOrderNotesPills(view: View, event: ScheduledEvent) {
        val container = view.findViewById<FlexboxLayout>(R.id.orderNotesPillsContainer)
        container.removeAllViews()
        
        // (#77) Filter out TEXT_BOX - those are rendered as rows
        val pillTags = event.customTags.filter { it.widgetType != com.orderMate.modals.WidgetType.TEXT_BOX }
        
        // Only show pills if there are widget values on the order
        if (pillTags.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        
        // Add order-level widget pills (CALENDAR, SINGLE_SELECT, MULTI_SELECT - not TEXT_BOX)
        pillTags.forEach { tag ->
            // EventTag text is "Label: Value" format, extract just the value for pills
            val parts = tag.text.split(": ", limit = 2)
            val value = if (parts.size > 1) parts[1] else tag.text
            
            val pillView = WidgetColorUtils.createPillView(
                requireContext(), container, value, tag.widgetType
            )
            
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dpToPx(6), dpToPx(4))
            pillView.layoutParams = lp
            
            container.addView(pillView)
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
                
                // Use widget-based parsing for item-level notes (all 4 types)
                renderNotePillsWithWidgets(item.note)
            }
            
            /**
             * Render item-level pills using widget-based parsing.
             * Uses OrderNoteParser.extractTagsFromNote() and WidgetColorUtils.
             * Supports all 4 widget types with truncation for TEXT_BOX.
             */
            private fun renderNotePillsWithWidgets(note: String?) {
                notesPillsContainer.removeAllViews()
                
                if (note.isNullOrBlank()) {
                    notesPillsContainer.visibility = View.GONE
                    return
                }
                
                // Get item-level widgets for parsing
                val widgets = WidgetManager.getCachedWidgets()
                val itemLevelWidgets = widgets.filter { it.level == NoteLevel.ITEM }
                
                if (itemLevelWidgets.isEmpty()) {
                    notesPillsContainer.visibility = View.GONE
                    return
                }
                
                // Parse using widget-based approach (all 4 types including TEXT_BOX)
                val parsedTags = OrderNoteParser.extractTagsFromNote(note, itemLevelWidgets, NoteLevel.ITEM, includeTextBox = true)
                
                if (parsedTags.isEmpty()) {
                    notesPillsContainer.visibility = View.GONE
                    return
                }
                
                notesPillsContainer.visibility = View.VISIBLE
                val context = itemView.context
                
                parsedTags.forEach { tag ->
                    val pillView = WidgetColorUtils.createPillView(
                        context, notesPillsContainer, tag.value, tag.widgetType, cornerRadiusDp = 8f
                    )
                    
                    val lp = FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, dpToPx(4), dpToPx(4))
                    pillView.layoutParams = lp
                    
                    notesPillsContainer.addView(pillView)
                }
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
