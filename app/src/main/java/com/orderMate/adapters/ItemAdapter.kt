package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderDetailItemBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.modals.ItemModal
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.convertToTwoDecimal
import com.orderMate.utils.toDoubleFloatPoint

/**
 * Item Adapter for order detail - matches HTML .detail-item-row styling
 * - Background: rgba(0,0,0,0.2) with 10dp border radius
 * - Clickable rows with chevron indicator
 * - Note pills for categories and tags
 * - Opens OrderMate popup on click
 */
class ItemAdapter(
    private val data: MutableList<ItemModal?>,
    private val listener: IOrderItemClickListener
) : RecyclerView.Adapter<ItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewBinder {
        val binding = ItemOrderDetailItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewBinder(binding)
    }

    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        if (data.isEmpty()) return
        
        val item = data[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = data.size

    inner class MyViewBinder(val binding: ItemOrderDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemModal?, position: Int) {
            val context = binding.root.context

            // Item name
            binding.itemName.text = item?.order?.name ?: "-"

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

            // Icon based on item name
            val iconRes = getIconForItem(item?.order?.name ?: "")
            binding.itemIcon.setImageResource(iconRes)

            // Note pills
            setupNotePills(context, item?.order?.note)

            // Click listener - entire row opens OrderMate popup
            binding.root.setOnClickListener {
                listener.onOrderItemClick(position, item?.order?.item?.id)
            }
        }

        private fun getIconForItem(name: String): Int {
            val nameLower = name.lowercase()
            return when {
                nameLower.contains("cake") || nameLower.contains("chocolate") -> R.drawable.ic_item_cake
                nameLower.contains("cookie") || nameLower.contains("cupcake") -> R.drawable.ic_item_cookie
                nameLower.contains("coffee") || nameLower.contains("latte") || nameLower.contains("espresso") -> R.drawable.ic_item_coffee
                nameLower.contains("muffin") || nameLower.contains("pastry") || nameLower.contains("pastries") -> R.drawable.ic_item_cookie
                nameLower.contains("bread") || nameLower.contains("croissant") || nameLower.contains("bagel") -> R.drawable.ic_item_cookie
                nameLower.contains("tart") || nameLower.contains("fruit") -> R.drawable.ic_item_cookie
                else -> R.drawable.ic_item_box
            }
        }

        private fun setupNotePills(context: Context, noteString: String?) {
            binding.itemNotesContainer.removeAllViews()
            
            if (noteString.isNullOrEmpty() || noteString.trim().isEmpty()) return

            // Parse note string format: "Label:Value • Label:Value" (or legacy "|" delimiter)
            val delimiter = if (noteString.contains("•")) "•" else "|"
            val parts = noteString.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
            
            parts.forEach { part ->
                val colonIndex = part.indexOf(':')
                val label = if (colonIndex > 0) part.substring(0, colonIndex).trim().lowercase() else ""
                val value = if (colonIndex > 0) part.substring(colonIndex + 1).trim() else part
                
                if (value.isEmpty()) return@forEach
                
                // For multi-select values (comma-separated), create separate pills
                val values = if (label.contains("tag") || label.contains("category")) {
                    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    listOf(value)
                }
                
                values.forEach { displayText ->
                    val pillView = LayoutInflater.from(context)
                        .inflate(R.layout.item_note_pill, binding.itemNotesContainer, false) as LinearLayout
                    
                    val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                    val pillText = pillView.findViewById<TextView>(R.id.pillText)
                    
                    pillText.text = displayText
                    
                    // Get icon and color based on widget type
                    val (iconRes, tintColor) = getIconAndColorForLabel(label)
                    
                    // Set icon
                    if (iconRes != 0) {
                        pillIcon.setImageResource(iconRes)
                        pillIcon.setColorFilter(tintColor)
                        pillIcon.visibility = View.VISIBLE
                    } else {
                        pillIcon.visibility = View.GONE
                    }
                    
                    // Whole pill matches widget color (bg with 15% opacity, text/icon with full color)
                    if (tintColor != 0) {
                        // Create rounded background with widget color at 15% opacity
                        val bgColor = (tintColor and 0x00FFFFFF) or 0x26000000
                        val drawable = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 10f * context.resources.displayMetrics.density
                            setColor(bgColor)
                        }
                        pillView.background = drawable
                        pillText.setTextColor(tintColor)
                    } else {
                        // Default fallback
                        pillView.setBackgroundResource(R.drawable.bg_note_chip_tag)
                        pillText.setTextColor(ContextCompat.getColor(context, R.color.tag_pill_text))
                    }
                    
                    binding.itemNotesContainer.addView(pillView)
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
                // Default - no icon
                else -> Pair(0, 0)
            }
        }
    }
}
