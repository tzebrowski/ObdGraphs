package org.obd.graphs.bl.datalogger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.VehicleCapabilities

class VehicleMetadata(var name: String, var value: String)

private const val PREF_VEHICLE_CAPABILITIES = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"

class PIDsGroupManager {

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    internal fun updateCapabilities(vehicleCapabilities: VehicleCapabilities) {
        Prefs.edit().apply {
            putStringSet(PREF_VEHICLE_CAPABILITIES, vehicleCapabilities.capabilities)
            putString(PREF_VEHICLE_METADATA, mapper.writeValueAsString(vehicleCapabilities.metadata))
            putStringSet(PREF_DTC, vehicleCapabilities.dtc.map { it.code }.toHashSet())
            apply()
        }
    }

    fun getCapabilities(): MutableList<String> {
        val pidList = dataLogger.pidDefinitionRegistry().findAll()
        return Prefs.getStringSet(PREF_VEHICLE_CAPABILITIES, emptySet())!!.toMutableList()
            .sortedWith(compareBy{t -> pidList.firstOrNull { a -> a.pid == t.uppercase() } }).toMutableList()
    }

    fun getDTC(): MutableList<String> {
        return Prefs.getStringSet(PREF_DTC, emptySet())!!.toMutableList()
    }

    fun getVehicleCapabilities(): MutableList<VehicleMetadata> {
        val it = Prefs.getString(PREF_VEHICLE_METADATA, "")!!
        return if (it.isEmpty()) mutableListOf() else {
            val map: Map<String,String> = mapper.readValue(it)
            return map.map { (k,v) -> VehicleMetadata(k,v) }.toMutableList()
        }
    }
}

val vehicleCapabilitiesManager =  PIDsGroupManager()





