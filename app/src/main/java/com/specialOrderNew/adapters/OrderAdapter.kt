package com.specialOrderNew.adapters

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Refund
import com.specialOrderNew.R
import com.specialOrderNew.communicators.IOrderItemClickListener
import com.specialOrderNew.databinding.ItemOrderBinding
import com.specialOrderNew.utils.Constants
import com.specialOrderNew.utils.changeColorAsPerPaymentStatus
import com.specialOrderNew.utils.convertToSymbol
import com.specialOrderNew.utils.exceptionHandler
import com.specialOrderNew.utils.formatMillisToDateTime
import com.specialOrderNew.utils.toDoubleFloatPoint
import com.specialOrderNew.utils.toDoubleFloatPointLatest


class OrderAdapter(
    private val data: ArrayList<Order?>,
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

            exceptionHandler {
                orderEmployeeName.text = item?.employee?.jsonObject?.get("name").toString()

            }
            orderType.text = if (item?.orderType?.label?.trim()?.isEmpty() == false) {
                 item.orderType.label
            }else{
                root.context.getString(R.string.hypen_text)
            }

            orderSource.text = if(item?.isNotNullOnlineOrder == true) "Online" else "POS"
            orderDate.text = item?.createdTime?.formatMillisToDateTime(Constants.yearFormat)
            orderTime.text = item?.createdTime?.formatMillisToDateTime(Constants.dateFormat)


            orderStatus.text = item?.paymentState?.name
            item?.paymentState?.name?.let { orderStatus.changeColorAsPerPaymentStatus(it) }

            orderId.text = item?.id
            if (item?.paymentState?.name?.lowercase() == Constants.REFUNDED.lowercase()) {
                val refundedValue = item.refunds?.let { getTheRefundedValue(it) }
                ("${item.currency?.convertToSymbol()}${
                    item.total?.toInt()?.toDoubleFloatPoint()
                }\n" +
                        "(${item.currency?.convertToSymbol()}${refundedValue})").also {
                    orderPrice.text = changeTheColor(it)
                }
            } else {
                "${item?.currency?.convertToSymbol()}${
                    item?.total?.toInt()?.toDoubleFloatPoint()
                }".also {
                    orderPrice.text = it
                }
            }


            "${item?.customers?.get(0)?.firstName} ${item?.customers?.get(0)?.lastName}".also {
                if (it.trim().isEmpty() || item?.customers?.get(0)?.firstName?.trim()
                        ?.isEmpty() == true
                ) {
                    orderCustomerName.text = root.context.getString(R.string.hypen_text)
                } else {
                    orderCustomerName.text =
                        if (it.contains("null")) root.context.getString(R.string.hypen_text) else it
                }
            }

        }
    }

    private fun getPaymentTypes(payments: List<com.clover.sdk.v3.payments.Payment>?): String {
        var result = ""

        for ((_, element) in payments?.withIndex() ?: mutableListOf()) {
            if (result.contains(element.tender?.label ?: "")) {
                continue
            }
            result += "${element.tender?.label}, "
        }
        if (result.trim().isNotEmpty()) {
            result = result.trim().subSequence(0, result.length - 2).toString()
        }
        return result
    }


    private fun changeTheColor(text: String): SpannableString {
        val spannableString = SpannableString(text)
        try {
            val startIndex = text.indexOf("(")
            val endIndex = text.indexOf(")")

            // Set the color for the first half of the string
            val firstHalfColor = Color.RED
            spannableString.setSpan(
                ForegroundColorSpan(firstHalfColor),
                startIndex,
                endIndex + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set the color for the second half of the string
            val secondHalfColor = Color.BLACK
            spannableString.setSpan(
                ForegroundColorSpan(secondHalfColor),
                0,
                startIndex - 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return spannableString
    }

    private fun getTheRefundedValue(refunds: List<Refund>): String {
        var resultant = 0.0
        refunds.forEach {
            resultant += it.amount
        }
        return resultant.toDoubleFloatPointLatest()
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