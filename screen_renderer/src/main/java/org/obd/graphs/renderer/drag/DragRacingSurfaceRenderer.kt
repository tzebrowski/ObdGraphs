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
import android.graphics.Paint
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRendererType


private const val SHIFT_LIGHTS_WIDTH = 40f

@Suppress("NOTHING_TO_INLINE")
internal class DragRacingSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector) {

    private val shiftLightPaint = Paint()
    var shiftLghtsCounter = 0

    private val drawer = Drawer(context, settings)
    override fun getType(): SurfaceRendererType = SurfaceRendererType.DRAG_RACING
    override fun applyMetricsFilter() {
        metricsCollector.applyFilter(
            enabled = setOf(dragRacingResultRegistry.getVehicleSpeedPID()),
            query = setOf(dragRacingResultRegistry.getVehicleSpeedPID())
        )
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            val dragRaceResults = dragRacingResultRegistry.getResult()
            var shiftLightsMargin = 0f
            if (settings.getDragRacingSettings().shiftLightsEnabled){
               shiftLightsMargin = SHIFT_LIGHTS_WIDTH
            }

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1 - shiftLightsMargin.toInt()] = canvas.height - 1
            }

            drawer.drawBackground(canvas, area)

            var top = getDrawTop(area)
            var left = drawer.getMarginLeft(area.left.toFloat())

            if (settings.getDragRacingSettings().shiftLightsEnabled && dragRaceResults.enableShiftLights){
                left += SHIFT_LIGHTS_WIDTH
                if (shiftLghtsCounter%2 == 0) {

                    shiftLightPaint.color = Color.WHITE
                    canvas.drawRect(0f,0f, left, area.bottom.toFloat(),shiftLightPaint)
                    canvas.drawRect(area.width() - 1 - shiftLightsMargin,0f, area.width() - 1f,area.bottom.toFloat(), shiftLightPaint)
                }

                shiftLghtsCounter++
            } else {
                shiftLghtsCounter = 0
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
                    top = top,
                    dragRacingResults = dragRaceResults
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

    override fun release() {
        drawer.recycle()
    }


    init {
        applyMetricsFilter()
    }
}