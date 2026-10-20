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

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_DIALOG_CANCELLED_EVENT
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.ScreenLock
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_ACTION_COMPLETED
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.datalogger.dtc.DTC_SCREEN_LOCK_CONTEXT
import org.obd.graphs.bl.datalogger.dtc.DtcClearOutcome
import org.obd.graphs.bl.datalogger.dtc.DtcFilter
import org.obd.graphs.bl.datalogger.dtc.DtcHistoryStore
import org.obd.graphs.bl.datalogger.dtc.EngineStateTracker
import org.obd.graphs.bl.datalogger.dtc.PREF_DTC_DESELECTED_MODULES
import org.obd.graphs.bl.datalogger.dtc.PREF_DTC_OBD_MODULES
import org.obd.graphs.bl.datalogger.dtc.collapsedBy
import org.obd.graphs.bl.datalogger.dtc.displayCode
import org.obd.graphs.bl.datalogger.dtc.dtcActionTimeoutMs
import org.obd.graphs.bl.datalogger.dtc.dtcFilterCounts
import org.obd.graphs.bl.datalogger.dtc.dtcModuleRequestKey
import org.obd.graphs.bl.datalogger.dtc.dtcObdModulesAfterPick
import org.obd.graphs.bl.datalogger.dtc.dtcStatus
import org.obd.graphs.bl.datalogger.dtc.failureTypeLine
import org.obd.graphs.bl.datalogger.dtc.filteredBy
import org.obd.graphs.bl.datalogger.dtc.markedWith
import org.obd.graphs.bl.datalogger.dtc.remainingAfterClear
import org.obd.graphs.bl.datalogger.dtc.sortedForDisplay
import org.obd.graphs.bl.datalogger.dtc.statusLine
import org.obd.graphs.bl.datalogger.dtc.toDidLines
import org.obd.graphs.bl.datalogger.dtc.toDtcListItems
import org.obd.graphs.bl.datalogger.dtc.unansweredModules
import org.obd.graphs.bl.datalogger.dtc.withGoneSection
import org.obd.graphs.bl.datalogger.dtc.withModuleClearActions
import org.obd.graphs.bl.datalogger.dtc.withModuleSummaries
import org.obd.graphs.bl.readiness.PREF_READINESS_LAST
import org.obd.graphs.bl.readiness.ReadinessSnapshot
import org.obd.graphs.getSerializableCompat
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.dri.DiagnosticRequestIdFragment
import org.obd.graphs.preferences.getString
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.readiness.ReadinessDialogFragment
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.preferences.updateStringSet
import org.obd.graphs.registerReceiver
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.toast
import org.obd.graphs.ui.withDataLogger
import org.obd.metrics.api.model.DiagnosticTroubleCode

private const val PREF_DTC_LEGEND_EXPANDED = "pref.dtc.legend.expanded"

// Material's disabled emphasis.
private const val DISABLED_BUTTON_ALPHA = 0.38f

internal class DiagnosticTroubleCodePreferenceDialogFragment : CoreDialogFragment() {
    private lateinit var adapter: DiagnosticTroubleCodeViewAdapter
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button
    private lateinit var shareButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var readInfo: TextView
    private lateinit var readinessLink: TextView
    private lateinit var filterGroup: ChipGroup
    private val filterChips = mutableMapOf<DtcFilter, Chip>()

    // The filter chip picked; back to All each time the dialog opens.
    private var filter = DtcFilter.ALL

    // The "no longer reported" section starts collapsed each time the dialog opens.
    private var goneExpanded = false

    // Modules the user expanded / collapsed while the dialog is open; the others follow isExpandedByDefault().
    private val moduleExpanded = mutableMapOf<String, Boolean>()

