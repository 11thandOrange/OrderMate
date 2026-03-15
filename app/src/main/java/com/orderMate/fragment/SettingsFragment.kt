package com.orderMate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.orderMate.BuildConfig
import com.orderMate.R
import com.orderMate.utils.SettingsManager

/**
 * iOS-style Settings Fragment (#80, #83 requirement)
 * 
 * Features:
 * - Widget configuration
 * - Theme color customization
 * - Notifications toggle
 * - Auto sync settings
 */
class SettingsFragment : Fragment() {

    private lateinit var settingsManager: SettingsManager
    
    private var notificationsSwitch: Switch? = null
    private var autoSyncSwitch: Switch? = null
    private var syncNowButton: Button? = null
    private var appVersion: TextView? = null
    private var themeColorPreview: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_redesign, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsManager = SettingsManager(requireContext())
        
        initViews(view)
        setupListeners()
        loadSettings()
    }

    private fun initViews(view: View) {
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)
        autoSyncSwitch = view.findViewById(R.id.autoSyncSwitch)
        syncNowButton = view.findViewById(R.id.syncNowButton)
        appVersion = view.findViewById(R.id.appVersion)
        themeColorPreview = view.findViewById(R.id.themeColorPreview)
    }

    private fun setupListeners() {
        notificationsSwitch?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setNotificationsEnabled(isChecked)
        }

        autoSyncSwitch?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setAutoSyncEnabled(isChecked)
        }

        syncNowButton?.setOnClickListener {
            Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
            // Trigger sync
        }

        themeColorPreview?.setOnClickListener {
            // Show color picker dialog
            Toast.makeText(requireContext(), "Color picker coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        notificationsSwitch?.isChecked = settingsManager.getNotificationsEnabled()
        autoSyncSwitch?.isChecked = settingsManager.getAutoSyncEnabled()
        appVersion?.text = "OrderMate v${BuildConfig.VERSION_NAME}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationsSwitch = null
        autoSyncSwitch = null
        syncNowButton = null
        appVersion = null
        themeColorPreview = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
