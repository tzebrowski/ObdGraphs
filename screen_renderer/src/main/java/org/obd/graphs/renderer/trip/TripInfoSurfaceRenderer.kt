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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings

internal class TripInfoSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private val tripInfo = TripInfoDetails()

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
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
                tripInfo =
                    tripInfo.apply {
                        airTemp = metricsCollector.getMetric(Pid.POST_IC_AIR_TEMP_PID_ID)
                        totalMisfires = metricsCollector.getMetric(Pid.TOTAL_MISFIRES_PID_ID)
                        ambientTemp = metricsCollector.getMetric(Pid.AMBIENT_TEMP_PID_ID)
                        atmPressure = metricsCollector.getMetric(Pid.ATM_PRESSURE_PID_ID)
                        fuellevel = metricsCollector.getMetric(Pid.FUEL_LEVEL_PID_ID)
                        fuelConsumption = metricsCollector.getMetric(Pid.FUEL_CONSUMPTION_PID_ID)
                        coolantTemp = metricsCollector.getMetric(Pid.COOLANT_TEMP_PID_ID)
                        exhaustTemp = metricsCollector.getMetric(Pid.EXHAUST_TEMP_PID_ID)
                        oilTemp = metricsCollector.getMetric(Pid.OIL_TEMP_PID_ID)
                        gearboxOilTemp = metricsCollector.getMetric(Pid.GEARBOX_OIL_TEMP_PID_ID)
                        oilLevel = metricsCollector.getMetric(Pid.OIL_LEVEL_PID_ID)
                        torque = metricsCollector.getMetric(Pid.ENGINE_TORQUE_PID_ID)
                        intakePressure = metricsCollector.getMetric(Pid.INTAKE_PRESSURE_PID_ID)
                        distance = metricsCollector.getMetric(Pid.DISTANCE_PID_ID)
                        ibs = metricsCollector.getMetric(Pid.IBS_PID_ID)
                        batteryVoltage = metricsCollector.getMetric(Pid.BATTERY_VOLTAGE_PID_ID)
                        oilPressure = metricsCollector.getMetric(Pid.OIL_PRESSURE_PID_ID)
                        oilDegradation = metricsCollector.getMetric(Pid.OIL_DEGRADATION_PID_ID)
                        vehicleSpeed = metricsCollector.getMetric(Pid.VEHICLE_SPEED_PID_ID)
                        engineSpeed = metricsCollector.getMetric(Pid.ENGINE_SPEED_PID_ID)
                        gearEngaged = metricsCollector.getMetric(Pid.GEAR_ENGAGED_PID_ID)
                    },
            )
        }
    }

    override fun recycle() {
        tripInfoDrawer.recycle()
    }
}
