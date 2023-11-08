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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.renderer.drag.DragRacingSurfaceRenderer
import org.obd.graphs.renderer.gauge.GaugeSurfaceRenderer
import org.obd.graphs.renderer.giulia.GiuliaSurfaceRenderer

enum class SurfaceRendererType {
    GIULIA, GAUGE, DRAG_RACING
}

data class ViewSettings(var marginTop: Int = 0)

interface SurfaceRenderer {
    fun applyMetricsFilter()

    fun onDraw(canvas: Canvas, drawArea: Rect?)

    fun release()

    fun getType(): SurfaceRendererType

    companion object {
        fun allocate(
            context: Context,
            settings: ScreenSettings,
            metricsCollector: CarMetricsCollector,
            fps: Fps,
            surfaceRendererType: SurfaceRendererType = SurfaceRendererType.GIULIA,
            viewSettings: ViewSettings = ViewSettings()
        ): SurfaceRenderer =
            when (surfaceRendererType) {
                SurfaceRendererType.GAUGE -> GaugeSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.GIULIA -> GiuliaSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
                SurfaceRendererType.DRAG_RACING -> DragRacingSurfaceRenderer(context, settings, metricsCollector, fps, viewSettings)
            }
    }
}