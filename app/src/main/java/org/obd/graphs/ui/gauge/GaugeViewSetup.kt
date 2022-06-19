package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ApplicationContext
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.preferences.*

import org.obd.metrics.ObdMetric
import kotlin.math.roundToInt

class GaugeViewSetup {

    fun configureView(
        viewLifecycleOwner: LifecycleOwner,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        adapterContext: AdapterContext,
        enableDragManager: Boolean = true,
        enableOnTouchListener: Boolean = false,
        adapter: (
            context: Context,
            data: MutableList<ObdMetric>,
            resourceId: Int,
            height: Int?
        ) -> SimpleAdapter<*>,
        recycleViewPreferences: () -> RecycleViewPreferences<out RecycleViewPreference>
    ) {

        ApplicationContext.get()?.let { context ->
            val viewSerializer = recycleViewPreferences()
            val metricsIds = Prefs.getLongSet(metricsIdsPref)
            val metrics = MetricsProvider().findMetrics(metricsIds, getSortOrder(context,viewSerializer))
            recyclerView.layoutManager =
                GridLayoutManager(
                    context,
                    adapterContext.spanCount ?: calculateSpan(context, metrics)
                )

            recyclerView.adapter =
                adapter(context, metrics, adapterContext.layoutId, adapterContext.height).apply {
                    setHasStableIds(true)
                    notifyDataSetChanged()
                }

            attachDragManager(
                enableDragManager,
                recyclerView.adapter as GaugeAdapter,
                recyclerView,
                metricsIdsPref,
                viewSerializer
            )
            attachOnTouchListener(enableOnTouchListener, recyclerView)

            recyclerView.refreshDrawableState()
            MetricsObserver().observe(
                metricsIds,
                viewLifecycleOwner,
                recyclerView.adapter as RecyclerView.Adapter<*>,
                metrics
            )
        }
    }

    private fun requireContext(): Context = ApplicationContext.get()!!

    private fun getSortOrder(context: Context,viewSerializer: RecycleViewPreferences<out RecycleViewPreference>): Map<Long, Int>? =
        viewSerializer.load(context)?.associate {
            it.id to it.position
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
        adapter: GaugeAdapter,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewSerializer: RecycleViewPreferences<*>
    ) {
        if (!enableDragManager) {
            return
        }

        val dragCallback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END,
            object : SwappableAdapter {
                override fun swapItems(fromPosition: Int, toPosition: Int) {
                    adapter.swapItems(
                        fromPosition,
                        toPosition
                    )
                }

                override fun deleteItems(fromPosition: Int) {
                    val data = adapter.data
                    val itemId: ObdMetric = data[fromPosition]
                    data.remove(itemId)

                    Prefs.updateLongSet(
                        metricsIdsPref,
                        data.map { obdMetric -> obdMetric.command.pid.id }.toList()
                    )

                    adapter.notifyItemChanged(fromPosition)
                    adapter.notifyDataSetChanged()
                }

                override fun storePreferences(context: Context) {
                    viewSerializer.store(
                        context,
                        adapter.data
                    )
                }
            }
        )
        ItemTouchHelper(dragCallback).attachToRecyclerView(recyclerView)
    }

    private fun calculateSpan(context: Context, metrics: MutableList<ObdMetric>): Int {
        return when (isTablet(context)) {
            false -> {
                return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    3
                } else {
                    2
                }
            }
            else -> {
                when (metrics.size) {
                    0 -> 1
                    2 -> 2
                    1 -> 1
                    else -> (metrics.size / 2.0).roundToInt()
                }
            }
        }
    }
}