package com.example.deviceinfo.storage

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.example.deviceinfo.navigation.MainActivity
import com.example.deviceinfo.R
import com.example.deviceinfo.databinding.FragmentStorageBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import kotlin.math.roundToInt

class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!

    private val waveEntries = ArrayList<Entry>()
    private var time = 0f
    private val usageHistory = mutableListOf<Float>()

    private lateinit var waveDataSet: LineDataSet
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            _binding?.let {
                displayRamInfo()
                updateWaveChart()
            }
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        setupChart()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayRamInfo()
        handler.post(updateRunnable) // start updates

        showInternalStorage()
        showExternalStorage()

        binding.btnAppStorage.setOnClickListener { v ->
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in))
            (activity as? MainActivity)?.switchToPage(7) // Network tab
        }
    }

    private fun displayRamInfo() {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)
        val usedRamMB = totalRamMB - (memoryInfo.availMem / (1024 * 1024))
        val freeRamMB = memoryInfo.availMem / (1024 * 1024)
        val usedPercentage = ((usedRamMB.toFloat() / totalRamMB.toFloat()) * 100).toInt()

        binding.ramTitle.text = getString(R.string.ram_title, totalRamMB)
        binding.ramUsed.text = getString(R.string.ram_used, usedRamMB)
        binding.ramFree.text = getString(R.string.ram_free, freeRamMB)

        binding.batteryProgress.progress = usedPercentage
        binding.batteryPercentage.text = "$usedPercentage%"

        val runningAppCount = activityManager.runningAppProcesses?.size ?: 0
        binding.runningAppsCount.text = getString(R.string.running_apps_count, runningAppCount)

        usageHistory.add(usedPercentage.toFloat())
        if (usageHistory.size > 10) usageHistory.removeAt(0)
        val avgUsage = usageHistory.average().roundToInt()
        binding.averageUsage.text = getString(R.string.average_usage, avgUsage)
    }

    private fun setupChart() {
        waveDataSet = LineDataSet(ArrayList(), "RAM Usage").apply {
            color = ContextCompat.getColor(requireContext(), R.color.Chart_filler)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.Chart_filler)
            fillAlpha = 150
        }

        binding.ramWaveChart.apply {
            data = LineData(waveDataSet)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            description = Description().apply { text = "" }
            setNoDataText("")
        }
    }

    private fun updateWaveChart() {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)
        val usedRamMB = totalRamMB - (memoryInfo.availMem / (1024 * 1024))
        val usedPercentage = ((usedRamMB.toFloat() / totalRamMB.toFloat()) * 100)

        time += 1f
        waveDataSet.addEntry(Entry(time, usedPercentage))

        if (waveDataSet.entryCount > 30) {
            waveDataSet.removeFirst()
            for (i in 0 until waveDataSet.entryCount) {
                waveDataSet.getEntryForIndex(i).x = i.toFloat()
            }
        }

        waveDataSet.notifyDataSetChanged()
        binding.ramWaveChart.data.notifyDataChanged()
        binding.ramWaveChart.notifyDataSetChanged()
        binding.ramWaveChart.invalidate()
    }

    private fun showInternalStorage() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - freeBytes

        val totalGB = totalBytes / (1024.0 * 1024 * 1024)
        val freeGB = freeBytes / (1024.0 * 1024 * 1024)
        val usedGB = usedBytes / (1024.0 * 1024 * 1024)
        val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

        binding.internalFreeSpace.text = String.format("%.1f GB", freeGB)
        binding.internalTotalSpace.text = String.format("%.1f GB", totalGB)
        binding.internalUsedSpace.text = String.format("%.1f GB", usedGB)
        binding.internalStoragePercentage.text = "$usedPercent%"
    }

    private fun showExternalStorage() {
        val externalDirs = requireContext().getExternalFilesDirs(null)
        if (externalDirs.size > 1 && externalDirs[1] != null) {
            val externalDir = externalDirs[1]!!
            val stat = StatFs(externalDir.path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - freeBytes

            val totalGB = totalBytes / (1024.0 * 1024 * 1024)
            val freeGB = freeBytes / (1024.0 * 1024 * 1024)
            val usedGB = usedBytes / (1024.0 * 1024 * 1024)
            val usedPercent = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()

            binding.externalTotalSpace.text = String.format("%.1f GB", totalGB)
            binding.externalUsedSpace.text = String.format("%.1f GB", usedGB)
            binding.externalFreeSpace.text = String.format("%.1f GB", freeGB)
            binding.externalUsedPercentage.text = "$usedPercent%"
            binding.externalPath.text = externalDir.path

            // Folder sizes calculated in background to prevent UI freeze
            getFolderSizeAsync(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)) {
                binding.externalDownloadsSize.text = getString(R.string.external_downloads_size, it)
            }
            getFolderSizeAsync(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)) {
                binding.externalMediaSize.text = getString(R.string.external_media_size, it)
            }
            getFolderSizeAsync(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)) {
                binding.externalDocumentsSize.text = getString(R.string.external_documents_size, it)
            }

            binding.externalStorageLayout.visibility = View.VISIBLE
            binding.externalNoStorageLayout.visibility = View.GONE
        } else {
            binding.externalStorageLayout.visibility = View.GONE
            binding.externalNoStorageLayout.visibility = View.VISIBLE
        }
    }

    private fun getFolderSizeAsync(dir: File?, callback: (String) -> Unit) {
        if (dir == null || !dir.exists()) {
            callback("0.0")
            return
        }
        Thread {
            val size = getFolderSizeBytes(dir)
            val result = String.format("%.1f", size / (1024.0 * 1024.0 * 1024.0))
            requireActivity().runOnUiThread { callback(result) }
        }.start()
    }

    private fun getFolderSizeBytes(dir: File): Long =
        dir.listFiles()?.sumOf { if (it.isFile) it.length() else getFolderSizeBytes(it) } ?: 0L

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}