package com.orderMate.adapters

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.Refund
import com.orderMate.R
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.ItemOrderBinding
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.changeColorAsPerPaymentStatus
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.formatMillisToDateTime
import com.orderMate.utils.generateString
import com.orderMate.utils.getThePaymentState
import com.orderMate.utils.toDoubleFloatPoint
import com.orderMate.utils.toDoubleFloatPointLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OrderAdapter(
    private val data: ArrayList<Order?>,
    private val listener: IOrderItemClickListener
) :
    RecyclerView.Adapter<OrderAdapter.MyViewBinder>() {

    companion object {
        var resultantPickupDate = ""
    }

    // To prevent the unnecessary data read operations

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderAdapter.MyViewBinder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: OrderAdapter.MyViewBinder, position: Int) {
        holder.binding.apply {
            if (position >= data.size) {
                return
            }
            val item = data[position]
            // Get employee name - try jsonObject first, then use cached lookup
            val employeeName = item?.employee?.jsonObject?.get(Constants.name)?.toString()
                ?: item?.employee?.id?.let { MyApp.getInstance().getCachedEmployeeName(it) }
            orderEmployeeName.text = employeeName ?: root.context.getString(R.string.dash)
            orderType.text = if (item?.orderType?.label?.trim()?.isEmpty() == false) {
                item.orderType.label
            } else {
                root.context.getString(R.string.hypen_text)
            }

            orderSource.text =
                if (item?.isNotNullOnlineOrder == true) Constants.online else Constants.pos
            orderDate.text = item?.createdTime?.formatMillisToDateTime(Constants.yearFormat)
            orderTime.text = item?.createdTime?.formatMillisToDateTime(Constants.dateFormat)


            orderStatus.text = root.context.getThePaymentState(item)
            orderStatus.changeColorAsPerPaymentStatus(item)

            orderId.text = item?.id
            setupPaymentValues(this, item)

            if (item?.customers?.isNotEmpty() == true) {
                root.context.getString(
                    R.string.getFullName,
                    item.customers?.get(0)?.firstName,
                    item.customers?.get(0)?.lastName
                ).also {
                    if (it.trim().isEmpty() || item.customers?.get(0)?.firstName?.trim()
                            ?.isEmpty() == true
                    ) {
                        orderCustomerName.text = root.context.getString(R.string.hypen_text)
                    } else {
                        orderCustomerName.text =
                            if (it.contains(Constants.nullValue)) root.context.getString(R.string.hypen_text) else it
                    }
                }
            } else {
                orderCustomerName.text = root.context.getString(R.string.hypen_text)
            }
            val hasNotes = doesLineItemContainNotes(item?.lineItems, root.context)
            orderTenderTypes.text = getTenderTypes(item?.payments, root.context)
            orderNotes.text = generateString(item?.lineItems ?: emptyList(), root.context, true)
            customOrdersCount.text =
                if (hasNotes.first == 0) root.context.getString(R.string.dash) else root.context.getString(
                    if (hasNotes.first == 1) R.string.item_count_single else R.string.item_count,
                    hasNotes.first.toString()
                )
        }
    }

    private fun getTenderTypes(payments: List<Payment>?, context: Context): CharSequence {
        var result = ""
        payments?.forEach {
            if (it.tender?.label?.isNotEmpty() == true && !result.contains(
                    it.tender?.label ?: Constants.defaultString
                )
            ) {
                result += "${it.tender?.label} ,"
            }
        }
        return if (result.trim()
                .isEmpty()
        ) context.getString(R.string.dash) else result.substring(0, result.length - 2)
    }

    private fun doesLineItemContainNotes(
        lineItems: List<LineItem>?,
        context: Context
    ): Pair<Int, String> {
        var result = ""
        var count = 0
        for (i in lineItems ?: emptyList()) {
            if (i.note != null && i.note.trim().isNotEmpty()) {
                count++
                result += "${i.note}, "
            }
        }
        return if (result.trim().isEmpty()) Pair(0, context.getString(R.string.dash)) else Pair(
            count,
            result.substring(0, result.length - 2)
        )
    }

    private fun setupPaymentValues(reference: ItemOrderBinding, item: Order?) {
        reference.apply {
            if (item?.paymentState?.name?.lowercase() == Constants.REFUNDED.lowercase()
                || item?.paymentState?.name?.lowercase() == Constants.PARTIALLY_REFUNDED.lowercase()
            ) {
                val refundedValue = item.refunds?.let { getTheRefundedValue(it) }
                val requiredString = root.context.getString(
                    R.string.priceStringWithRefund,
                    item.currency?.convertToSymbol(),
                    item.total?.toInt()?.toDoubleFloatPoint(),
                    item.currency?.convertToSymbol(),
                    refundedValue
                )
                orderPrice.text = changeTheColor(requiredString)

            } else {

                orderPrice.text = root.context.getString(
                    R.string.priceString,
                    item?.currency?.convertToSymbol(),
                    item?.total?.toInt()?.toDoubleFloatPoint()
                )
            }
        }
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
                listener.onOrderItemClick(adapterPosition, null)
            }
            binding.orderNotes.setOnClickListener {
                listener.onOrderItemClick(adapterPosition, null)
            }
        }
    }


}