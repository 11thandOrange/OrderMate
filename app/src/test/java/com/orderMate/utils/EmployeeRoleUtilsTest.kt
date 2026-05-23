package com.orderMate.utils

import com.clover.sdk.v3.employees.AccountRole
import com.clover.sdk.v3.employees.Employee
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests for EmployeeRoleUtils (#81)
 * 
 * Tests cover:
 * - Role detection (isOwner, isAdmin, isManager, isEmployee)
 * - Permission checking (canAccessSettings)
 * - Role name formatting (getRoleName)
 */
class EmployeeRoleUtilsTest {

    // ==================== isOwner Tests ====================

    @Test
    fun `isOwner returns true when employee isOwner is true`() {
        val employee = createMockEmployee(isOwner = true)
        assertTrue(EmployeeRoleUtils.isOwner(employee))
    }

    @Test
    fun `isOwner returns false when employee isOwner is false`() {
        val employee = createMockEmployee(isOwner = false)
        assertFalse(EmployeeRoleUtils.isOwner(employee))
    }

    @Test
    fun `isOwner returns false when employee is null`() {
        assertFalse(EmployeeRoleUtils.isOwner(null))
    }

    // ==================== isAdmin Tests ====================

    @Test
    fun `isAdmin returns true when role is ADMIN`() {
        val employee = createMockEmployee(role = AccountRole.ADMIN)
        assertTrue(EmployeeRoleUtils.isAdmin(employee))
    }

    @Test
    fun `isAdmin returns false when role is MANAGER`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        assertFalse(EmployeeRoleUtils.isAdmin(employee))
    }

    @Test
    fun `isAdmin returns false when role is EMPLOYEE`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        assertFalse(EmployeeRoleUtils.isAdmin(employee))
    }

    @Test
    fun `isAdmin returns false when employee is null`() {
        assertFalse(EmployeeRoleUtils.isAdmin(null))
    }

    // ==================== isManager Tests ====================

    @Test
    fun `isManager returns true when role is MANAGER`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        assertTrue(EmployeeRoleUtils.isManager(employee))
    }

    @Test
    fun `isManager returns false when role is ADMIN`() {
        val employee = createMockEmployee(role = AccountRole.ADMIN)
        assertFalse(EmployeeRoleUtils.isManager(employee))
    }

    @Test
    fun `isManager returns false when role is EMPLOYEE`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        assertFalse(EmployeeRoleUtils.isManager(employee))
    }

    @Test
    fun `isManager returns false when employee is null`() {
        assertFalse(EmployeeRoleUtils.isManager(null))
    }

    // ==================== isEmployee Tests ====================

    @Test
    fun `isEmployee returns true when role is EMPLOYEE`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        assertTrue(EmployeeRoleUtils.isEmployee(employee))
    }

    @Test
    fun `isEmployee returns false when role is ADMIN`() {
        val employee = createMockEmployee(role = AccountRole.ADMIN)
        assertFalse(EmployeeRoleUtils.isEmployee(employee))
    }

    @Test
    fun `isEmployee returns false when role is MANAGER`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        assertFalse(EmployeeRoleUtils.isEmployee(employee))
    }

    @Test
    fun `isEmployee returns false when employee is null`() {
        assertFalse(EmployeeRoleUtils.isEmployee(null))
    }

    // ==================== canAccessSettings Tests ====================

    @Test
    fun `canAccessSettings returns true for owner regardless of settings`() {
        val employee = createMockEmployee(isOwner = true, role = AccountRole.EMPLOYEE)
        val settings = AdvancedSettings(
            allowAdminUpdateSettings = false,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns true for admin when allowAdminUpdateSettings is true`() {
        val employee = createMockEmployee(role = AccountRole.ADMIN)
        val settings = AdvancedSettings(allowAdminUpdateSettings = true)
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns false for admin when allowAdminUpdateSettings is false`() {
        val employee = createMockEmployee(role = AccountRole.ADMIN)
        val settings = AdvancedSettings(allowAdminUpdateSettings = false)
        assertFalse(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns true for manager when allowManagersUpdateSettings is true`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        val settings = AdvancedSettings(allowManagersUpdateSettings = true)
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns false for manager when allowManagersUpdateSettings is false`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        val settings = AdvancedSettings(allowManagersUpdateSettings = false)
        assertFalse(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns true for employee when allowEmployeesUpdateSettings is true`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val settings = AdvancedSettings(allowEmployeesUpdateSettings = true)
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns false for employee when allowEmployeesUpdateSettings is false`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val settings = AdvancedSettings(allowEmployeesUpdateSettings = false)
        assertFalse(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `canAccessSettings returns false when employee is null`() {
        val settings = AdvancedSettings()
        assertFalse(EmployeeRoleUtils.canAccessSettings(null, settings))
    }

    @Test
    fun `canAccessSettings returns false for unknown role`() {
        val employee = createMockEmployee(role = null)
        val settings = AdvancedSettings()
        assertFalse(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    // ==================== getRoleName Tests ====================

    @Test
    fun `getRoleName returns Owner when isOwner is true`() {
        val employee = createMockEmployee(isOwner = true, role = AccountRole.ADMIN)
        assertEquals("Owner", EmployeeRoleUtils.getRoleName(employee))
    }

    @Test
    fun `getRoleName returns Admin when role is ADMIN and not owner`() {
        val employee = createMockEmployee(isOwner = false, role = AccountRole.ADMIN)
        assertEquals("Admin", EmployeeRoleUtils.getRoleName(employee))
    }

    @Test
    fun `getRoleName returns Manager when role is MANAGER`() {
        val employee = createMockEmployee(role = AccountRole.MANAGER)
        assertEquals("Manager", EmployeeRoleUtils.getRoleName(employee))
    }

    @Test
    fun `getRoleName returns Employee when role is EMPLOYEE`() {
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        assertEquals("Employee", EmployeeRoleUtils.getRoleName(employee))
    }

    @Test
    fun `getRoleName returns Unknown when employee is null`() {
        assertEquals("Unknown", EmployeeRoleUtils.getRoleName(null))
    }

    @Test
    fun `getRoleName returns Unknown for null role`() {
        val employee = createMockEmployee(role = null)
        assertEquals("Unknown", EmployeeRoleUtils.getRoleName(employee))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `owner status takes precedence over role for canAccessSettings`() {
        // Even if role is EMPLOYEE and employees are not allowed, owner can still access
        val employee = createMockEmployee(isOwner = true, role = AccountRole.EMPLOYEE)
        val settings = AdvancedSettings(
            allowAdminUpdateSettings = false,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `all permissions enabled allows all roles`() {
        val settings = AdvancedSettings(
            allowAdminUpdateSettings = true,
            allowManagersUpdateSettings = true,
            allowEmployeesUpdateSettings = true
        )
        
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        
        assertTrue(EmployeeRoleUtils.canAccessSettings(admin, settings))
        assertTrue(EmployeeRoleUtils.canAccessSettings(manager, settings))
        assertTrue(EmployeeRoleUtils.canAccessSettings(employee, settings))
    }

    @Test
    fun `all permissions disabled blocks non-owners`() {
        val settings = AdvancedSettings(
            allowAdminUpdateSettings = false,
            allowManagersUpdateSettings = false,
            allowEmployeesUpdateSettings = false
        )
        
        val admin = createMockEmployee(role = AccountRole.ADMIN)
        val manager = createMockEmployee(role = AccountRole.MANAGER)
        val employee = createMockEmployee(role = AccountRole.EMPLOYEE)
        val owner = createMockEmployee(isOwner = true)
        
        assertFalse(EmployeeRoleUtils.canAccessSettings(admin, settings))
        assertFalse(EmployeeRoleUtils.canAccessSettings(manager, settings))
        assertFalse(EmployeeRoleUtils.canAccessSettings(employee, settings))
        assertTrue(EmployeeRoleUtils.canAccessSettings(owner, settings))
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
