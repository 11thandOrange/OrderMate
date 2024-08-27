package com.orderMate.fragment.orderDetail

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v1.Intents
import com.clover.sdk.v1.Intents.EXTRA_ORDER_ID
import com.clover.sdk.v3.apps.AppsConnector
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.Refund
import com.orderMate.R
import com.orderMate.adapters.ItemAdapter
import com.orderMate.adapters.RefundAdapter
import com.orderMate.adapters.TransactionAdapter
import com.orderMate.communicators.ILineItemUpdateListener
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.communicators.IShareEmailOrMessage
import com.orderMate.databinding.FragmentOrderDetailBinding
import com.orderMate.fragment.orderHistory.OrderHistoryFragment
import com.orderMate.modals.CustomItemJson
import com.orderMate.modals.ItemModal
import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal
import com.orderMate.utils.ConnectionManager
import com.orderMate.utils.Constants
import com.orderMate.utils.MyApp
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.changeColorAsPerPaymentStatus
import com.orderMate.utils.convertToSymbol
import com.orderMate.utils.countElementsByUniqueKeys
import com.orderMate.utils.createAndShowDialog
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.formatMillisToDateTime
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.getThePaymentState
import com.orderMate.utils.hideView
import com.orderMate.utils.onBackPressed
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showSnackBar
import com.orderMate.utils.showView
import com.orderMate.utils.toDoubleFloatPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class OrderDetailFragment : Fragment(), IOrderItemClickListener, ILineItemUpdateListener,
    IShareEmailOrMessage {

    companion object {
        var currencyName: String = ""
        var isPaymentBtnClicked = false
        var isReOpenBtnClicked = false
        var orderIdForReopen: String? = null
    }

    private var totalItemPriceSum: Long = 0L
    private var totalPriceFromApi: Long = 0L
    private var data: OrderDetailFragmentArgs? = null

    private val binding: FragmentOrderDetailBinding by lazy {
        FragmentOrderDetailBinding.inflate(layoutInflater)
    }

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val myApp by lazy {
        MyApp.getInstance()
    }
    private var appConnector: AppsConnector? = null

    private var lineItems: MutableList<ItemModal?> = mutableListOf()
    private var paymentItems: MutableList<Payment> = mutableListOf()
    private var refundItems: MutableList<Refund> = mutableListOf()
    private var orderArguments: Order? = null
    private lateinit var viewModel: OrderDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        data = arguments?.let { OrderDetailFragmentArgs.fromBundle(it) }
        orderArguments = data?.orderData
        setUpTheClickListeners()
        setUpRecyclerView()
        onBackPressed { findNavController().popBackStack() }
        setDataOnScreenWithArgs()
        viewModel = ViewModelProvider(this)[OrderDetailViewModel::class.java]
        setUpObserver()
        runOnBackgroundThread {
            appConnector = AppsConnector(requireContext(), myApp.getCloverAccount())
        }
    }


    private fun setUpObserver() {
        viewModel.successResponse.observe(viewLifecycleOwner) {
            when (it.first) {
                Constants.emailAddress -> {
                    if (it.second) {
                        runOnBackgroundThread {
                            exceptionHandler {
                                appConnector?.logMetered("WQFPE39Y84B4E", 1)
                            }
                        }
                    }
                    debugSnackBar(
                        if (it.second) getString(R.string.email_has_been_shared_successfully) else getString(
                            R.string.failed_to_share_the_email
                        )
                    )
                }

                Constants.phoneNumber -> {
                    if (it.second) {

                        runOnBackgroundThread {
                            exceptionHandler {
                                appConnector?.logMetered("WQFPE39Y84B4E", 1)
                            }

                        }
                    }
                    debugSnackBar(
                        if (it.second) getString(R.string.sms_has_been_shared_successfully) else getString(
                            R.string.fail_to_share_the_sms
                        )
                    )
                }

                Constants.noInternet -> {
                    debugSnackBar(getString(R.string.please_check_your_internet_connection))
                }
            }
        }
    }


    private fun setDataOnScreenWithArgs() {
        exceptionHandler {
            orderArguments?.apply {
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

                binding.orderPlacedAtValue.text =
                    createdTime?.formatMillisToDateTime(Constants.yearFormat)
                binding.merchantName.text = preferenceManager.getString(Constants.merchantName)

                binding.orderPlacedStatusValue.text =
                    requireContext().getThePaymentState(orderArguments)
                binding.orderPlacedStatusValue.changeColorAsPerPaymentStatus(
                    orderArguments
                )
                if (isStatusOpen()) {
                    binding.reOpenButton.isVisible = true
                    binding.deleteButton.isVisible = true
                    binding.addPayment.isVisible = true
                    binding.reIssueReceiptButton.isVisible = false
                } else {
                    binding.reOpenButton.isVisible = false
                    binding.deleteButton.isVisible = false
                    binding.addPayment.isVisible = false
                    binding.reIssueReceiptButton.isVisible = true
                }
                exceptionHandler {

                    if (customers?.isNotEmpty() == true) {
                        getString(
                            R.string.getFullName,
                            customers[0]?.firstName,
                            customers[0]?.lastName
                        ).also {
                            binding.orderPlacedCustomerValue.text = it
                        }
                        val result = getCustomerContactDetails(customers[0])
                        binding.orderPlacedCustomerNumberValue.text = result.first
                        binding.orderPlacedCustomerEmailValue.text = result.second

                    }
                }


                exceptionHandler({
                        binding.orderPlacedEmployeeValue.text =
                            employee?.jsonObject?.get(Constants.name)?.toString()
                })
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        val value = MyApp.getInstance().getEmployeeName(employee?.id)
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.orderPlacedEmployeeValue.text = value
                                ?: getString(R.string.dash)
                        }
                    }
                }
                setUpTheScreen()
            }
        }
    }


    private fun isStatusOpen(): Boolean {
        return ((orderArguments?.paymentState?.name?.equals(
            Constants.OPEN,
            true
        ) == true)

                || (orderArguments?.state?.equals(
            Constants.OPEN,
            true
        ) == true))

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

    override fun onResume() {
        super.onResume()


        // means don't non-required hit the API
        if (isPaymentBtnClicked) {
            CoroutineScope(Dispatchers.IO).launch {
                refreshUI(true)
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                refreshUI()
            }
        }





        orderIdForReopen = null
    }


    private fun setUpTheScreen() {
        showLineItemOnScreen()
        showThePaymentData()
        showTheDiscountAndTaxData()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showThePaymentData() {
        val isPaymentMadeForOrder =
            orderArguments?.payments != null || orderArguments?.payments?.isNotEmpty() == true
        val isRefundMadeForOrder =
            orderArguments?.refunds != null || orderArguments?.refunds?.isNotEmpty() == true

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
            orderArguments?.payments?.forEach {
                paymentItems.add(it)
            }
            binding.transactionRecycler.adapter?.notifyDataSetChanged()
        }

        // setUp the refund data for the order
        if (!isRefundMadeForOrder) {
            binding.refundRecycler.hideView()
        } else {
            refundItems.clear()
            orderArguments?.refunds?.forEach {
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
        if (orderArguments?.discounts == null || orderArguments?.discounts?.isEmpty() == true) {
            binding.discount.hideView()
            binding.discountValue.hideView()
        }

        "${currencyName.convertToSymbol()}${
            myApp.orderTax(orderArguments).toDoubleFloatPoint()
        }".also {
            binding.taxValue.text = it
        }
        "${currencyName.convertToSymbol()}${
            myApp.orderLineItemTotal(orderArguments).toDoubleFloatPoint()
        }".also {
            binding.subtotalValue.text = it
        }
        "${currencyName.convertToSymbol()}${
            myApp.orderDiscount(orderArguments).toDoubleFloatPoint()
        }".also {
            binding.discountValue.text = it
        }
        exceptionHandler {
            val discountName =
                orderArguments?.discounts?.get(0)?.jsonObject?.get(Constants.name)
            binding.discount.text = discountName.toString()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showLineItemOnScreen() {
        binding.syncingText.hideView()
        binding.apply {
            lineItems.clear()
            val data = orderArguments?.lineItems?.let {
                countElementsByUniqueKeys(
                    requireContext(),
                    it
                )
            }
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
            getString(R.string.order, orderArguments?.lineItems?.size).also {
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


    private fun setUpTheClickListeners() {
        binding.apply {
            backButton.setOnClickListener { findNavController().popBackStack() }
            binding.progressLayout.setOnClickListener {
                Constants.notImplementedLog
            }
            reIssueReceiptButton.setOnClickListener {
                val dror = Intent(Intents.ACTION_START_PRINT_RECEIPTS)
                dror.putExtra(EXTRA_ORDER_ID, orderArguments?.id)
                startActivity(dror)
            }

            addPayment.setOnClickListener {
                val dror = Intent(Intents.ACTION_CLOVER_PAY)
                dror.putExtra(EXTRA_ORDER_ID, orderArguments?.id)
                startActivity(dror)
                isPaymentBtnClicked = true
            }

            reOpenButton.setOnClickListener {
                val dror = Intent(Intents.ACTION_START_REGISTER)
                dror.putExtra(EXTRA_ORDER_ID, orderArguments?.id)
                startActivity(dror)
                orderIdForReopen = orderArguments?.id
                isReOpenBtnClicked = true
            }

            deleteButton.setOnClickListener {
                createAndShowDialog(
                    requireContext(),
                    "Are you sure you want to delete this order?",
                    "Delete Order",
                    getString(R.string.delete_order),
                    getString(R.string.cancel)
                ) {
                    runOnBackgroundThread {
                        if (deleteTheOrder()) {
                            runOnMainThread {
                                findNavController().popBackStack()
                            }
                            exceptionHandler {
                                // update the dashboard after the order is delay
                                Handler(Looper.getMainLooper()).postDelayed(
                                    { OrderHistoryFragment.getInstance().getTheOrderData(true) }, 1000
                                )

                            }
                        }
                    }
                }
            }


            sendNotificationButton.setOnClickListener {
                if (ConnectionManager.getInstance().isNetworkConnected(requireContext())) {
                    debugSnackBar(getString(R.string.please_check_your_internet_connection_1))
                    return@setOnClickListener
                }
                val obj =
                    SendNotificationDialog(orderArguments, this@OrderDetailFragment)
                obj.show(parentFragmentManager, "")
            }

            syncButton.setOnClickListener {

                CoroutineScope(Dispatchers.IO).launch {
                    refreshUI()
                }
            }
        }
    }


    private fun getTheRequiredData(data: List<Order>?): Order? {
        if (data?.isNotEmpty() == true) {
            data.forEach {
                if (it.id == orderArguments?.id) {
                    return it
                }
            }
        }
        return null
    }


    private suspend fun refreshUI(isFromResume: Boolean = false) {
        runOnMainThread {
            binding.progressLayout.showView()
            binding.syncButton.isClickable = false
            binding.syncingText.showView()
        }
        if (isFromResume) {
            delay(8000)
        }
        val data = myApp.getOrderConnector().getOrders(mutableListOf())
        orderArguments = getTheRequiredData(data)
        runOnMainThread {
            binding.syncingText.hideView()
            data?.let {
                setDataOnScreenWithArgs()
                binding.syncButton.isClickable = true
            }
            isPaymentBtnClicked = false
            isReOpenBtnClicked = false
            binding.progressLayout.hideView()
        }
    }

    private fun deleteTheOrder(): Boolean {
        orderArguments?.id?.let {
            return myApp.getOrderConnector().deleteOrder(it)
        }
        return false
    }


    // when user click on the add note button
    // if all the options are disabled by admin then this will not work
    // if a single option is available then it will show
    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {


        if (hasAddNoteAccess()) {
            CustomModalDialog(
                lineItemId,
                data?.orderData,
                orderArguments?.id,
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


    /*
    * When user update the note for the item the callback come here
    * */
    override fun updateLineItem(id: String?, list: String?, position: Int) {
        if (list == null) return
        updateNoteInTheLineItemOfOrder(id, list, position)
    }

    override fun dismissDialog() {
        Constants.notImplementedLog
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
            for (i in orderArguments?.lineItems ?: emptyList()) {
                if (i?.item?.id == id) {
                    i?.note = list
                }
            }
            OrderHistoryFragment.getInstance().updateTheOrder(orderArguments)
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
