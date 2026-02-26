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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.brake_boosting.BrakeBoostingDrawer

internal class PerformanceScreenSettings(
    original: ScreenSettings,
) : ScreenSettings by original {
    override fun isBreakLabelTextEnabled(): Boolean = true
}

internal class PerformanceSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private val screenSettings = PerformanceScreenSettings(settings)
    private val performanceInfoDetails = PerformanceInfoDetails()
    private val performanceDrawer: PerformanceDrawer =
        PerformanceDrawer(context, screenSettings)
    private val breakBoostingDrawer = BrakeBoostingDrawer(context, screenSettings)

    private val metricsCache = MetricsCache()

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
        val performanceScreenSettings = screenSettings.getPerformanceScreenSettings()
        drawArea?.let {
            performanceDrawer.drawBackground(canvas, it)

            val area = getArea(it, canvas)
            var top = getTop(area)
            val left = performanceDrawer.getMarginLeft(area.left.toFloat())

            if (screenSettings.isStatusPanelEnabled()) {
                performanceDrawer.drawStatusPanel(
                    canvas,
                    top,
                    left,
                    fps,
                    metricsCollector,
                    drawContextInfo = true,
                )
                top += MARGIN_TOP
                performanceDrawer.drawDivider(
                    canvas,
                    left,
                    area.width().toFloat(),
                    top,
                    Color.DKGRAY,
                )
                top += 40
            } else {
                top += MARGIN_TOP
            }

            metricsCache.update(performanceScreenSettings, metricsCollector)

            if (breakBoostingDrawer.isBrakeBoosting(
                    brakeBoostingSettings = performanceScreenSettings.brakeBoostingSettings,
                    gas = metricsCache.gasMetric,
                    torque = metricsCache.torqueMetric,
                )
            ) {
                top -= 30f

                breakBoostingDrawer.drawScreen(
                    canvas,
                    area,
                    top,
                    gas = metricsCache.gasMetric,
                    torque = metricsCache.torqueMetric,
                )
            } else {
                performanceDrawer.drawScreen(
                    canvas = canvas,
                    area = area,
                    left = left,
                    top = top,
                    performanceInfoDetails =
                        performanceInfoDetails.apply {
                            this.bottomMetrics = metricsCache.bottomMetrics
                            this.topMetrics = metricsCache.topMetrics
                        },
                )
            }
        }
    }

    override fun recycle() {
        performanceDrawer.recycle()
    }
}
