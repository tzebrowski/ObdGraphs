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
package org.obd.graphs.bl.datalogger

enum class PidId(val value: Long) {
    EXT_VEHICLE_SPEED_PID_ID(7046),
    EXT_ENGINE_SPEED_PID_ID(7008),
    DYNAMIC_SELECTOR_PID_ID(7036),
    ATM_PRESSURE_PID_ID(7021),
    AMBIENT_TEMP_PID_ID(7047),
    VEHICLE_SPEED_PID_ID(14),
    ENGINE_SPEED_PID_ID(13),
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
