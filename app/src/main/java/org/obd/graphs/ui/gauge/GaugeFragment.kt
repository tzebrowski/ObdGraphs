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
package org.obd.graphs.ui.gauge

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.bl.datalogger.Query
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.preferences.*
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.recycler.RefreshableFragment
import org.obd.graphs.ui.recycler.RecyclerViewAdapter
import kotlin.math.roundToInt

private const val ENABLE_DRAG_AND_DROP_PREF = "pref.gauge_enable_drag_and_drop"
private const val ENABLE_SWIPE_TO_DELETE_PREF = "pref.gauge.swipe_to_delete"
private const val CONFIGURE_CHANGE_EVENT_GAUGE = "recycler.view.change.configuration.event.gauge_id"
private const val GAUGE_PIDS_SETTINGS = "prefs.gauge.pids.settings"

class GaugeFragment : RefreshableFragment() {
    private val metricsCollector = CarMetricsCollector.instance()
    private val renderingThread: RenderingThread = RenderingThread(
        id = "GaugeFragmentRenderingThread",
        renderAction = {
            refreshRecyclerView(metricsCollector, R.id.recycler_view)
        },
        perfFrameRate = {
            Prefs.getS("pref.gauge.fps", "10").toInt()
        }
    )

    @SuppressLint("NotifyDataSetChanged")
    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CONFIGURE_CHANGE_EVENT_GAUGE -> {
                    configureView(false)
                }
                DATA_LOGGER_CONNECTING_EVENT -> {
                    val recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView
                    val adapter = recyclerView.adapter as RecyclerViewAdapter
                    val metrics = prepareMetrics(
                        metricsIdsPref = gaugeVirtualScreen.getVirtualScreenPrefKey(),
                        metricsSerializerPref = GAUGE_PIDS_SETTINGS
                    )
                    adapter.data.clear()
                    adapter.data.addAll(metrics)
                    adapter.notifyDataSetChanged()
                }
                DATA_LOGGER_CONNECTED_EVENT -> {
                    virtualScreensPanel {
                        it.isVisible = false
                    }
                    renderingThread.start()
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    virtualScreensPanel {
                        it.isVisible = true
                    }
                    renderingThread.stop()
                    attachToFloatingButton()
                }

                TOGGLE_TOOLBAR_ACTION -> {
                    virtualScreensPanel {
                        it.isVisible = !it.isVisible
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_gauge, container, false)

        dataLogger.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it)
            }
        }

        configureView(true)
        setupVirtualViewPanel()

        if (dataLogger.isRunning()) {
            dataLogger.updateQuery(query())
            renderingThread.start()
        } else {
            attachToFloatingButton()
        }

        return root
    }

    private fun query(): Query =
        if (dataLoggerPreferences.instance.queryForEachViewStrategyEnabled) {
            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                 .update(getSelectedPIDsList())
        } else {
            query.setStrategy(QueryStrategyType.SHARED_QUERY)
        }


    private fun attachToFloatingButton() {
        activity?.findViewById<FloatingActionButton>(R.id.connect_btn)?.setOnClickListener {
            Log.i(org.obd.graphs.activity.LOG_TAG, "GaugeFragment: Start data logging")
            dataLogger.start(query())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(CONFIGURE_CHANGE_EVENT_GAUGE)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(TOGGLE_TOOLBAR_ACTION)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderingThread.stop()
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

    private fun configureView(enableOnTouchListener: Boolean) {
        configureView(
            configureChangeEventId = CONFIGURE_CHANGE_EVENT_GAUGE,
            recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView,
            metricsIdsPref = gaugeVirtualScreen.getVirtualScreenPrefKey(),
            adapterContext = AdapterContext(
                layoutId = R.layout.item_gauge,
                spanCount = calculateSpan()
            ),

            enableSwipeToDelete = Prefs.getBoolean(ENABLE_SWIPE_TO_DELETE_PREF, false),
            enableDragManager = Prefs.getBoolean(ENABLE_DRAG_AND_DROP_PREF, false),
            enableOnTouchListener = enableOnTouchListener,
            adapter = { context: Context,
                        data: MutableList<CarMetric>,
                        resourceId: Int,
                        height: Int? ->
                GaugeAdapter(context, data, resourceId, height)
            },
            metricsSerializerPref = GAUGE_PIDS_SETTINGS
        )

        metricsCollector.applyFilter(getVisiblePIDsList(gaugeVirtualScreen.getVirtualScreenPrefKey()))
    }

    private fun calculateSpan(): Int {
        val numberOfPIDsToDisplay = getSelectedPIDsList().size

        return when (isTablet()) {
            false -> {
                return if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    3
                } else {
                    when (numberOfPIDsToDisplay) {
                        0 -> 1
                        1 -> 1
                        2 -> 1
                        3 -> 1
                        else -> 2
                    }
                }
            }
            else -> {
                when (numberOfPIDsToDisplay) {
                    0 -> 1
                    2 -> 2
                    1 -> 1
                    else -> (numberOfPIDsToDisplay / 2.0).roundToInt()
                }
            }
        }
    }

    private fun setVirtualViewBtn(btnId: Int, selection: String, viewId: String) {
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            } else {
                it.setBackgroundColor(COLOR_TRANSPARENT)
            }

            it.setOnClickListener {
                gaugeVirtualScreen.updateVirtualScreen(viewId)

                if (dataLogger.isRunning()) {
                    dataLogger.updateQuery(query())
                }

                configureView(true)
                setupVirtualViewPanel()
            }
        }
    }

    private fun setupVirtualViewPanel() {
        val currentVirtualScreen = gaugeVirtualScreen.getCurrentVirtualScreen()
        setVirtualViewBtn(R.id.virtual_view_1, currentVirtualScreen, "1")
        setVirtualViewBtn(R.id.virtual_view_2, currentVirtualScreen, "2")
        setVirtualViewBtn(R.id.virtual_view_3, currentVirtualScreen, "3")
        setVirtualViewBtn(R.id.virtual_view_4, currentVirtualScreen, "4")
        setVirtualViewBtn(R.id.virtual_view_5, currentVirtualScreen, "5")
        setVirtualViewBtn(R.id.virtual_view_6, currentVirtualScreen, "6")
        setVirtualViewBtn(R.id.virtual_view_7, currentVirtualScreen, "7")
        setVirtualViewBtn(R.id.virtual_view_8, currentVirtualScreen, "8")
    }

    private fun virtualScreensPanel(func: (p: LinearLayout) -> Unit) {
        if (Prefs.getBoolean("pref.gauge.toggle_virtual_screens_double_click", false)) {
            func(root.findViewById(R.id.virtual_view_panel))
        }
    }

    private fun getSelectedPIDsList(): Set<Long> {
        val longSet = Prefs.getLongSet(gaugeVirtualScreen.getVirtualScreenPrefKey())
        return if (dataLoggerPreferences.instance.queryForEachViewStrategyEnabled){
            longSet
        }else {
            val query = query.getPIDs()
             longSet.filter { query.contains(it) }.toSet()
        }
    }
}