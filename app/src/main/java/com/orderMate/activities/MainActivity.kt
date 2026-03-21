package com.orderMate.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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

/**
 * Main Activity with iOS-style side navigation (#80 requirement)
 */
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

    private lateinit var navController: NavController
    
    // Navigation items
    private var navList: FrameLayout? = null
    private var navCalendar: FrameLayout? = null
    private var navSettings: FrameLayout? = null
    private var navProfile: FrameLayout? = null
    
    // Indicators
    private var navListIndicator: View? = null
    private var navCalendarIndicator: View? = null
    private var navSettingsIndicator: View? = null
    
    private var currentNavItem: Int = R.id.navList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_redesign)
        
        setupNavigation()
        setupSideNav()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Listen for navigation changes to update side nav state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.orderListRedesignFragment, R.id.orderHistoryFragment -> updateNavState(R.id.navList)
                R.id.calendarFragment -> updateNavState(R.id.navCalendar)
                R.id.settingsFragment -> updateNavState(R.id.navSettings)
                R.id.profileSettingsFragment -> updateNavState(R.id.navProfile)
            }
        }
    }
    
    private fun setupSideNav() {
        // Initialize nav items
        navList = findViewById(R.id.navList)
        navCalendar = findViewById(R.id.navCalendar)
        navSettings = findViewById(R.id.navSettings)
        navProfile = findViewById(R.id.navProfile)
        
        // Initialize indicators
        navListIndicator = findViewById(R.id.navListIndicator)
        navCalendarIndicator = findViewById(R.id.navCalendarIndicator)
        navSettingsIndicator = findViewById(R.id.navSettingsIndicator)
        
        // Set up click listeners
        navList?.setOnClickListener { onNavItemClicked(R.id.navList) }
        navCalendar?.setOnClickListener { onNavItemClicked(R.id.navCalendar) }
        navSettings?.setOnClickListener { onNavItemClicked(R.id.navSettings) }
        navProfile?.setOnClickListener { onNavItemClicked(R.id.navProfile) }
        
        // Set initial state
        updateNavState(R.id.navList)
    }
    
    private fun onNavItemClicked(itemId: Int) {
        if (currentNavItem == itemId) return
        
        when (itemId) {
            R.id.navList -> {
                navController.navigate(R.id.orderListRedesignFragment)
            }
            R.id.navCalendar -> {
                navController.navigate(R.id.calendarFragment)
            }
            R.id.navSettings -> {
                navController.navigate(R.id.settingsFragment)
            }
            R.id.navProfile -> {
                navController.navigate(R.id.profileSettingsFragment)
            }
        }
        
        updateNavState(itemId)
    }
    
    private fun updateNavState(activeItemId: Int) {
        currentNavItem = activeItemId
        
        // Reset all items to inactive
        navList?.let { navListIndicator?.let { ind -> setNavItemActive(it, ind, false) } }
        navCalendar?.let { navCalendarIndicator?.let { ind -> setNavItemActive(it, ind, false) } }
        navSettings?.let { navSettingsIndicator?.let { ind -> setNavItemActive(it, ind, false) } }
        
        // Set active item
        when (activeItemId) {
            R.id.navList -> navList?.let { navListIndicator?.let { ind -> setNavItemActive(it, ind, true) } }
            R.id.navCalendar -> navCalendar?.let { navCalendarIndicator?.let { ind -> setNavItemActive(it, ind, true) } }
            R.id.navSettings -> navSettings?.let { navSettingsIndicator?.let { ind -> setNavItemActive(it, ind, true) } }
        }
    }
    
    private fun setNavItemActive(container: FrameLayout, indicator: View, isActive: Boolean) {
        val icon = container.getChildAt(0) as? ImageView
        
        if (isActive) {
            container.setBackgroundResource(R.drawable.bg_nav_item_active)
            icon?.imageTintList = ContextCompat.getColorStateList(this, R.color.text_light)
            indicator.visibility = View.VISIBLE
        } else {
            container.setBackgroundResource(R.drawable.bg_nav_item)
            icon?.imageTintList = ContextCompat.getColorStateList(this, R.color.text_muted)
            indicator.visibility = View.GONE
        }
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