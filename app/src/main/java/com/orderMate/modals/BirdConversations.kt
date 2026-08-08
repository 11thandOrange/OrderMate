package com.orderMate.modals

/**
 * Bird Conversations API Response Models
 * Used to fetch sent notification history for orders
 */

// Response for POST /workspaces/{workspaceId}/conversations (Create Conversation).
// Bird returns the created conversation, including its `id` - that id is what
// OrderMate persists (keyed by order id) so notification history can be looked
// back up later (see CloverRepository.getNotificationsForOrder).
data class ConversationItem(
    val id: String?,
    val name: String?,
    val status: String?,
    val channelId: String?,
    val featuredParticipants: List<Participant>?,
    val lastMessage: LastMessage?,
    val createdAt: String?,
    val updatedAt: String?
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

// Request for POST /workspaces/{workspaceId}/conversations (Create Conversation).
// Sending through this endpoint - instead of the old Channels API
// /workspaces/{id}/channels/{id}/messages send - returns a conversation `id` that
// OrderMate persists itself (keyed by order id, see CloverRepository) so notification
// history can be looked up later. `name` is required by Bird; there is no `resource`
// field here because Bird's `resource.type` enum has no value for an order and
// `resource.id` must be a UUID, which Clover order ids never are (confirmed via a
// live 422 InvalidPayload response).
data class CreateEmailConversationRequest(
    val channelId: String,
    val name: String,
    val participants: List<Recipient>,
    val initialMessage: InitialEmailMessageRequest
)

data class CreateSmsConversationRequest(
    val channelId: String,
    val name: String,
    val participants: List<Recipient>,
    val initialMessage: InitialSmsMessageRequest
)

data class InitialEmailMessageRequest(
    val body: Body,
    val recipients: List<Recipient>,
    val reference: String? = null
)

data class InitialSmsMessageRequest(
    val body: SmsBody,
    val recipients: List<Recipient>,
    val reference: String? = null
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
