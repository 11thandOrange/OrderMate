package com.orderMate.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.clover.sdk.v3.customers.Customer
import com.clover.sdk.v3.order.LineItem
import com.clover.sdk.v3.order.Order
import com.google.android.material.textview.MaterialTextView
import com.orderMate.R

import com.orderMate.modals.CustomItemJson
import com.orderMate.modals.ItemModal
import com.orderMate.modals.ModalData
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * @deprecated Use [DefaultWidgetFactory.createDefaults] instead for V2 schema.
 */
@Deprecated("Use DefaultWidgetFactory.createDefaults() for V2 schema")
private val modalData: ArrayList<ModalData> = arrayListOf(
    ModalData(
        Constants.isCustomModalShown,
        ModalDialogCategories.ModalShown,
        hasDropDown = false,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.isCustomModalBasket,
        ModalDialogCategories.BasketShown,
        hasDropDown = false,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbPickUp,
        ModalDialogCategories.PickUpDate,
        hasDropDown = false,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbType,
        ModalDialogCategories.OrderType,
        hasDropDown = true,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbStatus,
        ModalDialogCategories.OrderProgress,
        hasDropDown = true,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbCategory,
        ModalDialogCategories.OrderCategories,
        hasDropDown = true,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbSubcategory,
        ModalDialogCategories.OrderSubCategories,
        hasDropDown = true,
        isActive = true,
        list = mutableListOf()
    ),
    ModalData(
        Constants.fbDescription,
        ModalDialogCategories.Description,
        hasDropDown = false,
        isActive = true,
        list = mutableListOf()
    )
)


/**
 * @deprecated Use [DefaultWidgetFactory.createDefaults] instead for V2 schema.
 * This V1 data structure is being replaced by the WidgetConfig/PopupSettings schema.
 */
@Deprecated("Use DefaultWidgetFactory.createDefaults() for V2 schema")
val defaultCustomDataForFirebase = CustomItemJson(modalData)

fun getCustomerContactDetails(customer: Customer?): Pair<String, String> {
    var resultNumber = ""
    var resultEmail = ""

    customer?.phoneNumbers?.forEach {
        resultNumber += "${it?.phoneNumber?.removePrefix("+")}, "
    }
    customer?.emailAddresses?.forEach {
        resultEmail += "${it?.emailAddress?.removePrefix("+")}, "
    }
    if (resultNumber.trim().isNotEmpty()) resultNumber =
        resultNumber.substring(0, resultNumber.length - 2)
    if (resultEmail.trim().isNotEmpty()) resultEmail =
        resultEmail.substring(0, resultEmail.length - 2)
    return Pair(resultNumber, resultEmail)
}

/**
 * Formats Clover payment state to display text (UPPERCASE).
 * Single source of truth for payment status formatting.
 * 
 * Mapping (Clover SDK PaymentState enum):
 * - OPEN → OPEN
 * - PAID → PAID
 * - PARTIALLY_PAID → PARTIALLY PAID
 * - PARTIALLY_REFUNDED → PARTIALLY REFUNDED
 * - REFUNDED → REFUNDED
 * - CREDITED → CREDITED
 * - null/empty → empty string (no pill should be shown)
 */
fun formatPaymentState(state: String?): String {
    if (state.isNullOrEmpty()) return ""
    
    return when (state.uppercase()) {
        "OPEN" -> "OPEN"
        "PAID" -> "PAID"
        "PARTIALLY_PAID" -> "PARTIALLY PAID"
        "PARTIALLY_REFUNDED" -> "PARTIALLY REFUNDED"
        "REFUNDED" -> "REFUNDED"
        "CREDITED" -> "CREDITED"
        else -> state.replace("_", " ").uppercase()
    }
}

/**
 * (#76) Gets the payment state string from an Order.
 * Single source of truth for extracting payment state from orders.
 * 
 * Uses paymentState if available from Clover SDK, otherwise infers from payment data:
 * - No payments → OPEN
 * - Partial payment (paid < total) → PARTIALLY_PAID  
 * - Fully paid (paid >= total) → PAID
 * - Has refunds → REFUNDED or PARTIALLY_REFUNDED based on amounts
 */
