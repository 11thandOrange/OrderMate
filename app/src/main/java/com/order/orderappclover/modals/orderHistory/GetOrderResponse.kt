package com.order.orderappclover.modals.orderHistory

import com.order.orderappclover.modals.orderDetail.OrderPayments
import com.order.orderappclover.modals.orderDetail.PaymentElement

data class GetOrderResponse(
    val elements: List<OrderItems>?,
    val href: String?
)

data class OrderItems(
    val clientCreatedTime: Long?,
    val createdTime: Long?,
    val currency: String?,
    val device: Device?,
    val employee: Employee?,
    val groupLineItems: Boolean?,
    val href: String?,
    val id: String?,
    val isVat: Boolean?,
    val manualTransaction: Boolean?,
    val modifiedTime: Long?,
    val payType: String?,
    var paymentState: String?,
    val payments: OrderPayments?,
    val state: String?,
    val taxRemoved: Boolean?,
    val testMode: Boolean?,
    val total: Int? = 0
)

data class Device(
    val id: String? = ""
)

data class Employee(
    val id: String? = ""
)

data class Payments(
    val elements: List<PaymentElement>?
)

