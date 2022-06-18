package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.metrics.ObdMetric
import org.obd.graphs.ui.preferences.GaugePreferences
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import kotlin.math.roundToInt

class GaugeViewSetup {
    companion object {
        fun onCreateView(
            owner: LifecycleOwner,
            context: Context,
            root: View,
            recyclerViewId: Int,
            pidPreferencesId: String,
            resourceId: Int,
            spanCount: Int?,
            enableDragManager: Boolean = true,
            enableOnTouchListener: Boolean = false,
            height: Int? = null
        ) {

            val metricsViewContext =
                MetricsViewContext(owner, Prefs.getLongSet(pidPreferencesId))

            val sortOrderMap = GaugePreferences.SERIALIZER.load(context)?.map {
                it.id to it.position
            }!!.toMap()

            val metrics = metricsViewContext.findMetricsToDisplay(sortOrderMap)
            val recyclerView: RecyclerView = root.findViewById(recyclerViewId)
            recyclerView.layoutManager =
                GridLayoutManager(context, spanCount ?: calculateSpan(context, metrics))

            metricsViewContext.adapter = GaugeAdapter(context, metrics, resourceId, height)
            metricsViewContext.adapter.setHasStableIds(true)
            recyclerView.adapter = metricsViewContext.adapter

            if (enableDragManager) {
                attachDragManager(context, metricsViewContext, recyclerView)
            }

            if (enableOnTouchListener){
                recyclerView.addOnItemTouchListener(
                    ToggleToolbarDoubleClickListener(
                        context
                    )
                )
            }

            metricsViewContext.adapter.notifyDataSetChanged()
            metricsViewContext.observerMetrics(metrics)
        }

        private fun attachDragManager(
            context: Context,
            metricsViewContext: MetricsViewContext,
            recyclerView: RecyclerView
        ) {
            val dragCallback = DragManageAdapter(
                context,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.START or ItemTouchHelper.END,
                object : SwappableAdapter {
                    override fun swapItems(fromPosition: Int, toPosition: Int) {
                        (metricsViewContext.adapter as GaugeAdapter).swapItems(
                            fromPosition,
                            toPosition
                        )
                    }

                    override fun deleteItems(fromPosition: Int) {
                    }

                    override fun storePreferences(context: Context) {
                        GaugePreferences.SERIALIZER.store(
                            context,
                            (metricsViewContext.adapter as GaugeAdapter).data
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
}