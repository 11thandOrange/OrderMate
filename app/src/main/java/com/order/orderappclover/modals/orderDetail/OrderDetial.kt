package com.order.orderappclover.modals.orderDetail

data class OrderDetailRequest(
    val mid: String,
    val orderId: String,
    var expand: String
)

data class OrderDiscountRequest(
    val mid : String,
    val orderId : String
)

data class OrderDiscountResponse(
    val elements: List<Discount?>?,
    val href: String?
)

data class Discount(
    val id: String? ,
    val name: String? ,
    val orderRef: OrderRef? ,
    val percentage: Int?
)



data class OrderDetailResponse(
    val clientCreatedTime: Long?,
    val createdTime: Long?,
    val currency: String?,
    val device: Device?,
    val employee: Employee?,
    val groupLineItems: Boolean?,
    val href: String?,
    val id: String?,
    val isVat: Boolean?,
    val lineItems: LineItems?,
    val manualTransaction: Boolean?,
    val modifiedTime: Long?,
    val paymentState: String?,
    val state: String?,
    val taxRemoved: Boolean?,
    val testMode: Boolean?,
    val total: Int?
)

data class Device(
    val id: String?
)

data class Employee(
    val id: String?
)

data class LineItems(
    val elements: List<Element?>?
)

data class Element(
    val colorCode: String?,
    val createdTime: Long?,
    val exchanged: Boolean?,
    val id: String?,
    val isOrderFee: Boolean?,
    val isRevenue: Boolean?,
    val item: Item?,
    val name: String?,
    val orderClientCreatedTime: Long?,
    val orderRef: OrderRef?,
    val price: Int?,
    val printed: Boolean?,
    val refunded: Boolean?,
    var itemCount : Int?
)

data class Item(
    val id: String?
)

data class OrderRef(
    val id: String?
)