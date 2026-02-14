package com.example.deviceinfo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val fragments = listOf(
        DashBoardFragment(),
//        DeviceInfoFragment(),
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
