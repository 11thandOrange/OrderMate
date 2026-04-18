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
import com.orderMate.utils.getCustomerContactDetails
import com.orderMate.utils.getThePaymentState
import com.orderMate.utils.hideView
import com.orderMate.utils.onBackPressed
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread

import com.orderMate.utils.showView
import com.orderMate.utils.toDoubleFloatPoint
import com.orderMate.repository.CloverRepository
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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

                // #45: Clover tags in header - show ORDER state and PAYMENT state separately
                // orderPlacedStatusValue shows order.state (OPEN or LOCKED/CLOSED)
                // Dark background with tone-on-tone text
                val orderState = orderArguments?.state?.uppercase() ?: "OPEN"
                val isOrderOpen = orderState == "OPEN"
                binding.orderPlacedStatusValue.text = if (isOrderOpen) "OPEN" else "CLOSED"
                binding.orderPlacedStatusValue.setTextColor(
                    ContextCompat.getColor(requireContext(), 
                        if (isOrderOpen) R.color.open_status_text else R.color.closed_status_text)
                )
                binding.orderPlacedStatusValue.setBackgroundResource(
                    if (isOrderOpen) R.drawable.badge_background_open else R.drawable.badge_background_closed
                )
                
                // paymentStatusBadge shows order.paymentState (PAID, NOT_PAID, PARTIALLY_PAID, etc.)
                val paymentState = orderArguments?.paymentState?.name ?: "NOT_PAID"
                binding.paymentStatusBadge.text = formatPaymentState(paymentState).uppercase()
                updatePaymentBadgeStyle(paymentState)
                
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
                        
                        // #52: Truncate to 10 characters with ellipsis
                        val truncatedName = if (fullName.length > 10) {
                            fullName.take(10) + "…"
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
        setupOrderNotesPills()
        populateDescriptionRow()  // #46
        populateOrderTags()       // #46
    }
    
    /**
     * #46: Populate description row from order notes
     */
    private fun populateDescriptionRow() {
        val orderNote = orderArguments?.note
        val descriptionRow = binding.descriptionRow
        val descriptionDivider = binding.descriptionDivider
        val descriptionValue = binding.orderDescriptionValue
        
        // Extract description text from note (text after "text:" label)
        val description = extractDescriptionFromNote(orderNote)
        
        if (description.isNullOrBlank()) {
            descriptionRow.visibility = View.GONE
            descriptionDivider.visibility = View.GONE
        } else {
            descriptionRow.visibility = View.VISIBLE
            descriptionDivider.visibility = View.VISIBLE
            descriptionValue.text = description
        }
    }
    
    private fun extractDescriptionFromNote(noteString: String?): String? {
        if (noteString.isNullOrBlank()) return null
        
        val delimiter = if (noteString.contains("•")) "•" else "|"
        val parts = noteString.split(delimiter).map { it.trim() }
        
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val value = part.substring(colonIndex + 1).trim()
                if (label == "text" || label == "note" || label == "description") {
                    return value.takeIf { it.isNotBlank() }
                }
            }
        }
        // If no labeled text, return the whole note as description
        return if (!noteString.contains(":")) noteString else null
    }
    
    /**
     * #46: Populate custom order tags in the tags container
     */
    private fun populateOrderTags() {
        val tagsContainer = binding.tagsContainer
        tagsContainer.removeAllViews()
        
        val orderNote = orderArguments?.note
        val tags = extractTagsFromNote(orderNote)
        
        if (tags.isEmpty()) {
            // Add a dash or placeholder
            val placeholder = TextView(requireContext()).apply {
                text = "—"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 14f
            }
            tagsContainer.addView(placeholder)
            return
        }
        
        tags.forEachIndexed { index, tag ->
            val tagView = createTagView(tag.text, tag.type)
            tagsContainer.addView(tagView)
            
            // Add spacing between tags
            if (index < tags.size - 1) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(8.dpToPx(), 1)
                }
                tagsContainer.addView(spacer)
            }
        }
    }
    
    private data class OrderTag(val text: String, val type: String)
    
    private fun extractTagsFromNote(noteString: String?): List<OrderTag> {
        if (noteString.isNullOrBlank()) return emptyList()
        
        val tags = mutableListOf<OrderTag>()
        val delimiter = if (noteString.contains("•")) "•" else "|"
        val parts = noteString.split(delimiter).map { it.trim() }
        
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val value = part.substring(colonIndex + 1).trim()
                
                if (label.contains("tag") || label.contains("category") || label.contains("type")) {
                    // Split multiple values (comma-separated)
                    value.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                        tags.add(OrderTag(it, label))
                    }
                }
            }
        }
        return tags
    }
    
    private fun createTagView(text: String, type: String): TextView {
        val (bgColor, textColor) = when {
            type.contains("category") -> Pair(R.color.tag_category_bg, R.color.tag_category_text)
            type.contains("type") -> Pair(R.color.tag_type_bg, R.color.tag_type_text)
            type.contains("status") -> Pair(R.color.tag_status_bg, R.color.tag_status_text)
            else -> Pair(R.color.tag_default_bg, R.color.tag_default_text)
        }
        
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), textColor))
            setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
            
            val bg = android.graphics.drawable.GradientDrawable()
            bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            bg.cornerRadius = 12f * resources.displayMetrics.density
            bg.setColor(ContextCompat.getColor(requireContext(), bgColor))
            background = bg
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    
    /**
     * Setup order-level notes pills display (#93)
     */
    private fun setupOrderNotesPills() {
        val orderNote = orderArguments?.note
        val container = binding.orderNotesPillsContainer
        val section = binding.orderNotesSection
        
        container.removeAllViews()
        
        // Always show section so user can add notes
        section.visibility = View.VISIBLE
        
        if (orderNote.isNullOrBlank()) {
            // Show placeholder text when no notes
            val placeholder = TextView(requireContext()).apply {
                text = "No order notes"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 13f
            }
            container.addView(placeholder)
            return
        }
        
        // Parse and display notes
        val notes = parseOrderNote(orderNote)
        notes.forEach { noteItem ->
            val pillView = layoutInflater.inflate(R.layout.item_note_pill, container, false) as LinearLayout
            
            val pillIcon = pillView.findViewById<ImageView>(R.id.pillIcon)
            val pillText = pillView.findViewById<TextView>(R.id.pillText)
            
            pillText.text = noteItem.text
            pillText.maxLines = 1
            pillText.setTextColor(ContextCompat.getColor(requireContext(), R.color.order_pill_text))
            
            val iconRes = getIconForLabel(noteItem.label)
            pillIcon.setImageResource(iconRes)
            pillIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.order_pill_icon))
            
            // Purple background for order-level notes
            val bg = android.graphics.drawable.GradientDrawable()
            bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            bg.cornerRadius = 10f * resources.displayMetrics.density
            bg.setColor(ContextCompat.getColor(requireContext(), R.color.order_pill_bg))
            bg.setStroke(
                (1 * resources.displayMetrics.density).toInt(),
                ContextCompat.getColor(requireContext(), R.color.order_pill_border)
            )
            pillView.background = bg
            
            container.addView(pillView)
        }
    }
    
    private data class NoteItem(val text: String, val label: String)
    
    private fun parseOrderNote(noteString: String): List<NoteItem> {
        val notes = mutableListOf<NoteItem>()
        val delimiter = if (noteString.contains("•")) "•" else "|"
        val parts = noteString.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
        
        parts.forEach { part ->
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val label = part.substring(0, colonIndex).trim().lowercase()
                val rawValue = part.substring(colonIndex + 1).trim()
                
                val isMultiSelect = label.contains("category") || label.contains("tag")
                if (isMultiSelect) {
                    rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { value ->
                        notes.add(NoteItem(value, label))
                    }
                } else if (rawValue.isNotBlank()) {
                    notes.add(NoteItem(rawValue, label))
                }
            } else if (part.isNotBlank()) {
                notes.add(NoteItem(part, ""))
            }
        }
        return notes
    }
    
    private fun getIconForLabel(label: String): Int {
        return when {
            label.contains("date") || label.contains("pickup") -> R.drawable.ic_calendar
            label.contains("type") || label.contains("status") -> R.drawable.ic_check_box
            label.contains("category") || label.contains("tag") -> R.drawable.ic_label
            else -> R.drawable.ic_edit
        }
    }
    
    private fun populateHistoryCard() {
        val historyContainer = binding.historyContainer
        historyContainer.removeAllViews()
        
        data class HistoryItem(
            val title: String,
            val timestamp: Long,
            val iconRes: Int
        )
        
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
                                        findNavController().navigateUp()
                                    }
                                } catch (e: Exception) {
                                    // Navigation failed - fragment may be destroyed, ignore
                                }
                            }
                            // Update the dashboard after the order is deleted
                            Handler(Looper.getMainLooper()).postDelayed({
                                exceptionHandler {
                                    OrderHistoryFragment.getInstance().getTheOrderData(true)
                                }
                            }, 1000)
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
            orderDetailsCard.setOnClickListener {
                openOrderNoteDialog()
            }
            
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
            
            // Add/Edit Order Note button (#93 requirement)
            btnAddOrderNote.setOnClickListener {
                openOrderNoteDialog()
            }
        }
    }
    
    /**
     * Open order note dialog for editing order-level notes (#93)
     */
    private fun openOrderNoteDialog() {
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
        // Get line item data
        val lineItemGroup = lineItems.getOrNull(orderPosition)
        val lineItem = lineItemGroup?.order
        val existingNote = lineItem?.note
        val itemName = lineItem?.name ?: lineItem?.item?.name
        val itemQuantity = lineItemGroup?.count ?: lineItem?.unitQty?.toInt() ?: 1
        
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
                    // Update the line item note in UI
                    updateNoteInTheLineItemOfOrder(itemId, note, orderPosition)
                    
                    // Save to Clover via OrderConnector
                    runOnBackgroundThread {
                        exceptionHandler {
                            val orderId = orderArguments?.id ?: return@exceptionHandler
                            val allLineItems = orderArguments?.lineItems ?: return@exceptionHandler
                            
                            // Update note for matching line items
                            allLineItems.forEach { lineItem ->
                                if (lineItem?.item?.id == itemId) {
                                    lineItem.note = note
                                }
                            }
                            
                            // Save to Clover
                            myApp.getOrderConnector().updateLineItems(orderId, allLineItems)
                        }
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
    
    /**
     * #45: Update payment badge style based on payment state
     */
    private fun updatePaymentBadgeStyle(paymentState: String?) {
        // Dark background with tone-on-tone text
        val (textColorRes, bgRes) = when (paymentState?.uppercase()) {
            "PAID" -> Pair(R.color.paid_status_text, R.drawable.badge_background_paid)
            "NOT_PAID" -> Pair(R.color.unpaid_status_text, R.drawable.badge_background_unpaid)
            "PARTIALLY_PAID" -> Pair(R.color.partial_status_text, R.drawable.badge_background_partial)
            "PARTIALLY_REFUNDED" -> Pair(R.color.partial_status_text, R.drawable.badge_background_partial)
            "REFUNDED" -> Pair(R.color.closed_status_text, R.drawable.badge_background_closed)
            else -> Pair(R.color.open_status_text, R.drawable.badge_background_open)
        }
        binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes))
        binding.paymentStatusBadge.setBackgroundResource(bgRes)
    }
}
