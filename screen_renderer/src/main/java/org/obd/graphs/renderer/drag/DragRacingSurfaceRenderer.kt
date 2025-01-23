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
package org.obd.graphs.renderer.drag

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.drag.DragRacingResults
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.query.isGMEExtensionsEnabled
import org.obd.graphs.bl.query.namesRegistry
import org.obd.graphs.renderer.*
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_ECO

private const val LOG_TAG = "DragRacingSurfaceRenderer"

internal class DragRacingSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {

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

            val dragRacingSettings = settings.getDragRacingScreenSettings()
            val margin = if (dragRacingSettings.shiftLightsEnabled || dragRaceResults.readyToRace) SHIFT_LIGHTS_WIDTH else 0
            val area = getArea(it, canvas, margin)
            var top = getTop(area)
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
                dragRacingDrawer.drawShiftLights(canvas, area, color = COLOR_DYNAMIC_SELECTOR_ECO, blinking = true)
            }

            left += 5

            if (settings.isStatusPanelEnabled()) {
                dragRacingDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = settings.getDragRacingScreenSettings().contextInfoEnabled)
                top += MARGIN_TOP
                dragRacingDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            if (isGMEExtensionsEnabled()){
                val width =  (area.width() / 2f ) - 10
                metricsCollector.getMetric(namesRegistry.getVehicleSpeedPID())?.let {
                    dragRacingDrawer.drawMetric(
                        canvas = canvas,
                        area = area,
                        metric = it,
                        left = left,
                        top = top,
                        width = width
                    )
                }

                metricsCollector.getMetric(namesRegistry.getMeasuredIntakePressurePID())?.let {
                    top = dragRacingDrawer.drawMetric(
                        canvas = canvas,
                        area = area,
                        metric = it,
                        left = left + width,
                        top = top,
                        width = width
                    )
                }

            } else {
                metricsCollector.getMetric(namesRegistry.getVehicleSpeedPID())?.let {
                    top = dragRacingDrawer.drawMetric(
                        canvas = canvas,
                        area = area,
                        metric = it,
                        left = left,
                        top = top
                    )
                }
            }

            dragRacingDrawer.drawDragRaceResults(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                dragRacingResults = dragRaceResults)
        }
    }

    private fun isShiftLight(dragRaceResults: DragRacingResults) =
        settings.getDragRacingScreenSettings().shiftLightsEnabled && dragRaceResults.enableShiftLights

    override fun recycle() {
        dragRacingDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG,"Init Drag Racing Surface Renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.DRAG_RACING_QUERY))
    }
}