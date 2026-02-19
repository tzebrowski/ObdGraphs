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
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.getContext
import org.obd.graphs.renderer.CoreSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.ScreenSettings
import kotlin.math.max
import kotlin.math.min

internal class GaugeSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : CoreSurfaceRenderer() {
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

    override fun applyMetricsFilter(query: Query) {
        val gaugeSettings = settings.getGaugeRendererSetting()
        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            metricsCollector.applyFilter(enabled = gaugeSettings.selectedPIDs, order = gaugeSettings.getPIDsSortOrder())
        } else {
            val ids = query.getIDs()
            val intersection = gaugeSettings.selectedPIDs.filter { ids.contains(it) }.toSet()
            metricsCollector.applyFilter(enabled = intersection, order = gaugeSettings.getPIDsSortOrder())
        }
    }

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

            drawGrid(
                canvas = canvas,
                area = area,
                metrics = metricsCollector.getMetrics(),
                topOffset = top,
                maxItems = settings.getMaxItems(),
            )
        }
    }

    override fun recycle() {
        gaugeDrawer.recycle()
    }

    private fun drawGrid(
        canvas: Canvas,
        area: Rect,
        metrics: List<Metric>,
        topOffset: Float,
        maxItems: Int,
        drawScrollbar: Boolean = true,
    ) {
        val count = min(metrics.size, maxItems)
        if (count <= 0) return

        val isAA = settings.isAA()
        val isLandscape = getContext()!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val drawer = if (isAA) gaugeDrawer else mobileDrawer
        val drawBorder = !isAA
        val labelCenterYPadding = if (isAA && count <= 2) 22f else 20f

        val columns = columns(isAA, count)

        val cellWidth = area.width() / columns.toFloat()
        val availableHeight = area.height().toFloat()
        val itemMargin = 6f

        val gaugeWidth = gaugeWidth(isAA, cellWidth, count, isLandscape, columns, availableHeight, itemMargin)
        val rowHeight = rowHeight(isAA, availableHeight, isLandscape, columns, gaugeWidth, itemMargin)
        val startX = startX(area, isAA, count)

        val totalRows = kotlin.math.ceil(count / columns.toDouble()).toInt()
        val contentHeight = totalRows * rowHeight
        val viewportHeight = area.height().toFloat()

        val maxScroll = max(0f, contentHeight - viewportHeight)
        scrollOffset = if (count <= 1 || isAA) 0f else scrollOffset.coerceIn(0f, maxScroll)

        val startRow =
            kotlin.math
                .floor(scrollOffset / rowHeight)
                .toInt()
                .coerceAtLeast(0)
        val endRow =
            kotlin.math
                .floor((scrollOffset + viewportHeight - 1f) / rowHeight)
                .toInt()
                .coerceAtMost(totalRows - 1)

        val startIndex = startRow * columns
        val endIndex = min(count - 1, (endRow * columns) + (columns - 1))

        canvas.save()
        canvas.clipRect(area)
        canvas.translate(0f, -scrollOffset)

        for (i in startIndex..endIndex) {
            val metric = metrics[i]
            val row = i / columns
            val col = i % columns

            val aaLeftPadding = if (isAA) col * (gaugeWidth - 10f) else 0f
            val cellLeft = if (isAA) startX + aaLeftPadding else startX + (col * cellWidth)

            val aaTopOffset = if (isAA && row > 0) -8f else 0f
            val cellTop = topOffset + (row * rowHeight) + aaTopOffset

            val currentCellWidth = if (columns == 1 && !isAA) area.width().toFloat() else cellWidth

            val centeredLeft = if (isAA) cellLeft else cellLeft + (currentCellWidth - gaugeWidth) / 2f
            val centeredTop =
                if (!isAA &&
                    count == 1 &&
                    isLandscape
                ) {
                    cellTop + (availableHeight - gaugeWidth) / 2f
                } else if (isAA) {
                    cellTop
                } else {
                    cellTop + itemMargin
                }

            val borderRect = RectF(centeredLeft, centeredTop, centeredLeft + gaugeWidth, centeredTop + gaugeWidth)

            drawer.drawGauge(
                canvas = canvas,
                left = centeredLeft,
                top = centeredTop,
                width = gaugeWidth,
                metric = metric,
                labelCenterYPadding = labelCenterYPadding,
                drawBorder = drawBorder,
                borderArea = borderRect,
                drawModule = !isAA,
                drawMetricRate = settings.isFpsCounterEnabled(),
            )
        }

        canvas.restore()

        if (drawScrollbar && contentHeight > viewportHeight) {
            drawScrollbar(contentHeight, viewportHeight, maxScroll, area, canvas)
        }
    }

    private fun drawScrollbar(
        contentHeight: Float,
        viewportHeight: Float,
        maxScroll: Float,
        area: Rect,
        canvas: Canvas,
    ) {
        val verticalMargin = 30f
        val trackHeight = viewportHeight - (2 * verticalMargin)

        val calculatedBarHeight = (viewportHeight / contentHeight) * trackHeight
        val barHeight = max(calculatedBarHeight, 50f)

        val scrollPercentage = scrollOffset / maxScroll
        val availableTravel = trackHeight - barHeight
        val barTop = area.top + verticalMargin + (scrollPercentage * availableTravel)

        val barRect =
            RectF(
                area.left + 5f,
                barTop,
                area.left + 5f + scrollBarWidth,
                barTop + barHeight,
            )

        canvas.drawRoundRect(barRect, 10f, 10f, scrollBarPaint)
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
}
