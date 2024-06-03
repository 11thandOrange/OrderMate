package com.specialOrderNew.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.specialOrderNew.communicators.IOrderItemClickListener
import com.specialOrderNew.databinding.ListitemOrderBinding
import com.specialOrderNew.fragment.orderDetail.OrderDetailFragment
import com.specialOrderNew.modals.ItemModal
import com.specialOrderNew.utils.Constants
import com.specialOrderNew.utils.convertToSymbol
import com.specialOrderNew.utils.toDoubleFloatPoint

class ItemAdapter(
    private val data: MutableList<ItemModal?>,
    private val listener: IOrderItemClickListener
) :
    RecyclerView.Adapter<ItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.MyViewBinder {
        val binding =
            ListitemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: ItemAdapter.MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            "X${item?.itemCount}".also { orderItemCount.text = it }
            orderItemName.text = item?.order?.name

            "${OrderDetailFragment.currencyName.convertToSymbol()}${
                item?.order?.price?.toDoubleFloatPoint()?.toDouble()?.times(item.itemCount)
                    .toString()
            }".also {
                orderPrice.text = it
            }

            item?.order?.colorCode?.let {
                orderItemColor.setBackgroundColor(
                    Color.parseColor(item.order.colorCode ?: Constants.defaultString)
                )
            }

            // set up sub item adapter
            if (item?.order?.modifications != null && item.order.modifications?.isNotEmpty() == true) {
                subItemRecycler.layoutManager = LinearLayoutManager(root.context)
                val adapter = SubItemAdapter(item.order.modifications ?: mutableListOf())
                subItemRecycler.adapter = adapter
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