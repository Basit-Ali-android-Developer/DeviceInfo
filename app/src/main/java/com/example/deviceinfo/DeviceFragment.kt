package com.example.deviceinfo

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.deviceinfo.databinding.FragmentDeviceBinding
import java.util.*

class DeviceFragment : Fragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                loadUpTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)

    }

    override fun onResume() {
        super.onResume()

        loadSensorDetails()
    }

    private fun  loadSensorDetails(){

        with(binding) {

            val pm = requireContext().packageManager
            val km = requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

            // ---------------- Device Overview ----------------
            deviceName.text = "${Build.MODEL} (${Build.DEVICE})"
            manufacturer.text = Build.MANUFACTURER
            deviceType.text = getDeviceType()

            androidVersion.text = Build.VERSION.RELEASE
            sdkLevel.text = Build.VERSION.SDK_INT.toString()
            buildNumber.text = Build.DISPLAY

            securityPatch.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"

            // ---------------- System & Kernel ----------------
            kernel.text = System.getProperty("os.version") ?: "N/A"
            baseband.text = getBasebandVersion() ?: "N/A"

            language.text = Locale.getDefault().displayLanguage
            timezone.text = TimeZone.getDefault().id

            // ---------------- Security & Features ----------------
            screenLock.text = if (km.isKeyguardSecure) "PIN / Pattern / Password" else "None"
            biometric.text = if (hasBiometric(pm)) "Yes" else "No"
            encryption.text = if (isDeviceEncrypted(km)) "Enabled" else "Disabled"
            playServices.text = if (isGooglePlayServicesAvailable(pm)) "Installed" else "Not Installed"

            // ---------------- Hardware Features ----------------
            nfc.text = if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) "Yes" else "No"
            gps.text = if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) "Yes" else "No"
            camera.text = if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) "Yes" else "No"
        }


    }

    private fun  loadUpTime() {

        binding.uptime.text = formatUptime(SystemClock.elapsedRealtime())

    }


        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun getDeviceType(): String {
        val screenLayout = resources.configuration.screenLayout
        return if ((screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >=
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        ) "Tablet" else "Phone"
    }

    private fun formatUptime(milliseconds: Long): String {
        val seconds = milliseconds / 1000 % 60
        val minutes = milliseconds / (1000 * 60) % 60
        val hours = milliseconds / (1000 * 60 * 60) % 24
        val days = milliseconds / (1000 * 60 * 60 * 24)
        return "${days}d ${hours}h ${minutes}m ${seconds}s"
    }

    private fun hasBiometric(pm: PackageManager): Boolean {
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm.hasSystemFeature(PackageManager.FEATURE_FACE))
    }

    private fun isDeviceEncrypted(km: KeyguardManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) km.isDeviceSecure else false
    }

    private fun isGooglePlayServicesAvailable(pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getBasebandVersion(): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(clazz, "gsm.version.baseband") as String
        } catch (e: Exception) {
            null
        }
    }
}