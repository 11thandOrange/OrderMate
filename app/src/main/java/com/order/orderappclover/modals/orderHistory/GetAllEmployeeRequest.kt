package com.order.orderappclover.modals.orderHistory

data class GetAllEmployeeRequest(
    var merchantId : String
)
data class GetAllEmployeeListResponse(
    val elements: List<Element?>? = listOf(),
    val href: String? = ""
)

data class Element(
    val claimedTime: Long? = 0,
    val email: String? = "",
    val href: String? = "",
    val id: String? = "",
    val inviteSent: Boolean? = false,
    val isOwner: Boolean? = false,
    val name: String? = "",
    val orders: Orders? = Orders(),
    val pin: String? = "",
    val role: String? = ""
)

data class Orders(
    val href: String? = ""
)