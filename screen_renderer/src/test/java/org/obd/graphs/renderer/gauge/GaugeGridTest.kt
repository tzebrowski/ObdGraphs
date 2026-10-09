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
package org.obd.graphs.renderer.gauge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pins the layout as it was hand-tuned in GaugeSurfaceRenderer before it moved here.
class GaugeGridTest {
    private fun aa(count: Int) = GaugeGrid(isAA = true, isLandscape = true, count = count, maxColumns = 3)

    @Test
    fun `AA uses at most two rows`() {
        assertEquals(listOf(1, 2, 2, 2, 3, 3, 4, 4), (1..8).map { aa(it).columns })
        assertEquals(listOf(1, 1, 2, 2, 2, 2, 2, 2), (1..8).map { aa(it).rows })
    }

    @Test
    fun `AA dial size and start depend on the count`() {
        // 800 px wide DHU, cell = width / columns.
        assertEquals(800f * 0.65f, aa(1).gaugeWidth(800f, 400f, 6f), 0.01f)
        assertEquals(400f, aa(2).gaugeWidth(400f, 400f, 6f), 0.01f)
        assertEquals(300f, aa(4).gaugeWidth(400f, 400f, 6f), 0.01f)
        assertEquals(266.67f * 1.02f, aa(6).gaugeWidth(266.67f, 400f, 6f), 0.01f)

        assertEquals(800f / 6f, aa(1).startX(800f), 0.01f)
        assertEquals(5f, aa(2).startX(800f), 0.01f)
        assertEquals(100f, aa(3).startX(800f), 0.01f)
        assertEquals(100f, aa(4).startX(800f), 0.01f)
        assertEquals(5f, aa(5).startX(800f), 0.01f)
    }

    @Test
    fun `AA dials overlap their neighbours and rows split the height`() {
        val grid = aa(4)
        assertEquals(100f + 300f - AA_DIAL_OVERLAP, grid.left(1, 0f, 800f, 300f), 0.01f)
        assertEquals(100f, grid.left(2, 0f, 800f, 300f), 0.01f)
        assertEquals(200f, grid.rowHeight(400f, 300f, 6f), 0.01f)
        assertEquals(240f, grid.top(2, 40f, 200f, 400f, 300f, 6f), 0.01f)
        assertFalse(grid.fillsHeight)
    }

    @Test
    fun `phone portrait centres each dial in its column and fills the height`() {
        val grid = GaugeGrid(isAA = false, isLandscape = false, count = 4, maxColumns = 2)

        assertEquals(2, grid.columns)
        assertEquals(2, grid.rows)
        assertEquals(188f, grid.gaugeWidth(200f, 800f, 6f), 0.01f)
        assertEquals(200f, grid.rowHeight(800f, 188f, 6f), 0.01f)
        assertEquals(206f, grid.left(1, 0f, 400f, 188f), 0.01f)
        assertEquals(10f + 200f + 6f, grid.top(2, 10f, 200f, 800f, 188f, 6f), 0.01f)
        assertTrue(grid.fillsHeight)
    }

    @Test
    fun `phone landscape single dial is sized by and centred in the height`() {
        val grid = GaugeGrid(isAA = false, isLandscape = true, count = 1, maxColumns = 3)

        assertEquals(1, grid.columns)
        assertEquals(388f, grid.gaugeWidth(800f, 400f, 6f), 0.01f)
        assertEquals(400f, grid.rowHeight(400f, 388f, 6f), 0.01f)
        assertEquals(206f, grid.left(0, 0f, 800f, 388f), 0.01f)
        assertEquals(6f, grid.top(0, 0f, 400f, 400f, 388f, 6f), 0.01f)
        assertFalse(grid.fillsHeight)
    }
}
