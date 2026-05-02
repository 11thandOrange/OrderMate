package com.orderMate.utils

import com.clover.sdk.v3.employees.AccountRole
import com.clover.sdk.v3.employees.Employee

/**
 * #79: Utility for checking employee roles and permissions
 * 
 * Clover roles:
 * - Owner: Special flag (employee.isOwner), not a role - always has full access
 * - ADMIN: Full access to all features
 * - MANAGER: Most day-to-day features, some restrictions
 * - EMPLOYEE: Limited access, mainly POS operations
 */
object EmployeeRoleUtils {
    
    /**
     * Check if the employee is the account owner
     * Owner always has access to settings regardless of permission toggles
     */
    fun isOwner(employee: Employee?): Boolean {
        return employee?.isOwner == true
    }
    
    /**
     * Check if the employee has ADMIN role
     */
    fun isAdmin(employee: Employee?): Boolean {
        return employee?.role == AccountRole.ADMIN
    }
    
    /**
     * Check if the employee has MANAGER role
     */
    fun isManager(employee: Employee?): Boolean {
        return employee?.role == AccountRole.MANAGER
    }
    
    /**
     * Check if the employee has EMPLOYEE role
     */
    fun isEmployee(employee: Employee?): Boolean {
        return employee?.role == AccountRole.EMPLOYEE
    }
    
    /**
     * Check if the current employee can access settings based on their role
     * and the permission settings from Firebase
     * 
     * @param employee The current logged-in employee
     * @param settings The advanced settings containing permission toggles
     * @return true if the employee can access settings, false otherwise
     */
    fun canAccessSettings(employee: Employee?, settings: AdvancedSettings): Boolean {
        if (employee == null) return false
        
        // Owner always has access
        if (isOwner(employee)) return true
        
        // Check role-based permissions
        return when (employee.role) {
            AccountRole.ADMIN -> settings.allowAdminUpdateSettings
            AccountRole.MANAGER -> settings.allowManagersUpdateSettings
            AccountRole.EMPLOYEE -> settings.allowEmployeesUpdateSettings
            else -> false
        }
    }
    
    /**
     * Get a human-readable role name for the employee
     */
    fun getRoleName(employee: Employee?): String {
        if (employee == null) return "Unknown"
        if (isOwner(employee)) return "Owner"
        
        return when (employee.role) {
            AccountRole.ADMIN -> "Admin"
            AccountRole.MANAGER -> "Manager"
            AccountRole.EMPLOYEE -> "Employee"
            else -> "Unknown"
        }
    }
}
