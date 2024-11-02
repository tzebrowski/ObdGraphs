/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
import org.obd.graphs.bl.query.Query
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRenderer
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.sendBroadcastEvent

private const val LOG_KEY = "SurfaceController"


class SurfaceRendererController(
    private val carContext: CarContext,
    private val settings: CarSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    private val query: Query
) : DefaultLifecycleObserver {

    private var surfaceRenderer: SurfaceRenderer =
        SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, SurfaceRendererType.GIULIA)
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
                surfaceRenderer.applyMetricsFilter(query)
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
        surface?.release()
        surface = null
        try {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        } catch (e: androidx.car.app.HostException){
            Log.w(LOG_KEY, "Failed to set surface callback",e)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_KEY, "SurfaceRenderer destroyed (onDestroy) ")
        surface?.release()
        surface = null
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_KEY, "SurfaceRenderer destroyed (onPause)")
        surface?.release()
        surface = null
    }


    fun allocateSurfaceRenderer(surfaceRendererType: SurfaceRendererType) {
        Log.i(LOG_KEY, "Allocating Surface renderer, type=$surfaceRendererType")
        surfaceRenderer.recycle()
        surfaceRenderer  =
            SurfaceRenderer.allocate(carContext, settings, metricsCollector, fps, surfaceRendererType = surfaceRendererType)
        surfaceRenderer.applyMetricsFilter(query)
        renderFrame()
    }

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