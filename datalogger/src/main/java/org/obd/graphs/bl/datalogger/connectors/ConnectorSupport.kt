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

import android.bluetooth.BluetoothDevice
import android.util.Log
import org.obd.graphs.Network
import java.io.Closeable

private const val LOGGER_TAG = "ConnectorSupport"

/**
 * Closing a transport happens on paths that are already failing - a broken socket, a dropped GATT
 * link - so a failure to close is never worth propagating over whatever brought us here.
 */
internal fun Closeable?.closeQuietly() {
    try {
        this?.close()
    } catch (_: Throwable) {
    }
}

/**
 * Resolves a device the same way for both Bluetooth transports, including the
 * SecurityException -> permission-request handling every call site used to repeat.
 *
 * [requireBonded] is what separates them: a Classic adapter has to be bonded for an RFCOMM socket
 * to open, while most BLE OBD dongles are never bonded at all, so BLE resolves the MAC directly.
 */
internal fun resolveBluetoothDevice(
    address: String,
    requireBonded: Boolean
): BluetoothDevice? =
    if (requireBonded) {
        Network.findBluetoothAdapterByName(address)
    } else {
        Network.bluetoothDeviceByAddress(address)
    }.also {
        if (it == null) {
            Log.w(LOGGER_TAG, "Did not resolve Bluetooth device: $address, requireBonded=$requireBonded")
        }
    }
