package com.orderMate.fragment.orderDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v3.customers.Customer
import com.orderMate.R
import com.orderMate.databinding.DialogCustomerBinding

/**
 * Dialog to display customer details in the iOS-style redesign (#87)
 */
class CustomerDialog(
    private val customer: Customer?
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
        
        fun newInstance(customer: Customer?): CustomerDialog {
            return CustomerDialog(customer)
        }
    }
}
