 /**
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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.ScreenSettings
import kotlin.math.max
import kotlin.math.min

internal class GaugeSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private var lastAreaWidth = 0
    private var lastAreaHeight = 0
    private var lastItemCount = 0

    private var cachedColumns = 1
    private var cachedGaugeWidth = 0f
    private var cachedRowHeight = 0f
    private var cachedStartX = 0f
    private var cachedContentHeight = 0f
    private var cachedMaxScroll = 0f

    private val cachedBorderRects = mutableMapOf<Int, RectF>()
    private val cachedCenteredLefts = mutableMapOf<Int, Float>()
    private val cachedCenteredTops = mutableMapOf<Int, Float>()

    private val gaugeDrawer =
        GaugeDrawer(
            settings = settings,
            context = context,
            drawerSettings = DrawerSettings(gaugeProgressBarType = settings.getGaugeRendererSetting().gaugeProgressBarType),
        )

    private val mobileDrawer =
        GaugeDrawer(
            settings = settings,
            context = context,
            drawerSettings =
                DrawerSettings(
                    startAngle = 200f,
                    sweepAngle = 200f,
                    gaugeProgressBarType = settings.getGaugeRendererSetting().gaugeProgressBarType,
                ),
        )


    override fun getTop(area: Rect): Float =
        if (settings.isStatusPanelEnabled()) {
            area.top + getDefaultTopMargin()
        } else {
            area.top.toFloat()
        }

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
        drawArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            gaugeDrawer.drawBackground(canvas, area)

            var top = getTop(area)

            if (settings.isAA() && settings.isStatusPanelEnabled()) {
                val left = gaugeDrawer.getMarginLeft(area.left.toFloat())
                gaugeDrawer.drawStatusPanel(canvas, top, left, fps)
                top += MARGIN_TOP
                gaugeDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 10
            }

            drawGrid(canvas = canvas, area = area, topOffset = top)
        }
    }

    override fun recycle() {
        gaugeDrawer.recycle()
        cachedBorderRects.clear()
        cachedCenteredTops.clear()
        cachedCenteredLefts.clear()
    }

    private fun drawGrid(
        canvas: Canvas,
        area: Rect,
        topOffset: Float,
        metrics: List<Metric> = metricsCollector.getMetrics(),
        maxItems: Int = settings.getMaxItems(),
        drawScrollbar: Boolean = settings.isScrollbarEnabled(),
        drawMetricsRate: Boolean = settings.isFpsCounterEnabled(),
    ) {
        val count = min(metrics.size, maxItems)
        if (count <= 0) return

        val isAA = settings.isAA()
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        updateCacheIfNeeded(area, count, isAA, isLandscape, topOffset)

        scrollOffset = scrollOffset.coerceIn(0f, cachedMaxScroll)

        val viewportHeight = area.height().toFloat()

        val startRow =
            kotlin.math
                .floor(scrollOffset / cachedRowHeight)
                .toInt()
                .coerceAtLeast(0)
        val endRow =
            kotlin.math.floor((scrollOffset + viewportHeight - 1f) / cachedRowHeight).toInt().coerceAtMost(
                kotlin.math.ceil(count / cachedColumns.toDouble()).toInt() - 1,
            )

        val startIndex = startRow * cachedColumns
        val endIndex = min(count - 1, (endRow * cachedColumns) + (cachedColumns - 1))

        val drawer = if (isAA) gaugeDrawer else mobileDrawer
        val drawBorder = !isAA
        val labelCenterYPadding = if (isAA && count <= 2) 22f else 20f

        canvas.save()
        canvas.clipRect(area)
        canvas.translate(0f, -scrollOffset)

        for (i in startIndex..endIndex) {
            val metric = metrics[i]

            drawer.drawGauge(
                canvas = canvas,
                left = cachedCenteredLefts[i]!!,
                top = cachedCenteredTops[i]!!,
                width = cachedGaugeWidth,
                metric = metric,
                labelCenterYPadding = labelCenterYPadding,
                drawBorder = drawBorder,
                borderArea = cachedBorderRects[i]!!,
                drawModule = !isAA,
                drawMetricRate = drawMetricsRate,
            )
        }

        canvas.restore()

        if (drawScrollbar && cachedContentHeight > viewportHeight) {
            drawScrollbar(
                canvas = canvas,
                area = area,
                contentHeight = cachedContentHeight,
                viewportHeight = viewportHeight,
                topOffset = area.top.toFloat(),
                verticalMargin = 30f
            )
        }
    }



    private fun columns(
        isAA: Boolean,
        count: Int,
    ): Int {
        val columns =
            if (isAA) {
                when (count) {
                    1 -> 1
                    2 -> 2
                    else -> max(count / 2, count - count / 2)
                }
            } else {
                if (count == 1) 1 else settings.getMaxColumns()
            }
        return columns
    }

    private fun startX(
        area: Rect,
        isAA: Boolean,
        count: Int,
    ): Float =
        area.left +
            if (isAA) {
                when (count) {
                    1 -> area.width() / 6f
                    3, 4 -> area.width() / 8f
                    else -> 5f
                }
            } else {
                0f
            }

    private fun rowHeight(
        isAA: Boolean,
        availableHeight: Float,
        isLandscape: Boolean,
        columns: Int,
        gaugeWidth: Float,
        itemMargin: Float,
    ): Float =
        if (isAA) {
            availableHeight / 2f
        } else if (isLandscape && columns == 1) {
            availableHeight
        } else {
            gaugeWidth + (2 * itemMargin)
        }

    private fun gaugeWidth(
        isAA: Boolean,
        cellWidth: Float,
        count: Int,
        isLandscape: Boolean,
        columns: Int,
        availableHeight: Float,
        itemMargin: Float,
    ): Float =
        if (isAA) {
            cellWidth * widthScaleRatio(count)
        } else if (isLandscape) {
            if (columns == 1) availableHeight - (2 * itemMargin) else cellWidth - (2 * itemMargin)
        } else {
            cellWidth - (2 * itemMargin)
        }

    private fun widthScaleRatio(maxItems: Int): Float =
        when {
            maxItems <= 1 -> 0.65f
            maxItems == 2 -> 1.0f
            maxItems <= 4 -> 0.75f
            else -> 1.02f
        }

    private fun updateCacheIfNeeded(
        area: Rect,
        count: Int,
        isAA: Boolean,
        isLandscape: Boolean,
        topOffset: Float,
    ) {
        if (lastAreaWidth == area.width() && lastAreaHeight == area.height() && lastItemCount == count) {
            return
        }

        lastAreaWidth = area.width()
        lastAreaHeight = area.height()
        lastItemCount = count

        cachedColumns = columns(isAA, count)
        val cellWidth = area.width() / cachedColumns.toFloat()
        val availableHeight = area.height().toFloat()
        val itemMargin = 6f

        cachedGaugeWidth = gaugeWidth(isAA, cellWidth, count, isLandscape, cachedColumns, availableHeight, itemMargin)
        cachedRowHeight = rowHeight(isAA, availableHeight, isLandscape, cachedColumns, cachedGaugeWidth, itemMargin)
        cachedStartX = startX(area, isAA, count)

        val totalRows = kotlin.math.ceil(count / cachedColumns.toDouble()).toInt()
        cachedContentHeight = topOffset + (totalRows * cachedRowHeight) + 20f
        cachedMaxScroll = max(0f, cachedContentHeight - availableHeight)

        cachedBorderRects.clear()
        cachedCenteredLefts.clear()
        cachedCenteredTops.clear()

        for (i in 0 until count) {
            val row = i / cachedColumns
            val col = i % cachedColumns

            val aaLeftPadding = if (isAA) col * (cachedGaugeWidth - 10f) else 0f
            val cellLeft = if (isAA) cachedStartX + aaLeftPadding else cachedStartX + (col * cellWidth)
            val cellTop = topOffset + (row * cachedRowHeight)

            val currentCellWidth = if (cachedColumns == 1 && !isAA) area.width().toFloat() else cellWidth

            val centeredLeft = if (isAA) cellLeft else cellLeft + (currentCellWidth - cachedGaugeWidth) / 2f
            val centeredTop =
                if (!isAA &&
                    count == 1 &&
                    isLandscape
                ) {
                    cellTop + (availableHeight - cachedGaugeWidth) / 2f
                } else if (isAA) {
                    cellTop
                } else {
                    cellTop + itemMargin
                }

            cachedCenteredLefts[i] = centeredLeft
            cachedCenteredTops[i] = centeredTop
            cachedBorderRects[i] = RectF(centeredLeft, centeredTop, centeredLeft + cachedGaugeWidth, centeredTop + cachedGaugeWidth)
        }
    }
}
