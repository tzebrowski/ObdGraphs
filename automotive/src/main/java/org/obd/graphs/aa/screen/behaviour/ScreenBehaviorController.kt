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
package org.obd.graphs.aa.screen.behaviour

import android.content.Context
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.Identity
import org.obd.graphs.renderer.api.SurfaceRendererType

class ScreenBehaviorController(
    carContext: Context,
    metricsCollector: MetricsCollector,
    carSettings: CarSettings,
    fps: Fps,
) {

    private val gaugeScreenBehavior: GaugeScreenBehavior = GaugeScreenBehavior(carContext, metricsCollector, carSettings, fps)

    private val tripInfoScreenBehavior: TripInfoScreenBehavior = TripInfoScreenBehavior(carContext, metricsCollector, carSettings, fps)

    private val giuliaScreenBehavior: GiuliaScreenBehavior = GiuliaScreenBehavior(carContext, metricsCollector, carSettings, fps)

    private val dragRacingScreenBehavior: DragRacingScreenBehavior =
        DragRacingScreenBehavior(carContext, metricsCollector, carSettings, fps)

    private val performanceScreenBehavior: PerformanceScreenBehavior =
        PerformanceScreenBehavior(carContext, metricsCollector, carSettings, fps)

    fun recycle() {
        gaugeScreenBehavior.getSurfaceRenderer().recycle()
        tripInfoScreenBehavior.getSurfaceRenderer().recycle()
        giuliaScreenBehavior.getSurfaceRenderer().recycle()
        dragRacingScreenBehavior.getSurfaceRenderer().recycle()
        performanceScreenBehavior.getSurfaceRenderer().recycle()
    }

    fun getScreenBehavior(screenId: Identity): ScreenBehavior? =
        when (screenId) {
            SurfaceRendererType.GIULIA -> giuliaScreenBehavior
            SurfaceRendererType.GAUGE -> gaugeScreenBehavior
            SurfaceRendererType.DRAG_RACING -> dragRacingScreenBehavior
            SurfaceRendererType.PERFORMANCE -> performanceScreenBehavior
            SurfaceRendererType.TRIP_INFO -> tripInfoScreenBehavior
            else -> null
        }
}
