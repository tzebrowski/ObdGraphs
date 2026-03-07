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
package org.obd.graphs.ui.welcome

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.obd.graphs.R

class TestingFragment : Fragment(R.layout.fragment_testing) {
    // UI Elements for Step 1: Adapter Connection
    private lateinit var progressStep1: ProgressBar
    private lateinit var checkStep1: ImageView

    // UI Elements for Step 2: Protocol Initialization
    private lateinit var progressStep2: ProgressBar
    private lateinit var checkStep2: ImageView

    // UI Elements for Step 3: ECU Negotiation
    private lateinit var progressStep3: ProgressBar
    private lateinit var checkStep3: ImageView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI Elements
        progressStep1 = view.findViewById(R.id.progressStep1)
        checkStep1 = view.findViewById(R.id.checkStep1)

        progressStep2 = view.findViewById(R.id.progressStep2)
        checkStep2 = view.findViewById(R.id.checkStep2)

        progressStep3 = view.findViewById(R.id.progressStep3)
        checkStep3 = view.findViewById(R.id.checkStep3)

        // Fetch the saved device data
        val sharedPrefs = requireActivity().getSharedPreferences("OBD_PREFS", Context.MODE_PRIVATE)
        val connectionType = sharedPrefs.getString("CONNECTION_TYPE", "BLUETOOTH")
        val deviceAddress = sharedPrefs.getString("SELECTED_DEVICE_ADDRESS", "")

        // Start the connection process
        startConnectionTest(connectionType, deviceAddress)
    }

    private fun startConnectionTest(
        connectionType: String?,
        deviceAddress: String?,
    ) {
        // We use lifecycleScope to ensure the coroutine cancels if the user leaves the screen early
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ---------------------------------------------------------
                // STEP 1: Connect to Adapter (Bluetooth Socket / Wi-Fi / USB)
                // ---------------------------------------------------------
                // TODO: Insert actual socket connection code here.
                // e.g., BluetoothAdapter.getRemoteDevice(deviceAddress).createRfcommSocketToServiceRecord(...)
                delay(1500) // Simulated delay

                withContext(Dispatchers.Main) {
                    progressStep1.visibility = View.GONE
                    checkStep1.visibility = View.VISIBLE
                }

                // ---------------------------------------------------------
                // STEP 2: Initialize OBD Protocol
                // ---------------------------------------------------------
                // TODO: Here is where you pass your input/output streams to your
                // obd-metrics Workflow.start() method. It sends ATZ, ATE0, etc.
                delay(1500) // Simulated delay

                withContext(Dispatchers.Main) {
                    progressStep2.visibility = View.GONE
                    checkStep2.visibility = View.VISIBLE
                }

                // ---------------------------------------------------------
                // STEP 3: Negotiate with Vehicle ECU
                // ---------------------------------------------------------
                // TODO: Wait for the obd-metrics library to confirm ECU response
                // (e.g., successful response to PID 0100).
                delay(1500) // Simulated delay

                withContext(Dispatchers.Main) {
                    progressStep3.visibility = View.GONE
                    checkStep3.visibility = View.VISIBLE
                }

                // ---------------------------------------------------------
                // SUCCESS: Navigate to the final screen
                // ---------------------------------------------------------
                delay(800) // Brief pause so the user can see the final checkmark
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_testing_to_success)
                }
            } catch (e: Exception) {
                // If anything fails (e.g., socket timeout, ECU not responding),
                // catch the error and update the UI so the user knows what went wrong.
                withContext(Dispatchers.Main) {
                    handleConnectionError(e)
                }
            }
        }
    }

    private fun handleConnectionError(e: Exception) {
        // In a real app, you would inspect the exception to see which step failed
        // and display an AlertDialog or SnackBar with troubleshooting steps.
        // For example: "Could not reach ECU. Please ensure the car ignition is ON."
    }
}
