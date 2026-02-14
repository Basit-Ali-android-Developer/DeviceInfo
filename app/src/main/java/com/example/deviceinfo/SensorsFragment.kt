package com.example.deviceinfo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.deviceinfo.databinding.FragmentSensorsBinding
import com.example.deviceinfo.databinding.ItemSensorCardBinding

class SensorsFragment : Fragment() {

    private var _binding: FragmentSensorsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSensorDetails()
    }

    private fun loadSensorDetails() {
        val ctx = context ?: return
        val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        // Show total sensor count
        binding.sensorCount.text = getString(R.string.sensor_count, sensorList.size)

        // Clear old views
        binding.sensorListContainer.removeAllViews()

        if (sensorList.isEmpty()) {
            val emptyText = TextView(ctx)
            emptyText.text = getString(R.string.Sensors_message)
            emptyText.setTextColor(resources.getColor(R.color.Network_sim, ctx.theme))
            emptyText.textSize = 14f
            binding.sensorListContainer.addView(emptyText)
            return
        }

        // Inflate each sensor card with Data Binding
        val inflater = LayoutInflater.from(ctx)
        for (sensor in sensorList) {
            val cardBinding = ItemSensorCardBinding.inflate(inflater, binding.sensorListContainer, false)

            cardBinding.tvSensorName.text = sensor.name
            cardBinding.tvSensorDetails.text = cardBinding.root.context.getString(
                R.string.sensor_details,
                sensor.stringType,
                sensor.vendor,
                sensor.version.toString(),
                sensor.power.toString(),
                sensor.resolution.toString(),
                sensor.maximumRange.toString()
            )


            binding.sensorListContainer.addView(cardBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
