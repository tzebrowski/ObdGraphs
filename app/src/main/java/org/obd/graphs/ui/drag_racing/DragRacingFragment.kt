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
package org.obd.graphs.ui.drag_racing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRenderer
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.renderer.ViewSettings
import org.obd.graphs.ui.common.SurfaceController
import org.obd.graphs.ui.common.attachToFloatingButton

open class DragRacingFragment : Fragment() {
    private lateinit var root: View

    private val query = Query().apply {
        setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
    }

    private val metricsCollector = CarMetricsCollector.instance()
    private val fps = Fps()
    private val settings = DragRacingSettings(query)
    private lateinit var surfaceController: SurfaceController

    private val renderingThread: RenderingThread = RenderingThread(
        id = "DragRacingRenderingThread",
        renderAction = {
            surfaceController.renderFrame()
        },
        perfFrameRate = {
            settings.getSurfaceFrameRate()
        }
    )

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                DATA_LOGGER_CONNECTED_EVENT -> {
                    dataLogger.updateQuery(query)
                    renderingThread.start()
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    renderingThread.stop()
                    attachToFloatingButton(activity, query)
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
        activity?.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
        })
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

        root  = inflater.inflate(R.layout.fragment_drag_racing, container, false)

        val surfaceView = root.findViewById<SurfaceView>(R.id.surface_view)
        val renderer = SurfaceRenderer.allocate(
            requireContext(), settings, metricsCollector, fps,
            surfaceRendererType = SurfaceRendererType.DRAG_RACING, viewSettings = ViewSettings(marginTop = 40)
        )

        surfaceController = SurfaceController(renderer)
        surfaceView.holder.addCallback(surfaceController)

        metricsCollector.applyFilter(
            enabled = query.getPIDs()
        )

        dataLogger.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it)
            }
        }

        if (dataLogger.isRunning()) {
            dataLogger.updateQuery(query)
            renderingThread.start()
        }

        attachToFloatingButton(activity, query)

        return root
    }
}