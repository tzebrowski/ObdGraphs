package org.obd.graphs.ui.gauge

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric

private const val LOG_KEY = "MetricsCollector"

internal val metricsCollector = CarMetricsCollector()

internal class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()

    fun metrics() = metrics.values.filter { it.enabled }

    fun configure(selectedPIDs: Set<Long>) {
        val pidsToQuery = dataLoggerPreferences.getPIDsToQuery()

        if (metrics.isEmpty() || metrics.size != pidsToQuery.size) {
            Log.i(LOG_KEY, "Rebuilding metrics configuration for: $pidsToQuery")
            metrics = MetricsProvider().findMetrics(pidsToQuery).associate {
                it.command.pid.id to CarMetric.newInstance(it)
            }.toMutableMap()
        }
        metrics.forEach { (t, u) ->
            u.enabled = selectedPIDs.contains(t)
        }

        Log.i(LOG_KEY, "Updating visible metrics for: $selectedPIDs")
    }

    fun append(input: ObdMetric?) {
        input?.let { metric ->
            metrics[metric.command.pid.id]?.let {
                it.value = metric
            }
        }
    }
}