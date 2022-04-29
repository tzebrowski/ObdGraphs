package org.openobd2.core.logger.ui.graph

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.ui.common.setText
import org.openobd2.core.logger.ui.dashboard.round

class MarkerWindowViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<ObdMetric>
) :
    RecyclerView.Adapter<MarkerWindowViewAdapter.ViewHolder>() {
    var mData: MutableCollection<ObdMetric> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.metric_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val metric = mData.elementAt(position) as ObdMetric
        holder.metricName.setText(metric.command.label, Color.GRAY, 1.0f)
        holder.metricMode.setText(metric.command.pid.mode, Color.parseColor("#01804F"), 0.9f)

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.metricMaxValue.setText(convert(metric, max).toString(), Color.parseColor("#01804F"), 1.0f)
            holder.metricMinValue.setText(convert(metric, min).toString(), Color.parseColor("#01804F"), 1.0f)
        }
        holder.metricValue.setText(metric.valueToString(), Color.parseColor("#01804F"), 1.1f)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var metricName: TextView = itemView.findViewById(R.id.metric_name)
        var metricValue: TextView = itemView.findViewById(R.id.metric_value)
        var metricMaxValue: TextView = itemView.findViewById(R.id.metric_max_value)
        var metricMode: TextView = itemView.findViewById(R.id.metric_mode)
        var metricMinValue: TextView = itemView.findViewById(R.id.metric_min_value)

        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    private fun convert(metric: ObdMetric, value: Double): Number {
        if (value.isNaN()){
            return 0.0
        }
        return if (metric.command.pid.type == null) value.round(2) else
            metric.command.pid.type.let {
                return when (metric.command.pid.type) {
                    PidDefinition.ValueType.DOUBLE -> value.round(2)
                    PidDefinition.ValueType.INT -> value.toInt()
                    PidDefinition.ValueType.SHORT -> value.toInt()
                    else -> value.round(1)
                }
            }
    }
}