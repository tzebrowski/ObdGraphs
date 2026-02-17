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
import android.util.Log
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.CoreSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.break_boosting.BreakBoostingDrawer

private const val LOG_TAG = "PerformanceSurfaceRenderer"

internal class PerformanceSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : CoreSurfaceRenderer() {
    private val performanceInfoDetails = PerformanceInfoDetails()
    private val performanceDrawer = PerformanceDrawer(context, settings)
    private val breakBoostingDrawer = BreakBoostingDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        Log.d(LOG_TAG, "Query strategy ${query.getStrategy()}, selected id's: ${query.getIDs()}")

        metricsCollector.applyFilter(
            enabled = query.getIDs(),
        )
    }

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
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

            if (breakBoostingDrawer.isBreakBoosting(
                    breakBoostingSettings = settings.getPerformanceScreenSettings().breakBoostingSettings,
                    gas = metricsCollector.getMetric(Pid.GAS_PID_ID),
                    torque = metricsCollector.getMetric(Pid.ENGINE_TORQUE_PID_ID),
                )
            ) {
                top -= 30f

                breakBoostingDrawer.drawScreen(
                    canvas,
                    area,
                    top,
                    gas = metricsCollector.getMetric(Pid.GAS_PID_ID),
                    torque = metricsCollector.getMetric(Pid.ENGINE_TORQUE_PID_ID),
                )
            } else {
                performanceDrawer.drawScreen(
                    canvas = canvas,
                    area = area,
                    left = left,
                    top = top,
                    performanceInfoDetails =
                        performanceInfoDetails.apply {
                            gas = metricsCollector.getMetric(Pid.GAS_PID_ID)
                            postICAirTemp = metricsCollector.getMetric(Pid.POST_IC_AIR_TEMP_PID_ID)
                            ambientTemp = metricsCollector.getMetric(Pid.AMBIENT_TEMP_PID_ID)
                            atmPressure = metricsCollector.getMetric(Pid.ATM_PRESSURE_PID_ID)
                            coolantTemp = metricsCollector.getMetric(Pid.COOLANT_TEMP_PID_ID)
                            exhaustTemp = metricsCollector.getMetric(Pid.EXHAUST_TEMP_PID_ID)
                            oilTemp = metricsCollector.getMetric(Pid.OIL_TEMP_PID_ID)
                            gearboxOilTemp = metricsCollector.getMetric(Pid.GEARBOX_OIL_TEMP_PID_ID)
                            torque = metricsCollector.getMetric(Pid.ENGINE_TORQUE_PID_ID)
                            intakePressure = metricsCollector.getMetric(Pid.INTAKE_PRESSURE_PID_ID)
                            preICAirTemp = metricsCollector.getMetric(Pid.PRE_IC_AIR_TEMP_PID_ID)
                            wcacTemp = metricsCollector.getMetric(Pid.WCA_TEMP_PID_ID)
                            vehicleSpeed = metricsCollector.getMetric(Pid.EXT_VEHICLE_SPEED_PID_ID)
                            gearEngaged = metricsCollector.getMetric(Pid.GEAR_ENGAGED_PID_ID)
                        },
                )
            }
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
