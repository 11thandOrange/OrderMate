package com.orderMate.modals

/**
 * Bird Conversations API Response Models
 * Used to fetch sent notification history for orders
 */

// Response for GET /workspaces/{workspaceId}/conversations
data class ConversationsResponse(
    val results: List<ConversationItem>?,
    val nextPageToken: String?,
    val total: Int?
)

data class ConversationItem(
    val id: String?,
    val name: String?,
    val status: String?,
    val channelId: String?,
    val featuredParticipants: List<Participant>?,
    val lastMessage: LastMessage?,
    val createdAt: String?,
    val updatedAt: String?,
    val resource: ConversationResource?
)

data class ConversationResource(
    val type: String?,
    val id: String?
)

data class Participant(
    val id: String?,
    val type: String?,
    val displayName: String?,
    val contact: ParticipantContact?
)

data class ParticipantContact(
    val identifierKey: String?,
    val identifierValue: String?,
    val platformAddress: String?
)

data class LastMessage(
    val id: String?,
    val type: String?,
    val preview: MessagePreview?,
    val status: String?,
    val createdAt: String?
)

data class MessagePreview(
    val text: String?
)

// Response for GET /workspaces/{workspaceId}/conversations/{conversationId}/messages
data class MessagesResponse(
    val results: List<MessageItem>?,
    val nextPageToken: String?
)

data class MessageItem(
    val id: String?,
    val conversationId: String?,
    val reference: String?,
    val status: String?,
    val source: String?,
    val body: MessageBody?,
    val recipients: List<Recipient>?,
    val sender: MessageSender?,
    val createdAt: String?,
    val updatedAt: String?,
    val meta: MessageItemMeta?
)

data class MessageBody(
    val type: String?,
    val text: MessageText?,
    val html: MessageHtml?
)

data class MessageText(
    val text: String?
)

data class MessageHtml(
    val text: String?,
    val html: String?
)

data class Recipient(
    val type: String?,
    val identifierKey: String?,
    val identifierValue: String?
)

data class MessageSender(
    val id: String?,
    val type: String?,
    val displayName: String?
)

data class MessageItemMeta(
    val extraInformation: Map<String, String>?
)

// Contact search request/response
data class ContactSearchRequest(
    val identifier: ContactIdentifier
)

data class ContactIdentifier(
    val key: String,
    val value: String
)

data class ContactSearchResponse(
    val results: List<ContactResult>?
)

data class ContactResult(
    val id: String?,
    val computedDisplayName: String?,
    val featuredIdentifiers: List<ContactIdentifier>?
)
