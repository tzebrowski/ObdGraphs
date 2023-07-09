package org.obd.graphs.ui.metrics

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.setText
import org.obd.graphs.ui.common.toNumber
import org.obd.metrics.command.obd.ObdCommand


class MetricsViewAdapter internal constructor(
    context: Context?,
    var data: MutableList<CarMetric>
) :
    RecyclerView.Adapter<MetricsViewAdapter.ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_metric, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val metric = data.elementAt(position)
        var valueTxt: String? = metric.source.valueToString()
        if (valueTxt != null) {
            valueTxt += " " + (metric.source.command as ObdCommand).pid.units
        }

        holder.metricName.setText(metric.source.command.label, COLOR_PHILIPPINE_GREEN,Typeface.NORMAL, 1.0f)

        holder.metricMaxValue.setText(
            metric.source.toNumber(metric.max).toString(),
            Color.GRAY,
            Typeface.NORMAL,
            1.0f
        )
        holder.metricMinValue.setText(
            metric.source.toNumber(metric.min).toString(),
            Color.GRAY,
            Typeface.NORMAL,
            1.0f
        )
        holder.metricMeanValue.setText(
            metric.source.toNumber(metric.mean).toString(),
            Color.GRAY,
            Typeface.NORMAL,
            1.0f
        )

        holder.metricValue.setText(valueTxt, Color.GRAY, 1.0f)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val metricName: TextView = itemView.findViewById(R.id.metric_name)
        val metricValue: TextView = itemView.findViewById(R.id.metric_value)
        val metricMinValue: TextView = itemView.findViewById(R.id.metric_min_value)
        val metricMaxValue: TextView = itemView.findViewById(R.id.metric_max_value)
        val metricMeanValue: TextView = itemView.findViewById(R.id.metric_avg_value)

        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
        }
    }
}