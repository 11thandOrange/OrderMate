package com.specialOrder.communicators

import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal

interface IShareEmailOrMessage {
    fun sendEmail(data : ShareMessageJson)
    fun shareSms(data : ShareSmsModal)
}