    private val dtcNotificationsReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    DATA_LOGGER_DTC_ACTION_COMPLETED -> handleDTCChangedNotification(intent.getSerializableCompat())
                    // The read/clear will never report back after the user cancels the progress
                    // overlay or the connection drops, so restore the idle state here instead of
                    // leaving the Clear button stuck on "Clearing...".
                    SCREEN_LOCK_DIALOG_CANCELLED_EVENT,
                    DATA_LOGGER_ERROR_EVENT,
                    DATA_LOGGER_STOPPED_EVENT -> resetActionState()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requestWindowFeatures()

        val root = inflater.inflate(R.layout.dialog_dtc, container, false)

        recyclerView = root.findViewById(R.id.recycler_view)
        readInfo = root.findViewById(R.id.dtc_read_info)
        readinessLink = root.findViewById(R.id.dtc_readiness_link)
        attachReadinessLink()
        attachLegend(root)
        attachFilter(root)

        adapter =
            DiagnosticTroubleCodeViewAdapter(
                context,
                onGoneHeaderClick = {
                    goneExpanded = !goneExpanded
                    refreshList()
                },
                onModuleHeaderClick = { header ->
                    moduleExpanded[header.module] = !header.expanded
                    refreshList()
                },
                onModuleClearClick = { module -> clearModule(module) }
            )
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachButtons(root)
        attachCloseButton(root)
        refreshList()

        // Registered for the whole view lifetime (not just while resumed) so a result arriving
        // while the dialog is paused - e.g. behind the share chooser - isn't dropped, leaving a
        // stale list and a stuck progress overlay.
        registerReceiver(requireContext(), dtcNotificationsReceiver) {
            it.addAction(DATA_LOGGER_DTC_ACTION_COMPLETED)
            it.addAction(SCREEN_LOCK_DIALOG_CANCELLED_EVENT)
            it.addAction(DATA_LOGGER_ERROR_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
        }

        return root
    }

    private fun attachButtons(root: View) {
        refreshButton = root.findViewById(R.id.action_refresh_dtc)
        shareButton = root.findViewById(R.id.action_share)
        clearButton = root.findViewById(R.id.action_clear_dtc)

        shareButton.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE

        shareButton.setOnClickListener {
            shareDtcReport(diagnosticTroubleCodes())
        }

        refreshButton.setOnClickListener {
            if (DataLoggerRepository.isRunning()) {
                pickDtcModules(
                    titleRes = R.string.pref_dtc_select_modules_title,
                    confirmRes = R.string.pref_dtc_select_modules_confirm,
                    messageRes = R.string.pref_dtc_select_modules_description,
                    confirmWhenEmpty = false
                ) { selectedModules ->
                    setLoadingState(true, selectedModules.size)
                    withDataLogger {
                        scheduleDTCRead(selectedModules)
                    }
                }
            } else {
                toast(R.string.pref_dtc_no_connection_established)
            }
        }

        clearButton.setOnClickListener {
            if (DataLoggerRepository.isRunning()) {
                confirmEngineOff {
                    pickDtcModules(
                        titleRes = R.string.pref_dtc_clean_dialog_title,
                        confirmRes = R.string.pref_dtc_select_modules_confirm_clear,
                        messageRes = R.string.pref_dtc_clean_dialog_confirm_message,
                        confirmWhenEmpty = true
                    ) { selectedModules -> startClear(selectedModules) }
                }
            } else {
                toast(R.string.pref_dtc_no_connection_established)
            }
        }
    }

    private fun startClear(modules: Set<String>) {
        setLoadingState(true, modules.size)
        withDataLogger {
            scheduleDTCCleanup(modules)
        }

        toast(R.string.pref_dtc_clean_dialog_send_message)
        clearButton.setText(R.string.dtc_action_clearing)
    }

