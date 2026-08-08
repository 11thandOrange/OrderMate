package com.orderMate.utils

import android.content.Context
import com.clover.sdk.v3.order.Order
import org.json.JSONArray

/**
 * Locally persists every order OrderMate has observed at runtime, independent of both Clover's
 * on-device retention-windowed cache (MyApp.getAllOrders()) and the REST backfill
 * (CloverRepository.loadOlderOrders(), which requires a REST permission grant that may not have
 * synced to a given install - see #138). This is a last-resort fallback: it can't backfill
 * orders that aged out of Clover's local cache before OrderMate was installed/running to see
 * them, but it guarantees that any order OrderMate has displayed at least once won't disappear
 * from the list again for as long as the app stays installed, regardless of whether the REST
 * path is working.
 */
class OrderHistoryStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "order_history_store"
        private const val KEY_ORDERS = "orders_by_id"

        // Caps how much history a single install accumulates over time - oldest-created
        // orders are dropped first once over the limit.
        private const val MAX_STORED_ORDERS = 2000

        @Volatile
        private var instance: OrderHistoryStore? = null

        fun getInstance(context: Context): OrderHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: OrderHistoryStore(context.applicationContext).also { instance = it }
            }
        }
    }

    /** Merges [orders] into the store, keyed by id, overwriting any existing entry for that id. */
    @Synchronized
    fun remember(orders: List<Order?>) {
        val newById = orders.mapNotNull { order -> order?.id?.let { it to order } }
        if (newById.isEmpty()) return

        val stored = readAll().toMutableMap()
        newById.forEach { (id, order) -> stored[id] = order }

        val trimmed = if (stored.size > MAX_STORED_ORDERS) {
            stored.values
                .sortedByDescending { it.createdTime ?: 0L }
                .take(MAX_STORED_ORDERS)
                .associateBy { it.id }
        } else {
            stored
        }

        writeAll(trimmed)
    }

    /** Returns every order remembered so far, in no particular order. */
    fun getAll(): List<Order> = readAll().values.toList()

    private fun readAll(): Map<String, Order> {
        val json = prefs.getString(KEY_ORDERS, null) ?: return emptyMap()
        val array = try {
            JSONArray(json)
        } catch (e: Exception) {
            return emptyMap()
        }

        val result = mutableMapOf<String, Order>()
        for (i in 0 until array.length()) {
            try {
                val order = Order(array.getJSONObject(i))
                val id = order.id ?: continue
                result[id] = order
            } catch (e: Exception) {
                // Skip malformed entries rather than losing the whole store over one bad record.
            }
        }
        return result
    }

    private fun writeAll(orders: Map<String, Order>) {
        val array = JSONArray()
        orders.values.forEach { array.put(it.jsonObject) }
        prefs.edit().putString(KEY_ORDERS, array.toString()).apply()
    }
}
