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
package org.obd.graphs.aa.screen.nav

import android.graphics.Canvas
import android.graphics.Color
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
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.QueryType
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRenderer
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.sendBroadcastEvent

private const val LOG_KEY = "SurfaceController"

internal class SurfaceController(
    private val carContext: CarContext,
    private val settings: CarSettings,
    private val metricsCollector: CarMetricsCollector,
    private val fps: Fps
) :
    DefaultLifecycleObserver {

    private var surfaceRenderer: SurfaceRenderer =
        SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, settings.getSurfaceRendererType())
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {

        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface is now available")
                surface?.release()
                surface = surfaceContainer.surface
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val frameRate = settings.getSurfaceFrameRate() + 5f
                    Log.i(LOG_KEY, "Setting surface Frame Rate to=$frameRate")
                    surface?.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
                surfaceRenderer.applyMetricsFilter()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface visible area changed: w=${visibleArea.width()} h=${visibleArea.height()},l=${visibleArea.left}")
                this@SurfaceController.visibleArea = visibleArea

                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface stable area changed: w=${stableArea.width()} h=${stableArea.height()}")
                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
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
        surface?.release()
        surface = null

        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_KEY, "(onDestroy) SurfaceRenderer destroyed")
        surface?.release()
        surface = null
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_KEY, "(onPause) SurfaceRenderer destroyed")
        surface?.release()
        surface = null
    }

    fun onCarConfigurationChanged() {
        renderFrame()
    }

    fun getToggleSurfaceRendererBtnColor(): Int = if (surfaceRenderer.getType() == SurfaceRendererType.DRAG_RACING)
        Color.RED else Color.WHITE

    fun toggleSurfaceRenderer() {
        surfaceRenderer.release()
        surfaceRenderer = if (surfaceRenderer.getType() == SurfaceRendererType.DRAG_RACING) {
            dataLogger.updateQuery(QueryType.METRICS)
            metricsCollector.applyFilter(settings.getSelectedPIDs())
            SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, surfaceRendererType = settings.getSurfaceRendererType())
        } else {
            dataLogger.updateQuery(QueryType.DRAG_RACING)
            SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, surfaceRendererType = SurfaceRendererType.DRAG_RACING)
        }
        surfaceRenderer.applyMetricsFilter()
    }

    fun allocateSurfaceRender() {
        surfaceRenderer.release()
        surfaceRenderer =
            SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, surfaceRendererType = settings.getSurfaceRendererType())
        renderFrame()
    }

    fun isDragRacingEnabled(): Boolean = surfaceRenderer.getType() == SurfaceRendererType.DRAG_RACING

    fun isVirtualScreensEnabled(): Boolean = !isDragRacingEnabled()

    @MainThread
    fun renderFrame() {

        surface?.let {
            var canvas: Canvas? = null
            if (it.isValid && !surfaceLocked) {
                try {
                    canvas = it.lockHardwareCanvas()
                    surfaceLocked = true
                    surfaceRenderer.onDraw(
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