package com.order.orderappclover.modals.orderDetail


data class OrderPaymentRequest(
    val merchantId : String ,
    val orderId : String,
    var expand: String
)

data class OrderPaymentResponse(
    val elements: List<PaymentElement?>?,
    val href: String?
)

data class Order(
    val id: String?
)



