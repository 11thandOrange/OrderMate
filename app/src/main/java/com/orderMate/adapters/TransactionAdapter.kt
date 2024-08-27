package com.orderMate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.payments.Payment
import com.orderMate.R
import com.orderMate.databinding.ItemTransactionBinding
import com.orderMate.fragment.orderDetail.OrderDetailFragment
import com.orderMate.utils.Constants
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.formatMillisToDateTime
import com.orderMate.utils.toDoubleFloatPoint


class TransactionAdapter(
    private val data: List<Payment>
) : RecyclerView.Adapter<TransactionAdapter.MyViewBinder>() {

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionAdapter.MyViewBinder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            root.context.getString(
                R.string.priceString,
                OrderDetailFragment.currencyName.convertToSymbol(),
                item.amount?.toDoubleFloatPoint()
            ).also {
                paymentAmount.text = it
            }
            paymentDate.text = item.createdTime?.formatMillisToDateTime(Constants.yearFormat)
            paymentTime.text = item.createdTime?.formatMillisToDateTime(Constants.dateFormat)

            paymentModeName.text = item.tender?.label
            paymentTypeImage.setImageResource(
                if (item.tender?.label?.lowercase()
                        ?.equals(Constants.cash.lowercase()) == true
                ) R.drawable.ic_cash
                else if (item.tender?.label?.lowercase()
                        ?.equals(Constants.check.lowercase()) == true
                )
                    R.drawable.ic_cheque else R.drawable.ic_card
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)
}