package org.obd.graphs.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.model.GradientColor
import org.obd.graphs.R
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.graphs.ui.common.highLightText
import org.obd.graphs.ui.common.isTablet
import org.obd.graphs.ui.gauge.SimpleAdapter
import org.obd.graphs.ui.preferences.Prefs
import java.util.*


class DashboardViewAdapter(
    context: Context,
    data: MutableList<ObdMetric>,
    resourceId: Int,
    height: Int? = null
) :
    SimpleAdapter<DashboardViewAdapter.ViewHolder>(context, data, resourceId, height) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View
    private val dashboardPreferences: DashboardPreferences by lazy { getDashboardPreferences() }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItemId(position: Int): Long {
        return data[position].command.pid.id
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)
        if (height != null) {
            view.layoutParams.height = height
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = data.elementAt(position)

        val obdCommand = metric.command as ObdCommand
        holder.buildChart(obdCommand.pid)

        val segmentNum: Int = holder.segments.indexOf(metric.valueToDouble())
        (segmentNum > 0).apply {
            //reset
            (0 until holder.chart.data.dataSetCount).reversed().forEach { e ->
                val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                dataSet.color = Color.parseColor("#0D000000")//transparent
            }

            (0..segmentNum).forEach { e ->
                (holder.chart.data.getDataSetByIndex(e) as BarDataSet).run {
                    color = Prefs.getInt("pref.dash.background_color_1", -1)
                    gradientColors = listOf(
                        GradientColor(
                            Prefs.getInt("pref.dash.background_color_1", -1),
                            Prefs.getInt("pref.dash.background_color_2", -1)
                        )
                    )

                }
            }

            val percent75: Int = (holder.segments.numOfSegments * 75) / 100
            if (segmentNum > percent75) {

                if (dashboardPreferences.colorsEnabled) {
                    (percent75..segmentNum).forEach { e ->
                        (holder.chart.data.getDataSetByIndex(e) as BarDataSet).run{
                            gradientColors = listOf(
                                GradientColor(
                                    Prefs.getInt("pref.dash.background_color_1", -1),
                                    Prefs.getInt("pref.dash.background_color_1", -1)
                                )
                            )
                        }
                    }
                }

                if (dashboardPreferences.blinkEnabled) {
                    if (!holder.anim.hasStarted() || holder.anim.hasEnded()) {
                        holder.itemView.startAnimation(holder.anim)
                    }
                }
            } else {
                if (dashboardPreferences.blinkEnabled) {
                    holder.itemView.clearAnimation()
                }
            }
        }

        holder.chart.invalidate()
        holder.label.text = obdCommand.pid.description
        val units = (metric.command as ObdCommand).pid.units
        val value = metric.valueToString() + " " + units
        holder.value.text = value
        holder.value.highLightText(units, 0.3f, Color.parseColor("#01804F"))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var chart: BarChart = itemView.findViewById(R.id.chart)
        var label: TextView = itemView.findViewById(R.id.dash_label)
        var value: TextView = itemView.findViewById(R.id.dash_value)
        var anim: Animation = AlphaAnimation(0.0f, 1.0f)

        lateinit var segments: Segments
        private var initialized: Boolean = false

        fun buildChart(pid: PidDefinition) {
            if (!initialized) {
                anim.run {
                    duration = 300
                    startOffset = 20
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                val numOfSegments = 30
                this.segments = Segments(numOfSegments, pid.min.toDouble(), pid.max.toDouble())

                this.label.text = pid.description
                chart.run {
                    description = Description()
                    legend.isEnabled = false
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(false)
                    setTouchEnabled(false)
                    setDrawBorders(false)
                    setAddStatesFromChildren(false)
                    description.isEnabled = false
                    setPinchZoom(false)
                    setDrawGridBackground(false)

                    xAxis.run {
                        spaceMax = 0.1f
                        spaceMin = 0.1f

                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        setDrawLabels(true)
                        setDrawGridLinesBehindData(false)
                        setDrawLimitLinesBehindData(false)
                        setDrawAxisLine(false)
                        setCenterAxisLabels(false)
                    }
                    axisLeft.run {
                        axisMinimum = pid.min.toFloat()
                        setDrawGridLines(false)
                        setDrawTopYLabelEntry(false)
                        setDrawAxisLine(false)
                        setDrawGridLinesBehindData(false)
                        setDrawLabels(false)
                        setDrawZeroLine(false)
                        setDrawLimitLinesBehindData(false)

                        setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                        spaceTop = 15f
                    }
                    legend.run {
                        verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                        orientation = Legend.LegendOrientation.HORIZONTAL
                        setDrawInside(false)
                        form = Legend.LegendForm.SQUARE
                    }
                    axisRight.run {
                        setDrawGridLines(false)
                    }
                }

                val dataSets: ArrayList<IBarDataSet> = ArrayList()

                this.segments.to().forEach { v: Double ->
                    val values: ArrayList<BarEntry> = ArrayList()
                    values.add(BarEntry(v.toFloat(), v.toFloat()))
                    val set1 = BarDataSet(values, "")
                    set1.setDrawIcons(false)
                    set1.setDrawValues(false)
                    dataSets.add(set1)
                }

                val barData = BarData(dataSets)
                barData.setDrawValues(false)

                barData.barWidth = pid.max.toFloat() / this.segments.numOfSegments / 1.05f
                chart.data = barData

                if (isTablet(context)) {
                    when (data.size) {
                        1 -> value.textSize *= 1.6f
                        2 -> value.textSize *= 1.5f
                        3 -> value.textSize *= 1.4f
                        4 -> value.textSize *= 1.3f
                        5 -> value.textSize *= 1.2f
                        6 -> value.textSize *= 1.2f
                        else -> value.textSize *= 1.1f
                    }
                }
                initialized = true
            }
        }
    }
}