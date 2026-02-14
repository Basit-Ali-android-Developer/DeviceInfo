package com.example.deviceinfo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.deviceinfo.databinding.FragmentDashBoardBinding
import java.io.File

class DashBoardFragment : Fragment() {

    private val PHONE_PERMISSION = Manifest.permission.READ_PHONE_STATE
    private val REQUEST_CODE = 100
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable



    private var previousRxBytes: Long = 0



    private lateinit var binding : FragmentDashBoardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBoardBinding.inflate(inflater, container, false)


        previousRxBytes = TrafficStats.getTotalRxBytes()



        return binding.root
    }

    fun loadFragmentAtPosition(position: Int) {
        (activity as? MainActivity)?.switchToPage(position)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadDeviceInfo()
        checkPhonePermission()
        loadBatteryLevel()
        loadRamUsage()
        setScreenDetails()
        showSensorCount()
        showInternalStorage()

        val cpuName = getCpuName()

        binding.textCpu.text = getString(R.string.DashBoard_cpu_t1, cpuName)

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                loadBatteryLevel()
                loadNetworkInfo()
                updateCpuInfo()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)

        binding.networkLayout.setOnClickListener { loadFragmentAtPosition(5) }
        binding.batteryLayout.setOnClickListener { loadFragmentAtPosition(2) }
        binding.ramLayout.setOnClickListener { loadFragmentAtPosition(3) }
        binding.sensorLayout.setOnClickListener { loadFragmentAtPosition(6) }
        binding.displayLayout.setOnClickListener { loadFragmentAtPosition(4) }
        binding.processorLayout.setOnClickListener { loadFragmentAtPosition(1) }
        binding.storageLayout.setOnClickListener { loadFragmentAtPosition(3) }
    }

    private fun checkPhonePermission() {
        context?.let { ctx ->
            if (ContextCompat.checkSelfPermission(ctx, PHONE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                activity?.let { act ->
                    ActivityCompat.requestPermissions(
                        act,
                        arrayOf(PHONE_PERMISSION),
                        REQUEST_CODE
                    )
                }
            } else {
                loadNetworkInfo()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadNetworkInfo()
        }
    }

    private fun loadDeviceInfo() {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        binding.textDeviceName.text = "$manufacturer $model"
    }

    @SuppressLint("MissingPermission")
    private fun getActiveDataCarrierName(): String {
        return context?.let { ctx ->
            val subscriptionManager = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (defaultDataSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return getString(R.string.DashBoard_message1)
            val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val dataTm = telephonyManager.createForSubscriptionId(defaultDataSubId)
            dataTm.simOperatorName ?: getString(R.string.DashBoard_message2)
        } ?: getString(R.string.DashBoard_message2)
    }

    @SuppressLint("MissingPermission")
    private fun loadNetworkInfo() {
        context?.let { ctx ->
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nc = cm.getNetworkCapabilities(cm.activeNetwork)
            if (nc != null) {
                when {
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val ssid = wifiManager.connectionInfo?.ssid ?: getString(R.string.DashBoard_message3)
                        binding.textNetworkName.text = if (ssid != getString(R.string.DashBoard_message3)) ssid.replace("\"", "") else getString(
                            R.string.Dashboard_message4
                        )
                    }
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        binding.textNetworkName.text = getString(R.string.Dashboard_network_mobiledata1, getActiveDataCarrierName())
                    }
                    else ->  binding.textNetworkName.text = getString(R.string.Dashboard_network_mobiledata2)
                }

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val bytesDiff = currentRxBytes - previousRxBytes
                previousRxBytes = currentRxBytes

                val kbps = (bytesDiff * 8) / 1024 / 2
                val speedText = if (kbps >= 1024) {
                    String.format(getString(R.string.Dashboard_network_speed1), kbps / 1024.0)
                } else "⬇ $kbps Kbps"
                binding.textNetworkSpeed.text = speedText
            } else {
                binding.textNetworkSpeed.text = getString(R.string.Dashboard_network_mobiledata2)
                binding.textNetworkSpeed.text = "--"
            }
        }
    }

    private fun loadBatteryLevel() {
        context?.let { ctx ->
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            binding.batteryProgress.progress = batteryPct
            binding.batteryText.text = "$batteryPct%"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadRamUsage() {
        context?.let { ctx ->
            val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val totalMem = memoryInfo.totalMem.toDouble()
            val availMem = memoryInfo.availMem.toDouble()
            val usedMem = totalMem - availMem
            val usedPercent = ((usedMem / totalMem) * 100).toInt()

            binding.ramProgress.progress = usedPercent
            binding.ramtext.text = "$usedPercent%"
            binding.ramReadingName.text = getString(
                R.string.ram_reading,
                (usedMem / (1024 * 1024)).toInt(),
                (totalMem / (1024 * 1024)).toInt()
            )}
    }

    private fun setScreenDetails() {
        val displayMetrics = resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val densityDpi = displayMetrics.densityDpi
        val xInches = widthPixels / displayMetrics.xdpi
        val yInches = heightPixels / displayMetrics.ydpi
        val diagonalInches = Math.sqrt((xInches * xInches + yInches * yInches).toDouble())
        binding.screenSize.text = String.format("%.1f\"", diagonalInches)
        binding.screenResolution.text = "$widthPixels x $heightPixels • $densityDpi dpi"
    }

    private fun showSensorCount() {
        context?.let { ctx ->
            val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
            binding.sensorCount.text = sensorList.size.toString()
        }
    }

    private fun showInternalStorage() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        val totalGB = totalBytes / (1024.0 * 1024 * 1024)
        val freeGB = freeBytes / (1024.0 * 1024 * 1024)
        val usedPercent = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()

        binding.storageFree.text = getString(R.string.storage_free, freeGB)
        binding.storageTotal.text = getString(R.string.storage_total, totalGB)



        binding.storagePercentage.text = "$usedPercent%"
        binding.storageProgress.progress = usedPercent
        context?.let { ctx ->
            binding.storageProgress.progressTintList = when {
                usedPercent < 60 -> ContextCompat.getColorStateList(ctx, android.R.color.holo_green_light)
                usedPercent < 85 -> ContextCompat.getColorStateList(ctx, android.R.color.holo_orange_light)
                else -> ContextCompat.getColorStateList(ctx, android.R.color.holo_red_light)
            }
        }
    }

    private fun updateCpuInfo() {
        val cpuUsage = getCpuUsageByApp()
        binding.cpuLoadProgress.progress = cpuUsage
        context?.let { ctx ->
            val colorRes = when {
                cpuUsage < 50 -> android.R.color.holo_green_light
                cpuUsage < 80 -> android.R.color.holo_orange_light
                else -> android.R.color.holo_red_light
            }
            binding.cpuLoadProgress.progressTintList = ContextCompat.getColorStateList(ctx, colorRes)
        }
    }

    private fun getCpuUsageByApp(): Int {
        return try {
            context?.let { ctx ->
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                val totalMem = info.totalMem.toDouble()
                val availMem = info.availMem.toDouble()
                (((totalMem - availMem) / totalMem) * 100).toInt().coerceIn(0, 100)
            } ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getCpuName(): String {
        return try {
            val reader = File("/proc/cpuinfo").bufferedReader()
            var cpuName = ""
            reader.forEachLine { line ->
                if (line.lowercase().startsWith("hardware") || line.lowercase().startsWith("model name")) {
                    cpuName = line.split(":")[1].trim()
                    return@forEachLine
                }
            }
            reader.close()
            if (cpuName.isNotEmpty()) cpuName else Build.HARDWARE.uppercase()
        } catch (e: Exception) {
            Build.HARDWARE.uppercase()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}
