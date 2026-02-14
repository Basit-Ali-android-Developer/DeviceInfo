package com.example.deviceinfo

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class DeviceInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceNameText: TextView = view.findViewById(R.id.deviceName)
        val manufacturerText: TextView = view.findViewById(R.id.manufacturer)
        val modelText: TextView = view.findViewById(R.id.model)
        val androidVersionText: TextView = view.findViewById(R.id.androidVersion)
        val apiLevelText: TextView = view.findViewById(R.id.apiLevel)

        // Get device info
        deviceNameText.text = "${Build.MANUFACTURER} ${Build.MODEL}"
        manufacturerText.text = "Manufacturer: ${Build.MANUFACTURER}"
        modelText.text = "Model: ${Build.MODEL}"
        androidVersionText.text = "Android Version: ${Build.VERSION.RELEASE}"
        apiLevelText.text = "API Level: ${Build.VERSION.SDK_INT}"
    }

}