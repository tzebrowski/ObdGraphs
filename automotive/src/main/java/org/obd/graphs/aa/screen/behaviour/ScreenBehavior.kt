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

import org.obd.graphs.aa.CarSettings
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.Identity
import org.obd.graphs.renderer.api.SurfaceRendererType

interface ScreenBehavior {
    val queryStrategyType: QueryStrategyType

    fun getSelectedPIDs(carSettings: CarSettings): Set<Long> = emptySet()

    fun getSortOrder(carSettings: CarSettings): Map<Long, Int>? = null

    fun getCurrentVirtualScreen(carSettings: CarSettings): Int = -1

    fun setCurrentVirtualScreen(
        carSettings: CarSettings,
        id: Int,
    ) {
    }

    fun applyFilters(
        carSettings: CarSettings,
        metricsCollector: MetricsCollector,
        query: Query,
    ) {
        query.setStrategy(queryStrategyType)

        when (queryStrategyType) {
            QueryStrategyType.INDIVIDUAL_QUERY -> {
                val selectedPIDs = getSelectedPIDs(carSettings)
                metricsCollector.applyFilter(enabled = selectedPIDs, order = getSortOrder(carSettings))
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
            }

            QueryStrategyType.SHARED_QUERY -> {
                val selectedPIDs = getSelectedPIDs(carSettings)
                val queryIds = query.getIDs()
                val intersection = selectedPIDs.filter { queryIds.contains(it) }.toSet()
                metricsCollector.applyFilter(enabled = intersection, order = getSortOrder(carSettings))
            }

            else -> {}
        }
    }

    companion object {
        fun getScreenBehavior(screenId: Identity): ScreenBehavior? =
            when (screenId) {
                SurfaceRendererType.GIULIA -> GiuliaScreenBehavior
                SurfaceRendererType.GAUGE -> GaugeScreenBehavior
                SurfaceRendererType.DRAG_RACING -> DragRacingScreenBehavior
                SurfaceRendererType.PERFORMANCE -> PerformanceScreenBehavior
                SurfaceRendererType.TRIP_INFO -> TripInfoScreenBehavior
                else -> null
            }
    }
}
