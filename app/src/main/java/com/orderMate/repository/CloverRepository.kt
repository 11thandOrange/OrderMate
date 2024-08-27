package com.orderMate.repository

import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal
import com.orderMate.networkManager.RetrofitInstanceWithAuth
import com.orderMate.utils.Constants


/*
* This Repository Deals with the Bird Messaging Api Call.
*
* */
class CloverRepository private constructor() {

    companion object {
        // instance holder
        private var instance: CloverRepository? = null

        /*
        * make this class instance a singleton
        * if the instance of the class is available then that instance is provided
        * if the instance is not created the create the new instance.
        * */
        fun getInstance(): CloverRepository {
            return instance ?: synchronized(this) {
                CloverRepository().apply {
                    instance = this
                }
            }


        }


        private val apiWithAuth = RetrofitInstanceWithAuth.getApiService()

    }

    suspend fun sendEmail(data: ShareMessageJson) =
        apiWithAuth.shareEmail(Constants.workSpaceId, Constants.channelId, data)

    suspend fun sendSms(data: ShareSmsModal) =
        apiWithAuth.shareSms(Constants.workSpaceId, Constants.SMSChannelId, data)

}