/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.preferences.pid


import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText
import java.util.*

class PIDsViewAdapter internal constructor(
    context: Context?,
    var data: List<PidDefinitionDetails>
) : RecyclerView.Adapter<PIDsViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    fun swapItems(fromPosition: Int, toPosition: Int){
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_pids, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            holder.file.setText(source.resourceFile, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)
            holder.module.setText(source.module, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)
            holder.name.setText(source.description, COLOR_RAINBOW_INDIGO, Typeface.NORMAL, 1f)

            if (source.stable) {
                holder.status.setText("Yes", Color.GRAY, Typeface.NORMAL, 0.7f)
            } else {
                holder.status.setText("No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.7f)
            }

            if (supported) {
                holder.supported.setText("Yes", Color.GRAY, Typeface.NORMAL, 0.7f)
            } else {
                holder.supported.setText("No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.7f)
            }

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) {
                    checked = isChecked
                }
            }
            val lowerThreshold = source.alert.lowerThreshold
            val upperThreshold = source.alert.upperThreshold
            if (lowerThreshold != null || upperThreshold != null){
                var text =  ""
                if (lowerThreshold != null){
                    text += " x<$lowerThreshold"
                }

                if (upperThreshold != null){
                    text += " x>$upperThreshold"
                }

                holder.alert.setText(text, Color.GRAY, Typeface.NORMAL, 0.6f)
            } else {
                holder.alert.setText("", Color.GRAY, Typeface.NORMAL, 0.6f)
            }
        }
    }

    fun updateData(data: List<PidDefinitionDetails>){
        this.data = data
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val file: TextView = itemView.findViewById(R.id.pid_file)
        val module: TextView = itemView.findViewById(R.id.pid_module)
        val name: TextView = itemView.findViewById(R.id.pid_name)
        val status: TextView = itemView.findViewById(R.id.pid_status)
        val selected: CheckBox = itemView.findViewById(R.id.pid_selected)
        val alert: TextView = itemView.findViewById(R.id.pid_alert)
        val supported: TextView = itemView.findViewById(R.id.pid_supported)

    }
}