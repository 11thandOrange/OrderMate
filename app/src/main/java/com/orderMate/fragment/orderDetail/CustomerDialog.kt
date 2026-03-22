package com.orderMate.fragment.orderDetail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.customers.Customer
import com.orderMate.R
import com.orderMate.databinding.DialogCustomerBinding

/**
 * Dialog to display customer details in the iOS-style redesign (#87)
 * Edit button launches Clover's customer editor (same as Register app)
 */
class CustomerDialog(
    private val customer: Customer?,
    private val orderId: String? = null,
    private val onCustomerEdited: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogCustomerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        customer?.let { cust ->
            // Set customer name
            val fullName = "${cust.firstName ?: ""} ${cust.lastName ?: ""}".trim()
            binding.customerName.text = fullName.ifEmpty { getString(R.string.customer) }
            
            // Set avatar initials
            val initials = getInitials(cust.firstName, cust.lastName)
            binding.customerAvatar.text = initials
            
            // Set phone number
            val phoneNumber = cust.phoneNumbers?.firstOrNull()?.phoneNumber
            if (!phoneNumber.isNullOrEmpty()) {
                binding.customerPhone.text = phoneNumber
            } else {
                binding.customerPhone.text = getString(R.string.no_number)
            }
            
            // Set email
            val email = cust.emailAddresses?.firstOrNull()?.emailAddress
            if (!email.isNullOrEmpty()) {
                binding.customerEmail.text = email
            } else {
                binding.customerEmail.text = getString(R.string.hypen_text)
            }
            
            // Set notes if available (using metadata or addresses as notes placeholder)
            val notes = cust.addresses?.firstOrNull()?.let { addr ->
                listOfNotNull(addr.address1, addr.city, addr.state).joinToString(", ")
            }
            if (!notes.isNullOrEmpty()) {
                binding.notesRow.visibility = View.VISIBLE
                binding.customerNotes.text = notes
            }
        } ?: run {
            binding.customerName.text = getString(R.string.customer)
            binding.customerAvatar.text = "?"
            binding.customerPhone.text = getString(R.string.no_number)
            binding.customerEmail.text = getString(R.string.hypen_text)
        }
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // Edit button - launches Clover's customer selector/editor
        // Same behavior as Clover Register app
        binding.btnEdit.setOnClickListener {
            launchCloverCustomerEditor()
        }
    }
    
    /**
     * Launch Clover's customer editor/selector
     * Uses Register app to edit customer on order (same as Clover Register flow)
     */
    private fun launchCloverCustomerEditor() {
        try {
            // Launch Register app with order - user can then add/edit customer
            if (!orderId.isNullOrEmpty()) {
                val intent = Intent(Intents.ACTION_START_REGISTER).apply {
                    putExtra(Intents.EXTRA_ORDER_ID, orderId)
                }
                startActivityForResult(intent, REQUEST_CUSTOMER_EDIT)
            } else {
                // Fallback: launch Clover Customers app directly
                val intent = Intent().apply {
                    setClassName("com.clover.customers", "com.clover.customers.activities.CustomersActivity")
                }
                startActivityForResult(intent, REQUEST_CUSTOMER_SELECT)
            }
        } catch (e: Exception) {
            // Try alternative customers activity
            try {
                val intent = Intent().apply {
                    setClassName("com.clover.customers", "com.clover.customers.CustomerSelectActivity")
                }
                startActivityForResult(intent, REQUEST_CUSTOMER_SELECT)
            } catch (e2: Exception) {
                // No Clover Customers app available
                android.widget.Toast.makeText(
                    requireContext(),
                    R.string.clover_customers_not_available,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CUSTOMER_EDIT || requestCode == REQUEST_CUSTOMER_SELECT) {
            // Customer was edited or selected, notify parent to refresh
            onCustomerEdited?.invoke()
            dismiss()
        }
    }

    private fun getInitials(firstName: String?, lastName: String?): String {
        val first = firstName?.firstOrNull()?.uppercaseChar() ?: ""
        val last = lastName?.firstOrNull()?.uppercaseChar() ?: ""
        return "$first$last".ifEmpty { "?" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CustomerDialog"
        private const val REQUEST_CUSTOMER_EDIT = 1001
        private const val REQUEST_CUSTOMER_SELECT = 1002
        
        fun newInstance(
            customer: Customer?,
            orderId: String? = null,
            onCustomerEdited: (() -> Unit)? = null
        ): CustomerDialog {
            return CustomerDialog(customer, orderId, onCustomerEdited)
        }
    }
}
