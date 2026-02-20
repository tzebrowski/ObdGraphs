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
import org.obd.graphs.renderer.api.SurfaceRenderer

abstract class ScreenBehavior {
    protected val query = Query.instance()

    fun getQuery(
        carSettings: CarSettings,
        metricsCollector: MetricsCollector,
    ): Query {
        applyFilters(carSettings = carSettings, metricsCollector)
        return query
    }

    abstract fun getSurfaceRenderer(): SurfaceRenderer

    abstract fun queryStrategyType(): QueryStrategyType

    protected open fun getSelectedPIDs(carSettings: CarSettings): Set<Long> = emptySet()

    protected open fun getSortOrder(carSettings: CarSettings): Map<Long, Int>? = null

    open fun getCurrentVirtualScreen(carSettings: CarSettings): Int = -1

    open fun setCurrentVirtualScreen(
        carSettings: CarSettings,
        id: Int,
    ) {
    }

    protected open fun applyFilters(
        carSettings: CarSettings,
        metricsCollector: MetricsCollector,
    ) {
        query.setStrategy(queryStrategyType())
        val selectedPIDs = getSelectedPIDs(carSettings)
        val sortOrder = getSortOrder(carSettings)

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