fun getPaymentStateFromOrder(order: Order?): String? {
    if (order == null) {
        android.util.Log.d("PaymentStateDebug", "order is null")
        return null
    }
    
    val orderId = order.id?.takeLast(8) ?: "unknown"
    val cloverPaymentState = order.paymentState?.name
    val total = order.total ?: 0L
    val unpaidBalance = order.unpaidBalance
    val payments = order.payments
    val paymentCount = payments?.size ?: 0
    val state = order.state
    
    // Sum payment amounts
    var totalPaid = 0L
    payments?.forEach { payment ->
        totalPaid += payment.amount ?: 0L
    }
    
    android.util.Log.d("PaymentStateDebug", "Order #$orderId - paymentState: $cloverPaymentState, state: $state, total: $total, unpaidBalance: $unpaidBalance, payments: $paymentCount, totalPaid: $totalPaid")
    
    // Use Clover's paymentState if available
    if (cloverPaymentState != null) {
        return cloverPaymentState
    }
    
    // Clover doesn't populate paymentState, infer from payments vs total
    val inferred = when {
        totalPaid <= 0 -> "OPEN"
        totalPaid >= total -> "PAID"
        else -> "PARTIALLY_PAID"
    }
    android.util.Log.d("PaymentStateDebug", "Order #$orderId - Inferred: $inferred")
    return inferred
}

/**
 * (#76) Gets the formatted payment state from an Order.
 * Combines getPaymentStateFromOrder() and formatPaymentState() for convenience.
 * Use this when you need the display text directly.
 * 
 * Returns formatted payment state for ALL states including OPEN (shown as UNPAID).
 */
fun getFormattedPaymentState(order: Order?): String {
    val paymentState = getPaymentStateFromOrder(order)
    return formatPaymentState(paymentState)
}

// formatOrderState and formatOrderStateTitleCase REMOVED
// Order status (order.state) is no longer used - only payment status is shown

/**
 * (#76) Formats Clover payment state to title case (only first letter capitalized).
 * Used specifically for Settings filters tab display.
 */
