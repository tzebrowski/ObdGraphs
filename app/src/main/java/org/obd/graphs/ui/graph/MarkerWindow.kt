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
package org.obd.graphs.ui.graph

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry
import java.util.Locale

private const val SEARCH_SCOPE = 300
private const val LOG_KEY = "MarkerWindow"

data class MarkerMetric(
    val obdMetric: ObdMetric,
    val min: Double,
    val max: Double,
    val mean: Double
)

class MarkerWindow(
    context: Context?,
    layoutResource: Int,
    private val chart: LineChart
) : MarkerView(context, layoutResource) {

    override fun refreshContent(
        e: Entry,
        highlight: Highlight?
    ) {
        findViewById<TextView>(R.id.marker_timestamp)?.text = formatElapsedTime(e.x)

        val metrics = findClosestMetrics(e)
        val adapter = MarkerWindowViewAdapter(context, metrics)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF = MPPointF.getInstance(-(width / 2).toFloat(), -height.toFloat())

    private fun formatElapsedTime(elapsedMs: Float): String {
        val totalSeconds = (elapsedMs / 1000).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun findClosestMetrics(e: Entry): MutableCollection<MarkerMetric> {
        val metricsMap = mutableMapOf<Long, MarkerMetric>()
        e.data?.let {
            metricsMap[(e.data.toString().toLong())] =
                buildMetrics((e.data.toString().toLong()), e.y, listOf(e.y))
        }

        var time = System.currentTimeMillis()
        chart.data.dataSets.forEach { dataSet ->
            var x = 0

            do {
                val entriesForXValue = dataSet.getEntriesForXValue(e.x + x)
                if (entriesForXValue.isNotEmpty()) {
                    entriesForXValue.forEach { entry ->
                        e.data?.let {
                            val id = entry.data.toString().toLong()
                            metricsMap[id] =
                                buildMetrics(id, entry.y, valuesUpTo(dataSet, entry.x))
                        }
                    }
                } else {
                    if (x > SEARCH_SCOPE) {
                        Log.d(LOG_KEY, "Did not find entry for=${dataSet.label}.")
                        break
                    }
                    x = updateXValue(x)
                }
            } while (entriesForXValue.isEmpty())
        }
        time = System.currentTimeMillis() - time
        Log.d(LOG_KEY, "Build map, time: ${time}ms , values: ${metricsMap.values}.")
        return metricsMap.values.sortedBy { i -> i.obdMetric.command.pid.description }.toMutableList()
    }

    // The trip's running min/max/mean up to this point in time - not the trip-wide stats
    // already shown in the side panel, which is why this is computed per-tap rather than reused.
    private fun valuesUpTo(
        dataSet: ILineDataSet,
        x: Float
    ): List<Float> =
        (0 until dataSet.entryCount)
            .map { dataSet.getEntryForIndex(it) }
            .filter { it.x <= x }
            .map { it.y }

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
        rawValuesUpToNow: List<Float>
    ): MarkerMetric {
        val pidRegistry: PidDefinitionRegistry = DataLoggerRepository.getPidDefinitionRegistry()
        val pid = pidRegistry.findBy(id)
        val value = pid.scaleToRange(v)
        val obdMetric =
            ObdMetric
                .builder()
                .command(ObdCommand(pid))
                .value(value)
                .build()

        val values = rawValuesUpToNow.ifEmpty { listOf(v) }
        return MarkerMetric(
            obdMetric = obdMetric,
            min = pid.scaleToRange(values.min()).toDouble(),
            max = pid.scaleToRange(values.max()).toDouble(),
            mean = pid.scaleToRange(values.average().toFloat()).toDouble()
        )
    }
}
