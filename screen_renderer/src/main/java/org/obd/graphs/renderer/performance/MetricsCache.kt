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
package org.obd.graphs.renderer.performance

import android.util.Log
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.api.PerformanceScreenSettings

private const val TAG = "MetricCache"

internal class MetricsCache {
    val bottomMetrics = mutableListOf<Metric>()
    val topMetrics = mutableListOf<Metric>()

    var gasMetric: Metric? = null
    var torqueMetric: Metric? = null

    private val cachedMetrics = mutableListOf<Metric>()
    private var lastBottomIds: List<Long> = emptyList()
    private var lastTopIds: List<Long> = emptyList()

    fun update(
        settings: PerformanceScreenSettings,
        metricsCollector: MetricsCollector,
    ) {
        val allMetrics = metricsCollector.getMetrics()
        gasMetric = metricsCollector.getMetric(settings.brakeBoostingSettings.getGasMetric())
        torqueMetric = metricsCollector.getMetric(settings.brakeBoostingSettings.getTorqueMetric())

        val currentBottomMetrics = settings.bottomMetrics
        val currentTopMetrics = settings.topMetrics

        val cacheHit =
            allMetrics.size == cachedMetrics.size &&
                currentBottomMetrics == lastBottomIds &&
                currentTopMetrics == lastTopIds

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "--------------------------------------------------------------")
            Log.v(TAG, "LastBottomIds=$lastBottomIds")
            Log.v(TAG, "LastTopIds=$lastTopIds")
            Log.v(TAG, "AllMetrics=${allMetrics.map { it.pid.id }}")
            Log.v(TAG, "TopMetrics=${topMetrics.map { it.pid.id }}")
            Log.v(TAG, "BottomMetrics=${bottomMetrics.map { it.pid.id }}")
            Log.v(TAG, "HiddenMetrics=${settings.hiddenMetrics}")
            Log.v(TAG, "Cache hit=$cacheHit")
        }

        if (cacheHit) {
            return
        }

        cachedMetrics.clear()
        cachedMetrics.addAll(allMetrics)
        lastBottomIds = currentBottomMetrics
        lastTopIds = currentTopMetrics

        val hiddenSet = settings.hiddenMetrics
        val allMetricsSet = allMetrics.toSet()

        bottomMetrics.clear()

        for (i in 0 until currentBottomMetrics.size) {
            val id = currentBottomMetrics[i]
            if (!hiddenSet.contains(id)) {
                val metric = metricsCollector.getMetric(id)
                if (metric != null && allMetricsSet.contains(metric)) {
                    bottomMetrics.add(metric)
                }
            }
        }

        topMetrics.clear()
        for (i in 0 until currentTopMetrics.size) {
            val id = currentTopMetrics[i]
            if (!hiddenSet.contains(id)) {
                val metric = metricsCollector.getMetric(id)
                if (metric != null && allMetricsSet.contains(metric)) {
                    topMetrics.add(metric)
                }
            }
        }
    }
}
