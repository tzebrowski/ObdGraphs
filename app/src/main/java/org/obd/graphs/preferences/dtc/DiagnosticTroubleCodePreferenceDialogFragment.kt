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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.dri.DiagnosticRequestIdFragment
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet
import org.obd.graphs.registerReceiver
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.toast
import org.obd.graphs.ui.withDataLogger
import org.obd.metrics.api.model.DiagnosticTroubleCode

private const val PREF_DTC_DESELECTED_MODULES = "pref.dtc.module_picker.deselected"

internal class DiagnosticTroubleCodePreferenceDialogFragment : CoreDialogFragment() {
    private lateinit var adapter: DiagnosticTroubleCodeViewAdapter
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button
    private lateinit var shareButton: Button
    private lateinit var recyclerView: RecyclerView

    private val dtcNotificationsReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    DATA_LOGGER_DTC_ACTION_COMPLETED -> handleDTCChangedNotification()
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

        adapter = DiagnosticTroubleCodeViewAdapter(context)
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
                    setLoadingState(true)
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
                pickDtcModules(
                    titleRes = R.string.pref_dtc_clean_dialog_title,
                    confirmRes = R.string.pref_dtc_select_modules_confirm_clear,
                    messageRes = R.string.pref_dtc_clean_dialog_confirm_message,
                    confirmWhenEmpty = true
                ) { selectedModules ->
                    setLoadingState(true)
                    withDataLogger {
                        scheduleDTCCleanup(selectedModules)
                    }

                    toast(R.string.pref_dtc_clean_dialog_send_message)
                    clearButton.setText(R.string.dtc_action_clearing)
                }
            } else {
                toast(R.string.pref_dtc_no_connection_established)
            }
        }
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

        // Stored as the *deselected* keys so a module added later starts out checked.
        val deselected = Prefs.getStringSet(PREF_DTC_DESELECTED_MODULES)
        val listView =
            content.findViewById<MaxHeightListView>(R.id.dtc_picker_modules).apply {
                adapter =
                    android.widget.ArrayAdapter(
                        context,
                        android.R.layout.simple_list_item_multiple_choice,
                        labels
                    )
                requestKeys.forEachIndexed { index, key ->
                    setItemChecked(index, key !in deselected)
                }
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

        fun updateButtons() {
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

    private fun setLoadingState(isLoading: Boolean) =
        if (isLoading) {
            sendBroadcastEvent(
                SCREEN_LOCK_PROGRESS_EVENT,
                ScreenLock(
                    message = R.string.pref_dtc_screen_lock,
                    showCancel = true
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

        for (code in dtcList) {
            if (code.standardCode.isEmpty()) continue

            val formattedCode =
                if (!code.failureType?.code.isNullOrEmpty()) {
                    "${code.standardCode}-${code.failureType.code}"
                } else {
                    code.standardCode
                }

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

            val hex = code.rawHex ?: notAvailable
            val activeStatuses = code.activeStatuses?.joinToString(", ") ?: getString(R.string.dtc_share_none)
            reportBuilder.append(getString(R.string.dtc_share_status, activeStatuses, hex)).append("\n")

            val snapshot = code.snapshot
            if (snapshot != null && dataLoggerSettings.instance().adapter.dtcReadSnapshots) {
                reportBuilder.append(getString(R.string.dtc_share_snapshot, snapshot.size)).append("\n")
                snapshot.forEach { did ->
                    val value = did.decodedValue ?: notAvailable
                    val unit = did.definition?.units ?: ""
                    val desc = did.definition?.description ?: getString(R.string.dtc_share_unknown_did)
                    reportBuilder.append("  - $desc: $value $unit\n")
                }
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
        VehicleCapabilitiesManager
            .getDiagnosticTroubleCodes()
            .sortedWith(
                compareBy<DiagnosticTroubleCode> { code ->
                    code.module ?: ""
                }.thenBy { code ->
                    val desc = code.description
                    val isUnknown =
                        desc.isNullOrBlank() ||
                            desc.contains(
                                "Unknown DTC Description",
                                ignoreCase = true
                            )
                    if (isUnknown) 1 else 0
                }.thenBy { code ->
                    code.standardCode
                }
            ).toMutableList()

    override fun onDestroyView() {
        requireContext().unregisterReceiver(dtcNotificationsReceiver)
        setLoadingState(false)
        super.onDestroyView()
    }

    private fun handleDTCChangedNotification() {
        refreshList()
        resetActionState()
    }

    private fun refreshList() {
        val codes = diagnosticTroubleCodes()
        adapter.submitList(
            codes.toDtcListItems(
                scannedModules = VehicleCapabilitiesManager.getDtcScannedModules(),
                noCodesMessage = getString(R.string.pref_dtc_no_dtc_found),
                noCodesForModuleMessage = getString(R.string.pref_dtc_no_dtc_found_for_module)
            )
        )

        // Nothing to share or clear without codes.
        shareButton.isEnabled = codes.isNotEmpty()
        clearButton.isEnabled = codes.isNotEmpty()
    }

    private fun resetActionState() {
        setLoadingState(false)
        clearButton.setText(R.string.dtc_action_clear_codes)
    }
}
