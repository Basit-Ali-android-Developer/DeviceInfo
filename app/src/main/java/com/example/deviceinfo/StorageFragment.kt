package com.example.deviceinfo

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
import com.example.deviceinfo.databinding.FragmentStorageBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import kotlin.math.roundToInt

class StorageFragment : Fragment() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private val waveEntries = ArrayList<Entry>()
    private var time = 0f


    private val usageHistory = mutableListOf<Float>()



    private lateinit var binding : FragmentStorageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageBinding.inflate(inflater,container, false)


        setupChart()


        return binding.root
    }

    fun loadFragmentAtPosition(position: Int) {
        (activity as? MainActivity)?.switchToPage(position)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayRamInfo()

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                displayRamInfo()
                updateWaveChart()
                handler.postDelayed(this, 5000) // update every 5 sec
            }
        }
        handler.post(runnable)

        showInternalStorage()
        showExternalStorage()

        binding.btnAppStorage.setOnClickListener { v ->
            val ctx = context ?: return@setOnClickListener
            v.startAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in))
            loadFragmentAtPosition(7) // Network tab
        }
    }

    private fun displayRamInfo() {
        val ctx = context ?: return
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
        with(binding) {
            ramWaveChart.axisLeft.isEnabled = false
            ramWaveChart.axisRight.isEnabled = false
            ramWaveChart.xAxis.isEnabled = false
            ramWaveChart.legend.isEnabled = false
            ramWaveChart.setTouchEnabled(false)
            ramWaveChart.setScaleEnabled(false)
            ramWaveChart.description = Description().apply { text = "" }
            ramWaveChart.setNoDataText("")
        }
    }

    private fun updateWaveChart() {
        val ctx = context ?: return
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)
        val usedRamMB = totalRamMB - (memoryInfo.availMem / (1024 * 1024))
        val usedPercentage = ((usedRamMB.toFloat() / totalRamMB.toFloat()) * 100)

        time += 1f
        waveEntries.add(Entry(time, usedPercentage))

        if (waveEntries.size > 30) {
            waveEntries.removeAt(0)
            for (i in waveEntries.indices) {
                waveEntries[i].x = i.toFloat()
            }
        }

        val fillColorInt = ContextCompat.getColor(ctx, R.color.Chart_filler)

        val dataSet = LineDataSet(waveEntries, "RAM Usage").apply {
            color = fillColorInt
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = fillColorInt
            fillAlpha = 150
        }

        binding.ramWaveChart.data = LineData(dataSet)
        binding.ramWaveChart.invalidate()
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
        val usedGB = usedBytes / (1024.0 * 1024 * 1024)
        val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

        with(binding){

            internalFreeSpace.text = String.format("%.1f GB", freeGB)
            internalTotalSpace.text = String.format("%.1f GB", totalGB)
            internalUsedSpace.text = String.format("%.1f GB", usedGB)
            internalStoragePercentage.text = "$usedPercent%"

        }
    }

    private fun showExternalStorage() {
        val ctx = context ?: return
        val externalDirs = ctx.getExternalFilesDirs(null)

        if (externalDirs.size > 1 && externalDirs[1] != null) {
            val externalDir = externalDirs[1]!!
            val stat = StatFs(externalDir.path)

            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGB = totalBytes / (1024.0 * 1024 * 1024)
            val freeGB = freeBytes / (1024.0 * 1024 * 1024)
            val usedGB = usedBytes / (1024.0 * 1024 * 1024)
            val usedPercent = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()

            with(binding) {
                externalTotalSpace.text = String.format("%.1f GB", totalGB)
                externalUsedSpace.text = String.format("%.1f GB", usedGB)
                externalFreeSpace.text = String.format("%.1f GB", freeGB)
                externalUsedPercentage.text = "$usedPercent%"
            }

            binding.externalPath.text = externalDir.path

            binding.externalDownloadsSize.text = getString(
                R.string.external_downloads_size,
                getFolderSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            )

            binding.externalMediaSize.text = getString(
                R.string.external_media_size,
                getFolderSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            )

            binding.externalDocumentsSize.text = getString(
                R.string.external_documents_size,
                getFolderSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            )

            binding.externalStorageLayout.visibility = View.VISIBLE
            binding.externalNoStorageLayout.visibility = View.GONE

        } else {
            binding.externalStorageLayout.visibility = View.GONE
            binding.externalNoStorageLayout.visibility = View.VISIBLE
        }
    }

    private fun getFolderSize(dir: File?): String {
        if (dir == null || !dir.exists()) return "0.0"
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else getFolderSizeBytes(file)
        }
        return String.format("%.1f", size / (1024.0 * 1024.0 * 1024.0))
    }

    private fun getFolderSizeBytes(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else getFolderSizeBytes(file)
        }
        return size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}
