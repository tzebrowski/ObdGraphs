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
package org.obd.graphs.wizard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_CONNECT_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_NO_NETWORK_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_INCORRECT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_NOT_CONNECTED
import org.obd.graphs.bl.query.Query
import org.obd.graphs.registerReceiver
import org.obd.graphs.ui.common.toast
import org.obd.graphs.ui.withDataLogger

// Not a real deadline - some adapters (Bluetooth ELM327/STN handshakes especially) legitimately take
// this long or more. It only surfaces an informational "still trying" message; it must never cancel
// the attempt or race an in-flight CONNECTED_EVENT, since the real connection is still progressing.
private const val CONNECTION_SLOW_WARNING_MS = 45_000L

class TestConnectionStepFragment : Fragment(R.layout.fragment_wizard_test_connection) {
    private lateinit var tvStatus: TextView
    private lateinit var btnTest: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private val slowWarningRunnable =
        Runnable { updateStatus(R.string.wizard_step_test_connection_status_slow, enableButton = false) }

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    DATA_LOGGER_CONNECTING_EVENT -> updateStatus(R.string.wizard_step_test_connection_status_connecting, enableButton = false)
                    DATA_LOGGER_CONNECTED_EVENT -> onResult(R.string.wizard_step_test_connection_status_connected)
                    DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> onResult(R.string.wizard_step_test_connection_status_no_adapter)
                    DATA_LOGGER_ERROR_CONNECT_EVENT,
                    DATA_LOGGER_ERROR_EVENT,
                    DATA_LOGGER_WIFI_INCORRECT,
                    DATA_LOGGER_WIFI_NOT_CONNECTED,
                    DATA_LOGGER_NO_NETWORK_EVENT -> onResult(R.string.wizard_step_test_connection_status_failed)
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvWizardTestConnectionStatus)
        btnTest = view.findViewById(R.id.btnWizardTestConnection)

        btnTest.setOnClickListener {
            updateStatus(R.string.wizard_step_test_connection_status_connecting, enableButton = false)
            handler.postDelayed(slowWarningRunnable, CONNECTION_SLOW_WARNING_MS)
            withDataLogger { start(Query.instance()) }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(requireContext(), receiver) {
            it.addAction(DATA_LOGGER_CONNECTING_EVENT)
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            it.addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            it.addAction(DATA_LOGGER_ERROR_EVENT)
            it.addAction(DATA_LOGGER_WIFI_INCORRECT)
            it.addAction(DATA_LOGGER_WIFI_NOT_CONNECTED)
            it.addAction(DATA_LOGGER_NO_NETWORK_EVENT)
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(slowWarningRunnable)
        requireContext().unregisterReceiver(receiver)
    }

    // Only real, system-reported outcomes (a CONNECTED_EVENT or an actual error event) are treated as
    // final and stop the test connection - the slow-warning message above is not authoritative.
    private fun onResult(textRes: Int) {
        handler.removeCallbacks(slowWarningRunnable)
        updateStatus(textRes, enableButton = true)
        toast(textRes)
        withDataLogger { stop() }
    }

    private fun updateStatus(
        textRes: Int,
        enableButton: Boolean
    ) {
        if (!isAdded) return
        tvStatus.setText(textRes)
        btnTest.isEnabled = enableButton
    }
}
