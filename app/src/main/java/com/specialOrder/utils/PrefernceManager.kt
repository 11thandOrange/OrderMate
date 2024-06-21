package com.specialOrder.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.specialOrder.modals.CustomItemJson

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


    fun saveJsonString(key: String, value: Any) {
        val gson = Gson().toJson(value)
        editor?.putString(key, gson)?.apply()
    }


    fun getJsonString(): Any? {
        return try {
            val requiredJson = getString(Constants.customMenuJson)
            Gson().fromJson(requiredJson, CustomItemJson::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveString(key: String, value: String) {
        editor?.putString(key, value)?.apply()
    }

    fun getString(key: String): String {
        return sharedPreferences?.getString(key, Constants.defaultString) ?: Constants.defaultString
    }

}