package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.round
import org.obd.metrics.api.model.ObdMetric

fun isGMEExtensionsEnabled() = Prefs.getBoolean(PREF_PROFILE_2_0_GME_EXTENSION_ENABLED, false)


fun ObdMetric.valueToString(precision: Int = 2): String  =
    if (this.value == null) {
        "No data"
    } else {
        if (this.value is Double) value.toDouble().round(precision).toString() else this.value.toString()
    }

fun ObdMetric.isAtmPressure(): Boolean = command.pid.id == namesRegistry.getAtmPressurePID()
fun ObdMetric.isAmbientTemp(): Boolean = command.pid.id == namesRegistry.getAmbientTempPID()

fun ObdMetric.isDynamicSelector(): Boolean = command.pid.id == namesRegistry.getDynamicSelectorPID()
fun ObdMetric.isVehicleSpeed(): Boolean = command.pid.id == namesRegistry.getVehicleSpeedPID()
fun ObdMetric.isEngineRpm(): Boolean = command.pid.id == namesRegistry.getEngineRpmPID()

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

private const val FUEL_CONSUMPTION_PID_ID = 7035L
private const val FUEL_LEVEL_PID_ID = 7037L
private const val GEARBOX_OIL_TEMP_PID_ID = 7025L
private const val OIL_TEMP_PID_ID = 7003L
private const val COOLANT_TEMP_PID_ID = 7009L
private const val EXHAUST_TEMP_PID_ID = 7016L
private const val AIR_TEMP_PID_ID = 7002L
private const val TOTAL_MISFIRES_PID_ID = 17078L
private const val OIL_LEVEL_PID_ID = 7014L

private const val ENGINE_TORQUE_PID_ID = 7028L
private const val INTAKE_PRESSURE_PID_ID = 7005L
private const val DISTANCE_PID_ID = 7076L

private const val IBS_PID_ID = 7020L
private const val BATTERY_VOLTAGE_PID_ID = 7019L
private const val OIL_PRESSURE_PID_ID = 7018L


class PIDsNamesRegistry {

    fun getIbsPID(): Long = IBS_PID_ID
    fun getBatteryVoltagePID(): Long = BATTERY_VOLTAGE_PID_ID
    fun getOilPressurePID(): Long = OIL_PRESSURE_PID_ID

    fun getTotalMisfiresPID(): Long = TOTAL_MISFIRES_PID_ID
    fun getOilLevelPID(): Long = OIL_LEVEL_PID_ID

    fun getTorquePID(): Long = ENGINE_TORQUE_PID_ID

    fun getIntakePressurePID(): Long = INTAKE_PRESSURE_PID_ID

    fun getDistancePID(): Long = DISTANCE_PID_ID

    fun getFuelConsumptionPID(): Long = FUEL_CONSUMPTION_PID_ID
    fun getFuelLevelPID(): Long = FUEL_LEVEL_PID_ID
    fun getGearboxOilTempPID(): Long = GEARBOX_OIL_TEMP_PID_ID
    fun getOilTempPID(): Long = OIL_TEMP_PID_ID

    fun getCoolantTempPID(): Long = COOLANT_TEMP_PID_ID

    fun getExhaustTempPID(): Long = EXHAUST_TEMP_PID_ID

    fun getAirTempPID(): Long = AIR_TEMP_PID_ID

    fun getAtmPressurePID(): Long = EXT_ATM_PRESSURE_PID_ID
    fun getAmbientTempPID(): Long = EXT_AMBIENT_TEMP_PID_ID
    fun getMeasuredIntakePressurePID(): Long = EXT_MEASURED_INTAKE_PRESSURE_PID_ID
    fun getVehicleSpeedPID(): Long = if (isGMEExtensionsEnabled()) EXT_VEHICLE_SPEED_PID_ID else VEHICLE_SPEED_PID_ID

    fun getEngineRpmPID(): Long = if (isGMEExtensionsEnabled()) EXT_ENGINE_RPM_PID_ID else ENGINE_RPM_PID_ID

    fun getDynamicSelectorPID(): Long = EXT_DYNAMIC_SELECTOR_PID_ID
}