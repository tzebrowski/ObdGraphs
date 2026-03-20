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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.setText
import org.obd.metrics.api.model.DiagnosticTroubleCode

internal class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?,
) : ListAdapter<DiagnosticTroubleCode, DiagnosticTroubleCodeViewAdapter.ViewHolder>(DiffCallback) {
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
        val dtc = getItem(position)

        val formattedCode =
            if (!dtc.failureType?.code.isNullOrEmpty()) {
                "${dtc.standardCode}-${dtc.failureType.code}"
            } else {
                dtc.standardCode
            }

        var finalDescription = dtc.description
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
                    dtc.system?.description,
                    dtc.category?.description,
                    dtc.subsystem?.description,
                ).filter { it.isNotBlank() }

            finalDescription =
                if (fallbackParts.isNotEmpty()) {
                    fallbackParts.joinToString(" → ") + " (Unknown specific fault)"
                } else {
                    "Unknown DTC Description"
                }
        }

        if (dtc.standardCode.isEmpty()) {
            holder.code.setText("", Color.GRAY, Typeface.NORMAL, 1f)
            holder.description.setText(finalDescription, Color.DKGRAY, Typeface.NORMAL, 1f)
            holder.expandedContainer.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            return
        } else {
            holder.code.setText(formattedCode, COLOR_CARDINAL, Typeface.BOLD, 1f)
            if (isUnknown) {
                holder.description.setText(finalDescription, Color.GRAY, Typeface.ITALIC, 1f)
            } else {
                holder.description.setText(finalDescription, Color.DKGRAY, Typeface.NORMAL, 1f)
            }
        }

        val systemTxt = dtc.system?.description ?: "N/A"
        val categoryTxt = dtc.category?.description ?: "N/A"
        holder.detailSystemInfo.text = "System: $systemTxt | Category: $categoryTxt"

        val hex = dtc.rawHex ?: "N/A"
        val statusMask = String.format("0x%02X", dtc.statusMask)
        val activeStatuses = dtc.activeStatuses?.joinToString(", ") ?: "None"
        holder.detailHexStatus.text = "Hex: $hex | Mask: $statusMask\nStatus: $activeStatuses"

        val snapshot = dtc.snapshot
        if (snapshot != null) {
            holder.snapshotContainer.visibility = View.VISIBLE
            val snapshotText = StringBuilder()

            snapshot.forEach { did ->
                val value = did.decodedValue ?: "N/A"
                val unit = did.definition?.units ?: ""
                val desc = did.definition?.description ?: "Unknown DID"
                snapshotText.append("• $desc: $value $unit\n")
            }

            holder.detailSnapshotInfo.text = snapshotText.toString().trimEnd()
        } else {
            holder.snapshotContainer.visibility = View.GONE
        }

        val isExpanded = position == expandedPosition
        holder.expandedContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val previousExpandedPosition = expandedPosition
            expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else position

            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(expandedPosition)
        }

        holder.actionSearchWeb.setOnClickListener {
            val query = "OBD2 DTC $formattedCode ${dtc.description ?: ""}".trim()
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            holder.itemView.context.startActivity(intent)
        }

        holder.actionCopyCode.setOnClickListener {
            val clipboard =
                holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            var clipText = "DTC: $formattedCode\nDescription: ${dtc.description ?: "Unknown"}\nSystem: $systemTxt\nStatus: $activeStatuses"

            if (dtc.snapshot != null) {
                clipText += "\n\nSnapshot Data:\n${holder.detailSnapshotInfo.text}"
            }

            val clip = ClipData.newPlainText("DTC Info", clipText)
            clipboard.setPrimaryClip(clip)

            Toast
                .makeText(
                    holder.itemView.context,
                    "Copied $formattedCode to clipboard",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    inner class ViewHolder internal constructor(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        var code: TextView = itemView.findViewById(R.id.dtc_value)
        var description: TextView = itemView.findViewById(R.id.dtc_description)

        var expandedContainer: LinearLayout =
            itemView.findViewById(R.id.dtc_expanded_details_container)
        var detailSystemInfo: TextView = itemView.findViewById(R.id.dtc_detail_system_info)
        var detailHexStatus: TextView = itemView.findViewById(R.id.dtc_detail_hex_status)

        var snapshotContainer: LinearLayout = itemView.findViewById(R.id.dtc_snapshot_container)
        var detailSnapshotInfo: TextView = itemView.findViewById(R.id.dtc_detail_snapshot_info)

        var actionSearchWeb: Button = itemView.findViewById(R.id.dtc_action_search_web)
        var actionCopyCode: Button = itemView.findViewById(R.id.dtc_action_copy_code)
    }

    companion object {
        private val DiffCallback =
            object : DiffUtil.ItemCallback<DiagnosticTroubleCode>() {
                override fun areItemsTheSame(
                    oldItem: DiagnosticTroubleCode,
                    newItem: DiagnosticTroubleCode,
                ): Boolean = oldItem.standardCode == newItem.standardCode && oldItem.rawHex == newItem.rawHex

                override fun areContentsTheSame(
                    oldItem: DiagnosticTroubleCode,
                    newItem: DiagnosticTroubleCode,
                ): Boolean = oldItem == newItem
            }
    }
}
