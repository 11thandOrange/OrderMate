package com.orderMate.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NotificationTemplate model (#64)
 * 
 * Tests cover:
 * - Subject field addition
 * - Factory method with all parameters
 * - Default values
 * - Data class equality
 */
class NotificationTemplateTest {

    // ==================== Subject Field Tests (#64) ====================

    @Test
    fun `template has subject field`() {
        val template = NotificationTemplate(
            id = "test-id",
            name = "Test Template",
            content = "Hello {{customer_name}}",
            subject = "Your order is ready!"
        )
        
        assertEquals("Your order is ready!", template.subject)
    }

    @Test
    fun `template subject defaults to empty string`() {
        val template = NotificationTemplate(
            id = "test-id",
            name = "Test Template",
            content = "Hello {{customer_name}}"
        )
        
        assertEquals("", template.subject)
    }

    @Test
    fun `template subject is mutable`() {
        val template = NotificationTemplate(
            id = "test-id",
            name = "Test",
            content = "Content"
        )
        
        template.subject = "New Subject"
        assertEquals("New Subject", template.subject)
    }

    // ==================== Factory Method Tests ====================

    @Test
    fun `create factory generates unique ID`() {
        val template1 = NotificationTemplate.create()
        val template2 = NotificationTemplate.create()
        
        assertNotEquals(template1.id, template2.id)
        assertTrue(template1.id.isNotEmpty())
        assertTrue(template2.id.isNotEmpty())
    }

    @Test
    fun `create factory uses default name`() {
        val template = NotificationTemplate.create()
        assertEquals("New Template", template.name)
    }

    @Test
    fun `create factory uses default empty content`() {
        val template = NotificationTemplate.create()
        assertEquals("", template.content)
    }

    @Test
    fun `create factory uses default empty subject`() {
        val template = NotificationTemplate.create()
        assertEquals("", template.subject)
    }

    @Test
    fun `create factory accepts custom name`() {
        val template = NotificationTemplate.create(name = "Order Ready")
        assertEquals("Order Ready", template.name)
    }

    @Test
    fun `create factory accepts custom content`() {
        val template = NotificationTemplate.create(content = "Your order is ready!")
        assertEquals("Your order is ready!", template.content)
    }

    @Test
    fun `create factory accepts custom subject`() {
        val template = NotificationTemplate.create(subject = "Order Update")
        assertEquals("Order Update", template.subject)
    }

    @Test
    fun `create factory accepts all custom parameters`() {
        val template = NotificationTemplate.create(
            name = "Custom Template",
            content = "Hello {{customer_name}}",
            subject = "Important Update"
        )
        
        assertEquals("Custom Template", template.name)
        assertEquals("Hello {{customer_name}}", template.content)
        assertEquals("Important Update", template.subject)
    }

    // ==================== Data Class Behavior Tests ====================

    @Test
    fun `templates with same values are equal`() {
        val template1 = NotificationTemplate("id1", "Name", "Content", "Subject")
        val template2 = NotificationTemplate("id1", "Name", "Content", "Subject")
        
        assertEquals(template1, template2)
    }

    @Test
    fun `templates with different IDs are not equal`() {
        val template1 = NotificationTemplate("id1", "Name", "Content", "Subject")
        val template2 = NotificationTemplate("id2", "Name", "Content", "Subject")
        
        assertNotEquals(template1, template2)
    }

    @Test
    fun `templates with different subjects are not equal`() {
        val template1 = NotificationTemplate("id1", "Name", "Content", "Subject1")
        val template2 = NotificationTemplate("id1", "Name", "Content", "Subject2")
        
        assertNotEquals(template1, template2)
    }

    @Test
    fun `template copy preserves subject`() {
        val original = NotificationTemplate("id1", "Name", "Content", "Subject")
        val copy = original.copy()
        
        assertEquals(original.subject, copy.subject)
    }

    @Test
    fun `template copy can override subject`() {
        val original = NotificationTemplate("id1", "Name", "Content", "Original Subject")
        val copy = original.copy(subject = "New Subject")
        
        assertEquals("New Subject", copy.subject)
        assertEquals("Original Subject", original.subject)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `template handles empty subject`() {
        val template = NotificationTemplate("id", "Name", "Content", "")
        assertEquals("", template.subject)
    }

    @Test
    fun `template handles long subject`() {
        val longSubject = "A".repeat(200)
        val template = NotificationTemplate("id", "Name", "Content", longSubject)
        assertEquals(longSubject, template.subject)
    }

    @Test
    fun `template handles special characters in subject`() {
        val specialSubject = "Your order #{{order_id}} is ready! 🎉"
        val template = NotificationTemplate("id", "Name", "Content", specialSubject)
        assertEquals(specialSubject, template.subject)
    }
}