    // A recent reading says the engine runs: most modules would refuse the clear (7F 14 22), so say so first and let
    // the user go on anyway. Unknown (no engine PID in the running query) goes straight on.
    private fun confirmEngineOff(onConfirmed: () -> Unit) {
        if (EngineStateTracker.engineRunning() != true) {
            onConfirmed()
            return
        }
        AlertDialog
            .Builder(requireContext())
            .setTitle(R.string.dtc_engine_running_title)
            .setMessage(R.string.dtc_engine_running_message)
            .setPositiveButton(R.string.dtc_engine_running_clear_anyway) { _, _ -> onConfirmed() }
            .setNegativeButton(R.string.pref_dtc_select_modules_cancel, null)
            .show()
    }

    // "Clear codes in <module>": one module, one confirmation (with the engine warning folded in), no picker.
    private fun clearModule(module: String) {
        if (!DataLoggerRepository.isRunning()) {
            toast(R.string.pref_dtc_no_connection_established)
            return
        }
        val requestKey = dtcModuleRequestKey(DiagnosticRequestIDManager.getMappings(), module) ?: return
        val message =
            listOfNotNull(
                getString(R.string.dtc_engine_running_message).takeIf { EngineStateTracker.engineRunning() == true },
                getString(R.string.dtc_module_clear_message)
            ).joinToString("\n\n")
        AlertDialog
            .Builder(requireContext())
            .setTitle(getString(R.string.dtc_module_clear_title, module))
            .setMessage(message)
            .setPositiveButton(R.string.dtc_module_clear_confirm) { _, _ -> startClear(setOf(requestKey)) }
            .setNegativeButton(R.string.pref_dtc_select_modules_cancel, null)
            .show()
    }

    // Lets the user restrict a DTC read/clear to a subset of the configured Diagnostic Request ID
    // modules for this action only (not persisted), folding in an optional message (a plain
    // description for the Scan flow, a destructive-action warning for the Clear flow) so a single
    // dialog covers both module selection and any needed confirmation instead of two dialogs in a
    // row. Skips straight to onPicked when there's nothing configured, matching the plain default
    // single-ECU behavior from before this feature - unless confirmWhenEmpty is set, in which case
    // the message is still shown on its own (used by Clear, since that warning must not be skipped
    // just because there's no module list to attach it to).
    private fun pickDtcModules(
        titleRes: Int,
        confirmRes: Int,
        messageRes: Int? = null,
        confirmWhenEmpty: Boolean = false,
        onPicked: (Set<String>) -> Unit
    ) {
        val mappings = DiagnosticRequestIDManager.getMappings().filter { it.headerValue.isNotEmpty() }

        if (mappings.isEmpty()) {
            if (confirmWhenEmpty && messageRes != null) {
                android.app.AlertDialog
                    .Builder(requireContext())
                    .setTitle(titleRes)
                    .setMessage(
                        "${resources.getString(messageRes)}\n\n" +
                            resources.getString(R.string.pref_dtc_select_modules_can_headers_hint)
                    )
                    .setPositiveButton(confirmRes) { d, _ ->
                        d.dismiss()
                        onPicked(emptySet())
                    }
                    .setNegativeButton(R.string.pref_dtc_select_modules_cancel, null)
                    .setNeutralButton(R.string.pref_dtc_select_modules_can_headers) { d, _ ->
                        d.dismiss()
                        jumpToCanHeaders()
                    }
                    .show()
            } else {
                onPicked(emptySet())
            }
            return
        }

        val labels = mappings.map { it.displayName }.toTypedArray()
        val requestKeys = mappings.map { it.requestKey }

        // A plain AlertDialog can show a message OR a multi-choice list, never both - the
        // platform only wires the list into the layout when no message is set. So the Clear
        // flow's warning is folded into a custom view (message + a real ListView) instead of
        // setMessage()/setMultiChoiceItems(), letting one dialog cover both module selection
        // and the destructive-action confirmation.
        val context = requireContext()
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_dtc_module_picker, null)

        content.findViewById<android.widget.TextView>(R.id.dtc_picker_message).apply {
            if (messageRes != null) {
                setText(messageRes)
                visibility = View.VISIBLE
            }
        }

