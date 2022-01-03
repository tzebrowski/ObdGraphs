package org.openobd2.core.logger.ui.gauge

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.common.MetricsViewContext
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.GaugePreferences
import org.openobd2.core.logger.ui.preferences.Preferences

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
    ): View {
        root = inflater.inflate(R.layout.fragment_gauge, container, false)

        val metricsViewContext = MetricsViewContext(viewLifecycleOwner, Preferences.getLongSet(requireContext(), "pref.gauge.pids.selected"))

        val sortOrderMap = GaugePreferences.SERIALIZER.load(requireContext())?.map {
            it.id to it.position
        }!!.toMap()

        val metrics = metricsViewContext.findMetrics(sortOrderMap)

        metricsViewContext.adapter = GaugeViewAdapter(root.context, metrics)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
        recyclerView.adapter = metricsViewContext.adapter

        val dragCallback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END,
            object : SwappableAdapter {
                override fun swapItems(fromPosition: Int, toPosition: Int) {
                    (metricsViewContext.adapter as GaugeViewAdapter).swapItems(fromPosition, toPosition)
                }

                override fun deleteItems(fromPosition: Int) {
                    TODO("Not yet implemented")
                }

                override fun storePreferences(context: Context) {
                    GaugePreferences.SERIALIZER.store(context, (metricsViewContext.adapter as GaugeViewAdapter).mData)
                }
            }
        )

        ItemTouchHelper(dragCallback).attachToRecyclerView(recyclerView)
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )

        metricsViewContext.adapter.notifyDataSetChanged()
        metricsViewContext.observerMetrics(metrics)
        return root
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
    }
}