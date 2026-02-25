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
        val allMetrics = metricsCollector.getMetrics(enabled = true)
        gasMetric = metricsCollector.getMetric(settings.breakBoostingSettings.getGasMetric())
        torqueMetric = metricsCollector.getMetric(settings.breakBoostingSettings.getTorqueMetric())

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "lastBottomIds=$lastBottomIds")
            Log.v(TAG, "lastTopIds=$lastTopIds")
            Log.v(TAG, "allMetrics=${allMetrics.map { it.pid.id }}")
            Log.v(TAG, "topMetrics=${topMetrics.map { it.pid.id }}")
            Log.v(TAG, "bottomMetrics=${bottomMetrics.map { it.pid.id }}")
        }

        val currentBottomMetrics = settings.getBottomMetrics()
        val currentTopMetrics = settings.getTopMetrics()

        if (allMetrics.size == cachedMetrics.size &&
            currentBottomMetrics == lastBottomIds &&
            currentTopMetrics == lastTopIds
        ) {
            return
        }

        cachedMetrics.clear()
        cachedMetrics.addAll(allMetrics)
        lastBottomIds = currentBottomMetrics
        lastTopIds = currentTopMetrics


        bottomMetrics.clear()
        for (id in currentBottomMetrics) {
            val metric = metricsCollector.getMetric(id)
            if (metric != null && allMetrics.contains(metric)) {
                bottomMetrics.add(metric)
            }
        }

        topMetrics.clear()
        for (id in currentTopMetrics) {
            val metric = metricsCollector.getMetric(id)
            if (metric != null && allMetrics.contains(metric)) {
                topMetrics.add(metric)
            }
        }
    }
}
