package org.obd.graphs.query

import org.obd.metrics.api.model.ObdMetric


const val ATM_PRESSURE_PID_ID = 7021L
const val AMBIENT_TEMP_PID_ID = 7047L
const val MEASURED_INTAKE_PRESSURE_PID_ID = 7005L
const val VEHICLE_SPEED_PID_ID = 7046L
const val ENGINE_RPM_PID_ID = 7008L
const val DYNAMIC_SELECTOR_PID_ID = 7036L

fun ObdMetric.isAtmPressure(): Boolean =  command.pid.id == ATM_PRESSURE_PID_ID
fun ObdMetric.isAmbientTemp(): Boolean =  command.pid.id == AMBIENT_TEMP_PID_ID

fun ObdMetric.isDynamicSelector(): Boolean =  command.pid.id == DYNAMIC_SELECTOR_PID_ID
fun ObdMetric.isVehicleSpeed(): Boolean =  command.pid.id == VEHICLE_SPEED_PID_ID
fun ObdMetric.isEngineRpm(): Boolean =  command.pid.id == ENGINE_RPM_PID_ID