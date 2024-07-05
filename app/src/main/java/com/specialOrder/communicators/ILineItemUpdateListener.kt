package com.specialOrder.communicators

interface ILineItemUpdateListener {
    fun updateLineItem(id : String? , list : String? , position :Int)
    fun dismissDialog()
}