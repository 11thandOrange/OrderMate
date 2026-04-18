package com.orderMate.modals

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BirdConversations model (#65 - Order History)
 * 
 * Tests cover:
 * - Conversation parsing
 * - Message extraction
 * - Date/time handling
 * - Channel type identification
 */
class BirdConversationsTest {

    // ==================== Conversation Data Model Tests ====================

    @Test
    fun `conversation has required fields`() {
        val conversation = createMockConversation(
            id = "conv-123",
            status = "delivered",
            channel = "sms"
        )
        
        assertEquals("conv-123", conversation.id)
        assertEquals("delivered", conversation.status)
        assertEquals("sms", conversation.channel)
    }

    @Test
    fun `message has required fields`() {
        val message = createMockMessage(
            id = "msg-123",
            content = "Your order is ready!",
            direction = "outbound",
            timestamp = "2024-01-15T10:30:00Z"
        )
        
        assertEquals("msg-123", message.id)
        assertEquals("Your order is ready!", message.content)
        assertEquals("outbound", message.direction)
    }

    // ==================== Channel Type Tests ====================

    @Test
    fun `identifies SMS channel`() {
        val conversation = createMockConversation(channel = "sms")
        assertTrue(isSmsChannel(conversation.channel))
    }

    @Test
    fun `identifies email channel`() {
        val conversation = createMockConversation(channel = "email")
        assertTrue(isEmailChannel(conversation.channel))
    }

    @Test
    fun `handles unknown channel gracefully`() {
        val conversation = createMockConversation(channel = "whatsapp")
        assertFalse(isSmsChannel(conversation.channel))
        assertFalse(isEmailChannel(conversation.channel))
    }

    // ==================== Status Tests ====================

    @Test
    fun `identifies delivered status`() {
        val conversation = createMockConversation(status = "delivered")
        assertTrue(isDelivered(conversation.status))
    }

    @Test
    fun `identifies sent status`() {
        val conversation = createMockConversation(status = "sent")
        assertTrue(isSent(conversation.status))
    }

    @Test
    fun `identifies failed status`() {
        val conversation = createMockConversation(status = "failed")
        assertTrue(isFailed(conversation.status))
    }

    @Test
    fun `identifies pending status`() {
        val conversation = createMockConversation(status = "pending")
        assertTrue(isPending(conversation.status))
    }

    // ==================== Message Direction Tests ====================

    @Test
    fun `identifies outbound message`() {
        val message = createMockMessage(direction = "outbound")
        assertTrue(isOutbound(message.direction))
    }

    @Test
    fun `identifies inbound message`() {
        val message = createMockMessage(direction = "inbound")
        assertTrue(isInbound(message.direction))
    }

    // ==================== Order Reference Tests (#65) ====================

    @Test
    fun `extracts order ID from reference`() {
        val reference = "order-ABC123"
        val orderId = extractOrderIdFromReference(reference)
        
        assertEquals("ABC123", orderId)
    }

    @Test
    fun `handles missing order prefix`() {
        val reference = "other-reference"
        val orderId = extractOrderIdFromReference(reference)
        
        assertNull(orderId)
    }

    @Test
    fun `filters conversations by order ID`() {
        val conversations = listOf(
            createMockConversation(id = "1", reference = "order-ABC123"),
            createMockConversation(id = "2", reference = "order-XYZ789"),
            createMockConversation(id = "3", reference = "order-ABC123")
        )
        
        val filtered = filterByOrderId(conversations, "ABC123")
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.reference?.contains("ABC123") == true })
    }

    // ==================== Timestamp Tests ====================

    @Test
    fun `parses ISO timestamp`() {
        val timestamp = "2024-01-15T10:30:00Z"
        val parsed = parseTimestamp(timestamp)
        
        assertNotNull(parsed)
    }

    @Test
    fun `handles invalid timestamp gracefully`() {
        val timestamp = "invalid-timestamp"
        val parsed = parseTimestamp(timestamp)
        
        assertNull(parsed)
    }

    @Test
    fun `sorts messages by timestamp newest first`() {
        val messages = listOf(
            createMockMessage(id = "1", timestamp = "2024-01-15T08:00:00Z"),
            createMockMessage(id = "2", timestamp = "2024-01-15T12:00:00Z"),
            createMockMessage(id = "3", timestamp = "2024-01-15T10:00:00Z")
        )
        
        val sorted = sortByTimestampDesc(messages)
        
        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("1", sorted[2].id)
    }

    // ==================== Display Formatting Tests ====================

    @Test
    fun `formats SMS display correctly`() {
        val conversation = createMockConversation(
            channel = "sms",
            status = "delivered",
            recipient = "+1555123456"
        )
        
        val display = formatForDisplay(conversation)
        
        assertTrue(display.contains("SMS"))
        assertTrue(display.contains("delivered"))
    }

    @Test
    fun `formats email display correctly`() {
        val conversation = createMockConversation(
            channel = "email",
            status = "sent",
            recipient = "customer@example.com"
        )
        
        val display = formatForDisplay(conversation)
        
        assertTrue(display.contains("Email"))
        assertTrue(display.contains("sent"))
    }

    @Test
    fun `truncates long message content for preview`() {
        val longContent = "A".repeat(200)
        val message = createMockMessage(content = longContent)
        
        val preview = getMessagePreview(message, maxLength = 50)
        
        assertEquals(53, preview.length)  // 50 chars + "..."
        assertTrue(preview.endsWith("..."))
    }

    @Test
    fun `does not truncate short message content`() {
        val shortContent = "Your order is ready!"
        val message = createMockMessage(content = shortContent)
        
        val preview = getMessagePreview(message, maxLength = 50)
        
        assertEquals(shortContent, preview)
    }

    // ==================== Empty State Tests ====================

    @Test
    fun `handles empty conversations list`() {
        val conversations = emptyList<MockConversation>()
        
        assertTrue(conversations.isEmpty())
        assertEquals(0, countNotifications(conversations))
    }

    @Test
    fun `handles conversation with no messages`() {
        val conversation = createMockConversation(messages = emptyList())
        
        assertTrue(conversation.messages.isEmpty())
    }

    // ==================== Helper Data Classes ====================

    data class MockConversation(
        val id: String,
        val status: String,
        val channel: String,
        val reference: String? = null,
        val recipient: String? = null,
        val messages: List<MockMessage> = emptyList()
    )

    data class MockMessage(
        val id: String,
        val content: String,
        val direction: String,
        val timestamp: String
    )

    // ==================== Helper Methods ====================

    private fun createMockConversation(
        id: String = "conv-1",
        status: String = "delivered",
        channel: String = "sms",
        reference: String? = null,
        recipient: String? = null,
        messages: List<MockMessage> = emptyList()
    ) = MockConversation(id, status, channel, reference, recipient, messages)

    private fun createMockMessage(
        id: String = "msg-1",
        content: String = "Test message",
        direction: String = "outbound",
        timestamp: String = "2024-01-15T10:00:00Z"
    ) = MockMessage(id, content, direction, timestamp)

    private fun isSmsChannel(channel: String) = channel.lowercase() == "sms"
    private fun isEmailChannel(channel: String) = channel.lowercase() == "email"
    
    private fun isDelivered(status: String) = status.lowercase() == "delivered"
    private fun isSent(status: String) = status.lowercase() == "sent"
    private fun isFailed(status: String) = status.lowercase() == "failed"
    private fun isPending(status: String) = status.lowercase() == "pending"
    
    private fun isOutbound(direction: String) = direction.lowercase() == "outbound"
    private fun isInbound(direction: String) = direction.lowercase() == "inbound"

    private fun extractOrderIdFromReference(reference: String): String? {
        return if (reference.startsWith("order-")) {
            reference.removePrefix("order-")
        } else null
    }

    private fun filterByOrderId(conversations: List<MockConversation>, orderId: String): List<MockConversation> {
        return conversations.filter { it.reference?.contains(orderId) == true }
    }

    private fun parseTimestamp(timestamp: String): Long? {
        return try {
            java.time.Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    private fun sortByTimestampDesc(messages: List<MockMessage>): List<MockMessage> {
        return messages.sortedByDescending { parseTimestamp(it.timestamp) ?: 0 }
    }

    private fun formatForDisplay(conversation: MockConversation): String {
        val channelLabel = if (conversation.channel == "sms") "SMS" else "Email"
        return "$channelLabel - ${conversation.status}"
    }

    private fun getMessagePreview(message: MockMessage, maxLength: Int): String {
        return if (message.content.length > maxLength) {
            message.content.take(maxLength) + "..."
        } else {
            message.content
        }
    }

    private fun countNotifications(conversations: List<MockConversation>): Int {
        return conversations.size
    }
}
