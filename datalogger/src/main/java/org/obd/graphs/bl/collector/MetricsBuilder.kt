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


import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

class MetricsBuilder {

    fun buildDiff(metric: Metric): Metric =
        buildFor(
            ObdMetric.builder()
                .command(metric.source.command)
                .value(
                    if (metric.source.value == null) {
                        null
                    } else {
                        metric.max - metric.min
                    }
                ).build()
        )


    fun buildFor(obdMetric: ObdMetric): Metric {
        val histogramSupplier = dataLogger.getDiagnostics().histogram()
        val histogram = histogramSupplier.findBy(obdMetric.command.pid)
        return Metric
            .newInstance(
                min = histogram?.min ?: 0.0,
                max = histogram?.max ?: 0.0,
                mean = histogram?.mean ?: 0.0,
                value = histogram?.latestValue ?: 0,
                source = obdMetric
            )
    }

    fun buildFor(ids: Set<Long>) = buildFor(ids, emptyMap())

    fun buildFor(ids: Set<Long>, sortOrder: Map<Long, Int>?): MutableList<Metric> {
        val metrics = buildMetrics(ids)
        sortOrder?.let { order ->
            metrics.sortWith { m1: Metric, m2: Metric ->
                if (order.containsKey(m1.source.command.pid.id) && order.containsKey(
                        m2.source.command.pid.id
                    )
                ) {
                    order[m1.source.command.pid.id]!!
                        .compareTo(order[m2.source.command.pid.id]!!)
                } else {
                    -1
                }
            }
        }

        return metrics
    }


    private fun buildMetrics(ids: Set<Long>): MutableList<Metric> {
        val pidRegistry: PidDefinitionRegistry = dataLogger.getPidDefinitionRegistry()
        val histogramSupplier = dataLogger.getDiagnostics().histogram()

        return ids.mapNotNull {
            pidRegistry.findBy(it)?.let { pid ->
                val histogram = histogramSupplier.findBy(pid)
                Metric
                    .newInstance(
                        min = histogram?.min ?: 0.0,
                        max = histogram?.max ?: 0.0,
                        mean = histogram?.mean ?: 0.0,
                        value = histogram?.latestValue ?: 0,
                        source = ObdMetric.builder()
                            .command(ObdCommand(pid))
                            .value(histogram?.latestValue).build()
                    )
            }
        }.toMutableList()
    }
}
