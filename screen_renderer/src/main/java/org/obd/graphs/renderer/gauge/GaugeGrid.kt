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

import kotlin.math.max

// Neighbouring AA dials overlap by this much: their squares are wider than the arc they draw.
internal const val AA_DIAL_OVERLAP = 10f

/**
 * Where the Gauge screen puts its dials. Pure (no Canvas, no Context) so the hand-tuned AA layout
 * is pinned by tests. AA: at most two rows, the dials sized by count. Phone: a column grid of
 * `maxColumns`; landscape with one column shows a single dial over the full height.
 */
internal class GaugeGrid(
    private val isAA: Boolean,
    private val isLandscape: Boolean,
    private val count: Int,
    private val maxColumns: Int
) {
    val columns: Int =
        if (isAA) {
            when (count) {
                1 -> 1
                2 -> 2
                else -> max(count / 2, count - count / 2)
            }
        } else {
            if (count == 1) 1 else maxColumns
        }

    val rows: Int = if (columns <= 0) 0 else (count + columns - 1) / columns

    /** Offset of the first column from the area's left edge. */
    fun startX(areaWidth: Float): Float =
        if (isAA) {
            when (count) {
                1 -> areaWidth / 6f
                3, 4 -> areaWidth / 8f
                else -> 5f
            }
        } else {
            0f
        }

    fun gaugeWidth(
        cellWidth: Float,
        availableHeight: Float,
        itemMargin: Float
    ): Float =
        if (isAA) {
            cellWidth * aaWidthRatio()
        } else if (isLandscape && columns == 1) {
            availableHeight - (2 * itemMargin)
        } else {
            cellWidth - (2 * itemMargin)
        }

    fun rowHeight(
        availableHeight: Float,
        gaugeWidth: Float,
        itemMargin: Float
    ): Float =
        if (isAA) {
            availableHeight / 2f
        } else if (isLandscape && columns == 1) {
            availableHeight
        } else {
            gaugeWidth + (2 * itemMargin)
        }

    /** Phone cards (portrait, or landscape with several columns) share the free height. */
    val fillsHeight: Boolean = !isAA && !(isLandscape && columns == 1)

    /** Left edge of the dial at [index]. */
    fun left(
        index: Int,
        areaLeft: Float,
        areaWidth: Float,
        gaugeWidth: Float
    ): Float {
        val col = index % columns
        val startX = areaLeft + startX(areaWidth)
        if (isAA) return startX + col * (gaugeWidth - AA_DIAL_OVERLAP)

        val cellWidth = areaWidth / columns
        val currentCellWidth = if (columns == 1) areaWidth else cellWidth
        return startX + col * cellWidth + (currentCellWidth - gaugeWidth) / 2f
    }

    /** Top edge of the card at [index], before the dial is offset inside a taller card. */
    fun top(
        index: Int,
        topOffset: Float,
        rowHeight: Float,
        availableHeight: Float,
        gaugeWidth: Float,
        itemMargin: Float
    ): Float {
        val cellTop = topOffset + (index / columns) * rowHeight
        return when {
            isAA -> cellTop
            count == 1 && isLandscape -> cellTop + (availableHeight - gaugeWidth) / 2f
            else -> cellTop + itemMargin
        }
    }

    private fun aaWidthRatio(): Float =
        when {
            count <= 1 -> 0.65f
            count == 2 -> 1.0f
            count <= 4 -> 0.75f
            else -> 1.02f
        }
}
