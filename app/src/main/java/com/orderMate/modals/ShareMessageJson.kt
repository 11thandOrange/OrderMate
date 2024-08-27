package com.orderMate.modals


data class ShareMessageJson(
    val body: Body?,
    val `receiver`: Receiver?
)

data class Body(
    val html: Html?,
    val type: String? = ""
)


data class Html(
    val html: String? = "html",
    val metadata: Metadata?,
    val text: String? = ""
)

data class Metadata(
    val subject: String? = ""
)


data class ShareSmsModal(
    val body: SmsBody?,
    val `receiver`: Receiver? = Receiver()
)

data class SmsBody(
    val text: Text?,
    val type: String? = ""
)

data class Receiver(
    val contacts: List<Contact?>? = listOf()
)

data class Text(
    val text: String? = ""
)

data class Contact(
    val identifierKey: String?,
    val identifierValue: String?
)

data class ShareResponse(
    val body: ShareBody? ,
    val channelId: String? = "",
    val createdAt: String? = "",
    val direction: String? = "",
    val id: String? = "",
    val lastStatusAt: String? ,
    val parts: List<Any?>? ,
    val reason: String? = "",
    val `receiver`: Receiver? ,
    val reference: String? = "",
    val sender: Sender? ,
    val status: String? = "",
    val updatedAt: String? = ""
)

data class ShareBody(
    val html: ShareHtml? ,
    val type: String? = ""
)

data class Sender(
    val connector: Connector? = Connector()
)

data class ShareHtml(
    val metadata: Metadata? = Metadata(),
    val text: String? = ""
)


data class Connector(
    val id: String? = "",
    val identifierValue: String? = ""
)

