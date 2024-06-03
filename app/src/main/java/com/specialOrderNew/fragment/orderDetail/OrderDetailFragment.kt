package com.specialOrderNew.fragment.orderDetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.order.Discount
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.Refund
import com.specialOrderNew.R
import com.specialOrderNew.activities.MainActivity
import com.specialOrderNew.adapters.ItemAdapter
import com.specialOrderNew.adapters.RefundAdapter
import com.specialOrderNew.adapters.TransactionAdapter
import com.specialOrderNew.communicators.IOrderItemClickListener
import com.specialOrderNew.databinding.FragmentOrderDetailBinding
import com.specialOrderNew.modals.ItemModal
import com.specialOrderNew.utils.Constants
import com.specialOrderNew.utils.PreferenceManager
import com.specialOrderNew.utils.changeColorAsPerPaymentStatus
import com.specialOrderNew.utils.convertToSymbol
import com.specialOrderNew.utils.debugLog
import com.specialOrderNew.utils.exceptionHandler
import com.specialOrderNew.utils.formatMillisToDateTime
import com.specialOrderNew.utils.hideView
import com.specialOrderNew.utils.navigate
import com.specialOrderNew.utils.showView
import com.specialOrderNew.utils.toDoubleFloatPoint
import kotlin.math.abs


class OrderDetailFragment : Fragment(), IOrderItemClickListener {

    companion object {
        var isFromOrderDetails = false
        var currencyName: String = ""
        var totalItemPriceSum = 0L
        var totalItemTaxes = 0L
        var totalLineItems = 0
        var totalPriceFromApi = 0L
    }

    override fun onPause() {
        super.onPause()
        isFromOrderDetails = true
    }

