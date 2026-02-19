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
package org.obd.graphs.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.activity.LOG_TAG
import org.obd.graphs.activity.TOOLBAR_HIDE
import org.obd.graphs.activity.TOOLBAR_SHOW
import org.obd.graphs.activity.TOOLBAR_TOGGLE_ACTION
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_SCHEDULED_START_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.query.Query
import org.obd.graphs.getPowerPreferences
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRenderer
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.SurfaceController

private const val EVENT_THROTTLE_MS = 350L

internal abstract class SurfaceRendererFragment(
    private val fragmentId: Int,
    private val surfaceRendererType: SurfaceRendererType,
) : Fragment(),
    View.OnTouchListener {
    protected lateinit var root: View
    protected val metricsCollector = MetricsCollector.instance()
    protected val fps = Fps()

    protected abstract fun query(): Query

    protected lateinit var surfaceController: SurfaceController
    private val renderingThread: RenderingThread =
        RenderingThread(
            id = "RenderingThread-$surfaceRendererType",
            renderAction = {
                if (::surfaceController.isInitialized) {
                    surfaceController.renderFrame()
                }
            },
            perfFrameRate = {
                getScreenSettings().getSurfaceFrameRate()
            },
        )

    private var lastEventTime = 0L

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    sendBroadcastEvent(TOOLBAR_TOGGLE_ACTION)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastEventTime > EVENT_THROTTLE_MS) {
                        if (distanceY > 0) {
                            sendBroadcastEvent(TOOLBAR_HIDE)
                            lastEventTime = currentTime
                        } else if (distanceY < 0) {
                            sendBroadcastEvent(TOOLBAR_SHOW)
                            lastEventTime = currentTime
                        }
                    }

                    if (::surfaceController.isInitialized) {
                        surfaceController.updateScrollOffset(distanceY)
                        surfaceController.renderFrame()
                    }
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean = true
            },
        )

    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean = gestureDetector.onTouchEvent(event)

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
                            withDataLogger {
                                scheduleStart(getPowerPreferences().startDataLoggingAfter, query())
                            }
                        }
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        withDataLogger {
                            updateQuery(query())
                        }
                        renderingThread.start()
                    }

                    DATA_LOGGER_STOPPED_EVENT -> {
                        renderingThread.stop()
                        configureActionButton(query())
                    }
                }
            }
        }

    abstract fun getScreenSettings(): ScreenSettings

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

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(fragmentId, container, false)

        val surfaceView = root.findViewById<SurfaceView>(R.id.surface_view)
        val renderer =
            SurfaceRenderer.allocate(
                requireContext(),
                getScreenSettings(),
                metricsCollector,
                fps,
                surfaceRendererType = surfaceRendererType,
            )

        surfaceController = SurfaceController(renderer)
        surfaceView.holder.addCallback(surfaceController)
        surfaceView.setOnTouchListener(this)

        updateInsets()

        metricsCollector.applyFilter(
            enabled = query().getIDs(),
        )

        DataLoggerRepository.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it, forceAppend = false)
            }
        }

        if (DataLoggerRepository.isRunning()) {
            withDataLogger {
                updateQuery(query())
            }
            renderingThread.start()
        }

        configureActionButton(query())
        return root
    }

    protected open fun updateInsets() {
        val statusLayout = requireActivity().findViewById<ConstraintLayout>(R.id.status_panel_header)
        statusLayout.post {
            val density = resources.displayMetrics.density
            surfaceController.updateInsets(((statusLayout.height / density).toInt()) + 8)
        }
    }
}
