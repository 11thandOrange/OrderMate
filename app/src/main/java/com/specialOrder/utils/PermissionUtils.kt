package com.specialOrder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi


class PermissionUtils private constructor() {
    companion object {
        private var instance: PermissionUtils? = null

        fun getInstance(): PermissionUtils {
            return instance ?: synchronized(this) {
                PermissionUtils().also { instance = it }
            }
        }
    }

    fun isDisplayOverOtherAppsPermissionEnable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true
        return Settings.canDrawOverlays(context)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun gotoSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }
}
