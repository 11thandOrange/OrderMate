package com.orderMate.utils.migrations

import android.content.Context
import android.util.Log
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.order.LineItem
import com.orderMate.modals.*
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import java.util.UUID

/**
 * Migrates legacy Clover order notes to V2 widget configuration.
 * 
 * This migrator:
 * 1. Reads all orders from Clover (via OrderConnector)
 * 2. Analyzes legacy notes (parses label:value format)
 * 3. Creates V2 widgets in Firebase based on found labels/values
 * 4. Verifies Firebase save succeeded
 * 
 * Legacy note format: "label1:value1|label2:value2"
 * V2 note format: "[widgetId]label:value|[widgetId]label:value"
 * 
 * NOTE: This does NOT modify Clover orders - only creates widget config in Firebase.
 */
object CloverNotesToV2Migrator {
    
    private const val TAG = "CloverNotesToV2Migrator"
    
    // Known legacy label mappings to widget types
    private val LABEL_TYPE_MAP = mapOf(
        "pickup date" to WidgetType.CALENDAR,
        "due date" to WidgetType.CALENDAR,
        "status" to WidgetType.SINGLE_SELECT,
        "category" to WidgetType.SINGLE_SELECT,
        "subcategory" to WidgetType.MULTI_SELECT,
        "type" to WidgetType.MULTI_SELECT,
        "description" to WidgetType.TEXT_BOX,
        "notes" to WidgetType.TEXT_BOX
    )
    
    data class MigrationResult(
        val success: Boolean,
        val ordersAnalyzed: Int,
        val itemsAnalyzed: Int,
        val legacyNotesFound: Int,
        val widgetsCreated: Int,
        val errors: List<String>
    )
    
    data class LabelStats(
        val label: String,
        val occurrences: Int,
        val uniqueValues: MutableSet<String>,
        var inferredType: WidgetType
    )
    
    /**
     * Run the full migration
     */
    fun migrate(context: Context, merchantId: String, callback: (MigrationResult) -> Unit) {
        Log.d(TAG, "Starting Clover notes migration for merchant: $merchantId")
        
        val errors = mutableListOf<String>()
        
        // Step 1: Read orders from Clover
        Log.d(TAG, "Step 1: Reading orders from Clover...")
        readCloverOrders(context) { orders ->
            if (orders == null) {
                errors.add("Failed to read orders from Clover")
                callback(MigrationResult(false, 0, 0, 0, 0, errors))
                return@readCloverOrders
            }
            
            Log.d(TAG, "Loaded ${orders.size} orders from Clover")
            
            // Step 2: Analyze legacy notes
            Log.d(TAG, "Step 2: Analyzing legacy notes...")
            val analysisResult = analyzeNotes(orders)
            
            Log.d(TAG, "Found ${analysisResult.legacyNotesCount} legacy notes")
            Log.d(TAG, "Found ${analysisResult.labelStats.size} unique labels")
            
            if (analysisResult.labelStats.isEmpty()) {
                Log.d(TAG, "No legacy notes found - nothing to migrate")
                callback(MigrationResult(
                    success = true,
                    ordersAnalyzed = orders.size,
                    itemsAnalyzed = analysisResult.itemsAnalyzed,
                    legacyNotesFound = 0,
                    widgetsCreated = 0,
                    errors = emptyList()
                ))
                return@readCloverOrders
            }
            
            // Step 3: Create V2 widgets
            Log.d(TAG, "Step 3: Creating V2 widgets...")
            val widgets = createWidgetsFromLabels(analysisResult.labelStats)
            
            Log.d(TAG, "Created ${widgets.size} widgets")
            
            // Validate widgets before saving
            val validationErrors = validateWidgets(widgets)
            if (validationErrors.isNotEmpty()) {
                errors.addAll(validationErrors)
                callback(MigrationResult(
                    success = false,
                    ordersAnalyzed = orders.size,
                    itemsAnalyzed = analysisResult.itemsAnalyzed,
                    legacyNotesFound = analysisResult.legacyNotesCount,
                    widgetsCreated = 0,
                    errors = errors
                ))
                return@readCloverOrders
            }
            
            // Save to Firebase
            saveWidgetsToFirebase(merchantId, widgets) { saveSuccess ->
                if (!saveSuccess) {
                    errors.add("Failed to save widgets to Firebase")
                    callback(MigrationResult(
                        success = false,
                        ordersAnalyzed = orders.size,
                        itemsAnalyzed = analysisResult.itemsAnalyzed,
                        legacyNotesFound = analysisResult.legacyNotesCount,
                        widgetsCreated = 0,
                        errors = errors
                    ))
                    return@saveWidgetsToFirebase
                }
                
                // Step 4: Verify Firebase save
                Log.d(TAG, "Step 4: Verifying Firebase save...")
                verifyFirebaseSave(merchantId, widgets.size) { verifySuccess ->
                    if (!verifySuccess) {
                        errors.add("Firebase verification failed")
                    }
                    
                    val finalSuccess = saveSuccess && verifySuccess && errors.isEmpty()
                    Log.d(TAG, "Migration ${if (finalSuccess) "SUCCEEDED" else "FAILED"}")
                    
                    callback(MigrationResult(
                        success = finalSuccess,
                        ordersAnalyzed = orders.size,
                        itemsAnalyzed = analysisResult.itemsAnalyzed,
                        legacyNotesFound = analysisResult.legacyNotesCount,
                        widgetsCreated = if (finalSuccess) widgets.size else 0,
                        errors = errors
                    ))
                }
            }
        }
    }
    
