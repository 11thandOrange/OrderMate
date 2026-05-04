package com.orderMate.utils

import android.content.Context
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
 * - OPEN → UNPAID
 * - PAID → PAID
 * - PARTIALLY_PAID → PARTIALLY PAID
 * - PARTIALLY_REFUNDED → PARTIALLY REFUNDED
 * - REFUNDED → REFUNDED
 * - CREDITED → CREDITED
 */
fun formatPaymentState(state: String?): String {
    if (state.isNullOrEmpty()) return "UNPAID"
    
    return when (state.uppercase()) {
        "OPEN" -> "UNPAID"
        "PAID" -> "PAID"
        "PARTIALLY_PAID" -> "PARTIALLY PAID"
        "PARTIALLY_REFUNDED" -> "PARTIALLY REFUNDED"
        "REFUNDED" -> "REFUNDED"
        "CREDITED" -> "CREDITED"
        else -> state.replace("_", " ").uppercase()
    }
}

/**
 * (#76) Gets the payment state string from an Order with correct fallback.
 * Single source of truth for extracting payment state from orders.
 * 
 * Fallback logic:
 * - If paymentState is available, use it
 * - If paymentState is null but order.state is "locked" (closed), infer "PAID"
 *   (Clover requires payment to close an order)
 * - Otherwise fall back to "OPEN"
 */
fun getPaymentStateFromOrder(order: Order?): String {
    // Use paymentState if available
    order?.paymentState?.name?.let { return it }
    
    // Infer PAID from closed order state (Clover requires payment to close)
    if (order?.state == "locked") {
        return "PAID"
    }
    
    return "OPEN"
}

/**
 * (#76) Gets the formatted payment state from an Order.
 * Combines getPaymentStateFromOrder() and formatPaymentState() for convenience.
 * Use this when you need the display text directly.
 */
fun getFormattedPaymentState(order: Order?): String {
    return formatPaymentState(getPaymentStateFromOrder(order))
}

/**
 * Formats Clover order state to display text (UPPERCASE).
 * Single source of truth for order status formatting.
 * 
 * Mapping:
 * - open → OPEN
 * - locked → CLOSED
 */
fun formatOrderState(state: String?): String {
    if (state.isNullOrEmpty()) return "OPEN"
    
    return when (state.lowercase()) {
        "open" -> "OPEN"
        "locked" -> "CLOSED"
        else -> state.uppercase()
    }
}

/**
 * (#76) Formats Clover payment state to title case (only first letter capitalized).
 * Used specifically for Settings filters tab display.
 */
fun formatPaymentStateTitleCase(state: String?): String {
    if (state.isNullOrEmpty()) return "Unpaid"
    
    return when (state.uppercase()) {
        "OPEN" -> "Unpaid"
        "PAID" -> "Paid"
        "PARTIALLY_PAID" -> "Partially paid"
        "PARTIALLY_REFUNDED" -> "Partially refunded"
        "REFUNDED" -> "Refunded"
        "CREDITED" -> "Credited"
        else -> state.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * (#76) Formats Clover order state to title case (only first letter capitalized).
 * Used specifically for Settings filters tab display.
 */
fun formatOrderStateTitleCase(state: String?): String {
    if (state.isNullOrEmpty()) return "Open"
    
    return when (state.lowercase()) {
        "open" -> "Open"
        "locked" -> "Closed"
        else -> state.lowercase().replaceFirstChar { it.uppercase() }
    }
}

fun Context.getThePaymentState(order: Order?): String {
    // Use the single source of truth function for payment state
    return formatPaymentState(getPaymentStateFromOrder(order))
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
