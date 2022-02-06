package org.openobd2.core.logger.ui.graph

import android.content.Context
import android.content.Intent
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
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getLongSet
import java.text.SimpleDateFormat
import java.util.*


class GraphFragment : Fragment() {

    class MetricScaler {

        private val NEW_RANGE_MIN_VAL = 0f
        private val NEW_RANGE_MAX_VAL = 2000f

        fun scaleToPidRange(
            pid: PidDefinition,
            value: Float
        ): Float {
            return if (pid.id == 13L){
                value
            }else{
                scaleToNewRange(value,NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL,pid.min.toFloat(),pid.max.toFloat())
            }
        }

        fun scaleToNewRange(
            obdMetric: ObdMetric
        ): Float {
            return if (obdMetric.command.pid.id == 13L){
                obdMetric.valueToDouble().toFloat()
            }else{
                scaleToNewRange(obdMetric.valueToDouble().toFloat(),obdMetric.command.pid.min.toFloat(),
                    obdMetric.command.pid.max.toFloat(), NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL)
            }
        }

        private fun scaleToNewRange(
            currentValue: Float,
            currentMin: Float,
            currentMax: Float,
            targetMin: Float,
            targetMax: Float
        ): Float {
            return (currentValue - currentMin) * (targetMax - targetMin) / (currentMax - currentMin) + targetMin
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

    private class ReverseValueFormatter(val pid: PidDefinition,val scaler: MetricScaler): ValueFormatter(){
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
    private var firstTimeStamp: Long = System.currentTimeMillis()
    private val generatedColors: IntIterator  = ColorTemplate.MATERIAL_COLORS.iterator()
    private val scaler: MetricScaler = MetricScaler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)

        val visiblePids = Prefs.getLongSet("pref.graph.pids.selected")
        firstTimeStamp = System.currentTimeMillis()

        val metrics = DataLogger.INSTANCE.getEmptyMetrics(visiblePids)

        chart = initializeChart(root).apply {
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
                if (visiblePids.contains(it.command.pid.id)) {
                    addEntry(it)
                }
            }
        })

        return root
    }

    private fun addEntry(obdMetric: ObdMetric) {
        chart?.run{
            data.getDataSetByLabel(obdMetric.command.pid.description, true)?.let {
                val timestamp = (System.currentTimeMillis() - firstTimeStamp).toFloat()
                val newVal = scaler.scaleToNewRange(obdMetric)
                it.addEntry(Entry(timestamp, newVal, obdMetric))
                data.notifyDataChanged()
                notifyDataSetChanged()
                moveViewToX(it.entryCount.toFloat())
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
            setViewPortOffsets(0f, 0f, 0f, 0f)

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
            xAxis.run{
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
                axisMaximum = 7200f
                textColor = Color.rgb(255, 192, 56)
            }

            axisRight.run {
                isEnabled = false
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                textColor = ColorTemplate.getHoloBlue()
                setDrawGridLines(true)
                isGranularityEnabled = true
                axisMinimum = -10f
                axisMaximum = 1000f
                textColor = Color.rgb(255, 192, 56)
            }
         }
    }


    private fun createDataSet(obdMetric: ObdMetric) : LineDataSet {
        val values = mutableListOf<Entry>()
        val lineDataSet = LineDataSet(values, obdMetric.command.pid.description)
        val col = generatedColors.nextInt()
        lineDataSet.run {
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
            valueFormatter = ReverseValueFormatter(obdMetric.command.pid,scaler)
            fillAlpha = 35
            fillColor = col
            highLightColor = Color.rgb(244, 117, 117)
            setDrawCircleHole(false)
            valueTextSize = 14f
        }
        return lineDataSet
    }
}