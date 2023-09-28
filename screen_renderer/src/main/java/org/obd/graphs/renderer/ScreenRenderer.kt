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
import org.obd.graphs.renderer.gauge.GaugeScreenRenderer
import org.obd.graphs.renderer.giulia.GiuliaScreenRenderer

enum class ScreenRendererType {
    GIULIA, GAUGE
}

interface ScreenRenderer {
    fun onDraw(canvas: Canvas, drawArea: Rect?)

    fun release()

    companion object {
        fun allocate(
            context: Context,
            settings: ScreenSettings,
            metricsCollector: CarMetricsCollector,
            fps: Fps,
            screenRendererType: ScreenRendererType = ScreenRendererType.GIULIA
        ): ScreenRenderer =
            when (screenRendererType) {
                ScreenRendererType.GAUGE -> GaugeScreenRenderer(context, settings, metricsCollector, fps)
                ScreenRendererType.GIULIA -> GiuliaScreenRenderer(context, settings, metricsCollector, fps)
            }
    }
}