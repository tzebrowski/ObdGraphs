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
package org.obd.graphs.renderer.trip

import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.query.TRIP_INFO_DEFAULT_BOTTOM_PIDS
import org.obd.graphs.bl.query.TRIP_INFO_STATUS_PIDS
import org.obd.graphs.renderer.PidOrder

internal const val MAX_BOTTOM_ITEMS = 4

private const val DEFAULT_GRID_ROWS = 3

// Below this the labels are unreadable on an 800 × 480 head unit.
private const val MIN_GRID_SCALE = 0.75f
private const val GRID_SCALE_STEP = 0.05f
private const val EPSILON = 0.001f

internal class TripMetricDescriptor(
    val id: Long,
    val castToInt: Boolean = false,
    val statsEnabled: Boolean = true,
    val unitEnabled: Boolean = true,
    val valueDoublePrecision: Int = 2,
    val statsDoublePrecision: Int = 2,
    // Draws the change since the trip started instead of the raw value (odometer).
    val diff: Boolean = false
)

internal class TripInfoPlan(
    val top: List<TripMetricDescriptor>,
    val bottom: List<TripMetricDescriptor>
)

internal class TripInfoGrid(
    val columns: Int,
    val maxItems: Int,
    val scale: Float,
    // Tiles drawn as metrics; when items do not fit, the last cell shows "+hidden" instead.
    val shown: Int,
    val hidden: Int
)

internal object TripInfoMetrics {
    // The top grid Trip Info always drew, in its order and with its formatting.
    private val defaultTop =
        listOf(
            TripMetricDescriptor(Pid.POST_IC_AIR_TEMP_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.COOLANT_TEMP_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.OIL_TEMP_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.EXHAUST_TEMP_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.GEARBOX_OIL_TEMP_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.DISTANCE_PID_ID.id, statsEnabled = false, diff = true),
            TripMetricDescriptor(Pid.FUEL_LEVEL_PID_ID.id, valueDoublePrecision = 1, statsDoublePrecision = 1),
            TripMetricDescriptor(Pid.FUEL_CONSUMPTION_PID_ID.id, unitEnabled = false, statsDoublePrecision = 1),
            TripMetricDescriptor(Pid.BATTERY_VOLTAGE_PID_ID.id),
            TripMetricDescriptor(Pid.IBS_PID_ID.id, castToInt = true),
            TripMetricDescriptor(Pid.OIL_LEVEL_PID_ID.id),
            TripMetricDescriptor(Pid.TOTAL_MISFIRES_PID_ID.id, castToInt = true, unitEnabled = false, statsEnabled = false),
            TripMetricDescriptor(Pid.OIL_DEGRADATION_PID_ID.id, unitEnabled = false),
            TripMetricDescriptor(Pid.ENGINE_SPEED_PID_ID.id, unitEnabled = false),
            TripMetricDescriptor(Pid.VEHICLE_SPEED_PID_ID.id, unitEnabled = false),
            TripMetricDescriptor(Pid.GEAR_ENGAGED_PID_ID.id, unitEnabled = false)
        )

    private val defaultTopById = defaultTop.associateBy { it.id }

    private val bottomCastToInt = setOf(Pid.INTAKE_PRESSURE_PID_ID.id, Pid.ENGINE_TORQUE_PID_ID.id)

