package org.obd.graphs.ui.common

import org.obd.graphs.bl.datalogger.DataLogger

import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

internal class MetricsProvider {

    fun findMetrics(ids: Set<Long>, order: Map<Long, Int>?): MutableList<ObdMetric> {
        val metrics = buildEmptyMetrics(ids)
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

    private fun buildEmptyMetrics(ids: Set<Long>): MutableList<ObdMetric> {
        val pidRegistry: PidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()
        return ids.mapNotNull {
            pidRegistry.findBy(it)?.let { pid ->
                ObdMetric.builder().command(ObdCommand(pid)).value(null).build()
            }
        }.toMutableList()
    }
}