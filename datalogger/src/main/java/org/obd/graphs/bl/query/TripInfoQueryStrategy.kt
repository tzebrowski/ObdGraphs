/*
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

const val PREF_QUERY_TRIP_INFO_SELECTED = "pref.aa.trip_info.pids.selected"

// Which of the selected PIDs go to the bottom row. Unset means TRIP_INFO_DEFAULT_BOTTOM_PIDS;
// an empty set means the user removed them all.
const val PREF_QUERY_TRIP_INFO_BOTTOM = "pref.aa.trip_info.bottom.pids.selected"

// The bottom row Trip Info always had, kept as the default so existing setups look the same.
val TRIP_INFO_DEFAULT_BOTTOM_PIDS =
    listOf(
        Pid.INTAKE_PRESSURE_PID_ID.id,
        Pid.OIL_PRESSURE_PID_ID.id,
        Pid.ENGINE_TORQUE_PID_ID.id
    )

// Queried for the status panel and the dynamic selector theme, never drawn in the grid or bottom row.
// VEHICLE_STATUS is added to every query by QueryStrategyOrchestrator when the status panel is on.
val TRIP_INFO_STATUS_PIDS =
    setOf(
        Pid.AMBIENT_TEMP_PID_ID.id,
        Pid.ATM_PRESSURE_PID_ID.id,
        Pid.DYNAMIC_SELECTOR_PID_ID.id,
        Pid.VEHICLE_STATUS_PID_ID.id
    )

/**
 * The bottom row as the bottom row dialog shows it checked: only the PIDs it lists can be.
 * Saving the dialog unchanged must compare equal to this, otherwise a bottom PID that is not
 * currently selected for Trip Info (or the whole default row) would be dropped from the pref.
 *
 * @param persisted the bottom row pref, or null when never set.
 */
fun tripInfoBottomDialogSelection(
    persisted: Set<Long>?,
    listed: Collection<Long>
): Set<Long> = (persisted ?: TRIP_INFO_DEFAULT_BOTTOM_PIDS).filter { listed.contains(it) }.toSet()

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

    override fun getDefaultPIDs() = defaults

    override fun getPIDs() = Prefs.getLongSet(PREF_QUERY_TRIP_INFO_SELECTED).toMutableSet()
}
