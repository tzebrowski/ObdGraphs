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


data class TripInfoDetails(
    var ambientTemp: Number? = null,
    var atmPressure: Number? = null,
    var vehicleSpeed: Number? = null,
    var fuellevel: Number? = null,
    var fuelConsumption: Number? = null,
    var oilTemp: Number? = null,
    var coolantTemp: Number? = null,
    var airTemp: Number? = null,
    var exhaustTemp: Number? = null,
    var gearboxOilTemp: Number? = null,
    var gearboxEngaged: Number? = null,
)


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
                tripDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector)
                top += 4
                tripDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            }



            tripDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                tripInfo = getTripInfo()
            )
        }
    }

    private fun getTripInfo(): TripInfoDetails {
        metricsCollector.getMetric(namesRegistry.getVehicleSpeedPID())?.let {
            tripInfo.apply { vehicleSpeed = it.value }
        }

        metricsCollector.getMetric(namesRegistry.getAmbientTempPID())?.let {
            tripInfo.apply { ambientTemp = it.value }
        }

        metricsCollector.getMetric(namesRegistry.getAtmPressurePID())?.let {
            tripInfo.apply { atmPressure = it.value }
        }

        return tripInfo
    }

    private fun getArea(area: Rect, canvas: Canvas, margin: Int): Rect {
        val newArea = Rect()
        if (area.isEmpty) {
            newArea[0 + margin, viewSettings.marginTop, canvas.width - 1 - margin] = canvas.height - 1
        } else {
            val width = canvas.width - 1 - (margin)
            newArea[area.left + margin, area.top + viewSettings.marginTop, width] = canvas.height
        }
        return newArea
    }


    override fun recycle() {
        tripDrawer.recycle()
    }

    init {
        applyMetricsFilter(Query.instance(QueryStrategyType.TRIP_INFO_QUERY))
    }
}