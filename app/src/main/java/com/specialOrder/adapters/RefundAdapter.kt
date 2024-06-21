package com.specialOrder.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.payments.Refund
import com.specialOrder.R
import com.specialOrder.databinding.ItemTransactionBinding
import com.specialOrder.fragment.orderDetail.OrderDetailFragment
import com.specialOrder.utils.Constants
import com.specialOrder.utils.convertToSymbol
import com.specialOrder.utils.formatMillisToDateTime
import com.specialOrder.utils.hideView
import com.specialOrder.utils.toDoubleFloatPoint


class RefundAdapter(
    private val data: List<Refund>
) : RecyclerView.Adapter<RefundAdapter.MyViewBinder>() {

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewBinder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            root.context.getString(R.string.priceString ,OrderDetailFragment.currencyName.convertToSymbol(), item.amount?.toDoubleFloatPoint() ).also {
                paymentAmount.text = it
            }
            paymentDate.text = item.createdTime?.formatMillisToDateTime(Constants.yearFormat)
            paymentTime.text = item.createdTime?.formatMillisToDateTime(Constants.dateFormat)
            paymentText.text = Constants.REFUNDED
            paymentTypeImage.hideView()
            paymentModeName.hideView()
            paymentText.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    R.color.other_order_payment_status
                )
            )

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)
}