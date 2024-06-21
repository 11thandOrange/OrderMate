package com.specialOrder.utils

import android.content.Context
import android.net.ConnectivityManager


class ConnectionManager private constructor() {


    companion object {
        private var instance: ConnectionManager? = null

        fun getInstance(): ConnectionManager {
            return instance ?: synchronized(this) {
                ConnectionManager().also { instance = it }
            }
        }
    }


    // deprecated code is used here because we need to provide the support for android api level 17
     fun isNetworkConnected(context : Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo == null || !networkInfo.isConnected
    }

}