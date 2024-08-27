package com.orderMate.communicators

interface IOrderItemClickListener {
    fun onOrderItemClick(orderPosition : Int , lineItemId : String? )
}