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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderDetailItemBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.modals.ItemModal
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetType
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderNoteParser
import com.orderMate.utils.WidgetColorUtils
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.convertToTwoDecimal
import com.orderMate.utils.toDoubleFloatPoint

/**
 * Item Adapter for order detail - matches HTML .detail-item-row styling
 * - Background: rgba(0,0,0,0.2) with 10dp border radius
 * - Clickable rows with chevron indicator
 * - Note pills for categories and tags
 * - Opens OrderMate popup on click
 * - #59: Dynamic item icons from Clover (fallback to misc icon)
 */
class ItemAdapter(
    private val data: MutableList<ItemModal?>,
    private val listener: IOrderItemClickListener,
    private val merchantId: String? = null
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

            // #59: Load item icon from Clover if available, otherwise use generic misc icon
            loadItemIcon(context, item?.order?.item?.id, binding.itemIcon)

            // Note pills
            setupNotePills(context, item?.order?.note)

            // Click listener - entire row opens OrderMate popup
            binding.root.setOnClickListener {
                listener.onOrderItemClick(position, item?.order?.item?.id)
            }
        }

        /**
         * #59: Load item icon from Clover image URL if available
         * Falls back to generic misc icon if no image or on error
         */
        private fun loadItemIcon(context: Context, itemId: String?, imageView: ImageView) {
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

        private fun setupNotePills(context: Context, noteString: String?) {
            val container = binding.itemNotesContainer
            container.removeAllViews()
            
            android.util.Log.d("ItemPillDebug", "========== ITEM PILL DEBUG ==========")
            android.util.Log.d("ItemPillDebug", "noteString: '$noteString'")
            
            if (noteString.isNullOrEmpty() || noteString.trim().isEmpty()) {
                android.util.Log.d("ItemPillDebug", "EARLY RETURN: noteString is null/empty")
                return
            }

            val density = context.resources.displayMetrics.density
            
            // Use cached widgets only for pill rendering (never defaults)
            val itemLevelWidgets = WidgetManager.getInstance(context).getCachedItemWidgets()
            
            android.util.Log.d("ItemPillDebug", "itemLevelWidgets count: ${itemLevelWidgets.size}")
            itemLevelWidgets.forEach { widget ->
                android.util.Log.d("ItemPillDebug", "  Widget: id=${widget.id}, label=${widget.label}, type=${widget.type}, level=${widget.level}, enabled=${widget.isEnabled}")
            }
            
            val parsedTags = OrderNoteParser.extractTagsFromNote(noteString, itemLevelWidgets, NoteLevel.ITEM, includeTextBox = true)
            
            android.util.Log.d("ItemPillDebug", "parsedTags count: ${parsedTags.size}")
            parsedTags.forEach { tag ->
                android.util.Log.d("ItemPillDebug", "  Tag: label=${tag.label}, value=${tag.value}, type=${tag.type}, widgetType=${tag.widgetType}")
                addPillView(context, container, tag.value, tag.widgetType, density)
            }
            android.util.Log.d("ItemPillDebug", "======================================")
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
            val iconRes = WidgetColorUtils.getIconForWidgetType(widgetType)
            
            // Truncate to 12 chars, single line, no newlines
            val displayText = value.replace("\n", " ").take(12).let {
                if (value.length > 12) "$it..." else it
            }
            pillText.text = displayText
            pillText.maxLines = 1
            pillText.setTextColor(color)
            pillIcon.setImageResource(iconRes)
            pillIcon.setColorFilter(color)
            
            // Unified pill background: 15% opacity + 25% border
            pillView.background = WidgetColorUtils.createPillBackground(color, 10f, density)
            
            container.addView(pillView)
        }
    }
}
