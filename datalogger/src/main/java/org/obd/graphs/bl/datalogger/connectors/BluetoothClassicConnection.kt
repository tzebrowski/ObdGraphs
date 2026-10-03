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
package org.obd.graphs.bl.datalogger.connectors

import android.bluetooth.BluetoothSocket
import android.util.Log
import org.obd.graphs.Network
import org.obd.metrics.transport.AdapterConnection
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BluetoothClassicConnection"
private val RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Bluetooth Classic (RFCOMM/SPP) transport. Reaches only bonded devices - a BLE-only adapter has
 * no SPP record at all and is unreachable here; those go through [BleConnection].
 */
internal class BluetoothClassicConnection(private val deviceAddress: String) : AdapterConnection {

    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var socket: BluetoothSocket

    init {
        Log.i(LOGGER_TAG, "Created instance of BluetoothClassicConnection with devices: $deviceAddress")
    }

    override fun reconnect() {
        Log.i(LOGGER_TAG, "Reconnecting to the device: $deviceAddress")
        close()
        TimeUnit.MILLISECONDS.sleep(1000)
        connectToDevice()
        Log.i(LOGGER_TAG, "Successfully reconnect to the device: $deviceAddress")
    }

    override fun connect() {
        connectToDevice()
    }

    override fun close() {
        input.closeQuietly()
        output.closeQuietly()

        if (::socket.isInitialized) {
            socket.closeQuietly()
        }

        Log.i(LOGGER_TAG, "Socket for the device: $deviceAddress is closed.")
    }

    override fun openOutputStream(): OutputStream? {
        return output
    }

    override fun openInputStream(): InputStream? {
        return input
    }

    private fun connectToDevice() {
        try {
            Log.i(
                LOGGER_TAG,
                "Found bounded connections, size: ${Network.bluetoothAdapter()?.bondedDevices?.size}"
            )

            resolveBluetoothDevice(deviceAddress, requireBonded = true)?.let { adapter ->
                Log.i(
                    LOGGER_TAG,
                    "Opening connection to bounded device: ${adapter.name}"
                )
                socket =
                    adapter.createRfcommSocketToServiceRecord(RFCOMM_UUID)
                socket.connect()
                Log.i(LOGGER_TAG, "Doing socket connect for: ${adapter.name}")

                if (socket.isConnected) {
                    Log.i(
                        LOGGER_TAG,
                        "Successfully established connection for: ${adapter.name}"
                    )
                    input = socket.inputStream
                    output = socket.outputStream
                    Log.i(
                        LOGGER_TAG,
                        "Successfully opened  the sockets to device: ${adapter.name}"
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
        }
    }
}
