package org.obd.graphs.ui.dashboard

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ApplicationContext
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.gauge.AdapterContext
import org.obd.graphs.ui.gauge.SimpleAdapter
import org.obd.graphs.ui.preferences.*
import org.obd.metrics.ObdMetric

class DashboardViewSetup {

    fun configureView(
        viewLifecycleOwner: LifecycleOwner,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        adapterContext: AdapterContext,
        enableDragManager: Boolean = true,
        enableSwipeToDelete: Boolean = false,
        enableOnTouchListener: Boolean = false,
        adapter: (
            context: Context,
            data: MutableList<ObdMetric>,
            resourceId: Int,
            height: Int?
        ) -> SimpleAdapter<*>,
        recycleViewPreferences: () -> RecycleViewPreferences<out RecycleViewPreference>
    ) {

        val viewSerializer = recycleViewPreferences()
        val metricsIds = Prefs.getLongSet(metricsIdsPref)
        val metrics =  MetricsProvider().findMetrics(metricsIds, getSortOrderMap(viewSerializer))
        val spanCount = calculateSpanCount(metrics.size)
        val itemHeight = calculateItemHeight(metrics, spanCount)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        recyclerView.adapter = adapter(requireContext(),metrics,adapterContext.layoutId, itemHeight).apply {
            setHasStableIds(true)
            notifyDataSetChanged()
        }

        attachDragManager(
            enableSwipeToDelete = enableSwipeToDelete,
            enableDragManager = enableDragManager,
            recyclerView = recyclerView,
            metricsIdsPref =  metricsIdsPref,
            viewSerializer = viewSerializer)

        attachOnTouchListener(enableOnTouchListener, recyclerView)
        adapter(recyclerView).notifyDataSetChanged()
        recyclerView.refreshDrawableState()

        MetricsObserver().observe(metricsIds,viewLifecycleOwner,adapter(recyclerView), metrics)
    }

    private fun getSortOrderMap(viewSerializer: RecycleViewPreferences<out RecycleViewPreference>): Map<Long, Int>? =
        viewSerializer.load(requireContext())?.associate {
            it.id to it.position
        }

    private fun requireContext(): Context = ApplicationContext.get()!!

    private fun createSwappableAdapter(
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewSerializer: RecycleViewPreferences<out RecycleViewPreference>
    ): SwappableAdapter = object : SwappableAdapter {
        override fun swapItems(fromPosition: Int, toPosition: Int) {
            adapter(recyclerView).swapItems(
                fromPosition,
                toPosition
            )
        }

        override fun storePreferences(context: Context) {
            viewSerializer.store(
                context,
                adapter(recyclerView).data
            )
        }

        override fun deleteItems(fromPosition: Int) {
            val data = adapter(recyclerView).data
            val itemId: ObdMetric = data[fromPosition]
            data.remove(itemId)

            Prefs.updateLongSet(
                metricsIdsPref,
                data.map { obdMetric -> obdMetric.command.pid.id }.toList()
            )
            adapter(recyclerView).notifyItemChanged(fromPosition)
            adapter(recyclerView).notifyDataSetChanged()
        }
    }


    private fun attachOnTouchListener(
        enableOnTouchListener: Boolean,
        recyclerView: RecyclerView
    ) {
        if (enableOnTouchListener) {
            recyclerView.addOnItemTouchListener(
                ToggleToolbarDoubleClickListener(
                    requireContext()
                )
            )
        }
    }

    private fun attachDragManager(
        enableDragManager: Boolean = true,
        enableSwipeToDelete: Boolean = false,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewSerializer: RecycleViewPreferences<out RecycleViewPreference>
    ) {
        if (enableDragManager) {
            val swappableAdapter = createSwappableAdapter(recyclerView,metricsIdsPref,viewSerializer)

            val callback = if (enableSwipeToDelete)
                DragManageAdapter(
                    requireContext(),
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.START or ItemTouchHelper.END, swappableAdapter
                )
            else
                DragManageAdapter(
                    requireContext(),
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.ACTION_STATE_DRAG, swappableAdapter
                )

            ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
        }
    }

    private fun adapter(recyclerView: RecyclerView) =
        (recyclerView.adapter as DashboardViewAdapter)


    private fun calculateItemHeight(metrics: MutableList<ObdMetric>, spanCount: Int): Int {
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val size = if (metrics.size == 0) 1 else metrics.size
        return heightPixels / size * spanCount
    }

    private fun calculateSpanCount(numberOfItems: Int): Int {
        return if (numberOfItems <= 3) {
            1
        } else {
            if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}