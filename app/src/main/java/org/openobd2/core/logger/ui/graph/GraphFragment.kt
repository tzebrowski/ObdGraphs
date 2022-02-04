package org.openobd2.core.logger.ui.graph

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.MetricsAggregator
import org.openobd2.core.logger.ui.common.MetricsViewContext
import org.openobd2.core.logger.ui.preferences.DashPreferences
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getLongSet



class GraphFragment : Fragment() {

    private var chart: LineChart? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)
        chart = initializeChart(root)

        val metricsViewContext = MetricsViewContext(viewLifecycleOwner, Prefs.getLongSet("pref.dash.pids.selected"))
        val sortOrderMap = DashPreferences.SERIALIZER.load(requireContext())?.map {
            it.id to it.position
        }!!.toMap()

        val metrics = metricsViewContext.findMetricsToDisplay(sortOrderMap).groupBy { obdMetric: ObdMetric ->  obdMetric.command.pid}

        val data = LineData(metrics.map { createDataSet(it) }.toList())
        data.setValueTextColor(Color.RED)
        data.setValueTextSize(9f)
        chart!!.data = data

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            it?.let {
               data.getDataSetByLabel(it.command.pid.description,true)?.run {
                   
                   addEntry(Entry(it.timestamp.toFloat(),it.valueToDouble().toFloat()))
                   chart!!.invalidate()
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
            legend.isEnabled = false
            xAxis.run{
                position = XAxis.XAxisPosition.TOP_INSIDE
                textSize = 10f
                textColor = Color.GREEN
                setDrawAxisLine(false)
                setDrawGridLines(true)
                textColor = Color.rgb(255, 192, 56)
                setCenterAxisLabels(true)
                granularity = 0.1f // one hour
            }

            axisLeft.run {
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                textColor = ColorTemplate.getHoloBlue()
                setDrawGridLines(true)
                isGranularityEnabled = true
                axisMinimum = 0f
                axisMaximum = 170f
                yOffset = -9f
                textColor = Color.rgb(255, 192, 56)
            }
            axisRight.isEnabled = false

        }
        return chart
    }


    private fun createDataSet(entry: Map.Entry<PidDefinition,List<ObdMetric>>) : LineDataSet {

        val values = entry.value.map { Entry(it.timestamp.toFloat(),it.valueToDouble().toFloat()) }.toMutableList()
        val lineDataSet = LineDataSet(values, entry.key.description)
        lineDataSet.run {
            axisDependency = AxisDependency.LEFT
            color = ColorTemplate.getHoloBlue()
            valueTextColor = ColorTemplate.getHoloBlue()
            lineWidth = 1.5f
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