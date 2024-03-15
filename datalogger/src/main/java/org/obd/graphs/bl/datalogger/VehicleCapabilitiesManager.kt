/**
 * Copyright 2019-2024, Tomasz Żebrowski
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

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.VehicleCapabilities

class VehicleMetadata(var name: String, var value: String)

private const val PREF_VEHICLE_CAPABILITIES = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"

class VehicleCapabilitiesManager {

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    }

    internal fun updateCapabilities(vehicleCapabilities: VehicleCapabilities) {

        Prefs.edit().apply {

            Log.i(
                LOG_TAG,
                "Property `vehicleCapabilitiesReadingEnabled` is " +
                        "`${dataLoggerPreferences.instance.vehicleCapabilitiesReadingEnabled}`"
            )
            if (dataLoggerPreferences.instance.vehicleCapabilitiesReadingEnabled) {
                if (vehicleCapabilities.capabilities.isEmpty()) {
                    Log.i(LOG_TAG, "Did not receive Vehicle Capabilities. Do not update preferences.")
                } else {
                    Log.i(LOG_TAG, "Received Vehicle Capabilities. Updating preferences with=${vehicleCapabilities.capabilities}")
                    putStringSet(PREF_VEHICLE_CAPABILITIES, vehicleCapabilities.capabilities)
                }
            }

            putString(PREF_VEHICLE_METADATA, mapper.writeValueAsString(vehicleCapabilities.metadata))
            putStringSet(PREF_DTC, vehicleCapabilities.dtc.map { it.code }.toHashSet())
            apply()
        }
    }

    fun getCapabilities(): MutableList<String> {
        val pidList = dataLogger.getPidDefinitionRegistry().findAll()
        return Prefs.getStringSet(PREF_VEHICLE_CAPABILITIES, emptySet())!!.toMutableList()
            .sortedWith(compareBy { t -> pidList.firstOrNull { a -> a.pid == t.uppercase() } }).toMutableList()
    }

    fun getDTC(): MutableList<String> {
        return Prefs.getStringSet(PREF_DTC, emptySet())!!.toMutableList()
    }

    fun getVehicleCapabilities(): MutableList<VehicleMetadata> {
        val preferences = Prefs.getString(PREF_VEHICLE_METADATA, "")!!

        try {
            return if (preferences.isEmpty()) mutableListOf() else {
                val map: Map<String, String> = mapper.readValue(preferences)
                return map.map { (k, v) -> VehicleMetadata(k, v) }.toMutableList()
            }
        } catch (e: Throwable){
            Log.e(LOG_TAG, "Failed to read vehicle capabilities from prefs: '${preferences}'",e)
        }
        return mutableListOf()
    }
}

val vehicleCapabilitiesManager = VehicleCapabilitiesManager()