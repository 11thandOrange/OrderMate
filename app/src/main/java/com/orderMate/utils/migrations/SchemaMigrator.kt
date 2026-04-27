package com.orderMate.utils.migrations

import android.content.Context
import com.orderMate.modals.MerchantMeta
import com.orderMate.modals.PopupSettings
import com.orderMate.utils.DefaultWidgetFactory
import com.orderMate.utils.FirebaseConfigManager

/**
 * Coordinates schema migrations
 * Checks current schema version and migrates if needed
 */
object SchemaMigrator {
    
    private const val TAG = "SchemaMigrator"
    
    interface MigrationCallback {
        fun onSuccess()
        fun onFailure(error: String)
        fun onProgress(message: String)
    }
    
    /**
     * Run Clover notes migration
     * Reads orders from Clover, analyzes legacy notes, creates V2 widgets in Firebase
     */
    fun migrateCloverNotes(context: Context, merchantId: String, callback: (CloverNotesToV2Migrator.MigrationResult) -> Unit) {
        android.util.Log.d(TAG, "Starting Clover notes migration...")
        CloverNotesToV2Migrator.migrate(context, merchantId, callback)
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
     */
    private fun checkLegacyAndMigrate(merchantId: String, callback: (Boolean) -> Unit) {
        val firebase = FirebaseConfigManager.getInstance()
        
        android.util.Log.d(TAG, "Checking for legacy data...")
        
        firebase.legacyDataExists(merchantId) { exists ->
            if (exists) {
                android.util.Log.d(TAG, "Legacy data found, migrating...")
                V1ToV2Migrator.migrate(merchantId) { success ->
                    if (success) {
                        android.util.Log.d(TAG, "Migration successful")
                        firebase.setSchemaVersion(merchantId, MerchantMeta.CURRENT_SCHEMA_VERSION, callback)
                    } else {
                        android.util.Log.e(TAG, "Migration failed, creating defaults")
                        createDefaults(merchantId, callback)
                    }
                }
            } else {
                android.util.Log.d(TAG, "No legacy data, creating defaults...")
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
                        callback(false)
                    }
                }
            }
            else -> {
                // Unknown version, try to create defaults
                android.util.Log.w(TAG, "Unknown schema version: $fromVersion")
                createDefaults(merchantId, callback)
            }
        }
    }
    
    /**
     * Create default widgets and settings for new merchant
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
     */
    fun resetToDefaults(merchantId: String, callback: (Boolean) -> Unit) {
        createDefaults(merchantId, callback)
    }
}
