package org.openobd2.core.logger.ui.dash


import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.util.*
import kotlin.collections.ArrayList

class DashViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<CommandReply<*>>
) :
    RecyclerView.Adapter<DashViewAdapter.ViewHolder>() {
    var mData: MutableCollection<CommandReply<*>> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)



    class Segment {
        var from = 0
        var to = 0
        constructor(from:Int, to:Int){
            this.from = from
            this.to = to
        }
    }


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

        holder.pos(holder.segments, commandReply.value as Int).apply {
            val to = holder.segments.indexOf(this)
            (0..to - 1).reversed().forEach { e ->
                val dataSet = holder.chart!!.data.getDataSetByIndex(e) as BarDataSet
                dataSet.setColor(Color.rgb(104, 241, 175));
                dataSet.notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var chart: BarChart
        val numOfElemnts = 20
        lateinit var segments: List<Segment>;

        init {
            chart = itemView.findViewById(R.id.chart)

        }


        fun pos(segments: List<Segment>, value: Int): Segment {
            return segments.stream()
                .filter { r: Segment -> value >= r.from && value <= r.to }
                .findAny().get()
        }

        fun calculateRange(maxValue: Int): List<Segment> {
            val setSize = maxValue / numOfElemnts
            val list: MutableList<Segment> = LinkedList<Segment>()
            var cnt = setSize
            while (cnt <= maxValue) {
                list.add(Segment(cnt - setSize, cnt - 1))
                cnt += setSize
            }
            return list
        }


        fun buildChart(pid: PidDefinition?) {
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
            this.segments = calculateRange(pid!!.max.toInt())


            this.segments.forEach { i: Segment ->
                val v = i.to.toFloat()
                val values: ArrayList<BarEntry> = ArrayList()
                values.add(BarEntry(v,v))
                val set1 = BarDataSet(values, "")
                set1.setDrawIcons(false)
                set1.setDrawValues(false)
                set1.setColor(Color.rgb(187,187,187))
                dataSets.add(set1)
            }

            val data = BarData(dataSets)
            data.setDrawValues(false)

            data.barWidth = pid.max.toFloat()/numOfElemnts/1.2f
            chart!!.data = data
        }


    }





}