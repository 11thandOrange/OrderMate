package com.order.orderappclover.fragment.orderDetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.order.orderappclover.R
import com.order.orderappclover.activities.MainActivity
import com.order.orderappclover.adapters.ItemAdapter
import com.order.orderappclover.adapters.TransactionAdapter
import com.order.orderappclover.communicators.IOrderItemClickListener
import com.order.orderappclover.databinding.FragmentOrderDetailBinding
import com.order.orderappclover.modals.orderDetail.GetOrderLineItems
import com.order.orderappclover.modals.orderDetail.LineItemElement
import com.order.orderappclover.modals.orderDetail.OrderDetailRequest
import com.order.orderappclover.modals.orderDetail.OrderDiscountRequest
import com.order.orderappclover.modals.orderDetail.OrderDiscountResponse
import com.order.orderappclover.modals.orderDetail.OrderPaymentRequest
import com.order.orderappclover.modals.orderDetail.OrderPaymentResponse
import com.order.orderappclover.modals.orderDetail.OrderRefundResponse
import com.order.orderappclover.modals.orderDetail.PaymentElement
import com.order.orderappclover.utils.Constants
import com.order.orderappclover.utils.OrderApiTypes
import com.order.orderappclover.utils.PreferenceManager
import com.order.orderappclover.utils.changeColorAsPerPaymentStatus
import com.order.orderappclover.utils.convertToSymbol
import com.order.orderappclover.utils.debugLog
import com.order.orderappclover.utils.formatMillisToDateTime
import com.order.orderappclover.utils.getExpandFilterList
import com.order.orderappclover.utils.hideView
import com.order.orderappclover.utils.navigate
import com.order.orderappclover.utils.showView
import com.order.orderappclover.utils.toDoubleFloatPoint
import com.order.orderappclover.utils.toIntPoint
import com.order.orderappclover.utils.toIntPointResultInt
import kotlin.math.abs


class OrderDetailFragment : Fragment(), IOrderItemClickListener {

    companion object {
        var isFromOrderDetails = false
        var currency: String = ""
        var totalItemPriceSum = 0
        var totalPriceFromApi = 0
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
    private var lineItems: MutableList<LineItemElement> = mutableListOf()
    private var paymentItems: MutableList<PaymentElement> = mutableListOf()


    private lateinit var viewModel: OrderDetailViewModel
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
        setUpVariables()
        setUpObservers()
        setUpRecyclerView()
        onBackPressed()
        setDataOnScreenWithArgs()
    }


    private fun setDataOnScreenWithArgs() {
        orderArguments?.apply {
            totalPriceFromApi = total
            currency = curr
            "${curr.convertToSymbol()}${total.toDoubleFloatPoint()}".also {
                binding.totalValue.text = it
            }
            "${getString(R.string.dummy_heading)} $order".also { binding.orderId.text = it }
            val date = created.formatMillisToDateTime(
                Constants.yearFormatWithMonthName,
                true
            ) + " . " + created.formatMillisToDateTime(Constants.dateFormat)
            binding.orderPlacedAtValue.text = date
            binding.merchantName.text = preferenceManager.getString(Constants.merchantName)

            binding.orderPlacedStatusValue.text = orderArguments?.orderStatus
            binding.orderPlacedStatusValue.changeColorAsPerPaymentStatus(
                orderArguments?.orderStatus ?: Constants.defaultString
            )
            binding.orderPlacedEmployeeValue.text = orderArguments?.employeeName
            getAllTheLineItemsForTheOrder()
        }
    }

    private fun getAllTheLineItemsForTheOrder() {
        viewModel.getAllLineItems(
            OrderPaymentRequest(
                preferenceManager.getString(Constants.merchant_id),
                orderArguments?.order ?: Constants.defaultString,
                mutableListOf(Constants.PAYMENT).getExpandFilterList()
            )
        )
    }

    private fun setUpRecyclerView() {
        binding.apply {
            itemRecycler.layoutManager = LinearLayoutManager(requireContext())
            val adapter = ItemAdapter(lineItems, this@OrderDetailFragment)
            itemRecycler.adapter = adapter

            transactionRecycler.layoutManager = LinearLayoutManager(requireContext())
            val transactionAdapter = TransactionAdapter(paymentItems)
            transactionRecycler.adapter = transactionAdapter

        }
    }

