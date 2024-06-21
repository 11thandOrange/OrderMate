package com.specialOrder.communicators

import com.specialOrder.modals.ShareMessageJson
import com.specialOrder.modals.ShareSmsModal

interface IShareEmailOrMessage {
    fun sendEmail(data : ShareMessageJson)
    fun shareSms(data : ShareSmsModal)
}