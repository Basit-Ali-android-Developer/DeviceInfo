package com.example.deviceinfo

data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val size: String,
    val icon: android.graphics.drawable.Drawable
)
