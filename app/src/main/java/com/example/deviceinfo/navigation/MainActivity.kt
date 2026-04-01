package com.example.deviceinfo.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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

    override fun attachBaseContext(newBase: Context) {
        val langCode = LocaleHelper.getSavedLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, langCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply theme from SharedPreferences
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                //"custom" -> setTheme(R.style.Theme_DeviceInfo_Custom)
        }

        // Setup Toolbar
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Settings icon click
        binding.btnGoToSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // Restore selected tab after rotation
        selectedTabIndex = savedInstanceState?.getInt("tab_index") ?: 0

        // Setup ViewPager2
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

        // Attach TabLayout with ViewPager
        TabLayoutMediator(binding.tab, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Track page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedTabIndex = position
            }
        })
    }

    // Switch page programmatically (safe bounds check)
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