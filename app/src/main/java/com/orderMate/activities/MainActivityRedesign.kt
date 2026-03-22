package com.orderMate.activities

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.orderMate.R
import com.orderMate.fragment.orderHistory.OrderHistoryFragment
import com.orderMate.utils.Constants
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.FirebaseRealtimeDataBaseManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.PermissionUtils
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.ProfileSettingsManager
import com.orderMate.utils.createAndShowDialog
import com.orderMate.utils.defaultCustomDataForFirebase
import com.orderMate.utils.exceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * iOS-style redesigned MainActivity with side navigation (#80 requirement)
 * 
 * Features:
 * - Side navigation with List, Calendar, Settings, Profile
 * - App-wide gradient background based on theme color
 * - Profile avatar shows in nav bar
 */
class MainActivityRedesign : AppCompatActivity() {

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
    
    private lateinit var profileSettingsManager: ProfileSettingsManager
    private val firebaseConfigManager: FirebaseConfigManager by lazy {
        FirebaseConfigManager.getInstance()
    }

    private lateinit var navController: NavController
    private lateinit var rootLayout: ConstraintLayout
    
    // Navigation items
    private lateinit var navList: FrameLayout
    private lateinit var navCalendar: FrameLayout
    private lateinit var navSettings: FrameLayout
    private lateinit var navProfile: FrameLayout
    
    // Profile nav elements
    private lateinit var navProfileIcon: ImageView
    private lateinit var navProfileEmoji: TextView
    
    // Indicators
    private lateinit var navListIndicator: View
    private lateinit var navCalendarIndicator: View
    private lateinit var navSettingsIndicator: View
    
    private var currentNavItem: Int = R.id.navList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_redesign)
        
        profileSettingsManager = ProfileSettingsManager.getInstance(this)
        rootLayout = findViewById(R.id.rootLayout)
        
        setupNavigation()
        setupSideNav()
        
        // Load profile settings from Firebase, then apply theme
        loadAndApplyProfileSettings()
    }
    
    /**
     * Load profile settings from Firebase and sync to SharedPreferences
     * Then apply the theme to the UI
     */
    private fun loadAndApplyProfileSettings() {
        val merchantId = myApplication.getMerchantId()
        if (!merchantId.isNullOrEmpty()) {
            firebaseConfigManager.getProfileSettings(merchantId) { settings ->
                // Sync Firebase data to SharedPreferences
                profileSettingsManager.setThemeColor(settings.themeColor)
                if (settings.avatar.isNotEmpty()) {
                    profileSettingsManager.setAvatarEmoji(settings.avatar)
                }
                
                // Apply theme on main thread
                runOnUiThread {
                    applyThemeSettings()
                }
            }
        } else {
            // No merchant ID, just apply current settings
            rootLayout.post {
                applyThemeSettings()
            }
        }
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
    
    private fun setupSideNav() {
        // Initialize nav items
        navList = findViewById(R.id.navList)
        navCalendar = findViewById(R.id.navCalendar)
        navSettings = findViewById(R.id.navSettings)
        navProfile = findViewById(R.id.navProfile)
        
        // Initialize profile nav elements
        navProfileIcon = findViewById(R.id.navProfileIcon)
        navProfileEmoji = findViewById(R.id.navProfileEmoji)
        
        // Initialize indicators
        navListIndicator = findViewById(R.id.navListIndicator)
        navCalendarIndicator = findViewById(R.id.navCalendarIndicator)
        navSettingsIndicator = findViewById(R.id.navSettingsIndicator)
        
        // Set up click listeners
        navList.setOnClickListener { onNavItemClicked(R.id.navList) }
        navCalendar.setOnClickListener { onNavItemClicked(R.id.navCalendar) }
        navSettings.setOnClickListener { onNavItemClicked(R.id.navSettings) }
        navProfile.setOnClickListener { onNavItemClicked(R.id.navProfile) }
        
        // Set initial state
        updateNavState(R.id.navList)
    }
    
    /**
     * Apply theme settings (gradient background + nav avatar)
     * Called on create and when returning from profile settings
     */
    fun applyThemeSettings() {
        applyThemeGradient()
        updateNavProfileAvatar()
    }
    
    /**
     * Apply gradient background to entire app
     * Matches HTML: linear-gradient(135deg, baseColor 0%, lighterColor 100%)
     */
    private fun applyThemeGradient() {
        val themeColor = profileSettingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        val lighterColor = lightenColor(baseColor, 0.3f)
        
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(baseColor, lighterColor)
        )
        
        // Apply to root layout (already initialized in onCreate)
        rootLayout.background = gradientDrawable
    }
    
    /**
     * Update nav profile to show emoji avatar and apply theme color to background
     * Matches HTML: navProfile.innerHTML = profileSettings.avatar
     */
    private fun updateNavProfileAvatar() {
        val avatar = profileSettingsManager.getAvatarEmoji()
        
        if (avatar.isNotEmpty()) {
            // Show emoji, hide icon
            navProfileEmoji.text = avatar
            navProfileEmoji.visibility = View.VISIBLE
            navProfileIcon.visibility = View.GONE
        } else {
            // Show icon, hide emoji
            navProfileIcon.visibility = View.VISIBLE
            navProfileEmoji.visibility = View.GONE
        }
        
        // Apply theme color gradient to nav profile background
        applyThemeToNavProfile()
    }
    
    /**
     * Apply theme color gradient to nav profile button background
     */
    private fun applyThemeToNavProfile() {
        val themeColor = profileSettingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        val lighterColor = lightenColor(baseColor, 0.3f)
        
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(baseColor, lighterColor)
        )
        gradientDrawable.cornerRadius = 22f * resources.displayMetrics.density // Match 44dp / 2
        
        navProfile.background = gradientDrawable
    }
    
    /**
     * Lighten a color by percentage (matches HTML lightenColor function)
     */
    private fun lightenColor(color: Int, percent: Float): Int {
        val r = minOf(255, (Color.red(color) + 255 * percent).toInt())
        val g = minOf(255, (Color.green(color) + 255 * percent).toInt())
        val b = minOf(255, (Color.blue(color) + 255 * percent).toInt())
        return Color.rgb(r, g, b)
    }
    
    private fun onNavItemClicked(itemId: Int) {
        if (currentNavItem == itemId) return
        
        when (itemId) {
            R.id.navList -> {
                // Navigate to order list
                navController.navigate(R.id.orderHistoryFragment)
            }
            R.id.navCalendar -> {
                // Navigate to calendar
                navController.navigate(R.id.calendarFragment)
            }
            R.id.navSettings -> {
                // Navigate to settings
                navController.navigate(R.id.settingsFragment)
            }
            R.id.navProfile -> {
                // Navigate to profile settings
                navController.navigate(R.id.profileSettingsFragment)
            }
        }
        
        updateNavState(itemId)
    }
    
    private fun updateNavState(activeItemId: Int) {
        currentNavItem = activeItemId
        
        // Reset all items to inactive
        setNavItemActive(navList, navListIndicator, false)
        setNavItemActive(navCalendar, navCalendarIndicator, false)
        setNavItemActive(navSettings, navSettingsIndicator, false)
        
        // Set active item
        when (activeItemId) {
            R.id.navList -> setNavItemActive(navList, navListIndicator, true)
            R.id.navCalendar -> setNavItemActive(navCalendar, navCalendarIndicator, true)
            R.id.navSettings -> setNavItemActive(navSettings, navSettingsIndicator, true)
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
        
        // Refresh theme settings when returning from profile
        applyThemeSettings()
        
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRealTimeManager.getData(
                this@MainActivityRedesign,
                MyApp.getInstance().getMerchantId(), true
            ) {
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
                .saveData(this, list, myApplication.getMerchantId()) {}
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
