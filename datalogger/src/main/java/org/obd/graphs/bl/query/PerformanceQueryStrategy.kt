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

import android.util.Log
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongList
import org.obd.graphs.preferences.getLongSet

const val PREF_QUERY_PERFORMANCE_SELECTED = "pref.aa.performance.pids.selected"

// The profile's layout: grid PIDs, gauge PIDs and PIDs never drawn.
const val PREF_QUERY_PERFORMANCE_TOP = "pref.query.performance.top"
const val PREF_QUERY_PERFORMANCE_BOTTOM = "pref.query.performance.bottom"
const val PREF_QUERY_PERFORMANCE_HIDDEN = "pref.query.performance.hidden"

// Which of the selected PIDs are drawn as gauges. Unset means the profile's PREF_QUERY_PERFORMANCE_BOTTOM;
// an empty set means the user removed them all.
const val PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED = "pref.aa.performance.bottom.pids.selected"

// Added to every query by QueryStrategyOrchestrator when the status panel is on, never drawn.
val PERFORMANCE_STATUS_PIDS = setOf(Pid.VEHICLE_STATUS_PID_ID.id)

// Profiles define at most 5 gauges, so the cap leaves every existing layout as it was.
const val PERFORMANCE_MAX_GAUGES = 5

/** How many of [selectedCount] gauges do not fit and fall back into the grid. */
fun performanceGaugeOverflow(selectedCount: Int): Int = (selectedCount - PERFORMANCE_MAX_GAUGES).coerceAtLeast(0)

/**
 * The stored gauge selection limited to the PIDs still selected for the screen, or null when
 * nothing has to be written: the gauges were never set (the profile's apply) or none was dropped.
 * Without it a deselected gauge PID stayed stored and came back as a gauge once selected again.
 */
fun prunedPerformanceBottomSelection(
    stored: Set<Long>?,
    selected: Set<Long>
): Set<Long>? = stored?.filter { selected.contains(it) }?.toSet()?.takeIf { it.size != stored.size }

/**
 * The gauges as the gauge dialog shows them checked: the stored selection, or the profile's
 * gauges when never set, limited to the PIDs the dialog lists. See [tripInfoBottomDialogSelection].
 *
 * @param persisted the gauge pref, or null when never set.
 */
fun performanceBottomDialogSelection(
    persisted: Set<Long>?,
    listed: Collection<Long>
): Set<Long> = (persisted ?: Prefs.getLongList(PREF_QUERY_PERFORMANCE_BOTTOM)).filter { listed.contains(it) }.toSet()

const val PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_GAS_METRIC =
    "pref.query.performance.break_boosting.gas_pid"
const val PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_ARBITRARY_METRIC =
    "pref.query.performance.break_boosting.arbitrary_pid"

const val PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_VEHICLE_SPEED_METRIC =
    "pref.query.performance.break_boosting.vehicle_speed_pid"

internal class PerformanceQueryStrategy : QueryStrategy() {
    override fun getDefaultPIDs() =
        Prefs.getLongSet(PREF_QUERY_PERFORMANCE_TOP) +
            Prefs.getLongSet(PREF_QUERY_PERFORMANCE_BOTTOM) +
            Prefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_GAS_METRIC, -1).toLong() +
            Prefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_ARBITRARY_METRIC, -1).toLong() +
            Prefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_VEHICLE_SPEED_METRIC, -1).toLong()

    override fun getPIDs() = Prefs.getLongSet(PREF_QUERY_PERFORMANCE_SELECTED)

    init {
        Log.i("PerformanceQueryStrategy", "Read defaults=${getDefaultPIDs()}")
    }
}
