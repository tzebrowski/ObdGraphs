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
package org.obd.graphs.aa.screen.nav

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.MainThread
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.SurfaceRenderer
import org.obd.graphs.renderer.api.SurfaceRendererType
import org.obd.graphs.sendBroadcastEvent

private const val LOG_KEY = "SurfaceController"

class SurfaceRendererController(
    private val carContext: CarContext,
    private val settings: CarSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    private val query: Query
) : DefaultLifecycleObserver {

    private var surfaceRenderer: SurfaceRenderer? = allocateSurfaceRenderer(SurfaceRendererType.GIULIA)

    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {

        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceRendererController) {
                Log.i(LOG_KEY, "Surface is now available")
                surface?.release()
                surface = surfaceContainer.surface
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val frameRate = settings.getSurfaceFrameRate() + 5f
                    Log.i(LOG_KEY, "Setting surface Frame Rate to=$frameRate")
                    surface?.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceRendererController) {
                Log.i(LOG_KEY, "Surface visible area changed: w=${visibleArea.width()} h=${visibleArea.height()},l=${visibleArea.left}")
                this@SurfaceRendererController.visibleArea = visibleArea

                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceRendererController) {
                Log.i(LOG_KEY, "Surface stable area changed: w=${stableArea.width()} h=${stableArea.height()}")
                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceRendererController) {
                Log.i(LOG_KEY, "Surface destroyed")
                surface?.release()
                surface = null
                sendBroadcastEvent(SURFACE_DESTROYED_EVENT)
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.i(LOG_KEY, "SurfaceRenderer created")
        try {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        } catch (e: androidx.car.app.HostException) {
            Log.w(LOG_KEY, "Failed to set surface callback", e)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_KEY, "SurfaceRenderer destroyed (onDestroy)")
        synchronized(this) {
            surface?.release()
            surface = null
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_KEY, "SurfaceRenderer paused (onPause)")
    }

    fun allocateSurfaceRenderer(surfaceRendererType: SurfaceRendererType): SurfaceRenderer? {
        Log.e(LOG_KEY, "Allocating Surface renderer, type=$surfaceRendererType")

        surfaceRenderer?.recycle()

        surfaceRenderer = SurfaceRenderer.allocate(
            carContext,
            settings,
            metricsCollector,
            fps,
            surfaceRendererType = surfaceRendererType
        )

        when (surfaceRendererType) {
            SurfaceRendererType.GIULIA -> {
                val giuliaSettings = settings.getGiuliaRendererSetting()
                applySettingsFilter(giuliaSettings.selectedPIDs, giuliaSettings.getPIDsSortOrder())
            }

            SurfaceRendererType.GAUGE -> {
                val gaugeSettings = settings.getGaugeRendererSetting()
                applySettingsFilter(gaugeSettings.selectedPIDs, gaugeSettings.getPIDsSortOrder())
            }

            SurfaceRendererType.DRAG_RACING ->
                metricsCollector.applyFilter(
                    enabled = Query.instance(QueryStrategyType.DRAG_RACING_QUERY).getIDs(),
                )

            SurfaceRendererType.PERFORMANCE ->
                metricsCollector.applyFilter(
                    enabled = Query.instance(QueryStrategyType.PERFORMANCE_QUERY).getIDs(),
                )

            SurfaceRendererType.TRIP_INFO ->
                metricsCollector.applyFilter(
                    enabled = Query.instance(QueryStrategyType.TRIP_INFO_QUERY).getIDs(),
                )
        }

        renderFrame()
        return surfaceRenderer
    }

    private fun applySettingsFilter(selectedPIDs: Set<Long>, sortOrder: Map<Long, Int>?) {
        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            metricsCollector.applyFilter(enabled = selectedPIDs, order = sortOrder)
        } else {
            val intersection = selectedPIDs intersect query.getIDs()
            metricsCollector.applyFilter(enabled = intersection, order = sortOrder)
        }
    }

    @MainThread
    fun renderFrame() {
        synchronized(this) {
            surface?.let {
                var canvas: Canvas? = null
                if (it.isValid && !surfaceLocked) {
                    try {
                        canvas = it.lockHardwareCanvas()
                        surfaceLocked = true
                        surfaceRenderer?.onDraw(
                            canvas = canvas,
                            drawArea = visibleArea
                        )

                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Exception was thrown during surface locking.", e)
                        surface = null
                        sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                    } finally {
                        try {
                            canvas?.let { c ->
                                it.unlockCanvasAndPost(c)
                            }
                        } catch (e: Throwable) {
                            Log.e(LOG_KEY, "Exception was thrown during surface un-locking.", e)
                            sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                        }

                        surfaceLocked = false
                    }
                }
            }
        }
    }
}