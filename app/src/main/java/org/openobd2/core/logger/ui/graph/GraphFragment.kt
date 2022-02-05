package org.openobd2.core.logger.ui.graph

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getLongSet
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GraphFragment : Fragment() {


    val xyz = object: ValueFormatter() {
        val mFormat: SimpleDateFormat =
            SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH)

        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            val millis: Long = TimeUnit.HOURS.toMillis(value.toLong())
            val ret  = mFormat.format(Date(millis))

            Log.e("EEEEEE", "EEEEEEEE ${ret}")
            return ret
        }
    }

    private var chart: LineChart? = null
    var xx: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)
        chart = initializeChart(root)

        val visiblePids = Prefs.getLongSet("pref.graph.pids.selected")
        Log.i(
            "GraphFragment",
            "${visiblePids}"
        )
        val metrics = DataLogger.INSTANCE.getEmptyMetrics(visiblePids)
        val data = LineData(metrics.map { createDataSet(it) }.toList())
        data.setValueTextColor(Color.RED)
        data.setValueTextSize(9f)
        chart!!.data = data
        chart!!.invalidate()
        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (visiblePids.contains(it.command.pid.id)) {
                    data.getDataSetByLabel(it.command.pid.description, true)?.run {
                        Log.e(
                            "GraphFragment",
                            "${it.command.pid.description} = [ ${it.timestamp.toFloat()} : ${it.valueToDouble().toFloat()}]"
                        )
                        addEntryOrdered(Entry(it.timestamp.toFloat(), it.valueToDouble().toFloat()))

                        if (xx == 20) {
                            Log.e(
                                "GraphFragment",
                                "Update!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ${entryCount}"
                            )
                            chart!!.notifyDataSetChanged()
                            chart!!.refreshDrawableState()

                            chart!!.invalidate()
                            xx = 0;
                        }
                        xx++;

                    }
                }

            }
        })

        return root
    }

    private fun initializeChart(root: View) : LineChart {
        val chart: LineChart = root.findViewById(R.id.graph_view_chart)
        chart!!.run {
            description.isEnabled = false
            setTouchEnabled(true)
            dragDecelerationFrictionCoef = 0.9f

            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)
            isHighlightPerDragEnabled = true
            setBackgroundColor(Color.BLACK)
            setViewPortOffsets(0f, 0f, 0f, 0f)
            legend.isEnabled = true
            xAxis.run{
                position = XAxis.XAxisPosition.TOP_INSIDE
                textSize = 10f
                textColor = Color.GREEN
                setDrawAxisLine(true)
                setDrawGridLines(true)
                textColor = Color.rgb(255, 192, 56)
                setCenterAxisLabels(true)
                granularity = 1f // one hour
                valueFormatter = xyz
            }

            axisLeft.run {
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                textColor = ColorTemplate.getHoloBlue()
                setDrawGridLines(true)
                isGranularityEnabled = true
                axisMinimum = 0f
                axisMaximum = 1000f
//                yOffset = -9f
                textColor = Color.rgb(255, 192, 56)
            }
            axisRight.isEnabled = false

        }
        return chart
    }

    protected fun getRandom(range: Float, start: Float): Float {
        return (Math.random() * range).toFloat() + start
    }
    private fun createDataSet(obdMetric: ObdMetric) : LineDataSet {

        val values = mutableListOf<Entry>()

        if (false) {
            val now = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
            val to: Float = now + 1000f


            var x = now.toFloat()
            while (x < to) {
                val y: Float = getRandom(50f, 50f)

                values.add(Entry(x, y)) // add one entry per hour
                x++
            }
        }


        val lineDataSet = LineDataSet(values, obdMetric.command.pid.description)
        lineDataSet.run {
            axisDependency = AxisDependency.LEFT
            color = ColorTemplate.getHoloBlue()
            valueTextColor = ColorTemplate.getHoloBlue()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(true)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(244, 117, 117)
            setDrawCircleHole(false)
        }

        return lineDataSet
    }
}