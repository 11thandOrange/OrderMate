package com.specialOrderNew.repository

import com.specialOrderNew.modals.orderDetail.OrderDetailRequest
import com.specialOrderNew.modals.orderDetail.OrderDiscountRequest
import com.specialOrderNew.modals.orderDetail.OrderPaymentRequest
import com.specialOrderNew.modals.orderHistory.AccessTokenRequest
import com.specialOrderNew.modals.orderHistory.GetAllEmployeeRequest
import com.specialOrderNew.modals.orderHistory.GetAllOrderRequest
import com.specialOrderNew.networkManager.RetrofitClient
import com.specialOrderNew.networkManager.RetrofitInstanceWithAuth
import com.specialOrderNew.utils.Constants

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
        val apiWithOutAuth = RetrofitClient.getApiService()
        val apiWithAuth = RetrofitInstanceWithAuth.getApiService()


    }

    suspend fun getAccessToken(data : AccessTokenRequest) = apiWithOutAuth.getAccessToken(data)
    suspend fun getAllOrdersWithPagination(data: GetAllOrderRequest) = apiWithAuth.getAllOrders(data.merchantId , data.expand, data.limit , data.offset?: Constants.defaultOffset)
    suspend fun getOrderDetails(data : OrderDetailRequest) = apiWithAuth.getOrderDetails(data.mid, data.orderId, data.expand)
    suspend fun getOrderDiscount(data : OrderDiscountRequest) = apiWithAuth.getOrderDiscountDetails(data.mid, data.orderId)
    suspend fun getOrderPayment(data : OrderPaymentRequest) = apiWithAuth.getOrderPayments(data.merchantId, data.orderId , data.expand)
    suspend fun getOrderPaymentRefund(data : OrderPaymentRequest) = apiWithAuth.getOrderPaymentsRefund(data.merchantId, data.orderId , data.expand)
    suspend fun getEmployeeList(data : GetAllEmployeeRequest) = apiWithAuth.getEmployeeDetails(data.merchantId )
    suspend fun getAllLineItems(data:OrderPaymentRequest) = apiWithAuth.getOrderLineItems(data.merchantId, data.orderId, data.expand)
}