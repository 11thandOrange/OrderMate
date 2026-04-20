package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemDrawerLineItemBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.modals.ItemModal
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetType
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.convertToTwoDecimal
import com.orderMate.utils.toDoubleFloatPoint

/**
 * Drawer Item Adapter - simplified layout without item icons
 * Used only in the Register floating drawer
 */
class DrawerItemAdapter(
    private val data: MutableList<ItemModal?>,
    private val listener: IOrderItemClickListener
) : RecyclerView.Adapter<DrawerItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDrawerLineItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data.isEmpty()) return
        holder.bind(data[position], position)
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(private val binding: ItemDrawerLineItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemModal?, position: Int) {
            val context = binding.root.context

            // Item name
            binding.itemName.text = item?.order?.name ?: "-"

            // Item modifiers (from Clover)
            val modifiersString = item?.order?.modifications?.mapNotNull { it?.name }
                ?.joinToString(", ")
                ?.takeIf { it.isNotBlank() }
            if (modifiersString != null) {
                binding.itemModifiers.text = modifiersString
                binding.itemModifiers.visibility = View.VISIBLE
            } else {
                binding.itemModifiers.visibility = View.GONE
            }

            // Quantity badge
            val qty = item?.itemCount ?: 1
            binding.itemQty.text = "x$qty"

            // Price calculation
            val basePrice = item?.order?.price?.toDoubleFloatPoint()?.toDouble() ?: 0.0
            val totalPrice = if (item?.order?.hasUnitName() == true && item.order.hasUnitQty()) {
                basePrice * qty * ((item.order.unitQty ?: 1000) / 1000)
            } else {
                basePrice * qty
            }
            
            binding.itemPrice.text = context.getString(
                R.string.priceString,
                OrderDetailFragment.currencyName.convertToSymbol(),
                totalPrice.toDoubleFloatPoint().toString().convertToTwoDecimal()
            )

            // Note pills
            setupNotePills(context, item?.order?.note)

            // Click listener
            binding.root.setOnClickListener {
                listener.onOrderItemClick(position, item?.order?.item?.id)
            }
        }

        private fun setupNotePills(context: Context, noteString: String?) {
            val container = binding.itemNotesContainer
            container.removeAllViews()
            
            if (noteString.isNullOrEmpty() || noteString.trim().isEmpty()) return

            val density = context.resources.displayMetrics.density
            
            val widgets = WidgetManager.getCachedWidgets()
            val itemLevelWidgets = widgets.filter { it.level == NoteLevel.ITEM }
            
            val parsedTags = OrderNoteParser.extractTagsFromNote(noteString, itemLevelWidgets, NoteLevel.ITEM)
            parsedTags.forEach { tag ->
                addPillView(context, container, tag.value, tag.widgetType, density)
            }
        }
        
        private fun addPillView(
            context: Context,
            container: com.google.android.flexbox.FlexboxLayout,
            value: String,
            widgetType: WidgetType,
            density: Float
        ) {
            val pillView = LayoutInflater.from(context)
                .inflate(R.layout.item_note_pill, container, false) as LinearLayout
            
            val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
            val pillText = pillView.findViewById<TextView>(R.id.pillText)
            
            val color = WidgetColorUtils.getColorForWidgetType(widgetType)
            val iconRes = getIconForWidgetType(widgetType)
            
            val displayText = value.replace("\n", " ").take(12).let {
                if (value.length > 12) "$it..." else it
            }
            pillText.text = displayText
            pillText.maxLines = 1
            pillText.setTextColor(color)
            pillIcon.setImageResource(iconRes)
            pillIcon.setColorFilter(color)
            
            pillView.background = WidgetColorUtils.createPillBackground(color, 10f, density)
            
            container.addView(pillView)
        }
        
        private fun getIconForWidgetType(widgetType: WidgetType): Int {
            return when (widgetType) {
                WidgetType.CALENDAR -> R.drawable.ic_calendar
                WidgetType.SINGLE_SELECT -> R.drawable.ic_check_box
                WidgetType.MULTI_SELECT -> R.drawable.ic_label
                WidgetType.TEXT_BOX -> R.drawable.ic_edit
            }
        }
    }
}
