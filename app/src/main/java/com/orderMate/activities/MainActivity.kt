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
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.PermissionUtils
import com.orderMate.utils.PreferenceManager
import com.orderMate.utils.ProfileSettingsManager
import com.orderMate.utils.WidgetManager
import com.orderMate.utils.createAndShowDialog
import com.orderMate.utils.exceptionHandler
import com.orderMate.utils.migrations.SchemaMigrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Main Activity with iOS-style side navigation (#80 requirement)
 */
class MainActivity : AppCompatActivity() {

    private val permissionUtils: PermissionUtils by lazy {
        PermissionUtils.getInstance()
    }

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager.getInstance(this)
    }
    private val myApplication: MyApp by lazy {
        MyApp.getInstance()
    }
    private val profileSettingsManager: ProfileSettingsManager by lazy {
        ProfileSettingsManager.getInstance(this)
    }
    private val firebaseConfigManager: FirebaseConfigManager by lazy {
        FirebaseConfigManager.getInstance()
    }

    private lateinit var navController: NavController
    private var rootLayout: ConstraintLayout? = null
    
    // Navigation items
    private var navList: FrameLayout? = null
    private var navCalendar: FrameLayout? = null
    private var navSettings: FrameLayout? = null
    private var navProfile: FrameLayout? = null
    
    // Indicators
    private var navListIndicator: View? = null
    private var navCalendarIndicator: View? = null
    private var navSettingsIndicator: View? = null
    
    // Profile nav elements
    private var navProfileIcon: ImageView? = null
    private var navProfileEmoji: TextView? = null
    
    private var currentNavItem: Int = R.id.navList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_redesign)
        
        rootLayout = findViewById(R.id.rootLayout)
        
        setupNavigation()
        setupSideNav()
        
        // Apply theme settings immediately
        applyThemeSettings()
        
        // Sync from Firebase in background
        syncProfileSettingsFromFirebase()
    }
    
    /**
     * Apply theme settings (gradient background + nav avatar)
     */
    fun applyThemeSettings() {
        applyThemeGradient()
        updateNavProfileAvatar()
    }
    
    /**
     * Apply gradient background to entire app
     */
    private fun applyThemeGradient() {
        val themeColor = profileSettingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        
        // Apply solid color (no gradient)
        rootLayout?.setBackgroundColor(baseColor)
    }
    
    /**
     * Update nav profile to show emoji avatar
     */
    private fun updateNavProfileAvatar() {
        val avatar = profileSettingsManager.getAvatarEmoji()
        
        if (avatar.isNotEmpty()) {
            navProfileEmoji?.text = avatar
            navProfileEmoji?.visibility = View.VISIBLE
            navProfileIcon?.visibility = View.GONE
        } else {
            navProfileIcon?.visibility = View.VISIBLE
            navProfileEmoji?.visibility = View.GONE
        }
        
        applyThemeToNavProfile()
    }
    
    /**
     * Apply theme color gradient to nav profile button
     */
    private fun applyThemeToNavProfile() {
        val themeColor = profileSettingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        
        val drawable = GradientDrawable()
        drawable.setColor(baseColor)
        drawable.cornerRadius = 22f * resources.displayMetrics.density
        
        navProfile?.background = drawable
    }
    
    /**
     * Sync profile settings from Firebase
     */
    private fun syncProfileSettingsFromFirebase() {
        val merchantId = myApplication.getMerchantId()
        if (!merchantId.isNullOrEmpty()) {
            firebaseConfigManager.getProfileSettings(merchantId) { settings ->
                if (settings.themeColor != "#3C4B80" || settings.avatar.isNotEmpty()) {
                    profileSettingsManager.setThemeColor(settings.themeColor)
                    if (settings.avatar.isNotEmpty()) {
                        profileSettingsManager.setAvatarEmoji(settings.avatar)
                    }
                    runOnUiThread {
                        applyThemeSettings()
                    }
                }
            }
            
            // Run Clover notes migration (one-time)
            runCloverNotesMigration(merchantId)
        }
    }
    
    /**
     * Run migration from legacy Clover notes to V2 widgets
     * Reads orders, analyzes notes, creates widgets in Firebase
     */
    private fun runCloverNotesMigration(merchantId: String) {
        Log.d("MainActivity", "Starting Clover notes migration...")
        
        SchemaMigrator.migrateCloverNotes(this, merchantId) { result ->
            Log.d("MainActivity", "=== Migration Result ===")
            Log.d("MainActivity", "Success: ${result.success}")
            Log.d("MainActivity", "Orders analyzed: ${result.ordersAnalyzed}")
            Log.d("MainActivity", "Items analyzed: ${result.itemsAnalyzed}")
            Log.d("MainActivity", "Legacy notes found: ${result.legacyNotesFound}")
            Log.d("MainActivity", "Widgets created: ${result.widgetsCreated}")
            if (result.errors.isNotEmpty()) {
                Log.e("MainActivity", "Errors: ${result.errors}")
            }
            Log.d("MainActivity", "========================")
        }
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
        
        // Initialize profile nav elements
        navProfileIcon = findViewById(R.id.navProfileIcon)
        navProfileEmoji = findViewById(R.id.navProfileEmoji)
        
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
        
        // Refresh theme settings when returning from profile
        applyThemeSettings()
        
        // V2: Sync widgets with Firebase if merchantId available
        // (Defaults are guaranteed by Application.onCreate)
        val merchantId = MyApp.getInstance().getMerchantId()
        if (!merchantId.isNullOrEmpty()) {
            syncWidgetsFromFirebase(merchantId)
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
     * V2: Sync widgets with Firebase in background.
     * Uses level-specific fetches to prevent cross-contamination.
     */
    private fun syncWidgetsFromFirebase(merchantId: String) {
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
                            firebaseConfigManager.initializeMerchant(merchantId, allDefaults, settings) { _ -> }
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