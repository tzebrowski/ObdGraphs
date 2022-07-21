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
import org.obd.graphs.ui.common.convert
import org.obd.graphs.ui.common.setText


private const val ITEM_COLOR = "#01804F"

class TripViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<SensorData>
) :
    RecyclerView.Adapter<TripViewAdapter.ViewHolder>() {
    var mData: MutableCollection<SensorData> = data
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
        val pidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()
        val pid = pidDefinitionRegistry.findBy(metric.id)
        holder.metricName.setText(pid.description, Color.GRAY, 1.0f)
        holder.metricMode.setText(pid.mode, Color.parseColor(ITEM_COLOR), 1.0f)
        metric.run {
            holder.metricMaxValue.setText(
                "${convert(pid, max)}",
                Color.parseColor(ITEM_COLOR),
                1.0f
            )
            holder.metricMinValue.setText(
                "${convert(pid, min)}",
                Color.parseColor(ITEM_COLOR),
                1.0f
            )
            holder.metricMeanValue.setText(
                "${convert(pid, mean)}",
                Color.parseColor(ITEM_COLOR),
                1.0f
            )
        }
        holder.metricValue.visibility = View.GONE
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