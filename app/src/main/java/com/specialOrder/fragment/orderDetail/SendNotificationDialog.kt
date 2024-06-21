package com.specialOrder.fragment.orderDetail

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v3.order.Order
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.specialOrder.R
import com.specialOrder.communicators.IShareEmailOrMessage
import com.specialOrder.databinding.DialogSendNotificationBinding
import com.specialOrder.modals.Body
import com.specialOrder.modals.Contact
import com.specialOrder.modals.Html
import com.specialOrder.modals.Metadata
import com.specialOrder.modals.Receiver
import com.specialOrder.modals.ShareMessageJson
import com.specialOrder.modals.ShareSmsModal
import com.specialOrder.modals.SmsBody
import com.specialOrder.modals.Text
import com.specialOrder.utils.Constants
import com.specialOrder.utils.hideKeyboard
import com.specialOrder.utils.hideView
import com.specialOrder.utils.runOnBackgroundThread
import com.specialOrder.utils.runOnMainThread
import com.specialOrder.utils.showView

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


    @Synchronized
    fun tabChanged() {
        isSmsEnabled = !isSmsEnabled
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
        setUpClickListener()
        runOnBackgroundThread {
            dialog?.setCancelable(false)
            updateTheCustomerData()
        }
    }


    private fun setUpClickListener() {
        binding.apply {
            shareEmail.setOnClickListener {
                tabChanged()
                binding.apply {
                    emailSubject.showView()
                    llSubject.showView()
                    customerNumber.hint = getString(R.string.enter_the_customer_email_address)
                    customerEmail.text = getString(R.string.customer_email)
                    customerNumber.inputType = InputType.TYPE_CLASS_TEXT
                    shareSms.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )
                    shareEmail.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.darker_gray_light
                        )
                    )
                    updateThePassedArray(customerEmailArray)
                }
            }

            shareSms.setOnClickListener {
                tabChanged()
                binding.apply {
                    emailSubject.hideView()
                    llSubject.hideView()
                    customerNumber.hint = getString(R.string.enter_the_customer_number)
                    customerEmail.text = getString(R.string.customer_number)
                    customerNumber.inputType = InputType.TYPE_CLASS_PHONE
                    shareSms.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.darker_gray_light
                        )
                    )
                    binding.shareEmail.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )
                    updateThePassedArray(customerPhoneArray)
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
        return ShareMessageJson(body, messageReceiver)
    }

    //    make the request for the messaging Bird for sms
    private fun processTheSmSData(): ShareSmsModal {
        val list = getContactList()
        val messageReceiver = Receiver(list)

        val html = SmsBody(
            Text(binding.etNotes.text.toString()),
            Constants.text,
        )
        return ShareSmsModal(html, messageReceiver)
    }

    private fun getContactList(): List<Contact> {
        val resultant: MutableList<Contact> = mutableListOf()
        val data = binding.customerNumber.text.toString().split(",")
        data.forEach {
            resultant.add(
                Contact(
                    if (isSmsEnabled) Constants.phoneNumber else Constants.emailAddress,
                    it
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