package com.orderMate.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import com.orderMate.modals.LegacyCustomItemJson
import com.orderMate.modals.MerchantMeta
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.PopupSettings
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType

/**
 * Firebase CRUD operations for new schema structure
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
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ORDER.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val widgets = snapshot.children.mapNotNull { parseWidget(it) }
                callback(widgets.sortedBy { it.order })
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(emptyList())
            }
    }
    
    /**
     * Get only ITEM level widgets (for item popup settings)
     */
    fun getItemWidgets(merchantId: String, callback: (List<WidgetConfig>) -> Unit) {
        db.getReference(FirebasePaths.widgets(merchantId))
            .orderByChild("level")
            .equalTo(NoteLevel.ITEM.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val widgets = snapshot.children.mapNotNull { parseWidget(it) }
                callback(widgets.sortedBy { it.order })
            }
            .addOnFailureListener {
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
    
    // ==================== Legacy Data ====================
    
    fun getLegacyData(merchantId: String, callback: (LegacyCustomItemJson?) -> Unit) {
        db.getReference(FirebasePaths.legacyData(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val jsonStr = snapshot.getValue(String::class.java)
                    if (jsonStr != null) {
                        val legacy = gson.fromJson(jsonStr, LegacyCustomItemJson::class.java)
                        callback(legacy)
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }
            .addOnFailureListener { 
                it.printStackTrace()
                callback(null) 
            }
    }
    
    fun legacyDataExists(merchantId: String, callback: (Boolean) -> Unit) {
        db.getReference(FirebasePaths.legacyData(merchantId))
            .get()
            .addOnSuccessListener { callback(it.exists()) }
            .addOnFailureListener { callback(false) }
    }
    
    // ==================== Initialization ====================
    
    fun initializeMerchant(merchantId: String, widgets: List<WidgetConfig>, settings: PopupSettings, callback: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any?>()
        
        // Meta
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.SCHEMA_VERSION}"] = MerchantMeta.CURRENT_SCHEMA_VERSION
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.CREATED_AT}"] = ServerValue.TIMESTAMP
        updates["${FirebasePaths.meta(merchantId)}/${FirebasePaths.UPDATED_AT}"] = ServerValue.TIMESTAMP
        
        // Settings
        updates["${FirebasePaths.settings(merchantId)}/triggerOnItemAdd"] = settings.triggerOnItemAdd
        updates["${FirebasePaths.settings(merchantId)}/showOMButtonInRegister"] = settings.showOMButtonInRegister
        
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
        db.getReference(FirebasePaths.settings(merchantId))
            .get()
            .addOnSuccessListener { snapshot ->
                val settings = AdvancedSettings(
                    useOrderMateInRegister = snapshot.child("useOrderMateInRegister").getValue(Boolean::class.java) ?: true,
                    scheduledNotificationsEnabled = snapshot.child("scheduledNotificationsEnabled").getValue(Boolean::class.java) ?: false,
                    notificationDays = snapshot.child("notificationDays").getValue(Int::class.java) ?: 3,
                    notificationMinutes = snapshot.child("notificationMinutes").getValue(Int::class.java) ?: 0,
                    scheduledReceiptEnabled = snapshot.child("scheduledReceiptEnabled").getValue(Boolean::class.java) ?: false,
                    receiptDays = snapshot.child("receiptDays").getValue(Int::class.java) ?: 0,
                    receiptMinutes = snapshot.child("receiptMinutes").getValue(Int::class.java) ?: 60
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
            "scheduledNotificationsEnabled" to settings.scheduledNotificationsEnabled,
            "notificationDays" to settings.notificationDays,
            "notificationMinutes" to settings.notificationMinutes,
            "scheduledReceiptEnabled" to settings.scheduledReceiptEnabled,
            "receiptDays" to settings.receiptDays,
            "receiptMinutes" to settings.receiptMinutes
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
    
    // ==================== Profile Settings ====================
    
    /**
     * Get profile settings from Firebase
     */
    fun getProfileSettings(merchantId: String, callback: (ProfileSettings) -> Unit) {
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
     * Save profile settings to Firebase
     */
    fun saveProfileSettings(merchantId: String, settings: ProfileSettings, callback: (Boolean) -> Unit) {
        val updates = mapOf<String, Any>(
            "themeColor" to settings.themeColor,
            "avatar" to settings.avatar
        )
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
 */
data class AdvancedSettings(
    val useOrderMateInRegister: Boolean = true,
    val scheduledNotificationsEnabled: Boolean = false,
    val notificationDays: Int = 3,
    val notificationMinutes: Int = 0,
    val scheduledReceiptEnabled: Boolean = false,
    val receiptDays: Int = 0,
    val receiptMinutes: Int = 60
)

/**
 * Profile settings data class (#85 requirement)
 */
data class ProfileSettings(
    val themeColor: String = DEFAULT_THEME_COLOR,
    val avatar: String = DEFAULT_AVATAR
) {
    companion object {
        const val DEFAULT_THEME_COLOR = "#3C4B80"  // HTML default: rgb(60, 75, 128)
        const val DEFAULT_AVATAR = "😊"
    }
}
