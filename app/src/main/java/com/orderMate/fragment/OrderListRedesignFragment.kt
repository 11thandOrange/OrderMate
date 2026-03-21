package com.orderMate.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.payments.Payment
import com.google.gson.Gson
import com.orderMate.R
import com.orderMate.adapters.OrderCardRedesignAdapter
import com.orderMate.communicators.IOrderItemClickListener
import com.orderMate.databinding.FragmentOrderListRedesignBinding
import com.orderMate.modals.CustomItemJson
import com.orderMate.utils.Constants
import com.orderMate.utils.FilterCategories
import com.orderMate.utils.ModalDialogCategories
import com.orderMate.utils.MyApp
import com.orderMate.utils.MyApp.Companion.filterArray
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.exceptionHandlerWithReturn
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.hideView
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * iOS-style Order List Fragment (#80, #81 requirement)
 * 
 * Redesigned list view with:
 * - Glassmorphic order cards
 * - Modern search with date picker
 * - Filter pills instead of spinners
 * - Sync button with animation
 * 
 * Reuses backend logic from OrderHistoryFragment.
 */
class OrderListRedesignFragment : Fragment(), IOrderItemClickListener {

    companion object {
        private var instance: OrderListRedesignFragment? = null
        
        fun getInstance(): OrderListRedesignFragment {
            return instance ?: synchronized(this) {
                OrderListRedesignFragment().also { instance = it }
            }
        }
        
        val notesFilter: HashMap<String, MutableList<String>> = hashMapOf()
        var userSelectedPosition: Int = 0
        var userClickedData: Order? = null
    }

    private var _binding: FragmentOrderListRedesignBinding? = null
    private val binding get() = _binding!!

    // Filter data
    private var orderPaymentStatusType: MutableSet<String> = mutableSetOf(Constants.all_orders)
    private var orderEmployeeType: MutableSet<String> = mutableSetOf(Constants.all_employee)
    private var orderTenderType: MutableSet<String> = mutableSetOf(Constants.all_tenders)
    private var orderBookingType: MutableSet<String> = mutableSetOf(Constants.all_booking_type, Constants.pos, Constants.online)

    // Order lists
    private var orderItems: ArrayList<Order?> = ArrayList()
    private var filterData: ArrayList<Order?> = ArrayList()
    private var allItemList: ArrayList<Order?> = ArrayList()

    // Selected date filter
    private var selectedDateFilter: Date? = null

    private val preferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val myApp by lazy {
        MyApp.getInstance()
    }

    private var orderAdapter: OrderCardRedesignAdapter? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchRunnable: Runnable

    private var isSyncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListRedesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeRecyclerView()
        setupClickListeners()
        setupSearchListener()
    }

    override fun onStart() {
        super.onStart()
        if (orderItems.isEmpty()) {
            loadOrders()
        }
    }

    /**
     * Called from filter button or pull-to-refresh to sync orders
     */
    fun triggerSync() {
        syncOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== Initialization ====================

    private fun initializeRecyclerView() {
        binding.ordersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            orderAdapter = OrderCardRedesignAdapter(orderItems, this@OrderListRedesignFragment)
            adapter = orderAdapter
        }
    }

    private fun setupClickListeners() {
        // Filter button
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        // Reset button
        binding.resetButton.setOnClickListener {
            resetFilters()
        }

        // Calendar icon for date picker
        binding.calendarIcon.setOnClickListener {
            showDatePicker()
        }
    }

    private fun resetFilters() {
        // Clear all filters
        filterArray.keys.forEach { filterArray[it] = 0 }
        selectedDateFilter = null
        binding.searchInput.text?.clear()
        binding.searchInput.hint = getString(R.string.search_orders)
        
        // Hide pills
        binding.filterPillsScroll.visibility = View.GONE
        binding.filterPillsContainer.removeAllViews()
        
        // Reload all orders
        orderItems.clear()
        allItemList.forEach { orderItems.add(it) }
        filterData.clear()
        
        updateResultsInfo()
        notifyAdapter()
        updateEmptyState()
        
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun setupSearchListener() {
        binding.searchInput.doAfterTextChanged { text ->
            if (this::searchRunnable.isInitialized) {
                handler.removeCallbacks(searchRunnable)
            }
            searchRunnable = Runnable {
                searchOrders(text.toString().trim())
            }
            handler.postDelayed(searchRunnable, Constants.debouncingTime)
        }
    }

    // ==================== Data Loading ====================

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders() {
        showLoading(true)
        
        runOnBackgroundThread {
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
                showLoading(false)
                updateResultsInfo()
                orderAdapter?.notifyDataSetChanged()
                updateEmptyState()
            }

            // Update filter options
            CoroutineScope(Dispatchers.Default).launch {
                updateFilterOptions()
            }
        }
    }

    private fun syncOrders() {
        isSyncing = true
        binding.searchInput.text?.clear()
        selectedDateFilter = null
        
        // Show syncing indicator
        binding.syncingContainer.showView()

        runOnBackgroundThread {
            try {
                val orderData = myApp.getOrderConnector().getOrders(mutableListOf())
                orderItems.clear()
                allItemList.clear()
                filterData.clear()
                
                orderData?.forEach {
                    allItemList.add(it)
                    orderItems.add(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnMainThread {
                isSyncing = false
                binding.syncingContainer.hideView()
                
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
                
                Toast.makeText(requireContext(), "Orders synced", Toast.LENGTH_SHORT).show()
            }

            CoroutineScope(Dispatchers.Default).launch {
                updateFilterOptions()
            }
        }
    }

    // ==================== Search ====================

    private fun searchOrders(query: String?) {
        runOnBackgroundThread(Dispatchers.Default) {
            val isFilterApplied = isFilterActive()

            if (query.isNullOrEmpty()) {
                // Empty search - show all or filtered
                orderItems.clear()
                if (isFilterApplied) {
                    filterData.forEach { orderItems.add(it) }
                } else {
                    allItemList.forEach { orderItems.add(it) }
                }
            } else {
                // Search in data
                orderItems.clear()
                val sourceList = if (isFilterApplied) filterData else allItemList
                
                for (order in sourceList) {
                    if (matchesSearch(order, query.lowercase())) {
                        orderItems.add(order)
                    }
                }

                // If no results and query looks like order ID, try direct lookup
                if (orderItems.isEmpty() && query.length > 7) {
                    exceptionHandler {
                        val result = myApp.getOrderConnector().getOrder(query)
                        if (result != null) {
                            orderItems.add(result)
                        }
                    }
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun matchesSearch(order: Order?, query: String): Boolean {
        if (order == null) return false
        
        return exceptionHandlerWithReturn {
            val orderId = order.id?.lowercase() ?: ""
            val customerName = try {
                val customer = order.customers?.firstOrNull()
                "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".lowercase()
            } catch (e: Exception) { "" }
            
            val employeeName = try {
                order.employee?.jsonObject?.get(Constants.name)?.toString()?.lowercase() ?: ""
            } catch (e: Exception) { "" }
            
            val paymentStatus = order.paymentState?.name?.lowercase() ?: ""
            
            val customerContact = try {
                getCustomerContactDetails(order.customers?.firstOrNull())
            } catch (e: Exception) { Pair("", "") }

            // Check line item notes
            val notesMatch = order.lineItems?.any { lineItem ->
                lineItem?.note?.lowercase()?.contains(query) == true
            } ?: false

            orderId.contains(query) ||
            customerName.contains(query) ||
            employeeName.contains(query) ||
            paymentStatus.contains(query) ||
            customerContact.first.lowercase().contains(query) ||
            customerContact.second.lowercase().contains(query) ||
            notesMatch
        } ?: false
    }

    // ==================== Date Filter ====================

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDateFilter?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderMate_Dialog,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateFilter = calendar.time
                filterByDate(calendar.time)
                updateDateFilterUI()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterByDate(date: Date) {
        runOnBackgroundThread {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val targetDate = dateFormat.format(date)

            orderItems.clear()
            val sourceList = if (isFilterActive()) filterData else allItemList

            for (order in sourceList) {
                val orderDate = order?.createdTime?.let {
                    dateFormat.format(Date(it))
                }
                if (orderDate == targetDate) {
                    orderItems.add(order)
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun updateDateFilterUI() {
        // Could show a pill indicating date filter is active
        selectedDateFilter?.let {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            binding.searchInput.hint = "Filtering: ${dateFormat.format(it)}"
        }
    }

    // ==================== Filters ====================

    private fun showFilterDialog() {
        // TODO: Implement FilterDialogFragment
        Toast.makeText(requireContext(), "Filter dialog coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun isFilterActive(): Boolean {
        filterArray.forEach {
            if (it.value != 0) return true
        }
        return false
    }

    private fun updateFilterOptions() {
        orderPaymentStatusType.clear()
        orderPaymentStatusType.add(Constants.all_orders)
        
        orderEmployeeType.clear()
        orderEmployeeType.add(Constants.all_employee)
        
        orderTenderType.clear()
        orderTenderType.add(Constants.all_tenders)

        orderItems.forEach { order ->
            order?.paymentState?.name?.let { orderPaymentStatusType.add(it) }
            
            order?.payments?.forEach { payment ->
                orderTenderType.add(payment.tender.label)
            }
            
            val employeeName = try {
                order?.employee?.jsonObject?.get(Constants.name)?.toString()
            } catch (e: Exception) { null }
            
            employeeName?.let { orderEmployeeType.add(it) }
        }

        // Load custom notes filters
        exceptionHandler {
            val data = preferenceManager.getString(Constants.customMenuJson)
            val result = Gson().fromJson(data, CustomItemJson::class.java)
            loadNotesFilters(result)
        }
    }

    private fun loadNotesFilters(result: CustomItemJson?) {
        notesFilter.clear()
        notesFilter[getString(R.string.all_orders)] = mutableListOf()
        
        result?.types?.forEach { type ->
            if (type.list.isNotEmpty()) {
                val options = mutableListOf<String>()
                options.add(getString(R.string.all_orders))
                type.list.forEach { options.add(it) }
                notesFilter[type.name.trim()] = options
            }
        }
    }

    fun applyFilters(matchArray: HashMap<String, String>) {
        runOnBackgroundThread {
            orderItems.clear()
            filterData.clear()

            allItemList.forEach { order ->
                val employeeName = try {
                    order?.employee?.jsonObject?.get(Constants.name).toString()
                } catch (e: Exception) { null }
                
                val booking = if (order?.isNotNullOnlineOrder == true) Constants.online else Constants.pos
                val paymentMode = getPaymentMode(order?.payments)

                val matchesPayment = filterArray[FilterCategories.PaymentStatus.name] == 0 ||
                        matchArray.values.contains(order?.paymentState?.name)
                val matchesEmployee = filterArray[FilterCategories.EmployeeName.name] == 0 ||
                        matchArray.values.contains(employeeName)
                val matchesBooking = filterArray[FilterCategories.OrderBookingType.name] == 0 ||
                        matchArray.values.contains(booking)
                val matchesTender = filterArray[FilterCategories.TenderType.name] == 0 ||
                        paymentMode.contains(matchArray.values.firstOrNull() ?: "")
                val matchesNotes = isNoteDataMatched(matchArray, order?.lineItems)

                if (matchesPayment && matchesEmployee && matchesBooking && matchesTender && matchesNotes) {
                    orderItems.add(order)
                    filterData.add(order)
                }
            }

            runOnMainThread {
                updateResultsInfo()
                notifyAdapter()
                updateEmptyState()
            }
        }
    }

    private fun getPaymentMode(payments: List<Payment>?): String {
        return payments?.joinToString(", ") { it.tender?.label ?: "" } ?: ""
    }

    private fun isNoteDataMatched(matchArray: HashMap<String, String>, lineItems: MutableList<LineItem?>?): Boolean {
        // If no note filters applied, match all
        if (filterArray[ModalDialogCategories.OrderCategories.name] == 0 &&
            filterArray[ModalDialogCategories.OrderSubCategories.name] == 0 &&
            filterArray[ModalDialogCategories.OrderType.name] == 0 &&
            filterArray[ModalDialogCategories.OrderProgress.name] == 0) {
            return true
        }

        val category = matchArray[ModalDialogCategories.OrderCategories.name]?.trim() ?: ""
        val subCategory = matchArray[ModalDialogCategories.OrderSubCategories.name]?.trim() ?: ""
        val type = matchArray[ModalDialogCategories.OrderType.name]?.trim() ?: ""
        val status = matchArray[ModalDialogCategories.OrderProgress.name]?.trim() ?: ""

        lineItems?.forEach { item ->
            val note = item?.note ?: return@forEach
            if ((category.isNotEmpty() && note.contains(category, true)) ||
                (subCategory.isNotEmpty() && note.contains(subCategory, true)) ||
                (type.isNotEmpty() && note.contains(type, true)) ||
                (status.isNotEmpty() && note.contains(status, true))) {
                return true
            }
        }

        return false
    }

    // ==================== UI Updates ====================

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.isVisible = show
        binding.ordersRecycler.isVisible = !show && orderItems.isNotEmpty()
        binding.emptyState.isVisible = !show && orderItems.isEmpty()
    }

    private fun updateEmptyState() {
        binding.emptyState.isVisible = orderItems.isEmpty() && !binding.loadingContainer.isVisible
        binding.ordersRecycler.isVisible = orderItems.isNotEmpty()
    }

    private fun updateResultsInfo() {
        val count = orderItems.size
        val total = orderItems.sumOf { (it?.total ?: 0L) } / 100.0
        val totalFormatted = String.format("$%.2f", total)
        
        binding.resultsInfo.text = when {
            count == 0 -> "No orders found"
            count == 1 -> "Showing 1 order • Total: $totalFormatted"
            else -> "Showing $count orders • Total: $totalFormatted"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyAdapter() {
        orderAdapter?.notifyDataSetChanged()
    }

    // ==================== Order Click ====================

    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        if (orderPosition >= orderItems.size) return
        
        userSelectedPosition = orderPosition
        userClickedData = orderItems[orderPosition]
        
        val order = orderItems[orderPosition] ?: return
        
        // Navigate to order detail using nav controller
        try {
            val navController = androidx.navigation.Navigation.findNavController(requireView())
            val bundle = Bundle().apply {
                putParcelable("orderData", order)
            }
            navController.navigate(R.id.action_orderListRedesignFragment_to_orderDetailFragment, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== Public Methods ====================

    @SuppressLint("NotifyDataSetChanged")
    fun updateOrder(orderData: Order?) {
        runOnBackgroundThread {
            orderItems.forEachIndexed { index, order ->
                if (order?.id == orderData?.id) {
                    orderItems[index] = orderData
                    runOnMainThread {
                        orderAdapter?.notifyItemChanged(index)
                    }
                    return@runOnBackgroundThread
                }
            }
        }
    }

    fun refreshOrders() {
        loadOrders()
    }
}