    /**
     * Read all orders from Clover
     */
    private fun readCloverOrders(context: Context, callback: (List<Order>?) -> Unit) {
        try {
            val app = context.applicationContext as MyApp
            val orderConnector = app.getOrderConnector()
            
            // Run on background thread
            Thread {
                try {
                    val orders = orderConnector.getOrders(mutableListOf())
                    callback(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching orders: ${e.message}")
                    callback(null)
                }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order connector: ${e.message}")
            callback(null)
        }
    }
    
    data class AnalysisResult(
        val itemsAnalyzed: Int,
        val legacyNotesCount: Int,
        val labelStats: Map<String, LabelStats>
    )
    
    /**
     * Analyze all legacy notes from orders
     */
    private fun analyzeNotes(orders: List<Order>): AnalysisResult {
        val labelStats = mutableMapOf<String, LabelStats>()
        var itemsAnalyzed = 0
        var legacyNotesCount = 0
        
        for (order in orders) {
            // Analyze item-level notes (legacy notes are on line items)
            order.lineItems?.forEach { item ->
                itemsAnalyzed++
                val note = item.note
                if (!note.isNullOrBlank()) {
                    val parsed = parseLegacyNote(note)
                    if (parsed.isNotEmpty()) {
                        legacyNotesCount++
                        for ((label, value) in parsed) {
                            val normalizedLabel = label.trim()
                            val stats = labelStats.getOrPut(normalizedLabel.lowercase()) {
                                LabelStats(
                                    label = normalizedLabel,
                                    occurrences = 0,
                                    uniqueValues = mutableSetOf(),
                                    inferredType = inferWidgetType(normalizedLabel)
                                )
                            }
                            stats.uniqueValues.add(value.trim())
                            labelStats[normalizedLabel.lowercase()] = stats.copy(
                                occurrences = stats.occurrences + 1,
                                uniqueValues = stats.uniqueValues
                            )
                        }
                    }
                }
            }
        }
        
        return AnalysisResult(itemsAnalyzed, legacyNotesCount, labelStats)
    }
    
    /**
     * Parse legacy note format: "label1:value1|label2:value2"
     * Returns map of label -> value
     */
    private fun parseLegacyNote(note: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Skip if already V2 format (contains [uuid])
        if (note.contains(Regex("\\[[a-f0-9-]{36}\\]"))) {
            return result
        }
        
        // Split by | and parse each segment
        val segments = note.split("|")
        for (segment in segments) {
            val colonIndex = segment.indexOf(':')
            if (colonIndex > 0) {
                val label = segment.substring(0, colonIndex).trim()
                val value = segment.substring(colonIndex + 1).trim()
                if (label.isNotBlank() && value.isNotBlank()) {
                    result[label] = value
                }
            }
        }
        
        return result
    }
    
    /**
     * Infer widget type from label name
     */
    private fun inferWidgetType(label: String): WidgetType {
        val normalized = label.lowercase().trim()
        return LABEL_TYPE_MAP[normalized] ?: WidgetType.SINGLE_SELECT
    }
    
    /**
     * Create V2 widgets from collected label stats
     */
    private fun createWidgetsFromLabels(labelStats: Map<String, LabelStats>): List<WidgetConfig> {
        val widgets = mutableListOf<WidgetConfig>()
        var order = 0
        
        for ((_, stats) in labelStats) {
            val options = if (stats.inferredType == WidgetType.SINGLE_SELECT || 
                              stats.inferredType == WidgetType.MULTI_SELECT) {
                stats.uniqueValues.mapIndexed { index, value ->
                    WidgetOption(
                        id = UUID.randomUUID().toString(),
                        label = value,
                        value = value,
                        isDefault = index == 0,
                        color = null
                    )
                }.toMutableList()
            } else {
                mutableListOf()
            }
            
            val widget = WidgetConfig(
                id = UUID.randomUUID().toString(),
                type = stats.inferredType,
                label = stats.label,
                isEnabled = true,
                isRequired = false,
                showInFilter = stats.inferredType != WidgetType.TEXT_BOX,
                order = order++,
                level = NoteLevel.ITEM,
                options = options
            )
            
            widgets.add(widget)
            Log.d(TAG, "Created widget: ${widget.label} (${widget.type}) with ${widget.options.size} options")
        }
        
        return widgets
    }
    
    /**
     * Validate widgets before saving
     */
    private fun validateWidgets(widgets: List<WidgetConfig>): List<String> {
        val errors = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()
        val seenLabels = mutableSetOf<String>()
        
        for (widget in widgets) {
            // Check for duplicate IDs
            if (widget.id in seenIds) {
                errors.add("Duplicate widget ID: ${widget.id}")
            }
            seenIds.add(widget.id)
            
            // Check for duplicate labels
            val normalizedLabel = widget.label.lowercase()
            if (normalizedLabel in seenLabels) {
                errors.add("Duplicate widget label: ${widget.label}")
            }
            seenLabels.add(normalizedLabel)
            
            // Validate label not empty
            if (widget.label.isBlank()) {
                errors.add("Widget has empty label")
            }
            
            // Validate options for select widgets
            if (widget.type == WidgetType.SINGLE_SELECT || widget.type == WidgetType.MULTI_SELECT) {
                val optionIds = mutableSetOf<String>()
                for (option in widget.options) {
                    if (option.id in optionIds) {
                        errors.add("Duplicate option ID in widget ${widget.label}: ${option.id}")
                    }
                    optionIds.add(option.id)
                    
                    if (option.label.isBlank()) {
                        errors.add("Empty option label in widget ${widget.label}")
                    }
                }
            }
        }
        
        return errors
    }
    
    /**
     * Save widgets to Firebase
     */
    private fun saveWidgetsToFirebase(
        merchantId: String, 
        widgets: List<WidgetConfig>, 
        callback: (Boolean) -> Unit
    ) {
        val firebase = FirebaseConfigManager.getInstance()
        
        // Save each widget
        var savedCount = 0
        var failed = false
        
        if (widgets.isEmpty()) {
            callback(true)
            return
        }
        
        for (widget in widgets) {
            firebase.saveWidget(merchantId, widget) { success ->
                if (!success) {
                    failed = true
                    Log.e(TAG, "Failed to save widget: ${widget.label}")
                }
                savedCount++
                
                if (savedCount == widgets.size) {
                    callback(!failed)
                }
            }
        }
    }
    
    /**
     * Verify widgets were saved correctly to Firebase
     */
    private fun verifyFirebaseSave(
        merchantId: String, 
        expectedCount: Int, 
        callback: (Boolean) -> Unit
    ) {
        val firebase = FirebaseConfigManager.getInstance()
        
        firebase.getWidgets(merchantId) { widgets ->
            val actualCount = widgets.size
            val success = actualCount >= expectedCount
            
            if (success) {
                Log.d(TAG, "Firebase verification passed: $actualCount widgets found")
            } else {
                Log.e(TAG, "Firebase verification failed: expected $expectedCount, found $actualCount")
            }
            
            callback(success)
        }
    }
}
