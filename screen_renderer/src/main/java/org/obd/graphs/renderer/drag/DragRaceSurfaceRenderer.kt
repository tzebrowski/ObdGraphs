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
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.VEHICLE_SPEED_PID_ID
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRendererType

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

@Suppress("NOTHING_TO_INLINE")
internal class DragRaceSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractSurfaceRenderer(settings, context, fps) {

    private val valueScaler = ValueScaler()
    private val drawer = Drawer(context, settings)

    override fun getType(): SurfaceRendererType = SurfaceRendererType.DRAG_RACE

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val (valueTextSize, textSizeBase) = calculateFontSize(area)

            drawer.drawBackground(canvas, area)

            var top = area.top + textSizeBase
            val left = drawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                top = drawer.drawStatusBar(canvas, area.top.toFloat() + 6f, area.left.toFloat(), fps) + 18
                drawer.drawDivider(canvas, left, area.width().toFloat(), area.top + 10f, Color.DKGRAY)
            }

            val metric = metricsCollector.metrics().firstOrNull { it.source.command.pid.id == VEHICLE_SPEED_PID_ID }
            metric?.let {
                top = drawer.drawMetric(
                    canvas = canvas,
                    area = area,
                    metric = metric,
                    textSizeBase = textSizeBase,
                    valueTextSize = valueTextSize,
                    left = left,
                    top = top,
                )
            }

            drawer.drawDragRaceResults(canvas = canvas, area = area, left = left, top = top, dataLogger.getDragRaceResults())
        }
    }

    override fun release() {
        drawer.recycle()
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(settings.getFontSize().toFloat(), CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = area.width()

        val valueTextSize = (areaWidth / 20f) * scaleRatio
        val textSizeBase = (areaWidth / 26f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }

    init {
        metricsCollector.applyFilter(
            selectedPIDs = setOf(VEHICLE_SPEED_PID_ID),
            pidsToQuery = setOf(VEHICLE_SPEED_PID_ID)
        )
    }
}