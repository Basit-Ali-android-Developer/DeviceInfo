package com.example.deviceinfo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.databinding.FragmentAppsBinding
import java.io.File

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppsAdapter

    private val appList = mutableListOf<AppInfo>()
    private val filteredList = mutableListOf<AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { ctx ->
            binding.recycler.layoutManager = LinearLayoutManager(ctx)
            adapter = AppsAdapter(filteredList) { packageName ->
                openAppInfo(packageName)
            }
            binding.recycler.adapter = adapter
        }

        loadInstalledApps()

        // use extension from androidx.core.widget
        binding.searchBar.addTextChangedListener { editable ->
            filterApps(editable?.toString() ?: "")
        }
    }

    private fun loadInstalledApps() {
        val pm = context?.packageManager ?: return
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        appList.clear()
        for (app in packages) {
            // show only launchable apps
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                val name = pm.getApplicationLabel(app).toString()
                val icon = pm.getApplicationIcon(app)
                val version = try {
                    pm.getPackageInfo(app.packageName, 0).versionName ?: "N/A"
                } catch (e: Exception) {
                    "N/A"
                }
                val size = getAppSize(app)
                appList.add(AppInfo(name, app.packageName, version, size, icon))
            }
        }

        filteredList.clear()
        filteredList.addAll(appList)
        adapter.notifyDataSetChanged()
        binding.appCount.text = getString(R.string.Apps_count, appList.size)
    }

    private fun getAppSize(app: ApplicationInfo): String {
        return try {
            val file = File(app.sourceDir)
            val sizeInMB = file.length() / (1024 * 1024)
            "$sizeInMB MB"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun filterApps(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(appList)
        } else {
            val lower = query.lowercase()
            filteredList.addAll(appList.filter { it.name.lowercase().contains(lower) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun openAppInfo(packageName: String) {
        context?.let {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
