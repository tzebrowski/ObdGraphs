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

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import org.obd.graphs.Network
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.runAsync
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize
import java.util.LinkedList

private const val LOG_TAG = "BleAdaptersListPreferences"

/**
 * Picks the BLE adapter. Unlike [BluetoothClassicAdaptersListPreferences] this cannot list bonded
 * devices alone - most BLE OBD dongles are never paired in the system Bluetooth settings - so it
 * runs an active scan and merges the results with the bonded LE devices.
 *
 * The scan blocks for a few seconds, hence [runAsync] when the list is first built.
 */
class BleAdaptersListPreferences(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {

    // Scanning on every getEntries()/getEntryValues() call would freeze the settings screen, so
    // the result of the one scan is held for the lifetime of this preference.
    private val devices: MutableList<Device> = LinkedList()

    init {
        setOnPreferenceChangeListener { _, _ ->
            navigateToPreferencesScreen("pref.adapter.connection")
            true
        }

        runAsync {
            scan()
            publish()
        }
    }

    override fun getSummary(): CharSequence = super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)

    override fun getEntries(): Array<CharSequence> {
        publish()
        return super.getEntries()
    }

    override fun getEntryValues(): Array<CharSequence> {
        publish()
        return super.getEntryValues()
    }

    private fun publish() {
        setEntries(devices.map { it.label as CharSequence }.toTypedArray())
        entryValues = devices.map { it.address as CharSequence }.toTypedArray()
    }

    private fun scan() {
        try {
            val found =
                Network
                    .scanBleDevices()
                    .sortedBy { it.name ?: it.address }
                    .map { Device(address = it.address, label = formatDeviceLabel(it.name, it.address)) }

            devices.clear()
            devices.addAll(found)

            Log.i(LOG_TAG, "Found ${devices.size} BLE device(s)")
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
        }
    }
}
