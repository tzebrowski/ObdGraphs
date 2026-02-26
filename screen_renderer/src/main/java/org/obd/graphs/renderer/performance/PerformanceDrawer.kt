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
package org.obd.graphs.renderer.performance

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.api.GaugeProgressBarType
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.renderer.trip.TripInfoDrawer
import org.obd.graphs.isNumber

private const val MAX_ITEMS_IN_ROW = 5

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

        val itemWidth = area.width() / MAX_ITEMS_IN_ROW.toFloat()
        var rowTop = top + 2f
        val topMetrics = performanceInfoDetails.topMetrics
        val topMetricsSize = topMetrics.size

        for (i in 0 until topMetricsSize) {
            val metric = topMetrics[i]
            val columnIndex = i % MAX_ITEMS_IN_ROW

            if (columnIndex == 0 && i > 0) {
                drawDivider(
                    canvas,
                    left,
                    area.width().toFloat(),
                    rowTop + textSize  * 0.8f,
                    Color.DKGRAY
                )
                rowTop += 2 * textSize
            }

            val itemLeft = left + (columnIndex * itemWidth)

            tripInfoDrawer.drawMetric(
                metric,
                rowTop,
                itemLeft,
                canvas,
                textSize,
                statsEnabled = metric.source.isNumber(),
                area = area,
                castToInt = true
            )

            if (columnIndex < MAX_ITEMS_IN_ROW - 1 && i < topMetricsSize - 1) {
                val lineX = itemLeft + itemWidth
                canvas.drawLine(
                    lineX,
                    rowTop,
                    lineX,
                    rowTop + textSize * 0.8f,
                    dividerPaint
                )
            }
        }

        if (topMetricsSize > 0) {
            drawDivider(
                canvas,
                left,
                area.width().toFloat(),
                rowTop + textSize * 0.8f,
                Color.DKGRAY
            )
            rowTop += 1.8f * textSize
        }

        rowTop -= textSize * 0.7f

        val availableWidth = area.width().toFloat()
        val areaLeft = area.left.toFloat()
        val labelCenterYPadding = performanceScreenSettings.labelCenterYPadding - 4
        val bottomMetrics = performanceInfoDetails.bottomMetrics
        val count = bottomMetrics.size

        if (count > 0) {
            val width = if (count == 1) availableWidth / 2f else availableWidth / count.toFloat()
            val startLeft = if (count == 1) areaLeft + (availableWidth / 4f) else areaLeft
            val padding = if (count == 1) 6f else labelCenterYPadding

            for (i in 0 until count) {
                val gauge = bottomMetrics[i]
                drawGauge(gauge, canvas, rowTop, startLeft + (width * i), width, padding)
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
