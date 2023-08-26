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