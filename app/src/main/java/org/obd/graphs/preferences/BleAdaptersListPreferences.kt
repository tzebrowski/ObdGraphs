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
package org.obd.graphs.preferences

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import org.obd.graphs.Network
import org.obd.graphs.R
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.runAsync
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize
import org.obd.graphs.ui.common.toast
import java.util.concurrent.atomic.AtomicBoolean

private const val LOG_TAG = "BleAdaptersListPreferences"

/**
 * Picks the BLE adapter.
 *
 * Tapping the preference runs an explicit scan behind a progress dialog and opens the list once it
 * is done - a scan in the background gave the user no idea anything was happening, and its
 * results could not reach a list that was already open.
 *
 * Only LE-capable devices are listed. A dual-mode adapter shows up twice in Android - once as the
 * bonded Classic record, once as an advertising LE device with its OWN address - and the Classic
 * record (DEVICE_TYPE_CLASSIC) connects over BR/EDR, where the adapter exposes no GATT services.
 */
class BleAdaptersListPreferences(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {

    private val devices = mutableListOf<Device>()
    private val scanning = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Main thread only. Kept so that refreshing the instant sources does not throw away what the
    // last scan found - an unpaired BLE adapter is ONLY ever in here.
    private var lastScanned = emptyList<BluetoothDevice>()

    // Main thread only. Cleared by Cancel, so a scan that finishes afterwards does not pop the list.
    private var listRequested = false
    private var progressDialog: AlertDialog? = null

    init {
        setOnPreferenceChangeListener { _, _ ->
            navigateToPreferencesScreen("pref.adapter.connection")
            true
        }
    }

    override fun getSummary(): CharSequence = super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)

    override fun onAttached() {
        super.onAttached()
        // Not in init: the persisted value is only restored once attached, and the summary reads
        // "Not set" whenever the stored address is missing from the entries.
        publishCandidates()
    }

    override fun onClick() {
        listRequested = true
        showProgress()
        scan()
    }

    private fun showList() = super.onClick()

    private fun showProgress() {
        if (progressDialog?.isShowing == true) {
            return
        }

        val padding = (24 * context.resources.displayMetrics.density).toInt()
        val content =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(padding, padding, padding, padding)
                addView(ProgressBar(context))
                addView(
                    TextView(context).apply {
                        setText(R.string.pref_ble_scanning)
                        setPadding(padding, 0, 0, 0)
                    }
                )
            }

        progressDialog =
            AlertDialog
                .Builder(context)
                .setTitle(dialogTitle)
                .setView(content)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .setOnCancelListener { listRequested = false }
                .show()
    }

    private fun scan() {
        if (!scanning.compareAndSet(false, true)) {
            // Already running - its completion serves this request too.
            return
        }

        runAsync {
            try {
                val scanned = Network.scanBleDevices()
                Log.i(LOG_TAG, "Scan added ${scanned.size} advertising device(s)")

                // The scan runs off the main thread, but the entries it produces feed a
                // ListPreference - that, and the dialogs, have to be handed back to the main thread.
                mainHandler.post {
                    lastScanned = scanned
                    publishCandidates()
                    onScanFinished(scanned.size)
                }
            } finally {
                scanning.set(false)
            }
        }
    }

    private fun onScanFinished(scannedCount: Int) {
        progressDialog?.dismiss()
        progressDialog = null

        if (!listRequested) {
            return
        }
        listRequested = false

        reportScanOutcome(scannedCount)
        showList()
    }

    private fun publishCandidates() {
        val candidates = Network.bluetoothCandidates().filter(::isLeCapable) + lastScanned
        publish(candidates + listOfNotNull(selectedDevice(candidates)))
    }

    /**
     * The stored adapter is usually an unbonded one found by a scan, so after the screen is
     * rebuilt it is in none of the instant sources. Listing it anyway keeps the summary showing it.
     */
    private fun selectedDevice(candidates: List<BluetoothDevice>): BluetoothDevice? {
        val selected = value?.takeIf { it.isNotBlank() } ?: return null
        if (candidates.any { it.address.equals(selected, ignoreCase = true) }) {
            return null
        }
        return Network.bluetoothDeviceByAddress(selected)
    }

    private fun isLeCapable(device: BluetoothDevice): Boolean =
        try {
            device.type != BluetoothDevice.DEVICE_TYPE_CLASSIC
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            false
        }

    /**
     * A scan that finds nothing is indistinguishable from a broken one in the UI, and the usual
     * cause - location services switched off, which makes a BLE scan return nothing at all with
     * no error - is not something the user would think to check.
     */
    private fun reportScanOutcome(scannedCount: Int) {
        if (scannedCount > 0) {
            return
        }

        if (!Network.isLocationEnabled()) {
            Log.w(LOG_TAG, "Scan found nothing and location services are OFF")
            toast(R.string.pref_ble_scan_needs_location)
        } else {
            Log.w(LOG_TAG, "Scan found nothing while location services are on")
            toast(R.string.pref_ble_scan_found_nothing)
        }
    }

    /** Bonded and connected devices come first, then the scan, then the stored selection. */
    private fun publish(found: List<BluetoothDevice>) {
        val merged = LinkedHashMap<String, Device>()

        found.forEach { device ->
            merged.putIfAbsent(device.address, toEntry(device))
        }

        devices.clear()
        devices.addAll(merged.values)

        setEntries(devices.map { it.label as CharSequence }.toTypedArray())
        entryValues = devices.map { it.address as CharSequence }.toTypedArray()

        Log.i(LOG_TAG, "Published ${devices.size} BLE adapter candidate(s)")
    }

    private fun toEntry(device: BluetoothDevice): Device =
        try {
            val paired = device.bondState == BluetoothDevice.BOND_BONDED
            val name = device.name ?: device.address

            Device(
                address = device.address,
                label = formatDeviceLabel(if (paired) "✔ $name" else name, device.address)
            )
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            Device(address = device.address, label = formatDeviceLabel(null, device.address))
        }
}
