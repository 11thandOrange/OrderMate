package com.specialOrder.utils

import android.app.DatePickerDialog
import android.content.Context
import androidx.core.content.ContextCompat
import com.specialOrder.R
import com.specialOrder.communicators.IDateSelectedCommunicator
import java.util.Calendar

class DatePickerUtility private constructor() {

    private var datePickerDialog: DatePickerDialog? = null

    companion object {
        private var instance: DatePickerUtility? = null

        fun getInstance(): DatePickerUtility {
            return instance ?: synchronized(this) {
                DatePickerUtility().also { instance = it }
            }
        }
    }

    fun showDatePickerDialog(context: Context, listener: IDateSelectedCommunicator) {
        val currentDate = Calendar.getInstance()
        val year = currentDate[Calendar.YEAR]
        val month = currentDate[Calendar.MONTH]
        val day = currentDate[Calendar.DAY_OF_MONTH]

        datePickerDialog = DatePickerDialog(
            context,
            R.style.MyDatePickerDialogTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                listener.provideCurrentSelectedDate(selectedDate)
            },
            year,
            month,
            day
        )
        changeDatePickerColor(context)
        datePickerDialog?.show()
    }

    private fun changeDatePickerColor(context: Context, colorId: Int = R.color.back) {
        datePickerDialog?.datePicker?.setBackgroundColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
    }
}