package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.MetricsAggregator
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.DashPreferences
import org.openobd2.core.logger.ui.preferences.Preferences


class DashFragment : Fragment() {

    lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root.let {
            setupRecyclerView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_dash, container, false)
        setupRecyclerView()
        return root
    }

    private fun setupRecyclerView() {
        var pids = Preferences.getLongSet(requireContext(), "pref.dash.pids.selected")
        val data = loadMetrics(pids)

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
        recyclerView.adapter = adapter

        val callback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END,
            object : SwappableAdapter {
                override fun swapItems(fromPosition: Int, toPosition: Int) {
                    adapter.swapItems(fromPosition, toPosition)
                }

                override fun storePreferences(context: Context) {
                    DashPreferences.SERIALIZER.store(context, adapter.mData)
                }
            }
        )

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
        adapter.notifyDataSetChanged()

        MetricsAggregator.metrics.observe(viewLifecycleOwner, Observer {
            if (pids.contains(it.command.pid.id)) {
                val indexOf = data.indexOf(it)
                if (indexOf == -1) {
                    data.add(it)
                    adapter.notifyItemInserted(data.indexOf(it))
                } else {
                    data[indexOf] = it
                    adapter.notifyItemChanged(indexOf, it)
                }
            }
        })
        recyclerView.refreshDrawableState()
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )
    }

    private fun loadMetrics(pids: Set<Long>): MutableList<ObdMetric> {

        val metricsPreferences = DashPreferences.SERIALIZER.load(requireContext())?.map {
            it.id to it.position
        }!!.toMap()

        var data = DataLogger.INSTANCE.buildMetricsBy(pids)
        data.sortWith(Comparator { m1: ObdMetric, m2: ObdMetric ->
            if (metricsPreferences.containsKey(m1.command.pid.id) && metricsPreferences.containsKey(
                    m2.command.pid.id
                )
            ) {
                metricsPreferences[m1.command.pid.id]!!
                    .compareTo(metricsPreferences[m2.command.pid.id]!!)
            } else {
                -1
            }
        })
        return data
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    }
}