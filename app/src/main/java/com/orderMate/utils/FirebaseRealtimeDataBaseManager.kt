package com.orderMate.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.orderMate.modals.CustomItemJson


class FirebaseRealtimeDataBaseManager private constructor() {

    private var firebaseDatabaseInstance: FirebaseDatabase? = null


    companion object {
        private var instance: FirebaseRealtimeDataBaseManager? = null
        fun getInstance(): FirebaseRealtimeDataBaseManager {
            return instance ?: synchronized(this) {
                FirebaseRealtimeDataBaseManager().also { instance = it }
            }
        }
    }

    init {
        firebaseDatabaseInstance = FirebaseDatabase.getInstance()
    }


    fun saveData(context : Context , customData: String, merchantId: String? , task:(Boolean)->Unit) {
        if (merchantId == null) {
            return
        }
        val data = firebaseDatabaseInstance?.reference
        data?.child(Constants.customData)?.child(merchantId)?.child(Constants.data)
            ?.setValue(customData)?.
                addOnSuccessListener {
                    getData(context , merchantId){}
                    task(true)
                }
            ?.addOnCompleteListener {
                task(true) }
            ?.addOnFailureListener {
                task(false) }
    }


    /**
     * #54: record that a Bird conversation was created for an order, so the
     * notification history UI can look it back up later (Bird itself has no
     * server-side way to query conversations by order).
     */
    fun saveConversationForOrder(
        merchantId: String,
        orderId: String,
        conversationId: String,
        task: (Boolean) -> Unit
    ) {
        firebaseDatabaseInstance?.getReference(
            FirebasePaths.orderConversation(merchantId, orderId, conversationId)
        )?.setValue(true)
            ?.addOnSuccessListener { task(true) }
            ?.addOnFailureListener { task(false) }
    }

    /**
     * #54: fetch the Bird conversation ids previously recorded for an order.
     */
    fun getConversationsForOrder(
        merchantId: String,
        orderId: String,
        task: (List<String>) -> Unit
    ) {
        firebaseDatabaseInstance?.getReference(
            FirebasePaths.orderConversations(merchantId, orderId)
        )?.get()
            ?.addOnSuccessListener { snapshot ->
                task(snapshot.children.mapNotNull { it.key })
            }
            ?.addOnFailureListener { task(emptyList()) }
    }

    fun getData(
        context: Context,
        merchantId: String?,
        isMerchantDataSaved: Boolean = false,
        task : (Boolean) -> Unit
    ): Boolean {
        var result = false
        if (merchantId == null) {
            task(false)
            return false
        }
        val data = firebaseDatabaseInstance?.getReference(Constants.customData)?.child(merchantId)
        val preferenceManager = PreferenceManager.getInstance(context)
        data?.get()?.addOnFailureListener {
            task(false)
            Constants.notImplementedLog
        }
            ?.addOnSuccessListener { value ->
                if(!value.exists()){
                    if (isMerchantDataSaved) {
                        task(false)
                    }
                    return@addOnSuccessListener
                }
                value.children.forEach {
                    if (isMerchantDataSaved) {
                       task(true)
                    }
                    val newData = Gson().fromJson(it.value.toString(), CustomItemJson::class.java)
                    preferenceManager.saveJsonString(Constants.customMenuJson, newData) {}
                    val resultant = Gson().fromJson(
                        preferenceManager.getString(Constants.customMenuJson),
                        CustomItemJson::class.java
                    )
                    isAllFieldDisabled(preferenceManager, resultant)
                    isCustomOptionEnabled(preferenceManager, resultant)
                }

            }
            ?.addOnCompleteListener { _ ->
                Constants.notImplementedLog
            }

        return result
    }



}