    private fun setUpObservers() {
        viewModel.observerLiveData.observe(viewLifecycleOwner) {
            when (it.first) {
                OrderApiTypes.ErrorResponse -> showErrorResponse(it.second)
                OrderApiTypes.GetLineItemOfAnOrder -> showDataOnScreen(it.second)
                OrderApiTypes.GetOrderRefund -> addRefundData(it.second)
                OrderApiTypes.GetOrderDiscountResponse -> showTheDiscountData(it.second)
                OrderApiTypes.GetOrderPaymentStatus -> showThePaymentData(it.second)
                else -> "invalid response".debugLog(javaClass.simpleName)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addRefundData(response: Any) {
        if (response !is OrderRefundResponse) return
        var sum: Int = 0
        var createdTime: Long = 0L
        response.elements?.forEach { it ->
            it.refunds?.elements?.forEach { ref1 ->
                createdTime = ref1.payment?.createdTime ?: Constants.defaultLong
                sum += ref1.amount ?: Constants.defaultOffset
            }

        }

        val paymentElement = PaymentElement(
            sum, null, null, createdTime, null, null, null, null,
            null, null, null, null, null, true
        )

        if (!paymentItems.contains(paymentElement)) {
            paymentItems.add(paymentElement)
            binding.transactionRecycler.adapter?.notifyDataSetChanged()
        }
        binding.progressLayout.hideView()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showThePaymentData(response: Any) {
        if (response !is OrderPaymentResponse) return
        paymentItems.clear()

        if (response.elements?.isEmpty() == true) {
            binding.transactionRecycler.hideView()
            binding.noTransactionText.showView()
            binding.progressLayout.hideView()
            return
        }

        response.elements?.forEach {
            if (it != null) {
                paymentItems.add(it)
            }
        }
        binding.progressLayout.hideView()
        if (orderArguments?.orderStatus?.lowercase() == Constants.REFUNDED.lowercase()) {
            viewModel.getOrderPaymentStatusRefund(
                OrderPaymentRequest(
                    preferenceManager.getString(Constants.merchant_id),
                    orderArguments?.order ?: Constants.defaultString,
                    mutableListOf(Constants.refund).getExpandFilterList()
                )
            )
        }else{
            binding.progressLayout.hideView()
        }
        binding.transactionRecycler.adapter?.notifyDataSetChanged()
    }

    private fun showTheDiscountData(response: Any) {
        if (response !is OrderDiscountResponse) return

        // case refund


        // case 1 if no discount is applied then there will be tax applied
        if (response.elements?.isEmpty() == true) {
            "${currency.convertToSymbol()}${abs(totalItemPriceSum - totalPriceFromApi).toDoubleFloatPoint()}".also {
                binding.taxValue.text = it
            }
            binding.discount.hideView()
            binding.discountValue.hideView()
            return
        }

        // case 2 only Discount is applied then
        val discountValue = response.elements?.get(0)?.percentage
        val priceAfterDiscount =
            totalItemPriceSum.toDoubleFloatPoint().toDouble().times(discountValue ?: 1).div(100)
                .toInt()




        if (totalItemPriceSum.toIntPointResultInt() - priceAfterDiscount == totalPriceFromApi.toIntPointResultInt()) {
            "${currency.convertToSymbol()}0".also {
                binding.taxValue.text = it
            }
            binding.discount.text = response.elements?.get(0)?.name
            "${currency.convertToSymbol()}${priceAfterDiscount}".also {
                binding.discountValue.text = it
            }
            binding.tax.hideView()
            binding.taxValue.hideView()
        }
        // case 3 both discount and tax is applied
        else {
            binding.discount.text = response.elements?.get(0)?.name
            "${currency.convertToSymbol()}${priceAfterDiscount}".also {
                binding.discountValue.text = it
            }
            "${currency.convertToSymbol()}${(totalPriceFromApi - (totalItemPriceSum - priceAfterDiscount)).toIntPoint()}".also {
                if (orderArguments?.orderStatus?.lowercase().equals(Constants.REFUNDED.lowercase())) {
                    "${currency.convertToSymbol()}${(orderArguments?.amount?.toIntPointResultInt()?.plus (priceAfterDiscount)?.minus(
                            totalItemPriceSum.toIntPointResultInt()))
                            ?.let { it1 -> abs(it1).toString() }
                    }".also {
                        binding.taxValue.text = it
                       "${currency.convertToSymbol()}${orderArguments?.amount?.toDoubleFloatPoint()}".also {
                           binding.totalValue.text = it
                       }
                    }
                } else {
                    binding.taxValue.text = it
                }

            }
        }
    }