    private val binding: FragmentOrderDetailBinding by lazy {
        FragmentOrderDetailBinding.inflate(layoutInflater)
    }

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }
    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private var paymentItems: MutableList<Payment> = mutableListOf()
    private var refundItems: MutableList<Refund> = mutableListOf()
    private var orderArguments: OrderDetailFragmentArgs? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderArguments = arguments?.let { OrderDetailFragmentArgs.fromBundle(it) }
        setUpTheClickListeners()
        setUpRecyclerView()
        onBackPressed()
        setDataOnScreenWithArgs()
    }


    private fun setDataOnScreenWithArgs() {
        orderArguments?.orderData?.apply {
            totalPriceFromApi = total ?: Constants.defaultLong
            currencyName = currency ?: Constants.defaultString
            "${currencyName.convertToSymbol()}${total?.toDoubleFloatPoint()}".also {
                binding.totalValue.text = it
            }
            "${getString(R.string.dummy_heading)} $id".also { binding.orderId.text = it }
            val date = createdTime?.formatMillisToDateTime(
                Constants.yearFormatWithMonthName,
                true
            ) + " . " + createdTime?.formatMillisToDateTime(Constants.dateFormat)
            binding.orderPlacedAtValue.text = date
            binding.merchantName.text = preferenceManager.getString(Constants.merchantName)

            binding.orderPlacedStatusValue.text = paymentState?.name
            binding.orderPlacedStatusValue.changeColorAsPerPaymentStatus(
                paymentState?.name ?: Constants.defaultString
            )
            exceptionHandler {
                binding.orderPlacedEmployeeValue.text =
                    employee?.jsonObject?.get("name")?.toString()
                if (customers?.isNotEmpty() == true) {
                    "${customers[0]?.firstName} ${customers[0]?.lastName}".also {
                        binding.orderPlacedCustomerValue.text = it
                    }
                    binding.orderPlacedCustomerNumberValue.text = getMobileNumber(customers[0])
                }

            }
            setUpTheScreen()
        }
    }

    private fun getMobileNumber(customer: Customer?): String {
        var result = ""

        customer?.phoneNumbers?.forEach {
            result += "${it?.phoneNumber}, "
        }
        return if (result.trim().isEmpty()) result else result.substring(0, result.length - 2)
    }

    private fun setUpRecyclerView() {
        binding.apply {
            itemRecycler.layoutManager = LinearLayoutManager(requireContext())
            val adapter = ItemAdapter(lineItems, this@OrderDetailFragment)
            itemRecycler.adapter = adapter

            transactionRecycler.layoutManager = LinearLayoutManager(requireContext())
            val transactionAdapter = TransactionAdapter(paymentItems)
            transactionRecycler.adapter = transactionAdapter

            refundRecycler.layoutManager = LinearLayoutManager(requireContext())
            val refundAdapter = RefundAdapter(refundItems)
            refundRecycler.adapter = refundAdapter

        }
    }

    private fun setUpTheScreen() {
        showLineItemOnScreen()
        showThePaymentData()
        showTheDiscountAndTaxData()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showThePaymentData() {

        // check if no transaction and refund is made

        if (isRefundAndPaymentMade()) {
            binding.transactionRecycler.hideView()
            binding.refundRecycler.hideView()
            binding.noTransactionText.showView()
            return
        }

        // setup the payment data
        if (orderArguments?.orderData?.payments == null || orderArguments?.orderData?.payments?.isEmpty() == true) {
            binding.transactionRecycler.hideView()
        } else {
            paymentItems.clear()
            orderArguments?.orderData?.payments?.forEach {
                paymentItems.add(it)
            }
            binding.transactionRecycler.adapter?.notifyDataSetChanged()
        }

        // setUp the refund data
        if (orderArguments?.orderData?.refunds == null || orderArguments?.orderData?.refunds?.isEmpty() == true) {
            binding.refundRecycler.hideView()
        } else {
            refundItems.clear()
            orderArguments?.orderData?.refunds?.forEach {
                refundItems.add(it)
            }
            binding.refundRecycler.adapter?.notifyDataSetChanged()
        }
    }

    private fun isRefundAndPaymentMade(): Boolean {
        return (orderArguments?.orderData?.payments?.isEmpty() == true || orderArguments?.orderData?.payments == null) &&
                (orderArguments?.orderData?.refunds?.isEmpty() == true || orderArguments?.orderData?.refunds == null)
    }

    private fun showTheDiscountAndTaxData() {
        Log.e("CheckingOrderArguments", "showTheDiscountAndTaxData: $orderArguments")
        // case 1 if no discount is applied then there will be tax applied
        if (orderArguments?.orderData?.discounts == null || orderArguments?.orderData?.discounts?.isEmpty() == true) {
            "${currencyName.convertToSymbol()}${abs(totalItemPriceSum - totalPriceFromApi).toDoubleFloatPoint()}".also {
                binding.taxValue.text = it
            }
            binding.discount.hideView()
            binding.discountValue.hideView()
            return
        }


        // case 2 only Discount is applied then
        exceptionHandler {
            val discountName = try {
                orderArguments?.orderData?.discounts?.get(0)?.jsonObject?.get("name")
            } catch (e: Exception) {
                binding.discount.hideView()
                binding.discountValue.hideView()
                binding.tax.hideView()
                binding.taxValue.hideView()
            }

            val discountValue = getTheDiscountedValue(orderArguments?.orderData?.discounts?.get(0))


            if (discountValue == null) {
                binding.discount.hideView()
                binding.discountValue.hideView()
                binding.tax.hideView()
                binding.taxValue.hideView()
                return@exceptionHandler
            }

            val value1 = discountValue.toLong().toDoubleFloatPoint().toDouble()
            val value2 = orderArguments?.orderData?.total?.toDoubleFloatPoint()?.toDouble()
            val calculateValue = value2?.plus(abs(value1 ?: Constants.defaultDouble))
            // case 2  if  tax is applied then there will be discount applied
            if (calculateValue != totalPriceFromApi.toDoubleFloatPoint().toDouble()) {
                val value = calculateValue?.minus(totalItemPriceSum.toDoubleFloatPoint().toDouble())
                    ?.toDoubleFloatPoint()
                "${currencyName.convertToSymbol()}${value}".also {
                    binding.taxValue.text = it
                }
            } else {
                binding.tax.hideView()
                binding.taxValue.hideView()
            }

            binding.discount.text = discountName.toString()
            "-${currencyName.convertToSymbol()}${
                discountValue.toLong().toDoubleFloatPoint()
            }".also {
                binding.discountValue.text = it
            }
        }
    }

    private fun getTheDiscountedValue(discount: Discount?): String? {
        try {
            val value = discount?.jsonObject?.get("percentage").toString()
            val calculatedValue = (totalItemPriceSum.times(value.toInt()) / 100)
            return calculatedValue.toString()
        } catch (e: Exception) {
            binding.tax.hideView()
            binding.taxValue.hideView()
            binding.discount.hideView()
            binding.discountValue.hideView()
        }
        return null
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showLineItemOnScreen() {
        binding.syncingText.hideView()
        binding.apply {
            lineItems.clear()
            val data = orderArguments?.orderData?.lineItems?.let { countElementsByUniqueKeys(it) }
            data?.forEach {
                totalItemPriceSum += it.order?.price?.times(it.itemCount) ?: Constants.defaultLong
                lineItems.add(it)
            }
            "${currencyName.convertToSymbol()}${totalItemPriceSum.toDoubleFloatPoint()}".also {
                subtotalValue.text = it
            }
            "${getString(R.string.order)}(${orderArguments?.orderData?.lineItems?.size})".also {
                orderItemCountText.text = if (it == "null") {
                    itemRecycler.hideView()
                    getString(R.string.order)
                } else {
                    it
                }

            }
            itemRecycler.adapter?.notifyDataSetChanged()
            // if the total price is equal to sum of cost of each item then we will not do anything
        }
    }


    private fun countElementsByUniqueKeys(lineItems: List<LineItem?>): MutableList<ItemModal> {
        val elementCounts: MutableSet<ItemModal> = mutableSetOf()
        var isMatched: Boolean
        for (lineItem in lineItems) {
            isMatched = false
            val key = "${lineItem?.name}-${lineItem?.item?.id}"
            val data = ItemModal(lineItem, key)

            for (it in elementCounts) {
                if (it.orderKey == key) {
                    it.itemCount = it.itemCount + 1
                    isMatched = true
                    break
                }
            }
            if (!isMatched) {
                data.itemCount = 1
                elementCounts.add(data)
            }
        }
        return elementCounts.toMutableList()
    }


    private fun setUpTheClickListeners() {
        binding.apply {
            backButton.setOnClickListener { navigate(R.id.action_orderDetailFragment_to_orderHistoryFragment) }
        }
    }

    private fun freeVariables() {
        totalItemPriceSum = 0
        totalPriceFromApi = 0
        totalLineItems = 0
    }

    override fun onStop() {
        super.onStop()
        freeVariables()

    }

    override fun onOrderItemClick(orderPosition: Int) {
        "not implemented".debugLog(javaClass.simpleName)
    }

    private fun onBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigate(R.id.action_orderDetailFragment_to_orderHistoryFragment)
            }
        }
        if (activity != null)
            (activity as MainActivity).onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )
    }

}