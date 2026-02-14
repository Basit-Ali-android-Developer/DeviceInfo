package com.example.deviceinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.databinding.ItemAppBinding

class AppsAdapter(
    private val apps: List<AppInfo>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appName.text = app.name
            binding.appVersion.text = binding.root.context.getString(R.string.app_version, app.version)
            binding.appSize.text = binding.root.context.getString(R.string.app_storage, app.size)

            binding.root.setOnClickListener {
                onItemClick(app.packageName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size
}
