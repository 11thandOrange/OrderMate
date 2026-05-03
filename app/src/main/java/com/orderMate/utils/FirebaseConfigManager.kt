package com.orderMate.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import com.orderMate.modals.EmployeeProfile
import com.orderMate.modals.MerchantDiscount
import com.orderMate.modals.MerchantMeta
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.PopupSettings
import com.orderMate.modals.ReferralInfo
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType

/**
 * Firebase CRUD operations for schema structure
 * 
 * #81: Added support for:
 * - Per-employee profiles (color, avatar)
 * - Referrals (partner tracking)
 * - Discounts (admin-only, read in app)
 */
class FirebaseConfigManager private constructor() {
    
    private val db = FirebaseDatabase.getInstance()
    private val gson = Gson()
    
    companion object {
        @Volatile
        private var instance: FirebaseConfigManager? = null
        
        fun getInstance(): FirebaseConfigManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseConfigManager().also { instance = it }
            }
        }
    }
    
    // ==================== Meta ====================
    
    fun getSchemaVersion(merchantId: String, callback: (Int) -> Unit) {
        db.getReference(FirebasePaths.meta(merchantId))
            .child(FirebasePaths.SCHEMA_VERSION)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.getValue(Int::class.java) ?: 0)
            }
            .addOnFailureListener {
                callback(0)
            }
    }
    
    fun setSchemaVersion(merchantId: String, version: Int, callback: (Boolean) -> Unit) {
        val updates = mapOf<String, Any>(
            FirebasePaths.SCHEMA_VERSION to version,
            FirebasePaths.UPDATED_AT to ServerValue.TIMESTAMP
        )
        db.getReference(FirebasePaths.meta(merchantId))
            .updateChildren(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
    
    private fun updateTimestamp(merchantId: String) {
        db.getReference(FirebasePaths.meta(merchantId))
            .child(FirebasePaths.UPDATED_AT)
            .setValue(ServerValue.TIMESTAMP)
    }
    
    // ==================== Settings ====================
    
    fun getSettings(merchantId: String, callback: (PopupSettings) -> Unit) {
        db.getReference(FirebasePaths.settings(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.value as? Map<String, Any?>
                callback(PopupSettings.fromMap(map))
            }
            .addOnFailureListener {
                callback(PopupSettings())
            }
    }
    
    fun saveSettings(merchantId: String, settings: PopupSettings, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.settings(merchantId))
            .setValue(settings.toMap())
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { callback(false) }
    }
    
    // ==================== Widgets ====================
    
    fun getWidgets(merchantId: String, callback: (List<WidgetConfig>) -> Unit) {
        db.getReference(FirebasePaths.widgets(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val widgets = mutableListOf<WidgetConfig>()
                snapshot.children.forEach { child ->
                    try {
                        val widget = parseWidget(child)
                        if (widget != null) widgets.add(widget)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback(widgets.sortedBy { it.order })
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(emptyList())
            }
    }
    
    /**
     * Get only ORDER level widgets (for order popup settings)
     */
    fun getOrderWidgets(merchantId: String, callback: (List<WidgetConfig>) -> Unit) {
        android.util.Log.d("FirebaseWidgetDebug", "getOrderWidgets: fetching from Firebase for merchant=$merchantId")
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ORDER.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val widgets = snapshot.children.mapNotNull { parseWidget(it) }
                android.util.Log.d("FirebaseWidgetDebug", "getOrderWidgets: Firebase returned ${widgets.size} ORDER widgets")
                widgets.forEach { w ->
                    android.util.Log.d("FirebaseWidgetDebug", "  FIREBASE ORDER: id=${w.id}, label=${w.label}")
                }
                callback(widgets.sortedBy { it.order })
            }
            .addOnFailureListener {
                android.util.Log.e("FirebaseWidgetDebug", "getOrderWidgets: FAILED", it)
                it.printStackTrace()
                callback(emptyList())
            }
    }
    
    /**
     * Get only ITEM level widgets (for item popup settings)
     */
    fun getItemWidgets(merchantId: String, callback: (List<WidgetConfig>) -> Unit) {
        android.util.Log.d("FirebaseWidgetDebug", "getItemWidgets: fetching from Firebase for merchant=$merchantId")
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ITEM.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val widgets = snapshot.children.mapNotNull { parseWidget(it) }
                android.util.Log.d("FirebaseWidgetDebug", "getItemWidgets: Firebase returned ${widgets.size} ITEM widgets")
                widgets.forEach { w ->
                    android.util.Log.d("FirebaseWidgetDebug", "  FIREBASE ITEM: id=${w.id}, label=${w.label}")
                }
                callback(widgets.sortedBy { it.order })
            }
            .addOnFailureListener {
                android.util.Log.e("FirebaseWidgetDebug", "getItemWidgets: FAILED", it)
                it.printStackTrace()
                callback(emptyList())
            }
    }
    
    private fun parseWidget(snapshot: DataSnapshot): WidgetConfig? {
        val id = snapshot.key ?: return null
        val typeStr = snapshot.child("type").getValue(String::class.java)
        val label = snapshot.child("label").getValue(String::class.java) ?: return null
        
        val options = mutableListOf<WidgetOption>()
        snapshot.child("options").children.forEach { optChild ->
            val optId = optChild.key ?: return@forEach
            val optLabel = optChild.child("label").getValue(String::class.java) ?: return@forEach
            val optValue = optChild.child("value").getValue(String::class.java) ?: optLabel
            val isDefault = optChild.child("isDefault").getValue(Boolean::class.java) ?: false
            val color = optChild.child("color").getValue(String::class.java)
            
            options.add(WidgetOption(optId, optLabel, optValue, isDefault, color))
        }
        
        // Parse level field, default to ITEM for backward compatibility
        val levelStr = snapshot.child("level").getValue(String::class.java)
        val level = try {
            NoteLevel.valueOf(levelStr ?: "ITEM")
        } catch (e: Exception) {
            NoteLevel.ITEM
        }
        
        return WidgetConfig(
            id = id,
            type = WidgetType.fromString(typeStr),
            label = label,
            isEnabled = snapshot.child("isEnabled").getValue(Boolean::class.java) ?: true,
            isRequired = snapshot.child("isRequired").getValue(Boolean::class.java) ?: false,
            showInFilter = snapshot.child("showInFilter").getValue(Boolean::class.java) ?: true,
            options = options,
            order = snapshot.child("order").getValue(Int::class.java) ?: 0,
            level = level
        )
    }
    
    fun getWidget(merchantId: String, widgetId: String, callback: (WidgetConfig?) -> Unit) {
        db.getReference(FirebasePaths.widget(merchantId, widgetId))
            .get()
            .addOnSuccessListener { snapshot ->
                callback(parseWidget(snapshot))
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    
    fun saveWidget(merchantId: String, widget: WidgetConfig, callback: (Boolean) -> Unit) {
        // Ensure level is never null - default to ITEM for backward compatibility
        if (widget.level == null) {
            widget.level = NoteLevel.ITEM
        }
        
        db.getReference(FirebasePaths.widget(merchantId, widget.id))
            .setValue(widget.toMap())
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(false) 
            }
    }
    
    fun saveWidgetsBatch(merchantId: String, widgets: List<WidgetConfig>, callback: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any?>()
        
        widgets.forEach { widget ->
            val basePath = "${FirebasePaths.widgets(merchantId)}/${widget.id}"
            updates["$basePath/type"] = widget.type.name
            updates["$basePath/label"] = widget.label
            updates["$basePath/isEnabled"] = widget.isEnabled
            updates["$basePath/isRequired"] = widget.isRequired
            updates["$basePath/showInFilter"] = widget.showInFilter
            updates["$basePath/order"] = widget.order
            // Task 13: Ensure level is always saved to prevent item/order widget mixing
            updates["$basePath/level"] = widget.level.name
            
            widget.options.forEach { opt ->
                updates["$basePath/options/${opt.id}/label"] = opt.label
                updates["$basePath/options/${opt.id}/value"] = opt.value
                updates["$basePath/options/${opt.id}/isDefault"] = opt.isDefault
                if (opt.color != null) {
                    updates["$basePath/options/${opt.id}/color"] = opt.color
                }
            }
        }
        
        db.reference.updateChildren(updates)
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(false) 
            }
    }
    
    fun deleteWidget(merchantId: String, widgetId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.widget(merchantId, widgetId))
            .removeValue()
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { callback(false) }
    }
    
    /**
     * Atomically replace ALL widgets with the provided list.
     * This uses setValue() which completely replaces the widgets node,
     * ensuring no stale widgets remain.
     * 
     * @deprecated Use replaceItemWidgets() or replaceOrderWidgets() for level-specific operations
     * to avoid race conditions when resetting one level while the other is being modified.
     */
    @Deprecated("Use replaceItemWidgets() or replaceOrderWidgets() for level-specific operations")
    fun replaceAllWidgets(merchantId: String, widgets: List<WidgetConfig>, callback: (Boolean) -> Unit) {
        val widgetsMap = mutableMapOf<String, Any>()
        widgets.forEach { widget ->
            widgetsMap[widget.id] = widget.toMap()
        }
        
        db.getReference(FirebasePaths.widgets(merchantId))
            .setValue(widgetsMap)
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
    
    /**
     * Replace only ITEM-level widgets, leaving ORDER widgets untouched.
     * Uses Firebase query to delete existing ITEM widgets, then adds new ones atomically.
     * This eliminates race conditions when resetting item widgets while order widgets are being modified.
     */
    fun replaceItemWidgets(merchantId: String, itemWidgets: List<WidgetConfig>, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ITEM.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any?>()
                
                // Delete all existing ITEM widgets
                snapshot.children.forEach { child ->
                    updates["${FirebasePaths.widgets(merchantId)}/${child.key}"] = null
                }
                
                // Add new ITEM widgets
                itemWidgets.forEach { widget ->
                    val widgetPath = "${FirebasePaths.widgets(merchantId)}/${widget.id}"
                    updates[widgetPath] = widget.toMap()
                }
                
                // Atomic update
                db.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        updateTimestamp(merchantId)
                        callback(true)
                    }
                    .addOnFailureListener { 
                        it.printStackTrace()
                        callback(false) 
                    }
            }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(false) 
            }
    }
    
    /**
     * Replace only ORDER-level widgets, leaving ITEM widgets untouched.
     * Uses Firebase query to delete existing ORDER widgets, then adds new ones atomically.
     * This eliminates race conditions when resetting order widgets while item widgets are being modified.
     */
    fun replaceOrderWidgets(merchantId: String, orderWidgets: List<WidgetConfig>, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ORDER.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any?>()
                
                // Delete all existing ORDER widgets
                snapshot.children.forEach { child ->
                    updates["${FirebasePaths.widgets(merchantId)}/${child.key}"] = null
                }
                
                // Add new ORDER widgets
                orderWidgets.forEach { widget ->
                    val widgetPath = "${FirebasePaths.widgets(merchantId)}/${widget.id}"
                    updates[widgetPath] = widget.toMap()
                }
                
                // Atomic update
                db.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        updateTimestamp(merchantId)
                        callback(true)
                    }
                    .addOnFailureListener { 
                        it.printStackTrace()
                        callback(false) 
                    }
            }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(false) 
            }
    }
    
    // ==================== Options ====================
    
    fun addOption(merchantId: String, widgetId: String, option: WidgetOption, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.option(merchantId, widgetId, option.id))
            .setValue(option.toMap())
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { callback(false) }
    }
    
    fun deleteOption(merchantId: String, widgetId: String, optionId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.option(merchantId, widgetId, optionId))
            .removeValue()
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { callback(false) }
    }
    
    // #78: Removed Legacy Data section - migration no longer needed
    
    // ==================== Initialization ====================
    
    fun initializeMerchant(merchantId: String, widgets: List<WidgetConfig>, settings: PopupSettings, callback: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any?>()
        val advancedDefaults = AdvancedSettings()  // #78: Use data class defaults
        
        // Meta
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.SCHEMA_VERSION}"] = MerchantMeta.CURRENT_SCHEMA_VERSION
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.CREATED_AT}"] = ServerValue.TIMESTAMP
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.UPDATED_AT}"] = ServerValue.TIMESTAMP
        
        // PopupSettings
        updates["${FirebasePaths.settings(merchantId)}/triggerOnItemAdd"] = settings.triggerOnItemAdd
        updates["${FirebasePaths.settings(merchantId)}/showOMButtonInRegister"] = settings.showOMButtonInRegister
        
        // #78: AdvancedSettings defaults - store in DB for new merchants
        updates["${FirebasePaths.settings(merchantId)}/useOrderMateInRegister"] = advancedDefaults.useOrderMateInRegister
        updates["${FirebasePaths.settings(merchantId)}/useOrderMateRegisterInstead"] = advancedDefaults.useOrderMateRegisterInstead
        updates["${FirebasePaths.settings(merchantId)}/scheduledNotificationsEnabled"] = advancedDefaults.scheduledNotificationsEnabled
        updates["${FirebasePaths.settings(merchantId)}/notificationDays"] = advancedDefaults.notificationDays
        updates["${FirebasePaths.settings(merchantId)}/notificationMinutes"] = advancedDefaults.notificationMinutes
        updates["${FirebasePaths.settings(merchantId)}/scheduledReceiptEnabled"] = advancedDefaults.scheduledReceiptEnabled
        updates["${FirebasePaths.settings(merchantId)}/receiptDays"] = advancedDefaults.receiptDays
        updates["${FirebasePaths.settings(merchantId)}/receiptMinutes"] = advancedDefaults.receiptMinutes
        updates["${FirebasePaths.settings(merchantId)}/printNotesOnCustomerReceipts"] = advancedDefaults.printNotesOnCustomerReceipts
        updates["${FirebasePaths.settings(merchantId)}/printNotesOnOrderReceipts"] = advancedDefaults.printNotesOnOrderReceipts
        
        // #79: Permission Settings defaults
        updates["${FirebasePaths.settings(merchantId)}/allowAdminUpdateSettings"] = advancedDefaults.allowAdminUpdateSettings
        updates["${FirebasePaths.settings(merchantId)}/allowManagersUpdateSettings"] = advancedDefaults.allowManagersUpdateSettings
        updates["${FirebasePaths.settings(merchantId)}/allowEmployeesUpdateSettings"] = advancedDefaults.allowEmployeesUpdateSettings
        
        // Widgets
        widgets.forEach { widget ->
            val path = "${FirebasePaths.widgets(merchantId)}/${widget.id}"
            updates["$path/type"] = widget.type.name
            updates["$path/label"] = widget.label
            updates["$path/isEnabled"] = widget.isEnabled
            updates["$path/isRequired"] = widget.isRequired
            updates["$path/showInFilter"] = widget.showInFilter
            updates["$path/order"] = widget.order
            
            widget.options.forEach { opt ->
                updates["$path/options/${opt.id}/label"] = opt.label
                updates["$path/options/${opt.id}/value"] = opt.value
                updates["$path/options/${opt.id}/isDefault"] = opt.isDefault
                if (opt.color != null) {
                    updates["$path/options/${opt.id}/color"] = opt.color
                }
            }
        }
        
        db.reference.updateChildren(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(false) 
            }
    }
    
    fun merchantExists(merchantId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.meta(merchantId))
            .child(FirebasePaths.SCHEMA_VERSION)
            .get()
            .addOnSuccessListener { callback(it.exists()) }
            .addOnFailureListener { callback(false) }
    }
    
    // ==================== Notification Templates ====================
    
    fun getTemplates(merchantId: String, callback: (List<NotificationTemplate>) -> Unit) {
        db.getReference(FirebasePaths.templates(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val templates = mutableListOf<NotificationTemplate>()
                snapshot.children.forEach { child ->
                    try {
                        val id = child.key ?: return@forEach
                        val name = child.child("name").getValue(String::class.java) ?: "Untitled"
                        val content = child.child("content").getValue(String::class.java) ?: ""
                        val subject = child.child("subject").getValue(String::class.java) ?: ""  // #64
                        templates.add(NotificationTemplate(id, name, content, subject))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback(templates)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(emptyList())
            }
    }
    
    fun saveTemplate(merchantId: String, template: NotificationTemplate, callback: (Boolean) -> Unit) {
        val data = mapOf(
            "name" to template.name,
            "content" to template.content,
            "subject" to template.subject  // #64: Save email subject line
        )
        db.getReference(FirebasePaths.template(merchantId, template.id))
            .setValue(data)
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
    
    fun deleteTemplate(merchantId: String, templateId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.template(merchantId, templateId))
            .removeValue()
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener { callback(false) }
    }
    
    // ==================== Advanced Settings ====================
    
    fun getAdvancedSettings(merchantId: String, callback: (AdvancedSettings) -> Unit) {
        // #78: Use AdvancedSettings defaults for consistency
        val defaults = AdvancedSettings()
        
        db.getReference(FirebasePaths.settings(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val settings = AdvancedSettings(
                    useOrderMateInRegister = snapshot.child("useOrderMateInRegister").getValue(Boolean::class.java) ?: defaults.useOrderMateInRegister,
                    useOrderMateRegisterInstead = snapshot.child("useOrderMateRegisterInstead").getValue(Boolean::class.java) ?: defaults.useOrderMateRegisterInstead,
                    scheduledNotificationsEnabled = snapshot.child("scheduledNotificationsEnabled").getValue(Boolean::class.java) ?: defaults.scheduledNotificationsEnabled,
                    notificationDays = snapshot.child("notificationDays").getValue(Int::class.java) ?: defaults.notificationDays,
                    notificationMinutes = snapshot.child("notificationMinutes").getValue(Int::class.java) ?: defaults.notificationMinutes,
                    scheduledReceiptEnabled = snapshot.child("scheduledReceiptEnabled").getValue(Boolean::class.java) ?: defaults.scheduledReceiptEnabled,
                    receiptDays = snapshot.child("receiptDays").getValue(Int::class.java) ?: defaults.receiptDays,
                    receiptMinutes = snapshot.child("receiptMinutes").getValue(Int::class.java) ?: defaults.receiptMinutes,
                    printNotesOnCustomerReceipts = snapshot.child("printNotesOnCustomerReceipts").getValue(Boolean::class.java) ?: defaults.printNotesOnCustomerReceipts,
                    printNotesOnOrderReceipts = snapshot.child("printNotesOnOrderReceipts").getValue(Boolean::class.java) ?: defaults.printNotesOnOrderReceipts,
                    // #79: Permission Settings
                    allowAdminUpdateSettings = snapshot.child("allowAdminUpdateSettings").getValue(Boolean::class.java) ?: defaults.allowAdminUpdateSettings,
                    allowManagersUpdateSettings = snapshot.child("allowManagersUpdateSettings").getValue(Boolean::class.java) ?: defaults.allowManagersUpdateSettings,
                    allowEmployeesUpdateSettings = snapshot.child("allowEmployeesUpdateSettings").getValue(Boolean::class.java) ?: defaults.allowEmployeesUpdateSettings
                )
                callback(settings)
            }
            .addOnFailureListener {
                callback(AdvancedSettings())
            }
    }
    
    fun saveAdvancedSettings(merchantId: String, settings: AdvancedSettings, callback: (Boolean) -> Unit) {
        val updates = mapOf<String, Any>(
            "useOrderMateInRegister" to settings.useOrderMateInRegister,
            "useOrderMateRegisterInstead" to settings.useOrderMateRegisterInstead,
            "scheduledNotificationsEnabled" to settings.scheduledNotificationsEnabled,
            "notificationDays" to settings.notificationDays,
            "notificationMinutes" to settings.notificationMinutes,
            "scheduledReceiptEnabled" to settings.scheduledReceiptEnabled,
            "receiptDays" to settings.receiptDays,
            "receiptMinutes" to settings.receiptMinutes,
            "printNotesOnCustomerReceipts" to settings.printNotesOnCustomerReceipts,
            "printNotesOnOrderReceipts" to settings.printNotesOnOrderReceipts,
            // #79: Permission Settings
            "allowAdminUpdateSettings" to settings.allowAdminUpdateSettings,
            "allowManagersUpdateSettings" to settings.allowManagersUpdateSettings,
            "allowEmployeesUpdateSettings" to settings.allowEmployeesUpdateSettings
        )
        db.getReference(FirebasePaths.settings(merchantId))
            .updateChildren(updates)
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
    
    // ==================== #81: Employee Profiles (Per-Employee) ====================
    
    /**
     * Get employee profile from Firebase
     * Each employee has their own color and avatar settings
     */
    fun getEmployeeProfile(merchantId: String, employeeId: String, callback: (EmployeeProfile) -> Unit) {
        db.getReference(FirebasePaths.employeeProfile(merchantId, employeeId))
            .get()
            .addOnSuccessListener { snapshot ->
                val profile = EmployeeProfile(
                    color = snapshot.child("color").getValue(String::class.java) ?: EmployeeProfile.DEFAULT_COLOR,
                    avatar = snapshot.child("avatar").getValue(String::class.java) ?: EmployeeProfile.DEFAULT_AVATAR
                )
                callback(profile)
            }
            .addOnFailureListener {
                callback(EmployeeProfile())
            }
    }
    
    /**
     * Save employee profile to Firebase
     */
    fun saveEmployeeProfile(merchantId: String, employeeId: String, profile: EmployeeProfile, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.employeeProfile(merchantId, employeeId))
            .updateChildren(profile.toMap())
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
    
    // ==================== #81: Referrals ====================
    
    /**
     * Get all referrals for a merchant
     */
    fun getReferrals(merchantId: String, callback: (List<ReferralInfo>) -> Unit) {
        db.getReference(FirebasePaths.referrals(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val referrals = mutableListOf<ReferralInfo>()
                snapshot.children.forEach { child ->
                    val referral = parseReferral(child)
                    if (referral != null) referrals.add(referral)
                }
                callback(referrals.sortedByDescending { it.submittedAt })
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    
    /**
     * Get a single referral by ID
     */
    fun getReferral(merchantId: String, referralId: String, callback: (ReferralInfo?) -> Unit) {
        db.getReference(FirebasePaths.referral(merchantId, referralId))
            .get()
            .addOnSuccessListener { snapshot ->
                callback(parseReferral(snapshot))
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    
    /**
     * Save a new referral
     */
    fun saveReferral(merchantId: String, referral: ReferralInfo, callback: (Boolean) -> Unit) {
        val referralWithId = if (referral.id.isEmpty()) {
            referral.copy(id = ReferralInfo.generateId())
        } else {
            referral
        }
        
        db.getReference(FirebasePaths.referral(merchantId, referralWithId.id))
            .setValue(referralWithId.toMap())
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
    
    /**
     * Check if merchant has any referrals
     */
    fun hasAnyReferral(merchantId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.referrals(merchantId))
            .limitToFirst(1)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.exists() && snapshot.childrenCount > 0)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
    
    private fun parseReferral(snapshot: DataSnapshot): ReferralInfo? {
        if (!snapshot.exists()) return null
        return ReferralInfo(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            partnerName = snapshot.child("partnerName").getValue(String::class.java) ?: "",
            submittedAt = snapshot.child("submittedAt").getValue(Long::class.java) ?: 0,
            submittedBy = snapshot.child("submittedBy").getValue(String::class.java) ?: ""
        )
    }
    
    // ==================== #81: Discounts (Read-only in app) ====================
    
    /**
     * Get all discounts for a merchant
     */
    fun getDiscounts(merchantId: String, callback: (List<MerchantDiscount>) -> Unit) {
        db.getReference(FirebasePaths.discounts(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val discounts = mutableListOf<MerchantDiscount>()
                snapshot.children.forEach { child ->
                    val discount = parseDiscount(child)
                    if (discount != null) discounts.add(discount)
                }
                callback(discounts.sortedByDescending { it.createdAt })
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    
    /**
     * Get only active (valid) discounts
     */
    fun getActiveDiscounts(merchantId: String, callback: (List<MerchantDiscount>) -> Unit) {
        getDiscounts(merchantId) { discounts ->
            callback(discounts.filter { it.isValid() })
        }
    }
    
    /**
     * Get a single discount by ID
     */
    fun getDiscount(merchantId: String, discountId: String, callback: (MerchantDiscount?) -> Unit) {
        db.getReference(FirebasePaths.discount(merchantId, discountId))
            .get()
            .addOnSuccessListener { snapshot ->
                callback(parseDiscount(snapshot))
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    
    /**
     * Get total active discount amount
     */
    fun getTotalActiveDiscount(merchantId: String, callback: (Double) -> Unit) {
        getActiveDiscounts(merchantId) { discounts ->
            callback(discounts.sumOf { it.amount })
        }
    }
    
    private fun parseDiscount(snapshot: DataSnapshot): MerchantDiscount? {
        if (!snapshot.exists()) return null
        return MerchantDiscount(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            amount = snapshot.child("amount").getValue(Double::class.java) ?: 0.0,
            startDate = snapshot.child("startDate").getValue(Long::class.java) ?: 0,
            endDate = snapshot.child("endDate").getValue(Long::class.java) ?: 0,
            discountCode = snapshot.child("discountCode").getValue(String::class.java),
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0,
            isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: true
        )
    }
    
    // ==================== Legacy Profile Settings (Deprecated) ====================
    
    /**
     * @deprecated Use getEmployeeProfile() instead
     */
    @Deprecated("Use getEmployeeProfile() for per-employee profiles", 
        ReplaceWith("getEmployeeProfile(merchantId, employeeId, callback)"))
    fun getProfileSettings(merchantId: String, callback: (ProfileSettings) -> Unit) {
        @Suppress("DEPRECATION")
        db.getReference(FirebasePaths.profileSettings(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val settings = ProfileSettings(
                    themeColor = snapshot.child("themeColor").getValue(String::class.java) ?: ProfileSettings.DEFAULT_THEME_COLOR,
                    avatar = snapshot.child("avatar").getValue(String::class.java) ?: ProfileSettings.DEFAULT_AVATAR
                )
                callback(settings)
            }
            .addOnFailureListener {
                callback(ProfileSettings())
            }
    }
    
    /**
     * @deprecated Use saveEmployeeProfile() instead
     */
    @Deprecated("Use saveEmployeeProfile() for per-employee profiles",
        ReplaceWith("saveEmployeeProfile(merchantId, employeeId, profile, callback)"))
    fun saveProfileSettings(merchantId: String, settings: ProfileSettings, callback: (Boolean) -> Unit) {
        val updates = mapOf<String, Any>(
            "themeColor" to settings.themeColor,
            "avatar" to settings.avatar
        )
        @Suppress("DEPRECATION")
        db.getReference(FirebasePaths.profileSettings(merchantId))
            .updateChildren(updates)
            .addOnSuccessListener {
                updateTimestamp(merchantId)
                callback(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }
}

/**
 * Notification template data class
 */
data class NotificationTemplate(
    val id: String,
    var name: String,
    var content: String,
    var subject: String = ""  // #64: Email subject line field
) {
    companion object {
        fun create(name: String = "New Template", content: String = "", subject: String = ""): NotificationTemplate {
            return NotificationTemplate(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                content = content,
                subject = subject
            )
        }
    }
}

/**
 * Advanced settings data class
 * #78: Updated defaults - scheduledNotificationsEnabled=true, printNotesOnCustomerReceipts=true
 */
data class AdvancedSettings(
    val useOrderMateInRegister: Boolean = false,
    val useOrderMateRegisterInstead: Boolean = true,
    val scheduledNotificationsEnabled: Boolean = true,  // #78: Enable by default
    val notificationDays: Int = 3,  // #78: 3 days before due date
    val notificationMinutes: Int = 0,
    val scheduledReceiptEnabled: Boolean = false,
    val receiptDays: Int = 0,
    val receiptMinutes: Int = 60,
    val printNotesOnCustomerReceipts: Boolean = true,  // #78: Enable by default
    val printNotesOnOrderReceipts: Boolean = true,
    // #79: Permission Settings - control who can access settings
    val allowAdminUpdateSettings: Boolean = true,
    val allowManagersUpdateSettings: Boolean = true,
    val allowEmployeesUpdateSettings: Boolean = true
)

/**
 * Profile settings data class (#85 requirement)
 */
data class ProfileSettings(
    val themeColor: String = DEFAULT_THEME_COLOR,
    val avatar: String = DEFAULT_AVATAR
) {
    companion object {
        const val DEFAULT_THEME_COLOR = "#1C3527"  // HTML default: rgb(60, 75, 128)
        const val DEFAULT_AVATAR = "😊"
    }
}
