/**
 * Copyright 2019-2024, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.renderer.dynamic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.*
import org.obd.graphs.renderer.*

private const val LOG_TAG = "DynamicSurfaceRenderer"

internal class DynamicSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {
    private val tripInfo = DynamicInfoDetails()
    private val dynamicDrawer = DynamicDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        Log.d(LOG_TAG,"Query strategy ${query.getStrategy()}, selected ids: ${query.getIDs()}")

        metricsCollector.applyFilter(
            enabled = query.getIDs()
        )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { it ->

            dynamicDrawer.drawBackground(canvas, it)

            val margin = 0
            val area = getArea(it, canvas, margin)
            var top = getTop(area)
            val left = dynamicDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                dynamicDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = true)
                top += MARGIN_TOP
                dynamicDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            dynamicDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                dynamicInfoDetails = tripInfo.apply {
                    airTemp = metricsCollector.getMetric(namesRegistry.getAirTempPID())
                    totalMisfires = metricsCollector.getMetric(namesRegistry.getTotalMisfiresPID())
                    ambientTemp = metricsCollector.getMetric(namesRegistry.getAmbientTempPID())
                    atmPressure = metricsCollector.getMetric(namesRegistry.getAtmPressurePID())
                    fuellevel = metricsCollector.getMetric(namesRegistry.getFuelLevelPID())
                    fuelConsumption = metricsCollector.getMetric(namesRegistry.getFuelConsumptionPID())
                    coolantTemp = metricsCollector.getMetric(namesRegistry.getCoolantTempPID())
                    exhaustTemp = metricsCollector.getMetric(namesRegistry.getExhaustTempPID())
                    oilTemp = metricsCollector.getMetric(namesRegistry.getOilTempPID())
                    gearboxOilTemp = metricsCollector.getMetric(namesRegistry.getGearboxOilTempPID())
                    oilLevel = metricsCollector.getMetric(namesRegistry.getOilLevelPID())
                    torque = metricsCollector.getMetric(namesRegistry.getTorquePID())
                    intakePressure = metricsCollector.getMetric(namesRegistry.getIntakePressurePID())
                    distance = metricsCollector.getMetric(namesRegistry.getDistancePID())
                    ibs = metricsCollector.getMetric(namesRegistry.getIbsPID())
                    batteryVoltage = metricsCollector.getMetric(namesRegistry.getBatteryVoltagePID())
                    oilPressure = metricsCollector.getMetric(namesRegistry.getOilPressurePID())
                }
            )
        }
    }


    override fun recycle() {
        dynamicDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG,"Init Trip Info Surface renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.DYNAMIC))
    }
}