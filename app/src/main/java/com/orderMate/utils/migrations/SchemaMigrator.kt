package com.orderMate.utils.migrations

import com.orderMate.modals.MerchantMeta
import com.orderMate.modals.PopupSettings
import com.orderMate.utils.DefaultWidgetFactory
import com.orderMate.utils.FirebaseConfigManager

/**
 * Coordinates schema migrations
 * Checks current schema version and migrates if needed
 * 
 * IMPORTANT: Never overwrites existing widget IDs - they are embedded in notes.
 */
object SchemaMigrator {
    
    private const val TAG = "SchemaMigrator"
    
    interface MigrationCallback {
        fun onSuccess()
        fun onFailure(error: String)
        fun onProgress(message: String)
    }
    
    /**
     * Check and run migrations if needed
     * Should be called on app startup after merchant ID is available
     */
    fun migrateIfNeeded(merchantId: String, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        firebase.getSchemaVersion(merchantId) { version ->
            android.util.Log.d(TAG, "Current schema version: $version")
            
            when {
                version == 0 -> {
                    // No schema version - check for legacy data or create new
                    checkLegacyAndMigrate(merchantId, callback)
                }
                version < MerchantMeta.CURRENT_SCHEMA_VERSION -> {
                    // Need to migrate from older version
                    migrateFromVersion(merchantId, version, callback)
                }
                else -> {
                    // Already at current version
                    android.util.Log.d(TAG, "Schema is current (v$version)")
                    callback(true)
                }
            }
        }
    }
    
    /**
     * Check if legacy data exists and migrate, otherwise create defaults
     * Only creates defaults if merchant has NO existing widgets in Firebase.
     */
    private fun checkLegacyAndMigrate(merchantId: String, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        android.util.Log.d(TAG, "Checking for legacy data...")
        
        firebase.legacyDataExists(merchantId) { legacyExists ->
            if (legacyExists) {
                android.util.Log.d(TAG, "Legacy data found, migrating...")
                V1ToV2Migrator.migrate(merchantId) { success ->
                    if (success) {
                        android.util.Log.d(TAG, "Migration successful")
                        firebase.setSchemaVersion(merchantId, MerchantMeta.CURRENT_SCHEMA_VERSION, callback)
                    } else {
                        // Migration failed - check if widgets already exist before creating defaults
                        android.util.Log.e(TAG, "Migration failed, checking for existing widgets...")
                        checkAndCreateDefaultsIfNeeded(merchantId, callback)
                    }
                }
            } else {
                // No legacy data - check if V2 widgets already exist before creating defaults
                android.util.Log.d(TAG, "No legacy data, checking for existing widgets...")
                checkAndCreateDefaultsIfNeeded(merchantId, callback)
            }
        }
    }
    
    /**
     * Only create defaults if merchant has NO existing widgets.
     * This prevents overwriting existing widget IDs.
     */
    private fun checkAndCreateDefaultsIfNeeded(merchantId: String, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        // Check if widgets already exist in Firebase
        firebase.getWidgets(merchantId) { existingWidgets ->
            if (existingWidgets.isNotEmpty()) {
                // Widgets already exist - DON'T overwrite them!
                android.util.Log.d(TAG, "Found ${existingWidgets.size} existing widgets, preserving IDs")
                firebase.setSchemaVersion(merchantId, MerchantMeta.CURRENT_SCHEMA_VERSION, callback)
            } else {
                // No widgets exist - safe to create defaults
                android.util.Log.d(TAG, "No existing widgets, creating defaults...")
                createDefaults(merchantId, callback)
            }
        }
    }
    
    /**
     * Migrate from a specific version to current
     */
    private fun migrateFromVersion(merchantId: String, fromVersion: Int, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        android.util.Log.d(TAG, "Migrating from v$fromVersion to v${MerchantMeta.CURRENT_SCHEMA_VERSION}")
        
        when (fromVersion) {
            1 -> {
                // V1 to V2 migration
                V1ToV2Migrator.migrate(merchantId) { success ->
                    if (success) {
                        firebase.setSchemaVersion(merchantId, MerchantMeta.CURRENT_SCHEMA_VERSION, callback)
                    } else {
                        // Migration failed - don't blindly create defaults, check first
                        checkAndCreateDefaultsIfNeeded(merchantId, callback)
                    }
                }
            }
            else -> {
                // Unknown version - check for existing widgets before creating defaults
                android.util.Log.w(TAG, "Unknown schema version: $fromVersion")
                checkAndCreateDefaultsIfNeeded(merchantId, callback)
            }
        }
    }
    
    /**
     * Create default widgets and settings for new merchant.
     * Should ONLY be called when we've verified no widgets exist.
     */
    private fun createDefaults(merchantId: String, callback: (Boolean) -> Unit) {
        val defaults = DefaultWidgetFactory.createDefaults()
        val settings = PopupSettings()
        
        android.util.Log.d(TAG, "Creating ${defaults.size} default widgets")
        
        FirebaseConfigManager.getInstance().initializeMerchant(merchantId, defaults, settings) { success ->
            if (success) {
                android.util.Log.d(TAG, "Defaults created successfully")
            } else {
                android.util.Log.e(TAG, "Failed to create defaults")
            }
            callback(success)
        }
    }
    
    /**
     * Force reset to defaults (for debugging/testing)
     * WARNING: This will overwrite existing widget IDs! Use with caution.
     * @deprecated Use WidgetManager.resetItemWidgetsToDefaults() instead which preserves IDs.
     */
    @Deprecated("Use WidgetManager.resetItemWidgetsToDefaults() which preserves IDs")
    fun resetToDefaults(merchantId: String, callback: (Boolean) -> Unit) {
        createDefaults(merchantId, callback)
    }
}
