package com.example.deviceinfo.dashboard

data class DashboardItem(
    val title: String,
    val iconRes: Int,
    var info: String = "--",
    var progress: Int = 0
)
