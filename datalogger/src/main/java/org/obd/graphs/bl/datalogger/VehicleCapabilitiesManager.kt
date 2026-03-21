/**
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
package org.obd.graphs.bl.datalogger

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.VehicleCapabilities

class VehicleMetadata(
    var name: String,
    var value: String,
)

private const val PREF_VEHICLE_SUPPORTED_PIDS = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"

object VehicleCapabilitiesManager {
    private val mapper =
        ObjectMapper().apply {
            registerModule(KotlinModule())
            configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    internal fun updateCapabilities(vehicleCapabilities: VehicleCapabilities) {
        Prefs.edit().apply {
            Log.i(
                LOG_TAG,
                "Property `vehicleCapabilitiesReadingEnabled` is " +
                        "`${dataLoggerSettings.instance().adapter.vehicleCapabilitiesReadingEnabled}`",
            )
            if (dataLoggerSettings.instance().adapter.vehicleCapabilitiesReadingEnabled) {
                if (vehicleCapabilities.capabilities.isEmpty()) {
                    Log.i(
                        LOG_TAG,
                        "Did not receive Vehicle Capabilities. Do not update preferences.",
                    )
                } else {
                    Log.i(
                        LOG_TAG,
                        "Received Vehicle Capabilities. Updating preferences with=${vehicleCapabilities.capabilities}",
                    )
                    putStringSet(PREF_VEHICLE_SUPPORTED_PIDS, vehicleCapabilities.capabilities)
                }
            }

            putString(
                PREF_VEHICLE_METADATA,
                mapper.writeValueAsString(vehicleCapabilities.metadata),
            )
            commit()
        }
        if (dataLoggerSettings.instance().adapter.dtcEnabled) {
            updateDTC(vehicleCapabilities.dtc)
            if (vehicleCapabilities.dtc.isNotEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }
        }
    }


    internal fun updateDTC(dtc: Set<DiagnosticTroubleCode>) {
        Prefs.edit().apply {
            Log.i(
                LOG_TAG,
                "Updating DTC, size: ${dtc.size}",
            )
            putString(PREF_DTC, mapper.writeValueAsString(dtc))
            commit()
        }
    }

    fun getSupportedPIDs(): MutableList<String> {
        val pidList = DataLoggerRepository.getPidDefinitionRegistry().findAll()
        return Prefs
            .getStringSet(PREF_VEHICLE_SUPPORTED_PIDS, emptySet())!!
            .toMutableList()
            .sortedWith(compareBy { t -> pidList.firstOrNull { a -> a.pid == t.uppercase() } })
            .toMutableList()
    }

    fun getDiagnosticTroubleCodes(): MutableList<DiagnosticTroubleCode> =
        try {
            var preferences = Prefs.getString(PREF_DTC, "")!!
            if (preferences.startsWith("\"") && preferences.endsWith("\"")) {
                preferences = mapper.readValue<String>(preferences)
            }
            mapper.readValue<List<DiagnosticTroubleCode>>(preferences).toMutableList()
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read Diagnostic Trouble Code from preferences", e)
            mutableListOf()
        }

    fun getVehicleMetadata(): MutableList<VehicleMetadata> =
        try {
            val preferences = Prefs.getString(PREF_VEHICLE_METADATA, "")!!
            if (preferences.isEmpty()) {
                mutableListOf()
            } else {
                val map: Map<String, String> = mapper.readValue(preferences)
                map.map { (k, v) -> VehicleMetadata(k, v) }.toMutableList()
            }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read vehicle capabilities from prefs", e)
            mutableListOf()
        }
}
