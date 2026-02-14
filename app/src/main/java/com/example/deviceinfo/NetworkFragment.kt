package com.example.deviceinfo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.deviceinfo.databinding.FragmentNetworkBinding

class NetworkFragment : Fragment() {


    companion object {
        const val PERMISSION_REQUEST = 100
    }

    private var previousRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var previousTxBytes: Long = TrafficStats.getTotalTxBytes()

    private val handler = Handler(Looper.getMainLooper())
    private val speedUpdateRunnable = object : Runnable {
        override fun run() {
            loadNetworkInfo()
            loadSimInfo()
            updateWifiCard()
            updateMobileDataCard()
            handler.postDelayed(this, 2000)
        }
    }



    private lateinit var binding : FragmentNetworkBinding



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
         binding = FragmentNetworkBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAndLoadInfo()

        binding.btnRefreshNetwork.setOnClickListener {
            checkAndLoadInfo()
        }
        updateWifiCard()
        updateMobileDataCard()
        updateBluetoothNfcNetworkCard()
    }

    private fun checkAndLoadInfo() {
        val ctx = context ?: return

        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), PERMISSION_REQUEST)
        } else {
            startUpdatingNetwork()
            updateWifiCard()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val ctx = context ?: return

        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startUpdatingNetwork()
                updateWifiCard()
            } else {
                Toast.makeText(ctx, "Permissions required to show network and Wi-Fi info", Toast.LENGTH_LONG).show()
                showEmptyNetworkInfo()
                showNoSimLayout()
            }
        }
    }
    private fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }



    private fun showEmptyNetworkInfo() {
        binding.tvConnectionName.text = getString(R.string.Network_c1_offline_t1)
        binding.tvNetworkType.text = "-"
        binding.tvUploadSpeed.text = getString(R.string.Network_c1_offline_t2)
        binding.tvDownloadSpeed.text = getString(R.string.Network_c1_offline_t3)
        binding.imgNetworkIcon.setImageResource(R.drawable.offline)
    }

    private fun startUpdatingNetwork() {
        previousRxBytes = TrafficStats.getTotalRxBytes()
        handler.post(speedUpdateRunnable)
    }

    @SuppressLint("MissingPermission")
    private fun loadNetworkInfo() {
        val ctx = context ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)

        if (nc != null) {
            when {
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        ?: return

                    val wifiInfo = wifiManager.connectionInfo
                    var ssid = wifiInfo?.ssid?.replace("\"", "")

                    // 🔹 Handle unknown SSID issue (Android 10+)
                    if (ssid.isNullOrEmpty() || ssid.equals("<unknown ssid>", ignoreCase = true)) {
                        // Check if location is enabled
                        if (!isLocationEnabled(ctx)) {
                            ssid = getString(R.string.Network_wifi_location_disabled)
                        } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ssid = getString(R.string.Network_wifi_permission_denied)
                        } else {
                            ssid = getString(R.string.Network_c1_Wifi_t1) // fallback text
                        }
                    }

                    binding.tvConnectionName.text = ssid
                    binding.tvNetworkType.text = getString(R.string.Network_c1_wifi_t2)
                    binding.imgNetworkIcon.setImageResource(R.drawable.wifi)
                }

                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val carrier = getActiveDataCarrierName() ?: getString(R.string.Network_c1_mobiledata_t1)
                    binding.tvConnectionName.text = carrier
                    binding.imgNetworkIcon.setImageResource(R.drawable.mobile_data)
                    binding.tvNetworkType.text = getMobileNetworkType()
                }

                else -> {
                    showEmptyNetworkInfo()
                    return
                }
            }

            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()

            val rxBytesDiff = currentRxBytes - previousRxBytes
            val txBytesDiff = currentTxBytes - previousTxBytes

            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes

            val downloadSpeedKbps = (rxBytesDiff * 8) / 1024 / 2
            val uploadSpeedKbps = (txBytesDiff * 8) / 1024 / 2

            val downloadText = if (downloadSpeedKbps >= 1024) {
                val mbps = downloadSpeedKbps / 1024.0
                String.format("⬇ %.2f Mbps", mbps)
            } else {
                "⬇ $downloadSpeedKbps Kbps"
            }

            val uploadText = if (uploadSpeedKbps >= 1024) {
                val mbps = uploadSpeedKbps / 1024.0
                String.format("⬆ %.2f Mbps", mbps)
            } else {
                "⬆ $uploadSpeedKbps Kbps"
            }

            binding.tvDownloadSpeed.text = downloadText
            binding.tvUploadSpeed.text = uploadText
        } else {
            showEmptyNetworkInfo()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getActiveDataCarrierName(): String? {
        val ctx = context ?: return null
        val subscriptionManager = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return null

        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (defaultDataSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return getString(R.string.Network_c2_nosim_t1)

        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null
        val dataTm = telephonyManager.createForSubscriptionId(defaultDataSubId)
        return dataTm.simOperatorName ?: getString(R.string.Network_c2_nosim_t2)
    }

    @SuppressLint("MissingPermission")
    private fun getMobileNetworkType(): String {
        val ctx = context ?: return getString(R.string.Network_c2_sim_m1)
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (defaultDataSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return getString(R.string.Network_c2_sim_m1)

        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return getString(R.string.Network_c2_sim_m1)
        val dataTm = telephonyManager.createForSubscriptionId(defaultDataSubId)

        return when (dataTm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> getString(R.string.Network_c2_sim_t2_o1)
            TelephonyManager.NETWORK_TYPE_NR -> getString(R.string.Network_c2_sim_t2_o2)
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP -> getString(R.string.Network_c2_sim_t2_o3)
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS -> getString(R.string.Network_c2_sim_t2_o4)
            else -> getString(R.string.Network_c2_sim_m1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadSimInfo() {
        val ctx = context ?: return
        val subscriptionManager = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager ?: return
        val subscriptionList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

        when (subscriptionList.size) {
            0 -> showNoSimLayout()
            1 -> {
                val sim = subscriptionList[0]
                populateSim(sim, binding.tvSim1Name, binding.tvSim1Network, binding.tvSim1Roaming)
                binding.simOneLayout.visibility = View.VISIBLE
                binding.simTwoLayout.visibility = View.GONE
                binding.noSimLayout.visibility = View.GONE
            }
            else -> {
                populateSim(subscriptionList[0], binding.tvSim1Name, binding.tvSim1Network, binding.tvSim1Roaming)
                populateSim(subscriptionList[1], binding.tvSim2Name, binding.tvSim2Network, binding.tvSim2Roaming)
                binding.simOneLayout.visibility = View.VISIBLE
                binding.simTwoLayout.visibility = View.VISIBLE
                binding.noSimLayout.visibility = View.GONE
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun populateSim(sim: SubscriptionInfo, tvName: TextView, tvNetwork: TextView, tvRoaming: TextView) {
        val ctx = context ?: return
        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val tmForSim = telephonyManager.createForSubscriptionId(sim.subscriptionId)

        tvName.text = getString(R.string.sim_name_format, sim.displayName, sim.simSlotIndex + 1)


        val networkType = if (tmForSim.simState == TelephonyManager.SIM_STATE_READY) {
            when (tmForSim.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> getString(R.string.Network_c2_sim_o1)
                TelephonyManager.NETWORK_TYPE_NR -> getString(R.string.Network_c2_sim_o2)
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP -> getString(R.string.Network_c2_sim_o3)
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> getString(R.string.Network_c2_sim_o4)
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> getString(R.string.Network_c2_sim_o5)
                else -> getString(R.string.Network_c1_mobiledata_t1)
            }
        } else {
            getString(R.string.Display_cutout_o2)
        }

        tvNetwork.text = getString(R.string.Network_c3_t1, networkType)
        tvRoaming.text = getString(R.string.Network_c3_t2,
            if (tmForSim.isNetworkRoaming) getString(R.string.Network_c3_on) else getString(R.string.Network_c3_off)
        )

    }

    private fun showNoSimLayout() {
        binding.simOneLayout.visibility = View.GONE
        binding.simTwoLayout.visibility = View.GONE
        binding.noSimLayout.visibility = View.VISIBLE
    }

    @SuppressLint("MissingPermission")
    private fun updateWifiCard() {
        val ctx = context ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)

        if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            binding.linearLayoutWifi.visibility = View.VISIBLE
            binding.linearLayoutNoWifi.visibility = View.GONE

            val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            val wifiInfo = wifiManager.connectionInfo

            val ssid = wifiInfo?.ssid?.replace("\"", "") ?: getString(R.string.Network_c1_Wifi_t1)
            val ipAddress = (wifiInfo.ipAddress and 0xFF).toString() + "." +
                    ((wifiInfo.ipAddress shr 8) and 0xFF) + "." +
                    ((wifiInfo.ipAddress shr 16) and 0xFF) + "." +
                    ((wifiInfo.ipAddress shr 24) and 0xFF)
            val linkSpeed = wifiInfo.linkSpeed.coerceAtLeast(0)
            val frequency = wifiInfo.frequency
            val security = getWifiSecurityType(wifiManager, ssid)

            val displaySpeed = if (linkSpeed <= 0) "0 Kbps" else "$linkSpeed Mbps"

            binding.tvWifiSSID.text = ssid
            binding.tvWifiIP.text = "IP: $ipAddress"
            binding.tvWifiSpeed.text = displaySpeed
            binding.tvWifiFrequency.text = "${frequency / 1000} GHz"
            binding.tvWifiSecurity.text = security

            binding.progressWifiSpeed.progress = (linkSpeed.coerceAtMost(85))
        } else {
            binding.linearLayoutWifi.visibility = View.GONE
            binding.linearLayoutNoWifi.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun getWifiSecurityType(wifiManager: WifiManager, ssid: String): String {
        val ctx = context ?: return getString(R.string.Network_c1_mobiledata_t1)
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return getString(R.string.Network_c1_mobiledata_t1)

        val configuredNetworks = try {
            wifiManager.configuredNetworks
        } catch (e: SecurityException) {
            return getString(R.string.Network_c1_mobiledata_t1)
        }

        configuredNetworks?.forEach { config ->
            if (config.SSID.replace("\"", "") == ssid) {
                return when {
                    config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) -> "WPA/WPA2"
                    config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                            config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X) -> "WPA Enterprise"
                    config.wepKeys[0] != null -> "WEP"
                    else -> getString(R.string.Network_c3_wifi_connection)
                }
            }
        }
        return getString(R.string.Network_c1_mobiledata_t1)
    }

    @SuppressLint("MissingPermission")
    private fun updateMobileDataCard() {
        val ctx = context ?: return
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        // 🔹 Check Mobile Data Enabled
        val mobileDataEnabled = try {
            val method = tm.javaClass.getDeclaredMethod("getDataEnabled")
            method.isAccessible = true
            method.invoke(tm) as Boolean
        } catch (e: Exception) {
            false
        }

        // 🔹 Update Mobile Data Text & Icon
        binding.tvMobileDataStatus.text = getString(
            R.string.Network_c4_mobiledata_status,
            if (mobileDataEnabled) getString(R.string.Network_c4_on) else getString(R.string.Network_c4_off)
        )
        binding.mobileDataIcon.setColorFilter(
            ContextCompat.getColor(
                ctx,
                if (mobileDataEnabled) R.color.Network_connect1 else R.color.Network_not_connect
            )
        )

        // 🔹 Get SIM Operator Name
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        val subscriptionManager =
            ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val subInfo = subscriptionManager?.getActiveSubscriptionInfo(defaultDataSubId)
        binding.tvOperatorName.text =
            subInfo?.carrierName?.toString() ?: getString(R.string.Network_c1_mobiledata_t1)

        // 🔹 Check Hotspot (Wi-Fi Tethering) Status via Reflection
        val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
        method.isAccessible = true
        val hotspotActive = try {
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            false
        }

        // 🔹 Update Hotspot Text & Icon
        val hotspotStatusText = if (hotspotActive)
            getString(R.string.Network_c5_hotspot_on)
        else
            getString(R.string.Network_c5_hotspot_off)

        binding.tvHotspotStatus.text = getString(R.string.Network_c5_hotspot_status, hotspotStatusText)
        binding.hotspotIcon.setColorFilter(
            ContextCompat.getColor(
                ctx,
                if (hotspotActive) R.color.Network_connect2 else R.color.Network_not_connect
            )
        )
    }


    @SuppressLint("MissingPermission")
    private fun updateBluetoothNfcNetworkCard() {
        val act = activity ?: return
        val ctx = context ?: return

        // --- Bluetooth ---
        val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val btStatus = if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            val pairedDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                emptySet<android.bluetooth.BluetoothDevice>()
            } else {
                bluetoothAdapter.bondedDevices
            }

            val deviceName = pairedDevices.firstOrNull()?.name ?: getString(R.string.Network_bt_nodevice)
            getString(R.string.Network_bt_on, deviceName)
        } else {
            getString(R.string.Network_bt_off)
        }
        binding.tvBluetoothStatus.text = btStatus

        // --- NFC ---
        val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(ctx)
        binding.tvNFCStatus.text = if (nfcAdapter != null)
            getString(R.string.Network_nfc_supported)
        else
            getString(R.string.Network_nfc_notsupported)

        // --- Ping ---
        Thread {
            try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8")
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                val pingTime = Regex("time=(\\d+\\.\\d+)").find(output)?.groupValues?.get(1) ?: "-"
                act.runOnUiThread {
                    binding.tvPing.text = getString(R.string.Network_ping_result, pingTime)
                }
            } catch (e: Exception) {
                act.runOnUiThread { binding.tvPing.text = getString(R.string.Network_ping_na) }
            }
        }.start()

        // --- Public IP ---
        Thread {
            try {
                val publicIP = java.net.URL("https://api.ipify.org").readText()
                act.runOnUiThread { binding.tvPublicIP.text = getString(R.string.Network_publicip, publicIP) }
            } catch (e: Exception) {
                act.runOnUiThread { binding.tvPublicIP.text = getString(R.string.Network_publicip_na) }
            }
        }.start()

        // --- DNS ---
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nc = cm.activeNetwork
            val dnsServers = cm.getLinkProperties(nc)?.dnsServers?.joinToString(", ") ?: "N/A"
            binding.tvDNS.text = getString(R.string.Network_dns, dnsServers)
        } catch (e: Exception) {
            binding.tvDNS.text = getString(R.string.Network_dns_na)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(speedUpdateRunnable)
    }
}
