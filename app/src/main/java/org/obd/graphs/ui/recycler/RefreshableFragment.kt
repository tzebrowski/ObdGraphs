 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.ui.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.collector.*
import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateLongSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.SwappableAdapter
import org.obd.graphs.ui.common.ToggleToolbarDoubleClickListener
import org.obd.graphs.ui.gauge.AdapterContext

open class RefreshableFragment : Fragment() {

    protected val query: Query = Query.instance()
    protected lateinit var root: View

    protected fun refreshRecyclerView(metricsCollector: MetricsCollector, recyclerViewId: Int) {
        if (::root.isInitialized){
            val adapter = ((root.findViewById(recyclerViewId) as RecyclerView).adapter) as RecyclerViewAdapter<RecyclerView.ViewHolder>
            val data = adapter.data
            metricsCollector.getMetrics().forEach {
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
    ): MutableList<Metric> {
        val viewPreferences = ViewPreferencesSerializer(metricsSerializerPref)
        val metricsIds = query.filterBy(metricsIdsPref)
        return MetricsBuilder().buildFor(metricsIds, viewPreferences.getItemsSortOrder())
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
            data: MutableList<Metric>,
            resourceId: Int,
            height: Int?
        ) -> RecyclerViewAdapter<*>,
        metricsSerializerPref: String
    ) {

        val viewPreferences = ViewPreferencesSerializer(metricsSerializerPref)
        val metricsIds = query.filterBy(metricsIdsPref)
        val metrics = MetricsBuilder().buildFor(metricsIds, viewPreferences.getItemsSortOrder())

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

    private fun createSwappableAdapter(
        configureChangeEventId: String,
        recyclerView: RecyclerView,
        metricsIdsPref: String,
        viewSerializer: ViewPreferencesSerializer
    ): SwappableAdapter = object : SwappableAdapter {
        override fun swapItems(fromPosition: Int, toPosition: Int) {
            adapter(recyclerView).swapItems(
                fromPosition,
                toPosition
            )
        }

        override fun storePreferences(context: Context) {
            viewSerializer.store(adapter(recyclerView).data.map { it.source.command.pid.id })
        }

        override fun deleteItems(fromPosition: Int) {
            val data = adapter(recyclerView).data
            val itemId: Metric = data[fromPosition]
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
        viewPreferences: ViewPreferencesSerializer
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
