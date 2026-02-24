 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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

import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet

private const val TRIP_INFO_QUERY_PREF_KEY = "pref.aa.trip_info.pids.selected"

internal class TripInfoQueryStrategy : QueryStrategy() {
    private val defaults =
        setOf(
            Pid.FUEL_CONSUMPTION_PID_ID.id,
            Pid.FUEL_LEVEL_PID_ID.id,
            Pid.ATM_PRESSURE_PID_ID.id,
            Pid.AMBIENT_TEMP_PID_ID.id,
            Pid.GEARBOX_OIL_TEMP_PID_ID.id,
            Pid.OIL_TEMP_PID_ID.id,
            Pid.COOLANT_TEMP_PID_ID.id,
            Pid.EXHAUST_TEMP_PID_ID.id,
            Pid.POST_IC_AIR_TEMP_PID_ID.id,
            Pid.TOTAL_MISFIRES_PID_ID.id,
            Pid.OIL_LEVEL_PID_ID.id,
            Pid.ENGINE_TORQUE_PID_ID.id,
            Pid.INTAKE_PRESSURE_PID_ID.id,
            Pid.DYNAMIC_SELECTOR_PID_ID.id,
            Pid.DISTANCE_PID_ID.id,
            Pid.BATTERY_VOLTAGE_PID_ID.id,
            Pid.IBS_PID_ID.id,
            Pid.OIL_PRESSURE_PID_ID.id,
            Pid.OIL_DEGRADATION_PID_ID.id,
            Pid.ENGINE_SPEED_PID_ID.id,
            Pid.VEHICLE_SPEED_PID_ID.id,
            Pid.GEAR_ENGAGED_PID_ID.id
            ).toSet()

    override fun getDefaults() = defaults

    override fun getPIDs() = Prefs.getLongSet(TRIP_INFO_QUERY_PREF_KEY).toMutableSet()
}
