/*
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
package org.obd.graphs.renderer.performance

import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_BOTTOM
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_HIDDEN
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_SELECTED
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_TOP
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongList
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.renderer.api.PerformanceScreenSettings

internal const val PERFORMANCE_SORT_ORDER_PREF_KEY = "$PREF_QUERY_PERFORMANCE_SELECTED.view.settings"
internal const val PERFORMANCE_BOTTOM_SORT_ORDER_PREF_KEY = "$PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED.view.settings"

internal class BrakeBoosting(
    var gasMetric: Metric? = null,
    var arbitraryMetric: Metric? = null,
    var vehicleSpeedMetric: Metric? = null
)

internal class MetricsCache {
    val bottomMetrics = mutableListOf<Metric>()
    val topMetrics = mutableListOf<Metric>()

    val brakeBoosting: BrakeBoosting = BrakeBoosting()

    // The plan is rebuilt only when the queried PIDs change or cacheReset() is called.
    private var plannedIds = LongArray(0)
    private var plan = PerformancePlan(emptyList(), emptyList())

    @Volatile
    private var planOutdated = true

    fun cacheReset() {
        planOutdated = true
    }

    fun update(
        settings: PerformanceScreenSettings,
        metricsCollector: MetricsCollector
    ) {
        brakeBoosting.apply {
            gasMetric = metricsCollector.getMetric(settings.brakeBoostingSettings.getGasMetric())
            arbitraryMetric =
                metricsCollector.getMetric(settings.brakeBoostingSettings.getArbitraryMetric())
            vehicleSpeedMetric =
                metricsCollector.getMetric(settings.brakeBoostingSettings.getVehicleSpeedMetric())
        }

        updatePlan(metricsCollector.getMetrics())
        fill(topMetrics, plan.top, metricsCollector)
        fill(bottomMetrics, plan.bottom, metricsCollector)
    }

    private fun fill(
        target: MutableList<Metric>,
        ids: List<Long>,
        metricsCollector: MetricsCollector
    ) {
        target.clear()
        for (i in ids.indices) {
            metricsCollector.getMetric(ids[i])?.let { target.add(it) }
        }
    }

    // Prefs are read here rather than in a listener, so a rebuild never sees a half-applied change.
    private fun updatePlan(metrics: List<Metric>) {
        if (!planOutdated && plannedIds.size == metrics.size) {
            var same = true
            for (i in metrics.indices) {
                if (plannedIds[i] != metrics[i].pid.id) {
                    same = false
                    break
                }
            }
            if (same) return
        }

        planOutdated = false
        plannedIds = LongArray(metrics.size) { i -> metrics[i].pid.id }
        plan =
            PerformanceMetrics.plan(
                available = plannedIds.toList(),
                profileTop = Prefs.getLongList(PREF_QUERY_PERFORMANCE_TOP),
                profileBottom = Prefs.getLongList(PREF_QUERY_PERFORMANCE_BOTTOM),
                hidden = Prefs.getLongSet(PREF_QUERY_PERFORMANCE_HIDDEN),
                bottomSelection =
                if (Prefs.contains(PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED)) {
                    Prefs.getLongSet(PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED)
                } else {
                    null
                },
                sortOrder = ViewPreferencesSerializer(PERFORMANCE_SORT_ORDER_PREF_KEY).getItemsSortOrder(),
                bottomSortOrder = ViewPreferencesSerializer(PERFORMANCE_BOTTOM_SORT_ORDER_PREF_KEY).getItemsSortOrder()
            )
    }
}
