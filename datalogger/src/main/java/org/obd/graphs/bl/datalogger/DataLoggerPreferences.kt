/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.bl.datalogger

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.isEnabled


const val GENERIC_MODE = "Generic mode"
private const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"


data class DataLoggerPreferences(
    var queryForEachViewStrategyEnabled: Boolean,
    var debugLogging: Boolean,
    var connectionType: String,
    var tcpHost: String,
    var wifiSSID: String,
    var tcpPort: Int,
    var connectionTimeout: Int,
    var stnExtensionsEnabled: Boolean,
    var batchEnabled: Boolean,
    var mode22BatchSize: Int?,
    var mode01BatchSize: Int?,
    var reconnectWhenError: Boolean,
    var adapterId: String,
    var commandFrequency: Long,
    var dragRacingCommandFrequency: Long,
    var initDelay: Long,
    var mode: String,
    var generatorEnabled: Boolean,
    var adaptiveConnectionEnabled: Boolean,
    var resultsCacheEnabled: Boolean,
    var initProtocol: String,
    var maxReconnectNum: Int,
    var resources: Set<String>,
    var vehicleMetadataReadingEnabled: Boolean,
    var vehicleCapabilitiesReadingEnabled: Boolean,
    var vehicleDTCReadingEnabled: Boolean,
    var vehicleDTCCleaningEnabled: Boolean,
    var responseLengthEnabled: Boolean,
    var gracefulStop: Boolean,
    var dumpRawConnectorResponse: Boolean,
    var delayAfterReset: Long,
    )

class DataLoggerPreferencesManager {

    private inner class SharedPreferenceChangeListener :
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (Log.isLoggable(LOG_TAG,Log.VERBOSE)) {
                Log.v(LOG_TAG, "Key to update $key")
            }
            instance = loadPreferences()
        }
    }

    private var strongReference: SharedPreferenceChangeListener = SharedPreferenceChangeListener()
    var instance: DataLoggerPreferences

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        instance =  loadPreferences()
    }

    fun reload (){
        instance = loadPreferences()
    }

    private fun loadPreferences(): DataLoggerPreferences {
        val connectionType = Prefs.getS(PREFERENCE_CONNECTION_TYPE, "bluetooth")
        val timeout = Prefs.getS("pref.adapter.connection.timeout", "2000").toInt()
        val stnEnabled = Prefs.getBoolean("pref.adapter.stn.enabled", false)

        val queryForEachViewStrategyEnabled = Prefs.getBoolean("pref.adapter.query.individual.enabled", false)

        val tcpHost = Prefs.getS("pref.adapter.connection.tcp.host", "192.168.0.10")
        val tcpPort = Prefs.getS("pref.adapter.connection.tcp.port", "35000").toInt()
        val wifiSSID = Prefs.getS("pref.adapter.connection.tcp.ssid", "")

        val mode22batchSize = Prefs.getString("pref.adapter.batch.size", null)
        val mode01batchSize = Prefs.getString("pref.adapter.batch_01.size", null)

        val debugLogging = Prefs.getBoolean("pref.debug.logging.enabled", false)

        val batchEnabled = Prefs.getBoolean("pref.adapter.batch.enabled", true)
        val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
        val adapterId = Prefs.getS("pref.adapter.id", "OBDII")
        val commandFrequency = Prefs.getS("pref.adapter.command.freq", "6").toLong()
        val dragRacingCommandFrequency = Prefs.getS("pref.drag_race.vehicle_speed.freq", "10").toLong()


        val initDelay = Prefs.getS("pref.adapter.init.delay", "500").toLong()
        val delayAfterReset = Prefs.getS("pref.adapter.init.delay_after_reset", "0").toLong()

        val mode = Prefs.getS("pref.mode", GENERIC_MODE)
        val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
        val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
        val resultsCacheEnabled = Prefs.isEnabled("pref.adapter.cache.result.enabled")

        val initProtocol = Prefs.getS("pref.adapter.init.protocol", "AUTO")

        val maxReconnectNum =
            Prefs.getS("pref.adapter.reconnect.max_retry", "0").toInt()

        val vehicleMetadataReadingEnabled =
            Prefs.getBoolean("pref.adapter.init.fetchDeviceProperties", true)

        val vehicleCapabilitiesReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchSupportedPids", true)
        val vehicleDTCReadingEnabled = Prefs.getBoolean("pref.adapter.init.fetchDTC", false)
        val vehicleDTCCleaningEnabled = Prefs.getBoolean("pref.adapter.init.cleanDTC", false)

        val responseLength = Prefs.getBoolean("pref.adapter.responseLength.enabled", false)

        val gracefulStop = Prefs.getBoolean("pref.adapter.graceful_stop.enabled", true)


        val dumpRawConnectorResponse = Prefs.getBoolean("pref.debug.trip.save.connector_response", false)

        val dataLoggerPreferences = DataLoggerPreferences(
            dragRacingCommandFrequency = dragRacingCommandFrequency,
            mode22BatchSize = mode22batchSize?.toInt(),
            mode01BatchSize = mode01batchSize?.toInt(),
            stnExtensionsEnabled = stnEnabled,
            connectionType = connectionType,
            tcpHost = tcpHost,
            tcpPort = tcpPort,
            wifiSSID = wifiSSID,
            batchEnabled = batchEnabled,
            reconnectWhenError = reconnectWhenError,
            adapterId = adapterId,
            commandFrequency = commandFrequency,
            initDelay = initDelay,
            mode = mode,
            generatorEnabled = generatorEnabled,
            adaptiveConnectionEnabled = adaptiveConnectionEnabled,
            resultsCacheEnabled = resultsCacheEnabled,
            initProtocol = initProtocol,
            maxReconnectNum = maxReconnectNum,
            resources = resources(),
            vehicleMetadataReadingEnabled = vehicleMetadataReadingEnabled,
            vehicleCapabilitiesReadingEnabled = vehicleCapabilitiesReadingEnabled,
            vehicleDTCReadingEnabled = vehicleDTCReadingEnabled,
            vehicleDTCCleaningEnabled = vehicleDTCCleaningEnabled,
            responseLengthEnabled = responseLength,
            gracefulStop = gracefulStop,
            connectionTimeout = timeout,
            dumpRawConnectorResponse = dumpRawConnectorResponse,
            delayAfterReset = delayAfterReset,
            debugLogging = debugLogging,
            queryForEachViewStrategyEnabled = queryForEachViewStrategyEnabled
        )

        if (Log.isLoggable(LOG_TAG,Log.VERBOSE)) {
            Log.v(LOG_TAG, "Loaded data-logger preferences: $dataLoggerPreferences")
        }

        return dataLoggerPreferences
    }



    private fun resources(): MutableSet<String> =
        Prefs.getStringSet("pref.pids.registry.list", pidResources.getDefaultPidFiles().keys)!!
}

val dataLoggerPreferences  by lazy {  DataLoggerPreferencesManager() }