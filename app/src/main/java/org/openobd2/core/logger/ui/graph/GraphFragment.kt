package org.openobd2.core.logger.ui.graph

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.Cache
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.*
import org.openobd2.core.logger.bl.trip.TripRecorder
import org.openobd2.core.logger.ui.common.onDoubleClickListener
import org.openobd2.core.logger.ui.preferences.Prefs
import java.text.SimpleDateFormat
import java.util.*

private const val METRIC_COLLECTING_PROCESS_IS_RUNNING = "cache.graph.collecting_process_is_running"

class GraphFragment : Fragment() {

    private var prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "pref.graph.trips.selected") {
                sharedPreferences!!.getString(key,null)?.let {
                    if(!isDataCollectingProcessWorking()) {
                        context?.run {
                            tripRecorder.setCurrentTrip(it)
                        }
                    }
                }
            }
        }

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent? ) {
            when (intent?.action) {
                DATA_LOGGER_CONNECTING_EVENT -> {
                    Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] = true
                    initializeChart(root)
                }
                DATA_LOGGER_STOPPED_EVENT -> {
                    Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] = false
                }
            }
        }
    }

    private class ReverseValueFormatter(val pid: PidDefinition, val valueScaler: ValueScaler): ValueFormatter(){
        override fun getFormattedValue(value: Float): String {
            return valueScaler.scaleToPidRange(pid, value).toString()
        }
    }

    private val xAxisFormatter = object: ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return simpleDateFormat.format(Date(tripStartTs + value.toLong()))
        }
    }

    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
    private lateinit var chart: LineChart
    private var colors: IntIterator  = Colors().generate()
    private val valueScaler  = ValueScaler()
    private var tripStartTs: Long = System.currentTimeMillis()
    private lateinit var preferences: GraphPreferences
    private val tripRecorder: TripRecorder by lazy { TripRecorder.instance }
    private lateinit var root: View

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        root = inflater.inflate(R.layout.fragment_graph, container, false)

        Prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        colors  = Colors().generate()
        preferences = getGraphPreferences()

        initializeChart(root)

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (preferences.selectedPids.contains(it.command.pid.id)) {
                    addEntry(it)
                }
            }
        })

        loadCurrentTrip()
        registerReceivers()
        return root
    }

    private fun initializeChart(root: View) {
        chart = buildChart(root).apply {
            val metrics = DataLogger.instance.getEmptyMetrics(preferences.selectedPids)
            data = LineData(metrics.map { createDataSet(it.command.pid) }.toList())
            setOnTouchListener(onDoubleClickListener(requireContext()))
            invalidate()
        }
    }

    private fun loadCurrentTrip() {
        if (preferences.cacheEnabled) {
            val trip = tripRecorder.getCurrentTrip()
            tripStartTs = trip.startTs
            trip.entries.let { cache ->
                chart.run {
                    cache.forEach { (label, entries) ->
                        data.getDataSetByLabel(label, true)?.let { lineData ->
                            entries.forEach { entry ->  lineData.addEntry(entry) }
                            data.notifyDataChanged()
                        }
                    }

                    notifyDataSetChanged()

                    if (isDataCollectingProcessWorking()){
                        xAxis.axisMinimum  = 0f;
                    }else {
                        Log.i(LOGGER_KEY,"Set scale minima of XAxis to 6f")
                        setScaleMinima(6f, 0.1f)
                    }

                    invalidate()
                    debug("Reset view port")
                }
            }
        }
    }

    private fun LineChart.debug(label: String) {
        Log.i(
            "LineChart",
            "$label: axisMinimum=${xAxis.axisMinimum},axisMaximum=${xAxis.axisMaximum}, visibleXRange=${visibleXRange}"
        )
    }

    private fun isDataCollectingProcessWorking() =
        (Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] as Boolean?) ?: false

    private fun registerReceivers() {
        requireContext().registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
        })
    }

    private fun addEntry(obdMetric: ObdMetric) {
        chart.run {
            data.getDataSetByLabel(obdMetric.command.pid.description, true)?.let {
                val ts = (System.currentTimeMillis() - tripStartTs).toFloat()
                val entry = Entry(ts, valueScaler.scaleToNewRange(obdMetric))
                it.addEntry(entry)
                data.notifyDataChanged()
                notifyDataSetChanged()

                if (!xAxis.axisMaximum.isNaN() && !xAxis.axisMaximum.isInfinite()){
                    xAxis.axisMinimum = xAxis.axisMinimum + preferences.xAxisMinimumShift
                }
                invalidate()
            }
        }
    }

    private fun buildChart(root: View) : LineChart {
        return (root.findViewById(R.id.graph_view_chart) as LineChart).apply {
            description.isEnabled = false
            setTouchEnabled(true)
            dragDecelerationFrictionCoef = 0.9f
            isDoubleTapToZoomEnabled = false

            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)
            isHighlightPerDragEnabled = true
            setBackgroundColor(Color.BLACK)
            setViewPortOffsets(10f, 10f, 10f, 10f)

            legend.run {
                isEnabled = true
                form = Legend.LegendForm.LINE
                textColor = Color.WHITE
                textSize = 20f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                maxSizePercent = 0.20f
                xOffset = 40f
            }

            xAxis.run {
                position = XAxis.XAxisPosition.TOP_INSIDE
                textSize = 10f
                textColor = Color.GREEN
                setDrawAxisLine(true)
                setDrawGridLines(true)
                textColor = Color.rgb(255, 192, 56)
                setCenterAxisLabels(true)
                valueFormatter = xAxisFormatter
                isGranularityEnabled = true
            }

            axisLeft.run {
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                textColor = ColorTemplate.getHoloBlue()
                setDrawGridLines(true)
                setDrawMarkers(true)
                isGranularityEnabled = true

                axisMinimum = 0f
                axisMaximum = 5000f
                textColor = Color.rgb(255, 192, 56)
            }

            axisRight.run {
                isEnabled = false
            }


         }
    }

    private fun createDataSet(pid: PidDefinition) : LineDataSet {
        val values = mutableListOf<Entry>()
        val lineDataSet = LineDataSet(values, pid.description)
        val col = colors.nextInt()
        lineDataSet.run {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            label = pid.description
            lineDataSet.form = Legend.LegendForm.SQUARE
            axisDependency = AxisDependency.LEFT
            color = col
            valueTextColor = col
            lineWidth = 4f
            setDrawCircles(false)
            setDrawValues(true)
            setDrawFilled(true)
            fillColor = col
            valueFormatter = ReverseValueFormatter(pid, valueScaler)
            fillAlpha = 35
            fillColor = col
            highLightColor = Color.rgb(244, 117, 117)
            setDrawCircleHole(false)
            valueTextSize = 14f
            isHighlightEnabled = true
        }
        return lineDataSet
    }
}