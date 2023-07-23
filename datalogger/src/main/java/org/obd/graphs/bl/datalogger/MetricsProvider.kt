package org.obd.graphs.bl.datalogger


import org.obd.graphs.bl.collector.CarMetric
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

class MetricsProvider {
    fun findMetrics(ids: Set<Long>) = findMetrics(ids, emptyMap())

    fun findMetrics(ids: Set<Long>, order: Map<Long, Int>?): MutableList<CarMetric> {
        val metrics = buildMetrics(ids)
        order?.let { sortOrder ->
            metrics.sortWith { m1: CarMetric, m2: CarMetric ->
                if (sortOrder.containsKey(m1.source.command.pid.id) && sortOrder.containsKey(
                        m2.source.command.pid.id
                    )
                ) {
                    sortOrder[m1.source.command.pid.id]!!
                        .compareTo(sortOrder[m2.source.command.pid.id]!!)
                } else {
                    -1
                }
            }
        }

        return metrics
    }

    private fun buildMetrics(ids: Set<Long>): MutableList<CarMetric> {
        val pidRegistry: PidDefinitionRegistry = dataLogger.getPidDefinitionRegistry()
        val histogramSupplier = dataLogger.getDiagnostics().histogram()

        return ids.mapNotNull {
            pidRegistry.findBy(it)?.let { pid ->
                val histogram = histogramSupplier.findBy(pid)
                CarMetric.newInstance(ObdMetric.builder().command(ObdCommand(pid)).value(histogram?.latestValue).build())
            }
        }.toMutableList()
    }
}