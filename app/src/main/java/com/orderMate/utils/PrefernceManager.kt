package com.orderMate.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.orderMate.modals.CustomItemJson
import com.orderMate.utils.defaultCustomDataForFirebase

class PreferenceManager private constructor(context: Context) {

    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null

    companion object {
        private var instance: PreferenceManager? = null
        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                PreferenceManager(context).also {
                    instance = it
                }
            }
        }
    }


    init {
        sharedPreferences =
            context.getSharedPreferences(Constants.sharedPreferenceName, Context.MODE_PRIVATE)
        editor = sharedPreferences?.edit()
    }

    /**
     * @deprecated Use WidgetManager for V2 schema persistence.
     */
    @Deprecated("Use WidgetManager for V2 schema persistence")
    @Synchronized
    fun saveJsonString(key: String, value: Any, task: () -> Unit) {
        val gson = Gson().toJson(value)
        editor?.putString(key, gson)?.apply()
        task()
    }


    /**
     * @deprecated Use WidgetManager.widgets for V2 schema data.
     */
    @Deprecated("Use WidgetManager.widgets for V2 schema data")
    fun getJsonString(): CustomItemJson {
        return try {
            val requiredJson = getString(Constants.customMenuJson)
            if (requiredJson.isBlank()) {
                @Suppress("DEPRECATION")
                defaultCustomDataForFirebase
            } else {
                Gson().fromJson(requiredJson, CustomItemJson::class.java)
                    ?: @Suppress("DEPRECATION") defaultCustomDataForFirebase
            }
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            defaultCustomDataForFirebase
        }
    }

    fun saveString(key: String, value: String) {
        editor?.putString(key, value)?.apply()
    }

    fun getString(key: String): String {
        return sharedPreferences?.getString(key, Constants.defaultString) ?: Constants.defaultString
    }

    fun saveBoolean(key: String, value: Boolean) {
        editor?.putBoolean(key, value)?.apply()
    }

    fun getBoolean(key: String): Boolean {
        return sharedPreferences?.getBoolean(key, true) ?: Constants.alwaysTrue
    }

}