package com.orderMate.communicators

interface ILineItemUpdateListener {
    fun updateLineItem(id : String? , list : String? , position :Int)
    fun dismissDialog()
}