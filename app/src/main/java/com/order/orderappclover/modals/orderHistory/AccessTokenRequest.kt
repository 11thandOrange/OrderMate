package com.order.orderappclover.modals.orderHistory

import com.google.gson.annotations.SerializedName

data class AccessTokenRequest(
    @SerializedName("grant_type")
    val grantType: String = "authorization_code",
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_secret")
    val clientSecret: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("redirect_uri")
    val redirectUri: String
)
