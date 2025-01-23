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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.Query
import org.obd.graphs.renderer.drag.DragRacingSurfaceRenderer
import org.obd.graphs.renderer.dynamic.PerformanceSurfaceRenderer
import org.obd.graphs.renderer.gauge.GaugeSurfaceRenderer
import org.obd.graphs.renderer.giulia.GiuliaSurfaceRenderer
import org.obd.graphs.renderer.trip.TripInfoSurfaceRenderer


interface Identity {
    fun id(): Int
}

enum class SurfaceRendererType(private val code: Int) : Identity {
    GIULIA(0),
    GAUGE(4),
    DRAG_RACING(1),
    TRIP_INFO(3),
    PERFORMANCE(5);

    override fun id(): Int  = this.code

    companion object {
        fun fromInt(value: Int) = SurfaceRendererType.values().first { it.code == value }
    }
}

data class ViewSettings(var marginTop: Int = 0)

interface SurfaceRenderer {
    fun applyMetricsFilter(query: Query)
    fun onDraw(canvas: Canvas, drawArea: Rect?)
    fun recycle()

    companion object {
        fun allocate(
            context: Context,
            settings: ScreenSettings,
            metricsCollector: MetricsCollector,
            fps: Fps,
            surfaceRendererType: SurfaceRendererType = SurfaceRendererType.GIULIA,
            viewSettings: ViewSettings = ViewSettings()
        ): SurfaceRenderer =
            when (surfaceRendererType) {
                SurfaceRendererType.GAUGE -> GaugeSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.GIULIA -> GiuliaSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.DRAG_RACING -> DragRacingSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.TRIP_INFO -> TripInfoSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.PERFORMANCE -> PerformanceSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
            }
    }
}