package com.specialOrder.fragment.orderDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v3.order.Order
import com.specialOrder.R
import com.specialOrder.communicators.IDateSelectedCommunicator
import com.specialOrder.communicators.ILineItemUpdateListener
import com.specialOrder.databinding.DialogUpdateLineItemsBinding
import com.specialOrder.modals.CustomItemJson
import com.specialOrder.modals.ModalData
import com.specialOrder.utils.Constants
import com.specialOrder.utils.DatePickerUtility
import com.specialOrder.utils.ModalDialogCategories
import com.specialOrder.utils.MyApp
import com.specialOrder.utils.PreferenceManager
import com.specialOrder.utils.debugLog
import com.specialOrder.utils.debugSnackBar
import com.specialOrder.utils.disabledAndAlphaChange
import com.specialOrder.utils.disabledAndinVisible
import com.specialOrder.utils.hideKeyboard
import com.specialOrder.utils.runOnBackgroundThread
import com.specialOrder.utils.runOnMainThread


class CustomModalDialog(
    private val lineItemId: String?,
    private val orderData: Order?,
    private val position: Int?,
    private val listener: ILineItemUpdateListener?,
) : DialogFragment(), IDateSelectedCommunicator {


    private var customForData: CustomItemJson? = null

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val binding: DialogUpdateLineItemsBinding by lazy {
        DialogUpdateLineItemsBinding.inflate(layoutInflater)
    }

    private val datePickerUtility: DatePickerUtility by lazy {
        DatePickerUtility.getInstance()
    }

    private val myApp: MyApp by lazy {
        MyApp.getInstance()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val requiredData = preferenceManager.getJsonString()
        if (requiredData is CustomItemJson) {
            customForData = requiredData
        }
        dialog?.setCancelable(false)
        setUpDialog()
        setupClickListeners()
        getOrderCategory()
        prepopulateTheDialog()
//        AppMeteredEvent().count = AppMeteredEvent()?.count + 1
    }


    private fun prepopulateTheDialog() {
        runOnBackgroundThread {
            for (i in orderData?.lineItems ?: emptyList()) {
                if (i.item?.id == lineItemId && i.note != null && i?.note?.trim()
                        ?.isNotEmpty() == true
                ) {
                    addTheDataToDialog(i.note)
                    break
                }
            }
        }
    }


    /*
    * We have the fixed structure for the note , where each key will be distinguished on the basis of
    * // delimiters now we convert the string to array on the basis and later split the subArray on the basis
    * of key value pair. this made this logic dynamic as we just need to add the constant which will help in getting
    * the value and update the value in the dialog
    * */
    private fun addTheDataToDialog(note: String) {
        val array = note.split("//")
        runOnMainThread {
            binding.apply {
                array.forEach {
                    val splitItem = it.split("=")
                    if (splitItem.size >= 2) {
                        when (splitItem[0]) {
                            Constants.pickUp -> {
                                orderPickup.text = splitItem[1]
                            }

                            Constants.progress -> {
                                orderProgress.setSelection(
                                    getIndex(
                                        splitItem[1],
                                        ModalDialogCategories.OrderProgress
                                    )
                                )
                            }

                            Constants.category -> {
                                orderCategory.setSelection(
                                    getIndex(
                                        splitItem[1],
                                        ModalDialogCategories.OrderCategories
                                    )
                                )
                            }

                            Constants.subcategory -> {
                                orderSubcategory.setSelection(
                                    getIndex(
                                        splitItem[1],
                                        ModalDialogCategories.OrderSubCategories
                                    )
                                )
                            }

                            Constants.description -> {
                                etNotes.setText(splitItem[1])
                            }

                            Constants.type -> {
                                orderType.setSelection(
                                    getIndex(
                                        splitItem[1],
                                        ModalDialogCategories.OrderType
                                    )
                                )
                            }
                        }
                    }
                }

            }
        }
    }


    private fun getIndex(type: String?, orderType: ModalDialogCategories): Int {
        customForData?.types?.forEach {
            if (it.type == orderType) {
                for ((pos, i) in it.list.withIndex()) {
                    if (i == type) {
                        return pos + 1
                    }
                }
            }
        }
        return 0
    }


    private fun getOrderCategory() {
        val window = dialog?.window
        if (window != null) {
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.5f).toInt()
            window.attributes = params
        }
    }

    private fun setupClickListeners() {
        binding.cancelDialog.setOnClickListener {
            hideKeyboard(binding.root)
            dismiss()
        }
        binding.orderPickup.setOnClickListener {
            datePickerUtility.showDatePickerDialog(
                requireContext(), this@CustomModalDialog
            )
        }
        binding.updateButton.setOnClickListener {
            updateTheLineItem()
        }
    }

    private fun updateTheLineItem() {
        hideKeyboard(binding.root)

        var result = ""
        binding.apply {

            if (orderPickup.text?.toString()?.trim()?.isNotEmpty() == true) {
                result += getString(R.string.note_string_pickUp ,orderPickup.text?.toString()?.trim() )
            }
            if (orderProgress.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(R.string.note_string_progress ,orderProgress.selectedItem?.toString()?.trim() )
            }
            if (orderCategory.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(R.string.note_string_category ,orderCategory.selectedItem?.toString()?.trim() )
            }
            if (orderSubcategory.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(R.string.note_string_Subcategory ,orderSubcategory.selectedItem?.toString()?.trim() )

            }
            if (orderType.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(R.string.note_string_type ,orderType.selectedItem?.toString()?.trim() )

            }
            if (etNotes.text?.toString()?.trim()?.isNotEmpty() == true) {
                result += getString(R.string.note_string_description ,etNotes.text?.toString()?.trim())
            }
        }

        if (result.length > Constants.notes_max_length) {
            val requiredLength = result.length - Constants.notes_max_length
            debugSnackBar("Description should not exceed ${result.length - requiredLength} characters")
            return
        }

        // user does not fill any field
        if(result.trim().isEmpty()){
            dismiss()
            return
        }

        updateTheLineItemData(result.substring(0, result.length - 2))
    }

    private fun setUpDialog() {
        binding.etNotes.clearFocus()
        customForData?.types?.forEach {
            setupTheViews(it)
        }

    }

    private fun setupTheViews(it: ModalData) {
        when (it.type.name) {
            ModalDialogCategories.PickUpDate.toString() -> {
                if (!it.isActive) {
                    binding.orderPickup.disabledAndAlphaChange()
                    binding.pickUpDateHeading.disabledAndAlphaChange()
                }
            }

            ModalDialogCategories.Description.toString() -> {
                if (!it.isActive) {
                    binding.etNotes.disabledAndAlphaChange()
                    binding.orderDescriptionHeading.disabledAndAlphaChange()
                    binding.etNotesParent.disabledAndinVisible()
                }
            }

            ModalDialogCategories.OrderType.toString() -> {
                if (!it.isActive || it.list.isEmpty()) {
                    binding.spinnerOrderType.disabledAndAlphaChange()
                    binding.orderType.disabledAndAlphaChange()
                    binding.orderTypeHeading.disabledAndAlphaChange()
                }
                setupSpinner(it, binding.orderType)
            }

            ModalDialogCategories.OrderProgress.toString() -> {
                if (!it.isActive || it.list.isEmpty()) {
                    binding.spinnerOrderProgress.disabledAndAlphaChange()
                    binding.orderProgress.disabledAndAlphaChange()
                    binding.orderProgressHeading.disabledAndAlphaChange()
                }
                setupSpinner(it, binding.orderProgress)
            }

            ModalDialogCategories.OrderCategories.toString() -> {
                if (!it.isActive || it.list.isEmpty()) {
                    binding.spinnerOrderCategory.disabledAndAlphaChange()
                    binding.orderCategory.disabledAndAlphaChange()
                    binding.orderCategoryHeading.disabledAndAlphaChange()
                }
                setupSpinner(it, binding.orderCategory)
            }

            ModalDialogCategories.OrderSubCategories.toString() -> {
                if (!it.isActive || it.list.isEmpty()) {
                    binding.spinnerOrderSubcategory.disabledAndAlphaChange()
                    binding.orderSubcategory.disabledAndAlphaChange()
                    binding.orderSubCategoryHeading.disabledAndAlphaChange()
                }
                setupSpinner(it, binding.orderSubcategory)

            }

            else -> "wrong option".debugLog(javaClass.simpleName)
        }
    }

    private fun setupSpinner(it: ModalData, spinner: Spinner) {
        val resultant = refreshList(it)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, resultant)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun refreshList(it: ModalData): List<String> {
        val newList: MutableList<String> = mutableListOf(Constants.NA)
        it.list.forEach {
            newList.add(it)
        }
        return newList
    }

    override fun onDestroy() {
        super.onDestroy()
        hideKeyboard(binding.root)
        "Dialog is Destroyed".debugLog(javaClass.simpleName)
    }

    override fun provideCurrentSelectedDate(date: String) {
        binding.orderPickup.text = date
    }

    private fun updateTheLineItemData(
        json: String
    ) {
        runOnBackgroundThread {
            val allLineItemsOfAnOrder = orderData?.lineItems

            for (i in allLineItemsOfAnOrder ?: emptyList()) {
                if (i?.item?.id == lineItemId) {
                    i?.note = json
                    myApp.getOrderConnector().updateLineItems(orderData?.id, allLineItemsOfAnOrder)
                }
            }
            position?.let { listener?.updateLineItem(lineItemId, json, position) }
        }
        dismiss()
    }
}
