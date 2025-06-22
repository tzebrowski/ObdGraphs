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
package org.obd.graphs.bl.datalogger

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.PREF_MODULE_LIST
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.isEnabled


private const val GENERIC_MODE = "Generic mode"
private const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"

data class DataLoggerPreferences(
    var individualQueryStrategyEnabled: Boolean = false,
    var debugLogging: Boolean = false,
    var connectionType: String = "bluetooth",
    var tcpHost: String = "192.168.0.10",
    var wifiSSID: String = "",
    var tcpPort: Int = 35000,
    var connectionTimeout: Int = 2000,
    var stnExtensionsEnabled: Boolean = false,
    var batchEnabled: Boolean = true,
    var batchStricValidationEnabled: Boolean = false,
    var otherModesBatchSize: Int? = null,
    var mode01BatchSize: Int? = null,
    var reconnectWhenError: Boolean = true,
    var adapterId: String = "OBDII",
    var commandFrequency: Long = 6,
    var dragRacingCommandFrequency: Long = 10,
    var initDelay: Long = 500,
    var mode: String = GENERIC_MODE,
    var generatorEnabled: Boolean = false,
    var adaptiveConnectionEnabled: Boolean = false,
    var resultsCacheEnabled: Boolean = false,
    var initProtocol: String = "AUTO",
    var maxReconnectNum: Int = 0,
    var resources: Set<String> = modules.getDefaultModules().keys,
    var vehicleMetadataReadingEnabled: Boolean = true,
    var vehicleCapabilitiesReadingEnabled: Boolean = true,
    var vehicleDTCReadingEnabled: Boolean = false,
    var vehicleDTCCleaningEnabled: Boolean = false,
    var responseLengthEnabled: Boolean = false,
    var gracefulStop: Boolean = true,
    var dumpRawConnectorResponse: Boolean = false,
    var delayAfterReset: Long = 0,
    var fuelTankSize: Int = 58,
    var vehicleStatusPanelEnabled: Boolean =  false,
    var vehicleStatusDisconnectWhenOff: Boolean = false,
    var gmeExtensionsEnabled: Boolean = false
)


class DataLoggerPreferencesManager {

    private inner class SharedPreferenceChangeListener :
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Key to update $key")
            }
            instance = update()
        }
    }

    private var strongReference: SharedPreferenceChangeListener = SharedPreferenceChangeListener()
    var instance: DataLoggerPreferences = DataLoggerPreferences()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        instance = update()
    }

    fun reload() {
        instance = update()
    }

    private fun update(): DataLoggerPreferences = instance.apply{
        connectionType = Prefs.getS(PREFERENCE_CONNECTION_TYPE, "bluetooth")

        connectionTimeout = try {
            Prefs.getS("pref.adapter.connection.timeout", "2000").toInt()
        } catch (e: Exception) {
            2000
        }

        stnExtensionsEnabled = Prefs.getBoolean("pref.adapter.stn.enabled", false)

        individualQueryStrategyEnabled = Prefs.getBoolean("pref.adapter.query.individual.enabled", false)

        tcpHost = Prefs.getS("pref.adapter.connection.tcp.host", "192.168.0.10")

        tcpPort = try {
            Prefs.getS("pref.adapter.connection.tcp.port", "35000").toInt()
        } catch (e: Exception) {
            35000
        }

        wifiSSID = Prefs.getS("pref.adapter.connection.tcp.ssid", "")

        otherModesBatchSize = Prefs.getString("pref.adapter.batch.size", null)?.toInt()
        mode01BatchSize = Prefs.getString("pref.adapter.batch_01.size", null)?.toInt()
        batchEnabled = Prefs.getBoolean("pref.adapter.batch.enabled", true)
        batchStricValidationEnabled = Prefs.getBoolean("pref.adapter.batch.strict_validation.enabled", false)

        debugLogging = Prefs.getBoolean("pref.debug.logging.enabled", false)

        reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
        adapterId = Prefs.getS("pref.adapter.id", "OBDII")
        commandFrequency = Prefs.getS("pref.adapter.command.freq", "6").toLong()
        dragRacingCommandFrequency = Prefs.getS("pref.drag_race.vehicle_speed.freq", "10").toLong()

        initDelay = Prefs.getS("pref.adapter.init.delay", "500").toLong()
        delayAfterReset = Prefs.getS("pref.adapter.init.delay_after_reset", "0").toLong()

        mode = Prefs.getS("pref.mode", GENERIC_MODE)
        generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
        adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
        resultsCacheEnabled = Prefs.isEnabled("pref.adapter.cache.result.enabled")

        initProtocol = Prefs.getS("pref.adapter.init.protocol", "AUTO")

        maxReconnectNum = try {
            Prefs.getS("pref.adapter.reconnect.max_retry", "0").toInt()
        } catch (e: Exception) {
            0
        }

        vehicleMetadataReadingEnabled =
            Prefs.getBoolean("pref.adapter.init.fetchDeviceProperties", true)

        vehicleCapabilitiesReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchSupportedPids", true)
        vehicleDTCReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchDTC", false)
        vehicleDTCCleaningEnabled = Prefs.getBoolean("pref.adapter.init.cleanDTC", false)

        responseLengthEnabled = Prefs.getBoolean("pref.adapter.responseLength.enabled", false)

        gracefulStop = Prefs.getBoolean("pref.adapter.graceful_stop.enabled", true)
        dumpRawConnectorResponse = Prefs.getBoolean("pref.debug.trip.save.connector_response", false)

        resources  = Prefs.getStringSet(PREF_MODULE_LIST, modules.getDefaultModules().keys)!!

        fuelTankSize = Prefs.getS("pref.vehicle_settings.fuelTankSize", "58").toInt()
        vehicleStatusPanelEnabled = Prefs.getBoolean("pref.vehicle_settings.vehicle_status_panel_enabled", false)
        vehicleStatusDisconnectWhenOff = Prefs.getBoolean("pref.vehicle_settings.disconnect_when_off", false)
        gmeExtensionsEnabled = Prefs.getBoolean( "pref.profile.2_0_GME_extension.enabled", false)

        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.v(LOG_TAG, "Loaded data-logger preferences: $dataLoggerPreferences")
        }
    }
}

val dataLoggerPreferences by lazy { DataLoggerPreferencesManager() }
