package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import pl.pawelkleczkowski.customgauge.CustomGauge
import java.util.*


class GaugeViewAdapter internal constructor(
    val context: Context,
    val data: MutableList<ObdMetric>,
    private val resourceId: Int,
    private val height: Int
) :
    RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {
    var metrics: MutableList<ObdMetric> = data
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(metrics, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)
        view.layoutParams.height = height
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = metrics.elementAt(position)
        holder.labelTextView.text = metric.command.label
        holder.unitsTextView.text = (metric.command as ObdCommand).pid.units
        holder.valueTextView.text = metric.valueToString()

        var gauge: CustomGauge? =  holder.itemView.findViewById(R.id.gauge_view_id)
        gauge?.startValue = (metric.command as ObdCommand).pid.min?.toInt()
        gauge?.endValue = (metric.command as ObdCommand).pid.max?.toInt()
        gauge?.value = metric.valueToLong()?.toInt()

        val statistic =
            DataLogger.INSTANCE.statistics().findBy(metric.command.pid)
        holder.minTextView.text = statistic.min.toString()
        holder.maxTextView.text = statistic.max.toString()
    }

    override fun getItemCount(): Int {
        return metrics.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var labelTextView: TextView = itemView.findViewById(R.id.label)
        var valueTextView: TextView = itemView.findViewById(R.id.value)
        var unitsTextView: TextView = itemView.findViewById(R.id.unit)
        var minTextView: TextView = itemView.findViewById(R.id.min_value)
        var maxTextView: TextView = itemView.findViewById(R.id.max_value)
    }
}