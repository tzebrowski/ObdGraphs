package org.openobd2.core.logger.ui.metrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.MetricsAggregator


class MetricsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_metrics, container, false)
        var data: MutableList<ObdMetric> = arrayListOf()
        val adapter = MetricsViewAdapter(root.context, data)

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            val indexOf = data.indexOf(it)
            if (indexOf == -1) {
                data.add(it)
                adapter.notifyItemInserted(data.indexOf(it))
            } else {
                data[indexOf] = it
                adapter.notifyItemChanged(indexOf, it)
            }
        })

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        return root
    }
}