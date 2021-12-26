package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.common.AbstractMetricsFragment
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.DashPreferences
import org.openobd2.core.logger.ui.preferences.Preferences


private const val VISIBLE_PIDS = "pref.dash.pids.selected"

class DashFragment : AbstractMetricsFragment() {

    override fun getVisibleMetrics(): Set<Long> {
        return Preferences.getLongSet(requireContext(), VISIBLE_PIDS)
    }

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

        val sortOrderMap = DashPreferences.SERIALIZER.load(requireContext())?.map {
            it.id to it.position
        }!!.toMap()

        val metrics = findMetrics(sortOrderMap)
        var itemHeight = calculateItemHeight(metrics)

        adapter = DashViewAdapter(root.context, metrics, itemHeight)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount(metrics.size))
        recyclerView.adapter = adapter

        val swappableAdapter = object : SwappableAdapter {
            override fun swapItems(fromPosition: Int, toPosition: Int) {
                (adapter as DashViewAdapter).swapItems(fromPosition, toPosition)
            }

            override fun storePreferences(context: Context) {
                DashPreferences.SERIALIZER.store(context, (adapter as DashViewAdapter).mData)
            }

            override fun deleteItems(fromPosition: Int) {
                val metrics = (adapter as DashViewAdapter).mData
                val itemId: ObdMetric = metrics[fromPosition]
                metrics.remove(itemId)

                Preferences.updateLongSet(
                    requireContext(),
                    VISIBLE_PIDS,
                    metrics.map { obdMetric -> obdMetric.command.pid.id }.toList()
                )

                DashPreferences.SERIALIZER.store(requireContext(), metrics)
                var itemHeight = calculateItemHeight(metrics)
                adapter = DashViewAdapter(root.context, metrics, itemHeight)
                val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
                recyclerView.layoutManager =
                    GridLayoutManager(root.context, spanCount(metrics.size))
                recyclerView.adapter = adapter
                recyclerView.refreshDrawableState()

                observerMetrics(metrics)
                adapter.notifyDataSetChanged()

            }
        }
        val callback = if (Preferences.isEnabled(context!!,"pref.dash.swipe.to.delete"))
            DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END, swappableAdapter)
         else
            DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_DRAG, swappableAdapter)


        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
        recyclerView.refreshDrawableState()
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )

        observerMetrics(metrics)
        adapter.notifyDataSetChanged()
    }

    private fun calculateItemHeight(metrics: MutableList<ObdMetric>): Int {
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels / 2

        var itemHeight = 180
        if (metrics.size == 4) {
            itemHeight = (heightPixels / 4) - 10
        } else if (metrics.size == 3 || metrics.size == 4) {
            itemHeight = (heightPixels / 3) - 40
        } else if (metrics.size == 2) {
            itemHeight = (heightPixels / 2) - 40
        } else if (metrics.size == 1) {
            itemHeight =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    heightPixels - 40
                } else {
                    (heightPixels / 2) - 40
                }
        }
        itemHeight = (itemHeight * 1.3).toInt()
        return itemHeight
    }

    private fun spanCount(numberOfItems: Int): Int {
        return if (numberOfItems <= 4) {
            1
        } else {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}