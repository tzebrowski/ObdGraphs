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

class GaugeGeometryTest {
    private fun labels(scale: GaugeScale) = (0..scale.intervals).map { scale.label(it) }

    @Test
    fun `temperature scale uses round labels instead of thirds of the range`() {
        // Was -40, -6, 26, 60, 93, 126, 160.
        assertEquals(listOf("-40", "0", "40", "80", "120", "160"), labels(GaugeScale.of(-40.0, 160.0)))
    }

    @Test
    fun `gear scale has integer labels instead of 0,8 and 2,7`() {
        // Was -1.0, 0.8, 2.7, 4.5, 6.3, 8.2, 10.0; the start is extended to the nearest step.
        assertEquals(listOf("-2", "0", "2", "4", "6", "8", "10"), labels(GaugeScale.of(-1.0, 10.0)))
    }

    @Test
    fun `common ranges get round steps without extending the range`() {
        assertEquals(listOf("0", "20", "40", "60", "80", "100"), labels(GaugeScale.of(0.0, 100.0)))
        assertEquals(listOf("0", "2000", "4000", "6000", "8000"), labels(GaugeScale.of(0.0, 8000.0)))
        assertEquals(listOf("0", "50", "100", "150", "200", "250", "300"), labels(GaugeScale.of(0.0, 300.0)))
        assertEquals(listOf("0.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"), labels(GaugeScale.of(0.0, 3.0)))
    }

    @Test
    fun `scale always covers the PID range with 4 to 7 intervals`() {
        val ranges = listOf(-40.0 to 160.0, -1.0 to 10.0, 0.0 to 255.0, 0.0 to 65535.0, -0.5 to 0.5, 12.0 to 15.0, 0.0 to 1.0)
        ranges.forEach { (min, max) ->
            val scale = GaugeScale.of(min, max)
            assertTrue("$min..$max -> $scale", scale.min <= min && scale.max >= max)
            assertTrue("$min..$max -> $scale", scale.intervals in 4..7)
        }
    }

    @Test
    fun `no negative zero label`() {
        assertEquals("0", GaugeScale.of(-40.0, 160.0).label(1))
    }

    @Test
    fun `invalid range falls back to a single interval`() {
        val scale = GaugeScale.of(5.0, 5.0)

        assertEquals(1, scale.intervals)
        assertEquals(0f, scale.fraction(5.0))
    }

    @Test
    fun `value outside the scale sticks to its ends`() {
        // The progress arc used to be drawn past the end of the dial, or backwards below min.
        val scale = GaugeScale.of(0.0, 100.0)

        assertEquals(1f, scale.fraction(250.0))
        assertEquals(0f, scale.fraction(-30.0))
        assertEquals(0.5f, scale.fraction(50.0))
        assertEquals(0f, scale.fraction(Double.NaN))
    }

    @Test
    fun `red zones come from the alert thresholds only`() {
        val scale = GaugeScale.of(0.0, 100.0)

        assertTrue(GaugeRedZones.NONE.ranges(scale).isEmpty())
        assertEquals(listOf(0.8f..1f), GaugeRedZones(null, 80.0).ranges(scale))
        assertEquals(listOf(0f..0.2f, 0.9f..1f), GaugeRedZones(20.0, 90.0).ranges(scale))
        assertTrue(GaugeRedZones(null, 80.0).contains(80.0))
        assertFalse(GaugeRedZones(null, 80.0).contains(79.9))
    }

    @Test
    fun `thresholds beyond the scale draw no zone`() {
        assertTrue(GaugeRedZones(-10.0, 200.0).ranges(GaugeScale.of(0.0, 100.0)).isEmpty())
    }

    @Test
    fun `labels keep their radius unless they would reach into the ticks`() {
        val narrowAtSide = GaugeGeometry.labelCenterRadius(75f, 85f, 0.0, 10f, 8f)
        val wideAtSide = GaugeGeometry.labelCenterRadius(75f, 85f, 0.0, 40f, 8f)

        assertEquals(75f, narrowAtSide, 0.001f)
        assertEquals(65f, wideAtSide, 0.001f)
    }

    @Test
    fun `stats row stays clear of the end label when the dial ends below its centre`() {
        // 200 degree dial ends at 40 degrees, beside the stats row: "120" overlapped "max -29".
        val end = Math.toRadians(40.0)
        val limit = GaugeGeometry.statsMaxWidth(cardLimit = 300f, endAngleRadians = end, endLabelRadius = 100f, endLabelWidth = 30f, gap = 5f)

        assertEquals(2f * (100f * kotlin.math.cos(end).toFloat() - 15f - 5f), limit, 0.01f)
    }

    @Test
    fun `stats row uses the card when the dial ends at or above its centre`() {
        assertEquals(300f, GaugeGeometry.statsMaxWidth(300f, Math.toRadians(0.0), 100f, 30f, 5f))
        assertEquals(300f, GaugeGeometry.statsMaxWidth(300f, Math.toRadians(-20.0), 100f, 30f, 5f))
    }

    @Test
    fun `stats row is centred with equal gaps`() {
        val row = GaugeGeometry.statsRow(floatArrayOf(20f, 40f, 20f), gap = 10f, centerX = 100f, maxWidth = 500f)

        assertEquals(1f, row.scale)
        assertEquals(listOf(50f, 80f, 130f), row.lefts.toList())
    }

    @Test
    fun `stats row wider than the card is scaled down to fit`() {
        val row = GaugeGeometry.statsRow(floatArrayOf(100f, 100f), gap = 0f, centerX = 100f, maxWidth = 100f)

        assertEquals(0.5f, row.scale)
        assertEquals(listOf(50f, 100f), row.lefts.toList())
    }

    @Test
    fun `value text is only ever shrunk`() {
        assertEquals(1f, GaugeGeometry.fitScale(50f, 100f))
        assertEquals(0.5f, GaugeGeometry.fitScale(200f, 100f))
    }

    @Test
    fun `phone dial is centred on what it draws, not on its square`() {
        // 200 degree dial from 200: top of the circle to 40 degrees below the centre.
        val drawn = 50f * (1f + kotlin.math.sin(Math.toRadians(40.0)).toFloat())
        val offset = GaugeGeometry.dialTopOffset(cardHeight = 100f, gaugeWidth = 100f, startAngle = 200f, sweepAngle = 200f)

        assertEquals((100f - drawn) / 2f, offset, 0.01f)
    }

    @Test
    fun `upper half dial moves to the middle of a square card`() {
        assertEquals(25f, GaugeGeometry.dialTopOffset(100f, 100f, startAngle = 180f, sweepAngle = 180f), 0.01f)
    }

    @Test
    fun `full circle dial stays where it was`() {
        assertEquals(0f, GaugeGeometry.dialTopOffset(100f, 100f, startAngle = 0f, sweepAngle = 360f), 0.01f)
        assertEquals(25f, GaugeGeometry.dialTopOffset(150f, 100f, startAngle = 0f, sweepAngle = 360f), 0.01f)
    }

    @Test
    fun `cards fill the free height up to a limit, stay square when scrolling`() {
        // Four gauges in two columns used under half of a portrait phone screen.
        assertEquals(180f, GaugeGeometry.cardHeight(gaugeWidth = 140f, itemMargin = 6f, rows = 2, availableHeight = 384f))
        assertEquals(210f, GaugeGeometry.cardHeight(gaugeWidth = 140f, itemMargin = 6f, rows = 2, availableHeight = 1000f))
        assertEquals(140f, GaugeGeometry.cardHeight(gaugeWidth = 140f, itemMargin = 6f, rows = 5, availableHeight = 500f))
    }
}
