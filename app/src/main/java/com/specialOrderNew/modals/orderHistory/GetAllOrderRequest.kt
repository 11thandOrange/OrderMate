package com.specialOrderNew.modals.orderHistory

import com.specialOrderNew.utils.Constants

data class GetAllOrderRequest(
    val merchantId : String,
    val expand : String ,
    var limit : Int = Constants.defaultLimitForPagination, // limit of the api we need to hit. used for the pagination ,
    var offset : Int? = null // from where do we want the order that has to be returned eg if offset is 11 then order returned
    // from 11 + limit
)
