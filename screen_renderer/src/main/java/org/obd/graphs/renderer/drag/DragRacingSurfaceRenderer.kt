/**
 * Copyright 2019-2023, Tomasz Żebrowski
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
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.drag.DragRacingResults
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRendererType

@Suppress("NOTHING_TO_INLINE")
internal class DragRacingSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector) {

    private val drawer = Drawer(context, settings)
    override fun getType(): SurfaceRendererType = SurfaceRendererType.DRAG_RACING
    override fun applyMetricsFilter() {
        metricsCollector.applyFilter(
            enabled = setOf(dragRacingResultRegistry.getVehicleSpeedPID()),
            query = setOf(dragRacingResultRegistry.getVehicleSpeedPID())
        )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { it ->

            val dragRaceResults = dragRacingResultRegistry.getResult()
            drawer.drawBackground(canvas, it)

            val margin = if (settings.getDragRacingSettings().shiftLightsEnabled || dragRaceResults.readyToRace) SHIFT_LIGHTS_WIDTH else 0
            val area = getArea(it, canvas, margin)
            var top = getDrawTop(area)
            var left = drawer.getMarginLeft(area.left.toFloat())

            if (settings.getDragRacingSettings().shiftLightsEnabled) {
                dragRacingResultRegistry.setShiftLightsRevThreshold(settings.getDragRacingSettings().shiftLightsRevThreshold)
                // permanent white boxes
                drawer.drawShiftLights(canvas, area, blinking = false)
            }

            if (isShiftLight(dragRaceResults)) {
                drawer.drawShiftLights(canvas, area, blinking = true)
            }

            if (dragRaceResults.readyToRace){
                drawer.drawShiftLights(canvas, area, color = Color.GREEN, blinking = true)
            }

            left += 5

            if (settings.isStatusPanelEnabled()) {
                drawer.drawStatusBar(canvas, top, left, fps)
                top += 4
                drawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            }

            metricsCollector.findById(dragRacingResultRegistry.getVehicleSpeedPID())?.let {
                top = drawer.drawMetric(
                    canvas = canvas,
                    area = area,
                    metric = it,
                    left = left,
                    top = top
                )
            }

            drawer.drawDragRaceResults(
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
            newArea[0 + margin, 0, canvas.width - 1 - margin] = canvas.height - 1
        } else {
            val width = canvas.width - 1 - (margin)
            newArea[area.left + margin, area.top, width] = canvas.height
        }
        return newArea
    }

    private fun isShiftLight(dragRaceResults: DragRacingResults) =
        settings.getDragRacingSettings().shiftLightsEnabled && dragRaceResults.enableShiftLights

    override fun release() {
        drawer.recycle()
    }

    init {
        applyMetricsFilter()
    }
}