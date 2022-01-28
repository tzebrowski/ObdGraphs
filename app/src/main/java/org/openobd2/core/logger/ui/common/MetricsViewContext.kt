package org.openobd2.core.logger.ui.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator

class MetricsViewContext (private val owner: LifecycleOwner, private val visiblePids:  Set<Long>)  {
    lateinit var adapter: RecyclerView.Adapter<*>

    fun findMetricsToDisplay(sortOrder: Map<Long, Int>): MutableList<ObdMetric> {

        val metrics = DataLogger.INSTANCE.getEmptyMetrics(visiblePids)

        metrics.sortWith(Comparator { m1: ObdMetric, m2: ObdMetric ->
            if (sortOrder.containsKey(m1.command.pid.id) && sortOrder.containsKey(
                    m2.command.pid.id
                )
            ) {
                sortOrder[m1.command.pid.id]!!
                    .compareTo(sortOrder[m2.command.pid.id]!!)
            } else {
                -1
            }
        })
        return metrics
    }

    fun observerMetrics(
        data: MutableList<ObdMetric>
    ) {
        val visibleMetrics = visiblePids
        MetricsAggregator.metrics.observe(owner, Observer {
            it?.let {
                if (visibleMetrics.contains(it.command.pid.id)) {
                    val indexOf = data.indexOf(it)
                    if (indexOf == -1) {
                        data.add(it)
                        adapter.notifyItemInserted(data.indexOf(it))
                    } else {
                        data[indexOf] = it
                        adapter.notifyItemChanged(indexOf, it)
                    }
                }
            }
        })
    }
}