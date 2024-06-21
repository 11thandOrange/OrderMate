package com.specialOrder.fragment.orderDetail

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v1.Intents
import com.clover.sdk.v1.Intents.EXTRA_ORDER_ID
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.Refund
import com.specialOrder.R
import com.specialOrder.activities.MainActivity
import com.specialOrder.adapters.ItemAdapter
import com.specialOrder.adapters.RefundAdapter
import com.specialOrder.adapters.TransactionAdapter
import com.specialOrder.communicators.ILineItemUpdateListener
import com.specialOrder.communicators.IOrderItemClickListener
import com.specialOrder.communicators.IShareEmailOrMessage
import com.specialOrder.databinding.FragmentOrderDetailBinding
import com.specialOrder.fragment.orderHistory.OrderHistoryFragment
import com.specialOrder.modals.CustomItemJson
import com.specialOrder.modals.ItemModal
import com.specialOrder.modals.ShareMessageJson
import com.specialOrder.modals.ShareSmsModal
import com.specialOrder.utils.ConnectionManager
import com.specialOrder.utils.Constants
import com.specialOrder.utils.MyApp
import com.specialOrder.utils.PreferenceManager
import com.specialOrder.utils.changeColorAsPerPaymentStatus
import com.specialOrder.utils.convertToSymbol
import com.specialOrder.utils.debugSnackBar
import com.specialOrder.utils.exceptionHandler
import com.specialOrder.utils.formatMillisToDateTime
import com.specialOrder.utils.hideView
import com.specialOrder.utils.runOnBackgroundThread
import com.specialOrder.utils.runOnMainThread
import com.specialOrder.utils.showSnackBar
import com.specialOrder.utils.showView
import com.specialOrder.utils.toDoubleFloatPoint
import kotlinx.coroutines.Dispatchers


