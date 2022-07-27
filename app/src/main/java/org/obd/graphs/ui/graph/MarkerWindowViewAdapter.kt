package org.obd.graphs.ui.graph

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.api.model.ObdMetric
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_LIGHT_SHADE_GRAY
import org.obd.graphs.ui.common.setText

class MarkerWindowViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<ObdMetric>
) :
    RecyclerView.Adapter<MarkerWindowViewAdapter.ViewHolder>() {
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

        data.elementAt(position).run {
            holder.metricName.setText(command.label, COLOR_LIGHT_SHADE_GRAY, 1.0f)
            holder.metricValue.setText(valueToString(), COLOR_LIGHT_SHADE_GRAY, 1.1f)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val metricName: TextView = itemView.findViewById(R.id.metric_name)
        val metricValue: TextView = itemView.findViewById(R.id.metric_value)

        override fun onClick(view: View?) {
        }

        init {
            itemView.setOnClickListener(this)
            itemView.findViewById<TextView>(R.id.metric_max_value).apply {
                visibility = View.GONE
            }

            itemView.findViewById<TextView>(R.id.metric_min_value).apply {
                visibility = View.GONE
            }

            itemView.findViewById<TextView>(R.id.metric_avg_value).apply {
                visibility = View.GONE
            }
       }
    }
}