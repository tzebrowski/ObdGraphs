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
package org.obd.graphs.bl.collector

import android.util.Log
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.Pid
import org.obd.metrics.api.model.ObdMetric
import java.util.concurrent.ConcurrentHashMap

private const val LOG_TAG = "InMemoryCollector"

internal class InMemoryCarMetricsCollector : MetricsCollector {
    private val metrics = ConcurrentHashMap<Long, Metric>()

    @Volatile
    private var visibleMetrics: List<Metric> = emptyList()

    private val metricBuilder = MetricsBuilder()

    override fun getMetrics(enabled: Boolean): List<Metric> = if (enabled) visibleMetrics else metrics.values.filter { !it.enabled }

    override fun reset() {
        metrics.values.forEach {
            it.inLowerAlertRisedHist = false
            it.inUpperAlertRisedHist = false
        }
    }

    override fun getMetric(
        id: Pid,
        enabled: Boolean,
    ): Metric? {
        val metric = metrics[id.id]
        return if (metric != null && metric.enabled == enabled) metric else null
    }

    @Synchronized
    override fun applyFilter(
        enabled: Set<Long>,
        order: Map<Long, Int>?,
    ) {
        Log.i(LOG_TAG, "Updating visible PIDs=$enabled with order=$order")

        val missingPids = enabled.filter { !metrics.containsKey(it) }

        if (missingPids.isNotEmpty()) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Rebuilding metrics configuration for missing PIDs: $missingPids")
            }
            metricBuilder.buildFor(missingPids.toSet()).forEach {
                Log.i(LOG_TAG, "Adding PID(${it.pid.id} = ${it.pid.description}) to metrics map.")
                metrics[it.pid.id] = it
            }
        }

        metrics.forEach { (k, v) ->
            v.enabled = enabled.contains(k)
        }

        val comparator =
            Comparator<Metric> { m1, m2 ->
                if (order != null) {
                    val order1 = order[m1.pid.id] ?: Int.MAX_VALUE
                    val order2 = order[m2.pid.id] ?: Int.MAX_VALUE

                    if (order1 != Int.MAX_VALUE || order2 != Int.MAX_VALUE) {
                        return@Comparator order1.compareTo(order2)
                    }
                }
                m1.pid.id.compareTo(m2.pid.id)
            }

        visibleMetrics =
            metrics.values
                .filter { it.enabled }
                .sortedWith(comparator)

        Log.d(LOG_TAG, "[${Thread.currentThread().id}] Updating visible PIDs: ${visibleMetrics.map { it.pid.id }}")
    }

    override fun append(
        input: ObdMetric?,
        forceAppend: Boolean,
    ) {
        if (input == null) return

        val key = input.command.pid.id

        // Fast O(1) lookup
        if (forceAppend && !metrics.containsKey(key)) {
            metrics[key] = metricBuilder.buildFor(input)
            Log.i(LOG_TAG, "Adding PID($key = ${input.command.pid.description}) to metrics map.")
        }

        // Update the metric properties in place
        metrics[key]?.let { metric ->
            metric.source = input
            metric.value = input.value

            if (input.isLowerAlert) {
                metric.inLowerAlertRisedHist = true
            }

            if (input.isUpperAlert) {
                metric.inUpperAlertRisedHist = true
            }

            DataLoggerRepository.findRateFor(input).ifPresent { r ->
                metric.rate = r.value
            }

            val hist = DataLoggerRepository.findHistogramFor(input)
            hist.mean?.let { mean ->
                metric.mean = mean
            }
            metric.max = hist.max
            metric.min = hist.min
        }
    }
}
