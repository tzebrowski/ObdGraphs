 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric

fun isGMEExtensionsEnabled() = Prefs.getBoolean(PREF_PROFILE_2_0_GME_EXTENSION_ENABLED, false)

fun ObdMetric.isAtmPressure(): Boolean = command.pid.id == namesRegistry.getAtmPressurePID()
fun ObdMetric.isAmbientTemp(): Boolean = command.pid.id == namesRegistry.getAmbientTempPID()

fun ObdMetric.isVehicleStatus(): Boolean = command.pid.id == namesRegistry.getVehicleStatusPID()

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
private const val GAS_PID_ID = 7007L
private const val OIL_DEGRADATION_PID_ID = 7015L

private const val VEHICLE_STATUS_PID_ID = 17091L

class PIDsNamesRegistry {

    fun getOilDegradationPID(): Long = OIL_DEGRADATION_PID_ID

    fun getGasPedalPID(): Long = GAS_PID_ID

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

    fun getVehicleStatusPID(): Long = VEHICLE_STATUS_PID_ID
}