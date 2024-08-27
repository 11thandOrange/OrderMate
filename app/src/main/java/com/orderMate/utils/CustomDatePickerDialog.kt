package com.orderMate.utils

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.orderMate.R
import com.orderMate.communicators.IDateSelectedCommunicator
import java.text.SimpleDateFormat
import java.util.*

/**
 *  custom class for the date picker dialog.
 */
class CustomDatePickerFragment(
    private val listener : DatePickerDialog.OnDateSetListener,
    private val reqListener : IDateSelectedCommunicator
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentDate = Calendar.getInstance()
        val year = currentDate[Calendar.YEAR]
        val month = currentDate[Calendar.MONTH]
        val day = currentDate[Calendar.DAY_OF_MONTH]


       val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.MyDatePickerDialogTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate[selectedYear, selectedMonth] = selectedDay
                val selectedDateString =  SimpleDateFormat("M/d/yy", Locale.getDefault()).format(selectedDate.time)
                reqListener.provideCurrentSelectedDate(selectedDateString)
            },
            year,
            month,
            day
        )
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL , context?.getString(R.string.clear)) { _, _ ->
            reqListener.provideCurrentSelectedDate(Constants.NA)
            datePickerDialog.dismiss()
        }
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE , context?.getString(R.string.cancel)) { _, _ ->
            datePickerDialog.dismiss()
        }


        datePickerDialog.datePicker.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.back))
        datePickerDialog.show()

        return datePickerDialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val density = displayMetrics.density
            val maxDialogWidth = (550 * density).toInt()
            val currentWidth = (displayMetrics.widthPixels * 0.6).toInt()
            val width = if (currentWidth > maxDialogWidth) maxDialogWidth else currentWidth
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            setLayout(width, height)
        }


    }
}
