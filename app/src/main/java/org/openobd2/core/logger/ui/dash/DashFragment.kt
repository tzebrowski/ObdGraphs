package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.common.AbstractMetricsFragment
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.DashPreferences
import org.openobd2.core.logger.ui.preferences.Preferences


class DashFragment : AbstractMetricsFragment() {

    override fun getVisibleMetrics(): Set<Long> {
        return Preferences.getLongSet(requireContext(), "pref.dash.pids.selected")
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

        adapter = DashViewAdapter(root.context, metrics)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
        recyclerView.adapter = adapter

        val callback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END,
            object : SwappableAdapter {
                override fun swapItems(fromPosition: Int, toPosition: Int) {
                    (adapter as DashViewAdapter).swapItems(fromPosition, toPosition)
                }

                override fun storePreferences(context: Context) {
                    DashPreferences.SERIALIZER.store(context, (adapter as DashViewAdapter).mData)
                }
            }
        )

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

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    }
}