    /**
     * Splits the queried PIDs between the top grid and the bottom row.
     *
     * @param bottomSelection the user's bottom row, or null when never set (the default row is used).
     */
    fun plan(
        available: Collection<Long>,
        bottomSelection: Set<Long>?,
        sortOrder: Map<Long, Int>?,
        bottomSortOrder: Map<Long, Int>?
    ): TripInfoPlan {
        val bottomIds =
            if (bottomSelection == null) {
                TRIP_INFO_DEFAULT_BOTTOM_PIDS.filter { available.contains(it) }
            } else {
                bottomSelection
                    .filter { available.contains(it) && !TRIP_INFO_STATUS_PIDS.contains(it) }
                    .sortedWith(PidOrder.comparator(bottomSortOrder, TRIP_INFO_DEFAULT_BOTTOM_PIDS))
            }.take(MAX_BOTTOM_ITEMS)

        // Default PIDs keep their fixed place so existing layouts do not move; any other PID
        // follows in the order set in the PID dialog.
        val top =
            defaultTop.filter { available.contains(it.id) && !bottomIds.contains(it.id) } +
                available
                    .filter { !bottomIds.contains(it) && !TRIP_INFO_STATUS_PIDS.contains(it) && !defaultTopById.containsKey(it) }
                    .sortedWith(PidOrder.comparator(sortOrder))
                    .map { TripMetricDescriptor(it) }

        return TripInfoPlan(
            top = top,
            // The bottom row draws no stats or units, but a diff PID (odometer) must still show the trip delta.
            bottom =
            bottomIds.map {
                TripMetricDescriptor(
                    it,
                    castToInt = bottomCastToInt.contains(it),
                    diff = defaultTopById[it]?.diff ?: false
                )
            }
        )
    }

    /**
     * Fits [itemCount] items into the height of a [baseRows]-row grid by shrinking the text and
     * adding columns. Up to [baseColumns] × [baseRows] items nothing changes (18 for Trip Info);
     * beyond the minimum scale the last cell becomes a "+N" marker for the items that are not drawn.
     * Performance reuses it with its own base grid.
     */
    fun grid(
        itemCount: Int,
        baseColumns: Int = MAX_ITEM_IN_THE_ROW,
        baseRows: Int = DEFAULT_GRID_ROWS
    ): TripInfoGrid {
        var scale = 1f
        while (true) {
            val columns = (baseColumns / scale + EPSILON).toInt()
            val maxItems = columns * (baseRows / scale + EPSILON).toInt()
            if (maxItems >= itemCount || scale - GRID_SCALE_STEP < MIN_GRID_SCALE - EPSILON) {
                val shown = if (itemCount > maxItems) maxItems - 1 else itemCount
                return TripInfoGrid(
                    columns = columns,
                    maxItems = maxItems,
                    scale = scale,
                    shown = shown,
                    hidden = maxOf(itemCount, 0) - shown
                )
            }
            scale -= GRID_SCALE_STEP
        }
    }
}

/**
 * The inputs besides the drawing area that change label geometry: the grid follows the top count,
 * the bottom row text size the width of the bottom labels and how they break.
 */
internal class TripInfoLabelLayout {
    private var topCount = -1
    private var bottomCount = -1
    private var bottomIds = LongArray(0)
    private var breakLabelTextEnabled: Boolean? = null

    // Called every frame, so it compares in place instead of building a key.
    fun requiresUpdate(
        topCount: Int,
        bottomCount: Int,
        bottom: List<TripInfoItem>,
        breakLabelTextEnabled: Boolean
    ): Boolean =
        this.topCount != topCount || this.bottomCount != bottomCount ||
            this.breakLabelTextEnabled != breakLabelTextEnabled || !sameIds(bottom)

    fun update(
        topCount: Int,
        bottomCount: Int,
        bottom: List<TripInfoItem>,
        breakLabelTextEnabled: Boolean
    ) {
        this.topCount = topCount
        this.bottomCount = bottomCount
        this.bottomIds = LongArray(bottom.size) { i -> bottom[i].descriptor.id }
        this.breakLabelTextEnabled = breakLabelTextEnabled
    }

    fun reset() {
        topCount = -1
        bottomCount = -1
        bottomIds = LongArray(0)
        breakLabelTextEnabled = null
    }

    private fun sameIds(bottom: List<TripInfoItem>): Boolean {
        if (bottomIds.size != bottom.size) return false
        for (i in bottom.indices) {
            if (bottomIds[i] != bottom[i].descriptor.id) return false
        }
        return true
    }
}
