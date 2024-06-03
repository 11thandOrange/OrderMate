package com.order.orderappclover.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.order.orderappclover.communicators.IOrderItemClickListener
import com.order.orderappclover.databinding.ItemOrderBinding
import com.order.orderappclover.fragment.orderHistory.OrderHistoryFragment
import com.order.orderappclover.modals.orderDetail.OrderPayments
import com.order.orderappclover.modals.orderHistory.OrderItems
import com.order.orderappclover.modals.orderHistory.Payments
import com.order.orderappclover.utils.Constants
import com.order.orderappclover.utils.changeColorAsPerPaymentStatus
import com.order.orderappclover.utils.convertToSymbol
import com.order.orderappclover.utils.formatMillisToDateTime
import com.order.orderappclover.utils.hideView
import com.order.orderappclover.utils.showView
import com.order.orderappclover.utils.toDoubleFloatPoint


class OrderAdapter(
    private val data: ArrayList<OrderItems>,
    private val listener: IOrderItemClickListener
) :
    RecyclerView.Adapter<OrderAdapter.MyViewBinder>() {

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderAdapter.MyViewBinder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: OrderAdapter.MyViewBinder, position: Int) {
        val item = data[position]

        holder.binding.apply {
            orderName.text = getEmployeeName(item)
            orderDate.text = item.createdTime?.formatMillisToDateTime(Constants.yearFormat)
            orderTime.text = item.createdTime?.formatMillisToDateTime(Constants.dateFormat)

            if(item.paymentState?.lowercase().equals(Constants.PAID.lowercase()) && item.total == 0){
                orderStatus.text = Constants.REFUNDED
                item.paymentState = Constants.REFUNDED
                orderStatus.changeColorAsPerPaymentStatus(item.paymentState ?: Constants.defaultString , true)
            }else{
                orderStatus.text = item.paymentState
                orderStatus.changeColorAsPerPaymentStatus(item.paymentState ?: Constants.defaultString)
            }
            orderId.text = item.id
            "${item.currency?.convertToSymbol()}${item.total?.toDoubleFloatPoint()}".also {
                orderPrice.text = it
            }
            val doesUserMadePayment = getPaymentTypes(item.payments)
            if (doesUserMadePayment.trim().isEmpty()) {
                orderPaymentTypes.hideView()
            } else {
                orderPaymentTypes.showView()
                orderPaymentTypes.text = doesUserMadePayment
            }
        }
    }

    private fun getPaymentTypes(payments: OrderPayments?): String {
        payments?.elements?.size
        var result = ""

        for ((_, element) in payments?.elements?.withIndex() ?: mutableListOf()) {
            if(result.contains(element?.tender?.label?:"")){
                continue
            }
            result += "${element?.tender?.label}, "
        }
        if(result.trim().isNotEmpty()){
            result = result.trim().subSequence(0 , result.length-2).toString()
        }
        return result
    }

    private fun getEmployeeName(item: OrderItems): String {
        OrderHistoryFragment.employeeMapList.forEach {
            if (it.id == item.employee?.id) {
                return it.name ?: Constants.defaultString
            }
        }
        return Constants.defaultString
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onOrderItemClick(adapterPosition)
            }
        }
    }


}