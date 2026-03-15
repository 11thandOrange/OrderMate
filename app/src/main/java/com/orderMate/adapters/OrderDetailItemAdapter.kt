package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.LineItem
import com.orderMate.R
import com.orderMate.databinding.ItemOrderDetailItemBinding
import com.orderMate.utils.toDoubleFloatPoint

/**
 * Adapter for order detail items with note pills (#87 requirement)
 * Displays items with icon, name, pills, quantity, and price
 */
class OrderDetailItemAdapter(
    private val items: List<LineItem>
) : RecyclerView.Adapter<OrderDetailItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderDetailItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= items.size) return
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemOrderDetailItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LineItem) {
            val context = binding.root.context

            // Item name
            binding.itemName.text = item.name ?: "-"

            // Quantity
            val qty = item.unitQty ?: 1
            binding.itemQty.text = "x$qty"

            // Price
            val price = item.price?.toInt()?.toDoubleFloatPoint() ?: "0.00"
            binding.itemPrice.text = "$$price"

            // Icon based on item name
            val iconRes = getIconForItem(item.name ?: "")
            binding.itemIcon.setImageResource(iconRes)

            // Note pills
            setupNotePills(context, item)
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

        private fun setupNotePills(context: Context, item: LineItem) {
            binding.itemNotesContainer.removeAllViews()
            
            val noteString = item.note?.trim()
            if (noteString.isNullOrEmpty()) return

            // Parse note string format: "category: X • Tag1 • Tag2"
            val parts = noteString.split("•").map { it.trim() }.filter { it.isNotEmpty() }
            
            parts.forEach { part ->
                val pill = LayoutInflater.from(context)
                    .inflate(R.layout.item_note_pill, binding.itemNotesContainer, false) as TextView
                
                val isCategory = part.lowercase().startsWith("category:")
                val displayText = if (isCategory) {
                    part.replace(Regex("^category:\\s*", RegexOption.IGNORE_CASE), "")
                } else {
                    part
                }
                
                pill.text = displayText
                
                if (isCategory) {
                    pill.setBackgroundResource(R.drawable.bg_note_chip_category)
                    pill.setTextColor(ContextCompat.getColor(context, R.color.tag_category_text))
                } else {
                    pill.setBackgroundResource(R.drawable.bg_note_chip_tag)
                    pill.setTextColor(ContextCompat.getColor(context, R.color.tag_pill_text))
                }
                
                binding.itemNotesContainer.addView(pill)
            }
        }
    }
}
