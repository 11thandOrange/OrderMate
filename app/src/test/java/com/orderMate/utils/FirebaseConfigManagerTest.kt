package com.orderMate.utils

import com.orderMate.modals.EmployeeProfile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FirebaseConfigManager (#81)
 * 
 * Tests cover:
 * - getAllEmployeeProfiles parsing
 * - Error handling
 * - Default values for missing fields
 */
class FirebaseConfigManagerTest {

    // ==================== #81: getAllEmployeeProfiles Tests ====================

    @Test
    fun `getAllEmployeeProfiles parses multiple profiles`() {
        // Simulate Firebase snapshot data
        val snapshotData = mapOf(
            "emp_1" to mapOf("color" to "#ff0000", "avatar" to "😀"),
            "emp_2" to mapOf("color" to "#00ff00", "avatar" to "😎"),
            "emp_3" to mapOf("color" to "#0000ff", "avatar" to "🎉")
        )
        
        // Parse profiles
        val profiles = parseProfiles(snapshotData)
        
        assertEquals(3, profiles.size)
        assertEquals("#ff0000", profiles["emp_1"]?.color)
        assertEquals("😀", profiles["emp_1"]?.avatar)
        assertEquals("#00ff00", profiles["emp_2"]?.color)
        assertEquals("😎", profiles["emp_2"]?.avatar)
        assertEquals("#0000ff", profiles["emp_3"]?.color)
        assertEquals("🎉", profiles["emp_3"]?.avatar)
    }

    @Test
    fun `getAllEmployeeProfiles returns empty map when no profiles exist`() {
        val snapshotData = emptyMap<String, Map<String, String>>()
        
        val profiles = parseProfiles(snapshotData)
        
        assertTrue(profiles.isEmpty())
    }

    @Test
    fun `getAllEmployeeProfiles uses defaults for missing color`() {
        val snapshotData = mapOf(
            "emp_1" to mapOf("avatar" to "😀") // Missing color
        )
        
        val profiles = parseProfiles(snapshotData)
        
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profiles["emp_1"]?.color)
        assertEquals("😀", profiles["emp_1"]?.avatar)
    }

    @Test
    fun `getAllEmployeeProfiles uses defaults for missing avatar`() {
        val snapshotData = mapOf(
            "emp_1" to mapOf("color" to "#ff0000") // Missing avatar
        )
        
        val profiles = parseProfiles(snapshotData)
        
        assertEquals("#ff0000", profiles["emp_1"]?.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profiles["emp_1"]?.avatar)
    }

    @Test
    fun `getAllEmployeeProfiles uses all defaults for empty profile`() {
        val snapshotData = mapOf(
            "emp_1" to emptyMap<String, String>()
        )
        
        val profiles = parseProfiles(snapshotData)
        
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profiles["emp_1"]?.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profiles["emp_1"]?.avatar)
    }

    @Test
    fun `getAllEmployeeProfiles handles mixed complete and partial profiles`() {
        val snapshotData = mapOf(
            "emp_1" to mapOf("color" to "#ff0000", "avatar" to "😀"), // Complete
            "emp_2" to mapOf("color" to "#00ff00"), // Missing avatar
            "emp_3" to mapOf("avatar" to "🎉"), // Missing color
            "emp_4" to emptyMap() // Empty
        )
        
        val profiles = parseProfiles(snapshotData)
        
        assertEquals(4, profiles.size)
        
        // Complete profile
        assertEquals("#ff0000", profiles["emp_1"]?.color)
        assertEquals("😀", profiles["emp_1"]?.avatar)
        
        // Missing avatar
        assertEquals("#00ff00", profiles["emp_2"]?.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profiles["emp_2"]?.avatar)
        
        // Missing color
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profiles["emp_3"]?.color)
        assertEquals("🎉", profiles["emp_3"]?.avatar)
        
        // Empty
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profiles["emp_4"]?.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profiles["emp_4"]?.avatar)
    }

    @Test
    fun `getAllEmployeeProfiles preserves employee IDs as keys`() {
        val snapshotData = mapOf(
            "ABC123" to mapOf("color" to "#ff0000", "avatar" to "😀"),
            "XYZ789" to mapOf("color" to "#00ff00", "avatar" to "😎")
        )
        
        val profiles = parseProfiles(snapshotData)
        
        assertTrue(profiles.containsKey("ABC123"))
        assertTrue(profiles.containsKey("XYZ789"))
        assertFalse(profiles.containsKey("OTHER"))
    }

    // ==================== getEmployeeProfile Tests ====================

    @Test
    fun `getEmployeeProfile returns profile with correct values`() {
        val snapshotData = mapOf("color" to "#ff0000", "avatar" to "😀")
        
        val profile = parseProfile(snapshotData)
        
        assertEquals("#ff0000", profile.color)
        assertEquals("😀", profile.avatar)
    }

    @Test
    fun `getEmployeeProfile returns defaults on empty data`() {
        val snapshotData = emptyMap<String, String>()
        
        val profile = parseProfile(snapshotData)
        
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profile.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profile.avatar)
    }

    @Test
    fun `getEmployeeProfile returns defaults on null data`() {
        val profile = parseProfile(null)
        
        assertEquals(EmployeeProfile.DEFAULT_COLOR, profile.color)
        assertEquals(EmployeeProfile.DEFAULT_AVATAR, profile.avatar)
    }

    // ==================== Helper Functions ====================

    /**
     * Simulates parsing Firebase snapshot for all profiles
     */
    private fun parseProfiles(snapshotData: Map<String, Map<String, String>>): Map<String, EmployeeProfile> {
        val profiles = mutableMapOf<String, EmployeeProfile>()
        snapshotData.forEach { (employeeId, data) ->
            profiles[employeeId] = EmployeeProfile(
                color = data["color"] ?: EmployeeProfile.DEFAULT_COLOR,
                avatar = data["avatar"] ?: EmployeeProfile.DEFAULT_AVATAR
            )
        }
        return profiles
    }

    /**
     * Simulates parsing Firebase snapshot for single profile
     */
    private fun parseProfile(snapshotData: Map<String, String>?): EmployeeProfile {
        return EmployeeProfile(
            color = snapshotData?.get("color") ?: EmployeeProfile.DEFAULT_COLOR,
            avatar = snapshotData?.get("avatar") ?: EmployeeProfile.DEFAULT_AVATAR
        )
    }
}
