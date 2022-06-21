package org.obd.graphs.ui.common

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ApplicationContext
import org.obd.graphs.ui.gauge.AdapterContext
import org.obd.graphs.ui.preferences.*
import org.obd.metrics.ObdMetric

class RecyclerViewSetup {

    fun configureView(
        viewLifecycleOwner: LifecycleOwner,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        adapterContext: AdapterContext,
        enableDragManager: Boolean = false,
        enableSwipeToDelete: Boolean = false,
        enableOnTouchListener: Boolean = false,
        adapter: (
            context: Context,
            data: MutableList<ObdMetric>,
            resourceId: Int,
            height: Int?
        ) -> SimpleAdapter<*>,
        metricsSerializerPref: String
    ) {

        val viewSerializer = RecycleViewPreferences(metricsSerializerPref)
        val metricsIds = Prefs.getLongSet(metricsIdsPref)
        val metrics =  MetricsProvider().findMetrics(metricsIds, getSortOrderMap(viewSerializer))
        recyclerView.layoutManager = GridLayoutManager(requireContext(), adapterContext.spanCount)

        recyclerView.adapter = adapter(requireContext(),metrics,adapterContext.layoutId,adapterContext.height).apply {
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

    private fun getSortOrderMap(viewSerializer: RecycleViewPreferences): Map<Long, Int>? =
        viewSerializer.load()?.associate {
            it.id to it.position
        }

    private fun requireContext(): Context = ApplicationContext.get()!!

    private fun createSwappableAdapter(
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewSerializer: RecycleViewPreferences
    ): SwappableAdapter = object : SwappableAdapter {
        override fun swapItems(fromPosition: Int, toPosition: Int) {
            adapter(recyclerView).swapItems(
                fromPosition,
                toPosition
            )
        }

        override fun storePreferences(context: Context) {
            viewSerializer.store(
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
        viewSerializer: RecycleViewPreferences
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
        (recyclerView.adapter as SimpleAdapter)
}