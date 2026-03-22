package com.orderMate.fragment

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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.R
import com.orderMate.databinding.FragmentProfileSettingsBinding
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp
import com.orderMate.utils.ProfileSettings
import com.orderMate.utils.ProfileSettingsManager

/**
 * Profile Settings Fragment (Issue #85)
 * 
 * Allows users to:
 * - Change theme color (single color → gradient)
 * - Change profile avatar (emoji picker)
 * - Avatar renders as profile icon in side nav
 * 
 * Default theme color: #3C4B80 (matches HTML)
 */
class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsManager: ProfileSettingsManager
    private lateinit var firebaseManager: FirebaseConfigManager
    private var onAvatarChangedListener: ((String?, Uri?) -> Unit)? = null
    
    // Default theme color matching HTML
    private val DEFAULT_THEME_COLOR = "#3C4B80"
    private val DEFAULT_AVATAR = "😊"

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
        
        settingsManager = ProfileSettingsManager(requireContext())
        firebaseManager = FirebaseConfigManager.getInstance()
        
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun setupClickListeners() {
        // Color picker - click on preview or Change button
        binding.colorPreview.setOnClickListener { showColorPickerDialog() }
        binding.btnChangeColor.setOnClickListener { showColorPickerDialog() }
        
        // Emoji picker - click on preview or Change button
        binding.avatarPreview.setOnClickListener { showEmojiPickerDialog() }
        binding.btnChangeAvatar.setOnClickListener { showEmojiPickerDialog() }
        
        // Header avatar also opens emoji picker
        binding.headerAvatarContainer.setOnClickListener { showEmojiPickerDialog() }
        
        // Reset button
        binding.btnReset.setOnClickListener { resetSettings() }
    }

    private fun loadCurrentSettings() {
        // Load theme color
        val themeColor = settingsManager.getThemeColor()
        applyThemeColor(themeColor)
        
        // Load avatar
        updateAvatarDisplay()
    }

    /**
     * Show color picker dialog
     * Matches HTML: Uses native color picker input (any color)
     * When selected: Creates gradient and applies to entire app background
     */
    private fun showColorPickerDialog() {
        // Use AmbilWarna color picker or simple AlertDialog with SeekBars
        // For now, we'll use a simple color grid that matches common use cases
        // The key difference from HTML is HTML uses native <input type="color">
        
        val colors = listOf(
            "#3C4B80", // Default (navy blue) - HTML default
            "#667eea", // Purple
            "#764ba2", // Violet  
            "#f093fb", // Pink
            "#f5576c", // Red
            "#4facfe", // Blue
            "#00f2fe", // Cyan
            "#43e97b", // Green
            "#38f9d7", // Teal
            "#fa709a", // Rose
            "#fee140", // Yellow
            "#ff9a9e", // Peach
            "#a18cd1", // Lavender
            "#fbc2eb", // Light pink
            "#2c3e50", // Dark blue
            "#6a11cb"  // Deep purple
        )

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)
        
        val colorGrid = view.findViewById<RecyclerView>(R.id.colorGrid)
        colorGrid.layoutManager = GridLayoutManager(requireContext(), 4)
        colorGrid.adapter = ColorAdapter(colors) { selectedColor ->
            // Match HTML behavior:
            // 1. Apply gradient to color preview
            applyThemeColor(selectedColor)
            // 2. Save to local settings
            settingsManager.setThemeColor(selectedColor)
            // 3. Save to Firebase
            saveToFirebase()
            // 4. Apply to entire app (will take effect on activity resume)
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
            showToast("Avatar updated!")
            dialog.dismiss()
        }
    }

    /**
     * Apply theme color and generate gradient
     * Matches HTML: linear-gradient(135deg, baseColor 0%, lighterColor 100%)
     */
    private fun applyThemeColor(hexColor: String) {
        val baseColor = Color.parseColor(hexColor)
        val lighterColor = lightenColor(baseColor, 0.3f)
        
        // Update color preview with gradient
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(baseColor, lighterColor)
        )
        gradientDrawable.cornerRadius = 10f * resources.displayMetrics.density
        binding.colorPreview.background = gradientDrawable
        
        // TODO: Apply gradient to app background
        // This would require updating the Activity's background
    }

    /**
     * Lighten a color by percentage (matches HTML lightenColor function)
     */
    private fun lightenColor(color: Int, percent: Float): Int {
        val r = Math.min(255, (Color.red(color) + 255 * percent).toInt())
        val g = Math.min(255, (Color.green(color) + 255 * percent).toInt())
        val b = Math.min(255, (Color.blue(color) + 255 * percent).toInt())
        return Color.rgb(r, g, b)
    }

    fun selectAvatar(emoji: String) {
        settingsManager.setAvatarEmoji(emoji)
        settingsManager.clearCustomAvatarUri()
        updateAvatarDisplay()
        onAvatarChangedListener?.invoke(emoji, null)
    }

    private fun updateAvatarDisplay() {
        val customUri = settingsManager.getCustomAvatarUri()
        val emoji = settingsManager.getAvatarEmoji()

        // Update header avatar
        binding.headerAvatarEmoji.text = emoji
        
        // Update avatar preview
        if (customUri != null) {
            binding.profileAvatar.setImageURI(customUri)
            binding.avatarPreviewEmoji.visibility = View.GONE
            binding.profileAvatar.visibility = View.VISIBLE
        } else {
            binding.avatarPreviewEmoji.text = emoji
            binding.avatarPreviewEmoji.visibility = View.VISIBLE
            binding.profileAvatar.visibility = View.GONE
        }
    }

    private fun resetSettings() {
        // Reset to default theme color (#3C4B80)
        applyThemeColor(DEFAULT_THEME_COLOR)
        settingsManager.setThemeColor(DEFAULT_THEME_COLOR)
        
        // Reset avatar to default
        settingsManager.setAvatarEmoji(DEFAULT_AVATAR)
        settingsManager.clearCustomAvatarUri()
        updateAvatarDisplay()
        
        // Save to Firebase
        saveToFirebase()
        
        showToast("Settings reset to defaults")
    }
    
    /**
     * Save profile settings to Firebase
     */
    private fun saveToFirebase() {
        val merchantId = MyApp.getInstance().getMerchantId()
        if (!merchantId.isNullOrEmpty()) {
            val settings = ProfileSettings(
                themeColor = settingsManager.getThemeColor(),
                avatar = settingsManager.getAvatarEmoji()
            )
            firebaseManager.saveProfileSettings(merchantId, settings) { success ->
                if (!success) {
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
     * Color adapter for color picker grid
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
            val lighterColor = lightenColor(baseColor, 0.3f)
            
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(baseColor, lighterColor)
            )
            gradientDrawable.cornerRadius = 8f * holder.itemView.resources.displayMetrics.density
            holder.colorView.background = gradientDrawable
            
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
