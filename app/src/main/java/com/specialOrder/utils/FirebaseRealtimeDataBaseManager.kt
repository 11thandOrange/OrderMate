package com.specialOrder.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.specialOrder.modals.CustomItemJson


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