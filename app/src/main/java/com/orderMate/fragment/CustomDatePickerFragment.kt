package com.specialOrderNew.fragment.customFields

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.orderMate.R
import com.orderMate.utils.ProfileSettingsManager
import java.util.*

/**
 * Custom class for the date picker dialog.
 * (#13) Updated to use user's theme color for header and orange accent for selection
 */
class CustomDatePickerFragment(
    private val listener: DatePickerDialog.OnDateSetListener
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentDate = Calendar.getInstance()
        val year = currentDate[Calendar.YEAR]
        val month = currentDate[Calendar.MONTH]
        val day = currentDate[Calendar.DAY_OF_MONTH]

        val datePicker = DatePickerDialog(requireContext(), R.style.MyDatePickerDialogTheme, listener, year, month, day)
        
        // Set background color
        datePicker.datePicker.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.back))
        
        // (#13) Apply user's theme color to header
        try {
            val settingsManager = ProfileSettingsManager.getInstance(requireContext())
            val themeColor = Color.parseColor(settingsManager.getThemeColor())
            
            // Find and color the header view
            val headerId = resources.getIdentifier("android:id/date_picker_header", null, null)
            if (headerId != 0) {
                datePicker.datePicker.findViewById<android.view.View>(headerId)?.setBackgroundColor(themeColor)
            }
        } catch (e: Exception) {
            // Fallback - use default styling if theme color fails
            e.printStackTrace()
        }
        
        return datePicker
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = (displayMetrics.widthPixels * 0.6).toInt()
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            setLayout(width, height)
        }
    }
}
