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
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.location.LocationManagerCompat
import pub.devrel.easypermissions.EasyPermissions
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"
const val REQUEST_LOCATION_PERMISSIONS = "REQUEST_LOCATION_PERMISSION"
private const val BLE_SCAN_DURATION_MS = 8000L

/**
 * How long a targeted scan waits for ONE known adapter. Shorter than the picker's sweep: this one
 * runs in front of a connect attempt, and an adapter that is powered and in range answers within
 * an advertising interval or two.
 */
private const val BLE_TARGET_SCAN_DURATION_MS = 5000L

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
     *
     * A device the system already knows is PREFERRED over one synthesised from the MAC, because
     * `getRemoteDevice(String)` always labels the address PUBLIC. An adapter advertising a random
     * address then gets connected as the wrong address type, which typically presents as a link
     * that comes up and then exposes no GATT services at all.
     */
    fun bluetoothDeviceByAddress(deviceAddress: String): BluetoothDevice? =
        try {
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                knownDeviceByAddress(deviceAddress) ?: bluetoothAdapter()?.getRemoteDevice(deviceAddress)
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

    /** The bonded and currently-connected devices carry their real address type; a MAC does not. */
    fun knownDeviceByAddress(deviceAddress: String): BluetoothDevice? =
        bluetoothCandidates()
            .find { it.address.equals(deviceAddress, ignoreCase = true) }
            ?.also { Log.i(TAG, "Resolved $deviceAddress from the bonded/connected devices, type=${it.type}") }

    /**
     * Waits for ONE specific MAC to advertise and returns the scanner's own [BluetoothDevice].
     *
     * Two things come out of this that `getRemoteDevice(mac)` cannot give:
     *
     * 1. The real ADDRESS TYPE. `getRemoteDevice(String)` always labels an address PUBLIC, so an
     *    adapter using a random address is connected as the wrong kind of peer - the link comes up
     *    and exposes no GATT services, or fails with status 133/147 for no visible reason. A device
     *    handed over by the scanner carries the type it actually advertised with.
     * 2. Proof that the adapter is IN RANGE. Without it every connect attempt is spent blind,
     *    ~30s at a time, against an adapter that may be switched off or still bonded elsewhere.
     *
     * Returns null as soon as the scan window elapses; the caller falls back to connecting blind,
     * because an adapter the phone is ALREADY connected to does not advertise at all.
     */
    fun findAdvertisingBleDevice(
        deviceAddress: String,
        scanDurationMs: Long = BLE_TARGET_SCAN_DURATION_MS
    ): BluetoothDevice? {
        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            Log.w(TAG, "Not a valid Bluetooth MAC address: $deviceAddress")
            return null
        }

        // Written by the scanner's binder thread, read here once the latch releases.
        val found = AtomicReference<BluetoothDevice>()

        try {
            val adapter = bluetoothAdapter() ?: return null
            val scanner = adapter.bluetoothLeScanner

            if (scanner == null || !adapter.isEnabled) {
                Log.w(TAG, "BLE scanner is not available. Bluetooth enabled=${adapter.isEnabled}")
                return null
            }

            if (!isLocationEnabled()) {
                Log.w(TAG, "Location services are OFF: a BLE scan will return no results on this platform.")
            }

            val latch = CountDownLatch(1)
            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult?
                    ) {
                        result?.device?.let {
                            if (found.compareAndSet(null, it)) {
                                logDevice("target scan", it)
                                latch.countDown()
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "BLE scan failed, errorCode=$errorCode")
                        latch.countDown()
                    }
                }

            // Filtering by ADDRESS is safe; filtering by service UUID is not - OBD adapters
            // advertise a local name and expose their services only once connected.
            val filters = listOf(ScanFilter.Builder().setDeviceAddress(deviceAddress).build())
            val settings =
                ScanSettings
                    .Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

            scanner.startScan(filters, settings, callback)
            latch.await(scanDurationMs, TimeUnit.MILLISECONDS)
            scanner.stopScan(callback)
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan for $deviceAddress", e)
        }

        if (found.get() == null) {
            Log.w(TAG, "$deviceAddress did not advertise within ${scanDurationMs}ms")
        }

        return found.get()
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
    /**
     * Devices that can be offered as a BLE adapter without waiting for a scan: everything bonded,
     * plus anything already connected.
     *
     * Deliberately UNFILTERED by [BluetoothDevice.getType]. That property reports how a device was
     * bonded or discovered, not what it supports - a dual-mode adapter paired over SPP reports
     * DEVICE_TYPE_CLASSIC while still exposing a GATT server. Filtering on it hid a CCY STN-2120,
     * which works over the HM-10 (FFE0) profile. The bonded list is short; let the user choose.
     */
    fun bluetoothCandidates(): List<BluetoothDevice> {
        val candidates = LinkedHashMap<String, BluetoothDevice>()

        try {
            bluetoothAdapter()?.bondedDevices?.forEach { candidates[it.address] = it }

            // An already-connected device does NOT advertise, so no scan will ever surface it.
            (getContext()?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)
                ?.getConnectedDevices(BluetoothProfile.GATT)
                ?.forEach { candidates.putIfAbsent(it.address, it) }

            candidates.values.forEach { logDevice("bonded/connected", it) }
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect bonded Bluetooth devices", e)
        }

        return candidates.values.toList()
    }

    /**
     * Actively scans for advertising BLE devices. Blocks for [scanDurationMs]; callers run it off
     * the main thread and should show [bluetoothCandidates] in the meantime.
     *
     * Unfiltered: scanning with a service-UUID filter matches only devices that ADVERTISE it, and
     * OBD adapters advertise a local name and expose their services only once connected.
     */
    fun scanBleDevices(scanDurationMs: Long = BLE_SCAN_DURATION_MS): List<BluetoothDevice> {
        // Filled from the scanner's binder thread while this one waits on the latch.
        val found = Collections.synchronizedMap(LinkedHashMap<String, BluetoothDevice>())

        try {
            val adapter = bluetoothAdapter() ?: return emptyList()
            val scanner = adapter.bluetoothLeScanner

            if (scanner == null || !adapter.isEnabled) {
                Log.w(TAG, "BLE scanner is not available. Bluetooth enabled=${adapter.isEnabled}")
                return emptyList()
            }

            // A scan silently returns NOTHING when location services are switched off, with no
            // error and no failure callback - so say so rather than looking like an empty area.
            if (!isLocationEnabled()) {
                Log.w(TAG, "Location services are OFF: a BLE scan will return no results on this platform.")
            }

            val latch = CountDownLatch(1)
            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult?
                    ) {
                        result?.device?.let { device ->
                            if (found.putIfAbsent(device.address, device) == null) {
                                logDevice("scanned", device)
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "BLE scan failed, errorCode=$errorCode")
                        latch.countDown()
                    }
                }

            // SCAN_MODE_LOW_LATENCY, not the startScan(callback) default of SCAN_MODE_LOW_POWER:
            // the low-power duty cycle listens for a fraction of a second every few seconds and
            // routinely misses an adapter that advertises at a slow interval. This scan is short
            // and user-initiated, so the battery cost does not matter.
            val settings =
                ScanSettings
                    .Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

            scanner.startScan(emptyList(), settings, callback)
            latch.await(scanDurationMs, TimeUnit.MILLISECONDS)
            scanner.stopScan(callback)

            Log.i(TAG, "BLE scan completed. Found ${found.size} advertising device(s)")
        } catch (_: SecurityException) {
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan for BLE devices", e)
        }

        // Results already queued on the binder thread can still land after stopScan().
        return synchronized(found) { found.values.toList() }
    }

    fun isLocationEnabled(): Boolean =
        try {
            val manager = getContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            manager != null && LocationManagerCompat.isLocationEnabled(manager)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read location state", e)
            true
        }

    /** Logged so a missing adapter can be diagnosed with `adb logcat -s Network`. */
    private fun logDevice(
        source: String,
        device: BluetoothDevice
    ) {
        try {
            Log.i(TAG, "BLE device [$source]: name=${device.name}, address=${device.address}, type=${device.type}, bondState=${device.bondState}")
        } catch (_: SecurityException) {
            Log.i(TAG, "BLE device [$source]: address=${device.address} (name needs BT permissions)")
        }
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
