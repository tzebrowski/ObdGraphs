/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.ui.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.collector.*
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.updateLongSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.SwappableAdapter
import org.obd.graphs.ui.common.ToggleToolbarDoubleClickListener
import org.obd.graphs.ui.gauge.AdapterContext

private const val LOG_KEY = "RefreshableFragment"

open class RefreshableFragment : Fragment() {

    protected val query: Query = Query()
    protected lateinit var root: View

    protected fun refreshRecyclerView(metricsCollector: CarMetricsCollector, recyclerViewId: Int) {
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
    ): MutableList<CarMetric> {
        val viewPreferences = ViewPreferencesSerializer(metricsSerializerPref)
        val metricsIds = getSelectedPIDs(metricsIdsPref)
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

        val viewPreferences = ViewPreferencesSerializer(metricsSerializerPref)
        val metricsIds = getSelectedPIDs(metricsIdsPref)
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

    protected fun getSelectedPIDs(pref: String): Set<Long> {
        val query = query.getPIDs()
        val selection = Prefs.getLongSet(pref)
        val intersection =  selection.filter { query.contains(it) }.toSet()
        Log.i(LOG_KEY,"Individual query enabled:${dataLoggerPreferences.instance.queryForEachViewStrategyEnabled}, " +
                " key:$pref, query=$query,selection=$selection, intersection=$intersection")

        return if (dataLoggerPreferences.instance.queryForEachViewStrategyEnabled) {
            Log.i(LOG_KEY,"Returning selection=$selection")
            selection
        }else {
            Log.i(LOG_KEY,"Returning intersection=$intersection")
            intersection
        }
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