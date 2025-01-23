 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.ui.graph

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import org.obd.graphs.R
import org.obd.graphs.ValueConverter
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

private const val SEARCH_SCOPE = 300
private const val LOG_KEY = "MarkerWindow"

class MarkerWindow(
    context: Context?,
    layoutResource: Int,
    private val chart: LineChart,
) : MarkerView(context, layoutResource) {
    private val valueConverter = ValueConverter()

    override fun refreshContent(
        e: Entry,
        highlight: Highlight?,
    ) {
        val metrics = findClosestMetrics(e)
        val adapter = MarkerWindowViewAdapter(context, metrics)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF = MPPointF.getInstance(-(width / 2).toFloat(), -height.toFloat())

    private fun findClosestMetrics(e: Entry): MutableCollection<ObdMetric> {
        val metricsMap = mutableMapOf<Long, ObdMetric>()
        e.data?.let {
            metricsMap[(e.data.toString().toLong())] =
                buildMetrics((e.data.toString().toLong()), e.y)
        }

        var time = System.currentTimeMillis()
        chart.data.dataSets.forEach {
            var x = 0

            do {
                val entriesForXValue = it.getEntriesForXValue(e.x + x)
                if (entriesForXValue.isNotEmpty()) {
                    entriesForXValue.forEach { entry ->
                        e.data?.let {
                            val id = entry.data.toString().toLong()
                            metricsMap[id] =
                                buildMetrics(id, entry.y)
                        }
                    }
                } else {
                    if (x > SEARCH_SCOPE) {
                        Log.d(LOG_KEY, "Did not find entry for=${it.label}.")
                        break
                    }
                    x = updateXValue(x)
                }
            } while (entriesForXValue.isEmpty())
        }
        time = System.currentTimeMillis() - time
        Log.d(LOG_KEY, "Build map, time: ${time}ms , values: ${metricsMap.values}.")
        return metricsMap.values.sortedBy { i -> i.command.pid.description }.toMutableList()
    }

    private fun updateXValue(x: Int): Int {
        var x1 = x
        when {
            x1 == 0 -> {
                x1++
            }
            x1 < 0 -> {
                x1 *= -1
                x1++
            }
            else -> {
                x1 *= -1
            }
        }
        return x1
    }

    private fun buildMetrics(
        id: Long,
        v: Float,
    ): ObdMetric {
        val pidRegistry: PidDefinitionRegistry = dataLogger.getPidDefinitionRegistry()
        val pid = pidRegistry.findBy(id)
        val value = valueConverter.scaleToPidRange(pid, v)
        return ObdMetric
            .builder()
            .command(ObdCommand(pid))
            .value(value)
            .build()
    }
}
