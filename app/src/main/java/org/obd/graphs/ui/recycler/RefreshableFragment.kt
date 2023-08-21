package org.obd.graphs.ui.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.collector.CarMetricsBuilder
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.updateLongSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.SwappableAdapter
import org.obd.graphs.ui.common.ToggleToolbarDoubleClickListener
import org.obd.graphs.ui.gauge.AdapterContext

open class RefreshableFragment : Fragment() {

    protected lateinit var root: View

    protected fun refreshRecyclerView(metricsCollector: CarMetricsCollector, recyclerViewId: Int) {
        if (::root.isInitialized){
            val adapter = ((root.findViewById(recyclerViewId) as RecyclerView).adapter) as RecyclerViewAdapter<RecyclerView.ViewHolder>
            val data = adapter.data
            metricsCollector.metrics().forEach {
                it.run {
                    val indexOf = data.indexOf(it)
                    if (indexOf > -1) {
                        data[indexOf] = it
                        adapter.notifyItemChanged(indexOf, it)
                    }
                }
            }
        }
    }

    protected fun prepareMetrics(
        metricsIdsPref: String,
        metricsSerializerPref: String
    ): MutableList<CarMetric> {
        val viewPreferences = RecycleViewPreferences(metricsSerializerPref)
        val metricsIds = getVisiblePIDsList(metricsIdsPref)
        return CarMetricsBuilder().buildFor(metricsIds, viewPreferences.getItemsSortOrder())
    }

    @SuppressLint("NotifyDataSetChanged")
    protected fun configureView(
        configureChangeEventId: String,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        adapterContext: AdapterContext,
        enableDragManager: Boolean = false,
        enableSwipeToDelete: Boolean = false,
        enableOnTouchListener: Boolean = false,
        adapter: (
            context: Context,
            data: MutableList<CarMetric>,
            resourceId: Int,
            height: Int?
        ) -> RecyclerViewAdapter<*>,
        metricsSerializerPref: String
    ) {

        val viewPreferences = RecycleViewPreferences(metricsSerializerPref)
        val metricsIds = getVisiblePIDsList(metricsIdsPref)
        val metrics = CarMetricsBuilder().buildFor(metricsIds, viewPreferences.getItemsSortOrder())

        recyclerView.layoutManager = GridLayoutManager(requireContext(), adapterContext.spanCount)
        recyclerView.adapter = adapter(requireContext(), metrics, adapterContext.layoutId, adapterContext.height).apply {
            setHasStableIds(true)
            notifyDataSetChanged()
        }

        attachDragManager(
            configureChangeEventId = configureChangeEventId,
            enableSwipeToDelete = enableSwipeToDelete,
            enableDragManager = enableDragManager,
            recyclerView = recyclerView,
            metricsIdsPref = metricsIdsPref,
            viewPreferences = viewPreferences
        )

        attachOnTouchListener(enableOnTouchListener, recyclerView)
        adapter(recyclerView).notifyDataSetChanged()
        recyclerView.refreshDrawableState()
    }

    protected fun getVisiblePIDsList(metricsIdsPref: String): Set<Long> {
        val query = dataLoggerPreferences.getPIDsToQuery()
        return Prefs.getLongSet(metricsIdsPref).filter { query.contains(it) }.toSet()
    }
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
            val itemId: CarMetric = data[fromPosition]
            data.remove(itemId)

            Prefs.updateLongSet(
                metricsIdsPref,
                data.map { obdMetric -> obdMetric.source.command.pid.id }.toList()
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
            val swappableAdapter = createSwappableAdapter(configureChangeEventId, recyclerView, metricsIdsPref, viewPreferences)

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
        (recyclerView.adapter as RecyclerViewAdapter)

}