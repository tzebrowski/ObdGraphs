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
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric

private const val  LOG_KEY = "CarMetricsCollector"
class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()

    fun metrics(enabled: Boolean = true) = metrics.values.filter { it.enabled == enabled }

    fun applyFilter(selectedPIDs: Set<Long>) {
        val pidsToQuery = dataLoggerPreferences.getPIDsToQuery()

        if (metrics.isEmpty() || metrics.size != pidsToQuery.size) {
            Log.d(LOG_KEY, "Rebuilding metrics configuration for: $pidsToQuery")
            metrics = CarMetricsBuilder().buildFor(pidsToQuery).associateBy { it.source.command.pid.id }.toMutableMap()
        }
        metrics.forEach { (t, u) ->
            u.enabled = selectedPIDs.contains(t)
        }

        Log.d(LOG_KEY, "Updating visible metrics for: $selectedPIDs")
    }

    fun append(input: ObdMetric?) {
        input?.let { metric ->

            metrics[metric.command.pid.id]?.let {
                it.source = metric

                it.value = metric.valueToDouble()
                val hist = dataLogger.findHistogramFor(metric)

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
}