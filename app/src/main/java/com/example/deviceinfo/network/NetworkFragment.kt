package com.example.deviceinfo.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.deviceinfo.R
import com.example.deviceinfo.databinding.FragmentNetworkBinding
import java.net.URL

class NetworkFragment : Fragment() {

    companion object {
        const val PERMISSION_REQUEST = 100
    }

    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0

    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var speedUpdateRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()

        handler = Handler(Looper.getMainLooper())
        speedUpdateRunnable = object : Runnable {
            override fun run() {
                _binding?.let {
                    loadNetworkInfo()
                    loadSimInfo()
                    updateWifiConnection()
                    updateMobileConnection()
                    updateBluetoothNfcNetworkCard()
                }
                handler.postDelayed(this, 2000)
            }
        }

        checkAndLoadInfo()
        binding.btnRefreshNetwork.setOnClickListener { checkAndLoadInfo() }
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
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val ctx = context ?: return

        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startUpdatingNetwork()
            } else {
                Toast.makeText(ctx, "Permissions required to show network and Wi-Fi info", Toast.LENGTH_LONG).show()
                showEmptyNetworkInfo()
                showNoSimLayout()
            }
        }
    }

    private fun startUpdatingNetwork() {
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()
        handler.post(speedUpdateRunnable)
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
                    var ssid = wifiManager.connectionInfo?.ssid?.replace("\"", "")

                    if (ssid.isNullOrEmpty() || ssid.equals("<unknown ssid>", true)) {
                        ssid = if (!isLocationEnabled(ctx)) getString(R.string.Network_wifi_location_disabled)
                        else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                        ) getString(R.string.Network_wifi_permission_denied)
                        else getString(R.string.Network_c1_Wifi_t1)
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
                else -> showEmptyNetworkInfo()
            }

            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()

            val rxDiff = currentRxBytes - previousRxBytes
            val txDiff = currentTxBytes - previousTxBytes

            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes

            val downloadSpeedKbps = (rxDiff * 8) / 1024 / 2
            val uploadSpeedKbps = (txDiff * 8) / 1024 / 2

            binding.tvDownloadSpeed.text = if (downloadSpeedKbps >= 1024) {
                String.format("⬇ %.2f Mbps", downloadSpeedKbps / 1024.0)
            } else "⬇ $downloadSpeedKbps Kbps"

            binding.tvUploadSpeed.text = if (uploadSpeedKbps >= 1024) {
                String.format("⬆ %.2f Mbps", uploadSpeedKbps / 1024.0)
            } else "⬆ $uploadSpeedKbps Kbps"
        } else showEmptyNetworkInfo()
    }

    @SuppressLint("MissingPermission")
    private fun getActiveDataCarrierName(): String? {
        val ctx = context ?: return null
        val subscriptionManager = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return null
        val defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return getString(R.string.Network_c2_nosim_t1)
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.createForSubscriptionId(defaultSubId).simOperatorName ?: getString(R.string.Network_c2_nosim_t2)
    }

    @SuppressLint("MissingPermission")
    private fun getMobileNetworkType(): String {
        val ctx = context ?: return getString(R.string.Network_c2_sim_m1)
        val defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return getString(R.string.Network_c2_sim_m1)
        val tm = (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).createForSubscriptionId(defaultSubId)

        return when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> getString(R.string.Network_c2_sim_t2_o1)
            TelephonyManager.NETWORK_TYPE_NR -> getString(R.string.Network_c2_sim_t2_o2)
            TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSPAP -> getString(R.string.Network_c2_sim_t2_o3)
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> getString(R.string.Network_c2_sim_t2_o4)
            else -> getString(R.string.Network_c2_sim_m1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadSimInfo() {
        val ctx = context ?: return
        val subscriptionManager = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager ?: return
        val subs = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

        when (subs.size) {
            0 -> showNoSimLayout()
            1 -> {
                populateSim(subs[0], binding.tvSim1Name, binding.tvSim1Network, binding.tvSim1Roaming)
                binding.simOneLayout.visibility = View.VISIBLE
                binding.simTwoLayout.visibility = View.GONE
                binding.noSimLayout.visibility = View.GONE
            }
            else -> {
                populateSim(subs[0], binding.tvSim1Name, binding.tvSim1Network, binding.tvSim1Roaming)
                populateSim(subs[1], binding.tvSim2Name, binding.tvSim2Network, binding.tvSim2Roaming)
                binding.simOneLayout.visibility = View.VISIBLE
                binding.simTwoLayout.visibility = View.VISIBLE
                binding.noSimLayout.visibility = View.GONE
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun populateSim(sim: SubscriptionInfo, tvName: TextView, tvNetwork: TextView, tvRoaming: TextView) {
        val ctx = context ?: return
        val tm = (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).createForSubscriptionId(sim.subscriptionId)
        tvName.text = getString(R.string.sim_name_format, sim.displayName, sim.simSlotIndex + 1)
        val networkType = if (tm.simState == TelephonyManager.SIM_STATE_READY) {
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> getString(R.string.Network_c2_sim_o1)
                TelephonyManager.NETWORK_TYPE_NR -> getString(R.string.Network_c2_sim_o2)
                TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSPAP -> getString(
                    R.string.Network_c2_sim_o3
                )
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> getString(
                    R.string.Network_c2_sim_o4
                )
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> getString(R.string.Network_c2_sim_o5)
                else -> getString(R.string.Network_c1_mobiledata_t1)
            }
        } else getString(R.string.Display_cutout_o2)

        tvNetwork.text = getString(R.string.Network_c3_t1, networkType)
        tvRoaming.text = getString(
            R.string.Network_c3_t2, if (tm.isNetworkRoaming) getString(R.string.Network_c3_on) else getString(
                R.string.Network_c3_off
            ))
    }

    private fun showNoSimLayout() {
        binding.simOneLayout.visibility = View.GONE
        binding.simTwoLayout.visibility = View.GONE
        binding.noSimLayout.visibility = View.VISIBLE
    }

    @SuppressLint("MissingPermission")
    private fun updateWifiConnection() {
        val ctx = context ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)

        if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            binding.linearLayoutWifi.visibility = View.VISIBLE
            binding.linearLayoutNoWifi.visibility = View.GONE

            val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "") ?: getString(R.string.Network_c1_Wifi_t1)
            val ip = (wifiInfo.ipAddress and 0xFF).toString() + "." +
                    ((wifiInfo.ipAddress shr 8) and 0xFF) + "." +
                    ((wifiInfo.ipAddress shr 16) and 0xFF) + "." +
                    ((wifiInfo.ipAddress shr 24) and 0xFF)
            val speed = wifiInfo.linkSpeed.coerceAtLeast(0)
            val freq = wifiInfo.frequency
            binding.tvWifiSSID.text = ssid
            binding.tvWifiIP.text = "IP: $ip"
            binding.tvWifiSpeed.text = "$speed Mbps"
            binding.tvWifiFrequency.text = "${freq / 1000} GHz"
            binding.tvWifiSecurity.text = getWifiSecurityType()
            binding.progressWifiSpeed.progress = speed.coerceAtMost(85)
        } else {
            binding.linearLayoutWifi.visibility = View.GONE
            binding.linearLayoutNoWifi.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun getWifiSecurityType(): String {
        val ctx = context ?: return "-"
        val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connection = wifiManager.connectionInfo ?: return "-"

        val ssid = connection.ssid?.replace("\"", "") ?: return "-"
        // Use scan results to get security type
        val scanResults = wifiManager.scanResults
        val matching = scanResults.firstOrNull { it.SSID == ssid } ?: return "Unknown"

        return when {
            matching.capabilities.contains("WEP") -> "WEP"
            matching.capabilities.contains("WPA") -> "WPA/WPA2"
            matching.capabilities.contains("EAP") -> "WPA Enterprise"
            else -> "Open/Unknown"
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateMobileConnection() {
        val ctx = context ?: return
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val mobileDataEnabled = try {
            val method = tm.javaClass.getDeclaredMethod("getDataEnabled")
            method.isAccessible = true
            method.invoke(tm) as Boolean
        } catch (e: Exception) { false }

        binding.tvMobileDataStatus.text = getString(
            R.string.Network_c4_mobiledata_status,
            if (mobileDataEnabled) getString(R.string.Network_c4_on) else getString(R.string.Network_c4_off)
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateBluetoothNfcNetworkCard() {
        val ctx = context ?: return

        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        binding.tvBluetoothStatus.text = if (btAdapter != null && btAdapter.isEnabled)
            getString(R.string.Network_bt_on, btAdapter.name ?: "Device")
        else getString(R.string.Network_bt_off)

        val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(ctx)
        binding.tvNFCStatus.text = if (nfcAdapter != null)
            getString(R.string.Network_nfc_supported)
        else getString(R.string.Network_nfc_notsupported)

        Thread {
            try {
                val pingTime = Regex("time=(\\d+\\.\\d+)").find(Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8").inputStream.bufferedReader().readText())?.groupValues?.get(1) ?: "-"
                activity?.runOnUiThread { binding.tvPing.text = getString(R.string.Network_ping_result, pingTime) }
            } catch (e: Exception) { activity?.runOnUiThread { binding.tvPing.text = getString(R.string.Network_ping_na) } }
        }.start()

        Thread {
            try {
                val publicIP = URL("https://api.ipify.org").readText()
                activity?.runOnUiThread { binding.tvPublicIP.text = getString(R.string.Network_publicip, publicIP) }
            } catch (e: Exception) { activity?.runOnUiThread { binding.tvPublicIP.text = getString(R.string.Network_publicip_na) } }
        }.start()

        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val dnsServers = cm.getLinkProperties(cm.activeNetwork)?.dnsServers?.joinToString(", ") ?: "N/A"
            binding.tvDNS.text = getString(R.string.Network_dns, dnsServers)
        } catch (e: Exception) { binding.tvDNS.text = getString(R.string.Network_dns_na) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}