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
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.ScreenLock
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_ACTION_COMPLETED
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.registerReceiver
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.toast
import org.obd.graphs.ui.withDataLogger
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.command.dtc.DtcComponent

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
                if (intent?.action == DATA_LOGGER_DTC_ACTION_COMPLETED) {
                    handleDTCChangedNotification()
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
        val sortedDtcList = diagnosticTroubleCodes()

        recyclerView = root.findViewById(R.id.recycler_view)

        adapter = DiagnosticTroubleCodeViewAdapter(context)
        adapter.submitList(sortedDtcList.toDtcListItems())
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachButtons(root, sortedDtcList)
        attachCloseButton(root)

        return root
    }

    private fun attachButtons(
        root: View,
        sortedDtcList: List<DiagnosticTroubleCode>
    ) {
        refreshButton = root.findViewById(R.id.action_refresh_dtc)
        shareButton = root.findViewById(R.id.action_share)
        clearButton = root.findViewById(R.id.action_clear_dtc)

        shareButton.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE

        if (isDtcAvailable(sortedDtcList)) {
            shareButton.isEnabled = false
            clearButton.isEnabled = false
        }

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
                    clearButton.text = "Clearing..."
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
                    .setMessage(messageRes)
                    .setPositiveButton(confirmRes) { d, _ ->
                        d.dismiss()
                        onPicked(emptySet())
                    }
                    .setNegativeButton(R.string.pref_dtc_select_modules_cancel, null)
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
        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()

        val container =
            android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(padding, padding, padding, 0)
            }

        if (messageRes != null) {
            container.addView(
                android.widget.TextView(context).apply {
                    text = resources.getString(messageRes)
                    setPadding(0, 0, 0, padding)
                }
            )
        }

        val listView =
            android.widget.ListView(context).apply {
                choiceMode = android.widget.ListView.CHOICE_MODE_MULTIPLE
                adapter =
                    android.widget.ArrayAdapter(
                        context,
                        android.R.layout.simple_list_item_multiple_choice,
                        labels
                    )
                for (index in labels.indices) {
                    setItemChecked(index, true)
                }
            }
        container.addView(
            listView,
            android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // The platform's own button bar auto-stacks into a vertical column once it estimates
        // the combined button text won't fit on one line - which is exactly what happened here
        // with three buttons (Cancel / Select-Deselect All / Clear Codes). Building the row
        // ourselves with equal-weight buttons keeps it horizontal unconditionally.
        fun barButton(textRes: Int): android.widget.Button =
            android.widget.Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
                setText(textRes)
                layoutParams =
                    android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

        val cancelButton = barButton(R.string.pref_dtc_select_modules_cancel)
        val toggleButton = barButton(R.string.pref_dtc_select_modules_deselect_all)
        val confirmButton =
            barButton(confirmRes).apply {
                // A plain solid color would stay just as bright when the button is disabled
                // (no modules selected), losing that visual cue - a ColorStateList keeps the
                // brand color for the enabled state while still dimming on disable.
                val enabledColor = androidx.core.content.ContextCompat.getColor(context, R.color.rainbow_indigo)
                val disabledColor = androidx.core.content.ContextCompat.getColor(context, android.R.color.darker_gray)
                setTextColor(
                    android.content.res.ColorStateList(
                        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                        intArrayOf(disabledColor, enabledColor)
                    )
                )
            }

        container.addView(
            android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, padding, 0, 0)
                addView(cancelButton)
                addView(toggleButton)
                addView(confirmButton)
            }
        )

        val dialog =
            android.app.AlertDialog
                .Builder(context)
                .setTitle(titleRes)
                .setView(container)
                .create()

        fun allChecked() = (0 until labels.size).all { listView.isItemChecked(it) }

        fun noneChecked() = (0 until labels.size).none { listView.isItemChecked(it) }

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

        confirmButton.setOnClickListener {
            dialog.dismiss()
            val selected = requestKeys.filterIndexed { index, _ -> listView.isItemChecked(index) }.toSet()
            onPicked(selected)
        }

        dialog.show()
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
        val reportBuilder = StringBuilder()
        reportBuilder.append("Vehicle Diagnostic Report\n")
        reportBuilder.append("-------------------------\n\n")

        for (code in dtcList) {
            if (code.standardCode.isEmpty()) continue

            val formattedCode =
                if (!code.failureType?.code.isNullOrEmpty()) {
                    "${code.standardCode}-${code.failureType.code}"
                } else {
                    code.standardCode
                }

            reportBuilder.append("DTC: $formattedCode\n")
            reportBuilder.append("Description: ${code.description ?: "Unknown"}\n")

            val systemTxt = code.system?.description
            val categoryTxt = code.category?.description
            if (!systemTxt.isNullOrBlank() || !categoryTxt.isNullOrBlank()) {
                reportBuilder.append("System: ${systemTxt ?: "N/A"} | Category: ${categoryTxt ?: "N/A"}\n")
            }

            val hex = code.rawHex ?: "N/A"
            val activeStatuses = code.activeStatuses?.joinToString(", ") ?: "None"
            reportBuilder.append("Status: $activeStatuses (Hex: $hex)\n")

            val snapshot = code.snapshot
            if (snapshot != null && dataLoggerSettings.instance().adapter.dtcReadSnapshots) {
                reportBuilder.append("Snapshot (Record ${snapshot.size}):\n")
                snapshot.forEach { did ->
                    val value = did.decodedValue ?: "N/A"
                    val unit = did.definition?.units ?: ""
                    val desc = did.definition?.description ?: "Unknown DID"
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

        val shareIntent = Intent.createChooser(sendIntent, "Share Diagnostic Report")
        startActivity(shareIntent)
    }

    private fun diagnosticTroubleCodes(): List<DiagnosticTroubleCode> =
        VehicleCapabilitiesManager
            .getDiagnosticTroubleCodes()
            .apply {
                if (isEmpty()) {
                    add(
                        DiagnosticTroubleCode(
                            "",
                            "",
                            null,
                            resources.getString(R.string.pref_dtc_no_dtc_found),
                            0,
                            null,
                            null,
                            null,
                            null,
                            DtcComponent("", "")
                        )
                    )
                }
            }.sortedWith(
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

    override fun onResume() {
        super.onResume()
        registerReceiver(requireContext(), dtcNotificationsReceiver) {
            it.addAction(DATA_LOGGER_DTC_ACTION_COMPLETED)
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(dtcNotificationsReceiver)
        setLoadingState(false)
    }

    private fun handleDTCChangedNotification() {
        setLoadingState(false)

        val newCodes = diagnosticTroubleCodes()
        adapter.submitList(newCodes.toDtcListItems())

        if (isDtcAvailable(newCodes)) {
            shareButton.isEnabled = false
            clearButton.isEnabled = false
        } else {
            shareButton.isEnabled = true
            clearButton.isEnabled = true
        }

        clearButton.text = "Clear Codes"
    }

    private fun isDtcAvailable(newCodes: List<DiagnosticTroubleCode>): Boolean =
        newCodes.size == 1 && newCodes.first().standardCode.isEmpty()
}
