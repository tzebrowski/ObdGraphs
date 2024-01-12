package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric


fun ObdMetric.isAtmPressure(): Boolean =  command.pid.id == namesRegistry.getAtmPressurePID()
fun ObdMetric.isAmbientTemp(): Boolean =  command.pid.id == namesRegistry.getAmbientTempPID()

fun ObdMetric.isDynamicSelector(): Boolean =  command.pid.id == namesRegistry.getDynamicSelectorPID()
fun ObdMetric.isVehicleSpeed(): Boolean =  command.pid.id == namesRegistry.getVehicleSpeedPID()
fun ObdMetric.isEngineRpm(): Boolean =  command.pid.id == namesRegistry.getEngineRpmPID()

val namesRegistry = PIDsNamesRegistry()

const val PREF_PROFILE_2_0_GME_EXTENSION_ENABLED = "pref.profile.2_0_GME_extension.enabled"
private const val EXT_ATM_PRESSURE_PID_ID = 7021L
private const val EXT_AMBIENT_TEMP_PID_ID = 7047L
private const val EXT_MEASURED_INTAKE_PRESSURE_PID_ID = 7005L
private const val EXT_DYNAMIC_SELECTOR_PID_ID = 7036L

private const val EXT_VEHICLE_SPEED_PID_ID = 7046L
private const val VEHICLE_SPEED_PID_ID = 14L


private const val EXT_ENGINE_RPM_PID_ID = 7008L
private const val ENGINE_RPM_PID_ID = 13L

class PIDsNamesRegistry {

    fun getAtmPressurePID(): Long = EXT_ATM_PRESSURE_PID_ID
    fun getAmbientTempPID(): Long = EXT_AMBIENT_TEMP_PID_ID
    fun getMeasuredIntakePressurePID (): Long =  EXT_MEASURED_INTAKE_PRESSURE_PID_ID
    fun getVehicleSpeedPID (): Long = if (isProfileExtensionsEnabled()) EXT_VEHICLE_SPEED_PID_ID else VEHICLE_SPEED_PID_ID

    fun getEngineRpmPID (): Long = if (isProfileExtensionsEnabled())  EXT_ENGINE_RPM_PID_ID else ENGINE_RPM_PID_ID

    fun getDynamicSelectorPID(): Long = EXT_DYNAMIC_SELECTOR_PID_ID

    private fun isProfileExtensionsEnabled() = Prefs.getBoolean(PREF_PROFILE_2_0_GME_EXTENSION_ENABLED, false)

}