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
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_MSG_EXTRA_PARAM_NAME
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.activity.SCREEN_LOCK_SHOW_CANCEL_BUTTON_EXTRA_PARAM_NAME
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
        adapter.submitList(sortedDtcList)
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
                setLoadingState(true)
                withDataLogger {
                    scheduleDTCRead()
                }
            } else {
                toast(R.string.pref_dtc_no_connection_established)
            }
        }

        clearButton.setOnClickListener {
            android.app.AlertDialog
                .Builder(requireContext())
                .setTitle(resources.getString(R.string.pref_dtc_clean_dialog_title))
                .setMessage(
                    resources.getString(R.string.pref_dtc_clean_dialog_confirm_message)
                ).setPositiveButton("Clear Codes") { dialog, _ ->
                    if (DataLoggerRepository.isRunning()) {
                        setLoadingState(true)
                        withDataLogger {
                            scheduleDTCCleanup()
                        }

                        toast(R.string.pref_dtc_clean_dialog_send_message)
                        clearButton.text = "Clearing..."
                        dialog.dismiss()
                    } else {
                        toast(R.string.pref_dtc_no_connection_established)
                    }
                }.setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setLoadingState(isLoading: Boolean) =
        if (isLoading) {
            sendBroadcastEvent(
                SCREEN_LOCK_PROGRESS_EVENT,
                mapOf(
                    SCREEN_LOCK_SHOW_CANCEL_BUTTON_EXTRA_PARAM_NAME to true,
                    SCREEN_LOCK_MSG_EXTRA_PARAM_NAME to org.obd.graphs.getContext()
                        ?.getText(R.string.pref_dtc_screen_lock) as String
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
        adapter.submitList(newCodes)

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
