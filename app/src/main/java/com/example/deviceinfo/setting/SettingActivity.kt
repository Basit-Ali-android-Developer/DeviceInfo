package com.example.deviceinfo.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.deviceinfo.LocaleHelper
import com.example.deviceinfo.R
import com.example.deviceinfo.about.AboutDialog
import com.example.deviceinfo.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private var isFirstSelection = true
    private val handler = Handler(Looper.getMainLooper()) // Handler for delayed tasks

    override fun attachBaseContext(newBase: android.content.Context) {
        val langCode = LocaleHelper.getSavedLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, langCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        loadCurrentSettings()
        setupThemeToggle()
        setupLanguageDropdown()
        setupFeedbackCard()
        setupAboutCard()
    }

    // ==================== BACK BUTTON ====================
    private fun setupBackButton() {
        binding.backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ==================== LOAD CURRENT SETTINGS ====================
    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        binding.switchTheme.isChecked = prefs.getString("theme", "light") == "dark"
    }

    // ==================== THEME TOGGLE ====================
    private fun setupThemeToggle() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDark = prefs.getString("theme", "light") == "dark"
        binding.switchTheme.isChecked = isDark

        // Set initial switch colors
        updateSwitchColors(isDark)

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Show loader
            showLoader("Applying theme...")

            // Delay theme change by 3 seconds to ensure loader is visible
            handler.postDelayed({
                // Save preference
                prefs.edit().putString("theme", if (isChecked) "dark" else "light").apply()

                // Apply theme globally
                if (isChecked)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                // Update switch thumb & track immediately
                updateSwitchColors(isChecked)

                // Activity may recreate; no need to dismiss loader manually
            }, 3000)
        }
    }

    // ==================== UPDATE SWITCH COLORS ====================
    private fun updateSwitchColors(isChecked: Boolean) {
        // Thumb color always white
        binding.switchTheme.thumbTintList = getColorStateList(R.color.switch_thumb_color)

        // Track changes according to ON/OFF
        val trackColor = if (isChecked) getColor(R.color.switch_track_on)
        else getColor(R.color.switch_track_off)
        binding.switchTheme.trackTintList = android.content.res.ColorStateList.valueOf(trackColor)
    }

    // ==================== LANGUAGE DROPDOWN ====================
    private fun setupLanguageDropdown() {
        val languages = arrayOf("English", "اردو", "العربية")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter

        val currentLang = LocaleHelper.getSavedLanguage(this)
        val selectedIndex = when (currentLang) {
            "ur" -> 1
            "ar" -> 2
            else -> 0
        }
        binding.spinnerLanguage.setSelection(selectedIndex)

        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
                ) {
                    if (isFirstSelection) {
                        isFirstSelection = false
                        return
                    }

                    val langCode = when (position) {
                        1 -> "ur"
                        2 -> "ar"
                        else -> "en"
                    }

                    if (langCode != LocaleHelper.getSavedLanguage(this@SettingActivity)) {
                        // Show loader
                        showLoader("Applying language...")

                        // Delay applying language by 3 seconds to let loader show
                        handler.postDelayed({
                            LocaleHelper.saveLanguagePreference(this@SettingActivity, langCode)
                            LocaleHelper.applyLocaleAndRestart(this@SettingActivity, langCode)
                        }, 3000)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ==================== FEEDBACK CARD ====================
    private fun setupFeedbackCard() {
        binding.cardFeedback.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:developer@example.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback - Device Info App")
                }
                startActivity(intent)
            } catch (e: Exception) {
                showLoader("No email app found")
            }
        }
    }

    // ==================== ABOUT CARD ====================
    private fun setupAboutCard() {
        binding.cardAbout.setOnClickListener {
            val dialog = AboutDialog(this)
            dialog.show()
        }
    }

    // ==================== LOADER ====================
    private fun showLoader(message: String): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_loader, null)
        view.findViewById<TextView>(R.id.loaderText).text = message
        builder.setView(view)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        return dialog
    }
}