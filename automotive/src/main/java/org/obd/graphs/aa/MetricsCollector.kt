package org.obd.graphs.aa

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric

val metricsCollector = CarMetricsCollector()

class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()
    private val histogram by lazy { dataLogger.diagnostics().histogram() }

    fun metrics() = metrics.values.filter { it.enabled }

    fun configure() {
        val selectedPIDs = carScreenSettings.getSelectedPIDs()
        Log.i(LOG_KEY, "Rebuilding metrics configuration for: $selectedPIDs")
        if (metrics.isEmpty()){
            metrics = MetricsProvider().findMetrics(dataLoggerPreferences.getPIDsToQuery()).associate {
                it.command.pid.id to CarMetric.newInstance(it)
            }.toMutableMap()
        } else {
            metrics.forEach { (t, u) ->
                u.enabled = selectedPIDs.contains(t)
            }
        }
    }

    fun append(input: ObdMetric?) {
        input?.let { metric ->
            metrics[metric.command.pid.id]?.let {
                it.value = input.valueToDouble()
                val hist = histogram.findBy(metric.command.pid)

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