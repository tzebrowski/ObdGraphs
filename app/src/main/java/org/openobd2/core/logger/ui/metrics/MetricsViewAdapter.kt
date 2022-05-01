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
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.ui.common.convert
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
        return ViewHolder(mInflater.inflate(R.layout.metric_item, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val metric = mData.elementAt(position)
        var valueTxt: String? = metric.valueToString()
        if (valueTxt != null) {
            valueTxt += " " + (metric.command as ObdCommand).pid.units
        }

        holder.metricName.setText(metric.command.label, Color.GRAY, 1.2f)
        holder.metricMode.setText(metric.command.pid.mode, Color.parseColor("#01804F"), 1.2f)

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.metricMaxValue.setText(
                metric.convert(max).toString(),
                Color.parseColor("#01804F"),
                1.2f
            )
            holder.metricMinValue.setText(
                metric.convert(min).toString(),
                Color.parseColor("#01804F"),
                1.2f
            )
            holder.metricMeanValue.setText(
                metric.convert(mean).toString(),
                Color.parseColor("#01804F"),
                1.2f
            )
        }
        holder.metricValue.setText(valueTxt, Color.parseColor("#01804F"), 1.3f)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var metricName: TextView = itemView.findViewById(R.id.metric_name)
        var metricValue: TextView = itemView.findViewById(R.id.metric_value)
        var metricMinValue: TextView = itemView.findViewById(R.id.metric_min_value)
        var metricMaxValue: TextView = itemView.findViewById(R.id.metric_max_value)
        var metricMode: TextView = itemView.findViewById(R.id.metric_mode)
        var metricMeanValue: TextView = itemView.findViewById(R.id.metric_avg_value)

        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
        }
    }
}