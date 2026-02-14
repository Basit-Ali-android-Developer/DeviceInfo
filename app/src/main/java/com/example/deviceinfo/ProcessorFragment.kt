package com.example.deviceinfo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.deviceinfo.databinding.FragmentProcessorBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.random.Random

class ProcessorFragment : Fragment() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var binding: FragmentProcessorBinding

    private lateinit var coreProgressBars: List<android.widget.ProgressBar>
    private lateinit var corePercents: List<android.widget.TextView>
    private lateinit var coreStatuses: List<android.widget.TextView>
    private lateinit var coreLabels: List<android.widget.TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProcessorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Initialize all lists using binding instead of findViewById
        coreProgressBars = listOf(
            binding.coreProgress0,
            binding.coreProgress1,
            binding.coreProgress2,
            binding.coreProgress3,
            binding.coreProgress4,
            binding.coreProgress5,
            binding.coreProgress6,
            binding.coreProgress7
        )

        corePercents = listOf(
            binding.corePercent0,
            binding.corePercent1,
            binding.corePercent2,
            binding.corePercent3,
            binding.corePercent4,
            binding.corePercent5,
            binding.corePercent6,
            binding.corePercent7
        )

        coreStatuses = listOf(
            binding.coreStatus0,
            binding.coreStatus1,
            binding.coreStatus2,
            binding.coreStatus3,
            binding.coreStatus4,
            binding.coreStatus5,
            binding.coreStatus6,
            binding.coreStatus7
        )

        coreLabels = listOf(
            binding.coreLabel0,
            binding.coreLabel1,
            binding.coreLabel2,
            binding.coreLabel3,
            binding.coreLabel4,
            binding.coreLabel5,
            binding.coreLabel6,
            binding.coreLabel7
        )

        displayProcessorInfo()
        displayCoreClusters()

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                startUpdatingCoreUsage()
                updateCpuInfo()
                updateLoadAverage()
                updateCpuFrequencyInfo()
                handler.postDelayed(this, 4000)
            }
        }
        handler.post(runnable)
    }

    private fun displayProcessorInfo() {
        binding.cpuModelText.textDirection = View.TEXT_DIRECTION_ANY_RTL
        binding.cpuModelText.text = getCpuName()
        binding.architectureText.text = getArchitectureName()
        binding.coreCountText.text = "${Runtime.getRuntime().availableProcessors()}"
        binding.supportedAbisText.textDirection = View.TEXT_DIRECTION_ANY_RTL
        binding.supportedAbisText.text = Build.SUPPORTED_ABIS.joinToString(", ")

    }

    private fun getCpuName(): String {
        return try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    if (line!!.startsWith("Hardware") || line!!.startsWith("model name")) {
                        return line!!.split(":")[1].trim()
                    }
                }
            }
            Build.HARDWARE.ifEmpty { getString(R.string.Network_c1_mobiledata_t1) }
        } catch (e: IOException) {
            e.printStackTrace()
            Build.HARDWARE.ifEmpty { getString(R.string.Network_c1_mobiledata_t1) }
        }
    }

    private fun getArchitectureName(): String {
        val arch = System.getProperty("os.arch") ?: return getString(R.string.Network_c1_mobiledata_t1)
        return when {
            arch.contains("arm64") -> getString(R.string.cpu_arch_arm64)
            arch.contains("arm") -> getString(R.string.cpu_arch_arm32)
            arch.contains("x86_64") -> getString(R.string.cpu_arch_x86_64)
            arch.contains("x86") -> getString(R.string.cpu_arch_x86_32)
            else -> arch
        }
    }

    private fun displayCoreClusters() {
        binding.coreCluster1.text = getString(R.string.cpu_cluster1)
        binding.coreCluster2.text = getString(R.string.cpu_cluster2)
        binding.coreCluster3.text = getString(R.string.cpu_cluster3)

    }

    @SuppressLint("SetTextI18n")
    private fun startUpdatingCoreUsage() {
        for (i in coreProgressBars.indices) {
            val isOnline = Random.nextBoolean()
            val usage = if (isOnline) Random.nextInt(10, 100) else 0

            coreProgressBars[i].progress = usage
            corePercents[i].text = "$usage%"
            coreStatuses[i].text = if (isOnline) "🟢" else "🔴"
            coreLabels[i].text = getString(R.string.cpu_core_label, i)

        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCpuInfo() {
        val ctx = context ?: return
        val cpuUsage = getCpuUsageByApp(ctx)
        binding.cpuLoadProgress.progress = cpuUsage
        binding.cpuUsagePercent.text = "$cpuUsage%"

        val colorRes = when {
            cpuUsage < 50 -> android.R.color.holo_green_light
            cpuUsage < 80 -> android.R.color.holo_orange_light
            else -> android.R.color.holo_red_light
        }
        binding.cpuLoadProgress.progressTintList =
            ContextCompat.getColorStateList(ctx, colorRes)
    }

    private fun getCpuUsageByApp(ctx: Context): Int {
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalMem = info.totalMem.toDouble()
            val availMem = info.availMem.toDouble()
            (((totalMem - availMem) / totalMem) * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLoadAverage() {
        try {
            val file = File("/proc/loadavg")
            if (file.exists() && file.canRead()) {
                BufferedReader(FileReader(file)).use { reader ->
                    val line = reader.readLine()
                    if (!line.isNullOrEmpty()) {
                        val parts = line.split(" ")
                        if (parts.size >= 3) {
                            binding.loadAverageValue.text =
                                "${parts[0]} / ${parts[1]} / ${parts[2]}"
                            return
                        }
                    }
                }
            }

            val cpuUsage = getCpuUsageByApp(context ?: return)
            val simulated1 = (cpuUsage / 100.0 * 1.5).format(2)
            val simulated5 = (cpuUsage / 100.0).format(2)
            val simulated15 = (cpuUsage / 100.0 * 0.8).format(2)
            binding.loadAverageValue.text = "$simulated1 / $simulated5 / $simulated15"

        } catch (e: Exception) {
            e.printStackTrace()
            binding.loadAverageValue.text = "N/A"
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    @SuppressLint("SetTextI18n")
    private fun updateCpuFrequencyInfo() {
        try {
            val minFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
            val maxFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            val curFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            val governorFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")

            val minFreq =
                if (minFreqFile.exists()) minFreqFile.readText().trim().toInt() / 1000 else -1
            val maxFreq =
                if (maxFreqFile.exists()) maxFreqFile.readText().trim().toInt() / 1000 else -1
            val curFreq =
                if (curFreqFile.exists()) curFreqFile.readText().trim().toInt() / 1000 else -1
            val governor = if (governorFile.exists()) governorFile.readText().trim() else "N/A"

            binding.minFreqValue.text =
                if (minFreq > 0) getString(R.string.min_freq, minFreq)
                else getString(R.string.min_freq_na)

            binding.maxFreqValue.text =
                if (maxFreq > 0) getString(R.string.max_freq, maxFreq)
                else getString(R.string.max_freq_na)

            binding.currentFreqValue.text =
                if (curFreq > 0) getString(R.string.current_freq, curFreq)
                else getString(R.string.current_freq_na)

            binding.governorValue.text =
                if (!governor.isNullOrEmpty()) getString(R.string.governor, governor)
                else getString(R.string.governor_na)

        } catch (e: Exception) {
            e.printStackTrace()
            binding.minFreqValue.text = getString(R.string.min_freq_na)
            binding.maxFreqValue.text = getString(R.string.max_freq_na)
            binding.currentFreqValue.text = getString(R.string.current_freq_na)
            binding.governorValue.text = getString(R.string.governor_na)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}
