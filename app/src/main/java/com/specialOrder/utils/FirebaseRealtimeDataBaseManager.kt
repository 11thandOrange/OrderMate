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

    fun saveData(customData: String) {
        val data = firebaseDatabaseInstance?.reference
        data?.child(Constants.customData)?.child(Constants.data)?.setValue(customData)
    }

    fun getData(context: Context) {
        val data = firebaseDatabaseInstance?.getReference(Constants.customData)
        val preferenceManager = PreferenceManager.getInstance(context)
        data?.get()?.addOnFailureListener { Constants.notImplementedLog }
            ?.addOnSuccessListener { value ->
                    value.children.forEach {
                        val result =
                            Gson().fromJson(it.value.toString(), CustomItemJson::class.java)
                        preferenceManager.saveJsonString(Constants.customMenuJson, result)
                    }
                }
            ?.addOnCompleteListener { _ ->
                Constants.notImplementedLog
            }
    }
}