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
package org.obd.graphs.renderer.drag

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.drag.DragRacingResults
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.query.isVehicleSpeed
import org.obd.graphs.renderer.*


@Suppress("NOTHING_TO_INLINE")
internal class DragRacingSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    viewSettings: ViewSettings
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector, viewSettings) {

    private val dragRacingDrawer = DragRacingDrawer(context, settings)
    override fun applyMetricsFilter(query: Query) {
        metricsCollector.applyFilter(
            enabled = query.getIDs()
       )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { it ->

            val dragRaceResults = dragRacingResultRegistry.getResult()
            dragRacingDrawer.drawBackground(canvas, it)

            val dragRacingSettings = settings.getDragRacingSettings()
            val margin = if (dragRacingSettings.shiftLightsEnabled || dragRaceResults.readyToRace) SHIFT_LIGHTS_WIDTH else 0
            val area = getArea(it, canvas, margin)
            var top = getDrawTop(area)
            var left = dragRacingDrawer.getMarginLeft(area.left.toFloat())

            if (dragRacingSettings.shiftLightsEnabled) {
                dragRacingResultRegistry.setShiftLightsRevThreshold(dragRacingSettings.shiftLightsRevThreshold)
                // permanent white boxes
                dragRacingDrawer.drawShiftLights(canvas, area, blinking = false)
            }

            if (isShiftLight(dragRaceResults)) {
                dragRacingDrawer.drawShiftLights(canvas, area, blinking = true)
            }

            if (dragRaceResults.readyToRace){
                dragRacingDrawer.drawShiftLights(canvas, area, color = Color.GREEN, blinking = true)
            }

            left += 5

            if (settings.isStatusPanelEnabled()) {
                dragRacingDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector)
                top += 4
                dragRacingDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            }

            metricsCollector.getMetrics().firstOrNull { it.source.isVehicleSpeed() }?.let {
                top = dragRacingDrawer.drawVehicleSpeed(
                    canvas = canvas,
                    area = area,
                    metric = it,
                    left = left,
                    top = top
                )
            }

            dragRacingDrawer.drawDragRaceResults(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                dragRacingResults = dragRaceResults)
        }
    }

    private fun getArea(area: Rect, canvas: Canvas, margin: Int) : Rect {
        val newArea = Rect()
        if (area.isEmpty) {
            newArea[0 + margin, viewSettings.marginTop, canvas.width - 1 - margin] = canvas.height - 1
        } else {
            val width = canvas.width - 1 - (margin)
            newArea[area.left + margin, area.top + viewSettings.marginTop, width] = canvas.height
        }
        return newArea
    }

    private fun isShiftLight(dragRaceResults: DragRacingResults) =
        settings.getDragRacingSettings().shiftLightsEnabled && dragRaceResults.enableShiftLights

    override fun recycle() {
        dragRacingDrawer.recycle()
    }

    init {
        applyMetricsFilter(Query.instance(QueryStrategyType.DRAG_RACING_QUERY))
    }
}