package com.order.orderappclover.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.order.orderappclover.communicators.IOrderItemClickListener
import com.order.orderappclover.databinding.ListitemOrderBinding
import com.order.orderappclover.fragment.orderDetail.OrderDetailFragment
import com.order.orderappclover.modals.orderDetail.Element
import com.order.orderappclover.modals.orderDetail.LineItemElement
import com.order.orderappclover.utils.convertToSymbol
import com.order.orderappclover.utils.toDoubleFloatPoint

class ItemAdapter(
    private val data: List<LineItemElement>,
    private val listener: IOrderItemClickListener
) :
    RecyclerView.Adapter<ItemAdapter.MyViewBinder>() {

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.MyViewBinder {
        val binding =
            ListitemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: ItemAdapter.MyViewBinder, position: Int) {
        val item = data[position]

        holder.binding.apply {
            "X${item.itemCount}".also { orderItemCount.text = it }
            orderItemName.text = item.name

            "${OrderDetailFragment.currency.convertToSymbol()}${
                item.price?.toDoubleFloatPoint()?.toDouble()?.times(item.itemCount ?: 1).toString()
            }".also {
                orderPrice.text = it
            }
            item.colorCode?.let {
                orderItemColor.dividerColor = Color.parseColor(item.colorCode)
            }
        }
    }


    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ListitemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onOrderItemClick(adapterPosition)
            }
        }
    }


}