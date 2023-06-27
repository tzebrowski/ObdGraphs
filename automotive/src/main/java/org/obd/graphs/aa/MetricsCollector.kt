package org.obd.graphs.aa

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric

val metricsCollector = CarMetricsCollector()

class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()
    private val histogram by lazy { dataLogger.diagnostics().histogram() }

    fun metrics() = metrics.values

    fun configure() {
        metrics = MetricsProvider().findMetrics(carScreenSettings.getSelectedPIDs()).associate {
            it.command.pid.id to CarMetric(it.command.pid, null, 0.0, 0.0, 0.0)
        }.toMutableMap()

        Log.i(LOG_KEY, "Rebuilding metrics configuration: $metrics")
    }

    fun collect(input: ObdMetric?) {
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