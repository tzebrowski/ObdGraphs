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
package org.obd.graphs.screen.behaviour

import android.content.Context
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.api.SurfaceRendererType
import org.obd.graphs.renderer.api.VirtualScreenConfig

abstract class VirtualScreenBehavior(
    context: Context,
    metricsCollector: MetricsCollector,
    settings: ScreenSettings,
    fps: Fps,
    surfaceRendererType: SurfaceRendererType,
) : ScreenBehavior(context, metricsCollector, settings, fps, surfaceRendererType) {

    protected abstract val virtualScreenConfig: VirtualScreenConfig

    override fun getCurrentVirtualScreen(): Int = virtualScreenConfig.getVirtualScreen()

    override fun setCurrentVirtualScreen(id: Int) = virtualScreenConfig.setVirtualScreen(id)

    override fun queryStrategyType(): QueryStrategyType =
        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            QueryStrategyType.INDIVIDUAL_QUERY
        } else {
            QueryStrategyType.SHARED_QUERY
        }

    override fun applyFilters(metricsCollector: MetricsCollector) {
        query.setStrategy(queryStrategyType())
        val selectedPIDs = virtualScreenConfig.selectedPIDs
        val sortOrder = virtualScreenConfig.getPIDsSortOrder()

        when (queryStrategyType()) {
            QueryStrategyType.INDIVIDUAL_QUERY -> {
                metricsCollector.applyFilter(enabled = selectedPIDs, order = sortOrder)
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
            }

            QueryStrategyType.SHARED_QUERY -> {
                val queryIds = query.getIDs()
                val intersection = selectedPIDs.filter { queryIds.contains(it) }.toSet()
                metricsCollector.applyFilter(enabled = intersection, order = sortOrder)
            }

            else -> {}
        }
    }
}
