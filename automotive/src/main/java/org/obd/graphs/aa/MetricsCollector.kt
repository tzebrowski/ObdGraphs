package org.obd.graphs.aa

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric

val metricsCollector = CarMetricsCollector()

class CarMetricsCollector {

    private var metrics: MutableMap<Long, CarMetric> = mutableMapOf()
    private val histogram by lazy { dataLogger.diagnostics().histogram() }

    fun metrics() = metrics.values.filter { it.enabled }

    fun configure() {
        val ids = carScreenSettings.getSelectedPIDs()
        Log.i(LOG_KEY, "Rebuilding metrics configuration for: $ids")
        val newMetrics = MetricsProvider().findMetrics(ids)
        if (metrics.isEmpty()){
            metrics = newMetrics.associate {
                it.command.pid.id to toCarMetric(it)
            }.toMutableMap()
        } else {
            //append new
            newMetrics.forEach {
                if (!metrics.containsKey(it.command.pid.id)){
                    metrics[it.command.pid.id] = toCarMetric(it)
                }
            }
            metrics.forEach { (t, u) ->
                u.enabled = ids.contains(t)
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

    private fun toCarMetric(it: ObdMetric) = CarMetric(it.command.pid, null, 0.0, 0.0, 0.0)

}