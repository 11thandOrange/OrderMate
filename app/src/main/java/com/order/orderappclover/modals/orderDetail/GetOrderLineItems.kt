package com.order.orderappclover.modals.orderDetail

data class GetOrderLineItems(
    val elements: List<LineItemElement?>?,
    val href: String?
)

data class LineItemElement(
    val alternateName: String?,
    val colorCode: String?,
    val createdTime: Long?,
    val exchanged: Boolean?,
    val exchangedLineItem: ExchangedLineItem?,
    val id: String?,
    val isOrderFee: Boolean?,
    val isRevenue: Boolean?,
    val item: Item?,
    val itemCode: String?,
    val name: String?,
    val orderClientCreatedTime: Long?,
    val orderRef: OrderRef?,
    val price: Int?,
    val printed: Boolean?,
    val refunded: Boolean?,
    var itemCount : Int?
)

data class ExchangedLineItem(
    val id: String? = ""
)



