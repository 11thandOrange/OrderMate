package com.orderMate.fragment.orderDetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import com.orderMate.communicators.IDateSelectedCommunicator
import com.orderMate.communicators.ILineItemUpdateListener
import com.orderMate.databinding.DialogUpdateLineItemsBinding
import com.orderMate.modals.CustomItemJson
import com.orderMate.modals.ModalData
import com.orderMate.utils.Constants
import com.orderMate.utils.CustomDatePickerFragment
import com.orderMate.utils.ModalDialogCategories
import com.orderMate.utils.MyApp
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.countElementsByUniqueKeys
import com.orderMate.utils.debugLog
import com.orderMate.utils.debugSnackBar
import com.orderMate.utils.disabledAndAlphaChange
import com.orderMate.utils.hideKeyboard
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread


class CustomModalDialog(
    private val lineItemId: String?,
    private val orderData: Order?,
    private val orderId: String?,
    private val position: Int?,

    private val listener: ILineItemUpdateListener?,
    private val isFromDirectDialog : Boolean = false,
) : DialogFragment(), IDateSelectedCommunicator {


    private var customForData: CustomItemJson? = null
    private var populatedArray: MutableList<String> = mutableListOf()
    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }

    private val binding: DialogUpdateLineItemsBinding by lazy {
        DialogUpdateLineItemsBinding.inflate(layoutInflater)
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
    }

    private fun prepopulateTheDialog() {
        //&& position == index
        val data = countElementsByUniqueKeys(requireContext(), orderData?.lineItems ?: emptyList())
        runOnBackgroundThread {
            for ((index, i) in data.withIndex() ?: emptyList()) {
                if (i.order?.item?.id == lineItemId && i.order?.note != null && i.order.note?.trim()
                        ?.isNotEmpty() == true &&
                    position == index
                ) {

                    addTheDataToDialog(i.order?.note)
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
    private fun addTheDataToDialog(note: String?) {
        val array = note?.split("•")
        runOnMainThread {
            binding.apply {
                array?.forEach {
                    val splitItem = it.split(":")
                    if (splitItem.size >= 2) {
                        when (splitItem[0].trim().lowercase()) {
                            Constants.pickUp.lowercase() -> {
                                orderPickup.text = splitItem[1]
                            }

                            Constants.progress.lowercase() -> {
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
                    if (i.trim().equals(type?.trim(), true)) {
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
            listener?.dismissDialog()
            dismiss()
        }
        binding.orderPickup.setOnClickListener {
            showDatePickerDialog()
        }
        binding.updateButton.setOnClickListener {
            updateTheLineItem()
        }
    }

    private fun showDatePickerDialog() {
        val newFragment: DialogFragment = CustomDatePickerFragment({ _, year, month, day ->
            val selectedDate = "$day/${month + 1}/$year"
            provideCurrentSelectedDate(selectedDate)
        }, this)
        newFragment.show(parentFragmentManager, "datePicker")
    }

    private fun updateTheLineItem() {
        hideKeyboard(binding.root)

        var result = ""
        binding.apply {

            if (orderPickup.text?.toString()?.trim()
                    ?.isNotEmpty() == true && orderPickup.text?.toString()?.trim() != Constants.NA
            ) {
                result += getString(
                    R.string.note_string_pickUp,
                    orderPickup.text?.toString()?.trim()
                ) + getString(R.string.bullet_symbol)
            }
            if (orderProgress.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(
                    R.string.note_string_progress,
                    orderProgress.selectedItem?.toString()?.trim()
                ) + getString(R.string.bullet_symbol)
            }
            if (orderCategory.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(
                    R.string.note_string_category,
                    orderCategory.selectedItem?.toString()?.trim()
                ) + getString(R.string.bullet_symbol)
            }
            if (orderSubcategory.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(
                    R.string.note_string_Subcategory,
                    orderSubcategory.selectedItem?.toString()?.trim()
                ) + getString(R.string.bullet_symbol)

            }
            if (orderType.selectedItem?.toString()?.trim() != Constants.NA) {
                result += getString(
                    R.string.note_string_type,
                    orderType.selectedItem?.toString()?.trim()
                ) + getString(R.string.bullet_symbol)

            }
            if (etNotes.text?.toString()?.trim()?.isNotEmpty() == true) {
                result += getString(
                    R.string.note_string_description,
                    etNotes.text?.toString()?.trim()
                )
            }
        }

        if (result.length > Constants.notes_max_length) {
            val requiredLength = result.length - Constants.notes_max_length
            debugSnackBar("Description should not exceed ${result.length - requiredLength} characters")
            return
        }

        if (result.length > 2) {
            updateTheLineItemData(result.substring(0, result.length - 1))
        } else {
            updateTheLineItemData(result)
        }
    }

    private fun setUpDialog() {

        customForData?.types?.forEach {
            setupTheViews(it)
        }

    }

    private fun setupTheViews(it: ModalData) {
        when (it.type.name.trim()) {
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
                    binding.etNotesParent.disabledAndAlphaChange()
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
        json: String?,
        itemId: String = ""
    ) {
        runOnBackgroundThread {
            val data =
                countElementsByUniqueKeys(requireContext(), orderData?.lineItems ?: emptyList())
            val allLineItemsOfAnOrder = orderData?.lineItems



            for (it in allLineItemsOfAnOrder ?: emptyList()) {
                Log.d(
                    "codeCheckingAndroid",
                    "codeCheck: data is ${allLineItemsOfAnOrder}",
                )
                Log.d("codeCheckingAndroid", "updateTheLineItemData:  ${allLineItemsOfAnOrder?.size} ${it.id} ${data[position?:getPosition()].lineItemDifferentId}",)
                if (
                    ( !isFromDirectDialog && data[position?:getPosition()].lineItemDifferentId.contains(it.id))||
                    (isFromDirectDialog && it.item.id == lineItemId)) { // &&   // data[position?:getPosition()].lineItemDifferentId.contains(it.id)
                    Log.d("AboveErrorCheck", "updateTheLineItemData: Code check $lineItemId ${data[position ?: getPosition()]}  ")
                    it.note = json?.trim()
                    myApp.getOrderConnector().updateLineItems(orderId, allLineItemsOfAnOrder)
                }
            }
            Log.d("codeCheck", "updateTheLineItemData: id is $lineItemId  $json")
            position?.let { listener?.updateLineItem(lineItemId, json, position) }
        }
        listener?.dismissDialog()
        dismiss()
    }

    private fun getPosition(): Int {
        for (i in orderData?.lineItems ?: emptyList()) {
            Log.d("lineItemIs", "getPosition: the lineItemId is $lineItemId and item id is ${i.item.id}",)
            if (i.item.id == lineItemId) {
                return position ?: 0
            }
        }
        return 0
    }
}
