package org.obd.graphs.ui.recycler

import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.CarMetricsCollector

open class RefreshableFragment : Fragment() {

    protected lateinit var root: View

    fun refreshRecyclerView(metricsCollector: CarMetricsCollector, recyclerViewId: Int) {
        if (::root.isInitialized){
            val adapter = ((root.findViewById(recyclerViewId) as RecyclerView).adapter) as SimpleAdapter<RecyclerView.ViewHolder>
            val data = adapter.data
            metricsCollector.metrics().forEach {
                it.run {
                    val indexOf = data.indexOf(it.value)
                    if (indexOf == -1) {
                        data.add(it.value)
                        adapter.notifyItemInserted(data.indexOf(it.value))
                    } else {
                        data[indexOf] = it.value
                        adapter.notifyItemChanged(indexOf, it.value)
                    }
                }
            }
        }
    }
}