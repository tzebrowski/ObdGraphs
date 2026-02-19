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
package org.obd.graphs.renderer.drag

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.drag.DragRacingService
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.ScreenSettings

private const val LOG_TAG = "DragRacingSurfaceRenderer"

internal data class DragRaceDetails(
    var ambientTemp: Metric? = null,
    var atmPressure: Metric? = null,
    var intakePressure: Metric? = null,
    var torque: Metric? = null,
    var gas: Metric? = null,
    var vehicleSpeed: Metric? = null,
)

internal class DragRacingSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private val dragRaceDetails = DragRaceDetails()
    private val dragRacingDrawer = DragRacingDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        metricsCollector.applyFilter(
            enabled = query.getIDs(),
        )
    }

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
        drawArea?.let {
            val dragRaceResults = DragRacingService.registry.getResult()
            dragRacingDrawer.drawBackground(canvas, it)

            val margin =
                if (settings.getDragRacingScreenSettings().shiftLightsEnabled || dragRaceResults.readyToRace) {
                    SHIFT_LIGHTS_WIDTH
                } else {
                    0
                }
            val area = getArea(it, canvas, margin)
            var top = getTop(area)
            var left = dragRacingDrawer.getMarginLeft(area.left.toFloat())

            left += 5

            if (settings.isStatusPanelEnabled()) {
                dragRacingDrawer.drawStatusPanel(
                    canvas,
                    top,
                    left,
                    fps,
                    metricsCollector,
                    drawContextInfo = settings.getDragRacingScreenSettings().displayMetricsExtendedEnabled,
                )

                top += MARGIN_TOP
                dragRacingDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            dragRacingDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                pTop = top,
                dragRacingResults = dragRaceResults,
                dragRaceDetails =
                    dragRaceDetails.apply {
                        gas = metricsCollector.getMetric(Pid.GAS_PID_ID)
                        ambientTemp = metricsCollector.getMetric(Pid.AMBIENT_TEMP_PID_ID)
                        atmPressure = metricsCollector.getMetric(Pid.ATM_PRESSURE_PID_ID)
                        torque = metricsCollector.getMetric(Pid.ENGINE_TORQUE_PID_ID)
                        intakePressure = metricsCollector.getMetric(Pid.INTAKE_PRESSURE_PID_ID)
                        vehicleSpeed =
                            metricsCollector.getMetric(
                                if (dataLoggerSettings.instance().gmeExtensionsEnabled) {
                                    Pid.EXT_VEHICLE_SPEED_PID_ID
                                } else {
                                    Pid.VEHICLE_SPEED_PID_ID
                                },
                            )
                    },
            )
        }
    }

    override fun recycle() {
        dragRacingDrawer.recycle()
    }

    init {
        Log.i(LOG_TAG, "Init Drag Racing Surface Renderer")
        applyMetricsFilter(Query.instance(QueryStrategyType.DRAG_RACING_QUERY))
    }
}
