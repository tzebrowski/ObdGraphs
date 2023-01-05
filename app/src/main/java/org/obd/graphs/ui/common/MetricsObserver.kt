package org.obd.graphs.ui.common

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric

internal class MetricsObserver {
    fun observe(
        metrics: Set<Long>,
        lifecycleOwner: LifecycleOwner,
        adapter: RecyclerView.Adapter<*>,
        data: MutableList<ObdMetric>
    ) = dataLogger.observe(lifecycleOwner) {
        it.run {
            if (metrics.contains(command.pid.id)) {
                val indexOf = data.indexOf(this)
                if (indexOf == -1) {
                    data.add(this)
                    adapter.notifyItemInserted(data.indexOf(this))
                } else {
                    data[indexOf] = this
                    adapter.notifyItemChanged(indexOf, this)
                }
            }
        }
    }
}