package com.orderMate.integration

import com.clover.sdk.v3.employees.AccountRole
import com.clover.sdk.v3.employees.Employee
import com.orderMate.utils.AdvancedSettings
import com.orderMate.utils.EmployeeRoleUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Integration tests for Settings Permission Guardrails (#81)
 * 
 * Tests cover the permission flow:
 * 1. MainActivity.onResume() -> checkSettingsNavVisibility()
 * 2. SettingsFragment.onViewCreated() -> entry guard redirect
 * 3. Advanced tab -> permissionSettingsCard visibility
 * 
 * Permission Matrix:
 * | Role     | Owner | Settings | allowAdminUpdateSettings | allowManagersUpdateSettings | allowEmployeesUpdateSettings |
 * |----------|-------|----------|--------------------------|-----------------------------|-----------------------------|
 * | Owner    | true  | ✅ Always |           -              |              -              |              -              |
 * | Admin    | false | Depends  |          true/false      |              -              |              -              |
 * | Manager  | false | Depends  |            -             |          true/false         |              -              |
 * | Employee | false | Depends  |            -             |              -              |          true/false         |
 */
class SettingsPermissionTest {

    private lateinit var defaultSettings: AdvancedSettings
    private lateinit var restrictiveSettings: AdvancedSettings
    private lateinit var adminOnlySettings: AdvancedSettings

    @Before
    fun setUp() {
        // Default: all roles can access
        defaultSettings = AdvancedSettings(
            allowAdminUpdateSettings = true,
            allowManagersUpdateSettings = true,
            allowEmployeesUpdateSettings = true
        )
        
        // Restrictive: no roles can access (only owner)
        restrictiveSettings = AdvancedSettings(
            allowAdminUpdateSettings = false,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
        
        // Admin only: only admin and owner can access
        adminOnlySettings = AdvancedSettings(
            allowAdminUpdateSettings = true,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
    }

    // ==================== Permission Card Visibility Tests ====================
    // These test the logic that determines if permissionSettingsCard is visible
    // In SettingsFragment: permissionSettingsCard?.visibility = if (isOwner) View.VISIBLE else View.GONE

    @Test
    fun `permissionSettingsCard is visible for owner`() {
        val owner = createMockEmployee(isOwner = true)
        val isOwner = EmployeeRoleUtils.isOwner(owner)
        assertTrue("Permission settings card should be visible for owner", isOwner)
    }

    @Test
    fun `permissionSettingsCard is hidden for admin`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val isOwner = EmployeeRoleUtils.isOwner(admin)
        assertFalse("Permission settings card should be hidden for admin", isOwner)
    }

    @Test
    fun `permissionSettingsCard is hidden for manager`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val isOwner = EmployeeRoleUtils.isOwner(manager)
        assertFalse("Permission settings card should be hidden for manager", isOwner)
    }

    @Test
    fun `permissionSettingsCard is hidden for employee`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val isOwner = EmployeeRoleUtils.isOwner(employee)
        assertFalse("Permission settings card should be hidden for employee", isOwner)
    }

    // ==================== Settings Entry Guard Tests ====================
    // These test the logic in SettingsFragment.onViewCreated() that redirects non-permitted users

    @Test
    fun `non-permitted admin is redirected from settings`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val canAccess = EmployeeRoleUtils.canAccessSettings(admin, restrictiveSettings)
        assertFalse("Admin should be redirected when not permitted", canAccess)
    }

    @Test
    fun `non-permitted manager is redirected from settings`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val canAccess = EmployeeRoleUtils.canAccessSettings(manager, restrictiveSettings)
        assertFalse("Manager should be redirected when not permitted", canAccess)
    }

    @Test
    fun `non-permitted employee is redirected from settings`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, restrictiveSettings)
        assertFalse("Employee should be redirected when not permitted", canAccess)
    }

    @Test
    fun `permitted admin can enter settings`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val canAccess = EmployeeRoleUtils.canAccessSettings(admin, defaultSettings)
        assertTrue("Permitted admin should enter settings", canAccess)
    }

    @Test
    fun `permitted manager can enter settings`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val canAccess = EmployeeRoleUtils.canAccessSettings(manager, defaultSettings)
        assertTrue("Permitted manager should enter settings", canAccess)
    }

    @Test
    fun `permitted employee can enter settings`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, defaultSettings)
        assertTrue("Permitted employee should enter settings", canAccess)
    }

    @Test
    fun `owner can always enter settings even when all permissions disabled`() {
        val owner = createMockEmployee(isOwner = true)
        val canAccess = EmployeeRoleUtils.canAccessSettings(owner, restrictiveSettings)
        assertTrue("Owner should always enter settings", canAccess)
    }

    // ==================== Nav Visibility Tests ====================
    // These test the logic in MainActivity.checkSettingsNavVisibility()

    @Test
    fun `settings nav is visible for owner`() {
        val owner = createMockEmployee(isOwner = true)
        val canAccess = EmployeeRoleUtils.canAccessSettings(owner, restrictiveSettings)
        assertTrue("Settings nav should be visible for owner", canAccess)
    }

    @Test
    fun `settings nav is visible for permitted admin`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val canAccess = EmployeeRoleUtils.canAccessSettings(admin, defaultSettings)
        assertTrue("Settings nav should be visible for permitted admin", canAccess)
    }

    @Test
    fun `settings nav is hidden for non-permitted admin`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val canAccess = EmployeeRoleUtils.canAccessSettings(admin, restrictiveSettings)
        assertFalse("Settings nav should be hidden for non-permitted admin", canAccess)
    }

    @Test
    fun `settings nav is hidden for non-permitted manager`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val canAccess = EmployeeRoleUtils.canAccessSettings(manager, restrictiveSettings)
        assertFalse("Settings nav should be hidden for non-permitted manager", canAccess)
    }

    @Test
    fun `settings nav is hidden for non-permitted employee`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, restrictiveSettings)
        assertFalse("Settings nav should be hidden for non-permitted employee", canAccess)
    }

    // ==================== Admin-Only Configuration Tests ====================

    @Test
    fun `admin can access when admin-only settings enabled`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val canAccess = EmployeeRoleUtils.canAccessSettings(admin, adminOnlySettings)
        assertTrue("Admin should access with admin-only settings", canAccess)
    }

    @Test
    fun `manager cannot access when admin-only settings enabled`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val canAccess = EmployeeRoleUtils.canAccessSettings(manager, adminOnlySettings)
        assertFalse("Manager should not access with admin-only settings", canAccess)
    }

    @Test
    fun `employee cannot access when admin-only settings enabled`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, adminOnlySettings)
        assertFalse("Employee should not access with admin-only settings", canAccess)
    }

    @Test
    fun `owner can access when admin-only settings enabled`() {
        val owner = createMockEmployee(isOwner = true)
        val canAccess = EmployeeRoleUtils.canAccessSettings(owner, adminOnlySettings)
        assertTrue("Owner should always access regardless of settings", canAccess)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `null employee cannot access settings`() {
        val canAccess = EmployeeRoleUtils.canAccessSettings(null, defaultSettings)
        assertFalse("Null employee should not access settings", canAccess)
    }

    @Test
    fun `employee with null role cannot access settings`() {
        val employee = createMockEmployee(role = null)
        val canAccess = EmployeeRoleUtils.canAccessSettings(employee, defaultSettings)
        assertFalse("Employee with null role should not access settings", canAccess)
    }

    @Test
    fun `owner with employee role still has full access`() {
        // Edge case: owner flag set but role is EMPLOYEE
        val ownerWithEmployeeRole = createMockEmployee(isOwner = true, role = AccountRole.EMPLOYEE)
        val canAccess = EmployeeRoleUtils.canAccessSettings(ownerWithEmployeeRole, restrictiveSettings)
        assertTrue("Owner flag should override role restrictions", canAccess)
    }

    // ==================== Permission Change Scenarios ====================
    // These simulate what happens when permissions change

    @Test
    fun `admin loses access when permission toggled off`() {
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        
        // Initially permitted
        val initialSettings = AdvancedSettings(allowAdminUpdateSettings = true)
        assertTrue(EmployeeRoleUtils.canAccessSettings(admin, initialSettings))
        
        // Permission revoked
        val updatedSettings = AdvancedSettings(allowAdminUpdateSettings = false)
        assertFalse(EmployeeRoleUtils.canAccessSettings(admin, updatedSettings))
    }

    @Test
    fun `manager gains access when permission toggled on`() {
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        
        // Initially not permitted
        val initialSettings = AdvancedSettings(allowManagersUpdateSettings = false)
        assertFalse(EmployeeRoleUtils.canAccessSettings(manager, initialSettings))
        
        // Permission granted
        val updatedSettings = AdvancedSettings(allowManagersUpdateSettings = true)
        assertTrue(EmployeeRoleUtils.canAccessSettings(manager, updatedSettings))
    }

    @Test
    fun `owner access unaffected by permission changes`() {
        val owner = createMockEmployee(isOwner = true)
        
        // All permissions off
        val restrictive = AdvancedSettings(
            allowAdminUpdateSettings = false,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
        assertTrue(EmployeeRoleUtils.canAccessSettings(owner, restrictive))
        
        // All permissions on
        val permissive = AdvancedSettings(
            allowAdminUpdateSettings = true,
            allowManagersUpdateSettings = true,
            allowEmployeesUpdateSettings = true
        )
        assertTrue(EmployeeRoleUtils.canAccessSettings(owner, permissive))
    }

    // ==================== Helper Functions ====================

    private fun createMockEmployee(
        isOwner: Boolean = false,
        role: AccountRole? = AccountRole.EMPLOYEE
    ): Employee {
        val employee = mock(Employee::class.java)
        `when`(employee.isOwner).thenReturn(isOwner)
        `when`(employee.role).thenReturn(role)
        return employee
    }
}
