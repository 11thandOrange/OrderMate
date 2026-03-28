package com.orderMate.fragment.customFields

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.orderMate.R
import com.orderMate.adapters.MenuItemAdapter
import com.orderMate.databinding.FragmentCustomFieldsBinding
import com.orderMate.fragment.orderHistory.OrderHistoryFragment
import com.orderMate.modals.CustomItemJson
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseRealtimeDataBaseManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.defaultCustomDataForFirebase
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.hideKeyboard
import com.orderMate.utils.isAllFieldDisabled
import com.orderMate.utils.onBackPressed
import com.orderMate.utils.runOnBackgroundThread
import com.orderMate.utils.runOnMainThread
import com.orderMate.utils.showView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


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

    private val myApplication: MyApp by lazy {
        MyApp.getInstance()
    }

    private var customData: CustomItemJson? = null


    @Synchronized
    fun updateCustomData(value: CustomItemJson) {
        customData = value
        runOnMainThread {
            setUpRecyclerView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exceptionHandler { hideKeyboard(binding.root) }
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
        setListener()
        onBackPressed {
            backPressesHandler()
        }
        runOnBackgroundThread {
            val result = preferenceManager.getJsonString()
            updateCustomData(result)
        }
    }

    private fun setListener() {
        binding.menuEnableButton.isChecked =
            preferenceManager.getBoolean(Constants.isMenuOptionEnabled)
        binding.menuBasketButton.isChecked =
            preferenceManager.getBoolean(Constants.isMenuBasketOptionEnabled)
    }


    private fun setUpRecyclerView() {
        binding.menuItemRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.menuItemRecycler.adapter = customData?.types?.let { MenuItemAdapter(it) }
    }


    private fun setUpClickListener() {
        binding.apply {
            backButton.setOnClickListener {
                backPressesHandler()
            }
            saveLineItems.setOnClickListener {
                isDataSaved = true
                binding.progressLayout.showView()
                CoroutineScope(Dispatchers.Default).launch {
                    saveTheData()
                }
            }
        }
    }

    private fun backPressesHandler() {
        if (isDataSaved) {
            findNavController().popBackStack()
        } else {
            showConfirmationDialog()
        }
    }

    private suspend fun saveTheData() {
        var isSuccess = false
        customData?.let { it1 ->
            isAllFieldDisabled(preferenceManager, it1)
            val list = Gson().toJson(it1)
          val result = CoroutineScope(Dispatchers.IO).async {
              FirebaseRealtimeDataBaseManager.getInstance()
                  .saveData(requireContext(),list, myApplication.getMerchantId()) {
                       isSuccess = it
                  }
          }
            result.await()
            preferenceManager.saveJsonString(
                Constants.customMenuJson,
                it1
            ) {
                OrderHistoryFragment.getInstance().updateTheSpinners(isSuccess)
                runOnMainThread {
                    findNavController().popBackStack()

                }
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
                binding.progressLayout.showView()
                CoroutineScope(Dispatchers.IO).launch {
                    saveTheData()
                }
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