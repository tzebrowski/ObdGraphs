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

@Suppress("NOTHING_TO_INLINE")
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

            if (settings.isStatusPanelEnabled()) {
                val left = gaugeDrawer.getMarginLeft(area.left.toFloat())
                gaugeDrawer.drawStatusPanel(canvas, top, left, fps)
                top += MARGIN_TOP
                gaugeDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 10
            }

            val maxItems = min(metricsCollector.getMetrics().size, settings.getMaxItems())
            val metrics = metricsCollector.getMetrics()

            if (settings.isAA()) {
                if (maxItems == 0) return

                val marginLeft =
                    when (maxItems) {
                        1 -> area.width() / 6f
                        3, 4 -> area.width() / 8f
                        else -> 5f
                    }

                val topOffset =
                    when (maxItems) {
                        1 -> 6f
                        2 -> area.height() / 7f
                        else -> 0f
                    }

                val labelCenterYPadding = if (maxItems <= 2) 22f else 20f

                val partitionIndex = if (maxItems <= 2) maxItems else maxItems / 2

                draw(
                    canvas = canvas,
                    area = area,
                    metrics = metrics,
                    marginLeft = marginLeft,
                    top = top + topOffset,
                    labelCenterYPadding = labelCenterYPadding,
                    maxItems = maxItems,
                    partitionIndex = partitionIndex,
                )
            } else {
                drawMobile(canvas, area, metrics, top = top, labelCenterYPadding = 20f, maxItems = maxItems)
            }
        }
    }

    override fun recycle() {
        gaugeDrawer.recycle()
    }

    private fun draw(
        canvas: Canvas,
        area: Rect,
        metrics: List<Metric>,
        marginLeft: Float = 5f,
        top: Float,
        labelCenterYPadding: Float = 0f,
        maxItems: Int,
        partitionIndex: Int,
    ) {
        val firstHalf = metrics.subList(0, partitionIndex)
        val secondHalf = metrics.subList(partitionIndex, maxItems)
        val height = (area.height() / 2)

        val widthDivider =
            when (maxItems) {
                1 -> 1
                2 -> 2
                else -> max(firstHalf.size, secondHalf.size)
            }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(maxItems)
        val padding = 10f

        var left = marginLeft
        firstHalf.forEach {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = area.left + left,
                top = top,
                width = width,
                metric = it,
                labelCenterYPadding = labelCenterYPadding,
            )
            left += width - padding
        }

        if (secondHalf.isNotEmpty()) {
            left = marginLeft
            secondHalf.forEach {
                gaugeDrawer.drawGauge(
                    canvas = canvas,
                    left = area.left + left,
                    top = top + height - 8f,
                    width = width,
                    metric = it,
                    labelCenterYPadding = labelCenterYPadding,
                )
                left += width - padding
            }
        }
    }

    private fun drawMobile(
        canvas: Canvas,
        area: Rect,
        metrics: List<Metric>,
        top: Float,
        labelCenterYPadding: Float = 0f,
        maxItems: Int,
    ) {
        val count = min(metrics.size, maxItems)
        if (count <= 0) return
        val isLandscape = getContext()!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (count == 1) {
            val scaleFactor = if (isLandscape) 1.5f else 1.0f
            val baseSize = min(area.width(), area.height()).toFloat()
            val drawSize = baseSize * scaleFactor
            val leftOffset = (area.width() - drawSize) / 2f

            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = area.left + leftOffset,
                top = top,
                width = drawSize,
                metric = metrics[0],
                labelCenterYPadding = labelCenterYPadding,
            )
        } else {
            val scaleFactor = if (isLandscape) 1.0f else 1.0f

            val columns = 2
            val cellWidth = area.width() / columns.toFloat()

            val availableHeight = area.height().toFloat()
            val cellHeight = min(cellWidth, availableHeight)

            val drawWidth = cellWidth * scaleFactor

            val totalRows = kotlin.math.ceil(count / columns.toDouble()).toInt()
            val contentHeight = totalRows * cellHeight
            val viewportHeight = area.height().toFloat()

            val maxScroll = max(0f, contentHeight - viewportHeight)
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            canvas.save()
            canvas.clipRect(area)
            canvas.translate(0f, -scrollOffset)

            for (i in 0 until count) {
                val metric = metrics[i]
                val row = i / columns
                val col = i % columns

                val cellLeft = area.left + (col * cellWidth)
                val cellTop = top + (row * cellHeight)

                val offsetX = (drawWidth - cellWidth) / 2f

                gaugeDrawer.drawGauge(
                    canvas = canvas,
                    left = cellLeft - offsetX,
                    top = cellTop,
                    width = drawWidth,
                    metric = metric,
                    labelCenterYPadding = labelCenterYPadding,
                )
            }

            canvas.restore()

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
        if (contentHeight > viewportHeight) {
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
    }

    private fun widthScaleRatio(maxItems: Int): Float =
        when {
            maxItems <= 1 -> 0.65f
            maxItems == 2 -> 1.0f
            maxItems <= 4 -> 0.8f
            else -> 1.02f
        }
}
