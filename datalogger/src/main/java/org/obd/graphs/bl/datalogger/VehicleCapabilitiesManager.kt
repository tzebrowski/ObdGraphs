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
package org.obd.graphs.bl.datalogger

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.graphs.bl.datalogger.dtc.DtcReadInfo
import org.obd.graphs.bl.datalogger.dtc.vinFrom
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.VehicleCapabilities

class VehicleMetadata(
    var name: String,
    var value: String
)

private const val PREF_VEHICLE_SUPPORTED_PIDS = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"
private const val PREF_DTC_SCANNED_MODULES = "pref.datalogger.dtc.scanned_modules"
private const val PREF_DTC_READ_INFO = "pref.datalogger.dtc.read_info"

object VehicleCapabilitiesManager {
    private val mapper =
        ObjectMapper().apply {
            registerModule(KotlinModule())
            configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    // moduleScanOnConnect: the codes come from the module scan scheduled after connecting, which
    // stores them itself - vehicleCapabilities.dtc is empty then and must not replace the list.
    internal fun updateCapabilities(
        vehicleCapabilities: VehicleCapabilities,
        moduleScanOnConnect: Boolean = false
    ) {
        Prefs.edit().apply {
            Log.i(
                LOG_TAG,
                "Property `vehicleCapabilitiesReadingEnabled` is " +
                    "`${dataLoggerSettings.instance().adapter.vehicleCapabilitiesReadingEnabled}`"
            )
            if (dataLoggerSettings.instance().adapter.vehicleCapabilitiesReadingEnabled) {
                if (vehicleCapabilities.capabilities.isEmpty()) {
                    Log.i(
                        LOG_TAG,
                        "Did not receive Vehicle Capabilities. Do not update preferences."
                    )
                } else {
                    Log.i(
                        LOG_TAG,
                        "Received Vehicle Capabilities. Updating preferences with=${vehicleCapabilities.capabilities}"
                    )
                    putStringSet(PREF_VEHICLE_SUPPORTED_PIDS, vehicleCapabilities.capabilities)
                }
            }

            putString(
                PREF_VEHICLE_METADATA,
                mapper.writeValueAsString(vehicleCapabilities.metadata)
            )
            commit()
        }
        if (dataLoggerSettings.instance().adapter.dtcEnabled && !moduleScanOnConnect) {
            updateDTC(vehicleCapabilities.dtc)
            if (vehicleCapabilities.dtc.isNotEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }
        }
    }

    // scannedModules are the module labels (DiagnosticTroubleCode.module) a user-triggered scan
    // targeted, kept so the DTC list can tell "scanned, nothing reported" apart from "not scanned".
    // Empty for the default single-ECU scan and for automatic reads.
    // Stamped with the time and the VIN of the vehicle connected now: metadata is rewritten on every
    // connect (updateCapabilities) before any DTC read of that connection completes.
    internal fun updateDTC(
        dtc: Set<DiagnosticTroubleCode>,
        scannedModules: List<String> = emptyList(),
        now: Long = System.currentTimeMillis()
    ) {
        val readInfo = DtcReadInfo(timestamp = now, vin = getCurrentVin())
        Prefs.edit().apply {
            Log.i(
                LOG_TAG,
                "Updating DTC, size: ${dtc.size}, scanned modules: $scannedModules, read info: $readInfo"
            )
            putString(PREF_DTC, mapper.writeValueAsString(dtc))
            putString(PREF_DTC_SCANNED_MODULES, mapper.writeValueAsString(scannedModules))
            putString(PREF_DTC_READ_INFO, mapper.writeValueAsString(readInfo))
            commit()
        }
    }

    // Null for codes stored before the read info existed: their age is unknown, not "now".
    fun getDtcReadInfo(): DtcReadInfo? =
        try {
            val preferences = Prefs.getString(PREF_DTC_READ_INFO, "")!!
            if (preferences.isEmpty()) null else mapper.readValue<DtcReadInfo>(preferences)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read DTC read info from preferences", e)
            null
        }

    // The VIN of the vehicle from the last connection's metadata, if it reported one.
    fun getCurrentVin(): String? = vinFrom(getVehicleMetadata().associate { it.name to it.value })

    fun getDtcScannedModules(): List<String> =
        try {
            var preferences = Prefs.getString(PREF_DTC_SCANNED_MODULES, "")!!
            if (preferences.startsWith("\"") && preferences.endsWith("\"")) {
                preferences = mapper.readValue<String>(preferences)
            }
            if (preferences.isEmpty()) emptyList() else mapper.readValue<List<String>>(preferences)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read DTC scanned modules from preferences", e)
            emptyList()
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
