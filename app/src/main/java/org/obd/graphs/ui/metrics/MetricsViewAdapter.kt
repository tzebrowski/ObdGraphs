package org.obd.graphs.ui.metrics

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.convert
import org.obd.graphs.ui.common.setText


class MetricsViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<ObdMetric>
) :
    RecyclerView.Adapter<MetricsViewAdapter.ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.metric_item, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val metric = data.elementAt(position)
        var valueTxt: String? = metric.valueToString()
        if (valueTxt != null) {
            valueTxt += " " + (metric.command as ObdCommand).pid.units
        }

        holder.metricName.setText(metric.command.label, Color.GRAY, 1.2f)
        holder.metricMode.setText(metric.command.pid.mode, COLOR_PHILIPPINE_GREEN, 1.2f)

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.metricMaxValue.setText(
                metric.convert(max).toString(),
                COLOR_PHILIPPINE_GREEN,
                1.2f
            )
            holder.metricMinValue.setText(
                metric.convert(min).toString(),
                COLOR_PHILIPPINE_GREEN,
                1.2f
            )
            holder.metricMeanValue.setText(
                metric.convert(mean).toString(),
                COLOR_PHILIPPINE_GREEN,
                1.2f
            )
        }
        holder.metricValue.setText(valueTxt, COLOR_PHILIPPINE_GREEN, 1.3f)
    }

    override fun getItemCount(): Int {
        return data.size
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