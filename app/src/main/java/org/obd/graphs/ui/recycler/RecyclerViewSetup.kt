package org.obd.graphs.ui.recycler

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ApplicationContext
import org.obd.graphs.bl.datalogger.getPidsToQuery
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.MetricsObserver
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.gauge.AdapterContext
import org.obd.graphs.ui.preferences.*
import org.obd.metrics.api.model.ObdMetric

class RecyclerViewSetup {

    @SuppressLint("NotifyDataSetChanged")
    fun configureView(
        configureChangeEventId: String,
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

        val viewPreferences = RecycleViewPreferences(metricsSerializerPref)
        val query  = getPidsToQuery()
        val metricsIds = Prefs.getLongSet(metricsIdsPref).filter {  query.contains(it)}.toSet()

        val metrics =  MetricsProvider().findMetrics(metricsIds, viewPreferences.getItemsSortOrder())

        recyclerView.layoutManager = GridLayoutManager(requireContext(), adapterContext.spanCount)
        recyclerView.adapter = adapter(requireContext(),metrics,adapterContext.layoutId,adapterContext.height).apply {
            setHasStableIds(true)
            notifyDataSetChanged()
        }

        attachDragManager(
            configureChangeEventId = configureChangeEventId,
            enableSwipeToDelete = enableSwipeToDelete,
            enableDragManager = enableDragManager,
            recyclerView = recyclerView,
            metricsIdsPref =  metricsIdsPref,
            viewPreferences = viewPreferences)

        attachOnTouchListener(enableOnTouchListener, recyclerView)

        MetricsObserver().observe(metricsIds,viewLifecycleOwner, adapter(recyclerView), metrics)

        metrics.forEach {
            val simpleAdapter = adapter(recyclerView)
            simpleAdapter.notifyItemInserted(simpleAdapter.data.indexOf(it))
        }
    }

    private fun requireContext(): Context = ApplicationContext.get()!!

    private fun createSwappableAdapter(
        configureChangeEventId: String,
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
            sendBroadcastEvent(configureChangeEventId)
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
        configureChangeEventId: String,
        enableDragManager: Boolean = true,
        enableSwipeToDelete: Boolean = false,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewPreferences: RecycleViewPreferences
    ) {
        if (enableDragManager) {
            val swappableAdapter = createSwappableAdapter(configureChangeEventId, recyclerView,metricsIdsPref,viewPreferences)

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