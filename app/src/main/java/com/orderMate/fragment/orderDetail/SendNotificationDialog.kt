package com.orderMate.fragment.orderDetail

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v3.order.Order
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.orderMate.R
import com.orderMate.communicators.IShareEmailOrMessage
import com.orderMate.databinding.DialogSendNotificationBinding
import com.orderMate.modals.Body
import com.orderMate.modals.Contact
import com.orderMate.modals.Html
import com.orderMate.modals.MessageMeta
import com.orderMate.modals.Metadata
import com.orderMate.modals.Receiver
import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal
import com.orderMate.modals.SmsBody
import com.orderMate.modals.Text
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.NotificationTemplate
import com.orderMate.utils.hideKeyboard
import com.orderMate.utils.hideView
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import com.orderMate.utils.MyApp

class SendNotificationDialog(
    private val order: Order?,
    private val listener: IShareEmailOrMessage
) : DialogFragment() {

    private val binding: DialogSendNotificationBinding by lazy {
        DialogSendNotificationBinding.inflate(layoutInflater)
    }

    private var isSmsEnabled: Boolean = true
    private var customerEmailArray: MutableList<String?> = mutableListOf()
    private var customerPhoneArray: MutableList<String?> = mutableListOf()
    private var templates: List<NotificationTemplate> = emptyList()

    @Synchronized
    fun tabChanged() {
        isSmsEnabled = !isSmsEnabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_OrderMate_Dialog)
    }
    
    override fun onStart() {
        super.onStart()
        // Ensure dialog window can receive focus for input
        dialog?.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDialog()
        preventKeyboardAutoOpen()
        setUpClickListener()
        loadTemplates()
        
        runOnBackgroundThread {
            dialog?.setCancelable(false)
            updateTheCustomerData()
        }
    }

    private fun setupDialog() {
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.6f)
            // Use SOFT_INPUT_ADJUST_RESIZE to allow keyboard while keeping focus
            // SOFT_INPUT_STATE_UNCHANGED prevents auto-open but allows input
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or 
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
            )
            // Ensure dialog can receive input focus
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun preventKeyboardAutoOpen() {
        // Clear focus from input fields initially to prevent auto-show
        // But don't make them unfocusable - user can still tap to focus
        binding.customerNumber.clearFocus()
        binding.subject.clearFocus()
        binding.etNotes.clearFocus()
    }

    private fun loadTemplates() {
        val app = requireContext().applicationContext as? MyApp ?: return
        val merchantId = app.getMerchantId() ?: return
        
        FirebaseConfigManager.getInstance().getTemplates(merchantId) { loadedTemplates ->
            templates = loadedTemplates
            runOnMainThread {
                setupTemplateSpinner()
            }
        }
    }

    private fun setupTemplateSpinner() {
        val templateNames = mutableListOf(getString(R.string.select_template))
        templateNames.addAll(templates.map { it.name })
        
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            templateNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.text_light))
                    textSize = 14f
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.text_light))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.glass_background))
                    setPadding(32, 24, 32, 24)
                    textSize = 14f
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.templateSpinner.adapter = adapter
        
        binding.templateSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position <= templates.size) {
                    val selectedTemplate = templates[position - 1]
                    // #63: Auto-fill content
                    binding.etNotes.setText(selectedTemplate.content)
                    // #64: Auto-fill subject for email (use template subject, not name)
                    if (!isSmsEnabled && selectedTemplate.subject.isNotBlank()) {
                        binding.subject.setText(selectedTemplate.subject)
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setUpClickListener() {
        binding.apply {
            shareEmail.setOnClickListener {
                if (isSmsEnabled) { // Only change if switching tabs
                    tabChanged()
                    updateTabStyles(isEmail = true)
                    binding.apply {
                        subjectContainer.showView()
                        customerNumber.hint = getString(R.string.enter_the_customer_email_address)
                        customerEmail.text = getString(R.string.customer_email)
                        customerNumber.inputType = InputType.TYPE_CLASS_TEXT
                        updateThePassedArray(customerEmailArray)
                    }
                }
            }

            shareSms.setOnClickListener {
                if (!isSmsEnabled) { // Only change if switching tabs
                    tabChanged()
                    updateTabStyles(isEmail = false)
                    binding.apply {
                        subjectContainer.hideView()
                        customerNumber.hint = getString(R.string.enter_the_customer_number)
                        customerEmail.text = getString(R.string.customer_number)
                        customerNumber.inputType = InputType.TYPE_CLASS_PHONE
                        updateThePassedArray(customerPhoneArray)
                    }
                }
            }
            
            cancelButton.setOnClickListener {
                hideKeyboard(binding.root)
                dismiss()
            }

            sendButton.setOnClickListener {
                hideKeyboard(binding.root)
                val validate = validateTheData()
                if (validate.second) {
                    Snackbar.make(binding.root, validate.first, BaseTransientBottomBar.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                if (isSmsEnabled) {
                    val data = processTheSmSData()
                    listener.shareSms(data)
                } else {
                    val data = processTheEmailData()
                    listener.sendEmail(data)
                }
                dismiss()
            }
        }
    }

    private fun updateTabStyles(isEmail: Boolean) {
        binding.apply {
            if (isEmail) {
                // Email tab selected
                shareEmail.setBackgroundResource(R.drawable.bg_filter_option_selected)
                shareEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                shareSms.setBackgroundResource(R.drawable.bg_filter_option)
                shareSms.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light))
            } else {
                // SMS tab selected
                shareSms.setBackgroundResource(R.drawable.bg_filter_option_selected)
                shareSms.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                shareEmail.setBackgroundResource(R.drawable.bg_filter_option)
                shareEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light))
            }
        }
    }


    /*
    * Validate the user enter data on the click of the send Button
    * @isSmsEnabled is true  : Check the data with respect to the sms validations
    * @isSmsEnabled is false : Check the data with respect to the email validation
    * */
    private fun validateTheData(): Pair<String, Boolean> {
        var result: Pair<String, Boolean> = Pair("", false)
        // if any of the field is empty and subject will be choose if the email field is active
        if (binding.customerNumber.text?.trim()?.isEmpty() == true ||
            (!isSmsEnabled && binding.subject.text?.trim()?.isEmpty() == true) ||
            binding.etNotes.text?.toString()?.trim()?.isEmpty() == true
        ) {
            result = Pair(getString(R.string.please_fill_all_the_fields), true)
            return result
        }
        // if user enter a invalid email id
        val data = binding.customerNumber.text?.split(",")
        if (data != null) {
            // if user has chosen the mobile field
            if (!isSmsEnabled) {
                for (i in data) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(i.trim()).matches()) {
                        result = Pair(getString(R.string.enter_the_valid_email_id), true)
                        break
                    }
                }
            } else {
                for (i in data) {
                    if (!i.trim().matches(Constants.numberRegex)) {
                        result = Pair(getString(R.string.enter_the_valid_mobile_number), true)
                        break
                    }
                }
            }

        }
        return result
    }

