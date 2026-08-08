package com.orderMate.communicators

import com.orderMate.modals.CreateEmailConversationRequest
import com.orderMate.modals.CreateSmsConversationRequest

interface IShareEmailOrMessage {
    fun sendEmail(data : CreateEmailConversationRequest)
    fun shareSms(data : CreateSmsConversationRequest)
}
