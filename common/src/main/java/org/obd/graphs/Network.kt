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
package org.obd.graphs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"
const val REQUEST_LOCATION_PERMISSIONS = "REQUEST_LOCATION_PERMISSION"
private const val BLE_SCAN_DURATION_MS = 6000L

object Network {

    private class BTReceiver(
        private val targetMacAddress: String,
        private val func: () -> Unit
    ) : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (device?.address?.equals(targetMacAddress, ignoreCase = true) == true) {
                        Log.i(TAG, "Device $targetMacAddress connected at Link Level")
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.i(TAG, "Bluetooth Radio toggled ON, checking bonded devices...")
                        checkBondedAndNotify(context, targetMacAddress, func)
                    }
                }
            }
        }
    }

    private const val TAG: String = "Network"

    var currentSSID: String? = ""

    fun bluetoothAdapter(context: Context? = getContext()): BluetoothAdapter? =
        (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private fun checkBondedAndNotify(
        context: Context,
        targetMacAddress: String,
        func: () -> Unit
    ): Boolean =
        try {
            val adapter = bluetoothAdapter(context)
            val device =
                adapter?.bondedDevices?.find {
                    it.address.equals(
                        targetMacAddress,
                        ignoreCase = true
                    )
                }

            if (device == null) {
                Log.w(TAG, "Target MAC $targetMacAddress not found in bonded devices.")
                false
            } else {
                Log.i(
                    TAG,
                    "Target BT (${device.name}) found in bonded list. Triggering Event."
                )
                func()
                true
            }
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
            false
        }

    fun findBluetoothAdapterByName(deviceAddress: String): BluetoothDevice? {
        return try {
            bluetoothAdapter()?.bondedDevices?.find { deviceAddress == it.address }
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
            return null
        }
    }

    /**
     * Resolves a device straight from its MAC, without requiring it to be bonded.
     *
     * Most BLE OBD dongles are never paired in the system Bluetooth settings, so
     * [findBluetoothAdapterByName] - which only searches bonded devices - cannot find them.
     */
    fun bluetoothDeviceByAddress(deviceAddress: String): BluetoothDevice? =
        try {
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                bluetoothAdapter()?.getRemoteDevice(deviceAddress)
            } else {
                Log.w(TAG, "Not a valid Bluetooth MAC address: $deviceAddress")
                null
            }
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
            null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to resolve Bluetooth device: $deviceAddress", e)
            null
        }

    /**
     * Collects BLE devices for the adapter picker: a time-boxed active scan merged with the
     * bonded LE/dual devices, deduplicated by MAC.
     *
     * The scan is deliberately unfiltered. Filtering on a service UUID only matches devices that
     * ADVERTISE it, and OBD adapters advertise a local name and expose their GATT services only
     * once connected - filtering that way yields an empty list against a working adapter.
     *
     * Blocks the calling thread for [scanDurationMs]; callers run it off the main thread.
     */
    fun scanBleDevices(scanDurationMs: Long = BLE_SCAN_DURATION_MS): List<BluetoothDevice> {
        // Filled from the scanner's binder thread while this one waits on the latch.
        val found = ConcurrentHashMap<String, BluetoothDevice>()

        try {
            val adapter = bluetoothAdapter() ?: return emptyList()

            adapter.bondedDevices
                ?.filter { it.type == BluetoothDevice.DEVICE_TYPE_LE || it.type == BluetoothDevice.DEVICE_TYPE_DUAL }
                ?.forEach { found[it.address] = it }

            val scanner = adapter.bluetoothLeScanner
            if (scanner == null || !adapter.isEnabled) {
                Log.w(TAG, "BLE scanner is not available. Bluetooth enabled=${adapter.isEnabled}")
                return found.values.toList()
            }

            val latch = CountDownLatch(1)
            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult?
                    ) {
                        result?.device?.let { found.putIfAbsent(it.address, it) }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "BLE scan failed, errorCode=$errorCode")
                        latch.countDown()
                    }
                }

            scanner.startScan(callback)
            latch.await(scanDurationMs, TimeUnit.MILLISECONDS)
            scanner.stopScan(callback)

            Log.i(TAG, "BLE scan completed. Found ${found.size} device(s)")
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan for BLE devices", e)
        }

        return found.values.toList()
    }

    fun findWifiSSID(): List<String> =
        if (EasyPermissions.hasPermissions(
                getContext()!!,
                Manifest.permission.ACCESS_FINE_LOCATION,
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

    fun setupConnectedNetworksCallback() {
        try {
            Log.i(LOG_LEVEL, "Starting network setup")

            val wifiCallback =
                when {
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
                    val request =
                        NetworkRequest
                            .Builder()
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

    fun startBondedDeviceMonitor(
        context: Context,
        targetMacAddress: String,
        func: () -> Unit
    ) {
        if (checkBondedAndNotify(context, targetMacAddress, func)) {
            return
        }

        try {
            val filter =
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                }
            context.registerReceiver(BTReceiver(targetMacAddress, func), filter)
            Log.i(TAG, "Passive monitor started for $targetMacAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register passive monitor", e)
        }
    }
}
