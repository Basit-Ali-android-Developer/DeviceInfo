package com.example.deviceinfo

import android.content.*
import android.os.*
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.deviceinfo.databinding.FragmentBatteryBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class BatteryFragment : Fragment() {


    private lateinit var handler: Handler
    private var timeIndex = 0f
    private lateinit var lineDataSet: LineDataSet



    private lateinit var lowerHandler: Handler
    private lateinit var lowerRunnable: Runnable

    private lateinit var binding : FragmentBatteryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBatteryBinding.inflate(inflater, container, false)





        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        updateBatteryGraph() // initial point

        handler = Handler(Looper.getMainLooper())

        val updateRunnable = object : Runnable {
            override fun run() {
                updateBatteryGraph()
                handler.postDelayed(this, 30_000) // update every 30 seconds
            }
        }
        handler.post(updateRunnable)

        updateBatteryDetails()

        lowerHandler = Handler(Looper.getMainLooper())
        lowerRunnable = object : Runnable {
            override fun run() {
                updateBatteryDetails()
                lowerHandler.postDelayed(this, 2_000)
            }
        }
        lowerHandler.post(lowerRunnable)
    }

    private fun setupChart() {
        context?.let { ctx ->
            val bgColor = ContextCompat.getColor(ctx, R.color.chart_bg)
            val textColor = ContextCompat.getColor(ctx, R.color.chart_text)
            val lineColor = ContextCompat.getColor(ctx, R.color.chart_line)

            lineDataSet = LineDataSet(mutableListOf(Entry(0f, 0f)), "Battery Level (%)").apply {
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                color = lineColor
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillAlpha = 100
                fillColor = lineColor
                highLightColor = android.graphics.Color.TRANSPARENT
            }

            binding.batteryLineChart.apply {
                data = LineData(lineDataSet)
                setBackgroundColor(bgColor)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                animateX(1000)

                axisLeft.textColor = textColor
                axisLeft.setDrawGridLines(false)
                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    this.textColor = textColor
                    setDrawGridLines(false)
                    labelRotationAngle = -45f
                    granularity = 1f
                    valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter()
                }

                invalidate()
            }
        }
    }

    private fun updateBatteryGraph() {
        context?.let { ctx ->
            val batteryLevel = getBatteryLevel(ctx)

            lineDataSet.addEntry(Entry(timeIndex, batteryLevel.toFloat()))
            timeIndex += 0.5f

            while (lineDataSet.entryCount > 20) {
                lineDataSet.removeFirst()
                for (i in 0 until lineDataSet.entryCount) {
                    lineDataSet.getEntryForIndex(i).x = i * 0.5f
                }
                timeIndex = lineDataSet.entryCount * 0.5f
            }

            val data = binding.batteryLineChart.data
            data.notifyDataChanged()
            binding.batteryLineChart.notifyDataSetChanged()
            binding.batteryLineChart.setVisibleXRangeMaximum(10f)
            binding.batteryLineChart.moveViewToX(data.entryCount.toFloat())
            binding.batteryLineChart.invalidate()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 0
    }

    private fun updateBatteryDetails() {
        context?.let { ctx ->
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A"

            val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
            binding.batteryLevel.text = getString(R.string.battery_level, batteryPercent)



            binding.chargingStatus.text = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.Battery_t2_option1)
                BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.Battery_t2_option2)
                else -> getString(R.string.Battery_t2_option3)
            }
            binding.batteryHealth.text = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.Battery_t3_option1)
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.Battery_t3_option2)
                BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.Battery_t3_option3)
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.Battery_t3_option4)
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> getString(R.string.Battery_t3_option5)
                else -> getString(R.string.Battery_t3_option6)
            }

            binding.batteryVoltage.text = getString(R.string.Battery_t4, voltage)
            binding.batteryTemp.text = getString(R.string.Battery_t5, temp / 10.0)
            binding.batteryTech.text = getString(R.string.Battery_t6, tech)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        lowerHandler.removeCallbacksAndMessages(null)
    }
}
