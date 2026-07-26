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
package org.obd.graphs.preferences.dri

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.MODULE_DISCOVERY_RESULT_EVENT
import org.obd.graphs.bl.datalogger.ModuleDiscoveryResult
import org.obd.graphs.getSerializableCompat
import org.obd.graphs.registerReceiver
import org.obd.graphs.ui.common.toast
import org.obd.graphs.ui.withDataLogger

private const val PROBE_TIMEOUT_MS = 1500L

// Sweeps a range of candidate CAN headers (default DAxxF1, the convention used across every
// bundled Giorgio-platform profile) with a UDS TesterPresent probe, letting the user add whichever
// headers answer as new (unnamed) Diagnostic Request ID entries - renamed afterward via the
// existing DRI edit UI, same as any other entry.
class EcuDiscoveryDialogFragment(
    private val onModulesAdded: () -> Unit
) : BottomSheetDialogFragment() {

    private var sweepJob: Job? = null
    private val checkboxes = mutableMapOf<String, CheckBox>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_ecu_discovery, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val etHeaderPrefix = view.findViewById<TextInputEditText>(R.id.etHeaderPrefix)
        val etHeaderSuffix = view.findViewById<TextInputEditText>(R.id.etHeaderSuffix)
        val etRangeStart = view.findViewById<TextInputEditText>(R.id.etRangeStart)
        val etRangeEnd = view.findViewById<TextInputEditText>(R.id.etRangeEnd)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val btnStart = view.findViewById<MaterialButton>(R.id.btnStart)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val tvResultsTitle = view.findViewById<TextView>(R.id.tvResultsTitle)
        val llResults = view.findViewById<android.widget.LinearLayout>(R.id.llResults)
        val btnAddToDri = view.findViewById<MaterialButton>(R.id.btnAddToDri)

        btnStart.setOnClickListener {
            if (!DataLoggerRepository.isRunning()) {
                toast(R.string.pref_dtc_no_connection_established)
                return@setOnClickListener
            }

            val prefix = etHeaderPrefix.text.toString().trim().uppercase()
            val suffix = etHeaderSuffix.text.toString().trim().uppercase()
            val start = etRangeStart.text.toString().trim().toIntOrNull(16)
            val end = etRangeEnd.text.toString().trim().toIntOrNull(16)

            if (start == null || end == null || start !in 0..255 || end !in 0..255 || start > end) {
                toast(R.string.pref_adapter_ecu_discovery_invalid_range)
                return@setOnClickListener
            }

            checkboxes.clear()
            llResults.removeAllViews()
            tvResultsTitle.visibility = View.GONE
            btnAddToDri.visibility = View.GONE
            btnStart.visibility = View.GONE
            btnCancel.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
            progressBar.max = end - start + 1

            sweepJob = viewLifecycleOwner.lifecycleScope.launch {
                val total = end - start + 1
                for (b in start..end) {
                    val header = "$prefix${"%02X".format(b)}$suffix"
                    tvStatus.text = getString(
                        R.string.pref_adapter_ecu_discovery_status_scanning,
                        header,
                        b - start + 1,
                        total
                    )

                    if (probeHeader(header)) {
                        addResultRow(llResults, header)
                        tvResultsTitle.visibility = View.VISIBLE
                        btnAddToDri.visibility = View.VISIBLE
                    }

                    progressBar.progress = b - start + 1
                }

                tvStatus.text = getString(R.string.pref_adapter_ecu_discovery_status_done, checkboxes.size)
                progressBar.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
            }
        }

        btnCancel.setOnClickListener {
            sweepJob?.cancel()
            tvStatus.text = getString(R.string.pref_adapter_ecu_discovery_status_done, checkboxes.size)
            progressBar.visibility = View.GONE
            btnCancel.visibility = View.GONE
            btnStart.visibility = View.VISIBLE
        }

        btnAddToDri.setOnClickListener {
            checkboxes.filterValues { it.isChecked }.keys.forEach { header ->
                DiagnosticRequestIDManager.addMapping(
                    requestKey = header,
                    headerValue = header,
                    description = getString(R.string.pref_adapter_ecu_discovery_discovered_description)
                )
            }
            onModulesAdded()
            dismiss()
        }
    }

    private fun addResultRow(
        container: android.widget.LinearLayout,
        header: String
    ) {
        val checkBox = CheckBox(requireContext())
        checkBox.text = header
        checkBox.isChecked = true
        container.addView(checkBox)
        checkboxes[header] = checkBox
    }

    private suspend fun probeHeader(header: String): Boolean =
        withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val result = intent?.getSerializableCompat<ModuleDiscoveryResult>() ?: return
                        if (result.header == header) {
                            runCatching { requireContext().unregisterReceiver(this) }
                            if (continuation.isActive) {
                                continuation.resume(result.found, onCancellation = null)
                            }
                        }
                    }
                }

                registerReceiver(requireContext(), receiver) { it.addAction(MODULE_DISCOVERY_RESULT_EVENT) }
                continuation.invokeOnCancellation {
                    runCatching { requireContext().unregisterReceiver(receiver) }
                }

                withDataLogger { discoverModule(header) }
            }
        } ?: false

    override fun onDestroyView() {
        sweepJob?.cancel()
        super.onDestroyView()
    }
}
