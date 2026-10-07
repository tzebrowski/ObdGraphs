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
package org.obd.graphs.renderer.performance

import org.obd.graphs.bl.query.PERFORMANCE_MAX_GAUGES
import org.obd.graphs.bl.query.PERFORMANCE_STATUS_PIDS
import org.obd.graphs.renderer.trip.TripInfoGrid
import org.obd.graphs.renderer.trip.TripInfoMetrics

internal const val PERFORMANCE_GRID_COLUMNS = 5

// Three full rows always fitted above the gauges; more are shrunk into the same height.
private const val PERFORMANCE_GRID_ROWS = 3

internal class PerformancePlan(
    val top: List<Long>,
    val bottom: List<Long>
)

internal object PerformanceMetrics {
    /**
     * Splits the queried PIDs between the top grid and the gauges.
     *
     * @param profileTop the profile's grid PIDs, drawn first in their order.
     * @param profileBottom the profile's gauge PIDs, used when [bottomSelection] was never set.
     * @param hidden the profile's PIDs that are never drawn.
     * @param bottomSelection the user's gauges, or null when never set.
     */
    fun plan(
        available: Collection<Long>,
        profileTop: List<Long>,
        profileBottom: List<Long>,
        hidden: Set<Long>,
        bottomSelection: Set<Long>?,
        sortOrder: Map<Long, Int>?,
        bottomSortOrder: Map<Long, Int>?
    ): PerformancePlan {
        fun drawable(id: Long) = available.contains(id) && !hidden.contains(id) && !PERFORMANCE_STATUS_PIDS.contains(id)

        val bottom =
            if (bottomSelection == null) {
                profileBottom.filter { drawable(it) }
            } else {
                bottomSelection.filter { drawable(it) }.sortedWith(byOrder(bottomSortOrder))
            }.distinct().take(PERFORMANCE_MAX_GAUGES)

        // The profile's grid PIDs keep their place so existing layouts do not move; any other PID
        // follows in the order set in the PID dialog.
        val profileTopIds = profileTop.toSet()
        val top =
            profileTop.filter { drawable(it) && !bottom.contains(it) }.distinct() +
                available
                    .filter { drawable(it) && !bottom.contains(it) && !profileTopIds.contains(it) }
                    .distinct()
                    .sortedWith(byOrder(sortOrder))

        return PerformancePlan(top = top, bottom = bottom)
    }

    /** Up to 15 grid PIDs nothing changes; more shrink into the same three-row height. */
    fun grid(itemCount: Int): TripInfoGrid = TripInfoMetrics.grid(itemCount, PERFORMANCE_GRID_COLUMNS, PERFORMANCE_GRID_ROWS)

    private fun byOrder(sortOrder: Map<Long, Int>?): Comparator<Long> =
        compareBy<Long>({ sortOrder?.get(it) ?: Int.MAX_VALUE }, { it })
}
