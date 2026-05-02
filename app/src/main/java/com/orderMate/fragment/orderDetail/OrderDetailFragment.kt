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
import androidx.recyclerview.widget.RecyclerView
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
import com.orderMate.utils.formatPaymentState
import com.orderMate.utils.formatOrderState
import com.orderMate.utils.getFormattedPaymentState
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.getThePaymentState
import com.orderMate.utils.hideView
import com.orderMate.utils.onBackPressed
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread

import com.orderMate.utils.showView
import com.orderMate.utils.toDoubleFloatPoint
import com.orderMate.repository.CloverRepository
import com.orderMate.utils.WidgetManager
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetType
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.orderMate.viewmodel.SharedFilterViewModel
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
        @Volatile
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
    private val sharedFilterViewModel: SharedFilterViewModel by activityViewModels()

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
        setupOrderDetailsCardConstraints()
        runOnBackgroundThread {
            appConnector = AppsConnector(requireContext(), myApp.getCloverAccount())
        }
    }
    
    /**
     * Set up Order Details scroll indicator
     * ConstraintLayout handles card positioning - Order Details fills remaining space
     */
    private fun setupOrderDetailsCardConstraints() {
        val scrollView = binding.orderDetailsScrollView
        val scrollIndicator = binding.orderDetailsScrollIndicator
        
        // Wait for layout to be measured
        scrollView.post {
            // Check if content is scrollable
            val canScroll = scrollView.canScrollVertically(1)
            scrollIndicator.visibility = if (canScroll) View.VISIBLE else View.GONE
        }
        
        // Set up scroll listener to show/hide indicator
        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            val canScrollMore = scrollView.canScrollVertically(1)
            scrollIndicator.visibility = if (canScrollMore) View.VISIBLE else View.GONE
        }
    }
    
    /**
     * Update order details scroll indicator visibility after content changes
     * Called after populateOrderTags() and populateDynamicWidgetRows() to recheck scrollability
     */
    private fun updateOrderDetailsScrollIndicator() {
        binding.orderDetailsScrollView.post {
            val canScroll = binding.orderDetailsScrollView.canScrollVertically(1)
            binding.orderDetailsScrollIndicator.visibility = if (canScroll) View.VISIBLE else View.GONE
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

                // #45: Clover tags in header - using unified pill styling (15% opacity + border)
                val density = resources.displayMetrics.density
                
                // Order Status badge (Red)
                val orderState = orderArguments?.state
                binding.orderPlacedStatusValue.text = formatOrderState(orderState)
                binding.orderPlacedStatusValue.background = com.orderMate.utils.WidgetColorUtils.createPillBackground(
                    com.orderMate.utils.WidgetColorUtils.COLOR_ORDER_STATUS, 20f, density
                )
                binding.orderPlacedStatusValue.setTextColor(com.orderMate.utils.WidgetColorUtils.COLOR_ORDER_STATUS)
                
                // Payment Status badge (Yellow) - using shared function
                binding.paymentStatusBadge.text = getFormattedPaymentState(orderArguments)
                binding.paymentStatusBadge.background = com.orderMate.utils.WidgetColorUtils.createPillBackground(
                    com.orderMate.utils.WidgetColorUtils.COLOR_PAYMENT_STATUS, 20f, density
                )
                binding.paymentStatusBadge.setTextColor(com.orderMate.utils.WidgetColorUtils.COLOR_PAYMENT_STATUS)
                
                // Payment Type badge - get from order payments
                populatePaymentTypeBadge()
                
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
                        val customer = customers[0]
                        val fullName = getString(
                            R.string.getFullName,
                            customer?.firstName ?: "",
                            customer?.lastName ?: ""
                        ).trim()
                        
                        // #52: Truncate to 25 characters with ellipsis
                        val truncatedName = if (fullName.length > 25) {
                            fullName.take(25) + "…"
                        } else {
                            fullName
                        }
                        binding.orderPlacedCustomerValue.text = truncatedName
                        
                        // #53: Update customer avatar with initials or "?"
                        val initials = buildString {
                            customer?.firstName?.firstOrNull()?.let { append(it.uppercaseChar()) }
                            customer?.lastName?.firstOrNull()?.let { append(it.uppercaseChar()) }
                        }.ifEmpty { "?" }
                        binding.customerAvatar.text = initials
                        
                        val result = getCustomerContactDetails(customer)
                        binding.orderPlacedCustomerNumberValue.text = result.first
                        binding.orderPlacedCustomerEmailValue.text = result.second
                    } else {
                        // #53: No customer - show "?" in avatar
                        binding.orderPlacedCustomerValue.text = getString(R.string.dash)
                        binding.customerAvatar.text = "?"
                    }
                }


                exceptionHandler({
                        val employeeName = employee?.jsonObject?.get(Constants.name)?.toString()
                        binding.orderPlacedEmployeeValue.text = employeeName
                        binding.employeeValue.text = employeeName ?: getString(R.string.dash)
                })
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        val value = MyApp.getInstance().getEmployeeName(employee?.id)
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.orderPlacedEmployeeValue.text = value
                                ?: getString(R.string.dash)
                            binding.employeeValue.text = value
                                ?: getString(R.string.dash)
                        }
                    }
                }
                // Render screen immediately, then reload widgets and refresh pills
                setUpTheScreen()
                
                // Load widgets from Firebase and refresh all widget-dependent UI
                WidgetManager.getInstance(requireContext()).reloadAll { success ->
                    if (success) {
                        activity?.runOnUiThread {
                            // Refresh line item pills
                            populateItemTags()
                            // Refresh order-level tags and dynamic rows
                            populateOrderTags()
                            populateDynamicWidgetRows()
                        }
                    }
                }
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
            populateItemTags()
            
            // #61: Setup scroll indicator for item list
            setupScrollIndicator()

            transactionRecycler.layoutManager = LinearLayoutManager(requireContext())
            val transactionAdapter = TransactionAdapter(paymentItems)
            transactionRecycler.adapter = transactionAdapter

            refundRecycler.layoutManager = LinearLayoutManager(requireContext())
            val refundAdapter = RefundAdapter(refundItems)
            refundRecycler.adapter = refundAdapter
        }
    }
    
    /**
     * Populate item tags by re-creating the ItemAdapter.
     * Call this after widgets are loaded to render pills.
     */
    private fun populateItemTags() {
        binding.itemRecycler.adapter = ItemAdapter(lineItems, this@OrderDetailFragment)
    }
    
    /**
     * #61: Setup scroll indicator (carrot) that shows when more items are below
     */
    private fun setupScrollIndicator() {
        binding.apply {
            // Add scroll listener to show/hide indicator
            itemRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateScrollIndicatorVisibility()
                }
            })
            
            // Initial check after layout
            itemRecycler.post {
                updateScrollIndicatorVisibility()
            }
        }
    }
    
    /**
     * #61: Update scroll indicator visibility based on scroll state
     */
    private fun updateScrollIndicatorVisibility() {
        binding.apply {
            // Show indicator if can scroll down (more content below)
            val canScrollDown = itemRecycler.canScrollVertically(1)
            scrollIndicator.visibility = if (canScrollDown) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()


        // means don't non-required hit the API
        if (isPaymentBtnClicked) {
            CoroutineScope(Dispatchers.IO).launch {
                updateTheTransaction()
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
        populateHistoryCard()
        populateOrderTags()           // SINGLE_SELECT + MULTI_SELECT widgets
        populateDynamicWidgetRows()   // CALENDAR + TEXT_BOX widgets
        updateOrderDetailsScrollIndicator() // Update scroll indicator after content changes
    }
    
    /**
     * #46: Populate custom order tags in the tags container
     * Uses enabled SINGLE_SELECT and MULTI_SELECT widgets from order level
     * Styled like pills on order details list row using WidgetColorUtils
     * Hides entire section (including divider) when no tags exist
     */
    private fun populateOrderTags() {
        val tagsContainer = binding.tagsContainer
        val tagsSection = binding.tagsSection
        tagsContainer.removeAllViews()
        
        android.util.Log.d("OrderPillDebug", "========== ORDER PILL DEBUG ==========")
        
        val orderNote = orderArguments?.note
        val widgetManager = context?.let { WidgetManager.getInstance(it) }
        
        android.util.Log.d("OrderPillDebug", "orderNote: '$orderNote'")
        
        // Get cached widgets only for pill rendering (never defaults)
        val selectWidgets = widgetManager?.getCachedOrderWidgets()
            ?.filter { it.type == WidgetType.SINGLE_SELECT || it.type == WidgetType.MULTI_SELECT }
            ?: emptyList()
        
        android.util.Log.d("OrderPillDebug", "selectWidgets count: ${selectWidgets.size}")
        selectWidgets.forEach { widget ->
            android.util.Log.d("OrderPillDebug", "  Widget: id=${widget.id}, label=${widget.label}, type=${widget.type}, level=${widget.level}, enabled=${widget.isEnabled}")
        }
        
        // Parse and get tags from order note
        val tags = if (orderNote.isNullOrBlank() || selectWidgets.isEmpty()) {
            emptyList()
        } else {
            com.orderMate.utils.OrderNoteParser.extractTagsFromNote(
                orderNote, selectWidgets, NoteLevel.ORDER
            ).filter { it.type != com.orderMate.utils.OrderNoteParser.TagType.CALENDAR }
        }
        
        android.util.Log.d("OrderPillDebug", "tags count: ${tags.size}")
        tags.forEach { tag ->
            android.util.Log.d("OrderPillDebug", "  Tag: label=${tag.label}, value=${tag.value}, type=${tag.type}")
        }
        android.util.Log.d("OrderPillDebug", "======================================")
        
        if (tags.isEmpty()) {
            // Hide entire section (divider + row) when no tags
            tagsSection.visibility = View.GONE
            return
        }
        
        // Show section when there are tags
        tagsSection.visibility = View.VISIBLE
        
        tags.forEach { tag ->
            // Use shared pill utility for consistent styling
            com.orderMate.utils.WidgetColorUtils.addPillToContainer(
                requireContext(), tagsContainer, tag.value, tag.widgetType
            )
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    
    /**
     * Task 21: Populate dynamic widget rows in order details card
     * - CALENDAR type widgets render as their own rows (e.g., "Due Date: 12/1")
     * - TEXT_BOX type widgets render as their own rows (e.g., "Description: Custom cake")
     * - SINGLE_SELECT and MULTI_SELECT go into Order Tags row (handled by populateOrderTags)
     */
    private fun populateDynamicWidgetRows() {
        val container = binding.dynamicWidgetRowsContainer
        container.removeAllViews()
        
        val orderNote = orderArguments?.note
        val widgetManager = context?.let { WidgetManager.getInstance(it) } ?: return
        
        // Get cached widgets only for pill rendering (never defaults)
        val dynamicWidgets = widgetManager.getCachedOrderWidgets()
            .filter { it.type == WidgetType.CALENDAR || it.type == WidgetType.TEXT_BOX }
        
        if (orderNote.isNullOrBlank() || dynamicWidgets.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        // Parse order note to find widget values
        val parsedValues = com.orderMate.utils.OrderNoteParser.parseNotesByWidgetType(
            orderNote, dynamicWidgets, NoteLevel.ORDER
        )
        
        if (parsedValues.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        
        parsedValues.entries.forEachIndexed { index, (widget, value) ->
            // Add divider before each row (acts as separator from previous row)
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (1 * density).toInt()
                )
                setBackgroundColor(0x0DFFFFFF)
            }
            container.addView(divider)
            
            // Create row layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
            }
            
            // Label (widget label)
            val labelView = TextView(requireContext()).apply {
                text = widget.label
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            // Value - for TEXT_BOX allow wrapping, for CALENDAR show inline
            val valueView = TextView(requireContext()).apply {
                text = value
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light))
                textSize = 14f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                
                if (widget.type == WidgetType.TEXT_BOX) {
                    // TEXT_BOX: allow wrapping, max 2 lines
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).apply {
                        gravity = android.view.Gravity.END
                    }
                    gravity = android.view.Gravity.END
                }
            }
            
            rowLayout.addView(labelView)
            rowLayout.addView(valueView)
            container.addView(rowLayout)
        }
    }
    
    /**
     * History item data class for order history card
     * messageBody is optional - only set for notification items (#58)
     */
    data class HistoryItem(
        val title: String,
        val timestamp: Long,
        val iconRes: Int,
        val messageBody: String? = null
    )
    
    private fun populateHistoryCard() {
        val historyContainer = binding.historyContainer
        historyContainer.removeAllViews()
        
        val historyItems = mutableListOf<HistoryItem>()
        
        // Add order created event
        orderArguments?.createdTime?.let { createdTime ->
            historyItems.add(
                HistoryItem(
                    title = getString(R.string.order_created),
                    timestamp = createdTime,
                    iconRes = R.drawable.ic_add_circle
                )
            )
        }
        
        // Add order modified event (if different from created)
        orderArguments?.modifiedTime?.let { modifiedTime ->
            if (modifiedTime != orderArguments?.createdTime) {
                historyItems.add(
                    HistoryItem(
                        title = getString(R.string.order_modified),
                        timestamp = modifiedTime,
                        iconRes = R.drawable.ic_edit
                    )
                )
            }
        }
        
        // Add payment events
        orderArguments?.payments?.forEach { payment ->
            payment?.createdTime?.let { timestamp ->
                val amount = payment.amount?.let { amt ->
                    "${currencyName.convertToSymbol()}${(amt / 100.0).toDoubleFloatPoint()}"
                } ?: ""
                historyItems.add(
                    HistoryItem(
                        title = "${getString(R.string.payment_received)} $amount",
                        timestamp = timestamp,
                        iconRes = R.drawable.ic_credit_card
                    )
                )
            }
        }
        
        // Display initial items while loading notifications
        displayHistoryItems(historyItems)
        
        // Fetch notification history from Bird API (#54)
        val orderId = orderArguments?.id
        if (orderId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val notifications = CloverRepository.getInstance(requireContext())
                        .getNotificationsForOrder(orderId)
                    
                    // Add notification items
                    notifications.forEach { message ->
                        val timestamp = parseIsoTimestamp(message.createdAt)
                        val messageText = message.body?.text?.text 
                            ?: message.body?.html?.text 
                            ?: getString(R.string.notification_sent)
                        
                        // Determine icon based on message type (email vs SMS)
                        val notificationType = message.meta?.extraInformation?.get("type")
                        val iconRes = if (notificationType == "email") {
                            R.drawable.ic_email
                        } else {
                            R.drawable.ic_send
                        }
                        
                        // Truncate title but keep full message for dialog (#58)
                        val truncatedTitle = if (messageText.length > 40) {
                            "${messageText.take(40)}..."
                        } else {
                            messageText
                        }
                        
                        historyItems.add(
                            HistoryItem(
                                title = truncatedTitle,
                                timestamp = timestamp,
                                iconRes = iconRes,
                                messageBody = messageText
                            )
                        )
                    }
                    
                    // Re-display with notifications included
                    CoroutineScope(Dispatchers.Main).launch {
                        displayHistoryItems(historyItems)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Keep showing items without notifications on error
                }
            }
        }
    }
    
    /**
     * Parse ISO 8601 timestamp string to milliseconds
     */
    private fun parseIsoTimestamp(isoTimestamp: String?): Long {
        if (isoTimestamp == null) return 0L
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoTimestamp.substringBefore(".").substringBefore("Z"))?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Display history items in the card
     */
    private fun displayHistoryItems(historyItems: MutableList<HistoryItem>) {
        val historyContainer = binding.historyContainer
        historyContainer.removeAllViews()
        
        // Sort by timestamp descending (most recent first)
        historyItems.sortByDescending { it.timestamp }
        
        // Limit to 3 items for the card preview
        val displayItems = historyItems.take(3)
        
        displayItems.forEachIndexed { index, item ->
            // Add separator before items (except first)
            if (index > 0) {
                val separator = View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (1 * resources.displayMetrics.density).toInt()
                    )
                    setBackgroundColor(0x1AFFFFFF)
                }
                historyContainer.addView(separator)
            }
            
            // Create history item view
            val itemView = layoutInflater.inflate(R.layout.item_order_history, historyContainer, false)
            
            // Set icon
            itemView.findViewById<android.widget.ImageView>(R.id.historyIcon)?.apply {
                setImageResource(item.iconRes)
            }
            
            // Set title
            itemView.findViewById<android.widget.TextView>(R.id.historyTitle)?.apply {
                text = item.title
            }
            
            // Set timestamp
            itemView.findViewById<android.widget.TextView>(R.id.historyDate)?.apply {
                text = item.timestamp.formatMillisToDateTime(Constants.yearFormat)
            }
            
            historyContainer.addView(itemView)
        }
        
        // Show/hide scroll indicator based on total items
        binding.historyScrollIndicator.isVisible = historyItems.size > 3
        binding.historyFadeOverlay.isVisible = historyItems.size > 3
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
            binding.transactionRecycler.showView()
            binding.noTransactionText.hideView()
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


    /**
     * #78: Display order amounts using Clover OrderCalc
     * - Subtotal: line items before discounts
     * - Tax: tax + service charge + fees (combined as "Taxes & Fees")
     * - Discount: total discount amount
     */
    private fun showTheDiscountAndTaxData() {
        // Hide discount row if no discounts applied
        if (orderArguments?.discounts == null || orderArguments?.discounts?.isEmpty() == true) {
            binding.discount.hideView()
            binding.discountValue.hideView()
        }

        // #78: Get all amounts from Clover OrderCalc
        val tax = myApp.orderTax(orderArguments)
        val serviceCharge = myApp.orderServiceCharge(orderArguments)
        val fees = myApp.orderFees(orderArguments)
        val taxesAndFees = tax + serviceCharge + fees
        
        val subtotal = myApp.orderLineItemTotal(orderArguments)
        val discount = myApp.orderDiscount(orderArguments)

        // Display taxes & fees (tax + service charge + order fees)
        "${currencyName.convertToSymbol()}${taxesAndFees.toDoubleFloatPoint()}".also {
            binding.taxValue.text = it
        }
        
        // Display subtotal (before discounts)
        "${currencyName.convertToSymbol()}${subtotal.toDoubleFloatPoint()}".also {
            binding.subtotalValue.text = it
        }
        
        // Display discount amount
        "${currencyName.convertToSymbol()}${discount.toDoubleFloatPoint()}".also {
            binding.discountValue.text = it
        }
        
        // Display discount name if available
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

    suspend fun updateTheTransaction(){
        exceptionHandler {
            // update the dashboard after the order is delay
            val orderData = myApp.getOrderConnector().getOrder(orderArguments?.id)
            orderArguments = orderData

        }
        CoroutineScope(Dispatchers.Main).launch {
            showThePaymentData()
        }
        refreshUI(true)
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
                try{
                    val dror = Intent(Intents.ACTION_CLOVER_PAY)
                    dror.putExtra(EXTRA_ORDER_ID, orderArguments?.id)
                    startActivity(dror)
                    isPaymentBtnClicked = true
                }catch (e:Exception){
                    e.printStackTrace()
                }

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
                                // #44: Safe navigation - verify fragment is still attached before navigating
                                try {
                                    if (isAdded && activity != null && view != null) {
                                        // Signal the list fragment to refresh via ViewModel
                                        sharedFilterViewModel.triggerRefresh()
                                        findNavController().navigateUp()
                                    }
                                } catch (e: Exception) {
                                    // Navigation failed - fragment may be destroyed, ignore
                                }
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
            
            // Customer row click - opens customer dialog
            customerRow.setOnClickListener {
                val customers = orderArguments?.customers
                val customer = if (customers?.isNotEmpty() == true) customers[0] else null
                CustomerDialog.newInstance(
                    customer = customer,
                    orderId = orderArguments?.id,
                    onCustomerUpdated = { _ ->
                        // Refresh order data when customer is edited
                        CoroutineScope(Dispatchers.IO).launch {
                            refreshUI()
                        }
                    }
                ).show(parentFragmentManager, CustomerDialog.TAG)
            }
            
            // Order Details Card click - opens order-level notes popup
            // Set click listener on the entire card to ensure full clickability
            orderDetailsCard.setOnClickListener { openOrderNoteDialog() }
            
            // Use GestureDetector to detect taps while allowing scroll
            val gestureDetector = android.view.GestureDetector(requireContext(), 
                object : android.view.GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: android.view.MotionEvent): Boolean {
                        openOrderNoteDialog()
                        return true
                    }
                })
            orderDetailsScrollView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false // Return false to allow scroll to continue
            }
            // Also handle clicks on header (which is outside ScrollView)
            orderDetailsHeader.setOnClickListener { openOrderNoteDialog() }
            
            // Order History Card click - opens order history dialog
            orderHistoryCard.setOnClickListener {
                orderArguments?.let { order ->
                    OrderHistoryDialog.newInstance(order)
                        .show(parentFragmentManager, OrderHistoryDialog.TAG)
                }
            }
            
            // View All History button (hidden but kept for compatibility)
            viewAllHistoryButton.setOnClickListener {
                orderArguments?.let { order ->
                    OrderHistoryDialog.newInstance(order)
                        .show(parentFragmentManager, OrderHistoryDialog.TAG)
                }
            }
            
            // Note: Edit button removed - card click handles editing via orderDetailsCard.setOnClickListener
        }
    }
    
    /**
     * Open order note dialog for editing order-level notes (#93)
     */
    private fun openOrderNoteDialog() {
        // Check if order-level notes are enabled in settings
        val widgetManager = context?.let { WidgetManager.getInstance(it) }
        if (widgetManager?.isOrderNotesEnabled() != true) {
            return // Order notes disabled, don't show popup
        }
        
        val existingNote = orderArguments?.note
        OrderNoteDialogFragment.newInstance(
            orderId = orderArguments?.id,
            existingNote = existingNote
        ).apply {
            setListener(object : OrderNoteDialogFragment.OrderNoteListener {
                override fun onOrderNoteSaved(orderId: String?, note: String) {
                    if (orderId != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val success = CloverRepository.getInstance(requireContext())
                                .saveOrderNote(orderId, note)
                            if (success) {
                                refreshUI()
                            }
                        }
                    }
                }
                
                override fun onOrderNoteCancelled() {
                    // No action needed
                }
            })
        }.show(parentFragmentManager, OrderNoteDialogFragment.TAG)
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


    // When user clicks on an item row, open the OrderMate popup to add/edit notes
    override fun onOrderItemClick(orderPosition: Int, lineItemId: String?) {
        // Check if item-level notes are enabled in settings
        val widgetManager = context?.let { WidgetManager.getInstance(it) }
        if (widgetManager?.isItemNotesEnabled() != true) {
            return // Item notes disabled, don't show popup
        }
        
        // Get line item data
        val lineItemGroup = lineItems.getOrNull(orderPosition)
        val lineItem = lineItemGroup?.order
        val existingNote = lineItem?.note
        val itemName = lineItem?.getName()
        val itemQuantity = lineItemGroup?.itemCount ?: lineItem?.unitQty?.toInt() ?: 1
        
        // Build modifiers string from modifications
        val modifiersString = lineItem?.modifications?.mapNotNull { it?.name }
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
        
        // Dialog reads widgets from WidgetManager directly (like production)
        ItemNoteDialogFragment.newInstance(
            lineItemId = lineItemId,
            existingNote = existingNote,
            itemName = itemName,
            itemModifiers = modifiersString,
            itemQuantity = itemQuantity
        ).apply {
            setListener(object : ItemNoteDialogFragment.ItemNoteListener {
                override fun onNoteSaved(itemId: String?, note: String) {
                    try {
                        android.util.Log.d("ItemNoteReceivedDebug", "========== NOTE RECEIVED FROM DIALOG ==========")
                        android.util.Log.d("ItemNoteReceivedDebug", "itemId: $itemId")
                        android.util.Log.d("ItemNoteReceivedDebug", "note received: '$note'")
                        android.util.Log.d("ItemNoteReceivedDebug", "orderPosition: $orderPosition")
                        android.util.Log.d("ItemNoteReceivedDebug", "================================================")
                        
                        // Update the line item note in UI
                        updateNoteInTheLineItemOfOrder(itemId, note, orderPosition)
                        
                        // Save to Clover via OrderConnector
                        runOnBackgroundThread {
                            exceptionHandler {
                                val orderId = orderArguments?.id ?: return@exceptionHandler
                                val allLineItems = orderArguments?.lineItems ?: return@exceptionHandler
                                
                                android.util.Log.d("ItemNoteReceivedDebug", "Saving to Clover - orderId: $orderId")
                                
                                // Update note for matching line items
                                allLineItems.forEach { lineItem ->
                                    if (lineItem?.item?.id == itemId) {
                                        android.util.Log.d("ItemNoteReceivedDebug", "Setting note on lineItem: ${lineItem.id}")
                                        lineItem.note = note
                                    }
                                }
                                
                                // Save to Clover
                                myApp.getOrderConnector().updateLineItems(orderId, allLineItems)
                                android.util.Log.d("ItemNoteReceivedDebug", "Saved to Clover!")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ItemNoteReceivedDebug", "CRASH in onNoteSaved: ${e.message}", e)
                    }
                }
                
                override fun onNoteCancelled() {
                    // Do nothing
                }
            })
        }.show(parentFragmentManager, ItemNoteDialogFragment.TAG)
    }


    /*
    * When user update the note for the item the callback come here
    * */
    override fun updateLineItem(id: String?, list: String?, position: Int) {
        android.util.Log.d("ItemNoteUpdateDebug", "updateLineItem called - id: $id, list: '$list'")
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

            // update all item quantity if the order so that
            // each quantity of line item has same note
            for (i in orderArguments?.lineItems ?: emptyList()) {
                if (i?.item?.id == id) {
                    i?.note = list
                }
            }
        }
        runOnMainThread {
            // Signal refresh when navigating back (must be on main thread for LiveData)
            sharedFilterViewModel.triggerRefresh()
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
    
    /**
     * Populate Payment Type badge from order payments
     * Shows the tender type used (CASH, CREDIT_CARD, etc.)
     */
    private fun populatePaymentTypeBadge() {
        val payments = orderArguments?.payments
        if (payments.isNullOrEmpty()) {
            binding.paymentTypeBadge.visibility = View.GONE
            return
        }
        
        // Get the first payment's tender type
        val firstPayment = payments.firstOrNull()
        val tenderType = firstPayment?.tender?.label 
            ?: firstPayment?.tender?.labelKey
            ?: return
        
        val density = resources.displayMetrics.density
        binding.paymentTypeBadge.text = tenderType.uppercase()
        binding.paymentTypeBadge.background = com.orderMate.utils.WidgetColorUtils.createPillBackground(
            com.orderMate.utils.WidgetColorUtils.COLOR_PAYMENT_TYPE, 20f, density
        )
        binding.paymentTypeBadge.setTextColor(com.orderMate.utils.WidgetColorUtils.COLOR_PAYMENT_TYPE)
        binding.paymentTypeBadge.visibility = View.VISIBLE
    }
}
