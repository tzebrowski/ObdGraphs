package org.openobd2.core.logger.ui.gauge

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.MetricsViewContext
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.preferences.GaugePreferences
import org.openobd2.core.logger.ui.preferences.Preferences

class GaugeViewSetup {

    open fun onCreateView(owner: LifecycleOwner, context: Context, root: View, spanCount: Int, recyclerViewId: Int, pids: String){
        val metricsViewContext = MetricsViewContext(owner, Preferences.getLongSet(context, pids))

        val sortOrderMap = GaugePreferences.SERIALIZER.load(context)?.map {
            it.id to it.position
        }!!.toMap()

        val metrics = metricsViewContext.findMetrics(sortOrderMap)

        metricsViewContext.adapter = GaugeViewAdapter(context, metrics)
        val recyclerView: RecyclerView = root.findViewById(recyclerViewId)
        recyclerView.layoutManager = GridLayoutManager(context,spanCount)
        recyclerView.adapter = metricsViewContext.adapter

        val dragCallback = DragManageAdapter(
            context,
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
                context
            )
        )

        metricsViewContext.adapter.notifyDataSetChanged()
        metricsViewContext.observerMetrics(metrics)
    }
}