        val deselected = Prefs.getStringSet(PREF_DTC_DESELECTED_MODULES)
        val storedObdModules = Prefs.getStringSet(PREF_DTC_OBD_MODULES).orEmpty()
        val listView = content.findViewById<MaxHeightListView>(R.id.dtc_picker_modules)
        // Set below, once the buttons it updates exist; the adapter's callback only runs on a user tap.
        lateinit var updateButtons: () -> Unit
        val pickerAdapter =
            DtcModulePickerAdapter(
                context,
                labels.toList(),
                requestKeys,
                storedObdModules.intersect(requestKeys.toSet()).toMutableSet()
            ) { position ->
                listView.setItemChecked(position, false)
                updateButtons()
            }
        listView.adapter = pickerAdapter
        requestKeys.forEachIndexed { index, key ->
            listView.setItemChecked(index, key !in deselected)
        }

        val headersButton = content.findViewById<Button>(R.id.dtc_picker_can_headers)
        val cancelButton = content.findViewById<Button>(R.id.dtc_picker_cancel)
        val toggleButton = content.findViewById<Button>(R.id.dtc_picker_toggle_all)
        val confirmButton = content.findViewById<Button>(R.id.dtc_picker_confirm).apply { setText(confirmRes) }

        val dialog =
            android.app.AlertDialog
                .Builder(context)
                .setTitle(titleRes)
                .setView(content)
                .create()

        fun allChecked() = labels.indices.all { listView.isItemChecked(it) }

        fun noneChecked() = labels.indices.none { listView.isItemChecked(it) }

        updateButtons = {
            toggleButton.setText(
                if (allChecked()) {
                    R.string.pref_dtc_select_modules_deselect_all
                } else {
                    R.string.pref_dtc_select_modules_select_all
                }
            )
            confirmButton.isEnabled = !noneChecked()
        }

        updateButtons()

        listView.setOnItemClickListener { _, _, _, _ -> updateButtons() }

        toggleButton.setOnClickListener {
            val selectAll = !allChecked()
            for (index in labels.indices) {
                listView.setItemChecked(index, selectAll)
            }
            updateButtons()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        headersButton.setOnClickListener {
            dialog.dismiss()
            jumpToCanHeaders()
        }

        confirmButton.setOnClickListener {
            dialog.dismiss()
            val selected = requestKeys.filterIndexed { index, _ -> listView.isItemChecked(index) }.toSet()
            Prefs.updateStringSet(PREF_DTC_DESELECTED_MODULES, requestKeys.filter { it !in selected })
            Prefs.updateStringSet(PREF_DTC_OBD_MODULES, dtcObdModulesAfterPick(storedObdModules, requestKeys, pickerAdapter.obdMarked).toList())
            onPicked(selected)
        }

        dialog.show()
    }

