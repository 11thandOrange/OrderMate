package com.specialOrder.communicators

import com.clover.sdk.v3.order.Order

interface InterCommunication {
    fun communicate(orderItems : ArrayList<Order?>)
}