package org.obd.graphs.ui.common

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.metrics.ObdMetric

internal class MetricsObserver {

    fun observe(
        metrics: Set<Long>,
        lifecycleOwner: LifecycleOwner,
        adapter: RecyclerView.Adapter<*>,
        data: MutableList<ObdMetric>
    ) = MetricsAggregator.metrics.observe(lifecycleOwner) {
        if (metrics.contains(it.command.pid.id)) {
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
}