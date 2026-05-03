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
import com.orderMate.R
import com.orderMate.fragment.orderHistory.OrderHistoryFragment
import com.orderMate.modals.PopupSettings
import com.orderMate.utils.Constants
import com.orderMate.utils.DefaultWidgetFactory
import com.orderMate.utils.EmployeeRoleUtils
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.PermissionUtils
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.ProfileSettingsManager
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.createAndShowDialog
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
        
        // Apply theme immediately from SharedPreferences
        applyThemeSettings()
        
        // Also sync from Firebase in background (for cross-device sync)
        syncProfileSettingsFromFirebase()
    }
    
    /**
     * Sync profile settings from Firebase to SharedPreferences
     * Called on app start for cross-device synchronization
     */
    private fun syncProfileSettingsFromFirebase() {
        val merchantId = myApplication.getMerchantId()
        if (!merchantId.isNullOrEmpty()) {
            firebaseConfigManager.getProfileSettings(merchantId) { settings ->
                // Only update if Firebase has non-default values
                if (settings.themeColor != "#1A2A4D" || settings.avatar.isNotEmpty()) {
                    profileSettingsManager.setThemeColor(settings.themeColor)
                    if (settings.avatar.isNotEmpty()) {
                        profileSettingsManager.setAvatarEmoji(settings.avatar)
                    }
                    
                    // Re-apply theme with Firebase values
                    runOnUiThread {
                        applyThemeSettings()
                    }
                }
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
        
        // Logo click handler - navigate to list (home)
        val navLogo: FrameLayout = findViewById(R.id.navLogo)
        navLogo.setOnClickListener {
            if (currentNavItem != R.id.navList) {
                onNavItemClicked(R.id.navList)
            }
        }
        
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
        
        // Apply solid color (no gradient)
        rootLayout.setBackgroundColor(baseColor)
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
     * Apply theme color to nav profile button background (solid color)
     */
    private fun applyThemeToNavProfile() {
        val themeColor = profileSettingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        
        val drawable = GradientDrawable()
        drawable.setColor(baseColor)
        drawable.cornerRadius = 22f * resources.displayMetrics.density // Match 44dp / 2
        
        navProfile.background = drawable
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
        
        // Load widget data - matches production pattern:
        // Fetch from Firebase, if empty save defaults to Firebase AND cache
        val merchantId = MyApp.getInstance().getMerchantId()
        if (merchantId != null) {
            loadWidgetData(merchantId)
            // #79: Check permission settings for nav visibility
            checkSettingsNavVisibility(merchantId)
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
    
    /**
     * #79: Check if current employee can access settings based on their role
     * and the permission settings from Firebase.
     * Owner always sees settings. Other roles check their respective toggles.
     */
    private fun checkSettingsNavVisibility(merchantId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current employee from Clover
                val employee = myApplication.getEmployeeConnector()?.employee
                
                // Fetch permission settings from Firebase
                firebaseConfigManager.getAdvancedSettings(merchantId) { settings ->
                    val canAccess = EmployeeRoleUtils.canAccessSettings(employee, settings)
                    
                    runOnUiThread {
                        // Show or hide settings nav based on permission
                        navSettings.visibility = if (canAccess) View.VISIBLE else View.GONE
                        navSettingsIndicator.visibility = if (canAccess) View.VISIBLE else View.GONE
                        
                        // If currently on settings and no longer has access, navigate away
                        if (!canAccess && currentNavItem == R.id.navSettings) {
                            onNavItemClicked(R.id.navList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On error, default to showing settings (fail-open for owner/admin)
                runOnUiThread {
                    navSettings.visibility = View.VISIBLE
                    navSettingsIndicator.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Load widget data - EXACTLY matches production flow:
     * 1. Fetch from Firebase
     * 2. If Firebase has data → save to cache IMMEDIATELY in callback
     * 3. If Firebase empty → save DEFAULTS to cache
     */
    private fun loadWidgetData(merchantId: String) {
        val widgetManager = WidgetManager.getInstance(this)
        widgetManager.setMerchantId(merchantId)
        
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch item widgets
            firebaseConfigManager.getItemWidgets(merchantId) { itemWidgets ->
                if (itemWidgets.isNotEmpty()) {
                    widgetManager.saveItemWidgets(itemWidgets)
                }
                
                // Fetch order widgets
                firebaseConfigManager.getOrderWidgets(merchantId) { orderWidgets ->
                    if (orderWidgets.isNotEmpty()) {
                        widgetManager.saveOrderWidgets(orderWidgets)
                    }
                    
                    // Fetch settings
                    firebaseConfigManager.getSettings(merchantId) { settings ->
                        widgetManager.saveSettings(settings)
                        
                        // If no widgets exist, initialize with defaults
                        if (itemWidgets.isEmpty() && orderWidgets.isEmpty()) {
                            val defaultItemWidgets = DefaultWidgetFactory.createItemLevelDefaults()
                            val defaultOrderWidgets = DefaultWidgetFactory.createOrderLevelDefaults()
                            val allDefaults = defaultItemWidgets + defaultOrderWidgets
                            val defaultSettings = PopupSettings()
                            widgetManager.saveItemWidgets(defaultItemWidgets)
                            widgetManager.saveOrderWidgets(defaultOrderWidgets)
                            firebaseConfigManager.initializeMerchant(merchantId, allDefaults, defaultSettings) { _ -> }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        exceptionHandler { MyApp.getInstance().disconnectConnectors() }
        exceptionHandler { OrderHistoryFragment.isClicked = true }
    }
}
