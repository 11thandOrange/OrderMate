package com.orderMate.fragment.orderDetail

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.sdk.v3.customers.Customer
import com.orderMate.R
import com.orderMate.databinding.DialogCustomerSearchBinding
import com.orderMate.databinding.ItemCustomerSearchResultBinding
import com.orderMate.repository.CloverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Customer Search Dialog - styled like filter modal
 * Allows searching for existing Clover customers by name, phone, or email
 */
class CustomerSearchDialog(
    private val onCustomerSelected: ((Customer) -> Unit)?
) : DialogFragment() {

    private var _binding: DialogCustomerSearchBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: CustomerSearchAdapter
    private var searchJob: Job? = null
    private var customers: List<Customer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val maxWidthPx = (520 * displayMetrics.density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            window.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomerSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = CustomerSearchAdapter { customer ->
            onCustomerSelected?.invoke(customer)
            dismiss()
        }
        binding.customersList.layoutManager = LinearLayoutManager(requireContext())
        binding.customersList.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Debounce search
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    searchCustomers(query)
                }
            }
        })
        
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchJob?.cancel()
                searchCustomers(binding.searchInput.text?.toString() ?: "")
                true
            } else {
                false
            }
        }
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text?.clear()
            customers = emptyList()
            adapter.submitList(emptyList())
            binding.customersList.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun searchCustomers(query: String) {
        if (query.length < 2) {
            customers = emptyList()
            adapter.submitList(emptyList())
            binding.customersList.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
            return
        }
        
        binding.searchProgress.visibility = View.VISIBLE
        binding.customersList.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val repository = CloverRepository.getInstance(requireContext())
                customers = repository.searchCustomers(query)
                
                binding.searchProgress.visibility = View.GONE
                
                if (customers.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.customersList.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.customersList.visibility = View.VISIBLE
                    adapter.submitList(customers)
                }
            } catch (e: Exception) {
                binding.searchProgress.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.customersList.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }

    companion object {
        const val TAG = "CustomerSearchDialog"
        
        fun newInstance(onCustomerSelected: ((Customer) -> Unit)?): CustomerSearchDialog {
            return CustomerSearchDialog(onCustomerSelected)
        }
    }

    /**
     * Adapter for customer search results
     */
    inner class CustomerSearchAdapter(
        private val onSelect: (Customer) -> Unit
    ) : RecyclerView.Adapter<CustomerSearchAdapter.ViewHolder>() {
        
        private var items: List<Customer> = emptyList()
        
        fun submitList(list: List<Customer>) {
            items = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCustomerSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(
            private val binding: ItemCustomerSearchResultBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(customer: Customer) {
                // Name
                val fullName = "${customer.firstName ?: ""} ${customer.lastName ?: ""}".trim()
                binding.customerName.text = fullName.ifEmpty { "Unknown" }
                
                // Avatar initials
                val first = customer.firstName?.firstOrNull()?.uppercaseChar() ?: ""
                val last = customer.lastName?.firstOrNull()?.uppercaseChar() ?: ""
                binding.customerAvatar.text = "$first$last".ifEmpty { "?" }
                
                // Contact info (prefer phone, fallback to email)
                val phone = customer.phoneNumbers?.firstOrNull()?.phoneNumber
                val email = customer.emailAddresses?.firstOrNull()?.emailAddress
                binding.customerContact.text = phone ?: email ?: ""
                
                // Select button
                binding.btnSelect.setOnClickListener {
                    onSelect(customer)
                }
                
                // Also allow clicking the whole row
                binding.root.setOnClickListener {
                    onSelect(customer)
                }
            }
        }
    }
}
