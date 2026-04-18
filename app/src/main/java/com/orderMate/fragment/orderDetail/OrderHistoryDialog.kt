package com.orderMate.fragment.orderDetail

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import com.orderMate.databinding.DialogOrderHistoryBinding
import com.orderMate.databinding.ItemOrderHistoryDialogBinding
import com.orderMate.repository.CloverRepository
import com.orderMate.utils.hideView
import com.orderMate.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Order History Dialog - displays all history events for an order
 * Styled like the filter modal with glass background
 * #54/#58: Includes sent notifications with full message content
 */
class OrderHistoryDialog : DialogFragment() {

    private var _binding: DialogOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private var order: Order? = null
    private val historyItems = mutableListOf<HistoryItem>()

    /**
     * HistoryItem with optional messageBody for notification full text (#58)
     */
    data class HistoryItem(
        val title: String,
        val timestamp: Long,
        val iconRes: Int = R.drawable.ic_history,
        val messageBody: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDialog()
        setupClickListeners()
        loadHistoryData()
        setupRecyclerView()
    }

    private fun setupDialog() {
        dialog?.apply {
            setCanceledOnTouchOutside(true)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                setDimAmount(0.6f)
            }
        }
    }

    private fun setupClickListeners() {
        // Dialog dismisses on outside click
    }

    private fun loadHistoryData() {
        historyItems.clear()

        order?.let { o ->
            // Add created at event
            o.createdTime?.let { timestamp ->
                historyItems.add(
                    HistoryItem(
                        title = getString(R.string.order_created),
                        timestamp = timestamp,
                        iconRes = R.drawable.ic_add
                    )
                )
            }

            // Add modified time if different from created
            o.modifiedTime?.let { modTime ->
                if (o.createdTime == null || modTime != o.createdTime) {
                    historyItems.add(
                        HistoryItem(
                            title = getString(R.string.order_modified),
                            timestamp = modTime,
                            iconRes = R.drawable.ic_edit
                        )
                    )
                }
            }

            // Add payment events
            o.payments?.forEach { payment ->
                payment?.createdTime?.let { timestamp ->
                    val amount = payment.amount?.let { amt ->
                        String.format(Locale.US, "$%.2f", amt / 100.0)
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

            // Sort by timestamp descending (newest first)
            historyItems.sortByDescending { it.timestamp }
            
            // Update UI with initial items
            updateHistoryUI()
            
            // Fetch notification history from Bird API (#54)
            val orderId = o.id
            if (orderId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val notifications = CloverRepository.getInstance(requireContext())
                            .getNotificationsForOrder(orderId)
                        
                        // Add notification items with full message body (#58)
                        notifications.forEach { message ->
                            val timestamp = parseIsoTimestamp(message.createdAt)
                            val messageText = message.body?.text?.text 
                                ?: message.body?.html?.text 
                                ?: getString(R.string.notification_sent)
                            
                            // Determine icon based on message type
                            val notificationType = message.meta?.extraInformation?.get("type")
                            val iconRes = if (notificationType == "email") {
                                R.drawable.ic_email
                            } else {
                                R.drawable.ic_send
                            }
                            
                            historyItems.add(
                                HistoryItem(
                                    title = getString(R.string.notification_sent),
                                    timestamp = timestamp,
                                    iconRes = iconRes,
                                    messageBody = messageText  // Full message for #58
                                )
                            )
                        }
                        
                        // Re-sort and update UI
                        historyItems.sortByDescending { it.timestamp }
                        
                        CoroutineScope(Dispatchers.Main).launch {
                            updateHistoryUI()
                            binding.historyRecyclerView.adapter?.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Update empty state visibility
        updateHistoryUI()
    }
    
    /**
     * Parse ISO 8601 timestamp string to milliseconds
     */
    private fun parseIsoTimestamp(isoTimestamp: String?): Long {
        if (isoTimestamp == null) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoTimestamp.substringBefore(".").substringBefore("Z"))?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Update the history UI based on current items
     */
    private fun updateHistoryUI() {
        if (historyItems.isEmpty()) {
            binding.emptyState.showView()
            binding.historyRecyclerView.hideView()
        } else {
            binding.emptyState.hideView()
            binding.historyRecyclerView.showView()
        }
    }

    private fun setupRecyclerView() {
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(historyItems)
        }
    }

    fun setOrder(order: Order) {
        this.order = order
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Inner adapter class - uses dialog layout with message body support (#58)
    inner class HistoryAdapter(
        private val items: List<HistoryItem>
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US)

        inner class HistoryViewHolder(val binding: ItemOrderHistoryDialogBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = ItemOrderHistoryDialogBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return HistoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = items[position]
            holder.binding.apply {
                historyTitle.text = item.title
                historyDate.text = dateFormat.format(Date(item.timestamp))
                historyIcon.setImageResource(item.iconRes)
                
                // Show full message body for notification items (#58)
                if (item.messageBody != null) {
                    historyMessageBody.text = item.messageBody
                    historyMessageBody.visibility = View.VISIBLE
                } else {
                    historyMessageBody.visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "OrderHistoryDialog"

        fun newInstance(order: Order): OrderHistoryDialog {
            return OrderHistoryDialog().apply {
                setOrder(order)
            }
        }
    }
}
