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
import org.obd.graphs.getPowerPreferences
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.api.SurfaceRendererType
import org.obd.graphs.screen.behaviour.ScreenBehaviorController
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.SurfaceController
import java.lang.IllegalArgumentException

 private const val EVENT_THROTTLE_MS = 350L

internal abstract class SurfaceRendererFragment(
    private val fragmentId: Int,
    protected val surfaceRendererType: SurfaceRendererType,
    private val screenSettings: ScreenSettings,
) : Fragment(),
    View.OnTouchListener {
    protected val metricsCollector = MetricsCollector.instance()
    protected lateinit var screenBehaviorController: ScreenBehaviorController

    protected lateinit var root: View

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
                screenSettings.getSurfaceFrameRate()
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
                            val screenBehavior = screenBehaviorController.getScreenBehavior(surfaceRendererType)?: return
                            val query = screenBehavior.query()

                            Log.i(LOG_TAG, "Scheduling data logger for=${query.getIDs()}")
                            withDataLogger {
                                scheduleStart(getPowerPreferences().startDataLoggingAfter, query)
                            }
                        }
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        val screenBehavior = screenBehaviorController.getScreenBehavior(surfaceRendererType)?: return
                        withDataLogger {
                            val query = screenBehavior.query()
                            updateQuery(query)
                        }
                        renderingThread.start()
                    }

                    DATA_LOGGER_STOPPED_EVENT -> {
                        renderingThread.stop()
                        val screenBehavior = screenBehaviorController.getScreenBehavior(surfaceRendererType)?: return
                        val query = screenBehavior.query()
                        configureActionButton(query)
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
        screenBehaviorController.recycle()
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

        screenBehaviorController = ScreenBehaviorController(requireContext(),
            metricsCollector,
            mapOf(surfaceRendererType to screenSettings), Fps())

        val screenBehavior = screenBehaviorController.getScreenBehavior(surfaceRendererType)?:
            throw IllegalArgumentException("No screen behavior available for given surfaceRenderer: $surfaceRendererType")

        surfaceController = SurfaceController(screenBehavior.getSurfaceRenderer())
        surfaceView.holder.addCallback(surfaceController)
        surfaceView.setOnTouchListener(this)

        updateInsets()

        DataLoggerRepository.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it, forceAppend = false)
            }
        }

        if (DataLoggerRepository.isRunning()) {
            withDataLogger {
                updateQuery(screenBehavior.query())
            }
            renderingThread.start()
        }

        configureActionButton(screenBehavior.query())

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
