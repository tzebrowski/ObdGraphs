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
package org.obd.graphs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import pub.devrel.easypermissions.EasyPermissions


import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.RequiresPermission

private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"
const val REQUEST_LOCATION_PERMISSIONS = "REQUEST_LOCATION_PERMISSION"

object Network {

    private const val TAG: String = "Network"

    private class AutoConnectBTReceiver(private val targetMacAddress: String) : BroadcastReceiver() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.action
            when (action) {
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "12-second scan finished. The radio is resting.")
                    bluetoothAdapter(context)?.cancelDiscovery()
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val macAddress = device?.address

                    if (Log.isLoggable(TAG,Log.DEBUG)) {
                        Log.d(TAG, "Found device: ${device?.name} at $macAddress")
                    }
                    if (macAddress == targetMacAddress) {
                        Log.e(
                            TAG,
                            "Target (${device.name} ${device.address}) OBD adapter found via scan"
                        )
                        sendBroadcastEvent(DATA_LOGGER_AUTO_CONNECT_EVENT)
                    }
                }
            }

        }
    }

    var currentSSID: String? = ""
    fun bluetoothAdapter(context: Context? = getContext()): BluetoothAdapter? =
        (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    fun findBluetoothAdapterByName(deviceAddress: String): BluetoothDevice? {
        return try {
            bluetoothAdapter()?.bondedDevices?.find { deviceAddress == it.address }
        } catch (e: SecurityException) {
            requestBluetoothPermissions()
            return null
        }
    }

    fun findWifiSSID(): List<String> {
        return if (EasyPermissions.hasPermissions(
                getContext()!!, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            try {
                val wifiManager =
                    getContext()?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.startScan()
                val ll = mutableListOf<String>()
                wifiManager.scanResults.forEach {
                    Log.d(LOG_LEVEL, "Found WIFI SSID: ${it.SSID}")
                    ll.add(it.SSID)
                }
                ll
            } catch (e: SecurityException) {
                Log.e(LOG_LEVEL, "User does not has access to ACCESS_COARSE_LOCATION permission.")
                sendBroadcastEvent(REQUEST_LOCATION_PERMISSIONS)
                emptyList()
            }
        } else {
            Log.e(LOG_LEVEL, "User does not has access to ACCESS_COARSE_LOCATION permission.")
            sendBroadcastEvent(REQUEST_LOCATION_PERMISSIONS)
            emptyList()
        }
    }

    fun setupConnectedNetworksCallback() {
        try {
            Log.i(LOG_LEVEL, "Starting network setup")

            val wifiCallback = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities
                        ) {
                            currentSSID = readSSID(networkCapabilities)
                        }
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities
                        ) {
                            currentSSID = readSSID(networkCapabilities)
                        }
                    }
                }

                else -> null
            }

            wifiCallback?.let {
                getContext()?.let { contextWrapper ->
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
                    val connectivityManager =
                        contextWrapper.getSystemService(ConnectivityManager::class.java)
                    connectivityManager.requestNetwork(request, it)
                    connectivityManager.registerNetworkCallback(request, it)
                }
            }

            Log.i(LOG_LEVEL, "Network setup completed")

        } catch (e: Exception) {
            Log.e(LOG_LEVEL, "Failed to complete network registration", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readSSID(networkCapabilities: NetworkCapabilities): String? {
        val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
        val ssid = wifiInfo?.ssid?.trim()?.replace("\"", "")
        if (Log.isLoggable(LOG_LEVEL, Log.VERBOSE)) {
            Log.v(LOG_LEVEL, "Wifi state changed, current WIFI SSID: $ssid, $wifiInfo")
        }
        return ssid
    }

    fun bluetooth(enable: Boolean) {
        Log.i(LOG_LEVEL, "Changing status of Bluetooth, enable: $enable")

        try {
            bluetoothAdapter()?.let {
                if (enable) {
                    it.enable()
                } else {
                    it.disable()
                }
            }
        } catch (e: SecurityException) {
            requestBluetoothPermissions()
        }
    }

    fun requestBluetoothPermissions() {
        sendBroadcastEvent(REQUEST_PERMISSIONS_BT)
    }

    fun wifi(enable: Boolean) {
        Log.i(LOG_LEVEL, "Changing status of WIFI, enable: $enable")

        getContext()?.let { it ->
            (it.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.apply {
                isWifiEnabled = enable
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun startBackgroundBleScanForMac(context: Context, targetMacAddress: String) {
        try {

            if (!Permissions.isBLEScanPermissionsGranted(context)) return
            if (!Permissions.isLocationEnabled(context)) return

            val bluetoothAdapter: BluetoothAdapter? = bluetoothAdapter(context)

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Device doesn't support Bluetooth")
                return
            }

            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            context.registerReceiver(AutoConnectBTReceiver(targetMacAddress), filter)

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            val scanStarted = bluetoothAdapter.startDiscovery()
            Log.i(TAG, "Started scanning for Classic Bluetooth device = $scanStarted")

        } catch (_: SecurityException) {
            requestBluetoothPermissions()
        }
    }
}
