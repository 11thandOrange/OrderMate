package com.specialOrder.communicators

interface IOrderItemClickListener {
    fun onOrderItemClick(orderPosition : Int , lineItemId : String? )
}