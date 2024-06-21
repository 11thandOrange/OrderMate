package com.specialOrder.networkManager


import com.specialOrder.modals.ShareMessageJson
import com.specialOrder.modals.ShareSmsModal
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path


/*
* Every Bird Account have multiple workSpace.Where Each Workspace has multiple channels
* Such as Email , instagram and sms . Each channel has Unique Channel Id and you need the access
* token to share the sms , email etc that access token will remain same for all
* */
interface ApiCall {

    @POST("/workspaces/{merchantId}/channels/{channelId}/messages")
    suspend fun shareEmail(
        @Path("merchantId") merchantId: String,
        @Path("channelId") channelId: String,
        @Body request: ShareMessageJson
    ): Response<Any>


    @POST("/workspaces/{merchantId}/channels/{channelId}/messages")
    suspend fun shareSms(
        @Path("merchantId") merchantId: String,
        @Path("channelId") channelId: String,
        @Body request: ShareSmsModal
    ): Response<Any>

}