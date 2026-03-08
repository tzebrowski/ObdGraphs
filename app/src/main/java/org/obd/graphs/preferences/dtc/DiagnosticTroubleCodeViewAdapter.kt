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
package org.obd.graphs.preferences.dtc

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.setText
import org.obd.metrics.api.model.DiagnosticTroubleCode

internal class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?,
    private var data: List<DiagnosticTroubleCode>,
) : RecyclerView.Adapter<DiagnosticTroubleCodeViewAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    private var expandedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(mInflater.inflate(R.layout.item_dtc, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val item = data.elementAt(position)

        val formattedCode =
            if (!item.failureType?.code.isNullOrEmpty()) {
                "${item.standardCode}-${item.failureType.code}"
            } else {
                item.standardCode
            }

        var finalDescription = item.description
        var isUnknown = false

        if (finalDescription.isNullOrBlank() ||
            finalDescription.contains(
                "Unknown DTC Description",
                ignoreCase = true,
            )
        ) {
            isUnknown = true
            val fallbackParts =
                listOfNotNull(
                    item.system?.description,
                    item.category?.description,
                    item.subsystem?.description,
                ).filter { it.isNotBlank() }

            finalDescription =
                if (fallbackParts.isNotEmpty()) {
                    fallbackParts.joinToString(" → ") + " (Unknown specific fault)"
                } else {
                    "Unknown DTC Description"
                }
        }

        if (item.standardCode.isEmpty()) {
            holder.code.setText("", Color.GRAY, Typeface.NORMAL, 1f)
            holder.description.setText(finalDescription, Color.DKGRAY, Typeface.NORMAL, 1f)
            holder.expandedContainer.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            return
        }

        holder.code.setText(formattedCode, COLOR_CARDINAL, Typeface.BOLD, 1f)
        if (isUnknown) {
            holder.description.setText(finalDescription, Color.GRAY, Typeface.ITALIC, 1f)
        } else {
            holder.description.setText(finalDescription, Color.DKGRAY, Typeface.NORMAL, 1f)
        }

        val isExpanded = position == expandedPosition
        holder.expandedContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        val systemTxt = item.system?.description ?: "N/A"
        val categoryTxt = item.category?.description ?: "N/A"
        holder.detailSystemInfo.text = "System: $systemTxt | Category: $categoryTxt"

        val hex = item.rawHex ?: "N/A"
        val statusMask = String.format("0x%02X", item.statusMask)
        val activeStatuses = item.activeStatuses?.joinToString(", ") ?: "None"
        holder.detailHexStatus.text = "Hex: $hex | Mask: $statusMask\nStatus: $activeStatuses"

        holder.itemView.setOnClickListener {
            val previousExpandedPosition = expandedPosition

            expandedPosition =
                if (isExpanded) {
                    RecyclerView.NO_POSITION
                } else {
                    position
                }

            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(expandedPosition)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder internal constructor(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        var code: TextView = itemView.findViewById(R.id.dtc_value)
        var description: TextView = itemView.findViewById(R.id.dtc_description)

        var expandedContainer: LinearLayout = itemView.findViewById(R.id.dtc_expanded_details_container)
        var detailSystemInfo: TextView = itemView.findViewById(R.id.dtc_detail_system_info)
        var detailHexStatus: TextView = itemView.findViewById(R.id.dtc_detail_hex_status)
    }
}
