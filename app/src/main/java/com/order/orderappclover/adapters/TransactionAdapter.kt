package com.order.orderappclover.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.order.orderappclover.R
import com.order.orderappclover.databinding.ItemTransactionBinding
import com.order.orderappclover.fragment.orderDetail.OrderDetailFragment
import com.order.orderappclover.modals.orderDetail.PaymentElement
import com.order.orderappclover.utils.Constants
import com.order.orderappclover.utils.convertToSymbol
import com.order.orderappclover.utils.formatMillisToDateTime
import com.order.orderappclover.utils.hideView
import com.order.orderappclover.utils.toDoubleFloatPoint


class TransactionAdapter(
    private val data: List<PaymentElement>
) : RecyclerView.Adapter<TransactionAdapter.MyViewBinder>() {

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionAdapter.MyViewBinder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
                "${OrderDetailFragment.currency.convertToSymbol()}${item.amount?.toDoubleFloatPoint()}".also {
                    paymentAmount.text = it
                }
                paymentDate.text = item.createdTime?.formatMillisToDateTime(Constants.yearFormat)
                paymentTime.text = item.createdTime?.formatMillisToDateTime(Constants.dateFormat)

            if(item.id != null){
                paymentModeName.text = item.tender?.label
                paymentTypeImage.setImageResource(
                    if (item.tender?.label?.lowercase()?.equals(Constants.cash.lowercase()) == true) R.drawable.ic_cash
                    else if (item.tender?.label?.lowercase()?.equals(Constants.check.lowercase()) == true)
                        R.drawable.ic_cheque else R.drawable.ic_card
                )
            }else{
                paymentModeName.hideView()
                paymentTypeImage.hideView()
                paymentText.text = Constants.REFUNDED
                paymentText.setTextColor(ContextCompat.getColor(root.context,R.color.other_order_payment_status))
            }

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)
}