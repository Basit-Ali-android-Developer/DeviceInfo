package com.example.deviceinfo.display

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.deviceinfo.R
import com.example.deviceinfo.databinding.FragmentDisplayBinding
import kotlin.math.pow
import kotlin.math.sqrt

class DisplayFragment : Fragment() {

    private var _binding: FragmentDisplayBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            context?.let { updateDynamicDisplayInfo(it) }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            showStaticDisplayInfo(it)
            loadDisplayDetails()
            handler.post(updateRunnable)
        }
    }

    private fun showStaticDisplayInfo(context: Context) {
        val dm = context.resources.displayMetrics
        val widthPixels = dm.widthPixels
        val heightPixels = dm.heightPixels
        val xdpi = dm.xdpi
        val ydpi = dm.ydpi

        val widthInches = widthPixels / xdpi
        val heightInches = heightPixels / ydpi
        val diagonalInches = sqrt(widthInches.pow(2) + heightInches.pow(2))
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.refreshRate ?: 60f
        }

        binding.textHeightInches.text = String.format("%.1f in", heightInches)
        binding.textWidthInches.text = String.format("%.1f in", widthInches)
        binding.textScreenSize.text = getString(R.string.Display_screen_size, diagonalInches)
        binding.textResolution.text = getString(R.string.Display_screen_resolution, widthPixels, heightPixels)
        binding.textDensity.text = getString(R.string.Display_screen_density, dm.densityDpi)
        binding.textRefreshRate.text = getString(R.string.Display_screen_refresh_rate, refreshRate.toInt())
    }

    private fun updateDynamicDisplayInfo(context: Context) {
        val resolver: ContentResolver = context.contentResolver

        // Brightness
        binding.textProgressBrightness.progress = try {
            val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            (brightness / 255f * 100).toInt()
        } catch (e: Exception) {
            0
        }

        // Screen timeout
        binding.textTimeout.text = try {
            val timeout = Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT)
            "${timeout / 1000} sec"
        } catch (e: Exception) {
            "N/A"
        }

        // Orientation
        val autoRotate = try {
            Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION) == 1
        } catch (e: Exception) { false }

        binding.textOrientation.text = if (autoRotate) {
            "Auto Rotate"
        } else {
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
                getString(R.string.Display_screen_orientation_01)
            else
                getString(R.string.Display_screen_orientation_02)
        }

        val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        binding.textDarkMode.text = when (uiMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> getString(R.string.Display_dark_o1)
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> getString(R.string.Display_dark_o2)
            else -> getString(R.string.Display_dark_o3)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadDisplayDetails() {
        val dm = resources.displayMetrics

        // Display type
        binding.textTypeValue.text = getString(R.string.Display_type_o1)

        // Color Depth / Version
        binding.textColorDepthValue.text = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getString(R.string.Display_version_o1)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> getString(R.string.Display_version_o2)
            else -> getString(R.string.Display_version_o3)
        }

        // Aspect ratio
        binding.textAspectRatioValue.text = String.format("%.2f : 1", dm.widthPixels.toFloat() / dm.heightPixels.toFloat())

        // Peak brightness
        binding.textPeakBrightnessValue.text = try {
            val brightness = Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            String.format("%.0f nits", (brightness / 255f) * 1000)
        } catch (e: Exception) {
            getString(R.string.Display_brightness_else)
        }

        // Pixel ratio
        binding.textPixelRatioValue.text = String.format("%.3f", dm.densityDpi.toFloat() / 160f)

        // Display cutout
        binding.textDisplayCutoutValue.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (cutout != null) getString(R.string.Display_cutout_o1) else getString(R.string.Display_cutout_o2)
        } else getString(R.string.Display_cutout_o3)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}