fun formatPaymentStateTitleCase(state: String?): String {
    if (state.isNullOrEmpty()) return ""
    
    return when (state.uppercase()) {
        "OPEN" -> "Open"
        "PAID" -> "Paid"
        "PARTIALLY_PAID" -> "Partially paid"
        "PARTIALLY_REFUNDED" -> "Partially refunded"
        "REFUNDED" -> "Refunded"
        "CREDITED" -> "Credited"
        else -> state.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}

// formatOrderStateTitleCase REMOVED - order status no longer used

fun Context.getThePaymentState(order: Order?): String {
    // Use the single source of truth function for payment state
    return getFormattedPaymentState(order)
}

/**
 * SHARED PILL RENDERING FUNCTIONS
 * Use these everywhere Clover default pills need to render:
 * - Order List (OrderCardRedesignAdapter)
 * - Order Details (OrderDetailFragment)
 * - Calendar Event Preview (EventPreviewDialog)
 */

/**
 * Setup payment status pill on a TextView.
 * Shows: Open, Paid, Partially Paid, Refunded, etc.
 * Hides pill if no payment state available.
 */
fun setupPaymentStatusPill(textView: TextView, order: Order?) {
    val paymentState = getPaymentStateFromOrder(order)
    val displayText = formatPaymentState(paymentState)
    
    val orderId = order?.id?.takeLast(8) ?: "null"
    android.util.Log.d("PillDebug", "setupPaymentStatusPill: Order #$orderId - paymentState=$paymentState, displayText='$displayText'")
    
    if (displayText.isEmpty()) {
        android.util.Log.d("PillDebug", "setupPaymentStatusPill: Order #$orderId - HIDING pill (empty displayText)")
        textView.visibility = View.GONE
        return
    }
    
    val density = textView.context.resources.displayMetrics.density
    textView.text = displayText
    textView.background = WidgetColorUtils.createPillBackground(
        WidgetColorUtils.COLOR_PAYMENT_STATUS, 20f, density
    )
    textView.setTextColor(WidgetColorUtils.COLOR_PAYMENT_STATUS)
    textView.visibility = View.VISIBLE
    android.util.Log.d("PillDebug", "setupPaymentStatusPill: Order #$orderId - SHOWING pill with text='$displayText'")
}

/**
 * Setup payment status pill from a payment state string (for EventPreviewDialog).
 */
fun setupPaymentStatusPillFromState(textView: TextView, paymentState: String?) {
    val displayText = formatPaymentState(paymentState)
    
    if (displayText.isEmpty()) {
        textView.visibility = View.GONE
        return
    }
    
    val density = textView.context.resources.displayMetrics.density
    textView.text = displayText
    textView.background = WidgetColorUtils.createPillBackground(
        WidgetColorUtils.COLOR_PAYMENT_STATUS, 20f, density
    )
    textView.setTextColor(WidgetColorUtils.COLOR_PAYMENT_STATUS)
    textView.visibility = View.VISIBLE
}

/**
 * Setup payment type pill on a TextView.
 * Shows: Cash, Card, or tender label.
 * Hides pill if no payments.
 */
fun setupPaymentTypePill(textView: TextView, order: Order?) {
    val payments = order?.payments
    
    if (payments.isNullOrEmpty()) {
        textView.visibility = View.GONE
        return
    }
    
    val tenderLabel = payments.firstOrNull()?.tender?.label?.lowercase() ?: ""
    val displayLabel = when {
        tenderLabel.contains("cash") -> "CASH"
        tenderLabel.contains("card") || tenderLabel.contains("credit") || tenderLabel.contains("debit") -> "CARD"
        else -> payments.firstOrNull()?.tender?.label?.uppercase() ?: ""
    }
    
    if (displayLabel.isEmpty()) {
        textView.visibility = View.GONE
        return
    }
    
    val density = textView.context.resources.displayMetrics.density
    textView.text = displayLabel
    textView.background = WidgetColorUtils.createPillBackground(
        WidgetColorUtils.COLOR_PAYMENT_TYPE, 20f, density
    )
    textView.setTextColor(WidgetColorUtils.COLOR_PAYMENT_TYPE)
    textView.visibility = View.VISIBLE
}

/**
 * Get payment type display label from order.
 * Returns: "Cash", "Card", or tender label.
 */
fun getPaymentTypeLabel(order: Order?): String? {
    val payments = order?.payments
    if (payments.isNullOrEmpty()) return null
    
    val tenderLabel = payments.firstOrNull()?.tender?.label?.lowercase() ?: ""
    return when {
        tenderLabel.contains("cash") -> "Cash"
        tenderLabel.contains("card") || tenderLabel.contains("credit") || tenderLabel.contains("debit") -> "Card"
        else -> payments.firstOrNull()?.tender?.label
    }
}

/**
 * Create a payment status pill TextView (for dynamic containers like FlexboxLayout).
 * Returns null if no payment state.
 */
fun createPaymentStatusPillView(context: Context, paymentState: String?): TextView? {
    val displayText = formatPaymentState(paymentState)
    if (displayText.isEmpty()) return null
    
    val density = context.resources.displayMetrics.density
    return TextView(context).apply {
        text = displayText
        setTextColor(WidgetColorUtils.COLOR_PAYMENT_STATUS)
        textSize = 10f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(
            (10 * density).toInt(),
            (4 * density).toInt(),
            (10 * density).toInt(),
            (4 * density).toInt()
        )
        background = WidgetColorUtils.createPillBackground(WidgetColorUtils.COLOR_PAYMENT_STATUS, 10f, density)
    }
}

/**
 * Create a payment type pill TextView (for dynamic containers like FlexboxLayout).
 * Returns null if no payments.
 */
fun createPaymentTypePillView(context: Context, order: Order?): TextView? {
    val displayLabel = getPaymentTypeLabel(order)?.uppercase()
    if (displayLabel.isNullOrEmpty()) return null
    
    val density = context.resources.displayMetrics.density
    return TextView(context).apply {
        text = displayLabel
        setTextColor(WidgetColorUtils.COLOR_PAYMENT_TYPE)
        textSize = 10f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(
            (10 * density).toInt(),
            (4 * density).toInt(),
            (10 * density).toInt(),
            (4 * density).toInt()
        )
        background = WidgetColorUtils.createPillBackground(WidgetColorUtils.COLOR_PAYMENT_TYPE, 10f, density)
    }
}


fun isHeading(item: String): Boolean {
    return (item.equals(Constants.fbStatus, true)
            || item.equals(Constants.fbType, true)
            || item.equals(Constants.fbSubcategory, true)
            || item.equals(Constants.fbCategory, true))
}

fun createAndShowDialog(
    context: Context, message: String, title: String, positiveButtonText: String,
    negativeButtonText: String, positiveButtonTask: () -> Unit
) {
    try{
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setIcon(R.drawable.ic_order_big)
        alertDialog.setTitle(title)
        alertDialog.setPositiveButton(
            positiveButtonText
        ) { dialog, _ -> positiveButtonTask(); dialog?.dismiss() }
        alertDialog.setNegativeButton(negativeButtonText) { dialog, _ ->
            dialog?.dismiss()
        }
        alertDialog.setMessage(message)
        alertDialog.show()
    }catch (e : Exception){
        e.printStackTrace()
    }

}

// Holder for pickup date during parsing
private var resultantPickupDate = ""

//isTextRequired if true means from the order history screen
// else false means order detail screen
fun generateString(
    lineItems: List<LineItem>,
    context: Context,
    isTextRequired: Boolean = false
): String {
    var hashmap: HashMap<String, Int> = hashMapOf()
    var pickUpDate = ""
    resultantPickupDate = ""


    lineItems.forEach {
        if (it.note?.isNotEmpty() == true) {
            val resultant = addTheDataToDialog(hashmap, it.note)
            hashmap = resultant.first
            pickUpDate = resultant.second

        }
    }
    if (pickUpDate.trim().isNotEmpty()) {
        if (isTextRequired) {
            pickUpDate = "next pickup:$pickUpDate"
        }
        pickUpDate += context.getString(R.string.bullet_symbol) + " "
    }

    hashmap.forEach {
        pickUpDate += if (isTextRequired) {
            "${it.key.trim()}:${it.value} ${context.getString(R.string.bullet_symbol)} "
        } else {
            "${it.key}${context.getString(R.string.bullet_symbol)} "
        }
    }
    if (pickUpDate.length > 3) {
        pickUpDate = pickUpDate.substring(0, pickUpDate.length - 2)
    }
    return pickUpDate
}

fun isAllFieldDisabled(preferenceManager: PreferenceManager, it1: CustomItemJson) {
    it1.types.forEach {

        if (it.isActive && !((it.name.equals(Constants.isCustomModalShown , true)) ||
            (it.name.equals(Constants.isCustomModalBasket , true)))
            ) {
            preferenceManager.saveBoolean(Constants.isAllFieldDisabled, false)
            return
        }
    }
    preferenceManager.saveBoolean(Constants.isAllFieldDisabled, true)
}

fun isCustomOptionEnabled(preferenceManager: PreferenceManager, it1: CustomItemJson) {
    it1.types.forEach {
        if (it.name.equals(Constants.isCustomModalShown , true)) {
            preferenceManager.saveBoolean(Constants.isMenuOptionEnabled, it.isActive)
        }
        else if (it.name.equals(Constants.isCustomModalBasket , true)) {
            preferenceManager.saveBoolean(Constants.isMenuBasketOptionEnabled, it.isActive)
        }
    }
}


fun getCustomerName(context : Context? , order : Order? , view : MaterialTextView?){
    if (order?.customers?.isNotEmpty() == true) {
        context?.getString(
            R.string.getFullName,
            order.customers?.get(0)?.firstName?.trim() ?: Constants.defaultString,
            order.customers?.get(0)?.lastName?.trim() ?: Constants.defaultString
        ).also {
           view?.text =  context?.getString(R.string.customer_name_value , it)
            view?.showView()
        }
    }
    else{
        view?.hideView()
    }
}

/*
       * make provide the list of order line items with unique ness eg if a item is added 10 times
       * then it will provide one item in list with item count 10
       *
       * */
fun countElementsByUniqueKeys(
    context: Context?,
    lineItems: List<LineItem?>
): MutableList<ItemModal> {
    val elementCounts: MutableSet<ItemModal> = mutableSetOf()
    var isMatched: Boolean
    for (lineItem in lineItems) {
        isMatched = false

        val key = getTheUniqueOrderItemKey(context, lineItem)
        val data = key?.let { ItemModal(lineItem, it) }

        for (it in elementCounts) {
            if (it.orderKey == key) {
                it.itemCount = it.itemCount + 1
                it.lineItemDifferentId.add(lineItem?.id)
                isMatched = true
                break
            }
        }
        if (!isMatched) {
            data?.itemCount = 1
            data?.lineItemDifferentId?.add(lineItem?.id)
            if (data != null) {
                elementCounts.add(data)
            }
        }
    }
    return elementCounts.toMutableList()
}

/*
* Clover provide us the data in the raw format
* eg if i have added 10 ice-cream then it will provide us 10 ice cream objects
* so we are clubbing the same items together and result in the item listing as per unique items
* this function generated key will help in distinguishing items
* */
private fun getTheUniqueOrderItemKey(context: Context?, lineItems: LineItem?): String? {
    var modificationString = ""
    if (lineItems?.hasModifications() == true) {
        lineItems.modifications?.forEach {
            modificationString += " ${it.name}"
        }
    }
    return context?.getString(
        R.string.key,
        lineItems?.name.toString(),
        lineItems?.price.toString(),
        lineItems?.unitName,
        lineItems?.unitQty.toString(),
        lineItems?.note,
        modificationString
    )
}

private fun addTheDataToDialog(
    hashMap: HashMap<String, Int>,
    note: String
): Pair<HashMap<String, Int>, String> {
    val array = note.split("•")
    array.forEach {
        val splitItem = it.split(":")
        if (splitItem.size >= 2) {
            val isPickup = splitItem[0].trim().equals(Constants.pickUp, true)
            if (!isPickup && !splitItem[0].trim().equals(Constants.description, true)) {
                hashMap[splitItem[1]] = doesHashMapHasValue(hashMap, splitItem[1])
            } else {
                if (isPickup) {
                    resultantPickupDate =
                        compareDates(resultantPickupDate, splitItem[1])
                            ?: resultantPickupDate
                }
            }
        }
    }

    return Pair(hashMap, resultantPickupDate)
}

private fun compareDates(dateString1: String, dateString2: String): String? {
    val dateFormat = SimpleDateFormat("M/d/yy", Locale.getDefault())

    if (dateString1.trim().isEmpty()) {
        return dateString2
    }


    return try {
        val date1 = dateFormat.parse(dateString1)?.time ?: 0L
        val date2 = dateFormat.parse(dateString2)?.time ?: 0L
        val currentDate = System.currentTimeMillis()
        val date1Difference = kotlin.math.abs(currentDate - date1)
        val date2Difference = kotlin.math.abs(currentDate - date2)
        if (date1Difference > date2Difference) {
            dateString2
        } else dateString1
    } catch (e: Exception) {
        dateString1 // if parsing fails, return 0 to indicate equality
    }
}

private fun doesHashMapHasValue(hashMap: HashMap<String, Int>, key: String): Int {
    return if (hashMap.containsKey(key)) {
        val value = hashMap[key]
        value?.plus(1) ?: 1
    } else 1
}
