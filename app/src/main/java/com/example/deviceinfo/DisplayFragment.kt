package com.example.deviceinfo

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
//import com.example.deviceinfo.databinding.FragmentDisplayBinding
import kotlin.math.pow
import kotlin.math.sqrt

class DisplayFragment : Fragment() {

    private lateinit var textHeightInches: TextView
    private lateinit var textWidthInches: TextView
    private lateinit var textScreenSize: TextView
    private lateinit var textResolution: TextView
    private lateinit var textDensity: TextView
    private lateinit var textRefreshRate: TextView

    private lateinit var textProgressBrightness: ProgressBar
    private lateinit var textTimeout: TextView
    private lateinit var textOrientation: TextView
    private lateinit var textDarkMode: TextView

    private lateinit var textTypeValue: TextView
    private lateinit var textColorDepthValue: TextView
    private lateinit var textAspectRatioValue: TextView
    private lateinit var textPeakBrightnessValue: TextView
    private lateinit var textPixelRatioValue: TextView
    private lateinit var textDisplayCutoutValue: TextView

  //  private lateinit var binding : FragmentDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_display, container, false)

//        binding = FragmentDisplayBinding.inflate(inflater,container, false)

        textHeightInches = view.findViewById(R.id.textHeightInches)
        textWidthInches = view.findViewById(R.id.textWidthInches)
        textScreenSize = view.findViewById(R.id.textScreenSize)
        textResolution = view.findViewById(R.id.textResolution)
        textDensity = view.findViewById(R.id.textDensity)
        textRefreshRate = view.findViewById(R.id.textRefreshRate)

        textProgressBrightness = view.findViewById(R.id.textProgressBrightness)
        textTimeout = view.findViewById(R.id.textTimeout)
        textOrientation = view.findViewById(R.id.textOrientation)
        textDarkMode = view.findViewById(R.id.textDarkMode)

        textTypeValue = view.findViewById(R.id.textTypeValue)
        textColorDepthValue = view.findViewById(R.id.textColorDepthValue)
        textAspectRatioValue = view.findViewById(R.id.textAspectRatioValue)
        textPeakBrightnessValue = view.findViewById(R.id.textPeakBrightnessValue)
        textPixelRatioValue = view.findViewById(R.id.textPixelRatioValue)
        textDisplayCutoutValue = view.findViewById(R.id.textDisplayCutoutValue)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { ctx ->
            showDisplayInfo(ctx)
            loadDisplayEnvironment(ctx)
            loadDisplayDetails()
        }
    }

    private fun showDisplayInfo(context: Context) {
        val displayMetrics = context.resources.displayMetrics

        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val xdpi = displayMetrics.xdpi
        val ydpi = displayMetrics.ydpi

        val widthInches = widthPixels / xdpi
        val heightInches = heightPixels / ydpi
        val diagonalInches = sqrt(widthInches.pow(2) + heightInches.pow(2))
        val densityDpi = displayMetrics.densityDpi

        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.refreshRate ?: 60f
        }

         textHeightInches.text = String.format("%.1f in", heightInches)
         textWidthInches.text = String.format("%.1f in", widthInches)

        textScreenSize.text = getString(R.string.Display_screen_size, diagonalInches)
        textResolution.text = getString(R.string.Display_screen_resolution, widthPixels, heightPixels)
        textDensity.text = getString(R.string.Display_screen_density, densityDpi)
        textRefreshRate.text = getString(R.string.Display_screen_refresh_rate, refreshRate.toInt())

    }

    private fun loadDisplayEnvironment(context: Context) {
        val resolver: ContentResolver = context.contentResolver

        // Brightness
        try {
            val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            val brightnessPercent = (brightness / 255f * 100).toInt()
            textProgressBrightness.progress = brightnessPercent
        } catch (e: Exception) {
            e.printStackTrace()
            textProgressBrightness.progress = 0
        }

        // Screen timeout
        try {
            val timeout = Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT)
            val timeoutSeconds = timeout / 1000
            textTimeout.text = "$timeoutSeconds sec"
        } catch (e: Exception) {
            textTimeout.text = "N/A"
        }

        // Orientation
        val orientation = resources.configuration.orientation
        textOrientation.text =
            if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) getString(R.string.Display_screen_orientation_01) else getString(
                R.string.Display_screen_orientation_02
            )

        // Dark mode
        val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        textDarkMode.text = when (uiMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> getString(R.string.Display_dark_o1)
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> getString(R.string.Display_dark_o2)
            else -> getString(R.string.Display_dark_o3)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadDisplayDetails() {
        val displayMetrics = resources.displayMetrics

        // Display Type
        textTypeValue.text = getString(R.string.Display_type_o1)

        // Color Depth
        val colorDepth = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getString(R.string.Display_version_o1)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> getString(R.string.Display_version_o2)
            else -> getString(R.string.Display_version_o3)
        }
        textColorDepthValue.text = colorDepth

        // Aspect Ratio
        val ratio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()
        textAspectRatioValue.text = String.format("%.2f : 1", ratio)

        // Peak Brightness
        val peakBrightness = try {
            val brightness = Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val estimatedNits = (brightness / 255f) * 1000
            String.format("%.0f nits", estimatedNits)
        } catch (e: Exception) {
            getString(R.string.Display_brightness_else)
        }
        textPeakBrightnessValue.text = peakBrightness

        // Pixel Ratio
        textPixelRatioValue.text = String.format("%.3f", displayMetrics.densityDpi.toFloat() / 160f)

        // Display Cutout
        val hasCutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = activity?.window?.decorView?.rootWindowInsets
            if (windowInsets?.displayCutout != null) getString(R.string.Display_cutout_o1) else getString(
                R.string.Display_cutout_o2
            )
        } else {
            getString(R.string.Display_cutout_o3)
        }
        textDisplayCutoutValue.text = hasCutout
    }
}
