/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
package org.obd.graphs.bl.collector

import android.util.Log
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric
import java.util.*
import kotlin.Comparator

private const val LOG_KEY = "InMemoryCollector"

internal class InMemoryCarMetricsCollector : MetricsCollector {

    private var metrics: SortedMap<Long, Metric> = TreeMap()
    private val metricBuilder = MetricsBuilder()

    override fun getMetrics(enabled: Boolean): List<Metric> = metrics.values.filter { it.enabled == enabled }

    override fun getMetric(id: Long, enabled: Boolean): Metric?   =
        if (metrics.containsKey(id) && metrics[id]!!.enabled) {
            metrics[id]
        } else {
            null
        }


    override fun applyFilter(enabled: Set<Long>, order: Map<Long, Int>?) {
        Log.i(LOG_KEY, "Updating visible PIDs=$enabled with order=$order")

        if (metrics.isEmpty() || !metrics.keys.containsAll(enabled)) {

            if (Log.isLoggable(LOG_KEY, Log.DEBUG)) {
                Log.d(LOG_KEY, "Rebuilding metrics configuration for: $enabled != ${metrics.keys}")
            }
            metricBuilder.buildFor(enabled).forEach {
                val key = it.pid().id

                if (metrics.keys.indexOf(key)  ==-1) {
                    Log.i(LOG_KEY, "Adding PID($key = ${it.pid().description}) to metrics map.")
                    metrics[key] = it
                }
            }
        } else {
            if (Log.isLoggable(LOG_KEY, Log.DEBUG)) {
                Log.d(LOG_KEY, "Its okay. All PIDs are available. Metrics ${metrics.keys} contains $enabled")
            }
        }

        if (order != null && order.isNotEmpty()) {
            metrics = metrics.toSortedMap(comparator(order))
        }

        metrics.forEach { (k, v) ->
            v.enabled = enabled.contains(k)
        }
    }

    override fun append(input: ObdMetric?, forceAppend: Boolean) {

        input?.let { metric ->
            val key = metric.command.pid.id

            if (forceAppend && !metrics.containsKey(key)) {
                metrics[key] = metricBuilder.buildFor(metric)
                Log.i(LOG_KEY, "Adding PID($key = ${metric.command.pid.description}) to metrics map.")
            }

            metrics[metric.command.pid.id]?.let {
                it.source = metric

                it.value = metric.valueToDouble()
                val hist = dataLogger.findHistogramFor(metric)
                val rate = dataLogger.findRateFor(metric)

                rate.ifPresent { r ->
                    it.rate = r.value
                }

                hist.mean?.let { mean ->
                    it.mean = mean
                }

                hist.max.let { max ->
                    it.max = max
                }

                hist.min.let { min ->
                    it.min = min
                }
            }
        }
    }
    private fun comparator(order: Map<Long, Int>): Comparator<Long> = Comparator { m1, m2 ->
        if (order.containsKey(m1) && order.containsKey(m2)) {
            order[m1]!!.compareTo(order[m2]!!)
        } else {
            m1.compareTo(m2)
        }
    }
}