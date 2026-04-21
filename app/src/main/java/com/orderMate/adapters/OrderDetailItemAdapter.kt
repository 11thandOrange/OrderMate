package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.clover.sdk.v3.order.LineItem
import com.orderMate.R
import com.orderMate.databinding.ItemOrderDetailItemBinding
import com.orderMate.modals.NoteLevel
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.toDoubleFloatPoint

/**
 * Adapter for order detail items with note pills (#87 requirement)
 * Displays items with icon, name, pills, quantity, and price
 * Clicking row opens OrderMate editor popup
 * #59: Dynamic item icons from Clover (fallback to misc icon)
 */
class OrderDetailItemAdapter(
    private val items: List<LineItem>,
    private val onItemClick: ((position: Int, lineItemId: String?) -> Unit)? = null,
    private val merchantId: String? = null
) : RecyclerView.Adapter<OrderDetailItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderDetailItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= items.size) return
        val item = items[position]
        holder.bind(item, merchantId)
        
        // Set click listener for entire row - opens OrderMate editor popup
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position, item.item?.id)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemOrderDetailItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LineItem, merchantId: String?) {
            val context = binding.root.context

            // Item name
            binding.itemName.text = item.name ?: "-"

            // Quantity
            val qty = item.unitQty ?: 1
            binding.itemQty.text = "x$qty"

            // Price
            val price = item.price?.toInt()?.toDoubleFloatPoint() ?: "0.00"
            binding.itemPrice.text = "$$price"

            // #59: Load item icon from Clover if available, otherwise use generic misc icon
            loadItemIcon(context, item.item?.id, binding.itemIcon, merchantId)

            // Note pills
            setupNotePills(context, item)
        }

        /**
         * #59: Load item icon from Clover image URL if available
         * Falls back to generic misc icon if no image or on error
         */
        private fun loadItemIcon(context: Context, itemId: String?, imageView: ImageView, merchantId: String?) {
            val mId = merchantId ?: MyApp.getInstance().getMerchantId()
            
            if (mId != null && !itemId.isNullOrEmpty()) {
                // Clover item image URL pattern
                val imageUrl = "https://www.clover.com/p/items/$mId/$itemId/image_120.jpg"
                
                Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_category_misc)
                    .error(R.drawable.ic_category_misc)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            } else {
                // No merchantId or itemId - use fallback icon
                imageView.setImageResource(R.drawable.ic_category_misc)
            }
        }

        /**
         * Setup item-level note pills using widget-based parsing.
         * Renders all 4 widget types (SINGLE_SELECT, MULTI_SELECT, CALENDAR, TEXT_BOX).
         */
        private fun setupNotePills(context: Context, item: LineItem) {
            binding.itemNotesContainer.removeAllViews()
            
            val noteString = item.note?.trim()
            if (noteString.isNullOrEmpty()) return

            // Get item-level widgets for parsing
            val widgets = WidgetManager.getCachedWidgets()
            val itemLevelWidgets = widgets.filter { it.level == NoteLevel.ITEM }
            
            if (itemLevelWidgets.isEmpty()) return
            
            // Parse using widget-based approach (all 4 types including TEXT_BOX)
            val density = context.resources.displayMetrics.density
            val parsedTags = OrderNoteParser.extractTagsFromNote(noteString, itemLevelWidgets, NoteLevel.ITEM, includeTextBox = true)
            
            parsedTags.forEach { tag ->
                val pillView = LayoutInflater.from(context)
                    .inflate(R.layout.item_note_pill, binding.itemNotesContainer, false) as LinearLayout
                
                val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
                val pillText = pillView.findViewById<TextView>(R.id.pillText)
                
                val color = WidgetColorUtils.getColorForWidgetType(tag.widgetType)
                val iconRes = WidgetColorUtils.getIconForWidgetType(tag.widgetType)
                
                // Truncate TEXT_BOX values to 12 chars for compact display
                val displayText = tag.value.replace("\n", " ").take(12).let {
                    if (tag.value.length > 12) "$it..." else it
                }
                
                pillText.text = displayText
                pillText.maxLines = 1
                pillText.setTextColor(color)
                pillIcon.setImageResource(iconRes)
                pillIcon.setColorFilter(color)
                
                pillView.background = WidgetColorUtils.createPillBackground(color, 10f, density)
                
                binding.itemNotesContainer.addView(pillView)
            }
        }
    }
}
