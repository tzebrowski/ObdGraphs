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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.*
import org.obd.graphs.renderer.*

private const val LOG_TAG = "TripInfoSurfaceRenderer"

internal class TripInfoSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {

    private val tripInfo = TripInfoDetails()

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        Log.d(LOG_TAG,"Query strategy ${query.getStrategy()}, selected ids: ${query.getIDs()}")

        metricsCollector.applyFilter(
            enabled = query.getIDs()
        )
    }
    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let {

            tripInfoDrawer.drawBackground(canvas, it)

            val area = getArea(it, canvas)
            var top = getTop(area)
            val left = tripInfoDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                tripInfoDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = true)
                top += MARGIN_TOP
                tripInfoDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            tripInfoDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                tripInfo = tripInfo.apply {
                    airTemp = metricsCollector.getMetric(PidId.POST_IC_AIR_TEMP_PID_ID)
                    totalMisfires = metricsCollector.getMetric(PidId.TOTAL_MISFIRES_PID_ID)
                    ambientTemp = metricsCollector.getMetric(PidId.EXT_AMBIENT_TEMP_PID_ID)
                    atmPressure = metricsCollector.getMetric(PidId.EXT_ATM_PRESSURE_PID_ID)
                    fuellevel = metricsCollector.getMetric(PidId.FUEL_LEVEL_PID_ID)
                    fuelConsumption = metricsCollector.getMetric(PidId.FUEL_CONSUMPTION_PID_ID)
                    coolantTemp = metricsCollector.getMetric(PidId.COOLANT_TEMP_PID_ID)
                    exhaustTemp = metricsCollector.getMetric(PidId.EXHAUST_TEMP_PID_ID)
                    oilTemp = metricsCollector.getMetric(PidId.OIL_TEMP_PID_ID)
                    gearboxOilTemp = metricsCollector.getMetric(PidId.GEARBOX_OIL_TEMP_PID_ID)
                    oilLevel = metricsCollector.getMetric(PidId.OIL_LEVEL_PID_ID)
                    torque = metricsCollector.getMetric(PidId.ENGINE_TORQUE_PID_ID)
                    intakePressure = metricsCollector.getMetric(PidId.INTAKE_PRESSURE_PID_ID)
                    distance = metricsCollector.getMetric( PidId.DISTANCE_PID_ID)
                    ibs = metricsCollector.getMetric(PidId.IBS_PID_ID)
                    batteryVoltage = metricsCollector.getMetric(PidId.BATTERY_VOLTAGE_PID_ID)
                    oilPressure = metricsCollector.getMetric(PidId.OIL_PRESSURE_PID_ID)
                    oilDegradation = metricsCollector.getMetric(PidId.OIL_DEGRADATION_PID_ID)
                }
            )
        }
    }


    override fun recycle() {
        tripInfoDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG,"Init Trip Info Surface renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.TRIP_INFO_QUERY))
    }
}
