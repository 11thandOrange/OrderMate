package com.orderMate.fragment.orderDetail

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import com.orderMate.services.TemplateProcessor
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.NotificationTemplate
import com.orderMate.utils.hideKeyboard
import com.orderMate.utils.hideView
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import com.orderMate.utils.MyApp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var merchantName: String = ""

    @Synchronized
    fun tabChanged() {
        isSmsEnabled = !isSmsEnabled
    }
    
    /**
     * Process template content by replacing {{placeholders}} with actual order data
     */
    private fun processTemplateContent(template: String): String {
        val customerName = order?.customers?.firstOrNull()?.let { customer ->
            listOfNotNull(customer.firstName, customer.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        } ?: ""
        
        val orderTotal = order?.total?.let { total ->
            NumberFormat.getCurrencyInstance(Locale.US).format(total / 100.0)
        } ?: ""
        
        val dueDate = order?.createdTime?.let { timestamp ->
            SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date(timestamp))
        } ?: ""
        
        val dueTime = order?.createdTime?.let { timestamp ->
            SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
        } ?: ""
        
        val itemCount = order?.lineItems?.size ?: 0
        
        val orderNotes = order?.note ?: ""
        
        return TemplateProcessor.processForOrder(
            template = template,
            merchantName = merchantName,
            orderId = order?.id ?: "",
            customerName = customerName,
            orderTotal = orderTotal,
            dueDate = dueDate,
            dueTime = dueTime,
            itemCount = itemCount,
            orderNotes = orderNotes
        )
    }

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
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val maxWidthPx = (520 * displayMetrics.density).toInt()
            val targetWidth = minOf((screenWidth * 0.9).toInt(), maxWidthPx)
            val maxHeightPx = (displayMetrics.heightPixels * 0.85).toInt()

            window.setLayout(targetWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply {
                height = minOf(height, maxHeightPx)
            }
            // Ensure dialog can receive input focus
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
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
        dialog?.window?.setDimAmount(0.6f)
    }

    private fun preventKeyboardAutoOpen() {
        // Clear focus from input fields initially to prevent auto-show
        // But don't make them unfocusable - user can still tap to focus
        binding.customerNumber.clearFocus()
        binding.subject.clearFocus()
        binding.etNotes.clearFocus()
    }

    private fun loadTemplates() {
        setupTemplateSpinner()
        
        runOnBackgroundThread {
            try {
                val app = requireContext().applicationContext as? MyApp ?: return@runOnBackgroundThread
                val merchantId = app.getMerchantId() ?: return@runOnBackgroundThread
                
                // Load merchant name for template processing
                app.getMerchantName()?.let { name ->
                    merchantName = name
                }
                
                FirebaseConfigManager.getInstance().getTemplates(merchantId) { loadedTemplates ->
                    templates = loadedTemplates
                    runOnMainThread {
                        if (isAdded) {
                            setupTemplateSpinner()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupTemplateSpinner() {
        if (!isAdded || context == null) return
        
        val templateNames = mutableListOf<String>()
        templateNames.add(getString(R.string.select_template))
        
        if (templates.isNotEmpty()) {
            templateNames.addAll(templates.map { it.name })
        }
        
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
                    val density = resources.displayMetrics.density
                    val heightPx = (48 * density).toInt()
                    val horizPadding = (16 * density).toInt()
                    val cornerRadius = 10 * density
                    
                    val isFirstItem = position == 0
                    val isLastItem = position == count - 1
                    
                    // Create background with rounded corners for first/last items
                    val bgShape = android.graphics.drawable.GradientDrawable()
                    bgShape.setColor(android.graphics.Color.parseColor("#292D3E"))
                    
                    when {
                        count == 1 -> bgShape.cornerRadii = floatArrayOf(
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius
                        )
                        isFirstItem -> bgShape.cornerRadii = floatArrayOf(
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                            0f, 0f, 0f, 0f
                        )
                        isLastItem -> bgShape.cornerRadii = floatArrayOf(
                            0f, 0f, 0f, 0f,
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius
                        )
                        else -> bgShape.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    }
                    
                    // Add divider at bottom (except last item)
                    if (!isLastItem) {
                        val dividerDrawable = android.graphics.drawable.GradientDrawable()
                        dividerDrawable.setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                        val layerDrawable = android.graphics.drawable.LayerDrawable(arrayOf(bgShape, dividerDrawable))
                        layerDrawable.setLayerInset(1, 0, heightPx - (1 * density).toInt(), 0, 0)
                        background = layerDrawable
                    } else {
                        background = bgShape
                    }
                    
                    setPadding(horizPadding, 0, horizPadding, 0)
                    height = heightPx
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    textSize = 14f
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.templateSpinner.adapter = adapter
        
        // Set dropdown width and position to match the container
        binding.templateSpinnerContainer.post {
            val containerWidth = binding.templateSpinnerContainer.width
            binding.templateSpinner.dropDownWidth = containerWidth
            // Center dropdown under container (spinner has padding that offsets it)
            val spinnerPaddingStart = binding.templateSpinner.paddingStart
            binding.templateSpinner.dropDownHorizontalOffset = -spinnerPaddingStart
        }
        
        // Make container click also trigger spinner
        binding.templateSpinnerContainer.setOnClickListener {
            binding.templateSpinner.performClick()
        }
        
        binding.templateSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position <= templates.size) {
                    val selectedTemplate = templates[position - 1]
                    // #63: Auto-fill content with processed template (replace {{placeholders}})
                    val processedContent = processTemplateContent(selectedTemplate.content)
                    binding.etNotes.setText(processedContent)
                    // #64: Auto-fill subject for email (also process placeholders)
                    if (!isSmsEnabled && selectedTemplate.subject.isNotBlank()) {
                        val processedSubject = processTemplateContent(selectedTemplate.subject)
                        binding.subject.setText(processedSubject)
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