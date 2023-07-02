package org.obd.graphs.aa

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric

private const val LOG_KEY = "MetricsCollector"

internal val metricsCollector = CarMetricsCollector()

internal class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()

    fun metrics() = metrics.values.filter { it.enabled }

    fun configure() {
        val selectedPIDs = carSettings.getSelectedPIDs()
        val pidsToQuery = dataLoggerPreferences.getPIDsToQuery()

        if (metrics.isEmpty() || metrics.size !=pidsToQuery.size){
            Log.i(LOG_KEY, "Rebuilding metrics configuration for: $pidsToQuery")
            metrics = MetricsProvider().findMetrics(pidsToQuery).associate {
                it.command.pid.id to CarMetric.newInstance(it)
            }.toMutableMap()
        }
        Log.i(LOG_KEY, "Updating visible metrics for: $selectedPIDs")
        metrics.forEach { (t, u) ->
            u.enabled = selectedPIDs.contains(t)
        }
    }

    fun append(input: ObdMetric?) {
        input?.let { metric ->

            metrics[metric.command.pid.id]?.let {
                it.value = input.valueToDouble()
                val hist = dataLogger.diagnostics().histogram().findBy(metric.command.pid)

                hist.mean?.let { mean ->
                    it.avg = mean
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