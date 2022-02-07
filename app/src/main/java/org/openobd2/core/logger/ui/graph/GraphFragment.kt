package org.openobd2.core.logger.ui.graph

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
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
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.*
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator
import org.openobd2.core.logger.ui.common.Cache
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import java.text.SimpleDateFormat
import java.util.*

const val ACTION_TOGGLE_VALUES = "chart.action.actionToggleValues"
const val ACTION_TOGGLE_HIGHLIGHT = "chart.action.actionToggleHighlight"
const val ACTION_TOGGLE_FILLED = "chart.action.actionToggleFilled"

class GraphFragment : Fragment() {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DATA_LOGGER_NOTIFICATION_STOPPED -> {
                    chart?.run {
                        // xAxis.axisMinimum = firstVisibleRange ?: 0f
                        invalidate()
                    }
                }
                ACTION_TOGGLE_VALUES -> {
                    chart?.run {
                        data.dataSets.forEach {
                            it.setDrawValues(!it.isDrawValuesEnabled)
                        }
                        invalidate()
                    }
                }

                ACTION_TOGGLE_HIGHLIGHT -> {
                    chart?.run {
                      data.isHighlightEnabled = !data.isHighlightEnabled
                        invalidate()
                    }
                }

                ACTION_TOGGLE_FILLED -> {
                    chart?.run {
                        data.dataSets.forEach {
                            it.setDrawFilled(!it.isDrawFilledEnabled)
                        }
                        invalidate()
                    }
                }
            }
        }
    }

    private class GestureListener(val context: Context) : SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            context.sendBroadcast(Intent().apply {
                action = TOGGLE_TOOLBAR_ACTION
            })
            return true
        }
    }

    private class ReverseValueFormatter(val pid: PidDefinition, val scaler: Scaler): ValueFormatter(){
        override fun getFormattedValue(value: Float): String {
            return scaler.scaleToPidRange(pid, value).toString()
        }
    }

    private val xAxisFormatter = object: ValueFormatter() {
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
        override fun getFormattedValue(value: Float): String {
            return simpleDateFormat.format(Date(firstTimeStamp + value.toLong()))
        }
    }

    private var chart: LineChart? = null
    private val colorTemplate: IntIterator  = colorScheme()
    private val scaler  = Scaler()
    private var firstTimeStamp: Long = System.currentTimeMillis()
    private var firstVisibleRange: Float? = null
    private lateinit var preferences: GraphPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)

        preferences = getGraphPreferences()
        firstTimeStamp = System.currentTimeMillis()
        firstVisibleRange = null

        chart = initializeChart(root).apply {
            val metrics = DataLogger.INSTANCE.getEmptyMetrics(preferences.selectedPids)
            data = LineData(metrics.map { createDataSet(it) }.toList())
            val gestureDetector = GestureDetector(root.context, GestureListener(requireContext()))
            val onTouchListener: View.OnTouchListener = View.OnTouchListener { _, event -> gestureDetector.onTouchEvent(
                event
            ) }
            setOnTouchListener(onTouchListener)
            invalidate()
        }

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (preferences.selectedPids.contains(it.command.pid.id)) {
                    addEntry(it)
                }
            }
        })

        if (preferences.cacheEnabled) {
            firstTimeStamp = Cache[CACHE_TS_PROPERTY_NAME] as Long? ?: System.currentTimeMillis()
            Cache[CACHE_ENTRIES_PROPERTY_NAME]?.let {
                initFromCache(it as MutableMap<String, MutableList<Entry>>)
            }
        }

        registerReceivers()
        return root
    }

    private fun registerReceivers() {
        requireContext().registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_NOTIFICATION_STOPPED)
            addAction(ACTION_TOGGLE_VALUES)
            addAction(ACTION_TOGGLE_HIGHLIGHT)
            addAction(ACTION_TOGGLE_FILLED)
        })
    }

    private fun initFromCache(newCache: MutableMap<String, MutableList<Entry>>) {
        chart?.run {
            newCache.forEach { (label, entries) ->
                data.getDataSetByLabel(label, true)?.let { lineData ->
                    entries.forEach {  lineData.addEntry(it) }
                    data.notifyDataChanged()
                }
            }
            notifyDataSetChanged()
        }
    }

    private fun addEntry(obdMetric: ObdMetric) {
        chart?.run {
            data.getDataSetByLabel(obdMetric.command.pid.description, true)?.let {
                val timestamp = (System.currentTimeMillis() - firstTimeStamp).toFloat()
                val entry = Entry(timestamp, scaler.scaleToNewRange(obdMetric))
                it.addEntry(entry)
                data.notifyDataChanged()

                if (firstVisibleRange == null){
                    firstVisibleRange = timestamp
                }

                if (!visibleXRange.isNaN() && !visibleXRange.isInfinite()){

                    if (visibleXRange >= preferences.xAxisStartMovingAfter) {
                       xAxis.axisMinimum = xAxis.axisMinimum + preferences.xAxisMinimumShift
                    }
                }

                notifyDataSetChanged()
                invalidate()
            }
        }
    }

    private fun initializeChart(root: View) : LineChart {
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
//                granularity = 1f
            }

            axisRight.run {
                isEnabled = false
            }
         }
    }


    private fun createDataSet(obdMetric: ObdMetric) : LineDataSet {
        val values = mutableListOf<Entry>()
        val lineDataSet = LineDataSet(values, obdMetric.command.pid.description)
        val col = colorTemplate.nextInt()
        lineDataSet.run {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            label = obdMetric.command.pid.description
            lineDataSet.form = Legend.LegendForm.SQUARE
            axisDependency = AxisDependency.LEFT
            color = col
            valueTextColor = col
            lineWidth = 4f
            setDrawCircles(false)
            setDrawValues(true)
            setDrawFilled(true)
            fillColor = col
            valueFormatter = ReverseValueFormatter(obdMetric.command.pid, scaler)
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