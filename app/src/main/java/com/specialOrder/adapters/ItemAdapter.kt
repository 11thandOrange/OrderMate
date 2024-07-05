package com.specialOrder.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.specialOrder.R
import com.specialOrder.communicators.IOrderItemClickListener
import com.specialOrder.databinding.ListitemOrderBinding
import com.specialOrder.fragment.orderDetail.OrderDetailFragment
import com.specialOrder.modals.ItemModal
import com.specialOrder.utils.Constants
import com.specialOrder.utils.convertToSymbol
import com.specialOrder.utils.convertToTwoDecimal
import com.specialOrder.utils.exceptionHandler
import com.specialOrder.utils.hideView
import com.specialOrder.utils.showView
import com.specialOrder.utils.toDoubleFloatPoint

class ItemAdapter(
    private val data: MutableList<ItemModal?>,
    private val listener: IOrderItemClickListener
) :
    RecyclerView.Adapter<ItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.MyViewBinder {
        val binding = ListitemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: ItemAdapter.MyViewBinder, position: Int) {

        if(data.isEmpty()){
            return
        }

        val item = data[position]
        holder.binding.apply {
            root.context.getString(R.string.itemCount, item?.itemCount)
                .also { orderItemCount.text = it }
            orderItemName.text = item?.order?.name


            root.context.getString(
                R.string.priceString, OrderDetailFragment.currencyName.convertToSymbol(),
                item?.order?.price?.toDoubleFloatPoint()?.toDouble()?.times(item.itemCount)
                    .toString().convertToTwoDecimal()
            )
                .also {
                    orderPrice.text = it
                }

            exceptionHandler {
                item?.order?.colorCode?.let {
                    orderItemColor.setBackgroundColor(
                        Color.parseColor(item.order.colorCode ?: Constants.defaultString)
                    )
                }
            }
            // means these item are unit wise eg kg etc
            if (item?.order?.hasUnitName() == true && item.order.hasUnitQty()) {
                orderItemQuantity.showView()
                orderItemQuantity.text = root.context.getString(
                    R.string.subItemCost, item.order.unitQty?.div(1000),
                    item.order.unitName, item.order.price?.toDoubleFloatPoint(), item.order.unitName
                ).also {
                    orderItemQuantity.text = it
                }
                root.context.getString(
                    R.string.priceString, OrderDetailFragment.currencyName.convertToSymbol(),
                    item.order.price?.toDoubleFloatPoint()?.toDouble()?.times(item.itemCount)
                        ?.times((item.order.unitQty ?:1000 )/ 1000)
                        ?.toDoubleFloatPoint()
                        .toString().convertToTwoDecimal()
                )
                    .also {
                        orderPrice.text = it
                    }

            } else {
                root.context.getString(
                    R.string.priceString, OrderDetailFragment.currencyName.convertToSymbol(),
                    item?.order?.price?.toDoubleFloatPoint()?.toDouble()?.times(item.itemCount)
                        .toString().convertToTwoDecimal()
                )
                    .also {
                        orderPrice.text = it
                    }
                orderItemQuantity.hideView()
            }

            // set up the item quantity values
            subItemRecycler.isVisible = (item?.order?.hasModifications() == true)


            // set up sub item adapter
            if (item?.order?.modifications != null && item.order.modifications?.isNotEmpty() == true) {
                subItemRecycler.layoutManager = LinearLayoutManager(root.context)
                val adapter = SubItemAdapter(item.order.modifications ?: mutableListOf())
                subItemRecycler.adapter = adapter
            }
            val btnVisibility =
                item?.order?.note == null || item.order.note?.trim()?.isEmpty() == true
            editNote.isVisible = !btnVisibility
            addNotesButton.isVisible = btnVisibility
            orderNotes.isVisible = !btnVisibility
            orderNotes.text = item?.order?.note

            addNotesButton.setOnClickListener {
                listener.onOrderItemClick(position, item?.order?.item?.id)
            }

            editNote.setOnClickListener {
                listener.onOrderItemClick(position, item?.order?.item?.id)
            }
        }
    }





    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ListitemOrderBinding) :
        RecyclerView.ViewHolder(binding.root)


}