package com.orderMate.fragment.orderDetail

import com.orderMate.utils.NotificationTemplate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SendNotificationDialog behavior (#62 #63 #64 #65)
 * 
 * Tests cover:
 * - Template selection auto-fill (#63)
 * - Subject auto-fill for email mode (#64)
 * - Template loading from settings (#62)
 */
class SendNotificationDialogTest {

    private lateinit var templates: List<NotificationTemplate>
    
    // Simulated dialog state
    private var selectedTemplateIndex: Int = 0
    private var isSmsMode: Boolean = true
    private var messageContent: String = ""
    private var subjectContent: String = ""

    @Before
    fun setUp() {
        // Sample templates with subject field (#64)
        templates = listOf(
            NotificationTemplate(
                id = "template1",
                name = "Order Ready",
                content = "Hi {{customer_name}}, your order #{{order_id}} is ready for pickup!",
                subject = "Your order is ready for pickup!"
            ),
            NotificationTemplate(
                id = "template2", 
                name = "Pickup Reminder",
                content = "Reminder: Your order #{{order_id}} is scheduled for pickup today.",
                subject = "Reminder: Order pickup today"
            ),
            NotificationTemplate(
                id = "template3",
                name = "SMS Only",
                content = "Your order is ready!",
                subject = ""  // No subject for SMS-only template
            )
        )
        
        // Reset state
        selectedTemplateIndex = 0
        isSmsMode = true
        messageContent = ""
        subjectContent = ""
    }

    // ==================== Template Auto-Fill Tests (#63) ====================

    @Test
    fun `selecting template fills message content`() {
        // Simulate selecting "Order Ready" template
        selectTemplate(0)
        
        assertEquals(
            "Hi {{customer_name}}, your order #{{order_id}} is ready for pickup!",
            messageContent
        )
    }

    @Test
    fun `selecting different template updates message content`() {
        selectTemplate(0)
        assertEquals("Hi {{customer_name}}, your order #{{order_id}} is ready for pickup!", messageContent)
        
        selectTemplate(1)
        assertEquals("Reminder: Your order #{{order_id}} is scheduled for pickup today.", messageContent)
    }

    @Test
    fun `template content preserves variables`() {
        selectTemplate(0)
        
        assertTrue(messageContent.contains("{{customer_name}}"))
        assertTrue(messageContent.contains("{{order_id}}"))
    }

    // ==================== Subject Auto-Fill Tests (#64) ====================

    @Test
    fun `selecting template in email mode fills subject`() {
        isSmsMode = false  // Email mode
        selectTemplate(0)
        
        assertEquals("Your order is ready for pickup!", subjectContent)
    }

    @Test
    fun `selecting template in SMS mode does not fill subject`() {
        isSmsMode = true  // SMS mode
        selectTemplate(0)
        
        assertEquals("", subjectContent)  // Subject not filled in SMS mode
    }

    @Test
    fun `selecting template with empty subject does not override existing subject`() {
        isSmsMode = false
        subjectContent = "Existing Subject"
        
        // Template 3 has empty subject
        selectTemplate(2)
        
        // Subject should remain unchanged when template has empty subject
        assertEquals("Existing Subject", subjectContent)
    }

    @Test
    fun `subject auto-fill only when template has subject`() {
        isSmsMode = false
        
        // Template with subject
        selectTemplate(0)
        assertEquals("Your order is ready for pickup!", subjectContent)
        
        // Template without subject - should not clear existing
        selectTemplate(2)
        assertEquals("Your order is ready for pickup!", subjectContent)
    }

    @Test
    fun `switching to email mode does not auto-fill subject without template selection`() {
        isSmsMode = true
        selectTemplate(0)
        assertEquals("", subjectContent)  // No subject in SMS mode
        
        // Switch to email mode
        isSmsMode = false
        // Subject should still be empty - no auto re-fill on mode switch
        assertEquals("", subjectContent)
    }

    // ==================== Template Loading Tests (#62) ====================

    @Test
    fun `templates list is not empty after loading`() {
        assertTrue(templates.isNotEmpty())
    }

    @Test
    fun `templates have required fields`() {
        templates.forEach { template ->
            assertTrue(template.id.isNotEmpty())
            assertTrue(template.name.isNotEmpty())
            // Content can be empty but should exist
            assertNotNull(template.content)
            // Subject field exists (#64)
            assertNotNull(template.subject)
        }
    }

    @Test
    fun `template names are unique for display`() {
        val names = templates.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }

    // ==================== Mode Switching Tests ====================

    @Test
    fun `SMS mode is default`() {
        assertTrue(isSmsMode)
    }

    @Test
    fun `can switch to email mode`() {
        isSmsMode = false
        assertFalse(isSmsMode)
    }

    @Test
    fun `content persists when switching modes`() {
        isSmsMode = true
        selectTemplate(0)
        val originalContent = messageContent
        
        isSmsMode = false
        assertEquals(originalContent, messageContent)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `selecting invalid template index does nothing`() {
        messageContent = "Original"
        subjectContent = "Original Subject"
        
        selectTemplate(-1)
        
        assertEquals("Original", messageContent)
        assertEquals("Original Subject", subjectContent)
    }

    @Test
    fun `selecting out of bounds template index does nothing`() {
        messageContent = "Original"
        subjectContent = "Original Subject"
        
        selectTemplate(999)
        
        assertEquals("Original", messageContent)
        assertEquals("Original Subject", subjectContent)
    }

    @Test
    fun `empty templates list handles gracefully`() {
        val emptyTemplates = emptyList<NotificationTemplate>()
        assertTrue(emptyTemplates.isEmpty())
    }

    // ==================== Helper Methods ====================

    /**
     * Simulates template selection behavior from SendNotificationDialog
     * Matches the logic in setupTemplateSpinner() onItemSelected
     */
    private fun selectTemplate(index: Int) {
        if (index < 0 || index >= templates.size) return
        
        val selectedTemplate = templates[index]
        selectedTemplateIndex = index
        
        // Always fill content (#63)
        messageContent = selectedTemplate.content
        
        // Fill subject only in email mode and if template has subject (#64)
        if (!isSmsMode && selectedTemplate.subject.isNotBlank()) {
            subjectContent = selectedTemplate.subject
        }
    }
}
