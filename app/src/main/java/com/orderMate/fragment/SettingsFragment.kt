package com.orderMate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.orderMate.R
import com.orderMate.databinding.FragmentSettingsBinding
import com.orderMate.utils.SettingsManager

/**
 * Settings Fragment (Issue #83)
 * 
 * Contains 4 sub-tabs:
 * - General: Toggle register options
 * - Pop Up: Editable widget configuration
 * - Advanced: Notification frequency, receipt settings
 * - Notification: SMS number, templates
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsManager: SettingsManager
    private var currentSubtab = SubTab.GENERAL

    enum class SubTab {
        GENERAL, POPUP, ADVANCED, NOTIFICATION
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsManager = SettingsManager(requireContext())
        
        setupSubtabs()
        setupGeneralPanel()
        setupPopupPanel()
        setupAdvancedPanel()
        setupNotificationPanel()
        
        loadSettings()
    }

    private fun setupSubtabs() {
        binding.subtabGeneral.setOnClickListener { switchSubtab(SubTab.GENERAL) }
        binding.subtabPopup.setOnClickListener { switchSubtab(SubTab.POPUP) }
        binding.subtabAdvanced.setOnClickListener { switchSubtab(SubTab.ADVANCED) }
        binding.subtabNotification.setOnClickListener { switchSubtab(SubTab.NOTIFICATION) }
    }

    private fun switchSubtab(subtab: SubTab) {
        currentSubtab = subtab
        
        // Update button states
        binding.subtabGeneral.isSelected = subtab == SubTab.GENERAL
        binding.subtabPopup.isSelected = subtab == SubTab.POPUP
        binding.subtabAdvanced.isSelected = subtab == SubTab.ADVANCED
        binding.subtabNotification.isSelected = subtab == SubTab.NOTIFICATION
        
        // Show/hide panels
        binding.generalPanel.visibility = if (subtab == SubTab.GENERAL) View.VISIBLE else View.GONE
        binding.popupPanel.visibility = if (subtab == SubTab.POPUP) View.VISIBLE else View.GONE
        binding.advancedPanel.visibility = if (subtab == SubTab.ADVANCED) View.VISIBLE else View.GONE
        binding.notificationPanel.visibility = if (subtab == SubTab.NOTIFICATION) View.VISIBLE else View.GONE
    }

    // ==================== General Panel ====================

    private fun setupGeneralPanel() {
        binding.toggleOrderMateRegister.setOnClickListener {
            val isEnabled = !binding.toggleOrderMateRegister.isSelected
            binding.toggleOrderMateRegister.isSelected = isEnabled
            settingsManager.setUseOrderMateRegister(isEnabled)
        }

        binding.toggleBothRegisters.setOnClickListener {
            val isEnabled = !binding.toggleBothRegisters.isSelected
            binding.toggleBothRegisters.isSelected = isEnabled
            settingsManager.setUseBothRegisters(isEnabled)
        }
    }

    // ==================== Pop Up Panel ====================

    private fun setupPopupPanel() {
        // Widget management handled by WidgetAdapter
        // Add widget button
        binding.addWidgetBtn.setOnClickListener {
            val widgetCount = settingsManager.getWidgets().size
            if (widgetCount >= 7) {
                showToast("Maximum 7 widgets allowed")
                return@setOnClickListener
            }
            showAddWidgetDialog()
        }
    }

    private fun showAddWidgetDialog() {
        // Show dialog to select widget type
        // Options: Calendar, Single Select, Multi Select, Text Box
    }

    // ==================== Advanced Panel ====================

    private fun setupAdvancedPanel() {
        binding.notificationDaysInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val days = binding.notificationDaysInput.text.toString().toIntOrNull() ?: 3
                settingsManager.setNotificationDays(days)
            }
        }

        binding.receiptTimeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val time = binding.receiptTimeInput.text.toString().toIntOrNull() ?: 60
                settingsManager.setReceiptTime(time)
            }
        }

        binding.receiptUnitSpinner.setOnItemSelectedListener { position ->
            val unit = when (position) {
                0 -> "minutes"
                1 -> "hours"
                2 -> "days"
                else -> "minutes"
            }
            settingsManager.setReceiptUnit(unit)
        }
    }

    // ==================== Notification Panel ====================

    private fun setupNotificationPanel() {
        binding.smsNumberInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                settingsManager.setSmsNumber(binding.smsNumberInput.text.toString())
            }
        }

        binding.templateTextarea.addTextChangedListener { text ->
            binding.charCount.text = "${text?.length ?: 0}/250"
            settingsManager.setNotificationTemplate(text.toString())
        }
    }

    // ==================== Load Settings ====================

    private fun loadSettings() {
        // General
        binding.toggleOrderMateRegister.isSelected = settingsManager.getUseOrderMateRegister()
        binding.toggleBothRegisters.isSelected = settingsManager.getUseBothRegisters()

        // Advanced
        binding.notificationDaysInput.setText(settingsManager.getNotificationDays().toString())
        binding.receiptTimeInput.setText(settingsManager.getReceiptTime().toString())
        // Set spinner selection based on unit

        // Notification
        binding.smsNumberInput.setText(settingsManager.getSmsNumber())
        binding.templateTextarea.setText(settingsManager.getNotificationTemplate())
        binding.charCount.text = "${settingsManager.getNotificationTemplate().length}/250"
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}

// Extension function for spinner
private fun android.widget.Spinner.setOnItemSelectedListener(callback: (Int) -> Unit) {
    this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
            callback(position)
        }
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}

// Extension function for EditText
private fun android.widget.EditText.addTextChangedListener(callback: (CharSequence?) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callback(s)
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    })
}
