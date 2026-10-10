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
import org.obd.graphs.bl.query.PidOrder
import org.obd.graphs.renderer.gauge.GaugeGeometry
import org.obd.graphs.renderer.trip.TripInfoGrid
import org.obd.graphs.renderer.trip.TripInfoMetrics

internal const val PERFORMANCE_GRID_COLUMNS = 5

// At most three full rows above the gauges; more are shrunk into the same height.
private const val PERFORMANCE_GRID_ROWS = 3f
private const val MIN_GRID_ROWS = 1f

// Lower than Trip Info's 0.75: the grid shares the height with the gauges, and at 0.75 a 3-gauge
// row on a DHU left room for a single row of six.
private const val PERFORMANCE_MIN_GRID_SCALE = 0.6f

// The grid's height per row and its fixed parts, in text sizes, as PerformanceDrawer lays it out.
private const val GRID_ROW_HEIGHT = 2f
private const val GRID_GAUGE_OVERLAP = 0.7f
private const val GRID_TOP_PADDING = 2f

internal class PerformanceGaugeRow(
    val width: Float,
    // From the area's left edge to the first gauge.
    val left: Float
)

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
                bottomSelection.filter { drawable(it) }.sortedWith(PidOrder.comparator(bottomSortOrder, profileBottom))
            }.distinct().take(PERFORMANCE_MAX_GAUGES)

        // The profile's grid PIDs keep their place so existing layouts do not move; any other PID
        // follows in the order set in the PID dialog.
        val profileTopIds = profileTop.toSet()
        val top =
            profileTop.filter { drawable(it) && !bottom.contains(it) }.distinct() +
                available
                    .filter { drawable(it) && !bottom.contains(it) && !profileTopIds.contains(it) }
                    .distinct()
                    .sortedWith(PidOrder.comparator(sortOrder))

        return PerformancePlan(top = top, bottom = bottom)
    }

    /**
     * Width and start of the gauge row. A gauge is [availableWidth] / [count] wide (one gauge: half
     * the width), but never wider than fits [availableHeight]. The grid gives way first ([gridRows]);
     * this cap only bites when even a one-row grid leaves too little. A narrower row is centred.
     *
     * @param dialBottomRatio how far below its top a dial reaches, per width ([GaugeGeometry.dialBottomRatio]).
     */
    fun gaugeRow(
        count: Int,
        availableWidth: Float,
        availableHeight: Float,
        dialBottomRatio: Float
    ): PerformanceGaugeRow {
        val byWidth = if (count == 1) availableWidth / 2f else availableWidth / count.coerceAtLeast(1)
        val byHeight = if (dialBottomRatio > 0f) availableHeight.coerceAtLeast(0f) / dialBottomRatio else byWidth
        val width = minOf(byWidth, byHeight)
        return PerformanceGaugeRow(width, (availableWidth - width * count) / 2f)
    }

    /**
     * Grid rows (at full text size) that fit [height] above the gauges, between 1 and 3. The gauges
     * keep their size and the grid shrinks into what is left: three rows of grid plus full-size
     * gauges do not fit an 800 × 480 head unit.
     */
    fun gridRows(
        height: Float,
        textSize: Float
    ): Float =
        if (textSize <= 0f) {
            PERFORMANCE_GRID_ROWS
        } else {
            ((height - GRID_TOP_PADDING + GRID_GAUGE_OVERLAP * textSize) / (GRID_ROW_HEIGHT * textSize))
                .coerceIn(MIN_GRID_ROWS, PERFORMANCE_GRID_ROWS)
        }

    /** Up to 15 grid PIDs nothing changes when [rows] allows 3; more shrink into the same height. */
    fun grid(
        itemCount: Int,
        rows: Float = PERFORMANCE_GRID_ROWS
    ): TripInfoGrid = TripInfoMetrics.grid(itemCount, PERFORMANCE_GRID_COLUMNS, rows, PERFORMANCE_MIN_GRID_SCALE)
}
