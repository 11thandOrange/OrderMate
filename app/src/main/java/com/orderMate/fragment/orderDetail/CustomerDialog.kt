package com.orderMate.fragment.orderDetail

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.customers.EmailAddress
import com.clover.sdk.v3.customers.PhoneNumber
import com.orderMate.R
import com.orderMate.databinding.DialogCustomerBinding
import com.orderMate.repository.CloverRepository
import kotlinx.coroutines.launch

/**
 * Dialog to display and edit customer details
 * Features:
 * - Editable fields: first name, last name, phone, email
 * - Search button: opens CustomerSearchDialog to find existing customers
 * - Close button: dismisses without saving
 * - Save button: saves/updates customer in Clover and assigns to order
 */
class CustomerDialog(
    private var customer: Customer?,
    private val orderId: String? = null,
    private val onCustomerUpdated: ((Customer?) -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogCustomerBinding? = null
    private val binding get() = _binding!!
    
    private var isSaving = false

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
            val maxWidthPx = (420 * displayMetrics.density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            val maxHeightPx = (displayMetrics.heightPixels * 0.85).toInt()
            
            window.setLayout(targetWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply {
                height = minOf(height, maxHeightPx)
            }
            // Ensure dialog can receive input focus for keyboard
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            // Set soft input mode to resize when keyboard appears
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
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
        setupTextWatchers()
    }

    private fun setupUI() {
        customer?.let { cust ->
            // Populate editable fields
            binding.inputFirstName.setText(cust.firstName ?: "")
            binding.inputLastName.setText(cust.lastName ?: "")
            
            val phoneNumber = cust.phoneNumbers?.firstOrNull()?.phoneNumber
            binding.inputPhone.setText(phoneNumber ?: "")
            
            val email = cust.emailAddresses?.firstOrNull()?.emailAddress
            binding.inputEmail.setText(email ?: "")
            
            // Set avatar initials
            updateAvatar()
        }
    }

    private fun setupTextWatchers() {
        // Update avatar when name changes
        val nameWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAvatar()
            }
        }
        binding.inputFirstName.addTextChangedListener(nameWatcher)
        binding.inputLastName.addTextChangedListener(nameWatcher)
    }

    private fun updateAvatar() {
        val firstName = binding.inputFirstName.text?.toString()
        val lastName = binding.inputLastName.text?.toString()
        val initials = getInitials(firstName, lastName)
        binding.customerAvatar.text = initials
    }

    private fun setupClickListeners() {
        // Cancel button (footer)
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // Search button - opens customer search dialog
        binding.btnSearch.setOnClickListener {
            openCustomerSearchDialog()
        }
        
        // Save button - saves customer to Clover
        binding.btnSave.setOnClickListener {
            saveCustomer()
        }
    }

    /**
     * Open customer search dialog to find existing Clover customers
     */
    private fun openCustomerSearchDialog() {
        val searchDialog = CustomerSearchDialog.newInstance { selectedCustomer ->
            // Customer selected from search - populate fields
            customer = selectedCustomer
            setupUI()
        }
        searchDialog.show(parentFragmentManager, CustomerSearchDialog.TAG)
    }

    /**
     * Save customer to Clover and assign to order
     */
    private fun saveCustomer() {
        if (isSaving) return
        
        val firstName = binding.inputFirstName.text?.toString()?.trim()
        val lastName = binding.inputLastName.text?.toString()?.trim()
        val phone = binding.inputPhone.text?.toString()?.trim()
        val email = binding.inputEmail.text?.toString()?.trim()
        
        // Validate at least one field is filled
        if (firstName.isNullOrEmpty() && lastName.isNullOrEmpty() && 
            phone.isNullOrEmpty() && email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter customer details", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSaving = true
        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.saving_customer)
        
        lifecycleScope.launch {
            try {
                val repository = CloverRepository.getInstance(requireContext())
                
                // Create or update customer in Clover (#67: Persist to Clover)
                val updatedCustomer = if (customer?.id != null) {
                    // Update existing customer in Clover
                    repository.updateCustomerInClover(
                        customerId = customer!!.id,
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        email = email
                    )
                } else {
                    // Create new customer and save to Clover
                    repository.createAndSaveCustomerToClover(
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        email = email
                    )
                }
                
                // Assign customer to order if orderId is provided
                if (updatedCustomer != null && !orderId.isNullOrEmpty()) {
                    // Use the overloaded method that takes a Customer object
                    repository.assignCustomerToOrder(orderId, updatedCustomer)
                }
                
                Toast.makeText(requireContext(), R.string.customer_saved, Toast.LENGTH_SHORT).show()
                onCustomerUpdated?.invoke(updatedCustomer)
                dismiss()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.customer_save_failed, Toast.LENGTH_SHORT).show()
                isSaving = false
                binding.btnSave.isEnabled = true
                binding.btnSave.text = getString(R.string.save)
            }
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
        
        fun newInstance(
            customer: Customer?,
            orderId: String? = null,
            onCustomerUpdated: ((Customer?) -> Unit)? = null
        ): CustomerDialog {
            return CustomerDialog(customer, orderId, onCustomerUpdated)
        }
    }
}
