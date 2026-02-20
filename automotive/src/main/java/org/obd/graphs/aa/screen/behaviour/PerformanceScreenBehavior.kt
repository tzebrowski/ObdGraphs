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
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.SurfaceRenderer
import org.obd.graphs.renderer.api.SurfaceRendererType

internal class PerformanceScreenBehavior(
    carContext: Context,
    metricsCollector: MetricsCollector,
    carSettings: CarSettings,
    fps: Fps,
) : ScreenBehavior() {
    override fun queryStrategyType() = QueryStrategyType.PERFORMANCE_QUERY

    private val surfaceRenderer =
        SurfaceRenderer.allocate(
            carContext,
            carSettings,
            metricsCollector,
            fps,
            surfaceRendererType = SurfaceRendererType.PERFORMANCE,
        )

    override fun getSurfaceRenderer(): SurfaceRenderer = surfaceRenderer

    override fun applyFilters(
        carSettings: CarSettings,
        metricsCollector: MetricsCollector,
    ) {
        query.setStrategy(queryStrategyType())
        metricsCollector.applyFilter(enabled = query.getIDs())
        query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
    }
}
