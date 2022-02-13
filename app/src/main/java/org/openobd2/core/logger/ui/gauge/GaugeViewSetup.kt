package org.openobd2.core.logger.ui.gauge

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.ui.common.*
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.preferences.GaugePreferences
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getLongSet
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
            spanCount: Int?
        ) {
            val metricsViewContext =
                MetricsViewContext(owner, Prefs.getLongSet(pidPreferencesId))

            val sortOrderMap = GaugePreferences.SERIALIZER.load(context)?.map {
                it.id to it.position
            }!!.toMap()

            val metrics = metricsViewContext.findMetricsToDisplay(sortOrderMap)
            metricsViewContext.adapter = GaugeViewAdapter(context, metrics, resourceId)
            val recyclerView: RecyclerView = root.findViewById(recyclerViewId)
            recyclerView.layoutManager =
                GridLayoutManager(context, spanCount ?: calculateSpan(context, metrics))
            recyclerView.adapter = metricsViewContext.adapter

            val dragCallback = DragManageAdapter(
                context,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.START or ItemTouchHelper.END,
                object : SwappableAdapter {
                    override fun swapItems(fromPosition: Int, toPosition: Int) {
                        (metricsViewContext.adapter as GaugeViewAdapter).swapItems(
                            fromPosition,
                            toPosition
                        )
                    }

                    override fun deleteItems(fromPosition: Int) {
                    }

                    override fun storePreferences(context: Context) {
                        GaugePreferences.SERIALIZER.store(
                            context,
                            (metricsViewContext.adapter as GaugeViewAdapter).data
                        )
                    }
                }
            )

            ItemTouchHelper(dragCallback).attachToRecyclerView(recyclerView)
            recyclerView.addOnItemTouchListener(
                ToggleToolbarDoubleClickListener(
                    context
                )
            )

            metricsViewContext.adapter.notifyDataSetChanged()
            metricsViewContext.observerMetrics(metrics)
        }

        private fun calculateSpan(context: Context, metrics: MutableList<ObdMetric>): Int {
            when (isTablet(context)) {
                false -> return 2
                else -> {
                    return when (metrics.size) {
                        2 -> {
                            2
                        }
                        1 -> {
                            1
                        }
                        else -> {
                            (metrics.size / 2.0).roundToInt()
                        }
                    }
                }
            }
        }
    }
}