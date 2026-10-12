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
package org.obd.graphs.preferences.dtc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.datalogger.dtc.DtcListItem
import org.obd.graphs.bl.datalogger.dtc.displayCode
import org.obd.graphs.bl.datalogger.dtc.displayDescription
import org.obd.graphs.bl.datalogger.dtc.displayName
import org.obd.graphs.bl.datalogger.dtc.isDescriptionUnknown
import org.obd.graphs.bl.datalogger.dtc.itemKey
import org.obd.graphs.bl.datalogger.dtc.toDidLines
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.setText

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_DTC = 1
private const val VIEW_TYPE_MESSAGE = 2

internal class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?
) : ListAdapter<DtcListItem, RecyclerView.ViewHolder>(DiffCallback) {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    // The expanded row by identity, not position: after a read or clear the list changes and
    // another row would sit at the old position.
    private var expandedKey: String? = null

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DtcListItem.ModuleHeader -> VIEW_TYPE_HEADER
            is DtcListItem.DtcRow -> VIEW_TYPE_DTC
            is DtcListItem.Message -> VIEW_TYPE_MESSAGE
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(mInflater.inflate(R.layout.item_dtc_module_header, parent, false))
            VIEW_TYPE_MESSAGE -> MessageViewHolder(mInflater.inflate(R.layout.item_dtc_message, parent, false))
            else -> DtcViewHolder(mInflater.inflate(R.layout.item_dtc, parent, false))
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {
            is DtcListItem.ModuleHeader -> (holder as HeaderViewHolder).bind(item)
            is DtcListItem.DtcRow -> bindDtc(holder as DtcViewHolder, item)
            is DtcListItem.Message -> (holder as MessageViewHolder).bind(item)
        }
    }

    private fun bindDtc(
        holder: DtcViewHolder,
        item: DtcListItem.DtcRow
    ) {
        val dtc = item.dtc
        val context = holder.itemView.context
        val notAvailable = context.getString(R.string.dtc_share_not_available)
        val formattedCode = dtc.displayCode()
        val finalDescription = dtc.displayDescription()
        val isUnknown = dtc.isDescriptionUnknown()
        // Codes stand out in red; descriptions in the dialog's primary text colour (DKGRAY was unreadable on dark).
        val primaryText = ContextCompat.getColor(context, R.color.dialog_text_primary)
        val secondaryText = ContextCompat.getColor(context, R.color.dialog_text_secondary)

        if (dtc.standardCode.isEmpty()) {
            holder.code.setText("", secondaryText, Typeface.NORMAL, 1f)
            holder.description.setText(finalDescription, secondaryText, Typeface.NORMAL, 1f)
            holder.expandedContainer.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            return
        } else {
            holder.code.setText(formattedCode, COLOR_CARDINAL, Typeface.BOLD, 1f)
            if (isUnknown) {
                holder.description.setText(finalDescription, secondaryText, Typeface.ITALIC, 1f)
            } else {
                holder.description.setText(finalDescription, primaryText, Typeface.NORMAL, 1f)
            }
        }

        val systemTxt = dtc.system?.description ?: notAvailable
        val categoryTxt = dtc.category?.description ?: notAvailable
        holder.detailSystemInfo.text = context.getString(R.string.dtc_share_system_category, systemTxt, categoryTxt)

        val hex = dtc.rawHex ?: notAvailable
        val statusMask = String.format("0x%02X", dtc.statusMask)
        val activeStatuses = dtc.activeStatuses?.joinToString(", ") ?: context.getString(R.string.dtc_share_none)
        holder.detailHexStatus.text = context.getString(R.string.dtc_detail_hex_status, hex, statusMask, activeStatuses)

        // Both are read only with "Read DTC snapshots" on; an empty list means the ECU sent nothing usable.
        val unknownDid = context.getString(R.string.dtc_share_unknown_did)
        val snapshotLines = dtc.snapshot.toDidLines(unknownDid, notAvailable)
        val showSnapshot = dataLoggerSettings.instance().adapter.dtcReadSnapshots && snapshotLines.isNotEmpty()
        holder.snapshotContainer.visibility = if (showSnapshot) View.VISIBLE else View.GONE
        holder.detailSnapshotInfo.text = snapshotLines.joinToString("\n") { "• $it" }

        val extendedLines = dtc.details.toDidLines(unknownDid, notAvailable)
        holder.extendedContainer.visibility = if (extendedLines.isNotEmpty()) View.VISIBLE else View.GONE
        holder.detailExtendedInfo.text = extendedLines.joinToString("\n") { "• $it" }

        val key = item.itemKey()
        val isExpanded = key == expandedKey
        holder.expandedContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val previousKey = expandedKey
            expandedKey = if (isExpanded) null else key

            previousKey?.let { notifyRowChanged(it) }
            expandedKey?.let { notifyRowChanged(it) }
        }

        holder.actionSearchWeb.setOnClickListener {
            val query = "OBD2 DTC $formattedCode ${dtc.description ?: ""}".trim()
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }

        holder.actionCopyCode.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            var clipText =
                listOf(
                    context.getString(R.string.dtc_share_dtc, formattedCode),
                    context.getString(R.string.dtc_share_description, dtc.description ?: context.getString(R.string.dtc_share_unknown)),
                    context.getString(R.string.dtc_copy_system, systemTxt),
                    context.getString(R.string.dtc_copy_status, activeStatuses)
                ).joinToString("\n")

            if (showSnapshot) {
                clipText += "\n\n${context.getString(R.string.dtc_freeze_frame_data)}:\n${holder.detailSnapshotInfo.text}"
            }
            if (extendedLines.isNotEmpty()) {
                clipText += "\n\n${context.getString(R.string.dtc_extended_data)}:\n${holder.detailExtendedInfo.text}"
            }

            val clip = ClipData.newPlainText("DTC Info", clipText)
            clipboard.setPrimaryClip(clip)

            Toast
                .makeText(
                    context,
                    context.getString(R.string.dtc_copied_to_clipboard, formattedCode),
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun notifyRowChanged(key: String) {
        val position = currentList.indexOfFirst { it.itemKey() == key }
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position)
    }

    internal class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.dtc_module_header_name)

        // A module reporting codes in amber with its count, a clean one muted - neither in the codes' red.
        fun bind(header: DtcListItem.ModuleHeader) {
            name.text = header.displayName()
            name.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (header.codeCount > 0) R.color.dtc_module_with_codes else R.color.dialog_text_secondary
                )
            )
        }
    }

    internal class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.dtc_message)

        // "No codes reported" - good news, in green.
        fun bind(message: DtcListItem.Message) {
            text.text = message.text
            text.setTextColor(ContextCompat.getColor(itemView.context, R.color.philippine_green))
        }
    }

    internal class DtcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var code: TextView = itemView.findViewById(R.id.dtc_value)
        var description: TextView = itemView.findViewById(R.id.dtc_description)

        var expandedContainer: LinearLayout =
            itemView.findViewById(R.id.dtc_expanded_details_container)
        var detailSystemInfo: TextView = itemView.findViewById(R.id.dtc_detail_system_info)
        var detailHexStatus: TextView = itemView.findViewById(R.id.dtc_detail_hex_status)

        var snapshotContainer: LinearLayout = itemView.findViewById(R.id.dtc_snapshot_container)
        var detailSnapshotInfo: TextView = itemView.findViewById(R.id.dtc_detail_snapshot_info)

        var extendedContainer: LinearLayout = itemView.findViewById(R.id.dtc_extended_container)
        var detailExtendedInfo: TextView = itemView.findViewById(R.id.dtc_detail_extended_info)

        var actionSearchWeb: Button = itemView.findViewById(R.id.dtc_action_search_web)
        var actionCopyCode: Button = itemView.findViewById(R.id.dtc_action_copy_code)
    }

    companion object {
        private val DiffCallback =
            object : DiffUtil.ItemCallback<DtcListItem>() {
                override fun areItemsTheSame(
                    oldItem: DtcListItem,
                    newItem: DtcListItem
                ): Boolean = oldItem.itemKey() == newItem.itemKey()

                override fun areContentsTheSame(
                    oldItem: DtcListItem,
                    newItem: DtcListItem
                ): Boolean = oldItem == newItem
            }
    }
}
