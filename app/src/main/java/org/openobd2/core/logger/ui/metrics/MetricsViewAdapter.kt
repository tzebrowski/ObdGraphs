package org.openobd2.core.logger.ui.metrics

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
import org.openobd2.core.logger.ui.common.setText

class MetricsViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<ObdMetric>
) :
    RecyclerView.Adapter<MetricsViewAdapter.ViewHolder>() {
    var mData: MutableCollection<ObdMetric> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.livedata_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val commandReply = mData.elementAt(position)
        var valueTxt: String? = commandReply.valueToString()
        if (valueTxt != null) {
            valueTxt += " " + (commandReply.command as ObdCommand).pid.units
        }

        holder.metricNameTextView.setText(commandReply.command.label, Color.GRAY, 1.1f)
        holder.metricValueTextView.setText(valueTxt, Color.parseColor("#01804F"), 1.4f)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var metricNameTextView: TextView = itemView.findViewById(R.id.metric_name)
        var metricValueTextView: TextView = itemView.findViewById(R.id.metric_value)
        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
        }
    }
}