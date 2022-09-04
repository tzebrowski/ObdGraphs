package org.obd.graphs.preferences

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.metrics.api.model.DeviceProperties

private const val ECU_SUPPORTED_PIDS = "pref.datalogger.supported.pids"
private const val VEHICLE_PROPERTIES = "pref.datalogger.vehicle.properties"

private var mapper = ObjectMapper().apply {
    registerModule(KotlinModule())
}

class VehicleProperty(var name: String, var value: String)

internal fun updateVehiclePreferences(deviceProperties: DeviceProperties) {
    Prefs.edit().putStringSet(ECU_SUPPORTED_PIDS, deviceProperties.capabilities).apply()
    Prefs.edit().putString(VEHICLE_PROPERTIES, mapper.writeValueAsString(deviceProperties.properties)).apply()
}

fun getECUSupportedPIDs(): MutableSet<String> {
    return Prefs.getStringSet(ECU_SUPPORTED_PIDS, emptySet())!!
}

fun getVehicleProperties(): List<VehicleProperty> {
    val it = Prefs.getString(VEHICLE_PROPERTIES, "")!!
    return if (it.isEmpty()) listOf() else {
        val map: Map<String,String> = mapper.readValue(it)
        return map.map { (k,v) -> VehicleProperty(k,v) }
    }
}

