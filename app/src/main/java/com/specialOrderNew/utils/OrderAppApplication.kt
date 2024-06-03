package com.specialOrderNew.utils

import android.app.Application
import android.content.Context


class MyApp : Application() {

    companion object {
        private lateinit var instance: MyApp

        fun getContext(): Context {
            return instance.applicationContext
        }

        fun getApplication() : Application{
            return Application()
        }

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
