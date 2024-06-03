package com.specialOrderNew.modals.orderDetail

data class OrderRefundResponse(
    val elements: List<PaymentElement>?,
    val href: String?
)

data class PaymentElement(
    val amount: Int? ,
    val cashTendered: Int? ,
    val clientCreatedTime: Long?,
    val createdTime: Long? ,
    val device: Device? ,
    val employee: Employee? ,
    val id: String? ,
    val modifiedTime: Long? ,
    val order: Order?  ,
    val refunds: Refunds? ,
    val result: String?,
    val taxAmount: Int? ,
    val tender: Tender?,
    var isRefunded : Boolean? = false
)

data class Refunds(
    val elements: List<ElementX>? = listOf()
)

data class Tender(
    val editable: Boolean? ,
    val enabled: Boolean?,
    val href: String? ,
    val id: String? ,
    val label: String? ,
    val labelKey: String?,
    val opensCashDrawer: Boolean? ,
    val visible: Boolean?
)
data class ElementX(
    val amount: Int? ,
    val clientCreatedTime: Long? ,
    val createdTime: Long?,
    val device: Device? ,
    val id: String? = "",
    val orderRef: ElementOrderRef? ,
    val payment: Payment?,
    val status: String? = "",
    val taxAmount: Int? = 0,
    val voided: Boolean? = false
)

data class ElementOrderRef(
    val clientCreatedTime: Long?,
    val createdTime: Long? ,
    val currency: String? ,
    val device: DeviceXX? ,
    val employee: EmployeeXX? ,
    val groupLineItems: Boolean? ,
    val href: String? ,
    val id: String? ,
    val isVat: Boolean? ,
    val manualTransaction: Boolean? ,
    val modifiedTime: Long? ,
    val payType: String?,
    val state: String?,
    val taxRemoved: Boolean?,
    val testMode: Boolean?,
    val total: Int? = 0
)

data class Payment(
    val amount: Int?,
    val cashTendered: Int?,
    val cashbackAmount: Int? ,
    val clientCreatedTime: Long? ,
    val createdTime: Long? ,
    val employee: EmployeeXXX? ,
    val id: String?,
    val modifiedTime: Long? ,
    val offline: Boolean?,
    val result: String?,
    val taxAmount: Int?,
    val tender: Tender? ,
    val tipAmount: Int?
)

data class DeviceXX(
    val href: String? = "",
    val id: String? = ""
)

data class EmployeeXX(
    val href: String? = "",
    val id: String? = "",
    val orders: Orders? = Orders()
)

data class Orders(
    val href: String? = ""
)

data class EmployeeXXX(
    val href: String? = "",
    val id: String? = "",

)