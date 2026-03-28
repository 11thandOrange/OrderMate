package com.orderMate.fragment.orderHistory


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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.billing.AppMeteredEvent
import com.clover.sdk.v3.employees.Employee
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.google.android.material.textview.MaterialTextView
import com.orderMate.R
import com.orderMate.adapters.OrderAdapter
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.communicators.InterCommunication
import com.orderMate.databinding.FragmentOrderHistoryBinding
import com.orderMate.utils.Constants
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.Constants.Companion.fbCategory
import com.orderMate.utils.Constants.Companion.fbStatus
import com.orderMate.utils.Constants.Companion.fbSubcategory
import com.orderMate.utils.Constants.Companion.fbType
import com.orderMate.utils.Constants.Companion.notImplementedLog
import com.orderMate.utils.FilterCategories
import com.orderMate.utils.ModalDialogCategories
import com.orderMate.utils.MyApp
import com.orderMate.utils.MyApp.Companion.filterArray
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.getOnlyFirstName
import com.orderMate.utils.hideView
import com.orderMate.utils.isInArray
import com.orderMate.utils.navigate
import com.orderMate.utils.navigateDirection
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OrderHistoryFragment : Fragment(), IOrderItemClickListener, InterCommunication {
    val data = AppMeteredEvent()

    companion object {
        // This Bit is used for the verifying that does the current used merchant has admin access.
        var isClicked: Boolean = true
        private var instance: OrderHistoryFragment? = null

        fun getInstance(): OrderHistoryFragment {
            return instance ?: synchronized(this) {
                OrderHistoryFragment().also {
                    instance = it
                }
            }
        }

        val notesFilter: HashMap<String, MutableList<String>> = hashMapOf()
        var userSelectedPostion: Int = 0
        var userClikcedData : Order? = null
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
                    // Navigate to Settings instead of deprecated CustomFieldsFragment
                    navigate(R.id.settingsFragment)
                    true
                }

                else -> false
            }
        }
    }


    private fun setUpListeners() {
        binding.apply {
            searchBar.doAfterTextChanged {
                if (this@OrderHistoryFragment::runnable.isInitialized) handler.removeCallbacks(
                    runnable
                )
                runnable = Runnable {
                    searchTheData(it.toString().trim())
                }
                handler.postDelayed(runnable, Constants.debouncingTime)
            }


            setUpListenerForSpinners(
                recyclerHeader.orderPaymentStatusSpinner, FilterCategories.PaymentStatus.name
            )

            setUpListenerForSpinners(
                recyclerHeader.orderEmployeeNameSpinner, FilterCategories.EmployeeName.name
            )
            setUpListenerForSpinners(
                recyclerHeader.orderTenderSpinner, FilterCategories.TenderType.name
            )
            setUpListenerForSpinners(
                recyclerHeader.orderBookingTypeSpinner, FilterCategories.OrderBookingType.name
            )
            setUpListenerForSpinners(
                notesSpinnerDialog.orderTypeSpinner, ModalDialogCategories.OrderType.name
            )
            setUpListenerForSpinners(
                notesSpinnerDialog.orderStatusSpinner, ModalDialogCategories.OrderProgress.name
            )
            setUpListenerForSpinners(
                notesSpinnerDialog.orderCategorySpinner, ModalDialogCategories.OrderCategories.name
            )
            setUpListenerForSpinners(
                notesSpinnerDialog.orderSubcategorySpinner,
                ModalDialogCategories.OrderSubCategories.name
            )

        }
    }


    private fun setUpListenerForSpinners(view: Spinner, filter: String) {


        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
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
            if (it.key == filter) {
                filterArray[it.key] = p2
                applyFilterToList()
                return
            }
        }
        filterArray[filter] = p2
    }

    override fun onResume() {
        super.onResume()
    }

    private fun getPaymentMode(payments: List<Payment>?): String {
        var result = ""
        payments?.forEach {
            result += "${it.tender?.label}, "
        }
        return result
    }

    private fun applyFilterToList() {
        val matchArray: HashMap<String, String> = hashMapOf()
        exceptionHandler {
            updateUI(
                matchArray
            )
        }
        val isFilterApplied = isAllZero(
            filterArray
        )
        if (matchArray.isEmpty() || isFilterApplied) {
            return
        }
        updateTheFilterAppliedList(
            matchArray
        )
    }

    private fun updateUI(
        matchArray: HashMap<String, String>,
    ) {

        filterArray.forEach {
            when (it.key) {
                FilterCategories.TenderType.name -> filterArray[FilterCategories.TenderType.name]?.also { value ->
                    orderTenderType?.toList()?.get(value)?.let { it1 ->
                        if (value != 0) {
                            matchArray[FilterCategories.TenderType.name] = it1
                        }
                        updateTheFilterAppliedUi(binding.recyclerHeader.orderPriceFilter, it1)
                    }
                }

                FilterCategories.OrderBookingType.name -> filterArray[FilterCategories.OrderBookingType.name]?.also { value ->
                    orderBookingType?.toList()?.get(value)?.let { it1 ->
                        if (value != 0) {
                            matchArray[FilterCategories.OrderBookingType.name] = it1
                        }
                        updateTheFilterAppliedUi(binding.recyclerHeader.orderTypeFilter, it1)
                    }
                }

                FilterCategories.PaymentStatus.name -> filterArray[FilterCategories.PaymentStatus.name]?.also { value ->
                    orderPaymentStatusType?.toList()?.get(value)?.let { it1 ->
                        if (value != 0) {
                            matchArray[FilterCategories.PaymentStatus.name] = it1
                        }
                        updateTheFilterAppliedUi(binding.recyclerHeader.orderStatusFilter, it1)
                    }
                }

                FilterCategories.EmployeeName.name -> filterArray[FilterCategories.EmployeeName.name]?.also { value ->
                    orderEmployeeType?.toList()?.get(value)?.let { it1 ->
                        if (value != 0) {
                            matchArray[FilterCategories.EmployeeName.name] = it1
                        }
                        updateTheFilterAppliedUi(binding.recyclerHeader.orderNameFilter, it1)
                    }
                }

                else -> parseTheNotesDataAndGenerateString(matchArray)

            }
        }
    }

    private fun parseTheNotesDataAndGenerateString(matchArray: HashMap<String, String>) {
        var resultant = ""
        filterArray.forEach {
            when (it.key) {
                ModalDialogCategories.OrderProgress.name -> filterArray[ModalDialogCategories.OrderProgress.name]?.also { value ->
                    if (value != 0) {
                        val required = notesFilter[fbStatus]?.get(value) ?: ""
                        matchArray[ModalDialogCategories.OrderProgress.name] = required
                        resultant += required + getString(R.string.commas)
                    }
                }

                ModalDialogCategories.OrderType.name -> filterArray[ModalDialogCategories.OrderType.name]?.also { value ->
                    if (value != 0) {
                        val required = notesFilter[fbType]?.get(value) ?: ""
                        matchArray[ModalDialogCategories.OrderType.name] = required
                        resultant += required + getString(R.string.commas)
                    }
                }

                ModalDialogCategories.OrderCategories.name -> filterArray[ModalDialogCategories.OrderCategories.name]?.also { value ->
                    if (value != 0) {
                        val required = notesFilter[fbCategory]?.get(value) ?: ""
                        matchArray[ModalDialogCategories.OrderCategories.name] = required
                        resultant += required + getString(R.string.commas)
                    }
                }

                ModalDialogCategories.OrderSubCategories.name -> filterArray[ModalDialogCategories.OrderSubCategories.name]?.also { value ->
                    if (value != 0) {
                        val required = notesFilter[fbSubcategory]?.get(value) ?: ""
                        matchArray[ModalDialogCategories.OrderSubCategories.name] = required
                        resultant += required + getString(R.string.commas)
                    }
                }
            }
        }
        resultant = if (resultant.isEmpty()) {
            getString(R.string.all_orders)
        } else {
            if (resultant.length > 2) resultant.substring(0, resultant.length - 2) else resultant
        }
        updateTheFilterAppliedUi(binding.recyclerHeader.orderNotesFilterValue, resultant)
    }


    private fun updateTheFilterAppliedList(
        matchArray: HashMap<String, String>
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

            if ((filterArray[FilterCategories.PaymentStatus.name] == 0 || matchArray.values.contains(
                    it?.paymentState?.name
                )) && (filterArray[FilterCategories.EmployeeName.name] == 0 || matchArray.values.contains(
                    employeeName
                )) && (filterArray[FilterCategories.OrderBookingType.name] == 0 || matchArray.values.contains(
                    booking
                )) && (filterArray[FilterCategories.TenderType.name] == 0 || paymentMode.isInArray(
                    matchArray
                )) && (isNoteDataMatched(matchArray, it?.lineItems))
            ) {
                orderItems.add(it)
                filterData.add(it)
            }
        }
        notifyTheAdapter()
    }

    // This function return true if the note contain the applied filter else false
    private fun isNoteDataMatched(
        matchArray: HashMap<String, String>, lineItems: MutableList<LineItem?>?
    ): Boolean {


        if (filterArray[ModalDialogCategories.OrderCategories.name] == 0 && filterArray[ModalDialogCategories.OrderSubCategories.name] == 0 && filterArray[ModalDialogCategories.OrderType.name] == 0 && filterArray[ModalDialogCategories.OrderProgress.name] == 0) {
            return true
        }

        val categories = matchArray[ModalDialogCategories.OrderCategories.name]?.trim()
            ?: Constants.defaultString
        val subCategory = matchArray[ModalDialogCategories.OrderSubCategories.name]?.trim()
            ?: Constants.defaultString
        val type =
            matchArray[ModalDialogCategories.OrderType.name]?.trim() ?: Constants.defaultString
        val status =
            matchArray[ModalDialogCategories.OrderProgress.name]?.trim() ?: Constants.defaultString


        for (i in lineItems ?: emptyList()) {
            // if the note is empty then do nothing
            if (i?.note == null) {
                continue
            }
            if ((i.note.contains(categories, true) && categories.trim()
                    .isNotEmpty()) || (i.note.contains(subCategory, true) && subCategory.trim()
                    .isNotEmpty()) || (i.note.contains(type, true) && type.trim()
                    .isNotEmpty()) || (i.note.contains(status, true) && status.trim().isNotEmpty())
            ) {
                return true
            }
        }

        return false
    }

    // this will check whether user has applied any filter or not
    @SuppressLint("NotifyDataSetChanged")
    private fun isAllZero(
        array: Map<String, Int>
    ): Boolean {
        return if (doesAllAreZero(array)) {
            orderItems.clear()
            exceptionHandler {
                for (it in allItemList) {
                    orderItems.add(it)
                }
            }
            runOnMainThread {
                binding.progressLayout.hideView()
                binding.orderRecycler.adapter?.notifyDataSetChanged()
            }
            true
        } else false

    }


    // if not filter is Applied then then all the index are positioned to zero
    private fun doesAllAreZero(array: Map<String, Int>): Boolean {
        array.forEach {
            if (it.value != 0) return false
        }
        return true
    }


    private fun searchTheData(p0: String?) {
        // if search bar is empty

        runOnBackgroundThread(Dispatchers.Default) {
            val isFilterApplied = isFilterNotApplied()

            // search bar is empty
            if (p0?.trim()?.isEmpty() == true) {

                // if filters are applied.
                if (isFilterApplied) {
                    orderItems.clear()
                    exceptionHandler {
                        filterData.forEach {
                            orderItems.add(it)
                        }
                        notifyTheAdapter()
                    }
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

    private fun searchIsNotEmpty(p0: String?, isFilterApplied: Boolean) {
        orderItems.clear()
        allItemList.forEach {}
        for (data in if (isFilterApplied) filterData else allItemList) {
            if (p0?.let { matchWithData(data, it.lowercase()) } == true) {
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
            if (it.value != 0) {
                return true
            }
        }
        return false
    }

    /*
    * This will match the user search string in the array and provide you the result.
    * */
    private fun matchWithData(data: Order?, search: String): Boolean {
        var employeeName: String? = ""
        val result = exceptionHandlerWithReturn {
            var customerContact: Pair<String, String> = Pair(" ", "")
            val isPaymentStatusMatched = data?.paymentState?.name?.lowercase()
            exceptionHandler {
                employeeName =
                    data?.employee?.jsonObject?.get(Constants.name)?.toString()?.lowercase()
            }
            val orderId = data?.id?.lowercase()
            val paymentCost = data?.total?.toString()?.lowercase()
            val customer = if ((data?.customers?.size ?: Constants.defaultOffset) > 0) {
                customerContact = getCustomerContactDetails(data?.customers?.get(0))
                getString(
                    R.string.getFullName,
                    data?.customers?.get(0)?.firstName?.trim(),
                    data?.customers?.get(0)?.lastName?.trim()
                ).lowercase()
            } else ""
            val notes = getOrderNotes(data?.lineItems)
            (customer.contains(search, true) || isPaymentStatusMatched?.startsWith(
                search, true
            ) == true) || (employeeName?.contains(
                search, true
            ) == true) || (orderId?.startsWith(search, true) == true) || (paymentCost?.startsWith(
                search, true
            ) == true) || notes.contains(search, true) || customerContact.first.contains(
                search, true
            ) || customerContact.second.contains(search, true)
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

    private fun setupSpinners(spinner: Spinner, list: List<String>) {

        if (list.isEmpty()) {
            spinner.visibility = View.GONE
            return
        }
        val orderStatusAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner, list)
        orderStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = orderStatusAdapter
    }

    override fun onStart() {
        super.onStart()

        // this isClicked helps in the filter preserve
        if (!isClicked) return
        getTheOrderData()

    }


    @SuppressLint("NotifyDataSetChanged")
    fun getTheOrderData(isFromOrderDetail : Boolean = false) {
        runOnBackgroundThread {
            getTheEmployeeDataForAdminRole()
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderItems.clear()
                allItemList.clear()
                orderData?.forEach {
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
                runOnMainThread {
                    if(!isFromOrderDetail){
                        binding.progressLayoutNoData.hideView()
                    }
                    updateTheFilterData()
                }

            }
        }

    }

    private fun getTheEmployeeDataForAdminRole() {
        try{
            val employeeData: Employee = myApp.getEmployeeConnector()?.employee?:Employee()
            if (employeeData.role.name.equals(Constants.ADMIN, true)) {
                runOnMainThread {
                    binding.menuButton.showView()
                }
            } else {
                runOnMainThread {
                    binding.menuButton.hideView()
                }
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }


    // get the filters from the array and pass to the spinners so that user can
    // apply the filters. So filters are dynamic in nature if we have no data for a particular filter then
    // that filter will have only the default option of All.

    private  fun updateTheFilterData() {
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

                exceptionHandler {
                    // V2: Read widgets from WidgetManager instead of V1 CustomItemJson
                    addDataIntoNotesArray()
                }
            }
        runOnMainThread {
            setupSpinners(
                binding.recyclerHeader.orderBookingTypeSpinner,
                orderBookingType?.toMutableList() ?: emptyList()
            )
            setupSpinners(
                binding.recyclerHeader.orderEmployeeNameSpinner,
                orderEmployeeType?.toMutableList() ?: emptyList()
            )
            setupSpinners(
                binding.recyclerHeader.orderTenderSpinner,
                orderTenderType?.toMutableList() ?: emptyList()
            )
            setupSpinners(
                binding.recyclerHeader.orderPaymentStatusSpinner,
                orderPaymentStatusType?.toMutableList() ?: emptyList()
            )
            setupSpinners(
                binding.notesSpinnerDialog.orderCategorySpinner,
                notesFilter[fbCategory] ?: emptyList()
            )
            setupSpinners(
                binding.notesSpinnerDialog.orderSubcategorySpinner,
                notesFilter[fbSubcategory] ?: emptyList()
            )
            setupSpinners(
                binding.notesSpinnerDialog.orderTypeSpinner, notesFilter[fbType] ?: emptyList()
            )
            setupSpinners(
                binding.notesSpinnerDialog.orderStatusSpinner, notesFilter[fbStatus] ?: emptyList()
            )
            setUpListeners()
        }
    }

    /**
     * V2: Populate filter arrays from WidgetManager instead of V1 CustomItemJson
     */
    private fun addDataIntoNotesArray() {
        notesFilter.clear()
        notesFilter[getString(R.string.all_orders)] = mutableListOf()
        
        val widgets = WidgetManager.getInstance(requireContext()).getFilterableWidgets()
        for (widget in widgets) {
            if (widget.options.isEmpty()) {
                continue
            }
            val resultant: MutableList<String> = mutableListOf()
            resultant.add(getString(R.string.all_orders))
            widget.options.forEach { option ->
                resultant.add(option.label)
            }
            notesFilter[widget.label.trim()] = resultant
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
                binding.progressLayoutNoData.showView()
                searchBar.text?.clear()
                getTheOrderData()
            }/*
            * Every time from the firebase realtime database we will check that
            * */
            menuButton.setOnClickListener {
                // V2: Widget data is loaded by MainActivityRedesign, no need to refresh here
                popupMenu?.show()
            }
            recyclerHeader.apply {
                orderStatus.setOnClickListener { orderPaymentStatusSpinner.performClick() }
                orderName.setOnClickListener { orderEmployeeNameSpinner.performClick() }
                orderType.setOnClickListener { orderBookingTypeSpinner.performClick() }
                orderPrice.setOnClickListener { orderTenderSpinner.performClick() }
                orderId.setOnClickListener {
                    changeColorOfNotesField(!notesSpinnerDialog.container.isVisible)
                }
            }
            notesSpinnerDialog.apply {
                orderType.setOnClickListener { orderTypeSpinner.performClick() }
                orderStatus.setOnClickListener { orderStatusSpinner.performClick() }
                orderCategory.setOnClickListener { orderCategorySpinner.performClick() }
                orderSubcategory.setOnClickListener { orderSubcategorySpinner.performClick() }
                clearButton.setOnClickListener {
                    removeFiltersForNote()
                    searchBar.text?.clear()
                    changeColorOfNotesField(false)
                }
                closeButton.setOnClickListener {
                    changeColorOfNotesField(false)
                }
            }
        }
    }


    private fun changeColorOfNotesField(isVisible: Boolean) {
        binding.apply {
            notesSpinnerDialog.container.isVisible = isVisible
            recyclerHeader.orderIdParent.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), if (isVisible) R.color.darkBlue else R.color.header_blue
                )
            )
        }
    }


    private fun removeFiltersForNote() {
        filterArray[ModalDialogCategories.OrderCategories.name] = 0
        filterArray[ModalDialogCategories.OrderProgress.name] = 0
        filterArray[ModalDialogCategories.OrderType.name] = 0
        filterArray[ModalDialogCategories.OrderSubCategories.name] = 0
        binding.recyclerHeader.orderNotesFilterValue.text = getString(R.string.all_orders)
        runOnBackgroundThread {
            applyFilterToList()
        }
    }


    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        userSelectedPostion = orderPosition
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
        changeColorOfNotesField(false)
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

    fun updateTheSpinners(isSuccess: Boolean) {
        if (isSuccess) {
            Toast.makeText(
                requireContext(),
                if (isSuccess) "Data is Uploaded Successfully" else "Please try again! Failed to upload the Data",
                Toast.LENGTH_LONG
            ).show()
        }
            updateTheFilterData()


    }


}

