 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.getS
import org.obd.graphs.registerReceiver
import org.obd.graphs.ui.common.attachToFloatingButton
import org.obd.graphs.ui.recycler.RefreshableFragment
import org.obd.graphs.ui.gauge.AdapterContext

private const val CONFIGURATION_CHANGE_EVENT_DASH = "recycler.view.change.configuration.event.dash_id"
class DashboardFragment : RefreshableFragment() {
    private val metricsCollector = MetricsCollector.instance()

    private val renderingThread: RenderingThread = RenderingThread(
        renderAction = {
            refreshRecyclerView(metricsCollector, R.id.dashboard_recycler_view)
        },
        perfFrameRate = {
            Prefs.getS("pref.dashboard.fps", "10").toInt()
        }
    )

    private val dashboardPreferences: DashboardPreferences by lazy { getDashboardPreferences() }
    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CONFIGURATION_CHANGE_EVENT_DASH -> setupDashboardRecyclerView(false)

                DATA_LOGGER_CONNECTED_EVENT -> {
                    renderingThread.start()
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    renderingThread.stop()
                    attachToFloatingButton(activity, query())
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupDashboardRecyclerView(false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerReceiver(activity, broadcastReceiver) {
            it.addAction(CONFIGURATION_CHANGE_EVENT_DASH)
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        renderingThread.stop()
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(broadcastReceiver)
        renderingThread.stop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        setupDashboardRecyclerView(true)

        dataLogger.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it)
            }
        }

        if (dataLogger.isRunning()) {
            dataLogger.updateQuery(query())
            renderingThread.start()
        }

        attachToFloatingButton(activity, query())

        return root
    }

    private fun query() = query.apply(dashboardPreferences.dashboardSelectedMetrics.first)

    private fun setupDashboardRecyclerView(enableOnTouchListener: Boolean) {
        configureView(
            configureChangeEventId = CONFIGURATION_CHANGE_EVENT_DASH,
            recyclerView = root.findViewById(R.id.dashboard_recycler_view) as RecyclerView,
            metricsIdsPref = dashboardPreferences.dashboardSelectedMetrics.first,
            adapterContext = AdapterContext(
                layoutId = R.layout.item_dashboard,
                spanCount = calculateSpanCount(),
                height = calculateHeight(Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size)
            ),
            enableDragManager = dashboardPreferences.dragAndDropEnabled,
            enableOnTouchListener = enableOnTouchListener,
            enableSwipeToDelete = dashboardPreferences.swipeToDeleteEnabled,
            adapter = { context: Context,
                        data: MutableList<Metric>,
                        resourceId: Int,
                        height: Int? ->
                DashboardViewAdapter(context, data, resourceId, height)
            },
            metricsSerializerPref = "prefs.dash.pids.settings"
        )

        metricsCollector.applyFilter(dashboardPreferences.dashboardSelectedMetrics.second)
    }

    private fun calculateHeight(numberOfItems: Int): Int {
        val spanCount = calculateSpanCount()
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val size = if (numberOfItems == 0) 1 else numberOfItems
        return heightPixels / size * spanCount
    }

    private fun calculateSpanCount(): Int {
        val numberOfItems =  Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size
        return if (numberOfItems <= 3) {
            1
        } else {
            if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}
