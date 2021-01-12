package org.openobd2.core.logger.ui.dash


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.pid.PidDefinition

class DashViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<CommandReply<*>>
) :
    RecyclerView.Adapter<DashViewAdapter.ViewHolder>() {
    var mData: MutableCollection<CommandReply<*>> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.dash_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val commandReply = mData.elementAt(position)
        holder.buildChart((commandReply.command as ObdCommand).pid)

        holder.units.text = ((commandReply.command as ObdCommand).pid).units
        holder.value.text = commandReply.value?.toString()

        commandReply.value?.apply {
            val segmentNum: Int = holder.segments.indexOf(commandReply.value as Int)
            (segmentNum > 0).apply {

                val percent75: Int = (holder.segments.numOfSegments * 75)/100

                if (segmentNum > percent75){
                    (0..percent75).reversed().forEach { e ->
                        val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                        dataSet.setColor(Color.rgb(124, 252, 79));
                        dataSet.notifyDataSetChanged()
                    }
                    (percent75..segmentNum).reversed().forEach { e ->
                        val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                        dataSet.setColor(Color.rgb(194,38,54));
                        dataSet.notifyDataSetChanged()
                    }

                }else{
                    (0..segmentNum).reversed().forEach { e ->
                        val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                        dataSet.setColor(Color.rgb(124, 252, 79));
                        dataSet.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var chart: BarChart
        var label: TextView
        var value: TextView
        var units: TextView

        lateinit var segments: Segments
        var initialized: Boolean = false

        init {
            chart = itemView.findViewById(R.id.chart)
            label = itemView.findViewById(R.id.dash_label)
            units = itemView.findViewById(R.id.dash_units)
            value = itemView.findViewById(R.id.dash_value)
        }

        fun buildChart(pid: PidDefinition) {
            if (initialized) {
            } else {
                this.segments = Segments(30, pid!!.max.toInt())

                this.label.text = pid.description

                chart!!.setDrawBarShadow(false)
                chart!!.setDrawValueAboveBar(false)
                chart!!.setTouchEnabled(false)
                chart!!.description.isEnabled = false
                chart!!.setPinchZoom(false)
                chart!!.setDrawGridBackground(false)
                val xAxis = chart!!.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                val leftAxis = chart!!.axisLeft
                leftAxis.setDrawGridLines(false)
                leftAxis.setDrawTopYLabelEntry(false)
                leftAxis.setDrawAxisLine(false)
                leftAxis.setDrawGridLinesBehindData(false)
                leftAxis.setDrawLabels(false)
                leftAxis.setDrawZeroLine(false)
                leftAxis.setDrawLimitLinesBehindData(false)
                leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                leftAxis.spaceTop = 15f
                leftAxis.axisMinimum = 0f

                val rightAxis = chart!!.axisRight
                rightAxis.setDrawGridLines(false)

                val l = chart!!.legend
                l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                l.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                l.orientation = Legend.LegendOrientation.HORIZONTAL
                l.setDrawInside(false)
                l.form = Legend.LegendForm.SQUARE

                val dataSets: ArrayList<IBarDataSet> = ArrayList()


                this.segments.to().forEach { v: Float ->
                    val values: ArrayList<BarEntry> = ArrayList()
                    values.add(BarEntry(v, v))
                    val set1 = BarDataSet(values, "")
                    set1.setDrawIcons(false)
                    set1.setDrawValues(false)
                    set1.setColor(Color.rgb(187, 187, 187))
                    dataSets.add(set1)
                }

                val data = BarData(dataSets)
                data.setDrawValues(false)

                data.barWidth = pid.max.toFloat() / this.segments.numOfSegments / 1.2f
                chart!!.data = data
                initialized = true
            }
        }
    }
}