//    make the request for the messaging Bird for email
    private fun processTheEmailData(): ShareMessageJson {
        val list = getContactList()
        val messageReceiver = Receiver(list)

        val html = Html(
            binding.etNotes.text.toString(),
            Metadata(binding.subject.text.toString()),
            binding.etNotes.text.toString()
        )
        val body = Body(html, Constants.html)
        
        // Include order ID reference for notification history (#54)
        val orderId = order?.id
        val reference = orderId?.let { "order-$it" }
        val meta = orderId?.let {
            MessageMeta(
                extraInformation = mapOf(
                    "orderId" to it,
                    "type" to "email"
                )
            )
        }
        
        return ShareMessageJson(body, messageReceiver, reference, meta)
    }

    //    make the request for the messaging Bird for sms
    private fun processTheSmSData(): ShareSmsModal {
        val list = getContactList()
        val messageReceiver = Receiver(list)

        val smsBody = SmsBody(
            Text(binding.etNotes.text.toString()),
            Constants.text,
        )
        
        // Include order ID reference for notification history (#54)
        val orderId = order?.id
        val reference = orderId?.let { "order-$it" }
        val meta = orderId?.let {
            MessageMeta(
                extraInformation = mapOf(
                    "orderId" to it,
                    "type" to "sms"
                )
            )
        }
        
        return ShareSmsModal(smsBody, messageReceiver, reference, meta)
    }

    private fun getContactList(): List<Contact> {
        val resultant: MutableList<Contact> = mutableListOf()
        val data = binding.customerNumber.text.toString().split(",")
        data.forEach {
            resultant.add(
                Contact(
                    if (isSmsEnabled) Constants.phoneNumber else Constants.emailAddress,
                    if(isSmsEnabled && !it.contains("+" , true)) "+1$it" else it
                )
            )
        }
        return resultant
    }

    private fun updateThePassedArray(customerEmailArray: List<String?>) {
        var result = ""
        customerEmailArray.forEach {
            result += "$it, "
        }
        if (result.trim().isNotEmpty()) {
            result = result.substring(0, result.length - 2)
        }
        updateTheAdapter(result)
    }

    private fun updateTheCustomerData() {
        order?.customers?.forEach {
            it?.emailAddresses?.forEach { emailAddress ->
                customerEmailArray.add(emailAddress?.emailAddress)
            }
            it?.phoneNumbers?.forEach { phoneNumber ->
                customerPhoneArray.add(phoneNumber?.phoneNumber)
            }
            updateThePassedArray(customerPhoneArray)
        }
    }

    private fun updateTheAdapter(result: String) {
        runOnMainThread {
            binding.customerNumber.setText(result)
        }
    }


}