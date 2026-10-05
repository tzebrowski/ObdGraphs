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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.bl.datalogger.Pid

private const val CUSTOM_PID_1 = 6001L
private const val CUSTOM_PID_2 = 6002L
private const val CUSTOM_PID_3 = 6003L

class TripInfoMetricsTest {
    // Everything the PID dialog used to offer for Trip Info.
    private val legacySelection =
        listOf(
            Pid.FUEL_CONSUMPTION_PID_ID, Pid.FUEL_LEVEL_PID_ID, Pid.ATM_PRESSURE_PID_ID, Pid.AMBIENT_TEMP_PID_ID,
            Pid.GEARBOX_OIL_TEMP_PID_ID, Pid.OIL_TEMP_PID_ID, Pid.COOLANT_TEMP_PID_ID, Pid.EXHAUST_TEMP_PID_ID,
            Pid.POST_IC_AIR_TEMP_PID_ID, Pid.TOTAL_MISFIRES_PID_ID, Pid.OIL_LEVEL_PID_ID, Pid.ENGINE_TORQUE_PID_ID,
            Pid.INTAKE_PRESSURE_PID_ID, Pid.DYNAMIC_SELECTOR_PID_ID, Pid.DISTANCE_PID_ID, Pid.BATTERY_VOLTAGE_PID_ID,
            Pid.IBS_PID_ID, Pid.OIL_PRESSURE_PID_ID, Pid.OIL_DEGRADATION_PID_ID, Pid.ENGINE_SPEED_PID_ID,
            Pid.VEHICLE_SPEED_PID_ID, Pid.GEAR_ENGAGED_PID_ID
        ).map { it.id }

    // The order the former hard-coded drawer used.
    private val legacyTop =
        listOf(
            Pid.POST_IC_AIR_TEMP_PID_ID, Pid.COOLANT_TEMP_PID_ID, Pid.OIL_TEMP_PID_ID, Pid.EXHAUST_TEMP_PID_ID,
            Pid.GEARBOX_OIL_TEMP_PID_ID, Pid.DISTANCE_PID_ID, Pid.FUEL_LEVEL_PID_ID, Pid.FUEL_CONSUMPTION_PID_ID,
            Pid.BATTERY_VOLTAGE_PID_ID, Pid.IBS_PID_ID, Pid.OIL_LEVEL_PID_ID, Pid.TOTAL_MISFIRES_PID_ID,
            Pid.OIL_DEGRADATION_PID_ID, Pid.ENGINE_SPEED_PID_ID, Pid.VEHICLE_SPEED_PID_ID, Pid.GEAR_ENGAGED_PID_ID
        ).map { it.id }

    private val legacyBottom =
        listOf(Pid.INTAKE_PRESSURE_PID_ID, Pid.OIL_PRESSURE_PID_ID, Pid.ENGINE_TORQUE_PID_ID).map { it.id }

    @Test
    fun `default PIDs keep the former layout when no bottom row was ever set`() {
        // Shuffled and with a stale drag order: neither may move the default PIDs.
        val plan =
            TripInfoMetrics.plan(
                available = legacySelection.shuffled(),
                bottomSelection = null,
                sortOrder = legacySelection.reversed().withIndex().associate { it.value to it.index },
                bottomSortOrder = null
            )

        assertEquals(legacyTop, plan.top.map { it.id })
        assertEquals(legacyBottom, plan.bottom.map { it.id })
    }

    @Test
    fun `default PIDs keep their former formatting`() {
        val plan = TripInfoMetrics.plan(legacySelection, null, null, null)
        val top = plan.top.associateBy { it.id }
        val bottom = plan.bottom.associateBy { it.id }

        val distance = top.getValue(Pid.DISTANCE_PID_ID.id)
        assertTrue(distance.diff)
        assertFalse(distance.statsEnabled)

        val misfires = top.getValue(Pid.TOTAL_MISFIRES_PID_ID.id)
        assertTrue(misfires.castToInt)
        assertFalse(misfires.unitEnabled)
        assertFalse(misfires.statsEnabled)

        val fuelLevel = top.getValue(Pid.FUEL_LEVEL_PID_ID.id)
        assertEquals(1, fuelLevel.valueDoublePrecision)
        assertEquals(1, fuelLevel.statsDoublePrecision)

        assertTrue(top.getValue(Pid.COOLANT_TEMP_PID_ID.id).castToInt)
        assertFalse(top.getValue(Pid.FUEL_CONSUMPTION_PID_ID.id).unitEnabled)

        assertTrue(bottom.getValue(Pid.INTAKE_PRESSURE_PID_ID.id).castToInt)
        assertFalse(bottom.getValue(Pid.OIL_PRESSURE_PID_ID.id).castToInt)
        assertTrue(bottom.getValue(Pid.ENGINE_TORQUE_PID_ID.id).castToInt)
    }

