package com.specialOrderNew.networkManager

import com.specialOrderNew.modals.orderDetail.GetOrderLineItems
import com.specialOrderNew.modals.orderDetail.OrderDetailResponse
import com.specialOrderNew.modals.orderDetail.OrderDiscountResponse
import com.specialOrderNew.modals.orderDetail.OrderPaymentResponse
import com.specialOrderNew.modals.orderDetail.OrderRefundResponse
import com.specialOrderNew.modals.orderHistory.AccessTokenRequest
import com.specialOrderNew.modals.orderHistory.AccessTokenResponse
import com.specialOrderNew.modals.orderHistory.GetAllEmployeeListResponse
import com.specialOrderNew.modals.orderHistory.GetOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiCall {

    /*
    * Api to get the access token from the oauth process.
    * */

    @POST("/oauth/token")
    suspend fun getAccessToken(
        @Body request: AccessTokenRequest
    ): Response<AccessTokenResponse>

    @GET("/v3/merchants/{mid}/orders")
    suspend fun getAllOrders(
        @Path("mid") merchantId: String,
        @Query("expand") expand: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GetOrderResponse>

    @GET("/v3/merchants/{mid}/orders/{orderId}")
    suspend fun getOrderDetails(
        @Path("mid") merchantId: String,
        @Path("orderId") orderId: String,
        @Query("expand") expand: String
    ) : Response<OrderDetailResponse>

    @GET("v3/merchants/{mid}/orders/{orderId}/discounts")
    suspend fun getOrderDiscountDetails(
        @Path("mid") merchantId: String,
        @Path("orderId") orderId: String,
    ) : Response<OrderDiscountResponse>


    @GET("v3/merchants/{mid}/orders/{orderId}/payments")
    suspend fun getOrderPayments(
        @Path("mid") merchantId: String,
        @Path("orderId") orderId: String,
        @Query("expand") expand: String
    ) : Response<OrderPaymentResponse>


    @GET("v3/merchants/{mid}/orders/{orderId}/payments")
    suspend fun getOrderPaymentsRefund(
        @Path("mid") merchantId: String,
        @Path("orderId") orderId: String,
        @Query("expand") expand: String
    ) : Response<OrderRefundResponse>


    @GET("v3/merchants/{mid}/employees")
    suspend fun getEmployeeDetails(
        @Path("mid") merchantId: String,

    ) : Response<GetAllEmployeeListResponse>


    @GET("v3/merchants/{mId}/orders/{orderId}/line_items")
    suspend fun getOrderLineItems(
        @Path("mId") merchantId: String,
        @Path("orderId") orderId: String,
        @Query("expand") expand: String
        ) : Response<GetOrderLineItems>


}