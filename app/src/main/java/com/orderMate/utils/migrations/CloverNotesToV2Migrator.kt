package com.orderMate.utils.migrations

import android.content.Context
import android.util.Log
import com.clover.sdk.v3.order.Order
import com.orderMate.modals.NoteLevel
import com.orderMate.modals.WidgetConfig
import com.orderMate.modals.WidgetOption
import com.orderMate.modals.WidgetType
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.OrderNoteParser
import java.util.UUID

/**
 * Migrates legacy Clover order notes to V2 widget configuration (#151).
 *
 * This migrator:
 * 1. Reads all orders from Clover (via OrderConnector)
 * 2. Reads the merchant's existing widgets from Firebase
 * 3. Parses every line-item note via OrderNoteParser (handles both the current
 *    "label:value • label:value" format and the older "[widgetId]label:value" format)
 * 4. For any label found that does NOT already match an existing widget (case-insensitive),
 *    treats it as unmigrated legacy data and creates a new V2 widget for it
 * 5. Verifies Firebase save succeeded
 *
 * Labels that already match an existing widget are left alone - this is what makes the
 * migrator safe to run against a mix of new-format and legacy-format notes: it only ever
 * fills in gaps, never touches or duplicates widgets that already exist.
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

        FirebaseConfigManager.getInstance().getWidgets(merchantId) { existingWidgets ->
            val knownLabels = existingWidgets.map { it.label.trim().lowercase() }.toSet()

            readCloverOrders(context) { orders ->
                if (orders == null) {
                    errors.add("Failed to read orders from Clover")
                    callback(MigrationResult(false, 0, 0, 0, 0, errors))
                    return@readCloverOrders
                }

                Log.d(TAG, "Loaded ${orders.size} orders from Clover")

                val analysisResult = analyzeNotes(orders, knownLabels)

                Log.d(TAG, "Found ${analysisResult.legacyNotesCount} unmigrated legacy notes")
                Log.d(TAG, "Found ${analysisResult.labelStats.size} unique unmigrated labels")

                if (analysisResult.labelStats.isEmpty()) {
                    Log.d(TAG, "No unmigrated legacy notes found - nothing to migrate")
                    callback(
                        MigrationResult(
                            success = true,
                            ordersAnalyzed = orders.size,
                            itemsAnalyzed = analysisResult.itemsAnalyzed,
                            legacyNotesFound = 0,
                            widgetsCreated = 0,
                            errors = emptyList()
                        )
                    )
                    return@readCloverOrders
                }

                val widgets = createWidgetsFromLabels(analysisResult.labelStats)

                Log.d(TAG, "Created ${widgets.size} widgets")

                val validationErrors = validateWidgets(widgets, knownLabels)
                if (validationErrors.isNotEmpty()) {
                    errors.addAll(validationErrors)
                    callback(
                        MigrationResult(
                            success = false,
                            ordersAnalyzed = orders.size,
                            itemsAnalyzed = analysisResult.itemsAnalyzed,
                            legacyNotesFound = analysisResult.legacyNotesCount,
                            widgetsCreated = 0,
                            errors = errors
                        )
                    )
                    return@readCloverOrders
                }

                saveWidgetsToFirebase(merchantId, widgets) { saveSuccess ->
                    if (!saveSuccess) {
                        errors.add("Failed to save widgets to Firebase")
                        callback(
                            MigrationResult(
                                success = false,
                                ordersAnalyzed = orders.size,
                                itemsAnalyzed = analysisResult.itemsAnalyzed,
                                legacyNotesFound = analysisResult.legacyNotesCount,
                                widgetsCreated = 0,
                                errors = errors
                            )
                        )
                        return@saveWidgetsToFirebase
                    }

                    verifyFirebaseSave(merchantId, existingWidgets.size + widgets.size) { verifySuccess ->
                        if (!verifySuccess) {
                            errors.add("Firebase verification failed")
                        }

                        val finalSuccess = saveSuccess && verifySuccess && errors.isEmpty()
                        Log.d(TAG, "Migration ${if (finalSuccess) "SUCCEEDED" else "FAILED"}")

                        callback(
                            MigrationResult(
                                success = finalSuccess,
                                ordersAnalyzed = orders.size,
                                itemsAnalyzed = analysisResult.itemsAnalyzed,
                                legacyNotesFound = analysisResult.legacyNotesCount,
                                widgetsCreated = if (finalSuccess) widgets.size else 0,
                                errors = errors
                            )
                        )
                    }
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
     * Analyze all item-level notes from orders, keeping only labels that don't already
     * match an existing widget - those are the unmigrated legacy entries.
     */
    private fun analyzeNotes(orders: List<Order>, knownLabels: Set<String>): AnalysisResult {
        val labelStats = mutableMapOf<String, LabelStats>()
        var itemsAnalyzed = 0
        var legacyNotesCount = 0

        for (order in orders) {
            order.lineItems?.forEach { item ->
                itemsAnalyzed++
                val note = item.note
                if (!note.isNullOrBlank()) {
                    // OrderNoteParser already understands both the current "label:value" note
                    // format and the older "[widgetId]label:value" format, stripping any
                    // widget-id prefix so we always get back a bare label.
                    val parsed = OrderNoteParser.parseNoteToMap(note)
                    val unmigrated = parsed.filterKeys { it.trim().lowercase() !in knownLabels }
                    if (unmigrated.isNotEmpty()) {
                        legacyNotesCount++
                        for ((label, value) in unmigrated) {
                            val normalizedLabel = label.trim()
                            val key = normalizedLabel.lowercase()
                            val stats = labelStats.getOrPut(key) {
                                LabelStats(
                                    label = normalizedLabel,
                                    occurrences = 0,
                                    uniqueValues = mutableSetOf(),
                                    inferredType = inferWidgetType(normalizedLabel)
                                )
                            }
                            stats.uniqueValues.add(value.trim())
                            labelStats[key] = stats.copy(
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
                stats.inferredType == WidgetType.MULTI_SELECT
            ) {
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
     * Validate widgets before saving - both against each other and against the merchant's
     * existing widgets, since WidgetManager enforces globally unique labels.
     */
    private fun validateWidgets(widgets: List<WidgetConfig>, knownLabels: Set<String>): List<String> {
        val errors = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()
        val seenLabels = mutableSetOf<String>()

        for (widget in widgets) {
            if (widget.id in seenIds) {
                errors.add("Duplicate widget ID: ${widget.id}")
            }
            seenIds.add(widget.id)

            val normalizedLabel = widget.label.lowercase()
            if (normalizedLabel in seenLabels) {
                errors.add("Duplicate widget label: ${widget.label}")
            }
            if (normalizedLabel in knownLabels) {
                errors.add("Widget label collides with an existing widget: ${widget.label}")
            }
            seenLabels.add(normalizedLabel)

            if (widget.label.isBlank()) {
                errors.add("Widget has empty label")
            }

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
