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

data class Adapter(
    var connectionType: String = "bluetooth",
    var connectionTimeout: Int = 2000,
    var stnExtensionsEnabled: Boolean = false,
    var individualQueryStrategyEnabled: Boolean = false,
    var tcpPort: Int = 35000,
    var wifiSSID: String = "",
    var otherModesBatchSize: Int? = null,
    var mode01BatchSize: Int? = null,
    var batchEnabled: Boolean = true,
    var batchStricValidationEnabled: Boolean = false,
    var tcpHost: String = "192.168.0.10",
    var reconnectWhenError: Boolean = true,
    var adapterId: String = "OBDII",
    var commandFrequency: Long = 6,
    var initDelay: Long = 500,
    var delayAfterReset: Long = 0,
    var resultsCacheEnabled: Boolean = false,
    var initProtocol: String = "AUTO",
    var maxReconnectNum: Int = 0,
    var vehicleMetadataReadingEnabled: Boolean = true,
    var vehicleCapabilitiesReadingEnabled: Boolean = true,
    var vehicleDTCReadingEnabled: Boolean = false,
    var vehicleDTCCleaningEnabled: Boolean = false,
    var responseLengthEnabled: Boolean = false,
    var gracefulStop: Boolean = true,
    var adaptiveConnectionEnabled: Boolean = false,
)

data class GeneralPreferences(
    var adapter: Adapter = Adapter(),
    var debugLogging: Boolean = false,
    var dragRacingCommandFrequency: Long = 10,
    var mode: String = "Generic mode",
    var generatorEnabled: Boolean = false,
    var resources: Set<String> = modules.getDefaultModules().keys,
    var dumpRawConnectorResponse: Boolean = false,
    var fuelTankSize: Int = 58,
    var vehicleStatusPanelEnabled: Boolean = false,
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
    var instance: GeneralPreferences = GeneralPreferences()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        instance = update()
    }

    fun reload() {
        instance = update()
    }

    private fun update(): GeneralPreferences = instance.apply {
        adapter.connectionType = Prefs.getS("pref.adapter.connection.type", "bluetooth")
        adapter.connectionTimeout = try {
            Prefs.getS("pref.adapter.connection.timeout", "2000").toInt()
        } catch (e: Exception) {
            2000
        }

        adapter.stnExtensionsEnabled = Prefs.getBoolean("pref.adapter.stn.enabled", false)
        adapter.individualQueryStrategyEnabled = Prefs.getBoolean("pref.adapter.query.individual.enabled", false)
        adapter.tcpHost = Prefs.getS("pref.adapter.connection.tcp.host", "192.168.0.10")

        adapter.tcpPort = try {
            Prefs.getS("pref.adapter.connection.tcp.port", "35000").toInt()
        } catch (e: Exception) {
            35000
        }

        adapter.wifiSSID = Prefs.getS("pref.adapter.connection.tcp.ssid", "")
        adapter.otherModesBatchSize = Prefs.getString("pref.adapter.batch.size", null)?.toInt()
        adapter.mode01BatchSize = Prefs.getString("pref.adapter.batch_01.size", null)?.toInt()
        adapter.batchEnabled = Prefs.getBoolean("pref.adapter.batch.enabled", true)
        adapter.batchStricValidationEnabled = Prefs.getBoolean("pref.adapter.batch.strict_validation.enabled", false)
        adapter.reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
        adapter.adapterId = Prefs.getS("pref.adapter.id", "OBDII")
        adapter.commandFrequency = Prefs.getS("pref.adapter.command.freq", "6").toLong()
        adapter.initDelay = Prefs.getS("pref.adapter.init.delay", "500").toLong()
        adapter.delayAfterReset = Prefs.getS("pref.adapter.init.delay_after_reset", "0").toLong()
        adapter.resultsCacheEnabled = Prefs.isEnabled("pref.adapter.cache.result.enabled")
        adapter.initProtocol = Prefs.getS("pref.adapter.init.protocol", "AUTO")

        adapter.maxReconnectNum = try {
            Prefs.getS("pref.adapter.reconnect.max_retry", "0").toInt()
        } catch (e: Exception) {
            0
        }

        adapter.vehicleCapabilitiesReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchSupportedPids", true)
        adapter.vehicleDTCReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchDTC", false)
        adapter.vehicleDTCCleaningEnabled = Prefs.getBoolean("pref.adapter.init.cleanDTC", false)
        adapter.responseLengthEnabled = Prefs.getBoolean("pref.adapter.responseLength.enabled", false)
        adapter.vehicleMetadataReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchDeviceProperties", true)
        adapter.gracefulStop = Prefs.getBoolean("pref.adapter.graceful_stop.enabled", true)
        adapter.adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")


        debugLogging = Prefs.getBoolean("pref.debug.logging.enabled", false)
        dragRacingCommandFrequency = Prefs.getS("pref.drag_race.vehicle_speed.freq", "10").toLong()
        mode = Prefs.getS("pref.mode", "Generic mode")
        generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
        dumpRawConnectorResponse = Prefs.getBoolean("pref.debug.trip.save.connector_response", false)
        resources = Prefs.getStringSet(PREF_MODULE_LIST, modules.getDefaultModules().keys)!!
        fuelTankSize = Prefs.getS("pref.vehicle_settings.fuelTankSize", "58").toInt()
        vehicleStatusPanelEnabled = Prefs.getBoolean("pref.vehicle_settings.vehicle_status_panel_enabled", false)
        vehicleStatusDisconnectWhenOff = Prefs.getBoolean("pref.vehicle_settings.disconnect_when_off", false)
        gmeExtensionsEnabled = Prefs.getBoolean("pref.profile.2_0_GME_extension.enabled", false)

        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.v(LOG_TAG, "Loaded data-logger preferences: $dataLoggerPreferences")
        }
    }
}

val dataLoggerPreferences by lazy { DataLoggerPreferencesManager() }
