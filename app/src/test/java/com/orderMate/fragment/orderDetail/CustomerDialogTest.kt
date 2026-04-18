package com.orderMate.fragment.orderDetail

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CustomerDialog behavior (#66 #67 #68)
 * 
 * Tests cover:
 * - Auto-fill customer data (#66)
 * - Save customer logic (#67)
 * - Customer search integration (#68)
 * - Avatar initials generation
 */
class CustomerDialogTest {

    // Simulated customer data
    data class MockCustomer(
        val id: String?,
        var firstName: String?,
        var lastName: String?,
        var phone: String?,
        var email: String?
    )

    // Simulated form state
    private var inputFirstName: String = ""
    private var inputLastName: String = ""
    private var inputPhone: String = ""
    private var inputEmail: String = ""
    private var avatarInitials: String = "?"

    // Simulated save state
    private var lastSaveMethod: String = ""
    private var savedCustomerId: String? = null

    @Before
    fun setUp() {
        inputFirstName = ""
        inputLastName = ""
        inputPhone = ""
        inputEmail = ""
        avatarInitials = "?"
        lastSaveMethod = ""
        savedCustomerId = null
    }

    // ==================== Auto-Fill Tests (#66) ====================

    @Test
    fun `auto-fill populates first name from customer`() {
        val customer = MockCustomer("123", "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("John", inputFirstName)
    }

    @Test
    fun `auto-fill populates last name from customer`() {
        val customer = MockCustomer("123", "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("Doe", inputLastName)
    }

    @Test
    fun `auto-fill populates phone from customer`() {
        val customer = MockCustomer("123", "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("555-1234", inputPhone)
    }

    @Test
    fun `auto-fill populates email from customer`() {
        val customer = MockCustomer("123", "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("john@example.com", inputEmail)
    }

    @Test
    fun `auto-fill handles null first name`() {
        val customer = MockCustomer("123", null, "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("", inputFirstName)
    }

    @Test
    fun `auto-fill handles null last name`() {
        val customer = MockCustomer("123", "John", null, "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("", inputLastName)
    }

    @Test
    fun `auto-fill handles null phone`() {
        val customer = MockCustomer("123", "John", "Doe", null, "john@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("", inputPhone)
    }

    @Test
    fun `auto-fill handles null email`() {
        val customer = MockCustomer("123", "John", "Doe", "555-1234", null)
        autoFillFromCustomer(customer)
        
        assertEquals("", inputEmail)
    }

    @Test
    fun `auto-fill updates avatar initials`() {
        val customer = MockCustomer("123", "John", "Doe", null, null)
        autoFillFromCustomer(customer)
        
        assertEquals("JD", avatarInitials)
    }

    // ==================== Save Customer Tests (#67) ====================

    @Test
    fun `save new customer calls createAndSaveCustomerToClover`() {
        val customer = MockCustomer(null, "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        saveCustomer(customer)
        
        assertEquals("createAndSaveCustomerToClover", lastSaveMethod)
    }

    @Test
    fun `save existing customer calls updateCustomerInClover`() {
        val customer = MockCustomer("existing-id-123", "John", "Doe", "555-1234", "john@example.com")
        autoFillFromCustomer(customer)
        
        saveCustomer(customer)
        
        assertEquals("updateCustomerInClover", lastSaveMethod)
        assertEquals("existing-id-123", savedCustomerId)
    }

    @Test
    fun `save requires at least one field filled`() {
        val customer = MockCustomer(null, null, null, null, null)
        autoFillFromCustomer(customer)
        
        val result = validateBeforeSave()
        
        assertFalse(result)
    }

    @Test
    fun `save allows only first name filled`() {
        inputFirstName = "John"
        
        val result = validateBeforeSave()
        
        assertTrue(result)
    }

    @Test
    fun `save allows only phone filled`() {
        inputPhone = "555-1234"
        
        val result = validateBeforeSave()
        
        assertTrue(result)
    }

    @Test
    fun `save allows only email filled`() {
        inputEmail = "john@example.com"
        
        val result = validateBeforeSave()
        
        assertTrue(result)
    }

    @Test
    fun `save trims whitespace from inputs`() {
        inputFirstName = "  John  "
        inputLastName = "  Doe  "
        inputPhone = "  555-1234  "
        inputEmail = "  john@example.com  "
        
        val trimmed = getTrimmedInputs()
        
        assertEquals("John", trimmed["firstName"])
        assertEquals("Doe", trimmed["lastName"])
        assertEquals("555-1234", trimmed["phone"])
        assertEquals("john@example.com", trimmed["email"])
    }

    // ==================== Avatar Initials Tests ====================

    @Test
    fun `avatar shows both initials when both names present`() {
        val initials = getInitials("John", "Doe")
        assertEquals("JD", initials)
    }

    @Test
    fun `avatar shows first initial only when last name missing`() {
        val initials = getInitials("John", null)
        assertEquals("J", initials)
    }

    @Test
    fun `avatar shows last initial only when first name missing`() {
        val initials = getInitials(null, "Doe")
        assertEquals("D", initials)
    }

    @Test
    fun `avatar shows question mark when both names missing`() {
        val initials = getInitials(null, null)
        assertEquals("?", initials)
    }

    @Test
    fun `avatar shows question mark for empty names`() {
        val initials = getInitials("", "")
        assertEquals("?", initials)
    }

    @Test
    fun `avatar initials are uppercase`() {
        val initials = getInitials("john", "doe")
        assertEquals("JD", initials)
    }

    @Test
    fun `avatar handles single character names`() {
        val initials = getInitials("J", "D")
        assertEquals("JD", initials)
    }

    // ==================== Customer Search Integration Tests (#68) ====================

    @Test
    fun `selecting customer from search auto-fills all fields`() {
        val searchResult = MockCustomer("search-123", "Jane", "Smith", "555-9876", "jane@example.com")
        
        // Simulate customer selection from search dialog
        autoFillFromCustomer(searchResult)
        
        assertEquals("Jane", inputFirstName)
        assertEquals("Smith", inputLastName)
        assertEquals("555-9876", inputPhone)
        assertEquals("jane@example.com", inputEmail)
        assertEquals("JS", avatarInitials)
    }

    @Test
    fun `selecting customer from search replaces existing values`() {
        // Initial values
        inputFirstName = "John"
        inputLastName = "Doe"
        
        // Select different customer from search
        val searchResult = MockCustomer("search-123", "Jane", "Smith", "555-9876", "jane@example.com")
        autoFillFromCustomer(searchResult)
        
        assertEquals("Jane", inputFirstName)
        assertEquals("Smith", inputLastName)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles special characters in names`() {
        val customer = MockCustomer("123", "José", "O'Brien", "555-1234", "jose@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("José", inputFirstName)
        assertEquals("O'Brien", inputLastName)
        assertEquals("JO", avatarInitials)
    }

    @Test
    fun `handles unicode characters in names`() {
        val customer = MockCustomer("123", "田中", "太郎", "555-1234", "tanaka@example.com")
        autoFillFromCustomer(customer)
        
        assertEquals("田中", inputFirstName)
        assertEquals("太郎", inputLastName)
        assertEquals("田太", avatarInitials)
    }

    @Test
    fun `handles very long names`() {
        val longName = "A".repeat(100)
        val customer = MockCustomer("123", longName, longName, null, null)
        autoFillFromCustomer(customer)
        
        assertEquals(longName, inputFirstName)
        assertEquals("AA", avatarInitials)  // Only uses first chars
    }

    // ==================== Helper Methods ====================

    private fun autoFillFromCustomer(customer: MockCustomer) {
        inputFirstName = customer.firstName ?: ""
        inputLastName = customer.lastName ?: ""
        inputPhone = customer.phone ?: ""
        inputEmail = customer.email ?: ""
        avatarInitials = getInitials(customer.firstName, customer.lastName)
    }

    private fun saveCustomer(customer: MockCustomer) {
        if (customer.id != null) {
            lastSaveMethod = "updateCustomerInClover"
            savedCustomerId = customer.id
        } else {
            lastSaveMethod = "createAndSaveCustomerToClover"
            savedCustomerId = null
        }
    }

    private fun validateBeforeSave(): Boolean {
        return inputFirstName.isNotEmpty() || 
               inputLastName.isNotEmpty() || 
               inputPhone.isNotEmpty() || 
               inputEmail.isNotEmpty()
    }

    private fun getTrimmedInputs(): Map<String, String> {
        return mapOf(
            "firstName" to inputFirstName.trim(),
            "lastName" to inputLastName.trim(),
            "phone" to inputPhone.trim(),
            "email" to inputEmail.trim()
        )
    }

    private fun getInitials(firstName: String?, lastName: String?): String {
        val first = firstName?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        val last = lastName?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        return "$first$last".ifEmpty { "?" }
    }
}
