package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R

class GaugeViewAdapter internal constructor(
        context: Context?,
        data: MutableCollection<Metric<*>>
) :
        RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {
    var mData: MutableCollection<Metric<*>> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

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
        holder.valueTextView.text = metric.valueAsString()
        if (metric.statistic == null){
            holder.minTextView.text = ""
            holder.maxTextView.text = ""

        }else{
            holder.minTextView.text = metric.statistic.min.toString()
            holder.maxTextView.text = metric.statistic.max.toString()
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
        var labelTextView: TextView
        var valueTextView: TextView
        var unitsTextView: TextView
        var minTextView: TextView
        var maxTextView: TextView

        init {
            valueTextView = itemView.findViewById(R.id.value)
            labelTextView = itemView.findViewById(R.id.label)
            unitsTextView = itemView.findViewById(R.id.unit)
            minTextView = itemView.findViewById(R.id.min_value)
            maxTextView = itemView.findViewById(R.id.max_value)

        }
    }
}