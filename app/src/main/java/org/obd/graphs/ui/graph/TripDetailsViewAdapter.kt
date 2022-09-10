package org.obd.graphs.ui.graph

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.trip.SensorData
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.setText
import org.obd.graphs.ui.common.toNumber


class TripDetailsViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<SensorData>
) :
    RecyclerView.Adapter<TripDetailsViewAdapter.ViewHolder>() {
    var mData: MutableCollection<SensorData> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_metric, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        mData.elementAt(position).let { metric ->
            val pid = DataLogger.instance.pidDefinitionRegistry().findBy(metric.id)
            holder.metricName.setText(pid.description, COLOR_PHILIPPINE_GREEN, 1.0f)
            metric.run {
                holder.metricMaxValue.setText(
                    "${toNumber(pid, max)}",
                    Color.GRAY,
                    1.0f
                )
                holder.metricMinValue.setText(
                    "${toNumber(pid, min)}",
                    Color.GRAY,
                    1.0f
                )
                holder.metricMeanValue.setText(
                    "${toNumber(pid, mean)}",
                    Color.GRAY,
                    1.0f
                )
            }
        }


    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val metricName: TextView = itemView.findViewById(R.id.metric_name)

        val metricMinValue: TextView = itemView.findViewById(R.id.metric_min_value)
        val metricMaxValue: TextView = itemView.findViewById(R.id.metric_max_value)
        val metricMeanValue: TextView = itemView.findViewById(R.id.metric_avg_value)

        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
            itemView.findViewById<TextView>(R.id.metric_value).apply {
                visibility = View.GONE
            }
        }
    }
}