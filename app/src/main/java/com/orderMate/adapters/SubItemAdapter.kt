package com.orderMate.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Modification
import com.orderMate.R
import com.orderMate.databinding.ItemOrderSubItemsBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.toDoubleFloatPoint

class SubItemAdapter(
    private val data: MutableList<Modification?>,
) :
    RecyclerView.Adapter<SubItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewBinder {
        val binding =
            ItemOrderSubItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            orderItemName.text = item?.name
            root.context.getString(
                R.string.priceString,
                OrderDetailFragment.currencyName.convertToSymbol(),
                item?.amount?.toDoubleFloatPoint()?.toDouble()?.times(1).toString()
            ).also {
                orderPrice.text = it
            }

        }

    }


    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemOrderSubItemsBinding) :
        RecyclerView.ViewHolder(binding.root)


}