package com.example.deviceinfo.navigation

data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val size: String,
    val icon: android.graphics.drawable.Drawable
)
