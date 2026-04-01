package com.example.deviceinfo.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.deviceinfo.apps.AppsFragment
import com.example.deviceinfo.battery.BatteryFragment
import com.example.deviceinfo.dashboard.DashBoardFragment
import com.example.deviceinfo.deviceinfo.DeviceFragment
import com.example.deviceinfo.display.DisplayFragment
import com.example.deviceinfo.network.NetworkFragment
import com.example.deviceinfo.processor.ProcessorFragment
import com.example.deviceinfo.sensors.SensorsFragment
import com.example.deviceinfo.storage.StorageFragment

class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val fragments = listOf(
        DashBoardFragment(),
        DeviceFragment(),
        ProcessorFragment(),
        BatteryFragment(),
        StorageFragment(),
        DisplayFragment(),
        NetworkFragment(),
        SensorsFragment(),
        AppsFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
