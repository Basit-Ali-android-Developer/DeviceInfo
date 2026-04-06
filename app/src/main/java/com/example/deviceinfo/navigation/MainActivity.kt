package com.example.deviceinfo.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.example.deviceinfo.LocaleHelper
import com.example.deviceinfo.R
import com.example.deviceinfo.databinding.ActivityMainBinding
import com.example.deviceinfo.setting.SettingActivity
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedTabIndex = 0

    private var currentTheme: String? = null
    private var currentLang: String? = null

    private var isRecreating = false

    override fun attachBaseContext(newBase: Context) {
        val langCode = LocaleHelper.getSavedLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, langCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        currentTheme = prefs.getString("theme", "light")
        currentLang = LocaleHelper.getSavedLanguage(this)

        applyTheme(currentTheme)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnGoToSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // Restore selected tab
        selectedTabIndex = savedInstanceState?.getInt("tab_index") ?: 0

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = adapter.itemCount
        binding.viewPager.currentItem = selectedTabIndex

        val tabTitles = arrayOf(
            getString(R.string.tab_dashboard),
            getString(R.string.tab_deviceinfo),
            getString(R.string.tab_processor),
            getString(R.string.tab_battery),
            getString(R.string.tab_storage),
            getString(R.string.tab_display),
            getString(R.string.tab_network),
            getString(R.string.tab_sensors),
            getString(R.string.tab_apps)
        )

        TabLayoutMediator(binding.tab, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedTabIndex = position
            }
        })
    }

    // Smooth theme/language change
    override fun onResume() {
        super.onResume()

        if (isRecreating) return

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val newTheme = prefs.getString("theme", "light")
        val newLang = LocaleHelper.getSavedLanguage(this)

        if (newTheme != currentTheme || newLang != currentLang) {
            currentTheme = newTheme
            currentLang = newLang
            isRecreating = true

            // Show overlay loader
            showLoader("Loading ...")

            binding.root.postDelayed({
                // Recreate activity to apply theme/language
                recreate()
            }, 600) // short delay for smoothness
        } else {
            // Ensure loader hidden if no change
            binding.loaderOverlay.visibility = View.GONE
        }
    }

    private fun showLoader(message: String) {
        binding.loaderOverlay.visibility = View.VISIBLE
        binding.loaderText.text = message
    }

    private fun applyTheme(theme: String?) {
        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun switchToPage(position: Int) {
        val count = binding.viewPager.adapter?.itemCount ?: 0
        if (position in 0 until count) {
            binding.viewPager.currentItem = position
        }
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem != 0) {
            binding.viewPager.currentItem = 0
        } else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab_index", selectedTabIndex)
    }
}