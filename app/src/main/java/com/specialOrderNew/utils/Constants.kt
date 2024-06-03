package com.specialOrderNew.utils

class Constants {

    companion object {
        const val BASE_URL = "https://sandbox.dev.clover.com/"
        const val TIME_OUT_DURATION = 1L
        const val yearFormat = "MM/dd/yy"
        const val yearFormatWithMonthName = "MMMM d, yyyy"
        const val dateFormat = "hh:mm a"
        const val defaultString = "DefaultValue"
        const val defaultDouble : Double = 0.00
        const val defaultInt = Int.MAX_VALUE
        const val defaultLong = Long.MAX_VALUE
        const val defaultOffset: Int = 0
        const val defaultBoolean = false
        const val authorization: String = "Authorization"
        const val bearer: String = "Bearer"
        const val defaultLimitForPagination: Int = 30
        const val sharedPreferenceName = "sharedPreference"
        const val accessToken: String = "AccessToken"
        const val merchantName: String = "Merchant_Name"
        const val code = "code="
        const val clientCode = "code"
        const val merchant_id = "merchant_id"
        const val employee_id = "employee_id"
        const val client_id = "client_id"
        private const val codeBaseUrl = "https://sandbox.dev.clover.com/oauth/authorize"
//        const val clientSecret = "15069295-8f38-8b7a-5067-a7f0270e6ad2" // own
        const val clientSecret = "dafbc09e-5b2d-187d-d249-318b57ca6512" // personal
//         const val clientSecret = "7ef6ca0b-aefb-2e6O-b756-5ec49f5c97bd" // client

        // this is the app id of the sandbox
//        private const val sandBoxClientId = "M4PQA4CB3K1RG" // client
//        private const val sandBoxClientId = "9N7657JPMBCFC"  // own
        private const val sandBoxClientId = "4S2WB80HNCG68"  // personal

        // this url needs to be same as you have used in the sandbox dashboard
        const val redirectUri = "https://localhost:8000/" // Define a redirect URI for your app

        //        const val redirectUri = "https://sandbox.dev.clover.com/developer-home/" // Define a redirect URI for your app
        const val oauthUrl =
            "${codeBaseUrl}?client_id=${sandBoxClientId}&redirect_uri=${redirectUri}&response_type=code"


        // payment status
        const val OPEN = "OPEN"
        const val PARTIALLY_PAID = "PARTIALLY_PAID"
        const val PAID = "PAID"
        const val REFUNDED  = "REFUNDED"
        const val Unauthorized = "Unauthorized"

        // expand filter list clover

        const val lineItems = "lineItems"
        const val PAYMENT ="payments"
        const val tender = "tender"
        const val refund  = "refunds"
        const val cash = "Cash"
        const val check ="check"
        const val payment = "payment.tender"
    }
}