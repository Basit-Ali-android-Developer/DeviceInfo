package com.example.deviceinfo

import android.content.Context
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import com.example.deviceinfo.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var loadingDialog: android.app.AlertDialog? = null

    override fun attachBaseContext(newBase: Context) {
        val langCode = LocaleHelper.getSavedLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, langCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE super.onCreate()
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "custom" -> setTheme(R.style.Theme_DeviceInfo_Custom)
        }

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = adapter.itemCount

        val tabTitles = arrayOf(
            getString(R.string.tab_dashboard),
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
    }

    fun switchToPage(position: Int) {
        binding.viewPager.currentItem = position
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                showSettingsPopup(findViewById(R.id.menu_settings))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsPopup(anchor: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_settings, null)
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // RADIO GROUP FOR THEME
        val radioGroup = popupView.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "light" -> radioGroup.check(R.id.rbLight)
            "custom" -> radioGroup.check(R.id.rbCustom)
            "dark" -> radioGroup.check(R.id.rbDark)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLight -> prefs.edit().putString("theme", "light").apply()
                R.id.rbCustom -> prefs.edit().putString("theme", "custom").apply()
                R.id.rbDark -> prefs.edit().putString("theme", "dark").apply()
            }

            popupWindow.dismiss()
            showLoader(getString(R.string.applying_theme))

            Handler(Looper.getMainLooper()).postDelayed({
                hideLoader()
                recreate()
            }, 300)
        }

        // LANGUAGE SPINNER
        val spinner = popupView.findViewById<Spinner>(R.id.popupSpinnerLanguage)
        val languages = arrayOf("English", "اردو", "العربية")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter

        val currentLang = LocaleHelper.getSavedLanguage(this)
        spinner.setSelection(
            when (currentLang) {
                "ur" -> 1
                "ar" -> 2
                else -> 0
            }
        )

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val langCode = when (pos) {
                    1 -> "ur"
                    2 -> "ar"
                    else -> "en"
                }

                if (langCode != LocaleHelper.getSavedLanguage(this@MainActivity)) {
                    showLoader(getString(R.string.changing_language))
                    LocaleHelper.saveLanguagePreference(this@MainActivity, langCode)
                    Handler(Looper.getMainLooper()).postDelayed({
                        LocaleHelper.applyLocaleAndRestart(this@MainActivity, langCode)
                    }, 5000)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    private fun showLoader(message: String = getString(R.string.please_wait)) {
        if (loadingDialog == null) {
            val builder = android.app.AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_loader, null)
            val text = view.findViewById<TextView>(R.id.loaderText)
            text.text = message
            builder.setView(view)
            builder.setCancelable(false)
            loadingDialog = builder.create()
            loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        loadingDialog?.show()
    }

    private fun hideLoader() {
        loadingDialog?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        hideLoader()
    }
}