    // Opens the DRI (CAN header) manager fragment directly rather than routing through the
    // "pref.init" preference screen, which would just land the user on a list they'd still
    // have to tap "Manage" from. This mirrors the FragmentTransaction androidx's Preference
    // library performs for that "Manage" row internally (replace into the NavHostFragment's
    // own container, on its child FragmentManager - it hosts nav_preferences/nav_graph/etc.),
    // since there's no dedicated NavGraph destination for the DRI manager to navigate to.
    // Also closes this DTC dialog itself (not just the module-picker on top of it) - leaving
    // it open behind the DRI manager would be confusing to come back to.
    private fun jumpToCanHeaders() {
        dialog?.dismiss()
        requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, DiagnosticRequestIdFragment())
            ?.addToBackStack("dri_manager")
            ?.commit()
    }

    private fun setLoadingState(
        isLoading: Boolean,
        moduleCount: Int = 0
    ) = if (isLoading) {
        sendBroadcastEvent(
            SCREEN_LOCK_PROGRESS_EVENT,
            ScreenLock(
                message = R.string.pref_dtc_screen_lock,
                showCancel = true,
                context = DTC_SCREEN_LOCK_CONTEXT,
                timeoutMs = dtcActionTimeoutMs(moduleCount)
            )
        )
    } else {
        sendBroadcastEvent(
            SCREEN_UNLOCK_PROGRESS_EVENT
        )
    }

    private fun shareDtcReport(dtcList: List<DiagnosticTroubleCode>) {
        val notAvailable = getString(R.string.dtc_share_not_available)
        val reportTitle = getString(R.string.dtc_share_report_title)
        val reportBuilder = StringBuilder()
        reportBuilder.append("$reportTitle\n")
        reportBuilder.append("-".repeat(reportTitle.length)).append("\n\n")

        val readInfo = VehicleCapabilitiesManager.getDtcReadInfo()
        dtcReportHeader(
            readInfo = readInfo?.let { readInfoSummary(requireContext(), it) },
            durationMs = readInfo?.durationMs,
            unansweredModules = VehicleCapabilitiesManager.getDtcModuleStatus().unansweredModules(),
            labels =
            DtcReportHeaderLabels(
                duration = getString(R.string.dtc_share_duration),
                noResponse = getString(R.string.dtc_share_modules_no_response)
            )
        ).takeIf { it.isNotEmpty() }?.let { reportBuilder.append(it.joinToString("\n")).append("\n\n") }

        val statusLabels = dtcStatusLabels(requireContext())
        for (code in dtcList) {
            if (code.standardCode.isEmpty()) continue

            val formattedCode = code.displayCode()

            reportBuilder.append(getString(R.string.dtc_share_dtc, formattedCode)).append("\n")
            reportBuilder
                .append(getString(R.string.dtc_share_description, code.description ?: getString(R.string.dtc_share_unknown)))
                .append("\n")

            val systemTxt = code.system?.description
            val categoryTxt = code.category?.description
            if (!systemTxt.isNullOrBlank() || !categoryTxt.isNullOrBlank()) {
                reportBuilder
                    .append(getString(R.string.dtc_share_system_category, systemTxt ?: notAvailable, categoryTxt ?: notAvailable))
                    .append("\n")
            }
            code.failureTypeLine(::getString)?.let { reportBuilder.append(it).append("\n") }

            code.dtcStatus().statusLine(statusLabels)?.let { reportBuilder.append(getString(R.string.dtc_share_state, it)).append("\n") }

            val hex = code.rawHex ?: notAvailable
            val activeStatuses = code.activeStatuses?.joinToString(", ") ?: getString(R.string.dtc_share_none)
            reportBuilder.append(getString(R.string.dtc_share_status, activeStatuses, hex)).append("\n")

            val unknownDid = getString(R.string.dtc_share_unknown_did)
            val snapshotLines = code.snapshot.toDidLines(unknownDid, notAvailable)
            if (snapshotLines.isNotEmpty() && dataLoggerSettings.instance().adapter.dtcReadSnapshots) {
                reportBuilder.append(getString(R.string.dtc_share_snapshot, snapshotLines.size)).append("\n")
                snapshotLines.forEach { reportBuilder.append("  - $it\n") }
            }

            val extendedLines = code.details.toDidLines(unknownDid, notAvailable)
            if (extendedLines.isNotEmpty()) {
                reportBuilder.append(getString(R.string.dtc_extended_data)).append(":\n")
                extendedLines.forEach { reportBuilder.append("  - $it\n") }
            }

            reportBuilder.append("\n")
        }

        val sendIntent: Intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
                type = "text/plain"
            }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.dtc_share_chooser_title))
        startActivity(shareIntent)
    }

    private fun diagnosticTroubleCodes(): List<DiagnosticTroubleCode> =
        VehicleCapabilitiesManager.getDiagnosticTroubleCodes().sortedForDisplay()

    override fun onDestroyView() {
        requireContext().unregisterReceiver(dtcNotificationsReceiver)
        setLoadingState(false)
        super.onDestroyView()
    }

    // clearOutcome is set only when the completed action was a clear.
    private fun handleDTCChangedNotification(clearOutcome: DtcClearOutcome?) {
        refreshList()
        resetActionState()

        when (clearOutcome) {
            DtcClearOutcome.CLEARED -> toast(R.string.pref_dtc_clear_outcome_cleared)
            DtcClearOutcome.REFUSED -> toast(R.string.pref_dtc_clear_outcome_refused)
            DtcClearOutcome.PERMANENT_REMAIN ->
                toast(R.string.pref_dtc_clear_outcome_permanent_remain, diagnosticTroubleCodes().remainingAfterClear(permanent = true).toString())
            DtcClearOutcome.CODES_REMAIN ->
                toast(R.string.pref_dtc_clear_outcome_codes_remain, diagnosticTroubleCodes().remainingAfterClear(permanent = false).toString())
            null -> Unit
        }
    }

    private fun refreshList() {
        val codes = diagnosticTroubleCodes()
        val mappings = DiagnosticRequestIDManager.getMappings()
        // New / gone markers from the vehicle's DTC history, for the list stored with this read info.
        val history = DtcHistoryStore.diff(VehicleCapabilitiesManager.getDtcReadInfo())
        val items =
            codes
                .toDtcListItems(
                    scannedModules = VehicleCapabilitiesManager.getDtcScannedModules(),
                    noCodesMessage = getString(R.string.pref_dtc_no_dtc_found),
                    noCodesForModuleMessage = getString(R.string.pref_dtc_no_dtc_found_for_module),
                    unknownModuleLabel = getString(R.string.pref_dtc_unknown_module),
                    moduleStatus = VehicleCapabilitiesManager.getDtcModuleStatus(),
                    noResponseMessage = getString(R.string.pref_dtc_module_no_response),
                    refusedMessage = getString(R.string.pref_dtc_module_refused)
                ).markedWith(history)
        refreshFilterChips(items.dtcFilterCounts())
        adapter.submitList(
            items
                .filteredBy(filter, getString(R.string.dtc_filter_empty))
                .withModuleSummaries()
                .collapsedBy { moduleExpanded[it] }
                .withModuleClearActions { dtcModuleRequestKey(mappings, it) != null }
                // Gone codes have no current state: the section belongs to the unfiltered list only.
                .let { if (filter == DtcFilter.ALL) it.withGoneSection(history, goneExpanded) else it }
        )

        refreshReadInfo()

        // Nothing to share or clear without codes.
        shareButton.setEnabledDimmed(codes.isNotEmpty())
        clearButton.setEnabledDimmed(codes.isNotEmpty())
    }

    // Share's icon and Clear's text have fixed colours, which the disabled state does not grey out.
    private fun Button.setEnabledDimmed(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else DISABLED_BUTTON_ALPHA
    }

    private fun refreshReadInfo() {
        val info = VehicleCapabilitiesManager.getDtcReadInfo()
        if (info == null) {
            readInfo.visibility = View.GONE
            return
        }
        readInfo.text = readInfoSummary(requireContext(), info)
        readInfo.visibility = View.VISIBLE
    }

    // All / Active / Stored / Pending, each with its state chip's icon and colour. Filters what is on screen only; the
    // shared report and Clear Codes still cover every code.
    private fun attachFilter(root: View) {
        filterGroup = root.findViewById(R.id.dtc_filter_group)
        val inflater = LayoutInflater.from(requireContext())
        DtcFilter.entries.forEach { entry ->
            val chip = inflater.inflate(R.layout.item_dtc_filter_chip, filterGroup, false) as Chip
            chip.id = View.generateViewId()
            STATE_BADGES[entry.state]?.let { style ->
                chip.setChipIconResource(style.icon)
                chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), style.color))
                chip.isChipIconVisible = true
            }
            chip.isChecked = entry == filter
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked && filter != entry) {
                    filter = entry
                    refreshList()
                }
            }
            filterGroup.addView(chip)
            filterChips[entry] = chip
        }
    }

    // Chip labels carry the counts; a state with no code is greyed unless picked. Hidden without codes, and the filter
    // falls back to All, so a refresh that empties the list does not leave an invisible filter applied.
    private fun refreshFilterChips(counts: Map<DtcFilter, Int>) {
        val total = counts[DtcFilter.ALL] ?: 0
        if (total == 0) filter = DtcFilter.ALL
        filterGroup.visibility = if (total == 0) View.GONE else View.VISIBLE
        filterChips.forEach { (entry, chip) ->
            val count = counts[entry] ?: 0
            chip.text = getString(R.string.dtc_filter_chip, getString(entry.labelRes()), count)
            chip.isChecked = entry == filter
            chip.isEnabled = count > 0 || entry == filter
        }
    }

    // What the row chips mean, from the explanations the expanded rows use. Collapsed by default; the choice is kept.
    private fun attachLegend(root: View) {
        val toggle = root.findViewById<TextView>(R.id.dtc_legend_toggle)
        val legend = root.findViewById<LinearLayout>(R.id.dtc_legend)
        val inflater = LayoutInflater.from(requireContext())

        val table = root.findViewById<TableLayout>(R.id.dtc_legend_table)
        DTC_LEGEND.forEach { (label, style) ->
            val row = inflater.inflate(R.layout.item_dtc_legend_row, table, false)
            row.findViewById<TextView>(R.id.dtc_legend_badge).showBadge(getString(label), style)
            row.findViewById<TextView>(R.id.dtc_legend_text).text = legendExplanation(getString(style.explanation))
            table.addView(row)
        }
        root.findViewById<TextView>(R.id.dtc_legend_help).setOnClickListener { showDtcHistoryHelp(requireContext()) }

        fun show(expanded: Boolean) {
            legend.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (expanded) R.drawable.dtc_expand_less else R.drawable.dtc_expand_more, 0)
            toggle.compoundDrawableTintList = toggle.textColors
            toggle.contentDescription = getString(if (expanded) R.string.dtc_row_collapse else R.string.dtc_row_expand)
        }

        show(Prefs.getBoolean(PREF_DTC_LEGEND_EXPANDED, false))
        toggle.setOnClickListener {
            val expanded = legend.visibility != View.VISIBLE
            Prefs.updateBoolean(PREF_DTC_LEGEND_EXPANDED, expanded)
            show(expanded)
        }
    }

    // Monitors and codes come from the same self-tests (a failed monitor stores a code; a clear resets both), so the
    // list links to the readiness dialog. Shown from the child manager so the row refreshes when that dialog closes.
    private fun attachReadinessLink() {
        childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewDestroyed(
                    fm: FragmentManager,
                    f: Fragment
                ) {
                    if (f is ReadinessDialogFragment) refreshReadinessLink()
                }
            },
            false
        )
        readinessLink.setOnClickListener { ReadinessDialogFragment().show(childFragmentManager, null) }
        refreshReadinessLink()
    }

    private fun refreshReadinessLink() {
        val snapshot = ReadinessSnapshot.decode(Prefs.getString(PREF_READINESS_LAST))
        readinessLink.text =
            if (snapshot == null) {
                getString(R.string.dtc_readiness_link_unknown)
            } else {
                getString(
                    R.string.dtc_readiness_link,
                    getString(if (snapshot.milOn) R.string.readiness_mil_on else R.string.readiness_mil_off),
                    snapshot.complete,
                    snapshot.supported,
                    DateUtils.formatDateTime(
                        requireContext(),
                        snapshot.timestamp,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                    )
                )
            }
    }

    private fun resetActionState() {
        setLoadingState(false)
        clearButton.setText(R.string.dtc_action_clear_codes)
    }
}
