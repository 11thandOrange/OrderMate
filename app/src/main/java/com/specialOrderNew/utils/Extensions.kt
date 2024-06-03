package com.specialOrderNew.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.specialOrderNew.BuildConfig
import com.specialOrderNew.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
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
* @param --> context : context of the parent activity to which this toast will link
* @param --> toastLength : length of the toast 0 for short length and 1 for the long length.
* */
fun String.debugToast(context: Context, toastLength: Int = 0) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(context, this, toastLength).show()
    }
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
    return "%.2f".format(this.toDouble() / 100)
}

fun Long.toDoubleFloatPoint(): String {
    return "%.2f".format(this.toDouble() / 100)
}

fun Double.toDoubleFloatPoint(): String {
    return "%.2f".format(this)
}

fun Double.toDoubleFloatPointLatest(): String {
    return "%.2f".format(this / 100)
}

fun Int.toIntPoint(): String {
    return "%.0f".format(this.toDouble() / 100)
}

fun Long.toIntPoint(): String {
    return "%.0f".format(this.toDouble() / 100)
}


fun Int.toIntPointResultInt(): Int {
    return (this.toDouble() / 100).toInt()
}

fun Long.toIntPointResultInt(): Int {
    return (this.toDouble() / 100).toInt()
}

fun String.convertToSymbol(): String {
    if (this.trim().isEmpty()) return ""
    val currency: Currency = Currency.getInstance(this)
    return currency.symbol
}

fun MaterialTextView.changeColorAsPerPaymentStatus(
    paymentState: String,
    isRefunded: Boolean? = false
) {
    if (isRefunded == true) {
        this.setTextColor(ContextCompat.getColor(this.context, R.color.other_order_payment_status))
        return
    }
    val color = when (paymentState.lowercase()) {
        Constants.OPEN.lowercase(), Constants.PARTIALLY_PAID.lowercase() -> R.color.open_status_color
        Constants.PAID.lowercase() -> R.color.paid_status_color
        Constants.REFUNDED.lowercase() -> R.color.other_order_payment_status
        else -> R.color.other_order_payment_status
    }
    this.setTextColor(ContextCompat.getColor(this.context, color))
}

fun Fragment.navigate(action: Int) {
    findNavController().navigate(action)
}

fun Fragment.navigateDirection(navDirections: NavDirections) {
    findNavController().navigate(navDirections)
}

fun makePair(
    response: Any,
    type: OrderApiTypes
): Pair<OrderApiTypes, Any> {
    return Pair(type, response)
}


fun View.showSnackBar() {
    Snackbar.make(
        this,
        "Internal Error Please open the App Again",
        BaseTransientBottomBar.LENGTH_LONG
    ).show()
}

fun List<String>.getExpandFilterList(): String {
    var resultant = ""
    this.forEach {
        resultant += it
    }
    return resultant
}


fun getException(e: Exception, context: Context): String {
    return when (e) {
        is ConnectException -> {
            context.getString(R.string.something_wrong)
        }

        is IOException -> {
            context.getString(R.string.please_check_internet)
        }

        else -> {
            context.getString(R.string.something_wrong)
        }
    }
}

