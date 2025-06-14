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
import org.obd.graphs.bl.datalogger.PidId
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

            val area = getArea(it, canvas)
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
                    gas = metricsCollector.getMetric(PidId.GAS_PID_ID)
                    postICAirTemp = metricsCollector.getMetric(PidId.POST_IC_AIR_TEMP_PID_ID)
                    ambientTemp = metricsCollector.getMetric(PidId.EXT_AMBIENT_TEMP_PID_ID)
                    atmPressure = metricsCollector.getMetric(PidId.EXT_ATM_PRESSURE_PID_ID)
                    coolantTemp = metricsCollector.getMetric(PidId.COOLANT_TEMP_PID_ID)
                    exhaustTemp = metricsCollector.getMetric(PidId.EXHAUST_TEMP_PID_ID)
                    oilTemp = metricsCollector.getMetric(PidId.OIL_TEMP_PID_ID)
                    gearboxOilTemp = metricsCollector.getMetric(PidId.GEARBOX_OIL_TEMP_PID_ID)
                    torque = metricsCollector.getMetric(PidId.ENGINE_TORQUE_PID_ID)
                    intakePressure = metricsCollector.getMetric(PidId.INTAKE_PRESSURE_PID_ID)
                    preICAirTemp = metricsCollector.getMetric(PidId.PRE_IC_AIR_TEMP_PID_ID)
                    wcacTemp = metricsCollector.getMetric(PidId.WCA_TEMP_PID_ID)
                    vehicleSpeed = metricsCollector.getMetric(PidId.EXT_VEHICLE_SPEED_PID_ID)
                    gearEngaged =  metricsCollector.getMetric(PidId.GEAR_ENGAGED_PID_ID)
                }
            )
        }
    }

    override fun recycle() {
        performanceDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG, "Init Performance Surface renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.PERFORMANCE_QUERY))
    }
}
