package com.example.deviceinfo.setting

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.deviceinfo.LocaleHelper
import com.example.deviceinfo.about.AboutDialog
import com.example.deviceinfo.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private var isFirstSelection = true
    private val handler = Handler(Looper.getMainLooper())

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

    private fun setupBackButton() {
        binding.backArrow.setOnClickListener { finish() }
    }

    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        binding.switchTheme.isChecked = prefs.getString("theme", "light") == "dark"
    }

    private fun setupThemeToggle() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->

            binding.loaderOverlay.visibility = android.view.View.VISIBLE
            binding.loaderText.text = "Applying theme..."

            handler.postDelayed({
                prefs.edit().putString("theme", if (isChecked) "dark" else "light").apply()

                if (isChecked)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                recreate() // Apply immediately
            }, 500)
        }
    }

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

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (isFirstSelection) { isFirstSelection = false; return }

                val langCode = when (position) { 1 -> "ur"; 2 -> "ar"; else -> "en" }
                val currentSaved = LocaleHelper.getSavedLanguage(this@SettingActivity)

                if (langCode != currentSaved) {
                    binding.loaderOverlay.visibility = android.view.View.VISIBLE
                    binding.loaderText.text = "Applying language..."
                    handler.postDelayed({
                        LocaleHelper.saveLanguagePreference(this@SettingActivity, langCode)
                        recreate()
                    }, 500)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFeedbackCard() {
        binding.cardFeedback.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:developer@example.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback - Device Info App")
                }
                startActivity(intent)
            } catch (e: Exception) {
                binding.loaderOverlay.visibility = android.view.View.VISIBLE
                binding.loaderText.text = "No email app found"
            }
        }
    }

    private fun setupAboutCard() {
        binding.cardAbout.setOnClickListener {
            val dialog = AboutDialog(this)
            dialog.show()
        }
    }
}