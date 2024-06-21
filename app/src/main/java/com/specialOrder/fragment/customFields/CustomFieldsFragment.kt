package com.specialOrder.fragment.customFields

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.specialOrder.R
import com.specialOrder.adapters.MenuItemAdapter
import com.specialOrder.databinding.FragmentCustomFieldsBinding
import com.specialOrder.modals.CustomItemJson
import com.specialOrder.modals.ModalData
import com.specialOrder.utils.Constants
import com.specialOrder.utils.FirebaseRealtimeDataBaseManager
import com.specialOrder.utils.ModalDialogCategories
import com.specialOrder.utils.PreferenceManager
import com.specialOrder.utils.runOnBackgroundThread
import com.specialOrder.utils.runOnMainThread


class CustomFieldsFragment : Fragment() {

    companion object {
        var isDataSaved: Boolean = true
    }

    private val binding: FragmentCustomFieldsBinding by lazy {
        FragmentCustomFieldsBinding.inflate(layoutInflater)
    }

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(requireContext())
    }
    private var customData: CustomItemJson? = null

    private val modalData: ArrayList<ModalData> = arrayListOf(
        ModalData(
            "Pick Up Date",
            ModalDialogCategories.PickUpDate,
            hasDropDown = false,
            isActive = false,
            list = mutableListOf()
        ),
        ModalData(
            "Order Type",
            ModalDialogCategories.OrderType,
            hasDropDown = true,
            isActive = false,
            list = mutableListOf()
        ),
        ModalData(
            "Order Progress",
            ModalDialogCategories.OrderProgress,
            hasDropDown = true,
            isActive = false,
            list = mutableListOf()
        ),
        ModalData(
            "Category",
            ModalDialogCategories.OrderCategories,
            hasDropDown = true,
            isActive = false,
            list = mutableListOf()
        ),
        ModalData(
            "Sub-Category",
            ModalDialogCategories.OrderSubCategories,
            hasDropDown = true,
            isActive = true,
            list = mutableListOf()
        ),
        ModalData(
            "Description",
            ModalDialogCategories.Description,
            hasDropDown = false,
            isActive = true,
            list = mutableListOf()
        )
    )
    val data = CustomItemJson(modalData)

    @Synchronized
    fun updateCustomData(value: CustomItemJson) {
        customData = value
        runOnMainThread {
            setUpRecyclerView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpClickListener()

        addElementsToArray()
        runOnBackgroundThread {
            val result = preferenceManager.getJsonString()
            if (result is CustomItemJson) {
                updateCustomData(result)
            } else if (result == null) {
                updateCustomData(data)
            }

        }
    }

    private fun addElementsToArray() {
        binding.apply {

        }
    }

    private fun setUpRecyclerView() {
        binding.menuItemRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.menuItemRecycler.adapter = customData?.types?.let { MenuItemAdapter(it) }
    }


    private fun setUpClickListener() {
        binding.apply {
            backButton.setOnClickListener {
                if (isDataSaved) {
                    findNavController().popBackStack()
                } else {
                    showConfirmationDialog()
                }
            }
            saveLineItems.setOnClickListener {
                isDataSaved = true
                saveTheData()
                findNavController().popBackStack()
            }
        }
    }

    private fun saveTheData() {
        runOnBackgroundThread {
            customData?.let { it1 ->
                val list = Gson().toJson(it1)
                FirebaseRealtimeDataBaseManager.getInstance().saveData(list)
                preferenceManager.saveJsonString(
                    Constants.customMenuJson,
                    it1
                )
            }
        }
    }

    private fun showConfirmationDialog() {
        val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
        dialog.apply {
            setCancelable(false)
            setIcon(R.drawable.ic_order_big)
            setTitle(R.string.app_name)
            setMessage(getString(R.string.do_you_want_to_save_the_changes))
            setPositiveButton(R.string.save) { dialog, _ ->
                saveTheData()
                dialog.dismiss()
                findNavController().popBackStack()
            }
            setNegativeButton(getString(R.string.discard)) { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            val dialogInstance = dialog.create()
            dialogInstance.show()
        }
    }

    override fun onStop() {
        super.onStop()
        isDataSaved = true
    }

}