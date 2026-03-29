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
            val container = binding.itemNotesContainer
            container.removeAllViews()
            
            if (noteString.isNullOrEmpty() || noteString.trim().isEmpty()) return

            // Parse note string format: "Label:Value • Label:Value" (or legacy "|" delimiter)
            val delimiter = if (noteString.contains("•")) "•" else "|"
            val parts = noteString.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
            
            parts.forEach { part ->
                val colonIndex = part.indexOf(':')
                val label = if (colonIndex > 0) part.substring(0, colonIndex).trim().lowercase() else ""
                val value = if (colonIndex > 0) part.substring(colonIndex + 1).trim() else part
                
                if (value.isEmpty()) return@forEach
                
                // Split comma-separated values into separate pills (consistent for all types)
                val values = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                values.forEach { displayText ->
                    val pillView = LayoutInflater.from(context)
                        .inflate(R.layout.item_note_pill, container, false) as LinearLayout
                    
                    val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                    val pillText = pillView.findViewById<TextView>(R.id.pillText)
                    
                    // No icon, just colored text and background
                    pillIcon.visibility = View.GONE
                    val color = getColorForLabel(label)
                    val bgColor = (color and 0x00FFFFFF) or 0x26000000
                    
                    pillText.text = displayText
                    pillText.setTextColor(color)
                    pillView.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
                    
                    container.addView(pillView)
                }
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
    }
}
