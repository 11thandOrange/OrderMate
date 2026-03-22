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
import com.orderMate.databinding.ItemOrderHistoryBinding
import com.orderMate.utils.hideView
import com.orderMate.utils.showView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Order History Dialog - displays all history events for an order
 * Styled like the filter modal with glass background
 */
class OrderHistoryDialog : DialogFragment() {

    private var _binding: DialogOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private var order: Order? = null
    private val historyItems = mutableListOf<HistoryItem>()

    data class HistoryItem(
        val title: String,
        val timestamp: Long,
        val iconRes: Int = R.drawable.ic_history
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.DialogTheme)
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
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.6f)
        }
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener { dismiss() }
        binding.btnClose.setOnClickListener { dismiss() }
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
                            title = "${getString(R.string.payment_added)} $amount",
                            timestamp = timestamp,
                            iconRes = R.drawable.ic_credit_card
                        )
                    )
                }
            }

            // Sort by timestamp descending (newest first)
            historyItems.sortByDescending { it.timestamp }
        }

        // Update empty state visibility
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

    // Inner adapter class
    inner class HistoryAdapter(
        private val items: List<HistoryItem>
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US)

        inner class HistoryViewHolder(val binding: ItemOrderHistoryBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = ItemOrderHistoryBinding.inflate(
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
