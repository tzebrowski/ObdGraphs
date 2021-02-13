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
import java.util.*

class GaugeViewAdapter internal constructor(
    context: Context,
    data: MutableList<ObdMetric>
) :
    RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {
    var mData: MutableList<ObdMetric> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(mData, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.gauge_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = mData.elementAt(position)
        holder.labelTextView.text = metric.command.label
        holder.unitsTextView.text = (metric.command as ObdCommand).pid.units
        holder.valueTextView.text = metric.valueToString()

        holder.minTextView.text = ""
        holder.maxTextView.text = ""

        val statistic =
            DataLogger.INSTANCE.statistics().findBy(metric.command.pid)
        holder.minTextView.text = statistic.min.toString()
        holder.maxTextView.text = statistic.max.toString()

    }

    override fun getItemCount(): Int {
        return mData.size
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