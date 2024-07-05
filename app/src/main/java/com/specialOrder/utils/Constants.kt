package com.specialOrder.utils

class Constants {

    companion object {
        const val channelId : String = "995069a7-d9b3-4141-91ee-e080b7432710"
        const val SMSChannelId : String = "d1230582-78ec-4a82-bd44-403af8f7749a"
        const val accessKey : String = "IEJiQWtx1fT5qhF5BMget0VreTaPUSIUXi7h"
        const val workSpaceId : String ="d4c8ca3d-4d0e-4263-b07e-afbe5906fa0a"
        const val LINE_ITEM_ADDED_ORDER_DETAILS = "LINE_ITEM_ADDED_ORDER_DETAILS"
        const val LINE_ITEM_ADDED_ID = "LINE_ITEM_ADDED_ID"

        val numberRegex: Regex = "^\\d{7,15}\$".toRegex()

        const val accept = "Accept"
        const val format = "application/json"
        const val overlayIntentExtraOrder = "orderData"
        const val overlayIntentExtraLineItemId = "lineItemId"
        const val overlayIntentExtraLinePosition = "orderPosition"
        // used in the messagingBird api for sharing the email and phone identifier key
        const val emailAddress : String ="emailaddress"
        const val phoneNumber  : String ="phonenumber"
        const val noInternet  : String ="noInternet"
        const val NA: String = "NA"
        const val html : String = "html"
        const val text : String = "text"
        const val pickUp: String = "pickup date"
        const val category: String = "category"
        const val progress: String = "status"
        const val type: String = "type"
        const val description: String = "description"


// fb = firebase
        const val fbPickUp: String = "Pick Up Date"
        const val fbCategory: String = "Category"
        const val fbSubcategory: String = "Sub-Category"
        const val fbStatus: String = "Status"
        const val fbType: String = "Type"
        const val fbDescription: String = "Description"
        const val isCustomModalShown ="Trigger modal when item is added to cart"
        const val isCustomModalBasket ="Trigger modal from basket"
        const val isAllFieldDisabled: String = "isAllFieldDisabled"


        const val subcategory: String = "subcategory"
        const val customData: String = "customData"
        const val data: String = "data"
        const val debouncingTime: Long = 300
        const val BASE_URL = "https://api.bird.com/"
        const val TIME_OUT_DURATION = 1L
        const val yearFormat = "MM/dd/yy"
        const val yearFormatWithMonthName = "MMMM d, yyyy"
        const val dateFormat = "hh:mm a"
        const val defaultString = ""
        const val customMenuJson = "customMenuJson"
        const val defaultInt = 1
        const val defaultLong = 0L
        const val defaultOffset: Int = 0
        const val defaultBoolean = false
        const val alwaysTrue = true
        const val isMenuOptionEnabled = "registerMenu"
        const val isOrderSaved = "isOrderSaved"
        const val isTrue = "True"
        const val isFalse = "False"
        const val isMenuBasketOptionEnabled = "registerBasketMenu"
        const val authorization: String = "Authorization"
        const val bearer: String = "AccessKey"
        const val sharedPreferenceName = "sharedPreference"
        const val merchantName: String = "Merchant_Name"
        // payment status
        const val OPEN = "OPEN"
        const val PARTIALLY_PAID = "PARTIALLY_PAID"
        const val PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED"
        const val PAID = "PAID"
        const val REFUNDED = "REFUNDED"
        const val ADMIN = "ADMIN"
        const val cash = "Cash"
        const val check = "check"


        // clover order api keys name
        const val role = "role"
        const val nullValue = "null"
        const val online = "Online"
        const val pos = "POS"
        const val name = "name"
        const val all_orders = "All Orders"
        const val all_tenders = "All Tenders"
        const val all_employee = "All Employees"
        const val all_booking_type = "All Types"
        const val notes_max_length = 255
        const val item_max_length = 20
        val notImplementedLog  = "Need to Implemented later on".debugLog(javaClass.simpleName)
    }
}