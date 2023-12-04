/**
 * Copyright 2019-2023, Tomasz Żebrowski
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

internal class InMemoryCarMetricsCollector : CarMetricsCollector {

    private var metrics: SortedMap<Long, CarMetric> = TreeMap()

    override fun getMetrics(enabled: Boolean): List<CarMetric> = metrics.values.filter { it.enabled == enabled }

    override fun findById(id: Long): CarMetric? = getMetrics().firstOrNull { it.source.command.pid.id == id }

    override fun applyFilter(enabled: Set<Long>, order: Map<Long, Int>?) {

        if (metrics.isEmpty() || !metrics.keys.containsAll(enabled)) {

            if (Log.isLoggable(LOG_KEY, Log.DEBUG)) {
                Log.d(LOG_KEY, "Rebuilding metrics configuration for: $enabled != ${metrics.keys}")
            }
            CarMetricsBuilder().buildFor(enabled).forEach {
                val key = it.source.command.pid.id

                if (metrics.keys.indexOf(key)  ==-1) {
                    if (Log.isLoggable(LOG_KEY, Log.DEBUG)) {
                        Log.d(LOG_KEY, "Adding PID with id=$key to metrics map")
                    }
                    metrics[key] = it
                }
            }
        } else {
            if (Log.isLoggable(LOG_KEY, Log.DEBUG)) {
                Log.d(LOG_KEY, "Its okay. All PIDs are available. Metrics ${metrics.keys} contains $enabled")
            }
        }

        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
            Log.v(LOG_KEY, "Updating visible metrics for: $enabled")
        }

        order?.let {
            if (order.isNotEmpty()) {
                metrics = metrics.toSortedMap(comparator(it))
                Log.i(LOG_KEY, "Applied metrics sort order=$order")
            }
        }


        metrics.forEach { (k, v) ->
            v.enabled = enabled.contains(k)
        }

    }

    override fun append(input: ObdMetric?) {

        input?.let { metric ->
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
    private fun comparator(it: Map<Long, Int>): java.util.Comparator<Long> = Comparator { m1, m2 ->
        if (it.containsKey(m1) && it.containsKey(m2)) {
            it[m1]!!.compareTo(it[m2]!!)
        } else {
            -1
        }
    }
}