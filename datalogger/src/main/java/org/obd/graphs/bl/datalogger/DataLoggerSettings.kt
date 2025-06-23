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

data class DataLoggerSettings(
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
    var gmeExtensionsEnabled: Boolean = false,
)

interface SettingsManager {
    fun reload()

    fun instance(): DataLoggerSettings
}

internal class DataLoggerSettingsManager : SettingsManager {
    private inner class SharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?,
        ) {
            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Key to update $key")
            }
            instance = update(key)
        }
    }

    private var strongReference: SharedPreferenceChangeListener = SharedPreferenceChangeListener()
    private var instance: DataLoggerSettings = DataLoggerSettings()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        reload()
    }

    override fun instance(): DataLoggerSettings = instance

    override fun reload() {
        update("pref.adapter.connection.type")
        update("pref.adapter.connection.timeout")
        update("pref.adapter.stn.enabled")
        update("pref.adapter.query.individual.enabled")
        update("pref.adapter.connection.tcp.host")
        update("pref.adapter.connection.tcp.port")
        update("pref.adapter.connection.tcp.ssid")
        update("pref.adapter.batch.size")
        update("pref.adapter.batch_01.size")
        update("pref.adapter.batch.enabled")
        update("pref.adapter.batch.strict_validation.enabled")
        update("pref.adapter.reconnect")
        update("pref.adapter.id")
        update("pref.adapter.command.freq")
        update("pref.adapter.init.delay")
        update("pref.adapter.init.delay_after_reset")
        update("pref.adapter.cache.result.enabled")
        update("pref.adapter.init.protocol")
        update("pref.adapter.reconnect.max_retry")
        update("pref.adapter.init.fetchSupportedPids")
        update("pref.adapter.init.fetchDTC")
        update("pref.adapter.init.cleanDTC")
        update("pref.adapter.responseLength.enabled")
        update("pref.adapter.init.fetchDeviceProperties")
        update("pref.adapter.graceful_stop.enabled")
        update("pref.adapter.adaptive.enabled")
        update("pref.debug.logging.enabled")
        update("pref.drag_race.vehicle_speed.freq")
        update("pref.mode")
        update("pref.debug.generator.enabled")
        update("pref.debug.trip.save.connector_response")
        update(PREF_MODULE_LIST)
        update("pref.vehicle_settings.fuelTankSize")
        update("pref.vehicle_settings.vehicle_status_panel_enabled")
        update("pref.vehicle_settings.disconnect_when_off")
        update("pref.profile.2_0_GME_extension.enabled")
    }

    private fun update(key: String?): DataLoggerSettings =
        instance.apply {
            Log.i(LOG_TAG,"Updating preference: $key")
            when (key) {
                "pref.adapter.connection.type" -> adapter.connectionType = Prefs.getS(key, "bluetooth")
                "pref.adapter.connection.timeout" -> {
                    adapter.connectionTimeout =
                        try {
                            Prefs.getS(key, "2000").toInt()
                        } catch (e: Exception) {
                            2000
                        }
                }

                "pref.adapter.stn.enabled" -> adapter.stnExtensionsEnabled = Prefs.getBoolean(key, false)
                "pref.adapter.query.individual.enabled" -> adapter.individualQueryStrategyEnabled = Prefs.getBoolean(key, false)
                "pref.adapter.connection.tcp.host" -> adapter.tcpHost = Prefs.getS(key, "192.168.0.10")
                "pref.adapter.connection.tcp.port" ->
                    adapter.tcpPort =
                        try {
                            Prefs.getS(key, "35000").toInt()
                        } catch (e: Exception) {
                            35000
                        }

                "pref.adapter.connection.tcp.ssid" -> adapter.wifiSSID = Prefs.getS(key, "")
                "pref.adapter.batch.size" -> adapter.otherModesBatchSize = Prefs.getString(key, null)?.toInt()
                "pref.adapter.batch_01.size" -> adapter.mode01BatchSize = Prefs.getString(key, null)?.toInt()
                "pref.adapter.batch.enabled" -> adapter.batchEnabled = Prefs.getBoolean(key, true)
                "pref.adapter.batch.strict_validation.enabled" -> adapter.batchStricValidationEnabled = Prefs.getBoolean(key, false)
                "pref.adapter.reconnect" -> adapter.reconnectWhenError = Prefs.getBoolean(key, true)
                "pref.adapter.id" -> adapter.adapterId = Prefs.getS(key, "OBDII")
                "pref.adapter.command.freq" -> adapter.commandFrequency = Prefs.getS(key, "6").toLong()
                "pref.adapter.init.delay" -> adapter.initDelay = Prefs.getS(key, "500").toLong()
                "pref.adapter.init.delay_after_reset" -> adapter.delayAfterReset = Prefs.getS(key, "0").toLong()
                "pref.adapter.cache.result.enabled" -> adapter.resultsCacheEnabled = Prefs.isEnabled(key)
                "pref.adapter.init.protocol" -> adapter.initProtocol = Prefs.getS(key, "AUTO")
                "pref.adapter.reconnect.max_retry" ->
                    adapter.maxReconnectNum =
                        try {
                            Prefs.getS(key, "0").toInt()
                        } catch (e: Exception) {
                            0
                        }

                "pref.adapter.init.fetchSupportedPids" -> adapter.vehicleCapabilitiesReadingEnabled = Prefs.getBoolean(key, true)
                "pref.adapter.init.fetchDTC" -> adapter.vehicleDTCReadingEnabled = Prefs.getBoolean(key, false)
                "pref.adapter.init.cleanDTC" -> adapter.vehicleDTCCleaningEnabled = Prefs.getBoolean(key, false)
                "pref.adapter.responseLength.enabled" -> adapter.responseLengthEnabled =  Prefs.getBoolean(key, false)
                "pref.adapter.init.fetchDeviceProperties" -> adapter.vehicleMetadataReadingEnabled = Prefs.getBoolean(key, true)
                "pref.adapter.graceful_stop.enabled" -> adapter.gracefulStop = Prefs.getBoolean(key, true)
                "pref.adapter.adaptive.enabled" -> adapter.adaptiveConnectionEnabled = Prefs.isEnabled(key)
                "pref.debug.logging.enabled" -> debugLogging = Prefs.getBoolean(key, false)
                "pref.drag_race.vehicle_speed.freq" -> dragRacingCommandFrequency = Prefs.getS(key, "10").toLong()
                "pref.mode" -> mode = Prefs.getS(key, "Generic mode")
                "pref.debug.generator.enabled" -> generatorEnabled = Prefs.isEnabled(key)
                "pref.debug.trip.save.connector_response" -> dumpRawConnectorResponse = Prefs.getBoolean(key, false)
                "pref.pids.registry.list" -> resources = Prefs.getStringSet(key, modules.getDefaultModules().keys)!!
                "pref.vehicle_settings.fuelTankSize" -> fuelTankSize = Prefs.getS(key, "58").toInt()
                "pref.vehicle_settings.vehicle_status_panel_enabled" -> vehicleStatusPanelEnabled = Prefs.getBoolean(key, false)
                "pref.vehicle_settings.disconnect_when_off" -> vehicleStatusDisconnectWhenOff = Prefs.getBoolean(key, false)
                "pref.profile.2_0_GME_extension.enabled" -> gmeExtensionsEnabled = Prefs.getBoolean(key, false)
                else -> {
                    Log.w(LOG_TAG, "Received unknown preference: $key")
                }
            }
        }


}

val dataLoggerSettings: SettingsManager by lazy { DataLoggerSettingsManager() }
