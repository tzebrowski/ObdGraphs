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
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.renderer.trip.TripInfoDrawer

private const val MAX_ITEMS_IN_ROW = 6

@Suppress("NOTHING_TO_INLINE")
internal class PerformanceDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG
        )
    )

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, org.obd.graphs.renderer.R.drawable.drag_race_bg)

    override fun getBackground(): Bitmap = background

    override fun recycle() {
        // FIX: Removed dangerous super.getBackground().recycle() to prevent crashes
        this.background.recycle()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        performanceInfoDetails: PerformanceInfoDetails
    ) {
        val textSize = calculateFontSize(
            multiplier = area.width() / 17f,
            fontSize = settings.getPerformanceScreenSettings().fontSize
        )

        val itemWidth = area.width() / MAX_ITEMS_IN_ROW.toFloat()

        var rowTop = top + 2f

        val metricsToDraw = listOfNotNull(
            performanceInfoDetails.postICAirTemp,
            performanceInfoDetails.coolantTemp,
            performanceInfoDetails.oilTemp,
            performanceInfoDetails.exhaustTemp,
            performanceInfoDetails.gearboxOilTemp,
            performanceInfoDetails.ambientTemp,
            performanceInfoDetails.preICAirTemp,
            performanceInfoDetails.wcacTemp,
            performanceInfoDetails.gearEngaged
        )

        // Draw loop using positional arguments to match TripInfoDrawer signature
        metricsToDraw.take(MAX_ITEMS_IN_ROW).forEachIndexed { index, metric ->
            tripInfoDrawer.drawMetric(
                metric,
                rowTop,
                left + (index * itemWidth),
                canvas,
                textSize,
                statsEnabled = true,
                area = area,
                castToInt = true
            )
        }

        drawDivider(canvas, left, area.width().toFloat(), rowTop + textSize + 4, Color.DKGRAY)

        rowTop += textSize + 16

        val availableWidth = area.width().toFloat()
        val areaLeft = area.left.toFloat()

        val gauges = listOfNotNull(
            performanceInfoDetails.torque,
            performanceInfoDetails.intakePressure,
            performanceInfoDetails.gas,
            performanceInfoDetails.vehicleSpeed
        )

        val labelCenterYPadding = settings.getPerformanceScreenSettings().labelCenterYPadding - 4

        when (gauges.size) {
            4 -> {
                val topRowWidth = availableWidth / 2f

                drawGauge(performanceInfoDetails.torque, canvas, rowTop, areaLeft, topRowWidth, labelCenterYPadding)
                drawGauge(performanceInfoDetails.intakePressure, canvas, rowTop, areaLeft + topRowWidth, topRowWidth, labelCenterYPadding)

                val topRowHeight = topRowWidth * 0.8f
                val bottomRowTop = rowTop + (topRowHeight * 0.45f)
                val bottomRowWidth = availableWidth / 3.5f
                val centerOffset = (availableWidth - (bottomRowWidth * 2)) / 2f

                drawGauge(performanceInfoDetails.gas, canvas, bottomRowTop, areaLeft + centerOffset - 10f, bottomRowWidth, labelCenterYPadding)
                drawGauge(performanceInfoDetails.vehicleSpeed, canvas, bottomRowTop, areaLeft + centerOffset + bottomRowWidth + 10f, bottomRowWidth, labelCenterYPadding)
            }

            3 -> {
                val width = availableWidth / 3f
                drawGauge(gauges[0], canvas, rowTop, areaLeft, width, labelCenterYPadding)
                drawGauge(gauges[1], canvas, rowTop, areaLeft + width, width, labelCenterYPadding)
                drawGauge(gauges[2], canvas, rowTop, areaLeft + (width * 2), width, labelCenterYPadding)
            }

            2 -> {
                val width = availableWidth / 2f
                drawGauge(gauges[0], canvas, rowTop, areaLeft, width, labelCenterYPadding)
                drawGauge(gauges[1], canvas, rowTop, areaLeft + width, width, labelCenterYPadding)
            }

            1 -> {
                drawGauge(gauges[0], canvas, rowTop, areaLeft + (availableWidth / 4f), availableWidth / 2f, labelCenterYPadding = 6f)
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
                scaleEnabled = false
            )
            true
        }
}
