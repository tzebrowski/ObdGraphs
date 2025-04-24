/**
 * Copyright 2019-2025, Tomasz Żebrowski
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
import android.util.Log
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.*
import org.obd.graphs.renderer.*

private const val LOG_TAG = "PerformanceSurfaceRenderer"

internal class PerformanceSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {
    private val performanceInfoDetails = PerformanceInfoDetails()
    private val performanceDrawer = PerformanceDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        Log.d(LOG_TAG, "Query strategy ${query.getStrategy()}, selected id's: ${query.getIDs()}")

        metricsCollector.applyFilter(
            enabled = query.getIDs()
        )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let {

            performanceDrawer.drawBackground(canvas, it)

            val margin = 0
            val area = getArea(it, canvas, margin)
            var top = getTop(area)
            val left = performanceDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                performanceDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = true)
                top += MARGIN_TOP
                performanceDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            performanceDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                performanceInfoDetails = performanceInfoDetails.apply {
                    gas = metricsCollector.getMetric(namesRegistry.getGasPedalPID())
                    airTemp = metricsCollector.getMetric(namesRegistry.getAirTempPID())
                    ambientTemp = metricsCollector.getMetric(namesRegistry.getAmbientTempPID())
                    atmPressure = metricsCollector.getMetric(namesRegistry.getAtmPressurePID())
                    coolantTemp = metricsCollector.getMetric(namesRegistry.getCoolantTempPID())
                    exhaustTemp = metricsCollector.getMetric(namesRegistry.getExhaustTempPID())
                    oilTemp = metricsCollector.getMetric(namesRegistry.getOilTempPID())
                    gearboxOilTemp = metricsCollector.getMetric(namesRegistry.getGearboxOilTempPID())
                    torque = metricsCollector.getMetric(namesRegistry.getTorquePID())
                    intakePressure = metricsCollector.getMetric(namesRegistry.getIntakePressurePID())
                }
            )
        }
    }

    override fun recycle() {
        performanceDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG, "Init Performance Surface renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.PERFORMANCE))
    }
}
