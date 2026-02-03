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
package org.obd.graphs.ui.giulia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.activity.LOG_TAG
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_SCHEDULED_START_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.query.Query
import org.obd.graphs.getPowerPreferences
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRenderer
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.renderer.ViewSettings
import org.obd.graphs.ui.BaseFragment
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_TRANSPARENT
import org.obd.graphs.ui.common.SurfaceController
import org.obd.graphs.ui.withDataLogger

open class GiuliaFragment : BaseFragment() {
    private lateinit var root: View

    private val query = Query.instance()
    private val metricsCollector = MetricsCollector.instance()
    private val settings = GiuliaSettings(query)
    private val fps = Fps()

    private lateinit var surfaceController: SurfaceController

    private val renderingThread: RenderingThread =
        RenderingThread(
            id = "GiuliaFragmentRenderingThread",
            renderAction = {
                if (::surfaceController.isInitialized) {
                    surfaceController.renderFrame()
                }
            },
            perfFrameRate = {
                settings.getSurfaceFrameRate()
            },
        )

    private var broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    DATA_LOGGER_SCHEDULED_START_EVENT -> {
                        if (isAdded && isVisible) {
                            Log.i(LOG_TAG, "Scheduling data logger for=${query().getIDs()}")
                            withDataLogger { dataLogger ->
                                dataLogger.scheduleStart(getPowerPreferences().startDataLoggingAfter, query())
                            }
                        }
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        applyFilter()
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
        surfaceController.renderFrame()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerReceiver(activity, broadcastReceiver) {
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
            it.addAction(DATA_LOGGER_SCHEDULED_START_EVENT)
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
        savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_giulia, container, false)
        val surfaceView = root.findViewById<SurfaceView>(R.id.surface_view)
        setupVirtualViewPanel()
        surfaceController =
            SurfaceController(
                SurfaceRenderer.allocate(
                    context = requireContext(),
                    settings = settings,
                    metricsCollector = metricsCollector,
                    fps = fps,
                    surfaceRendererType = SurfaceRendererType.GIULIA,
                    viewSettings = ViewSettings(marginTop = 16),
                ),
            )
        surfaceView.holder.addCallback(surfaceController)

        applyFilter()

        DataLoggerRepository.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it, forceAppend = false)
            }
        }

        if (DataLoggerRepository.isRunning()) {
            withDataLogger { dataLogger ->
                dataLogger.updateQuery(query())
            }
            renderingThread.start()
        }

        attachToFloatingButton(activity, query())

        return root
    }

    private fun setVirtualViewBtn(
        btnId: Int,
        selection: String,
        viewId: String,
    ) {
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            } else {
                it.setBackgroundColor(COLOR_TRANSPARENT)
            }

            it.setOnClickListener {
                giuliaVirtualScreen.updateVirtualScreen(viewId)
                withDataLogger { dataLogger ->
                    dataLogger.updateQuery(query())
                }

                applyFilter()
                setupVirtualViewPanel()
                surfaceController.renderFrame()
            }
        }
    }

    private fun applyFilter() = metricsCollector.applyFilter(query.filterBy(giuliaVirtualScreen.getVirtualScreenPrefKey()))

    private fun query() = query.apply(giuliaVirtualScreen.getVirtualScreenPrefKey())

    private fun setupVirtualViewPanel() {
        val currentVirtualScreen = giuliaVirtualScreen.getCurrentVirtualScreen()
        setVirtualViewBtn(R.id.virtual_view_1, currentVirtualScreen, "1")
        setVirtualViewBtn(R.id.virtual_view_2, currentVirtualScreen, "2")
        setVirtualViewBtn(R.id.virtual_view_3, currentVirtualScreen, "3")
        setVirtualViewBtn(R.id.virtual_view_4, currentVirtualScreen, "4")
        setVirtualViewBtn(R.id.virtual_view_5, currentVirtualScreen, "5")
        setVirtualViewBtn(R.id.virtual_view_6, currentVirtualScreen, "6")
        setVirtualViewBtn(R.id.virtual_view_7, currentVirtualScreen, "7")
    }
}
