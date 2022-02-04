package org.openobd2.core.logger.ui.graph

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
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
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.MetricsAggregator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GraphFragment : Fragment() {

    private var chart: LineChart? = null

    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)
        var data: MutableList<ObdMetric> = arrayListOf()
//        tfRegular = Typeface.createFromAsset(context?.assets!!, "penSans-Regular.ttf");
//        tfLight = Typeface.createFromAsset(context?.assets!!, "OpenSans-Light.ttf");

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            it?.let {
//                val indexOf = data.indexOf(it)
//                if (indexOf == -1) {
//                    data.add(it)
//                    //adapter.notifyItemInserted(data.indexOf(it))
//                } else {
//                    data[indexOf] = it
//                    //adapter.notifyItemChanged(indexOf, it)
//                }
                val entries = MetricsAggregator.data.groupBy { it.command.pid }.entries
            }
        })


        initializeChart(root)
        return root
    }

    private fun initializeChart(root: View) {
        chart = root.findViewById(R.id.graph_view_chart)

        // no description text

        // no description text
        chart!!.description.isEnabled = false

        // enable touch gestures

        // enable touch gestures
        chart!!.setTouchEnabled(true)

        chart!!.dragDecelerationFrictionCoef = 0.9f

        // enable scaling and dragging

        // enable scaling and dragging
        chart!!.isDragEnabled = true
        chart!!.setScaleEnabled(true)
        chart!!.setDrawGridBackground(false)
        chart!!.isHighlightPerDragEnabled = true

        // set an alternative background color

        // set an alternative background color
        chart!!.setBackgroundColor(Color.WHITE)
        chart!!.setViewPortOffsets(0f, 0f, 0f, 0f)


        // get the legend (only possible after setting data)

        // get the legend (only possible after setting data)
        val l = chart!!.legend
        l.isEnabled = false

        val xAxis = chart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE
//        xAxis.typeface = tfLight
        xAxis.textSize = 10f
        xAxis.textColor = Color.WHITE
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(true)
        xAxis.textColor = Color.rgb(255, 192, 56)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f // one hour

        //        xAxis.setValueFormatter(object : IAxisValueFormatter {
        //            private val mFormat: SimpleDateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH)
        //            override fun getFormattedValue(value: Float, axis: AxisBase): String {
        //                val millis: Long = TimeUnit.HOURS.toMillis(value.toLong())
        //                return mFormat.format(Date(millis))
        //            }
        //        })

        val leftAxis = chart!!.axisLeft
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        leftAxis.typeface = tfLight
        leftAxis.textColor = ColorTemplate.getHoloBlue()
        leftAxis.setDrawGridLines(true)
        leftAxis.isGranularityEnabled = true
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 170f
        leftAxis.yOffset = -9f
        leftAxis.textColor = Color.rgb(255, 192, 56)

        val rightAxis = chart!!.axisRight
        rightAxis.isEnabled = false
    }


    private fun updateChartData(entry: Map.Entry<PidDefinition,List<ObdMetric>>) {

        // now in hours
        val values = listOf<Entry>()

        entry.value.map { it }
        // increment by 1 hour
//        while (x < to) {
//            val y: Float = getRandom(range, 50)
//            values.add(MutableMap.MutableEntry<Any?, Any?>(x, y)) // add one entry per hour
//            x++
//        }

        // create a dataset and give it a type
        val set1 = LineDataSet(values, entry.key.description)
        set1.axisDependency = AxisDependency.LEFT
        set1.color = ColorTemplate.getHoloBlue()
        set1.valueTextColor = ColorTemplate.getHoloBlue()
        set1.lineWidth = 1.5f
        set1.setDrawCircles(false)
        set1.setDrawValues(false)
        set1.fillAlpha = 65
        set1.fillColor = ColorTemplate.getHoloBlue()
        set1.highLightColor = Color.rgb(244, 117, 117)
        set1.setDrawCircleHole(false)

        // create a data object with the data sets
        val data = LineData(set1)
        data.setValueTextColor(Color.WHITE)
        data.setValueTextSize(9f)

        // set data
        chart!!.data = data
    }
}