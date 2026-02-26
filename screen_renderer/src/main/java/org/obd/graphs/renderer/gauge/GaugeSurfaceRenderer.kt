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
import android.util.Log
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import kotlin.math.max
import kotlin.math.min

private class GaugeLayoutCache {
    var lastAreaWidth = 0
    var lastAreaHeight = 0
    var lastItemCount = 0

    var columns = 1
    var gaugeWidth = 0f
    var rowHeight = 0f
    var startX = 0f
    var contentHeight = 0f
    var maxScroll = 0f

    var centeredLefts = FloatArray(100)
    var centeredTops = FloatArray(100)
    var borderRects = Array(100) { RectF() }

    fun resizeIfNeeded(count: Int) {
        if (count > centeredLefts.size) {
            centeredLefts = FloatArray(count)
            centeredTops = FloatArray(count)
            val oldRects = borderRects
            borderRects =
                Array(count) { index ->
                    if (index < oldRects.size) oldRects[index] else RectF()
                }
        }
    }
}

 private const val TAG = "cache"

 internal class GaugeSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private val layoutCache = GaugeLayoutCache()

    private val gaugeDrawer =
        GaugeDrawer(
            settings = settings,
            context = context,
            drawerSettings = DrawerSettings(gaugeProgressBarType = settings.getGaugeScreenSettings().gaugeProgressBarType),
        )

    private val mobileDrawer =
        GaugeDrawer(
            settings = settings,
            context = context,
            drawerSettings =
                DrawerSettings(
                    startAngle = 200f,
                    sweepAngle = 200f,
                    gaugeProgressBarType = settings.getGaugeScreenSettings().gaugeProgressBarType,
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
        mobileDrawer.recycle()
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

        scrollOffset = scrollOffset.coerceIn(0f, layoutCache.maxScroll)

        val viewportHeight = area.height().toFloat()

        val startRow =
            kotlin.math
                .floor(scrollOffset / layoutCache.rowHeight)
                .toInt()
                .coerceAtLeast(0)
        val endRow =
            kotlin.math.floor((scrollOffset + viewportHeight - 1f) / layoutCache.rowHeight).toInt().coerceAtMost(
                kotlin.math.ceil(count / layoutCache.columns.toDouble()).toInt() - 1,
            )

        val startIndex = startRow * layoutCache.columns
        val endIndex = min(count - 1, (endRow * layoutCache.columns) + (layoutCache.columns - 1))

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
                left = layoutCache.centeredLefts[i],
                top = layoutCache.centeredTops[i],
                width = layoutCache.gaugeWidth,
                metric = metric,
                labelCenterYPadding = labelCenterYPadding,
                drawBorder = drawBorder,
                borderArea = layoutCache.borderRects[i],
                drawModule = !isAA,
                drawMetricRate = drawMetricsRate,
            )
        }

        canvas.restore()

        if (drawScrollbar && layoutCache.contentHeight > viewportHeight) {
            drawScrollbar(
                canvas = canvas,
                area = area,
                contentHeight = layoutCache.contentHeight,
                viewportHeight = viewportHeight,
                topOffset = area.top.toFloat(),
                verticalMargin = 30f,
            )
        }
    }

    private fun columns(
        isAA: Boolean,
        count: Int,
    ): Int =
        if (isAA) {
            when (count) {
                1 -> 1
                2 -> 2
                else -> max(count / 2, count - count / 2)
            }
        } else {
            if (count == 1) 1 else settings.getMaxColumns()
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

        val cacheHit = layoutCache.lastAreaWidth == area.width() &&
                layoutCache.lastAreaHeight == area.height() &&
                layoutCache.lastItemCount == count


        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Layout cache-hit: $cacheHit")
        }
        if (cacheHit) {
            return
        }

        layoutCache.lastAreaWidth = area.width()
        layoutCache.lastAreaHeight = area.height()
        layoutCache.lastItemCount = count

        layoutCache.resizeIfNeeded(count)

        layoutCache.columns = columns(isAA, count)
        val cellWidth = area.width() / layoutCache.columns.toFloat()
        val availableHeight = area.height().toFloat()
        val itemMargin = 6f

        layoutCache.gaugeWidth = gaugeWidth(isAA, cellWidth, count, isLandscape, layoutCache.columns, availableHeight, itemMargin)
        layoutCache.rowHeight = rowHeight(isAA, availableHeight, isLandscape, layoutCache.columns, layoutCache.gaugeWidth, itemMargin)
        layoutCache.startX = startX(area, isAA, count)

        val totalRows = kotlin.math.ceil(count / layoutCache.columns.toDouble()).toInt()
        layoutCache.contentHeight = topOffset + (totalRows * layoutCache.rowHeight) + 20f
        layoutCache.maxScroll = max(0f, layoutCache.contentHeight - availableHeight)

        for (i in 0 until count) {
            val row = i / layoutCache.columns
            val col = i % layoutCache.columns

            val aaLeftPadding = if (isAA) col * (layoutCache.gaugeWidth - 10f) else 0f
            val cellLeft = if (isAA) layoutCache.startX + aaLeftPadding else layoutCache.startX + (col * cellWidth)
            val cellTop = topOffset + (row * layoutCache.rowHeight)

            val currentCellWidth = if (layoutCache.columns == 1 && !isAA) area.width().toFloat() else cellWidth

            val centeredLeft = if (isAA) cellLeft else cellLeft + (currentCellWidth - layoutCache.gaugeWidth) / 2f
            val centeredTop =
                if (!isAA && count == 1 && isLandscape) {
                    cellTop + (availableHeight - layoutCache.gaugeWidth) / 2f
                } else if (isAA) {
                    cellTop
                } else {
                    cellTop + itemMargin
                }

            layoutCache.centeredLefts[i] = centeredLeft
            layoutCache.centeredTops[i] = centeredTop
            layoutCache.borderRects[i].set(
                centeredLeft,
                centeredTop,
                centeredLeft + layoutCache.gaugeWidth,
                centeredTop + layoutCache.gaugeWidth,
            )
        }
    }
}
