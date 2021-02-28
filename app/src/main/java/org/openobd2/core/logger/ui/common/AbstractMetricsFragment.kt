package org.openobd2.core.logger.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator

abstract class AbstractMetricsFragment : Fragment() {

    lateinit var root: View
    lateinit var adapter: RecyclerView.Adapter<*>

    abstract fun getVisibleMetrics(): Set<Long>

    protected fun findMetrics(sortOrder: Map<Long, Int>): MutableList<ObdMetric> {

        val metrics = DataLogger.INSTANCE.getEmptyMetrics(getVisibleMetrics())

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

    protected fun observerMetrics(
        data: MutableList<ObdMetric>
    ) {
        val visibleMetrics = getVisibleMetrics()
        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
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