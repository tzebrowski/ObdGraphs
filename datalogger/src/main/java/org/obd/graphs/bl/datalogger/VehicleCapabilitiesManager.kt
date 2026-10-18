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
import org.obd.graphs.bl.datalogger.dtc.DtcHistoryStore
import org.obd.graphs.bl.datalogger.dtc.DtcNewCodesAnnouncer
import org.obd.graphs.bl.datalogger.dtc.DtcReadInfo
import org.obd.graphs.bl.datalogger.dtc.DtcScan
import org.obd.graphs.bl.datalogger.dtc.DtcScanKind
import org.obd.graphs.bl.datalogger.dtc.DtcStoredList
import org.obd.graphs.bl.datalogger.dtc.isSameVehicle
import org.obd.graphs.bl.datalogger.dtc.mergedOver
import org.obd.graphs.bl.datalogger.dtc.vinFrom
import org.obd.graphs.bl.datalogger.dtc.vinFromReply
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.VinPids
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.dtc.DtcModuleStatus
import org.obd.metrics.pid.PIDsGroup

class VehicleMetadata(
    var name: String,
    var value: String
)

private const val PREF_VEHICLE_SUPPORTED_PIDS = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"
private const val PREF_DTC_SCANNED_MODULES = "pref.datalogger.dtc.scanned_modules"
private const val PREF_DTC_READ_INFO = "pref.datalogger.dtc.read_info"
private const val PREF_DTC_MODULE_STATUS = "pref.datalogger.dtc.module_status"

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
            updateDTC(vehicleCapabilities.dtc, kind = DtcScanKind.CONNECT)
            if (vehicleCapabilities.dtc.isNotEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }
        }
    }

    // scannedModules are the module labels (DiagnosticTroubleCode.module) a user-triggered scan
    // targeted, kept so the DTC list can tell "scanned, nothing reported" apart from "not scanned".
    // Empty for the default single-ECU scan and for automatic reads.
    // Stamped with the time and the VIN: vin is the reply to the VIN request that opens every DTC action
    // (ObdMetrics), so a read works without the connect init's VIN read. Without a reply, the VIN of the vehicle
    // connected now: metadata is rewritten on every connect (updateCapabilities) before any DTC read of that
    // connection completes.
    // Also appended to the vehicle's history (DtcHistoryStore) with the same timestamp, which ties the
    // stored list to its history record. After a connect scan its codes are announced when new to the vehicle (DtcNewCodes).
    internal fun updateDTC(
        dtc: Set<DiagnosticTroubleCode>,
        scannedModules: List<String> = emptyList(),
        now: Long = System.currentTimeMillis(),
        kind: DtcScanKind = DtcScanKind.READ,
        moduleStatus: Map<String, DtcModuleStatus> = emptyMap(),
        vin: String? = null
    ) {
        val readInfo = DtcReadInfo(timestamp = now, vin = vinFromReply(vin) ?: getCurrentVin())
        // The list on screen keeps the modules this read did not target; history records this read alone.
        val previous = getDtcReadInfo()
        val list =
            DtcStoredList(dtc.toList(), scannedModules, moduleStatus).mergedOver(
                previous = DtcStoredList(getDiagnosticTroubleCodes(), getDtcScannedModules(), getDtcModuleStatus()),
                kind = kind,
                sameVehicle = isSameVehicle(previous, readInfo.vin)
            )
        Prefs.edit().apply {
            Log.i(
                LOG_TAG,
                "Updating DTC, size: ${dtc.size}, scanned modules: $scannedModules, read info: $readInfo, " +
                    "kept from the previous list: ${list.scannedModules - scannedModules.toSet()}"
            )
            putString(PREF_DTC, mapper.writeValueAsString(list.codes))
            putString(PREF_DTC_SCANNED_MODULES, mapper.writeValueAsString(list.scannedModules))
            putString(PREF_DTC_READ_INFO, mapper.writeValueAsString(readInfo))
            putString(PREF_DTC_MODULE_STATUS, mapper.writeValueAsString(list.moduleStatus))
            commit()
        }
        val firstScan = DtcHistoryStore.scans(readInfo.vin).isEmpty()
        DtcHistoryStore.append(readInfo.vin, DtcScan(now, kind, scannedModules, dtc.toList(), moduleStatus))
        DtcNewCodesAnnouncer.announce(kind, readInfo.vin, dtc, firstScan)
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

    // The VIN of the vehicle from the last connection's metadata, if it reported one; NO_VIN when the
    // VIN PID answered with an empty VIN.
    fun getCurrentVin(): String? = vinFrom(getVehicleMetadata().associate { it.name to it.value }, vinMetadataLabels())

    // Metadata is keyed by the PID description, so the VIN PIDs' descriptions tell which entry is the VIN
    // even when its value is empty. VIN PIDs are the ones marked "category": "vin" in the PID resource files
    // (ObdMetrics' VinPids, also used by its VIN reads).
    private fun vinMetadataLabels(): Set<String> =
        try {
            DataLoggerRepository
                .getPidDefinitionRegistry()
                .findBy(PIDsGroup.METADATA)
                .filter { VinPids.isVin(it) }
                .mapNotNull { it.description }
                .toSet()
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read VIN PID labels from the registry", e)
            emptySet()
        }

    // How each scanned module answered the stored read; empty for lists stored before it was recorded (= all answered).
    fun getDtcModuleStatus(): Map<String, DtcModuleStatus> =
        try {
            val preferences = Prefs.getString(PREF_DTC_MODULE_STATUS, "")!!
            if (preferences.isEmpty()) emptyMap() else mapper.readValue<Map<String, DtcModuleStatus>>(preferences)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to read DTC module status from preferences", e)
            emptyMap()
        }

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
