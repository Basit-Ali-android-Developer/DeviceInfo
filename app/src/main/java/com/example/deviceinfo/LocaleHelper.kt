package com.example.deviceinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.*

object LocaleHelper {

    // ✅ Applies locale + direction (RTL/LTR) exactly like your old version
    fun setLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        return prefs.getString("My_Lang", "en") ?: "en"
    }

    fun saveLanguagePreference(context: Context, langCode: String) {
        val editor = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
        editor.putString("My_Lang", langCode)
        editor.apply()
    }

    // ✅ Helper to restart activity and ensure direction applied
    fun applyLocaleAndRestart(activity: Activity, langCode: String) {
        saveLanguagePreference(activity, langCode)
        setLocale(activity, langCode)

        val intent = Intent(activity, activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.finish()
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}
