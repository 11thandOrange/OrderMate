package com.specialOrderNew.fragment.customFields

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.orderMate.R
import java.util.*

/**
 *  custom class for the date picker dialog.
 */
class CustomDatePickerFragment(
    private val listener: DatePickerDialog.OnDateSetListener
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentDate = Calendar.getInstance()
        val year = currentDate[Calendar.YEAR]
        val month = currentDate[Calendar.MONTH]
        val day = currentDate[Calendar.DAY_OF_MONTH]

        val datePicker = DatePickerDialog(requireContext(), R.style.MyDatePickerDialogTheme,listener, year, month, day)
        datePicker.datePicker.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.back))
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