class OrderDetailFragment : Fragment(), IOrderItemClickListener, ILineItemUpdateListener,
    IShareEmailOrMessage {

    companion object {
        var currencyName: String = ""
    }

    private var totalItemPriceSum: Long = 0L
    private var totalPriceFromApi: Long = 0L

    private val binding: FragmentOrderDetailBinding by lazy {
        FragmentOrderDetailBinding.inflate(layoutInflater)
    }

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val myApp by lazy {
        MyApp.getInstance()
    }


    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private var paymentItems: MutableList<Payment> = mutableListOf()
    private var refundItems: MutableList<Refund> = mutableListOf()
    private var orderArguments: OrderDetailFragmentArgs? = null
    private lateinit var viewModel: OrderDetailViewModel

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
        viewModel = ViewModelProvider(this)[OrderDetailViewModel::class.java]
        setUpObserver()
    }

    private fun setUpObserver() {
        viewModel.successResponse.observe(viewLifecycleOwner) {
            when (it.first) {
                Constants.emailAddress -> {
                    debugSnackBar(
                        if (it.second) getString(R.string.email_has_been_shared_successfully) else getString(
                            R.string.failed_to_share_the_email
                        )
                    )
                }
                Constants.phoneNumber -> {
                    debugSnackBar(
                        if (it.second) getString(R.string.sms_has_been_shared_successfully) else getString(
                            R.string.fail_to_share_the_sms
                        )
                    )
                }
                Constants.noInternet ->{
                    debugSnackBar(getString(R.string.please_check_your_internet_connection))
                }
            }
        }
    }


    private fun setDataOnScreenWithArgs() {
        orderArguments?.orderData?.apply {
            totalPriceFromApi = total ?: Constants.defaultLong
            currencyName = currency ?: Constants.defaultString
            getString(
                R.string.priceString,
                currencyName.convertToSymbol(),
                total?.toDoubleFloatPoint()
            ).also {
                binding.totalValue.text = it
            }
            getString(R.string.orders_text, id).also { binding.orderId.text = it }
            val date = getString(
                R.string.getDateWithDot, createdTime?.formatMillisToDateTime(
                    Constants.yearFormatWithMonthName,
                    true
                ), createdTime?.formatMillisToDateTime(Constants.dateFormat)
            )
            binding.orderPlacedAtValue.text = date
            binding.merchantName.text = preferenceManager.getString(Constants.merchantName)

            binding.orderPlacedStatusValue.text = paymentState?.name
            binding.orderPlacedStatusValue.changeColorAsPerPaymentStatus(
                paymentState?.name ?: Constants.defaultString
            )
            exceptionHandler {
                binding.orderPlacedEmployeeValue.text =
                    employee?.jsonObject?.get(Constants.name)?.toString()
                if (customers?.isNotEmpty() == true) {
                    getString(
                        R.string.getFullName,
                        customers[0]?.firstName,
                        customers[0]?.lastName
                    ).also {
                        binding.orderPlacedCustomerValue.text = it
                    }
                    val result = getMobileNumber(customers[0])
                    binding.orderPlacedCustomerNumberValue.text = result.first
                    binding.orderPlacedCustomerEmailValue.text = result.second
                }
            }
            setUpTheScreen()
        }
    }


    private fun getMobileNumber(customer: Customer?): Pair<String, String> {
        var resultNumber = ""
        var resultEmail = ""

        customer?.phoneNumbers?.forEach {
            resultNumber += "${it?.phoneNumber}, "
        }
        customer?.emailAddresses?.forEach {
            resultEmail += "${it?.emailAddress}, "
        }
        if (resultNumber.trim().isNotEmpty()) resultNumber =
            resultNumber.substring(0, resultNumber.length - 2)
        if (resultEmail.trim().isNotEmpty()) resultEmail =
            resultEmail.substring(0, resultEmail.length - 2)
        return Pair(resultNumber, resultEmail)
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
        val isPaymentMadeForOrder =
            orderArguments?.orderData?.payments != null || orderArguments?.orderData?.payments?.isNotEmpty() == true
        val isRefundMadeForOrder =
            orderArguments?.orderData?.refunds != null || orderArguments?.orderData?.refunds?.isNotEmpty() == true

        // if both payment and refund is not made for the order
        if (!isRefundMadeForOrder && !isPaymentMadeForOrder) {
            binding.transactionRecycler.hideView()
            binding.refundRecycler.hideView()
            binding.noTransactionText.showView()
            return
        }

        // check for the payment data for the order
        if (!isPaymentMadeForOrder) {
            binding.transactionRecycler.hideView()
        } else {
            paymentItems.clear()
            orderArguments?.orderData?.payments?.forEach {
                paymentItems.add(it)
            }
            binding.transactionRecycler.adapter?.notifyDataSetChanged()
        }

        // setUp the refund data for the order
        if (!isRefundMadeForOrder) {
            binding.refundRecycler.hideView()
        } else {
            refundItems.clear()
            orderArguments?.orderData?.refunds?.forEach {
                refundItems.add(it)
            }
            binding.refundRecycler.adapter?.notifyDataSetChanged()
        }
    }


    /*
    * For discount some time clover directly provide the value in the amount key sometime it will
    * provide us the percentage so we need to check this also.
    * */
    private fun showTheDiscountAndTaxData() {
        // case 1 if no discount is applied then there will be tax applied only.
        if (orderArguments?.orderData?.discounts == null || orderArguments?.orderData?.discounts?.isEmpty() == true) {
            binding.discount.hideView()
            binding.discountValue.hideView()
        }

        "${currencyName.convertToSymbol()}${
            myApp.orderTax(orderArguments?.orderData).toDoubleFloatPoint()
        }".also {
            binding.taxValue.text = it
        }
        "${currencyName.convertToSymbol()}${
            myApp.orderLineItemTotal(orderArguments?.orderData).toDoubleFloatPoint()
        }".also {
            binding.subtotalValue.text = it
        }
        "${currencyName.convertToSymbol()}${
            myApp.orderDiscount(orderArguments?.orderData).toDoubleFloatPoint()
        }".also {
            binding.discountValue.text = it
        }
        exceptionHandler {
            val discountName =
                orderArguments?.orderData?.discounts?.get(0)?.jsonObject?.get(Constants.name)
            binding.discount.text = discountName.toString()
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    private fun showLineItemOnScreen() {
        binding.syncingText.hideView()
        binding.apply {
            lineItems.clear()
            val data = orderArguments?.orderData?.lineItems?.let { countElementsByUniqueKeys(it) }
            data?.forEach {
                totalItemPriceSum += it.order?.price?.times(it.itemCount)
                    ?: Constants.defaultLong
                lineItems.add(it)
            }

            getString(
                R.string.priceString,
                currencyName.convertToSymbol(),
                totalItemPriceSum.toDoubleFloatPoint()
            ).also {
                subtotalValue.text = it
            }
            getString(R.string.order, orderArguments?.orderData?.lineItems?.size).also {
                orderItemCountText.text = if (it == Constants.nullValue) {
                    itemRecycler.hideView()
                    getString(R.string.order)
                } else {
                    it
                }
            }
            itemRecycler.adapter?.notifyDataSetChanged()
        }
    }


    /*
    * Clover provide us the data in the raw format
    * eg if i have added 10 ice-cream then it will provide us 10 ice cream objects
    * so we are clubbing the same items together and result in the item listing as per unique items
    * this function generated key will help in distinguishing items
    * */
    private fun getTheUniqueOrderItemKey(lineItems: LineItem?): String {
        var modificationString = ""
        if (lineItems?.hasModifications() == true) {
            lineItems.modifications?.forEach {
                modificationString += " ${it.name}"
            }
        }
        return getString(
            R.string.key,
            lineItems?.name.toString(),
            lineItems?.price.toString(),
            lineItems?.unitName,
            lineItems?.unitQty.toString(),
            modificationString
        )
    }

    /*
        * make provide the list of order line items with unique ness eg if a item is added 10 times
        * then it will provide one item in list with item count 10
        *
        * */
    private fun countElementsByUniqueKeys(lineItems: List<LineItem?>): MutableList<ItemModal> {
        val elementCounts: MutableSet<ItemModal> = mutableSetOf()
        var isMatched: Boolean
        for (lineItem in lineItems) {
            isMatched = false
            val key = getTheUniqueOrderItemKey(lineItem)
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
            backButton.setOnClickListener { findNavController().popBackStack() }
            reIssueReceiptButton.setOnClickListener {
                val dror = Intent(Intents.ACTION_START_PRINT_RECEIPTS)
                dror.putExtra(EXTRA_ORDER_ID, orderArguments?.orderData?.id)
                startActivity(dror)
            }
            sendNotificationButton.setOnClickListener {
                if(ConnectionManager.getInstance().isNetworkConnected(requireContext())){
                    debugSnackBar(getString(R.string.please_check_your_internet_connection_1))
                   return@setOnClickListener
                }
                val obj = SendNotificationDialog(orderArguments?.orderData, this@OrderDetailFragment)
                obj.show(parentFragmentManager, "")
            }
        }
    }


    // when user click on the add note button
    // if all the options are disabled by admin then this will not work
    // if a single option is available then it will show
    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        if (hasAddNoteAccess()) {
            CustomModalDialog(
                lineItemId,
                orderArguments?.orderData,
                orderPosition,
                this@OrderDetailFragment,
            ).show(
                parentFragmentManager,
                Constants.defaultString,
            )
        } else {
            binding.root.showSnackBar(getString(R.string.no_access))
        }
    }

    /*
    * @param : null
    * @return : true when there is  any active option for the dialog
    * @return : false when there is not any active option for the dialog
    * */
    private fun hasAddNoteAccess(): Boolean {

        val data = preferenceManager.getJsonString()
        // checks for if the provided data is null or not required JSON then we
        // not show the dialog as rest of the data in dialog will also be not valid
        // leads to irrelevant dialog
        if (data !is CustomItemJson) {
            return false
        }
        data.types.forEach {
            if (it.isActive) return true
        }
        return false
    }

    private fun onBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        if (activity != null)
            (activity as MainActivity).onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )
    }

    /*
    * When user update the note for the item the callback come here
    * */
    override fun updateLineItem(id: String?, list: String?, position: Int) {
        if (list == null) return
        updateNoteInTheLineItemOfOrder(id, list, position)
    }

    private fun updateNoteInTheLineItemOfOrder(id: String?, list: String, position: Int) {
        runOnBackgroundThread(Dispatchers.Default) {
            // update the order in the order detail screen
            for (i in lineItems) {
                if (i?.order?.item?.id == id) {
                    i?.order?.note = list
                }
            }


            // update the order in the order history screen
            // update all item quantity if the order so that
            // each quantity of line item has same note
            for (i in orderArguments?.orderData?.lineItems ?: emptyList()) {
                if (i?.item?.id == id) {
                    i?.note = list
                }
            }
            OrderHistoryFragment.getInstance().updateTheOrder(orderArguments?.orderData)
        }
        runOnMainThread {
            binding.itemRecycler.adapter?.notifyItemChanged(position)
        }
    }

    // when we want to share the email and sms to the customer
    override fun sendEmail(data: ShareMessageJson) {
        viewModel.shareEmail(data)
    }

    override fun shareSms(data: ShareSmsModal) {
        viewModel.shareSms(data)
    }
}
