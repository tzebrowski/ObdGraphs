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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.datalogger.dtc.DtcListItem
import org.obd.graphs.bl.datalogger.dtc.DtcState
import org.obd.graphs.bl.datalogger.dtc.DtcStatusLabels
import org.obd.graphs.bl.datalogger.dtc.displayCode
import org.obd.graphs.bl.datalogger.dtc.displayDescription
import org.obd.graphs.bl.datalogger.dtc.displayName
import org.obd.graphs.bl.datalogger.dtc.dtcStatus
import org.obd.graphs.bl.datalogger.dtc.isDescriptionUnknown
import org.obd.graphs.bl.datalogger.dtc.itemKey
import org.obd.graphs.bl.datalogger.dtc.moduleNameOrNull
import org.obd.graphs.bl.datalogger.dtc.stateLabel
import org.obd.graphs.bl.datalogger.dtc.toDidLines
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.setText

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_DTC = 1
private const val VIEW_TYPE_MESSAGE = 2
private const val VIEW_TYPE_GONE_HEADER = 3
private const val GONE_ROW_ALPHA = 0.6f
private const val BADGE_FILL_ALPHA = 0x33

internal data class BadgeStyle(
    val color: Int,
    val icon: Int,
    val explanation: Int
)

internal val STATE_BADGES =
    mapOf(
        DtcState.ACTIVE to BadgeStyle(R.color.dtc_state_active, R.drawable.dtc_state_active, R.string.dtc_state_active_explanation),
        DtcState.STORED to BadgeStyle(R.color.dtc_state_stored, R.drawable.dtc_state_stored, R.string.dtc_state_stored_explanation),
        DtcState.PENDING to BadgeStyle(R.color.dtc_state_pending, R.drawable.dtc_state_pending, R.string.dtc_state_pending_explanation),
        DtcState.HISTORY to BadgeStyle(R.color.dtc_state_history, R.drawable.dtc_state_history, R.string.dtc_state_history_explanation)
    )

internal val MIL_BADGE = BadgeStyle(R.color.dtc_state_mil, R.drawable.dtc_state_mil, R.string.dtc_state_mil_explanation)

// The "no longer reported" section header's chip (check + count).
internal val GONE_BADGE = BadgeStyle(R.color.philippine_green, R.drawable.dtc_history_gone, R.string.dtc_legend_gone)

internal val NEW_BADGE = BadgeStyle(R.color.dtc_history_new, R.drawable.dtc_history_new, R.string.dtc_history_new_explanation)

// A compact chip (icon + short label) in a section header's summary line.
internal fun LinearLayout.addMiniBadge(
    label: String,
    style: BadgeStyle,
    description: String
) {
    addView(
        TextView(context, null, 0, R.style.DtcMiniBadge).apply {
            showBadge(label, style)
            contentDescription = description
        },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = context.resources.getDimensionPixelSize(R.dimen.dtc_mini_badge_gap)
        }
    )
}

// The chips the DTC legend explains, in the order a row shows them: label and style.
internal val DTC_LEGEND: List<Pair<Int, BadgeStyle>> =
    listOf(
        R.string.dtc_history_new to NEW_BADGE,
        R.string.dtc_state_active to STATE_BADGES.getValue(DtcState.ACTIVE),
        R.string.dtc_state_stored to STATE_BADGES.getValue(DtcState.STORED),
        R.string.dtc_state_pending to STATE_BADGES.getValue(DtcState.PENDING),
        R.string.dtc_state_history to STATE_BADGES.getValue(DtcState.HISTORY),
        R.string.dtc_state_mil to MIL_BADGE
    )

// The explanations start with their label ("Active: ..."), which the legend already shows as the chip.
internal fun legendExplanation(explanation: String): String = explanation.substringAfter(": ", explanation)

internal fun dtcStatusLabels(context: Context) =
    DtcStatusLabels(
        active = context.getString(R.string.dtc_state_active),
        stored = context.getString(R.string.dtc_state_stored),
        pending = context.getString(R.string.dtc_state_pending),
        history = context.getString(R.string.dtc_state_history),
        warningLamp = context.getString(R.string.dtc_state_warning_lamp)
    )

// Icon + label; solid fill with white text on light backgrounds, faint fill with coloured text at night
// (dtc_badge_solid). Gone without a label.
internal fun TextView.showBadge(
    label: String?,
    style: BadgeStyle?
) {
    visibility = if (label != null && style != null) View.VISIBLE else View.GONE
    if (label == null || style == null) return
    val color = ContextCompat.getColor(context, style.color)
    val solid = context.resources.getBoolean(R.bool.dtc_badge_solid)
    val foreground = if (solid) Color.WHITE else color
    text = label
    setTextColor(foreground)
    setCompoundDrawablesRelativeWithIntrinsicBounds(style.icon, 0, 0, 0)
    compoundDrawableTintList = ColorStateList.valueOf(foreground)
    backgroundTintList = ColorStateList.valueOf(if (solid) color else ColorUtils.setAlphaComponent(color, BADGE_FILL_ALPHA))
}

