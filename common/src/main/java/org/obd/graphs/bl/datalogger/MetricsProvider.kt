package org.obd.graphs.bl.datalogger


import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

class MetricsProvider {

    fun findMetrics(ids: Set<Long>) = findMetrics(ids, emptyMap())

    fun findMetrics(ids: Set<Long>, order: Map<Long, Int>?): MutableList<ObdMetric> {
        val metrics = buildMetrics(ids)
        order?.let { sortOrder ->
            metrics.sortWith { m1: ObdMetric, m2: ObdMetric ->
                if (sortOrder.containsKey(m1.command.pid.id) && sortOrder.containsKey(
                        m2.command.pid.id
                    )
                ) {
                    sortOrder[m1.command.pid.id]!!
                        .compareTo(sortOrder[m2.command.pid.id]!!)
                } else {
                    -1
                }
            }
        }

        return metrics
    }

    private fun buildMetrics(ids: Set<Long>): MutableList<ObdMetric> {
        val pidRegistry: PidDefinitionRegistry = dataLogger.pidDefinitionRegistry()
        val histogramSupplier = dataLogger.diagnostics().histogram()

        return ids.mapNotNull {
            pidRegistry.findBy(it)?.let { pid ->
                val histogram = histogramSupplier.findBy(pid)
                ObdMetric.builder().command(ObdCommand(pid)).value(histogram?.latestValue).build()
            }
        }.toMutableList()
    }
}