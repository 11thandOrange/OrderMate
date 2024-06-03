package com.order.orderappclover.fragment.orderHistory


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.util.CloverAccount
import com.order.orderappclover.R
import com.order.orderappclover.adapters.OrderAdapter
import com.order.orderappclover.communicators.IOrderItemClickListener
import com.order.orderappclover.databinding.FragmentOrderHistoryBinding
import com.order.orderappclover.modals.ErrorResponse
import com.order.orderappclover.modals.orderHistory.AccessTokenRequest
import com.order.orderappclover.modals.orderHistory.AccessTokenResponse
import com.order.orderappclover.modals.orderHistory.Element
import com.order.orderappclover.modals.orderHistory.GetAllEmployeeListResponse
import com.order.orderappclover.modals.orderHistory.GetAllEmployeeRequest
import com.order.orderappclover.modals.orderHistory.GetAllOrderRequest
import com.order.orderappclover.modals.orderHistory.GetOrderResponse
import com.order.orderappclover.modals.orderHistory.OrderItems
import com.order.orderappclover.utils.Constants
import com.order.orderappclover.utils.OrderApiTypes
import com.order.orderappclover.utils.PreferenceManager
import com.order.orderappclover.utils.debugLog
import com.order.orderappclover.utils.getOnlyFirstName
import com.order.orderappclover.utils.hideView
import com.order.orderappclover.utils.navigateDirection
import com.order.orderappclover.utils.runOnBackgroundThread
import com.order.orderappclover.utils.runOnMainThread
import com.order.orderappclover.utils.showSnackBar
import com.order.orderappclover.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OrderHistoryFragment : Fragment(), IOrderItemClickListener {
    companion object {
        private var isApiDataLimitReached = false
        private var userHaveSyncTheData = false
        private var currentRunningOffset: Int = 0
        var employeeMapList: MutableList<Element> = mutableListOf()
        var isFromDetailScreen = false
    }

    private var allItemList: ArrayList<OrderItems> = ArrayList()
    private val binding by lazy {
        FragmentOrderHistoryBinding.inflate(layoutInflater)
    }

    private val preferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }


    private var viewModel: OrderHistoryViewModel? = null
    private var orderAdapter: OrderAdapter? = null
    private var orderItems: ArrayList<OrderItems> = ArrayList()

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
        setUpObserver()
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
            orderRecycler.addOnScrollListener(scrollManager)
            searchBar.doAfterTextChanged { searchTheData(it) }
        }
    }

    private val scrollManager = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0 && !isApiDataLimitReached) {
                hitOrderApi(orderItems.size, false)
                binding.orderProgressLoader.showView()
            }
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
                val resultant: MutableList<OrderItems> = mutableListOf()

                for (data in allItemList) {
                    if (p0?.toString()?.uppercase()?.let { data.id?.startsWith(it) } == true) {
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

        if (preferenceManager.getString(Constants.accessToken) == Constants.defaultString) {
            openWebViewForTheAuthToken()
            binding.progressBar.showView()
        } else {
            if (isFromDetailScreen) return
            hitOrderApi()
        }
    }

    private fun setUpObserver() {
        viewModel?.observerLiveData?.observe(viewLifecycleOwner) {
            when (it.first) {
                OrderApiTypes.AccessTokenResponse -> storeAndUpdateUI(it.second)
                OrderApiTypes.GetAllOrders -> showAndUpdateOrders(it.second)
                OrderApiTypes.GetAllEmployeeResponse -> mapEachOrderWithEmployee(it.second)
                OrderApiTypes.ErrorResponse -> apiReturnError(it.second)
                else -> "in valid response ".debugLog(javaClass.simpleName)
            }
        }
    }

    private fun mapEachOrderWithEmployee(response: Any) {
        if (response !is GetAllEmployeeListResponse) return

        employeeMapList.clear()
        response.elements?.forEach {
            if (it != null) {
                employeeMapList.add(it)
            }
        }

        notifyTheAdapter()
        binding.progressLayout.hideView()
    }

    private fun showAndUpdateOrders(response: Any) {
        if (response is GetOrderResponse) {
            verifyIsLimitReached(response)
            binding.orderRecycler.showView()
            binding.recyclerHeader.container.showView()
            binding.syncingText.hideView()
            binding.searchBar.showView()
            binding.searchIcon.showView()
            binding.syncButton.showView()
            binding.filterLayout.hideView()
            if (userHaveSyncTheData) {
                userHaveSyncTheData = !userHaveSyncTheData
                orderItems.clear()
            }
            if (response.elements != null) {
                allItemList.addAll(response.elements)
                orderItems.addAll(response.elements)
            }

            binding.orderProgressLoader.hideView()
            viewModel?.getAllEmployees(GetAllEmployeeRequest(preferenceManager.getString(Constants.merchant_id)))

        }
    }

    private fun verifyIsLimitReached(response: GetOrderResponse) {
        isApiDataLimitReached = (response.elements?.size
            ?: Constants.defaultOffset) < Constants.defaultLimitForPagination
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyTheAdapter() {
        binding.orderRecycler.adapter?.notifyDataSetChanged()
    }

    private fun apiReturnError(response: Any) {
        if (response !is ErrorResponse) return
        if (response.errorBody?.lowercase()?.contains(Constants.Unauthorized.lowercase()) == true) {

            openWebViewForTheAuthToken()
        }
        binding.root.showSnackBar()
    }

    private fun storeAndUpdateUI(response: Any) {
        if (response is AccessTokenResponse) {
            preferenceManager.saveString(Constants.accessToken, response.accessToken)
            hitOrderApi()
        }
    }

    private fun initializeVariable() {
        // @this --> describe that the owner of this viewModel instance is this class.
        viewModel = ViewModelProvider(this)[(OrderHistoryViewModel::class.java)]
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
                hitOrderApi()
            }

        }
    }


    private fun hitOrderApi(offset: Int? = null, isNotForPagination: Boolean = true) {
        // to prevent the multiple api calls
        if (currentRunningOffset == offset) return


        currentRunningOffset = offset ?: 0
        if (isNotForPagination) {
            binding.progressLayout.showView()
            binding.noTextFoundUpper.hideView()
            binding.noTextFoundLower.hideView()
        }

        viewModel?.getAllOrdersWithPagination(
            GetAllOrderRequest(
                preferenceManager.getString(
                    Constants.merchant_id
                ), Constants.payment, Constants.defaultLimitForPagination, offset
            )
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebViewForTheAuthToken() {
        binding.progressLayout.hideView()
        binding.apply {
            webView.settings.domStorageEnabled = true
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url?.contains(Constants.code) == true) {
                        saveDataIntoPreference(url)
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.hideView()
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                webView.showView()
                webView.loadUrl(Constants.oauthUrl)
            }
        }

    }

    private fun saveDataIntoPreference(url: String) {
        val data = getTheValue(url)
        preferenceManager.saveString(Constants.merchant_id, data[Constants.merchant_id] ?: "")
        preferenceManager.saveString(Constants.employee_id, data[Constants.employee_id] ?: "")
        preferenceManager.saveString(Constants.code, data[Constants.clientCode] ?: "")
        preferenceManager.saveString(Constants.client_id, data[Constants.client_id] ?: "")
        binding.webView.hideView()
        getAccessToken(data[Constants.clientCode] ?: "", data[Constants.client_id] ?: "")

    }

    private fun getTheValue(url: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        val splitArray = url.split('?')[1].split('&')
        splitArray.forEach {
            val split = it.split('=')
            data[split[0]] = split[1]
        }
        return data
    }

    private fun getAccessToken(code: String, clientId: String) {
        val accessTokenRequest = AccessTokenRequest(
            clientId = clientId,
            clientSecret = Constants.clientSecret,
            code = code,
            redirectUri = Constants.redirectUri
        )
        viewModel?.getAccessToken(accessTokenRequest)
    }

    override fun onOrderItemClick(orderPosition: Int) {
        var orderEmployeeName = Constants.defaultString
        employeeMapList.forEach {
            if (it.id == orderItems[orderPosition].employee?.id) {
                orderEmployeeName = it.name ?: Constants.defaultString
            }
        }
        val orderId = orderItems[orderPosition].id
        val orderStatus = orderItems[orderPosition].paymentState ?: Constants.defaultString
        val orderCreated = orderItems[orderPosition].createdTime ?: Constants.defaultLong
        val total = orderItems[orderPosition].total ?: Constants.defaultInt
        val currency = orderItems[orderPosition].currency ?: Constants.defaultString
        val tax = orderItems[orderPosition].payments?.elements?.get(0)?.taxAmount ?: Constants.defaultOffset
        val amount = orderItems[orderPosition].payments?.elements?.get(0)?.amount ?: Constants.defaultOffset
        val direction =
            orderId?.let {
                OrderHistoryFragmentDirections.actionOrderHistoryFragmentToOrderDetailFragment(
                    order = it, orderEmployeeName, orderStatus, orderCreated, total, currency , tax , amount
                )
            }
        if (direction != null) {
            navigateDirection(direction)
        }
    }

}