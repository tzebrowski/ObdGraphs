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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.query.performanceBottomDialogItems

private const val CUSTOM_PID_1 = 6001L
private const val CUSTOM_PID_2 = 6002L
private const val CUSTOM_PID_3 = 6003L

class PerformanceMetricsTest {
    // Bundled profile_8 (alfa_2_0_gme_aa.properties).
    private val profileSelection = listOf(7021L, 7047L, 7025L, 7003L, 7009L, 7016L, 7002L, 7028L, 7005L, 7036L, 7007L)
    private val profileTop =
        listOf(
            7021L, 7047L, 7025L, 7003L, 7009L, 7016L, 7002L, 7036L, 17079L, 7017L, 7029L,
            7026L, 7008L, 7041L, 17107L, 17108L, 7066L, 7067L, 7068L, 7069L, 17084L
        )
    private val profileBottom = listOf(7028L, 7005L, 7007L, 7046L, 17111L)
    private val profileHidden = setOf(7036L)

    // What the former MetricsCache drew: the profile lists, filtered by queried and hidden PIDs.
    private fun legacy(available: Collection<Long>) =
        Pair(
            profileTop.filter { available.contains(it) && !profileHidden.contains(it) },
            profileBottom.filter { available.contains(it) && !profileHidden.contains(it) }
        )

    private fun plan(
        available: Collection<Long>,
        bottomSelection: Set<Long>? = null,
        sortOrder: Map<Long, Int>? = null,
        bottomSortOrder: Map<Long, Int>? = null
    ) = PerformanceMetrics.plan(available, profileTop, profileBottom, profileHidden, bottomSelection, sortOrder, bottomSortOrder)

    @Test
    fun `profile PIDs keep the former layout when no gauges were ever set`() {
        // Shuffled and with a stale drag order: neither may move the profile's PIDs.
        val result =
            plan(
                available = profileSelection.shuffled(),
                sortOrder = profileSelection.reversed().withIndex().associate { it.value to it.index }
            )
        val (top, bottom) = legacy(profileSelection)

        assertEquals(top, result.top)
        assertEquals(bottom, result.bottom)
    }

    @Test
    fun `every profile PID selected keeps the former order`() {
        val all = profileTop + profileBottom
        val (top, bottom) = legacy(all)

        val result = plan(all)

        assertEquals(top, result.top)
        assertEquals(bottom, result.bottom)
    }

    @Test
    fun `selected PIDs outside the profile follow its grid PIDs in the dialog order`() {
        val result =
            plan(
                available = listOf(CUSTOM_PID_3, CUSTOM_PID_1, 7021L, CUSTOM_PID_2, 7047L),
                sortOrder = mapOf(CUSTOM_PID_2 to 0, CUSTOM_PID_1 to 1)
            )

        assertEquals(listOf(7021L, 7047L, CUSTOM_PID_2, CUSTOM_PID_1, CUSTOM_PID_3), result.top)
    }

    @Test
    fun `hidden PIDs are drawn nowhere`() {
        val result = plan(available = listOf(7036L, 7021L), bottomSelection = setOf(7036L))

        assertEquals(listOf(7021L), result.top)
        assertEquals(emptyList<Long>(), result.bottom)
    }

    @Test
    fun `vehicle status PID added to every query by the status panel is not drawn`() {
        val status = Pid.VEHICLE_STATUS_PID_ID.id
        val withStatus = profileSelection + status

        assertEquals(plan(profileSelection).top, plan(withStatus).top)
        assertEquals(emptyList<Long>(), plan(withStatus, bottomSelection = setOf(status)).bottom)
    }

    @Test
    fun `selected gauges are ordered, capped and the rest moves to the grid`() {
        val selection = listOf(7021L, 7047L, 7025L, 7003L, 7009L, 7016L, 7028L)
        val result =
            plan(
                available = profileSelection,
                bottomSelection = selection.toSet(),
                bottomSortOrder = selection.reversed().withIndex().associate { it.value to it.index }
            )

        assertEquals(listOf(7028L, 7016L, 7009L, 7003L, 7025L), result.bottom)
        // Not chosen as gauges: the capped ones and the profile's former gauges, profile grid PIDs first.
        assertEquals(listOf(7021L, 7047L, 7002L, 7005L, 7007L), result.top)
    }

    @Test
    fun `undragged gauges keep the profile order after one is removed`() {
        // Unchecking a gauge stores the rest as a set; without a drag order they must not jump to id order.
        val result = plan(available = profileSelection, bottomSelection = setOf(7007L, 7005L, 7028L))

        assertEquals(listOf(7028L, 7005L, 7007L), result.bottom)
    }

    @Test
    fun `dragged gauges follow the drag order, then the profile order`() {
        val result =
            plan(
                available = profileSelection + CUSTOM_PID_1,
                bottomSelection = setOf(7007L, 7005L, 7028L, CUSTOM_PID_1),
                bottomSortOrder = mapOf(7007L to 0)
            )

        assertEquals(listOf(7007L, 7028L, 7005L, CUSTOM_PID_1), result.bottom)
    }

    @Test
    fun `the order the gauge dialog stores on opening keeps the profile order`() {
        // The dialog stores its checked rows' order when first opened, and a stored order beats the profile one.
        val listed = performanceBottomDialogItems(profileSelection.reversed().toSet(), profileHidden, profileBottom)
        val gauges = setOf(7028L, 7005L, 7007L)
        val storedOrder = listed.filter { gauges.contains(it) }.withIndex().associate { it.value to it.index }

        val result = plan(available = profileSelection, bottomSelection = gauges, bottomSortOrder = storedOrder)

        assertEquals(listOf(7028L, 7005L, 7007L), result.bottom)
    }

    @Test
    fun `empty gauge selection means no gauges`() {
        val result = plan(available = profileSelection, bottomSelection = emptySet())

        assertEquals(emptyList<Long>(), result.bottom)
        assertTrue(result.top.containsAll(listOf(7028L, 7005L, 7007L)))
    }

    @Test
    fun `gauges that are not queried are skipped`() {
        val result = plan(available = listOf(7021L, 7005L), bottomSelection = setOf(7005L, 7028L))

        assertEquals(listOf(7005L), result.bottom)
    }

    @Test
    fun `grid is unchanged up to three full rows`() {
        for (count in 0..15) {
            val grid = PerformanceMetrics.grid(count)
            assertEquals(PERFORMANCE_GRID_COLUMNS, grid.columns)
            assertEquals(1f, grid.scale, 0.0001f)
            assertEquals(count, grid.shown)
        }
    }

    @Test
    fun `grid shrinks to fit every profile grid PID`() {
        val grid = PerformanceMetrics.grid(profileTop.size)

        assertTrue(grid.scale < 1f)
        assertTrue(grid.columns > PERFORMANCE_GRID_COLUMNS)
        assertEquals(profileTop.size, grid.shown)
        assertEquals(0, grid.hidden)
    }

    @Test
    fun `the hidden count marker always lands in the last column`() {
        // PerformanceDrawer draws "+N" at shown % columns, so it must be the last cell of a row.
        for (count in 0..200) {
            val grid = PerformanceMetrics.grid(count)
            if (grid.hidden > 0) {
                assertEquals("count=$count", grid.columns - 1, grid.shown % grid.columns)
            }
        }
    }

    @Test
    fun `items that do not fit are counted in the last cell`() {
        val grid = PerformanceMetrics.grid(100)

        assertEquals(grid.maxItems - 1, grid.shown)
        assertEquals(100 - grid.shown, grid.hidden)
    }
}
