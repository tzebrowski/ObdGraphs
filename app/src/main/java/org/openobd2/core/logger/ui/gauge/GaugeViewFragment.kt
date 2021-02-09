package org.openobd2.core.logger.ui.gauge

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
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.ModelChangePublisher
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.GaugePreferences
import org.openobd2.core.logger.ui.preferences.PreferencesHelper

class GaugeViewFragment : Fragment() {

    lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root.let {
            val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
            recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
            recyclerView.refreshDrawableState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val selectedPids = PreferencesHelper.getStringSet(requireContext(), "pref.gauge.pids.selected")
        val data = DataLogger.INSTANCE.buildMetricsBy(selectedPids)

        val metricsPreferences = GaugePreferences.SERIALIZER.load(requireContext())?.map {
            it.query to it.position
        }!!.toMap()

        data.sortWith(Comparator { m1: ObdMetric, m2: ObdMetric ->
            if (metricsPreferences.containsKey(m1.command.query) && metricsPreferences.containsKey(
                    m2.command.query
                )
            ) {
                metricsPreferences[m1.command.query]!!
                    .compareTo(metricsPreferences[m2.command.query]!!)
            } else {
                -1
            }
        })

        root = inflater.inflate(R.layout.fragment_gauge, container, false)
        val adapter = GaugeViewAdapter(root.context, data)
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
                    GaugePreferences.SERIALIZER.store(context, adapter.mData)
                }
            }
        )
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)

        adapter.notifyDataSetChanged()

        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )

        ModelChangePublisher.liveData.observe(viewLifecycleOwner, Observer {
            selectedPids.contains((it.command as ObdCommand).pid.pid).apply {
                if (selectedPids.contains((it.command as ObdCommand).pid.pid)) {
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
        return root
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
    }
}