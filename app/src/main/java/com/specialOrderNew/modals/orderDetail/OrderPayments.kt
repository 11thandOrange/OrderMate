package com.specialOrderNew.modals.orderDetail


data class OrderPayments(
    val elements: List<OrderPaymentElement?>?
)

data class OrderPaymentElement(
    val amount: Int? ,
    val cashTendered: Int? ,
    val clientCreatedTime: Long?,
    val createdTime: Long? ,
    val device: Device? ,
    val employee: Employee? ,
    val id: String? ,
    val modifiedTime: Long?,
    val note: String? ,
    val order: Order? ,
    val result: String?,
    val taxAmount: Int? ,
    val tender: TenderPayment?
)



data class TenderPayment(
    val editable: Boolean?,
    val enabled: Boolean? ,
    val href: String? ,
    val id: String? ,
    val label: String? ,
    val labelKey: String?,
    val opensCashDrawer: Boolean? ,
    val visible: Boolean?
)