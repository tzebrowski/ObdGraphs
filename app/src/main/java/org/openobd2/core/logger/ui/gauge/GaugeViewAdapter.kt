package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.ui.common.SpannableStringUtils
import org.openobd2.core.logger.ui.dash.round
import pl.pawelkleczkowski.customgauge.CustomGauge
import java.util.Collections


private val DEFAULT_HEIGHT = 230

class GaugeViewAdapter internal constructor(
    context: Context,
    val data: MutableList<ObdMetric>,
    private val resourceId: Int
) : RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var label: TextView = itemView.findViewById(R.id.label)
        var value: TextView = itemView.findViewById(R.id.value)
        var avgValue: TextView = itemView.findViewById(R.id.avg_value)
        var minValue: TextView = itemView.findViewById(R.id.min_value)
        var maxValue: TextView = itemView.findViewById(R.id.max_value)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)
        val measuredHeight = (parent.measuredHeight /2)
        if (measuredHeight > 0) {
            view.layoutParams.height = measuredHeight
        }else {
            view.layoutParams.height = DEFAULT_HEIGHT
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = data.elementAt(position)
        holder.label.text = metric.command.label

        val units = (metric.command as ObdCommand).pid.units
        holder.value.text = metric.valueToString() + " " + units

        val statistic = DataLogger.INSTANCE.statistics().findBy(metric.command.pid)
        holder.minValue.text = statistic.min.toString()
        holder.maxValue.text = statistic.max.toString()
        holder.avgValue.text = statistic.mean.round(2).toString()

        (holder.itemView.findViewById(R.id.gauge_view) as CustomGauge?)?.apply {
            startValue = (metric.command as ObdCommand).pid.min.toInt()
            endValue = (metric.command as ObdCommand).pid.max.toInt()
            value = metric.valueToLong().toInt()
        }

        SpannableStringUtils.setHighLightedText(
            holder.value, units, 0.3f,
            Color.parseColor("#01804F")
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }
}