// onGoneHeaderClick / onModuleHeaderClick: a section header was tapped; the owner rebuilds the list expanded / collapsed.
internal class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?,
    private val onGoneHeaderClick: () -> Unit = {},
    private val onModuleHeaderClick: (DtcListItem.ModuleHeader) -> Unit = {}
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
            is DtcListItem.GoneHeader -> VIEW_TYPE_GONE_HEADER
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(mInflater.inflate(R.layout.item_dtc_module_header, parent, false))
            VIEW_TYPE_MESSAGE -> MessageViewHolder(mInflater.inflate(R.layout.item_dtc_message, parent, false))
            VIEW_TYPE_GONE_HEADER -> GoneHeaderViewHolder(mInflater.inflate(R.layout.item_dtc_module_header, parent, false))
            else -> DtcViewHolder(mInflater.inflate(R.layout.item_dtc, parent, false))
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {
            is DtcListItem.ModuleHeader -> (holder as HeaderViewHolder).bind(item, onModuleHeaderClick)
            is DtcListItem.DtcRow -> bindDtc(holder as DtcViewHolder, item)
            is DtcListItem.Message -> (holder as MessageViewHolder).bind(item)
            is DtcListItem.GoneHeader -> (holder as GoneHeaderViewHolder).bind(item, onGoneHeaderClick)
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

        // A gone row is no longer reported: greyed out, its module named (the section mixes modules).
        holder.itemView.alpha = if (item.isGone) GONE_ROW_ALPHA else 1f

        if (dtc.standardCode.isEmpty()) {
            holder.code.setText("", secondaryText, Typeface.NORMAL, 1f)
            holder.description.setText(finalDescription, secondaryText, Typeface.NORMAL, 1f)
            holder.badges.visibility = View.GONE
            holder.expandedContainer.visibility = View.GONE
            holder.expandIndicator.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            return
        } else {
            holder.code.setText(formattedCode, if (item.isGone) secondaryText else COLOR_CARDINAL, Typeface.BOLD, 1f)
            // Struck through: the code is no longer reported. Holders are reused, so the flag is set both ways.
            holder.code.paintFlags =
                if (item.isGone) {
                    holder.code.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    holder.code.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            val description =
                dtc.moduleNameOrNull()?.takeIf { item.isGone }?.let { "$it: $finalDescription" } ?: finalDescription
            if (isUnknown || item.isGone) {
                holder.description.setText(description, secondaryText, if (isUnknown) Typeface.ITALIC else Typeface.NORMAL, 1f)
            } else {
                holder.description.setText(description, primaryText, Typeface.NORMAL, 1f)
            }
        }

        // State and MIL as chips under the code: own colour and icon each (none reuses the codes' red),
        // spelled out in the expanded details.
        // A gone row has no current state: its chips would describe the last scan that reported it.
        val status = dtc.dtcStatus()
        val stateStyle = STATE_BADGES[status.state].takeUnless { item.isGone }
        val showMil = status.warningLamp && !item.isGone
        holder.newBadge.showBadge(context.getString(R.string.dtc_history_new).takeIf { item.isNew }, NEW_BADGE)
        holder.stateBadge.showBadge(status.stateLabel(dtcStatusLabels(context)), stateStyle)
        holder.milBadge.showBadge(context.getString(R.string.dtc_state_mil).takeIf { showMil }, MIL_BADGE)
        // A gone row says when its code was last reported where the chips would be.
        val lastReported = item.history?.lastSeen?.takeIf { item.isGone }
        holder.goneInfo.visibility = if (lastReported != null) View.VISIBLE else View.GONE
        lastReported?.let {
            holder.goneInfo.text =
                context.getString(
                    R.string.dtc_history_last_reported,
                    DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                )
        }
        // The row's chip line, padding included, only when it holds something.
        holder.badges.visibility =
            if (listOf(holder.newBadge, holder.stateBadge, holder.milBadge, holder.goneInfo).any { it.visibility == View.VISIBLE }) {
                View.VISIBLE
            } else {
                View.GONE
            }
        val explanations =
            listOfNotNull(NEW_BADGE.takeIf { item.isNew }, stateStyle, MIL_BADGE.takeIf { showMil })
                .map { context.getString(it.explanation) }
        holder.detailState.visibility = if (explanations.isNotEmpty()) View.VISIBLE else View.GONE
        holder.detailState.text = explanations.joinToString("\n")

        val history = item.history
        holder.historyContainer.visibility = if (history != null) View.VISIBLE else View.GONE
        if (history != null) {
            fun date(timestamp: Long) =
                DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
            holder.detailHistory.text =
                listOf(
                    context.getString(R.string.dtc_history_first_seen, date(history.firstSeen)),
                    context.getString(R.string.dtc_history_last_seen, date(history.lastSeen)),
                    context.getString(R.string.dtc_history_seen_in, history.seenIn, history.coveredIn)
                ).joinToString("\n")
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
        holder.expandIndicator.visibility = View.VISIBLE
        holder.expandIndicator.rotation = if (isExpanded) 180f else 0f
        holder.expandIndicator.contentDescription =
            context.getString(if (isExpanded) R.string.dtc_row_collapse else R.string.dtc_row_expand)

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
        private val summary: LinearLayout = itemView.findViewById(R.id.dtc_module_header_summary)
        private val arrow: ImageView = itemView.findViewById(R.id.dtc_module_header_arrow)

        // A module reporting codes in amber with its count, a clean one muted - neither in the codes' red.
        // The summary chips (state icon + count, MIL, new) stay in both states, so a collapsed module still
        // says what it holds; the legend explains the icons.
        fun bind(
            header: DtcListItem.ModuleHeader,
            onClick: (DtcListItem.ModuleHeader) -> Unit
        ) {
            val context = itemView.context
            name.text = header.displayName()
            name.setTextColor(
                ContextCompat.getColor(context, if (header.codeCount > 0) R.color.dtc_module_with_codes else R.color.dialog_text_secondary)
            )

            summary.removeAllViews()
            val labels = dtcStatusLabels(context)
            val chips =
                header.summary.byState.entries
                    .sortedBy { it.key }
                    .mapNotNull { (state, count) -> STATE_BADGES[state]?.let { Triple(count.toString(), it, "$count ${state.label(labels)}") } } +
                    listOfNotNull(
                        Triple(context.getString(R.string.dtc_state_mil), MIL_BADGE, context.getString(R.string.dtc_state_warning_lamp))
                            .takeIf { header.summary.milCount > 0 },
                        Triple(header.summary.newCount.toString(), NEW_BADGE, "${header.summary.newCount} ${context.getString(R.string.dtc_history_new)}")
                            .takeIf { header.summary.newCount > 0 }
                    )
            chips.forEach { (label, style, description) -> summary.addMiniBadge(label, style, description) }
            if (header.codeCount == 0) {
                summary.addView(
                    ImageView(context).apply {
                        setImageResource(R.drawable.dtc_history_gone)
                        imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.philippine_green))
                        contentDescription = context.getString(R.string.dtc_module_clean)
                    },
                    context.resources.getDimensionPixelSize(R.dimen.dtc_module_clean_icon).let { LinearLayout.LayoutParams(it, it) }
                )
            }

            arrow.rotation = if (header.expanded) 180f else 0f
            itemView.contentDescription =
                "${name.text}. ${context.getString(if (header.expanded) R.string.dtc_row_collapse else R.string.dtc_row_expand)}"
            itemView.setOnClickListener { onClick(header) }
        }

        private fun DtcState.label(labels: DtcStatusLabels) =
            when (this) {
                DtcState.ACTIVE -> labels.active
                DtcState.STORED -> labels.stored
                DtcState.PENDING -> labels.pending
                DtcState.HISTORY -> labels.history
                DtcState.UNKNOWN -> ""
            }
    }

    // Same layout as a module header, so every section looks alike: green name and a green check chip with the count.
    internal class GoneHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.dtc_module_header_name)
        private val summary: LinearLayout = itemView.findViewById(R.id.dtc_module_header_summary)
        private val arrow: ImageView = itemView.findViewById(R.id.dtc_module_header_arrow)

        fun bind(
            header: DtcListItem.GoneHeader,
            onClick: () -> Unit
        ) {
            val context = itemView.context
            name.text = context.getString(R.string.dtc_history_gone_title)
            name.setTextColor(ContextCompat.getColor(context, R.color.philippine_green))
            summary.removeAllViews()
            summary.addMiniBadge(header.codeCount.toString(), GONE_BADGE, name.text.toString())
            arrow.rotation = if (header.expanded) 180f else 0f
            itemView.contentDescription =
                "${context.getString(R.string.dtc_history_gone_header, header.codeCount)}. " +
                context.getString(if (header.expanded) R.string.dtc_row_collapse else R.string.dtc_row_expand)
            itemView.setOnClickListener { onClick() }
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
        var badges: LinearLayout = itemView.findViewById(R.id.dtc_badges)
        var newBadge: TextView = itemView.findViewById(R.id.dtc_new_badge)
        var stateBadge: TextView = itemView.findViewById(R.id.dtc_state_badge)
        var milBadge: TextView = itemView.findViewById(R.id.dtc_mil_badge)
        var goneInfo: TextView = itemView.findViewById(R.id.dtc_gone_info)

        var expandedContainer: LinearLayout =
            itemView.findViewById(R.id.dtc_expanded_details_container)
        var detailState: TextView = itemView.findViewById(R.id.dtc_detail_state)
        var historyContainer: LinearLayout = itemView.findViewById(R.id.dtc_history_container)
        var detailHistory: TextView = itemView.findViewById(R.id.dtc_detail_history)
        var expandIndicator: ImageView = itemView.findViewById(R.id.dtc_expand_indicator)
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
