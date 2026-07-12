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
import org.obd.graphs.bl.datalogger.isUserCustom
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText
import java.util.Collections

class PidDefinitionViewAdapter internal constructor(
    private val context: Context?,
    var data: List<PidDefinitionDetails>,
    private val editModeEnabled: Boolean,
    private val onEditClicked: (PidDefinitionDetails) -> Unit,
    private val onDeleteClicked: (PidDefinitionDetails) -> Unit
) : RecyclerView.Adapter<PidDefinitionViewAdapter.ViewHolder>() {

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
            holder.description.setText(source.description ?: "", COLOR_RAINBOW_INDIGO, Typeface.BOLD, 1f)

            val longDesc = source.longDescription
            if (!longDesc.isNullOrEmpty() && longDesc != source.description) {
                holder.longDescription.visibility = View.VISIBLE
                holder.longDescription.setText(longDesc, Color.GRAY, Typeface.NORMAL, 0.85f)
            } else {
                holder.longDescription.visibility = View.GONE
            }

            if (source.stable) {
                holder.status.setText(context?.getString(R.string.pref_pid_manage_dialog_stable_yes), Color.GRAY, Typeface.NORMAL, 0.7f)
            } else {
                holder.status.setText(context?.getString(R.string.pref_pid_manage_dialog_stable_no), COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.7f)
            }

            holder.formula.text = source.formula ?: ""

            val lower = source.alert.lowerThreshold
            val upper = source.alert.upperThreshold
            val hasAlerts = lower != null || upper != null

            if (hasAlerts) {
                val alertsList = mutableListOf<String>()
                val minPrefix = context?.getString(R.string.pref_pid_manage_dialog_min_prefix)
                val maxPrefix = context?.getString(R.string.pref_pid_manage_dialog_max_prefix)

                if (lower != null) alertsList.add("$minPrefix $lower")
                if (upper != null) alertsList.add("$maxPrefix $upper")

                holder.alerts.text = "${context?.getString(R.string.pref_pid_manage_dialog_alerts_prefix)} ${alertsList.joinToString(", ")}"
            }

            if (editModeEnabled) {
                holder.selected.visibility = View.GONE
                holder.status.visibility = View.GONE
                holder.btnEdit.visibility = View.VISIBLE

                if (source.isUserCustom) {
                    holder.btnDelete.visibility = View.VISIBLE
                    holder.btnDelete.setOnClickListener {
                        onDeleteClicked(item)
                    }
                } else {
                    holder.btnDelete.visibility = View.GONE
                }

                holder.formula.visibility = View.VISIBLE
                holder.alerts.visibility = if (hasAlerts) View.VISIBLE else View.GONE
            } else {
                holder.selected.visibility = View.VISIBLE
                holder.status.visibility = View.VISIBLE
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE

                holder.formula.visibility = View.GONE
                holder.alerts.visibility = View.GONE
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
        val longDescription: TextView = binding.findViewById(R.id.pid_long_description)

        val formula: TextView = binding.findViewById(R.id.pid_formula)
        val alerts: TextView = binding.findViewById(R.id.pid_alerts)
        val status: TextView = binding.findViewById(R.id.pid_status)
        val selected: CheckBox = binding.findViewById(R.id.pid_selected)
        val btnEdit: ImageButton = binding.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = binding.findViewById(R.id.btnDelete)
    }
}
