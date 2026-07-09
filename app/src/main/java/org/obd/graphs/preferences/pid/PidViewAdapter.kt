/*
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
package org.obd.graphs.preferences.pid

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText
import java.util.Collections

class PidViewAdapter internal constructor(
    context: Context?,
    var data: List<PidDefinitionDetails>,
    private val editModeEnabled: Boolean,
    private val onEditClicked: (PidDefinitionDetails) -> Unit
) : RecyclerView.Adapter<PidViewAdapter.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_pid, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        item.run {
            holder.module.setText(source.resourceFile, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)

            val desc = if (source.longDescription.isNullOrEmpty()) source.description else source.longDescription
            holder.description.setText(desc, COLOR_RAINBOW_INDIGO, Typeface.NORMAL, 1f)

            if (source.stable) {
                holder.status.setText("Stable: Yes", Color.GRAY, Typeface.NORMAL, 0.7f)
            } else {
                holder.status.setText("Stable: No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.7f)
            }

            var formulaText = source.formula ?: ""
            val lower = source.alert.lowerThreshold
            val upper = source.alert.upperThreshold

            if (lower != null || upper != null) {
                val alerts = mutableListOf<String>()
                if (lower != null) alerts.add("x<$lower")
                if (upper != null) alerts.add("x>$upper")
                formulaText += " (Alerts: ${alerts.joinToString(", ")})"
            }
            holder.formula.text = formulaText

            if (editModeEnabled) {
                holder.selected.visibility = View.GONE
                holder.status.visibility = View.GONE
                holder.btnEdit.visibility = View.VISIBLE
                holder.formula.visibility = View.VISIBLE
            } else {
                holder.selected.visibility = View.VISIBLE
                holder.status.visibility = View.VISIBLE
                holder.btnEdit.visibility = View.GONE
                holder.formula.visibility = View.GONE
            }

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) checked = isChecked
            }

            holder.btnEdit.setOnClickListener {
                onEditClicked(item)
            }
        }
    }

    fun updateData(data: List<PidDefinitionDetails>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(binding: View) : RecyclerView.ViewHolder(binding) {
        val module: TextView = binding.findViewById(R.id.pid_module)
        val description: TextView = binding.findViewById(R.id.pid_description)
        val formula: TextView = binding.findViewById(R.id.pid_formula)
        val status: TextView = binding.findViewById(R.id.pid_status)
        val selected: CheckBox = binding.findViewById(R.id.pid_selected)
        val btnEdit: ImageButton = binding.findViewById(R.id.btnEdit)
    }
}
