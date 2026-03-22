package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
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
