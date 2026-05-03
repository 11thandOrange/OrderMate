package com.orderMate.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.databinding.FragmentProfileSettingsBinding
import com.orderMate.modals.EmployeeProfile
import com.orderMate.utils.EmployeeRoleUtils
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.ProfileSettings
import com.orderMate.utils.ProfileSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Profile Settings Fragment (Issue #85, #81)
 * 
 * Allows users to:
 * - Change theme color (flat solid color, no gradient)
 * - Change profile avatar (emoji picker)
 * - Avatar renders as profile icon in side nav
 * 
 * #81 additions:
 * - Per-employee profile storage (color, avatar)
 * - Referral button (Owner only) to enter partner who referred them
 * 
 * Default theme color: #1C3527 (matches HTML)
 */
class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsManager: ProfileSettingsManager
    private lateinit var firebaseManager: FirebaseConfigManager
    private var onAvatarChangedListener: ((String?, Uri?) -> Unit)? = null
    
    // #81: Cache employee info to avoid repeated Clover API calls
    private var currentEmployeeId: String? = null
    private var isOwner: Boolean = false
    
    // Default theme color matching HTML
    private val DEFAULT_THEME_COLOR = "#1C3527"
    private val DEFAULT_AVATAR = ""  // Empty = show placeholder icon

    // Emoji categories matching HTML
    private val emojiCategories = mapOf(
        "people" to listOf("😀","😃","😄","😁","😊","😇","🥰","😍","🤩","😎","🧐","🤓","😏","😌","😴","🤤","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😒","🙄","😬"),
        "food" to listOf("🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🌽","🥕","🥔","🍠","🥐","🥖","🍞","🥨","🥯","🧇","🥞","🍳","🧀","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕","🥪","🌮","🌯","🥗","🍝","🍜","🍲","🍛","🍣","🍱","🥟","🍤","🍙","🍚","🍘","🍥","🥮","🍢","🍡","🍧","🍨","🍦","🥧","🧁","🍰","🎂","🍮","🍭","🍬","🍫","🍿","🍩","🍪"),
        "activities" to listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🏓","🏸","🏒","🏑","🥍","🏏","🥅","⛳","🏹","🎣","🥊","🥋","🎽","🛹","🛼","🛷","⛸️","🥌","🎿","⛷️","🏂","🏋️","🤼","🤸","🤺","⛹️","🤾","🏌️","🏇","🧘","🏄","🏊","🤽","🚣","🧗","🚵","🚴","🏆","🥇","🥈","🥉","🏅","🎖️","🎗️","🎫","🎟️","🎪","🎭","🎨","🎬","🎤","🎧","🎼","🎹","🥁","🎷","🎺","🎸","🎻"),
        "objects" to listOf("⌚","📱","💻","⌨️","🖥️","🖨️","🖱️","💽","💾","💿","📀","📷","📸","📹","🎥","📞","☎️","📺","📻","🎙️","🎚️","🎛️","⏱️","⏲️","⏰","🕰️","⌛","⏳","📡","🔋","🔌","💡","🔦","🕯️","💸","💵","💴","💶","💷","💰","💳","💎","⚖️","🧰","🔧","🔨","🛠️","⚙️","🔩","🧱","🔫","💣","🔪","🗡️","⚔️","🛡️","🔮","📿","🧿","💈","⚗️","🔭","🔬","💊","💉")
    )

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            settingsManager.setCustomAvatarUri(it)
            updateAvatarDisplay()
            onAvatarChangedListener?.invoke(null, it)
            showToast("Avatar updated!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsManager = ProfileSettingsManager.getInstance(requireContext())
        firebaseManager = FirebaseConfigManager.getInstance()
        
        setupClickListeners()
        loadCurrentSettings()
        loadUserAndMerchantInfo()
        
        // #81: Check referral button visibility
        checkReferralButtonVisibility()
    }
    
    /**
     * Load active user's name and merchant business name from Clover
     * Must run on background thread as Clover API calls are blocking
     */
    private fun loadUserAndMerchantInfo() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val myApp = MyApp.getInstance()
                val employee = myApp.getEmployeeConnector()?.employee
                val merchantName = myApp.getMerchantName()
                
                withContext(Dispatchers.Main) {
                    // Active user's name
                    binding.profileName.text = employee?.name ?: "Unknown User"
                    
                    // Merchant business name
                    binding.profileEmail.text = merchantName ?: "Unknown Business"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.profileName.text = "Unknown User"
                    binding.profileEmail.text = "Unknown Business"
                }
            }
        }
    }

    private fun setupClickListeners() {
        // (#77) Color picker - entire section is clickable
        binding.colorSection.setOnClickListener { showColorPickerDialog() }
        binding.colorPreview.setOnClickListener { showColorPickerDialog() }
        
        // (#77) Emoji picker - entire section is clickable
        binding.avatarSection.setOnClickListener { showEmojiPickerDialog() }
        binding.avatarPreview.setOnClickListener { showEmojiPickerDialog() }
        
        // Header avatar also opens emoji picker
        binding.headerAvatarContainer.setOnClickListener { showEmojiPickerDialog() }
        
        // Reset button
        binding.btnReset.setOnClickListener { resetSettings() }
        
        // #81: Referral button
        binding.btnReferral.setOnClickListener { showReferralDialog() }
    }
    
    // ==================== #81: Referral Button Logic ====================
    
    /**
     * Check if referral button should be visible
     * Only show for Owners who haven't already submitted a referral
     */
    private fun checkReferralButtonVisibility() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val myApp = MyApp.getInstance()
                val employee = myApp.getCurrentEmployee()
                val merchantId = myApp.getMerchantId()
                
                // Cache employee info
                currentEmployeeId = employee?.id
                isOwner = EmployeeRoleUtils.isOwner(employee)
                
                // Only owners can see the referral button
                if (!isOwner || merchantId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.referralButtonContainer.visibility = View.GONE
                    }
                    return@launch
                }
                
                // Check if merchant already has a referral
                firebaseManager.hasAnyReferral(merchantId) { hasReferral ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.referralButtonContainer.visibility = 
                            if (hasReferral) View.GONE else View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.referralButtonContainer.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * Show referral partner dialog
     */
    private fun showReferralDialog() {
        val dialog = ReferralPartnerDialog.newInstance()
        dialog.setOnSaveListener { partnerName ->
            // Referral saved, hide button
            binding.referralButtonContainer.visibility = View.GONE
            showToast("Thank you! Referral saved: $partnerName")
        }
        dialog.show(childFragmentManager, ReferralPartnerDialog.TAG)
    }

    private fun loadCurrentSettings() {
        // Load theme color and apply to preview
        val themeColor = settingsManager.getThemeColor()
        applyThemeColor(themeColor)
        
        // Also apply to app background in case it wasn't applied
        applyColorToAppBackground(themeColor)
        
        // Load avatar
        updateAvatarDisplay()
        
        // Update nav avatar as well
        val emoji = settingsManager.getAvatarEmoji()
        updateNavAvatar(emoji)
    }

    /**
     * Show simple color picker dialog
     * Tap on color spectrum to choose any color, adjust brightness with slider
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showColorPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)
        
        // Get current color
        val currentColor = settingsManager.getThemeColor()
        var selectedHue = 0f
        var selectedSaturation = 1f
        var brightness = 1f
        
        // Parse current color to HSV
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.parseColor(currentColor), hsv)
        selectedHue = hsv[0]
        selectedSaturation = hsv[1]
        brightness = hsv[2]
        
        // UI Elements
        val colorPreviewLarge = view.findViewById<FrameLayout>(R.id.colorPreviewLarge)
        val colorSpectrum = view.findViewById<ImageView>(R.id.colorSpectrum)
        val brightnessSlider = view.findViewById<SeekBar>(R.id.brightnessSlider)
        val tvBrightness = view.findViewById<TextView>(R.id.tvBrightness)
        val tvHexColor = view.findViewById<TextView>(R.id.tvHexColor)
        val btnApply = view.findViewById<View>(R.id.btnApply)
        
        // Function to get current color
        fun getCurrentColor(): Int {
            return Color.HSVToColor(floatArrayOf(selectedHue, selectedSaturation, brightness))
        }
        
        // Function to update preview with solid color (no gradient)
        fun updatePreview() {
            val baseColor = getCurrentColor()
            val solidDrawable = GradientDrawable()
            solidDrawable.setColor(baseColor)
            solidDrawable.cornerRadius = 10f * resources.displayMetrics.density
            colorPreviewLarge.background = solidDrawable
            
            // Update hex display
            val hexColor = String.format("#%02X%02X%02X", 
                Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            tvHexColor.text = hexColor
        }
        
        // Initialize
        brightnessSlider.progress = (brightness * 100).toInt()
        tvBrightness.text = "${(brightness * 100).toInt()}%"
        updatePreview()
        
        // Tap on spectrum to pick color
        colorSpectrum.setOnTouchListener { v, event ->
            val x = event.x / v.width
            val y = event.y / v.height
            
            // X = hue (0-360), Y = saturation (1 at top, 0 at bottom)
            selectedHue = x * 360f
            selectedSaturation = 1f - y
            
            updatePreview()
            true
        }
        
        // Brightness slider
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightness = progress / 100f
                tvBrightness.text = "$progress%"
                if (fromUser) updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Apply button
        btnApply.setOnClickListener {
            val baseColor = getCurrentColor()
            val finalColor = String.format("#%02X%02X%02X", 
                Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            
            applyThemeColor(finalColor)
            settingsManager.setThemeColor(finalColor)
            saveToFirebase()
            applyColorToAppBackground(finalColor)
            // Update nav profile button background with new theme color
            activity?.findViewById<View>(R.id.navProfile)?.let { updateNavProfileBackground(it) }
            showToast("Theme color updated!")
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Show emoji picker dialog
     * Matches HTML: Dark overlay (#000000 70% opacity), white card, close on X or outside click
     */
    private fun showEmojiPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val view = layoutInflater.inflate(R.layout.dialog_emoji_picker, null)
        dialog.setContentView(view)
        
        // Setup emoji grids
        setupEmojiGrid(view.findViewById(R.id.emojiGridPeople), emojiCategories["people"]!!, dialog)
        setupEmojiGrid(view.findViewById(R.id.emojiGridFood), emojiCategories["food"]!!, dialog)
        setupEmojiGrid(view.findViewById(R.id.emojiGridActivities), emojiCategories["activities"]!!, dialog)
        setupEmojiGrid(view.findViewById(R.id.emojiGridObjects), emojiCategories["objects"]!!, dialog)
        
        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun setupEmojiGrid(recyclerView: RecyclerView, emojis: List<String>, dialog: Dialog) {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 8)
        recyclerView.adapter = EmojiAdapter(emojis) { emoji ->
            selectAvatar(emoji)
            saveToFirebase()
            // Update nav avatar in MainActivity immediately
            updateNavAvatar(emoji)
            showToast("Avatar updated!")
            dialog.dismiss()
        }
    }

    /**
     * Apply theme color as flat solid color (no gradient)
     */
    private fun applyThemeColor(hexColor: String) {
        val baseColor = Color.parseColor(hexColor)
        
        // Update color preview with solid color
        val solidDrawable = GradientDrawable()
        solidDrawable.setColor(baseColor)
        solidDrawable.cornerRadius = 10f * resources.displayMetrics.density
        binding.colorPreview.background = solidDrawable
    }
    
    /**
     * Apply flat solid color to entire app background immediately
     */
    private fun applyColorToAppBackground(hexColor: String) {
        val baseColor = Color.parseColor(hexColor)
        val colorDrawable = ColorDrawable(baseColor)
        
        // Apply to activity's root layout (like document.body)
        activity?.let { act ->
            act.window?.decorView?.background = colorDrawable
            // Also try to update root layout if available
            act.findViewById<View>(R.id.rootLayout)?.background = colorDrawable
        }
    }

    fun selectAvatar(emoji: String) {
        settingsManager.setAvatarEmoji(emoji)
        settingsManager.clearCustomAvatarUri()
        updateAvatarDisplay()
        onAvatarChangedListener?.invoke(emoji, null)
    }
    
    /**
     * Update nav avatar in MainActivity immediately
     * Matches HTML: navProfile.innerHTML = profileSettings.avatar
     * If emoji is empty, show placeholder icon instead
     */
    private fun updateNavAvatar(emoji: String) {
        activity?.let { act ->
            val navProfileIcon = act.findViewById<View>(R.id.navProfileIcon)
            val navProfileEmoji = act.findViewById<TextView>(R.id.navProfileEmoji)
            val navProfile = act.findViewById<View>(R.id.navProfile)
            
            if (navProfileIcon != null && navProfileEmoji != null) {
                if (emoji.isNotEmpty()) {
                    // Show emoji, hide icon
                    navProfileEmoji.text = emoji
                    navProfileEmoji.visibility = View.VISIBLE
                    navProfileIcon.visibility = View.GONE
                } else {
                    // Show placeholder icon, hide emoji
                    navProfileIcon.visibility = View.VISIBLE
                    navProfileEmoji.visibility = View.GONE
                }
            }
            
            // Also update nav profile background with theme color
            navProfile?.let { updateNavProfileBackground(it) }
        }
    }
    
    /**
     * Apply theme color (flat solid) to nav profile button background
     */
    private fun updateNavProfileBackground(navProfile: View) {
        val themeColor = settingsManager.getThemeColor()
        val baseColor = Color.parseColor(themeColor)
        
        val solidDrawable = GradientDrawable()
        solidDrawable.setColor(baseColor)
        solidDrawable.cornerRadius = 22f * resources.displayMetrics.density
        
        navProfile.background = solidDrawable
    }

    private fun updateAvatarDisplay() {
        val customUri = settingsManager.getCustomAvatarUri()
        val emoji = settingsManager.getAvatarEmoji()

        // Update header avatar - show placeholder if empty
        if (emoji.isNotEmpty()) {
            binding.headerAvatarEmoji.text = emoji
            binding.headerAvatarEmoji.visibility = View.VISIBLE
            binding.headerAvatarIcon?.visibility = View.GONE
        } else {
            binding.headerAvatarEmoji.visibility = View.GONE
            binding.headerAvatarIcon?.visibility = View.VISIBLE
        }
        
        // Update avatar preview
        if (customUri != null) {
            binding.profileAvatar.setImageURI(customUri)
            binding.avatarPreviewEmoji.visibility = View.GONE
            binding.avatarPreviewIcon?.visibility = View.GONE
            binding.profileAvatar.visibility = View.VISIBLE
        } else if (emoji.isNotEmpty()) {
            binding.avatarPreviewEmoji.text = emoji
            binding.avatarPreviewEmoji.visibility = View.VISIBLE
            binding.avatarPreviewIcon?.visibility = View.GONE
            binding.profileAvatar.visibility = View.GONE
        } else {
            // Show placeholder icon
            binding.avatarPreviewEmoji.visibility = View.GONE
            binding.avatarPreviewIcon?.visibility = View.VISIBLE
            binding.profileAvatar.visibility = View.GONE
        }
    }

    private fun resetSettings() {
        // Reset to default theme color (#1C3527)
        applyThemeColor(DEFAULT_THEME_COLOR)
        settingsManager.setThemeColor(DEFAULT_THEME_COLOR)
        
        // Apply flat color to entire app background immediately
        applyColorToAppBackground(DEFAULT_THEME_COLOR)
        
        // Reset avatar to default
        settingsManager.setAvatarEmoji(DEFAULT_AVATAR)
        settingsManager.clearCustomAvatarUri()
        updateAvatarDisplay()
        
        // Update nav avatar immediately
        updateNavAvatar(DEFAULT_AVATAR)
        
        // Save to Firebase
        saveToFirebase()
        
        showToast("Settings reset to defaults")
    }
    
    /**
     * Save profile settings to Firebase (#81: Per-employee storage)
     */
    private fun saveToFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val myApp = MyApp.getInstance()
            val merchantId = myApp.getMerchantId()
            val employeeId = currentEmployeeId ?: myApp.getEmployeeId()
            
            if (!merchantId.isNullOrEmpty() && !employeeId.isNullOrEmpty()) {
                val profile = EmployeeProfile(
                    color = settingsManager.getThemeColor(),
                    avatar = settingsManager.getAvatarEmoji()
                )
                firebaseManager.saveEmployeeProfile(merchantId, employeeId, profile) { success ->
                    // Silent fail - local settings are still saved
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun setOnAvatarChangedListener(listener: (String?, Uri?) -> Unit) {
        onAvatarChangedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Color adapter for color picker grid (solid colors, no gradient)
     */
    inner class ColorAdapter(
        private val colors: List<String>,
        private val onColorSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorView: FrameLayout = view.findViewById(R.id.colorView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val color = colors[position]
            val baseColor = Color.parseColor(color)
            
            val solidDrawable = GradientDrawable()
            solidDrawable.setColor(baseColor)
            solidDrawable.cornerRadius = 8f * holder.itemView.resources.displayMetrics.density
            holder.colorView.background = solidDrawable
            
            holder.colorView.setOnClickListener {
                onColorSelected(color)
            }
        }

        override fun getItemCount() = colors.size
    }

    /**
     * Emoji adapter for emoji picker grid
     */
    inner class EmojiAdapter(
        private val emojis: List<String>,
        private val onEmojiSelected: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val emojiText: TextView = view.findViewById(R.id.emojiText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emoji = emojis[position]
            holder.emojiText.text = emoji
            holder.emojiText.setOnClickListener {
                onEmojiSelected(emoji)
            }
        }

        override fun getItemCount() = emojis.size
    }

    companion object {
        fun newInstance() = ProfileSettingsFragment()
    }
}