    private fun showErrorResponse(response: Any) {
        "error response $response".debugLog(javaClass.simpleName)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDataOnScreen(response: Any) {
        binding.syncingText.hideView()
        if (response !is GetOrderLineItems) return
        binding.apply {
            lineItems.clear()
            response.elements?.forEach {
                it?.let { lineItems.add(it) }
            }
            val items = consolidateLineItems(response.elements ?: mutableListOf())
            lineItems.clear()
            items.forEach {
                lineItems.add(it)
            }
            "${currency.convertToSymbol()}${totalItemPriceSum.toDoubleFloatPoint()}".also {
                subtotalValue.text = it
            }

            itemRecycler.adapter?.notifyDataSetChanged()


            // if the total price is equal to sum of cost of each item then we will not do anything
            if (totalItemPriceSum != orderArguments?.total) {
                viewModel.getOrderDiscountData(
                    OrderDiscountRequest(
                        preferenceManager.getString(
                            Constants.merchant_id
                        ), orderId = orderArguments?.order ?: Constants.defaultString
                    )
                )
            } else {
                tax.hideView()
                taxValue.hideView()
                discount.hideView()
                discountValue.hideView()
            }

            viewModel.getOrderPaymentStatus(
                OrderPaymentRequest(
                    preferenceManager.getString(Constants.merchant_id),
                    orderArguments?.order ?: Constants.defaultString,
                    mutableListOf(Constants.tender).getExpandFilterList()
                )
            )

            viewModel.getOrderPaymentStatus(
                OrderPaymentRequest(
                    preferenceManager.getString(Constants.merchant_id),
                    orderArguments?.order ?: Constants.defaultString,
                    mutableListOf(Constants.tender).getExpandFilterList()
                )
            )


        }
    }


    private fun setUpVariables() {
        viewModel = ViewModelProvider(this)[OrderDetailViewModel::class.java]
        getOrderDetails()
    }

    private fun consolidateLineItems(lineItems: List<LineItemElement?>): List<LineItemElement> {
        val consolidatedItems = mutableMapOf<String, LineItemElement>() // Map for consolidation

        for (item in lineItems) {
            totalItemPriceSum += item?.price ?: 0
            val key =
                "${item?.name}-${item?.colorCode}-${item?.price}" // Combine name, color, and price

            if (consolidatedItems.containsKey(key)) {
                consolidatedItems[key]!!.apply { // Access existing item and apply changes
                    itemCount = (itemCount ?: 0) + 1 // Increment item count (handles null case)
                }
            } else {
                if (item != null)
                    consolidatedItems[key] = item.copy(itemCount = 1) // Add new item with count 1
            }
        }

        return consolidatedItems.values.toList() // Convert map values to a list
    }


    private fun getOrderDetails() {


        viewModel.getOrderDetailsWithLineItemFilter(
            OrderDetailRequest(
                preferenceManager.getString(
                    Constants.merchant_id
                ),
                orderArguments?.order ?: "",
                mutableListOf(Constants.lineItems).getExpandFilterList()
            )
        )
    }


    private fun setUpTheClickListeners() {
        binding.apply {
            backButton.setOnClickListener { navigate(R.id.action_orderDetailFragment_to_orderHistoryFragment) }
            syncButton.setOnClickListener {
                syncingText.showView()
                progressLayout.showView()
                getAllTheLineItemsForTheOrder()
                freeVariables()
            }
        }
    }

    private fun freeVariables() {
        totalItemPriceSum = 0
        totalPriceFromApi = 0
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