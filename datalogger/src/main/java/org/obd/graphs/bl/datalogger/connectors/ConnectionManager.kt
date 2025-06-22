 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.util.Log
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_CONNECT_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_INCORRECT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_NOT_CONNECTED
import org.obd.graphs.bl.datalogger.GeneralPreferences
import org.obd.graphs.bl.datalogger.LOG_TAG
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.getContext
import org.obd.graphs.network
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.transport.AdapterConnection

internal class ConnectionManager {
    fun obtain(): AdapterConnection? =
        when (dataLoggerPreferences.instance.adapter.connectionType) {
            "wifi" -> wifiConnection()
            "bluetooth" -> bluetoothConnection()
            "usb" -> getContext()?.let { UsbConnection.of(context = it) }
            else -> {
                null
            }
        }

    private fun bluetoothConnection(): AdapterConnection? =
        try {
            val deviceName = dataLoggerPreferences.instance.adapter.adapterId
            Log.i(LOG_TAG, "Connecting Bluetooth Adapter: $deviceName ...")

            if (deviceName.isEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
                null
            } else {
                if (network.findBluetoothAdapterByName(deviceName) == null) {
                    Log.e(LOG_TAG, "Did not find Bluetooth Adapter: $deviceName")
                    sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
                    null
                } else {
                    BluetoothConnection(deviceName)
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error occurred during establishing the connection $e")
            sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
            null
        }

    private fun wifiConnection(preferences: GeneralPreferences = dataLoggerPreferences.instance): WifiConnection? {
        try {
            Log.i(
                LOG_TAG,
                "Creating TCP connection to: ${preferences.adapter.tcpHost}:${preferences.adapter.tcpPort}.",
            )

            Log.i(LOG_TAG, "Selected WIFI SSID in preferences: ${preferences.adapter.wifiSSID}")
            Log.i(LOG_TAG, "Current connected WIFI SSID ${network.currentSSID}")

            if (preferences.adapter.wifiSSID.isEmpty()) {
                Log.d(LOG_TAG, "Target WIFI SSID is not specified in the prefs section. Connecting to the default one.")
            } else if (network.currentSSID.isNullOrBlank()) {
                sendBroadcastEvent(DATA_LOGGER_WIFI_NOT_CONNECTED)
                return null
            } else if (preferences.adapter.wifiSSID != network.currentSSID) {
                Log.w(
                    LOG_TAG,
                    "Preferences selected WIFI SSID ${preferences.adapter.wifiSSID} " +
                        "is different than current connected ${network.currentSSID}",
                )
                sendBroadcastEvent(DATA_LOGGER_WIFI_INCORRECT)
                return null
            }
            return WifiConnection.of()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error occurred during establishing the connection $e")
            sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        }
        return null
    }
}
