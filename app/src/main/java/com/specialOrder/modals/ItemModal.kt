package com.specialOrder.modals

import com.clover.sdk.v3.order.LineItem

data class ItemModal(
    val order: LineItem?,
    val orderKey : String,
    var itemCount: Int = 0
)