package org.obd.graphs.preferences

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.metrics.api.model.VehicleCapabilities

class VehicleProperty(var name: String, var value: String)

private const val VEHICLE_CAPABILITIES = "pref.datalogger.supported.pids"
private const val VEHICLE_METADATA = "pref.datalogger.vehicle.properties"

class VehicleCapabilitiesManager {

    private var mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    internal fun updateCapabilities(vehicleCapabilities: VehicleCapabilities) {
        Prefs.edit().apply {
            putStringSet(VEHICLE_CAPABILITIES, vehicleCapabilities.capabilities)
            putString(VEHICLE_METADATA, mapper.writeValueAsString(vehicleCapabilities.metadata))
            apply()
        }
    }

    fun getCapabilities(): MutableSet<String> {
        return Prefs.getStringSet(VEHICLE_CAPABILITIES, emptySet())!!
    }

    fun getVehicleProperties(): List<VehicleProperty> {
        val it = Prefs.getString(VEHICLE_METADATA, "")!!
        return if (it.isEmpty()) listOf() else {
            val map: Map<String,String> = mapper.readValue(it)
            return map.map { (k,v) -> VehicleProperty(k,v) }
        }
    }
}

val vehicleCapabilitiesManager =  VehicleCapabilitiesManager()





