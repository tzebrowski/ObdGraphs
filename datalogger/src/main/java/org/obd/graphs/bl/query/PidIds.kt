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


import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric


fun ObdMetric.isAtmPressure(): Boolean = command.pid.id == PidId.EXT_ATM_PRESSURE_PID_ID.value
fun ObdMetric.isAmbientTemp(): Boolean = command.pid.id == PidId.EXT_AMBIENT_TEMP_PID_ID.value
fun ObdMetric.isVehicleStatus(): Boolean = command.pid.id == PidId.VEHICLE_STATUS_PID_ID.value
fun ObdMetric.isDynamicSelector(): Boolean = command.pid.id == PidId.EXT_DYNAMIC_SELECTOR_PID_ID.value
fun ObdMetric.isVehicleSpeed(): Boolean = command.pid.id ==
        (if (dataLoggerPreferences.instance.gmeExtensionsEnabled) PidId.EXT_VEHICLE_SPEED_PID_ID else PidId.VEHICLE_SPEED_PID_ID).value
fun ObdMetric.isEngineRpm(): Boolean = command.pid.id ==
        (if (dataLoggerPreferences.instance.gmeExtensionsEnabled) PidId.EXT_ENGINE_RPM_PID_ID else PidId.ENGINE_RPM_PID_ID).value

enum class PidId(val value: Long) {
    EXT_ATM_PRESSURE_PID_ID(7021),
    EXT_AMBIENT_TEMP_PID_ID(7047),
    EXT_MEASURED_INTAKE_PRESSURE_PID_ID(7005),
    EXT_DYNAMIC_SELECTOR_PID_ID(7036),

    EXT_VEHICLE_SPEED_PID_ID(7046),
    VEHICLE_SPEED_PID_ID(14),


    EXT_ENGINE_RPM_PID_ID(7008),
    ENGINE_RPM_PID_ID(13),

    FUEL_CONSUMPTION_PID_ID(7035),
    FUEL_LEVEL_PID_ID(7037),
    GEARBOX_OIL_TEMP_PID_ID(7025),
    GEAR_ENGAGED_PID_ID(7029),

    OIL_TEMP_PID_ID(7003),
    COOLANT_TEMP_PID_ID(7009),
    EXHAUST_TEMP_PID_ID(7016),
    POST_IC_AIR_TEMP_PID_ID(7002),
    TOTAL_MISFIRES_PID_ID(17078),
    OIL_LEVEL_PID_ID(7014),

    ENGINE_TORQUE_PID_ID(7028),
    INTAKE_PRESSURE_PID_ID(7005),
    DISTANCE_PID_ID(7076),

    IBS_PID_ID(7020),
    BATTERY_VOLTAGE_PID_ID(7019),
    OIL_PRESSURE_PID_ID(7018),
    GAS_PID_ID(7007),
    OIL_DEGRADATION_PID_ID(7015),

    VEHICLE_STATUS_PID_ID(17091),

    WCA_TEMP_PID_ID(17079),
    PRE_IC_AIR_TEMP_PID_ID(7017)
 }