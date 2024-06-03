package com.order.orderappclover.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager private constructor(context: Context) {

    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
//    private var masterKeyAlias: MasterKey? = null

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
//         masterKeyAlias = MasterKey.Builder(
//            OrderAppApplication.getInstance().getContext(),
//            MasterKey.DEFAULT_MASTER_KEY_ALIAS
//        ).build()

//        sharedPreferences = EncryptedSharedPreferences.create(
//                OrderAppApplication.getInstance().getContext(),
//                Constants.sharedPreferenceName,
//                masterKeyAlias!!,
//                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//            )

        sharedPreferences = context.getSharedPreferences(Constants.sharedPreferenceName, Context.MODE_PRIVATE)
        editor = sharedPreferences?.edit()
    }


    fun saveString(key: String, value: String) {
        editor?.putString(key, value)?.apply()
    }

    fun getString(key: String): String {
        return sharedPreferences?.getString(key, Constants.defaultString) ?: Constants.defaultString
    }

    fun saveInteger(key: String, value: Int) {
        editor?.putInt(key, value)?.apply()
    }

    fun getInteger(key: String): Int {
        return sharedPreferences?.getInt(key, Constants.defaultInt) ?: Constants.defaultInt
    }

    fun saveBoolean(key: String, value: Boolean) {
        editor?.putBoolean(key, value)?.apply()
    }

    fun getBoolean(key: String): Boolean {
        return sharedPreferences?.getBoolean(key, Constants.defaultBoolean)
            ?: Constants.defaultBoolean
    }


}