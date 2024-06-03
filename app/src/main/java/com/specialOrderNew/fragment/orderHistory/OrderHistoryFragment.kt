package com.specialOrderNew.fragment.orderHistory


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.order.OrderConnector
import com.specialOrderNew.R
import com.specialOrderNew.adapters.OrderAdapter
import com.specialOrderNew.communicators.IOrderItemClickListener
import com.specialOrderNew.databinding.FragmentOrderHistoryBinding
import com.specialOrderNew.utils.Constants
import com.specialOrderNew.utils.PreferenceManager
import com.specialOrderNew.utils.debugSnackBar
import com.specialOrderNew.utils.exceptionHandlerWithReturn
import com.specialOrderNew.utils.getOnlyFirstName
import com.specialOrderNew.utils.hideView
import com.specialOrderNew.utils.navigateDirection
import com.specialOrderNew.utils.runOnBackgroundThread
import com.specialOrderNew.utils.runOnMainThread
import com.specialOrderNew.utils.showView
import kotlinx.coroutines.Dispatchers


class OrderHistoryFragment : Fragment(), IOrderItemClickListener {
    companion object {
        private var isApiDataLimitReached = false
        private var userHaveSyncTheData = false
        var isFromDetailScreen = false
    }


    private var allItemList: ArrayList<Order> = ArrayList()
    private val binding by lazy {
        FragmentOrderHistoryBinding.inflate(layoutInflater)
    }

    private val preferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }


    private var orderAdapter: OrderAdapter? = null
    private var orderItems: ArrayList<Order?> = ArrayList()
    private var orderConnector: OrderConnector? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeVariable()
        setUpClickListeners()
        setUpListeners()
        setupSpinners()
        getTheMerchantId()
    }


    override fun onResume() {
        super.onResume()
        binding.searchBar.clearFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        isFromDetailScreen = false
    }

    private fun setUpListeners() {
        binding.apply {
            searchBar.doAfterTextChanged { searchTheData(it) }
        }
    }

    private fun searchTheData(p0: CharSequence?) {
        // if search bar is empty

        runOnBackgroundThread(Dispatchers.Default) {
            if (p0?.trim()?.isEmpty() == true) {

                orderItems.clear()
                allItemList.forEach {
                    orderItems.add(it)
                }
                binding.progressLayout.hideView()
            }
            // if search bar is not empty
            else {
                val resultant: MutableList<Order> = mutableListOf()

                for (data in allItemList) {
                    if (p0?.toString()?.uppercase()
                            ?.let { matchWithData(data, it.lowercase()) } == true
                    ) {
                        resultant.add(data)
                    }
                }
                orderItems.clear()
                resultant.forEach {
                    orderItems.add(it)
                }
            }
            val item = orderItems.isEmpty()
            runOnMainThread { isSearchItemVisibility(item, !item, item) }

        }
        notifyTheAdapter()
    }

    private fun matchWithData(data: Order?, search: String): Boolean {
       val result = exceptionHandlerWithReturn {
            val isPaymentStatusMatched = data?.paymentState?.name?.lowercase()
            val employeeName = data?.employee?.jsonObject?.get("name")?.toString()?.lowercase()
            val orderId = data?.id?.lowercase()
            val paymentCost = data?.total?.toString()

            (isPaymentStatusMatched?.startsWith(search) == true) ||
                    (employeeName?.startsWith(search) == true) ||
                    (orderId?.startsWith(search) == true) ||
                    (paymentCost?.startsWith(search) == true)
        }
        return result?:Constants.defaultBoolean
    }


    private fun isSearchItemVisibility(layout: Boolean, loader: Boolean, views: Boolean) {
        binding.progressLoader.isVisible = loader
        binding.noTextFoundUpper.isVisible = views
        binding.noTextFoundLower.isVisible = views
        binding.progressLayout.isVisible = layout
    }


    private fun getTheMerchantId() {
        runOnBackgroundThread {
            val merchant = CloverAccount.getAccount(requireContext())
            preferenceManager.saveString(
                Constants.merchantName, merchant.name.getOnlyFirstName()
            )
        }

    }

    private fun setupSpinners() {
        binding.apply {
            val orderStatusAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.order_text_array, R.layout.item_spinner
            )

            orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            orderTypeSpinner.adapter = orderStatusAdapter

            val orderNameAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.order_name_array, R.layout.item_spinner
            )

            orderNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            orderNameSpinner.adapter = orderNameAdapter

            val orderPaymentAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.order_payment_array, R.layout.item_spinner
            )

            orderPaymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            orderPaymentSpinner.adapter = orderPaymentAdapter


            val orderBookingSpinner = ArrayAdapter.createFromResource(
                requireContext(), R.array.order_booking_array, R.layout.item_spinner
            )

            orderBookingSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            orderBookingTypeSpinner.adapter = orderBookingSpinner

        }
    }


    override fun onStart() {
        super.onStart()
        orderConnector = OrderConnector(
            requireContext(),
            CloverAccount.getAccount(requireContext()),
            null
        )
        orderConnector?.connect()
        getTheOrderData()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getTheOrderData() {
        runOnBackgroundThread {
            try {
                val orderData = orderConnector?.getOrders(mutableListOf())
                orderItems.clear()
                orderData?.forEach {
                    allItemList.add(it)
                    orderItems.add(it)
                }

                getTheFilterData()
            } catch (e: Exception) {
                e.printStackTrace()
                debugSnackBar("There is Issue with Your Account Please check the Clover Dashboard")
            }
            runOnMainThread {
                binding.orderRecycler.adapter?.notifyDataSetChanged()
                binding.syncingText.hideView()
            }
        }

    }

    private fun getTheFilterData() {
        allItemList.forEach {
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun notifyTheAdapter() {
        binding.orderRecycler.adapter?.notifyDataSetChanged()
    }


    private fun initializeVariable() {
        binding.apply {
            orderRecycler.layoutManager = LinearLayoutManager(requireContext())
            orderAdapter = OrderAdapter(orderItems, this@OrderHistoryFragment)
            orderRecycler.adapter = orderAdapter
        }

    }

    private fun setUpClickListeners() {
        binding.apply {
            syncButton.setOnClickListener {
                syncingText.showView()
                searchBar.text?.clear()
                userHaveSyncTheData = !userHaveSyncTheData
                isApiDataLimitReached = false
                getTheOrderData()
            }


        }
    }


    override fun onOrderItemClick(orderPosition: Int) {
        val direction = orderItems[orderPosition]?.let {
            OrderHistoryFragmentDirections.actionOrderHistoryFragmentToOrderDetailFragment(
                it
            )

        }
        if (direction != null) {
            navigateDirection(direction)
        }

    }

}

//private fun searchTheData(p0: CharSequence?) {
//    // if search bar is empty
//
//    runOnBackgroundThread(Dispatchers.Default) {
//        if (p0?.trim()?.isEmpty() == true) {
//
//            orderItems.clear()
//            allItemList.forEach {
//                orderItems.add(it)
//            }
//            binding.progressLayout.hideView()
//        }
//        // if search bar is not empty
//        else {
//            val resultant: MutableList<Order> = mutableListOf()
//
//            for (data in allItemList) {
//                if (p0?.toString()?.uppercase()?.let { data.id?.startsWith(it) } == true) {
//                    resultant.add(data)
//                }
//            }
//            orderItems.clear()
//            resultant.forEach {
//                orderItems.add(it)
//            }
//        }
//        val item = orderItems.isEmpty()
//        runOnMainThread { isSearchItemVisibility(item, !item, item) }
//
//    }
//    notifyTheAdapter()
//}
