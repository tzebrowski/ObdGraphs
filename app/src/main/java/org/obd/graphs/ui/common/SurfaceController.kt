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
package org.obd.graphs.ui.common

import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.MainThread
import org.obd.graphs.renderer.SurfaceRenderer

private const val LOG_KEY = "SurfaceController"

class SurfaceController(
    private val renderer: SurfaceRenderer,
) : SurfaceHolder.Callback {
    private lateinit var surfaceHolder: SurfaceHolder
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private var topInset = 0

    fun updateInsets(top: Int) {
        topInset = top
        // If surface already exists, update area and redraw immediately
        if (::surfaceHolder.isInitialized && surface?.isValid == true) {
            val frame = surfaceHolder.surfaceFrame
            updateVisibleArea(frame.width(), frame.height())
            renderFrame()
        }
    }

    private fun updateVisibleArea(
        width: Int,
        height: Int,
    ) {
        visibleArea?.set(
            10,
            10 + topInset,
            width,
            height,
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        surfaceHolder.addCallback(this)
        visibleArea = Rect()

        val frame = holder.surfaceFrame
        updateVisibleArea(frame.width(), frame.height())

        surface = surfaceHolder.surface
        renderFrame()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        surface = surfaceHolder.surface
        updateVisibleArea(width, height)
        renderFrame()
        renderFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface?.release()
    }

    @MainThread
    fun renderFrame() {
        surface?.let {
            var canvas: Canvas? = null
            if (it.isValid && !surfaceLocked) {
                try {
                    canvas = it.lockHardwareCanvas()
                    surfaceLocked = true
                    renderer.onDraw(
                        canvas = canvas,
                        drawArea = visibleArea,
                    )
                } catch (e: Throwable) {
                    Log.e(LOG_KEY, "Exception was thrown during surface locking.", e)
                    surface = null
                } finally {
                    try {
                        canvas?.let { c ->
                            it.unlockCanvasAndPost(c)
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Exception was thrown during surface un-locking.", e)
                    }

                    surfaceLocked = false
                }
            }
        }
    }
}
