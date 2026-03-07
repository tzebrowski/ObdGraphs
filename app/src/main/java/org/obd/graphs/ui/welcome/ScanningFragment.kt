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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R

class ScanningFragment : Fragment(R.layout.fragment_scanning) {
    private lateinit var adapter: DeviceListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private var connectionType: String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        tvTitle = view.findViewById(R.id.tvTitle)
        val rvDevices = view.findViewById<RecyclerView>(R.id.rvDevices)

        adapter =
            DeviceListAdapter { selectedDevice ->
                onDeviceClicked(selectedDevice)
            }
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = adapter

        val sharedPrefs = requireActivity().getSharedPreferences("OBD_PREFS", Context.MODE_PRIVATE)
        connectionType = sharedPrefs.getString("CONNECTION_TYPE", "BLUETOOTH")

        startScanning()
    }

    private fun startScanning() {
        tvTitle.text = "Looking for $connectionType adapters..."
        progressBar.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            simulateDeviceFound()
        }, 1500)
    }

    private fun simulateDeviceFound() {
        // Mock data based on the connection type
        when (connectionType) {
            "BLUETOOTH" -> {
                adapter.addDevice(ObdDevice("OBDII", "00:1D:A5:68:98:8A"))
                adapter.addDevice(ObdDevice("V-LINK", "11:22:33:44:55:66"))
            }
            "WIFI" -> {
                adapter.addDevice(ObdDevice("WiFi_OBD2", "192.168.0.10:35000"))
            }
            "USB" -> {
                adapter.addDevice(ObdDevice("FTDI Serial Device", "USB/COM3"))
            }
        }

        progressBar.visibility = View.GONE
        tvTitle.text = "Select your adapter"
    }

    private fun onDeviceClicked(device: ObdDevice) {
        val sharedPrefs = requireActivity().getSharedPreferences("OBD_PREFS", Context.MODE_PRIVATE)
        sharedPrefs
            .edit()
            .putString("SELECTED_DEVICE_NAME", device.name)
            .putString("SELECTED_DEVICE_ADDRESS", device.address)
            .apply()

        findNavController().navigate(R.id.action_scanning_to_testing)
    }
}
