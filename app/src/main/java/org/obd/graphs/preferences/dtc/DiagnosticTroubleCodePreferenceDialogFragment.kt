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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_CLEANUP_COMPLETED
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_CLEANUP_SCHEDULE
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_READ_SCHEDULE
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.registerReceiver
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.command.dtc.DtcComponent

internal class DiagnosticTroubleCodePreferenceDialogFragment : CoreDialogFragment() {
    private lateinit var adapter: DiagnosticTroubleCodeViewAdapter
    private lateinit var clearButton: Button

    private val dtcClearReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context?,
                intent: Intent?,
            ) {
                if (intent?.action == DATA_LOGGER_DTC_CLEANUP_COMPLETED) {
                    handleClearResult()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        requestWindowFeatures()

        val root = inflater.inflate(R.layout.dialog_dtc, container, false)
        val sortedDtcList = diagnosticTroubleCodes()

        adapter = DiagnosticTroubleCodeViewAdapter(context)
        adapter.submitList(sortedDtcList)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachButtons(root, sortedDtcList)
        attachCloseButton(root)

        return root
    }

    private fun attachButtons(
        root: View,
        sortedDtcList: List<DiagnosticTroubleCode>,
    ) {
        val shareButton: Button = root.findViewById(R.id.action_share)
        val refreshButton: Button = root.findViewById(R.id.action_refresh_dtc)

        clearButton = root.findViewById(R.id.action_clear_dtc)

        shareButton.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE

        shareButton.setOnClickListener {
            shareDtcReport(sortedDtcList)
        }

        refreshButton.setOnClickListener {
            sendBroadcastEvent(DATA_LOGGER_DTC_READ_SCHEDULE)
        }

        clearButton.setOnClickListener {
            android.app.AlertDialog
                .Builder(requireContext())
                .setTitle("Clear Diagnostic Codes?")
                .setMessage(
                    "Are you sure you want to clear all DTCs from the ECU?\n\nMake sure the engine is OFF, but the ignition is ON.",
                ).setPositiveButton("Clear Codes") { dialog, _ ->
                    sendBroadcastEvent(DATA_LOGGER_DTC_CLEANUP_SCHEDULE)
                    clearButton.isEnabled = false
                    clearButton.text = "Clearing..."
                    dialog.dismiss()
                }.setNegativeButton("Cancel", null)
                .show()
        }
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
            reportBuilder.append("\n") // Blank line between codes
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

    private fun diagnosticTroubleCodes(): List<DiagnosticTroubleCode> {
        val diagnosticTroubleCodes = VehicleCapabilitiesManager.getDiagnosticTroubleCodes()

        if (diagnosticTroubleCodes.isEmpty()) {
            val noDTC =
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
                    DtcComponent("", ""),
                )

            diagnosticTroubleCodes.add(noDTC)
        }

        val sortedDtcList =
            diagnosticTroubleCodes
                .sortedWith(
                    compareBy<DiagnosticTroubleCode> { code ->
                        val desc = code.description
                        val isUnknown =
                            desc.isNullOrBlank() ||
                                desc.contains(
                                    "Unknown DTC Description",
                                    ignoreCase = true,
                                )
                        if (isUnknown) 1 else 0
                    }.thenBy { code ->
                        code.standardCode
                    },
                ).toMutableList()
        return sortedDtcList
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(requireContext(), dtcClearReceiver) {
            it.addAction(DATA_LOGGER_DTC_CLEANUP_COMPLETED)
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(dtcClearReceiver)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleClearResult() {
        val newCodes = diagnosticTroubleCodes()
        adapter.submitList(newCodes)

        clearButton.isEnabled = true
        clearButton.text = "Clear Codes"
    }
}