    @Test
    fun `status panel and theme PIDs are not drawn in the grid`() {
        val plan = TripInfoMetrics.plan(legacySelection, null, null, null)
        val ids = plan.top.map { it.id } + plan.bottom.map { it.id }

        assertFalse(ids.contains(Pid.AMBIENT_TEMP_PID_ID.id))
        assertFalse(ids.contains(Pid.ATM_PRESSURE_PID_ID.id))
        assertFalse(ids.contains(Pid.DYNAMIC_SELECTOR_PID_ID.id))
    }

    @Test
    fun `other PIDs follow the default ones in the dialog order`() {
        val plan =
            TripInfoMetrics.plan(
                available = listOf(CUSTOM_PID_1, Pid.COOLANT_TEMP_PID_ID.id, CUSTOM_PID_2, CUSTOM_PID_3),
                bottomSelection = null,
                sortOrder = mapOf(CUSTOM_PID_3 to 0, CUSTOM_PID_1 to 1),
                bottomSortOrder = null
            )

        // Unordered PIDs go last.
        assertEquals(listOf(Pid.COOLANT_TEMP_PID_ID.id, CUSTOM_PID_3, CUSTOM_PID_1, CUSTOM_PID_2), plan.top.map { it.id })
        assertTrue(plan.bottom.isEmpty())
    }

    @Test
    fun `other PIDs get the generic formatting`() {
        val custom = TripInfoMetrics.plan(listOf(CUSTOM_PID_1), null, null, null).top.single()

        assertFalse(custom.castToInt)
        assertTrue(custom.statsEnabled)
        assertTrue(custom.unitEnabled)
        assertFalse(custom.diff)
        assertEquals(2, custom.valueDoublePrecision)
    }

    @Test
    fun `selected bottom row is ordered, capped and the rest moves to the grid`() {
        val bottomSelection = setOf(CUSTOM_PID_1, CUSTOM_PID_2, CUSTOM_PID_3, Pid.OIL_TEMP_PID_ID.id, Pid.COOLANT_TEMP_PID_ID.id)
        val plan =
            TripInfoMetrics.plan(
                available = legacySelection + listOf(CUSTOM_PID_1, CUSTOM_PID_2, CUSTOM_PID_3),
                bottomSelection = bottomSelection,
                sortOrder = null,
                bottomSortOrder = mapOf(CUSTOM_PID_3 to 0, Pid.OIL_TEMP_PID_ID.id to 1, CUSTOM_PID_1 to 2, CUSTOM_PID_2 to 3)
            )

        assertEquals(MAX_BOTTOM_ITEMS, plan.bottom.size)
        assertEquals(listOf(CUSTOM_PID_3, Pid.OIL_TEMP_PID_ID.id, CUSTOM_PID_1, CUSTOM_PID_2), plan.bottom.map { it.id })

        val top = plan.top.map { it.id }
        assertTrue(top.contains(Pid.COOLANT_TEMP_PID_ID.id))
        assertFalse(top.contains(Pid.OIL_TEMP_PID_ID.id))
        // Default bottom PIDs no longer chosen for the bottom row are still shown.
        assertTrue(top.containsAll(legacyBottom))
    }

    @Test
    fun `empty bottom selection means no bottom row`() {
        val plan = TripInfoMetrics.plan(legacySelection, emptySet(), null, null)

        assertTrue(plan.bottom.isEmpty())
        assertTrue(plan.top.map { it.id }.containsAll(legacyBottom))
    }

    @Test
    fun `bottom PIDs that are not queried are skipped`() {
        val plan = TripInfoMetrics.plan(listOf(Pid.OIL_PRESSURE_PID_ID.id), null, null, null)

        assertEquals(listOf(Pid.OIL_PRESSURE_PID_ID.id), plan.bottom.map { it.id })
    }

    @Test
    fun `grid is unchanged up to three full rows`() {
        for (count in 0..18) {
            val grid = TripInfoMetrics.grid(count)
            assertEquals(MAX_ITEM_IN_THE_ROW, grid.columns)
            assertEquals(1f, grid.scale)
            assertTrue(grid.maxItems >= count)
        }
    }

    @Test
    fun `grid shrinks and adds columns to fit more items`() {
        for (count in 19..72) {
            val grid = TripInfoMetrics.grid(count)
            assertTrue("count=$count", grid.maxItems >= count)
            assertTrue("count=$count", grid.scale < 1f)
            assertTrue("count=$count", grid.columns > MAX_ITEM_IN_THE_ROW)
        }
    }

    @Test
    fun `grid stops shrinking at the minimum scale`() {
        val grid = TripInfoMetrics.grid(500)

        assertEquals(0.5f, grid.scale, 0.01f)
        assertEquals(12, grid.columns)
        assertEquals(72, grid.maxItems)
    }
}
