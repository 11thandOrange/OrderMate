package com.specialOrder.fragment.orderHistory


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.google.android.material.textview.MaterialTextView
import com.specialOrder.R
import com.specialOrder.adapters.OrderAdapter
import com.specialOrder.communicators.IOrderItemClickListener
import com.specialOrder.communicators.InterCommunication
import com.specialOrder.databinding.FragmentOrderHistoryBinding
import com.specialOrder.modals.FilterData
import com.specialOrder.utils.Constants
import com.specialOrder.utils.Constants.Companion.notImplementedLog
import com.specialOrder.utils.FilterCategories
import com.specialOrder.utils.FirebaseRealtimeDataBaseManager
import com.specialOrder.utils.MyApp
import com.specialOrder.utils.MyApp.Companion.filterArray
import com.specialOrder.utils.PreferenceManager
import com.specialOrder.utils.debugSnackBar
import com.specialOrder.utils.exceptionHandler
import com.specialOrder.utils.exceptionHandlerWithReturn
import com.specialOrder.utils.getOnlyFirstName
import com.specialOrder.utils.hideView
import com.specialOrder.utils.isInArray
import com.specialOrder.utils.navigate
import com.specialOrder.utils.navigateDirection
import com.specialOrder.utils.runOnBackgroundThread
import com.specialOrder.utils.runOnMainThread
import com.specialOrder.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class OrderHistoryFragment : Fragment(), IOrderItemClickListener, InterCommunication {


    companion object {
        // This Bit is used for the verifying that does the current used merchant has admin access.
        var isClicked: Boolean = true
        private var isAdmin: Boolean = false
        private var instance: OrderHistoryFragment? = null
        fun getInstance(): OrderHistoryFragment {
            return instance ?: synchronized(this) {
                OrderHistoryFragment().also {
                    instance = it
                }
            }
        }

    }

    /*
    * This is the filter array contain the data for the respective filters
    * */
    private var orderPaymentStatusType: MutableSet<String>? = mutableSetOf(Constants.all_orders)
    private var orderEmployeeType: MutableSet<String>? = mutableSetOf(Constants.all_employee)
    private var orderTenderType: MutableSet<String>? = mutableSetOf(Constants.all_tenders)
    private var orderBookingType: MutableSet<String>? =
        mutableSetOf(Constants.all_booking_type, Constants.pos, Constants.online)


    // we need this list access in other fragments as well
    private var orderItems: ArrayList<Order?> = ArrayList()
    private var filterData: ArrayList<Order?> = ArrayList()

    // contain the list of all order. this is to manage the list after all the search and filters
    // do not apply any kind of operation on this. This is for the data management.
    private var allItemList: ArrayList<Order?> = ArrayList()


    private val binding by lazy {
        FragmentOrderHistoryBinding.inflate(layoutInflater)
    }

    private val preferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val myApp by lazy {
        MyApp.getInstance()
    }

    private var orderAdapter: OrderAdapter? = null

    private var popupMenu: PopupMenu? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeVariable()
        setUpClickListeners()
        getTheMerchantId()
        setUpPopupMenu()
    }

    // popup menu on the click of the menu button on the dashboard (only work for the admin merchant)
    private fun setUpPopupMenu() {
        popupMenu = PopupMenu(requireContext(), binding.menuButton)
        popupMenu?.menuInflater?.inflate(R.menu.custom_menu, popupMenu?.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.customMenu -> {
                    navigate(R.id.action_orderHistoryFragment_to_customFieldsFragment)
                    true
                }

                else -> false
            }
        }
    }


    private fun setUpListeners() {
        binding.apply {
            searchBar.doAfterTextChanged {
                if (this@OrderHistoryFragment::runnable.isInitialized)
                    handler.removeCallbacks(runnable)
                runnable = Runnable {
                    searchTheData(it.toString().uppercase().trim())
                }

                handler.postDelayed(runnable, Constants.debouncingTime)
            }
            setUpListenerForSpinners(
                recyclerHeader.orderPaymentStatusSpinner,
                FilterCategories.PaymentStatus.name
            )
            setUpListenerForSpinners(
                recyclerHeader.orderEmployeeNameSpinner,
                FilterCategories.EmployeeName.name
            )
            setUpListenerForSpinners(
                recyclerHeader.orderTenderSpinner,
                FilterCategories.TenderType.name
            )
            setUpListenerForSpinners(
                recyclerHeader.orderBookingTypeSpinner,
                FilterCategories.OrderBookingType.name
            )
        }
    }

    private fun setUpListenerForSpinners(view: Spinner, filter: String) {


        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                runOnBackgroundThread {
                    filterTheData(filter, p2)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                notImplementedLog
            }
        }
    }


    private fun updateTheFilterAppliedUi(view: MaterialTextView, p2: String) {
        runOnMainThread {
            view.text = p2
        }
    }

    private fun filterTheData(filter: String, p2: Int) {
        filterArray.forEach {
            if (it.type == filter) {
                it.index = p2
                applyFilterToList()
                return
            }
        }
        filterArray.add(FilterData(p2, filter))
    }

    private fun getPaymentMode(payments: List<Payment>?): String {
        var result = ""
        payments?.forEach {
            result += "${it.tender?.label}, "
        }
        return result
    }

    private fun applyFilterToList() {
        val matchArray: MutableList<String> = mutableListOf()
        val tenderIndex = filterArray[0].index
        val bookingIndex = filterArray[3].index
        val paymentStatusIndex = filterArray[1].index
        val employeeIndex = filterArray[2].index
        exceptionHandler {
            updateUI(matchArray, tenderIndex, bookingIndex, paymentStatusIndex, employeeIndex)
        }

        val isFilterApplied = isAllZero(
            tenderIndex,
            bookingIndex,
            paymentStatusIndex,
            employeeIndex
        )
        if (matchArray.isEmpty() || isFilterApplied) {
            return
        }

        updateTheFilterAppliedList(
            tenderIndex,
            bookingIndex,
            paymentStatusIndex,
            employeeIndex,
            matchArray
        )

    }

    private fun updateUI(
        matchArray: MutableList<String>,
        tenderIndex: Int?,
        bookingIndex: Int?,
        paymentStatusIndex: Int?,
        employeeIndex: Int?
    ) {
        tenderIndex?.also {
            orderTenderType?.toList()?.get(it)
                ?.let { it1 ->
                    if (it != 0) {
                        matchArray.add(it1)
                    }
                    updateTheFilterAppliedUi(binding.recyclerHeader.orderPriceFilter, it1)
                }
        }
        bookingIndex?.also {
            orderBookingType?.toList()?.get(it)
                ?.let { it1 ->
                    if (it != 0) {
                        matchArray.add(it1)
                    }
                    updateTheFilterAppliedUi(binding.recyclerHeader.orderTypeFilter, it1)
                }
        }
        paymentStatusIndex?.also {
            orderPaymentStatusType?.toList()?.get(it)
                ?.let { it1 ->
                    if (it != 0) {
                        matchArray.add(it1)
                    }
                    updateTheFilterAppliedUi(binding.recyclerHeader.orderStatusFilter, it1)
                }
        }
        employeeIndex?.also {
            orderEmployeeType?.toList()?.get(it)
                ?.let { it1 ->
                    if (it != 0) {
                        matchArray.add(it1)
                    }
                    updateTheFilterAppliedUi(binding.recyclerHeader.orderNameFilter, it1)
                }
        }
    }

    private fun updateTheFilterAppliedList(
        tenderIndex: Int?,
        bookingIndex: Int?,
        paymentStatusIndex: Int?,
        employeeIndex: Int?,
        matchArray: MutableList<String>
    ) {
        orderItems.clear()
        filterData.clear()
        allItemList.forEach {
            val employeeName = try {
                it?.employee?.jsonObject?.get(Constants.name).toString()
            } catch (e: Exception) {
                null
            }
            val booking = if (it?.isNotNullOnlineOrder == true) Constants.online else Constants.pos
            val paymentMode = getPaymentMode(it?.payments)

            // this  == 0  means that no filter is applied for this
            // so that for all filter the default case eg All Orders will work.
            if ((paymentStatusIndex == 0 || matchArray.contains(it?.paymentState?.name))
                && (employeeIndex == 0 || matchArray.contains(employeeName))
                && (bookingIndex == 0 || matchArray.contains(booking))
                && (tenderIndex == 0 || paymentMode.isInArray(matchArray))
            ) {
                orderItems.add(it)
                filterData.add(it)
            }
        }
        notifyTheAdapter()
    }


    // this will check whether user has applied any filter or not
    @SuppressLint("NotifyDataSetChanged")
    private fun isAllZero(
        tenderIndex: Int?,
        bookingIndex: Int?,
        paymentStatusIndex: Int?,
        employeeIndex: Int?
    ): Boolean {
        return if (tenderIndex == 0 && bookingIndex == 0 && paymentStatusIndex == 0 && employeeIndex == 0) {
            orderItems.clear()
            allItemList.forEach {
                orderItems.add(it)
            }
            runOnMainThread {
                binding.progressLayout.hideView()
                binding.orderRecycler.adapter?.notifyDataSetChanged()
            }
            true
        } else false

    }


    private fun searchTheData(p0: CharSequence?) {
        // if search bar is empty


        runOnBackgroundThread(Dispatchers.Default) {
            val isFilterApplied = isFilterNotApplied()

            // search bar is empty
            if (p0?.trim()?.isEmpty() == true) {

                // if filters are applied.
                if (isFilterApplied) {
                    orderItems.clear()
                    filterData.forEach {
                        orderItems.add(it)
                    }
                    notifyTheAdapter()
                    runOnMainThread {
                        binding.progressLayout.hideView()
                    }

                    return@runOnBackgroundThread
                }

                orderItems.clear()
                allItemList.forEach {
                    orderItems.add(it)
                }
                runOnMainThread {
                    binding.progressLayout.hideView()
                }

            }
            // if search bar is not empty
            else {
                searchIsNotEmpty(p0, isFilterApplied)
            }
            notifyTheAdapter()
        }
    }

    private fun searchIsNotEmpty(p0: CharSequence?, isFilterApplied: Boolean) {
        orderItems.clear()
        for (data in if (isFilterApplied) filterData else allItemList) {
            if (p0?.toString()
                    ?.let { matchWithData(data, it.lowercase()) } == true
            ) {
                orderItems.add(data)
            }
        }


        val item = orderItems.isEmpty()
        // search the order in using OrderConnector
        if (item && p0.toString().length > 7) {
            exceptionHandler {
                val result = myApp.getOrderConnector().getOrder(p0.toString())
                communicate(arrayListOf(result))
            }

        } else {
            isSearchItemVisibility(item, !item, item)
        }
    }

    private fun isFilterNotApplied(): Boolean {
        filterArray.forEach {
            if (it.index != 0) {
                return true
            }
        }
        return false
    }

    /*
    * This will match the user search string in the array and provide you the result.
    * */
    private fun matchWithData(data: Order?, search: String): Boolean {
        val result = exceptionHandlerWithReturn {
            val isPaymentStatusMatched = data?.paymentState?.name?.lowercase()
            val employeeName =
                data?.employee?.jsonObject?.get(Constants.name)?.toString()?.lowercase()
            val orderId = data?.id?.lowercase()
            val paymentCost = data?.total?.toString()
            val customer = if ((data?.customers?.size ?: Constants.defaultOffset) > 0) {
                getString(
                    R.string.getFullName,
                    data?.customers?.get(0)?.firstName,
                    data?.customers?.get(0)?.lastName
                )
            } else ""
            val notes = getOrderNotes(data?.lineItems)
            (customer.lowercase()
                .contains(search) || isPaymentStatusMatched?.startsWith(search) == true) || (employeeName?.contains(
                search
            ) == true) || (orderId?.startsWith(search) == true) || (paymentCost?.startsWith(search) == true) || notes.contains(
                search
            )
        }
        return result ?: Constants.defaultBoolean
    }

    private fun getOrderNotes(lineItems: List<LineItem>?): String {
        var result = ""
        lineItems?.forEach {
            if (it.note != null || it.note?.trim()?.isNotEmpty() == true) {
                result += it.note
            }
        }
        return result
    }


    private fun isSearchItemVisibility(layout: Boolean, loader: Boolean, views: Boolean) {
        runOnMainThread {
            binding.progressLoader.isVisible = loader
            binding.noTextFoundUpper.isVisible = views
            binding.noTextFoundLower.isVisible = views
            binding.progressLayout.isVisible = layout
        }
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
        val orderStatusAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            orderEmployeeType?.toMutableList() ?: emptyList()
        )
        orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recyclerHeader.orderEmployeeNameSpinner.adapter = orderStatusAdapter
    }


    private fun setupSpinnerBooking() {
        val orderStatusAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            orderBookingType?.toMutableList() ?: emptyList()
        )
        orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recyclerHeader.orderBookingTypeSpinner.adapter = orderStatusAdapter
    }

    private fun setupSpinnerTender() {
        val orderStatusAdapter =
            ArrayAdapter(
                requireContext(),
                R.layout.item_spinner,
                orderTenderType?.toMutableList() ?: emptyList()
            )
        orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recyclerHeader.orderTenderSpinner.adapter = orderStatusAdapter
    }

    private fun setupSpinnerStatus() {
        val orderStatusAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            orderPaymentStatusType?.toMutableList() ?: emptyList()
        )
        orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recyclerHeader.orderPaymentStatusSpinner.adapter = orderStatusAdapter
    }


    override fun onStart() {
        super.onStart()
        // this isClicked helps in the filter preserve
        if (!isClicked) return
        getTheOrderData()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getTheOrderData() {
        runOnBackgroundThread {
            val merchantName = myApp.getCloverAccount().name.split("|")[0]
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderItems.clear()
                allItemList.clear()
                orderData?.forEach {
                    if (!isAdmin) {
                        checkForAdminRole(it, merchantName)
                    }
                    allItemList.add(it)
                    orderItems.add(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                debugSnackBar(getString(R.string.there_is_issue_with_your_account))
            }
            runOnMainThread {
                binding.orderRecycler.adapter?.notifyDataSetChanged()
                binding.syncingText.hideView()
            }

            CoroutineScope(Dispatchers.Default).launch {
                updateTheFilterData()
            }
        }
    }

    // get the filters from the array and pass to the spinners so that user can
    // apply the filters. So filters are dynamic in nature if we have no data for a particular filter then
    // that filter will have only the default option of All.
    private suspend fun updateTheFilterData() {
        val job = CoroutineScope(Dispatchers.Default).async {
            orderItems.forEach {
                it?.paymentState?.name?.let { it1 ->
                    orderPaymentStatusType?.add(it1)
                }
                it?.payments?.forEach { pay ->
                    orderTenderType?.add(pay.tender.label)
                }
                val employeeName = try {
                    it?.employee?.jsonObject?.get(Constants.name)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (employeeName != null) {
                    orderEmployeeType?.add(employeeName.toString())
                }
            }
        }
        job.await()
        runOnMainThread {
            setupSpinnerBooking()
            setupSpinners()
            setupSpinnerTender()
            setupSpinnerStatus()
            setUpListeners()
        }
    }


    /*
    * This Checks that does any order contain the name of the merchant which is currently
    * using the app so if we found the merchant name then we check the role of the merchant
    * and that merchant with Admin Access will have the access of the Set Custom Menu option .
    * Rest of all the role's cannot access the Set Custom Menu option.
    * */
    private fun checkForAdminRole(it: Order?, merchantName: String) {
        try {
            val name = it?.employee?.jsonObject?.get(Constants.name).toString()
            if (name.trim() == merchantName.trim()) {
                try {
                    val role = it?.employee?.jsonObject?.get(Constants.role).toString()
                    if (role.lowercase() == Constants.ADMIN.lowercase()) {
                        runOnMainThread {
                            isAdmin = !isAdmin
                            binding.menuButton.showView()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnMainThread {
                        binding.menuButton.hideView()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun notifyTheAdapter() {
        runOnMainThread {
            val item = orderItems.isEmpty()
            isSearchItemVisibility(item, !item, item)
            binding.orderRecycler.adapter?.notifyDataSetChanged()
        }
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
                getTheOrderData()
            }
            /*
            * Every time from the firebase realtime database we will check that
            * */
            menuButton.setOnClickListener {
                runOnBackgroundThread {
                    FirebaseRealtimeDataBaseManager.getInstance().getData(requireContext())
                }
                popupMenu?.show()
            }
            recyclerHeader.apply {
                orderStatus.setOnClickListener { orderPaymentStatusSpinner.performClick() }
                orderName.setOnClickListener { orderEmployeeNameSpinner.performClick() }
                orderType.setOnClickListener { orderBookingTypeSpinner.performClick() }
                orderPrice.setOnClickListener { orderTenderSpinner.performClick() }
            }
        }
    }


    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        val direction = orderItems[orderPosition]?.let {
            OrderHistoryFragmentDirections.actionOrderHistoryFragmentToOrderDetailFragment(
                it
            )
        }

        if (direction != null) {
            navigateDirection(direction)
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        popupMenu?.dismiss()
    }


    override fun onPause() {
        super.onPause()
        isClicked = false
    }


    // update the order as note for the line item is added.
    fun updateTheOrder(orderData: Order?) {
        runOnBackgroundThread {
            for ((pos, i) in orderItems.withIndex()) {
                if (i?.id == orderData?.id) {
                    orderItems[pos] = orderData
                    runOnBackgroundThread {
                        binding.orderRecycler.adapter?.notifyItemChanged(pos)
                    }
                    break
                }
            }
        }
    }

    override fun communicate(orderItems: ArrayList<Order?>) {

        orderItems.forEach {
            this.orderItems.add(it)
        }
        val item = this.orderItems.isEmpty()
        isSearchItemVisibility(item, !item, item)
    }
}

