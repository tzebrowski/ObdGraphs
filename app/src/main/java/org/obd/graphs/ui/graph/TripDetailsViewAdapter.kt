 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.ui.graph

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.trip.SensorData
import org.obd.graphs.format
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.setText

class TripDetailsViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<SensorData>,
) : RecyclerView.Adapter<TripDetailsViewAdapter.ViewHolder>() {
    var mData: MutableCollection<SensorData> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(mInflater.inflate(R.layout.item_metric, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        mData.elementAt(position).let { metric ->
            val pid = DataLoggerRepository.getPidDefinitionRegistry().findBy(metric.id)
            holder.metricName.setText(pid.description, COLOR_PHILIPPINE_GREEN, 1.0f)
            metric.run {
                holder.metricMaxValue.setText(
                    max.format(pid),
                    Color.GRAY,
                    1.0f,
                )
                holder.metricMinValue.setText(
                    min.format(pid),
                    Color.GRAY,
                    1.0f,
                )
                holder.metricMeanValue.setText(
                    mean.format(pid),
                    Color.GRAY,
                    1.0f,
                )
            }
        }
    }

    override fun getItemCount(): Int = mData.size

    inner class ViewHolder internal constructor(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView),
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
