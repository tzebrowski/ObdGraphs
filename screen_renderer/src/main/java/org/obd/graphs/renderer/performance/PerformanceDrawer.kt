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

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.api.GaugeProgressBarType
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.renderer.gauge.GaugeGeometry
import org.obd.graphs.renderer.trip.TripInfoDrawer
import org.obd.graphs.renderer.trip.TripInfoGrid
import org.obd.graphs.isNumber
import org.obd.metrics.pid.ValueType

@Suppress("NOTHING_TO_INLINE")
internal class PerformanceDrawer(context: Context, settings: ScreenSettings) :
    AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG
        )
    )

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    private val gaugeBottomRatio = DrawerSettings().let { GaugeGeometry.dialBottomRatio(it.startAngle, it.sweepAngle) }

    // Rebuilt only when the grid item count changes, not every frame.
    private var grid: TripInfoGrid = PerformanceMetrics.grid(0)
    private var gridRows = -1f

    private val background: Bitmap =
        BitmapFactory.decodeResource(
            context.resources,
            org.obd.graphs.renderer.R.drawable.drag_race_bg
        )

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 2f
        alpha = 100
    }

    override fun invalidate() {
        super.invalidate()
        tripInfoDrawer.invalidate()
        gaugeDrawer.invalidate()
    }

    override fun getBackground(): Bitmap = background

    override fun recycle() {
        this.background.recycle()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        performanceInfoDetails: PerformanceInfoDetails
    ) {
        val performanceScreenSettings = settings.getPerformanceScreenSettings()

        val textSize = calculateFontSize(
            multiplier = area.width() / 17f,
            fontSize = performanceScreenSettings.fontSize
        )

        val availableWidth = area.width().toFloat()
        val bottomMetrics = performanceInfoDetails.bottomMetrics
        val count = bottomMetrics.size

        // Gauges first, at full size; the grid shrinks into the height left above them.
        val fullGauges = PerformanceMetrics.gaugeRow(count, availableWidth, area.bottom - top, gaugeBottomRatio)
        val gaugesHeight = if (count > 0) fullGauges.width * gaugeBottomRatio else 0f
        val rows = PerformanceMetrics.gridRows(area.bottom - top - gaugesHeight, textSize)

        val topMetrics = performanceInfoDetails.topMetrics
        val topMetricsSize = topMetrics.size
        if (grid.shown + grid.hidden != topMetricsSize || rows != gridRows) {
            grid = PerformanceMetrics.grid(topMetricsSize, rows)
            gridRows = rows
        }

        val gridTextSize = textSize * grid.scale
        val itemWidth = area.width() / grid.columns.toFloat()
        var rowTop = top + 2f
        val drawnCount = grid.shown

        for (i in 0 until drawnCount) {
            val metric = topMetrics[i]
            val columnIndex = i % grid.columns

            if (columnIndex == 0 && i > 0) {
                drawDivider(
                    canvas,
                    left,
                    area.width().toFloat(),
                    rowTop + gridTextSize * 0.8f,
                    Color.DKGRAY
                )
                rowTop += 2 * gridTextSize
            }

            val itemLeft = left + (columnIndex * itemWidth)

            tripInfoDrawer.drawMetric(
                metric,
                rowTop,
                itemLeft,
                canvas,
                gridTextSize,
                statsEnabled = metric.source.isNumber(),
                area = area,
                castToInt = metric.pid.type != ValueType.DOUBLE,
                maxWidth = itemWidth.toInt()
            )

            if (columnIndex < grid.columns - 1 && i < topMetricsSize - 1) {
                val lineX = itemLeft + itemWidth
                canvas.drawLine(
                    lineX,
                    rowTop,
                    lineX,
                    rowTop + gridTextSize * 0.8f,
                    dividerPaint
                )
            }
        }

        if (grid.hidden > 0) {
            // shown is capacity - 1, so the marker always lands in the last cell of the current row.
            val columnIndex = drawnCount % grid.columns
            tripInfoDrawer.drawHiddenCount(
                canvas,
                grid.hidden,
                left + (columnIndex * itemWidth),
                rowTop,
                gridTextSize * 0.8f
            )
        }

        if (topMetricsSize > 0) {
            drawDivider(
                canvas,
                left,
                area.width().toFloat(),
                rowTop + gridTextSize * 0.8f,
                Color.DKGRAY
            )
            rowTop += 1.8f * gridTextSize
        }

        rowTop -= textSize * 0.7f

        val areaLeft = area.left.toFloat()
        val labelCenterYPadding = performanceScreenSettings.labelCenterYPadding - 4

        if (count > 0) {
            val row = PerformanceMetrics.gaugeRow(count, availableWidth, area.bottom - rowTop, gaugeBottomRatio)
            val startLeft = areaLeft + row.left
            val padding = if (count == 1) 6f else labelCenterYPadding

            for (i in 0 until count) {
                val gauge = bottomMetrics[i]
                drawGauge(gauge, canvas, rowTop, startLeft + (row.width * i), row.width, padding)
            }
        }
    }

    fun drawGauge(
        metric: Metric?,
        canvas: Canvas,
        top: Float,
        left: Float,
        width: Float,
        labelCenterYPadding: Float = settings.getPerformanceScreenSettings().labelCenterYPadding,
    ): Boolean =
        if (metric == null) {
            false
        } else {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = left,
                top = top,
                width = width,
                metric = metric,
                labelCenterYPadding = labelCenterYPadding,
                fontSize = settings.getPerformanceScreenSettings().fontSize,
                scaleEnabled = false,
                statsEnabled = metric.source.isNumber()
            )
            true
        }
}
