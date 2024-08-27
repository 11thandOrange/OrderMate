package com.orderMate.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.orderMate.R
import com.orderMate.fragment.orderHistory.OrderHistoryFragment
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseRealtimeDataBaseManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.PermissionUtils
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.createAndShowDialog
import com.orderMate.utils.defaultCustomDataForFirebase
import com.orderMate.utils.exceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val permissionUtils: PermissionUtils by lazy {
        PermissionUtils.getInstance()
    }

    private val firebaseRealTimeManager: FirebaseRealtimeDataBaseManager by lazy {
        FirebaseRealtimeDataBaseManager.getInstance()
    }
    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(this)
    }
    private val myApplication: MyApp by lazy {
        MyApp.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
          firebaseRealTimeManager.getData(
                this@MainActivity,
                MyApp.getInstance().getMerchantId() , true
            ){
                if (it) {
                    Constants.notImplementedLog
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        saveTheData()
                    }

                }
            }


        }


        // this permission is required for the Devices above api level 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        if (permissionUtils.isDisplayOverOtherAppsPermissionEnable(this)) {
            Constants.notImplementedLog
        } else {
            createAndShowDialog(
                this,
                getString(R.string.this_permission_is_required),
                getString(R.string.overlay_permission),
                getString(R.string.settings),
                getString(R.string.cancel)
            ) {
                permissionUtils.gotoSettings(this)
            }
        }
    }


    private fun saveTheData() {
        defaultCustomDataForFirebase.let { it1 ->
            val list = Gson().toJson(it1)
            FirebaseRealtimeDataBaseManager.getInstance()
                .saveData(this,list, myApplication.getMerchantId()){}
            preferenceManager.saveJsonString(
                Constants.customMenuJson,
                it1
            ) {}
        }
    }

    override fun onPause() {
        super.onPause()
        exceptionHandler { MyApp.getInstance().disconnectConnectors() }
        exceptionHandler { OrderHistoryFragment.isClicked = true }
    }
}