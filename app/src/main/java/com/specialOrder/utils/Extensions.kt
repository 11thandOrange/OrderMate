package com.specialOrder.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.clover.sdk.v3.order.Order
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.specialOrder.BuildConfig
import com.specialOrder.R
import com.specialOrder.activities.MainActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale


fun String.debugLog(tagName: String) {
    if (BuildConfig.DEBUG) {
        Timber.tag(tagName).e("Debug log $this")
    }
}

fun Fragment.debugSnackBar(tagName: String) {
    this.view?.let { Snackbar.make(it, tagName, BaseTransientBottomBar.LENGTH_LONG).show() }
}


fun View.debugSnackBar(tagName: String) {
    Snackbar.make(this, tagName, BaseTransientBottomBar.LENGTH_LONG).show()
}

fun Long.formatMillisToDateTime(format: String, isDate: Boolean = false): String {


    val dateTime = Date(this)
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    val convertedDate = dateFormat.format(dateTime)
    if (isDate) {
        val date = Date()
        val currentDateFormat = dateFormat.format(date)

        if (currentDateFormat == convertedDate) return "Today"
    }

    return convertedDate
}


/*
* Hide the visibility of the view
* */
fun View.hideView() {
    this.visibility = View.GONE
}


/*
* Show the visibility of the view
* */
fun View.showView() {
    this.visibility = View.VISIBLE
}

/*
* why inline if you don't use the inline fun then it will create more overhead
* whether it is negligible but this is more concise.
* */
inline fun Fragment.runOnMainThread(crossinline task: () -> Unit) {
    this.lifecycleScope.launch {
        task()
    }
}

/*
* Fragment is used here so that is can be used in any Fragment we does not need to import this
* */
inline fun Fragment.runOnBackgroundThread(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    crossinline task: () -> Unit
) {
    CoroutineScope(dispatcher).launch {
        task()
    }
}


inline fun exceptionHandler(crossinline task: () -> Unit) {
    try {
        task()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

inline fun exceptionHandler(crossinline task: () -> Unit, crossinline catchTask: () -> Unit) {
    try {
        task()
    } catch (e: Exception) {
        exceptionHandler(catchTask)
        e.printStackTrace()
    }
}


inline fun <T> exceptionHandlerWithReturn(crossinline task: () -> T): T? {
    return try {
        task()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun String.getOnlyFirstName(): String {
    try {
        val split = this.split('|')
        return split[0]
    } catch (e: NullPointerException) {
        e.printStackTrace()
    } catch (e: ArrayIndexOutOfBoundsException) {
        e.printStackTrace()
    }
    return Constants.merchantName
}


fun Int.toDoubleFloatPoint(): String {
    return "%.2f".format(this.toFloat() / 100)
}

fun Long.toDoubleFloatPoint(): String {
    return "%.2f".format((this.toDouble() / 100))
}

fun String.convertToTwoDecimal(): String {
    return try {
        val decimalIndex = this.indexOfFirst { it == '.' }
        if (decimalIndex == -1) {
            "$this.00"
        } else {
            val wholePartStr = this.substring(0, decimalIndex)
            val fractionalPartStr = this.substring(decimalIndex + 1)
            val fractionalPart = fractionalPartStr.take(2)
            "$wholePartStr.$fractionalPart"
        }
    } catch (e: NumberFormatException) {
        e.printStackTrace()
        ""
    }
}

fun Double.toDoubleFloatPoint(): String {
    return "%.2f".format(this)
}

fun Double.toDoubleFloatPointLatest(): String {
    return "%.2f".format(this / 100)
}

fun String.convertToSymbol(): String {
    if (this.trim().isEmpty()) return ""
    val currency: Currency = Currency.getInstance(this)
    return currency.symbol
}


fun MaterialTextView.changeColorAsPerPaymentStatus(
    order: Order?,
    isRefunded: Boolean? = false
) {
    if (isRefunded == true) {
        this.setTextColor(ContextCompat.getColor(this.context, R.color.other_order_payment_status))
        return
    }

    val color = if (order?.paymentState?.name != null) {
        when (order.paymentState?.name?.lowercase()) {
            Constants.OPEN.lowercase(), Constants.PARTIALLY_PAID.lowercase() -> R.color.open_status_color
            Constants.PAID.lowercase() -> R.color.paid_status_color
            Constants.REFUNDED.lowercase() -> R.color.other_order_payment_status
            else -> R.color.other_order_payment_status
        }
    } else {
        when (order?.state?.lowercase()) {
            Constants.OPEN.lowercase(), Constants.PARTIALLY_PAID.lowercase() -> R.color.open_status_color
            Constants.PAID.lowercase() -> R.color.paid_status_color
            Constants.REFUNDED.lowercase() -> R.color.other_order_payment_status
            else -> R.color.other_order_payment_status
        }
    }


    this.setTextColor(ContextCompat.getColor(this.context, color))
}

fun Fragment.navigate(action: Int) {
    exceptionHandler {
        findNavController().navigate(action)
    }
}

fun String.isInArray(list: HashMap<String, String>): Boolean {
    val newArr = this.split(",")
    newArr.forEach {
        if (list.values.contains(it)) {
            return true
        }
    }
    return false
}

fun Fragment.navigateDirection(navDirections: NavDirections) {
    exceptionHandler {
        findNavController().navigate(navDirections)
    }

}

fun Fragment.hideKeyboard(view: View?) {
    val imm = view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

// if you want the ui in the custom menu dialog to be dynamic just change the below function to view gone
// ui will start adjusting itself.so first test this if not work then made more changes
fun View.disabledAndAlphaChange() {
    this.alpha = 0.5f
    this.isEnabled = false
}


fun View.showSnackBar(message: String) {
    Snackbar.make(
        this,
        message,
        BaseTransientBottomBar.LENGTH_LONG
    ).show()
}

fun Fragment.onBackPressed(task: () -> Unit) {
    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            task()
        }
    }
    if (activity != null)
        (activity as MainActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
}


