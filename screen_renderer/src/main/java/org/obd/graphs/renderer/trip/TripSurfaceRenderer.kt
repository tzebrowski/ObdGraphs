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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.*
import org.obd.graphs.renderer.*


@Suppress("NOTHING_TO_INLINE")
internal class TripSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    viewSettings: ViewSettings
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector, viewSettings) {
    private val tripInfo = TripInfoDetails()
    private val tripDrawer = TripDrawer(context, settings)
    override fun applyMetricsFilter(query: Query) {
        metricsCollector.applyFilter(
            enabled = query.getIDs()
        )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { it ->

            tripDrawer.drawBackground(canvas, it)

            val margin = 0
            val area = getArea(it, canvas, margin)
            var top = getDrawTop(area)
            var left = tripDrawer.getMarginLeft(area.left.toFloat())

            left += 5

            if (settings.isStatusPanelEnabled()) {
                tripDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = true)
                top += 4
                tripDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            }

            tripDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                tripInfo = tripInfo.apply {
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
                    gearboxEngaged = metricsCollector.getMetric(namesRegistry.getGearboxEngagedPID())
                    oilLevel = metricsCollector.getMetric(namesRegistry.getOilLevelPID())
                    torque = metricsCollector.getMetric(namesRegistry.getTorquePID())
                    intakePressure = metricsCollector.getMetric(namesRegistry.getIntakePressurePID())
                    distance = metricsCollector.getMetric(namesRegistry.getDistancePID())
                    fuelConsumed = metricsCollector.getMetric(namesRegistry.getFuelConsumedPID())
                }
            )
        }
    }

    override fun recycle() {
        tripDrawer.recycle()
    }

    init {
        applyMetricsFilter(Query.instance(QueryStrategyType.TRIP_INFO_QUERY))
    }
}