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

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        surfaceHolder.addCallback(this)
        visibleArea = Rect()
        visibleArea?.set(
            holder.surfaceFrame.left + 10,
            holder.surfaceFrame.top + 10,
            holder.surfaceFrame.right + 10,
            holder.surfaceFrame.bottom,
        )
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
        visibleArea?.set(holder.surfaceFrame.left + 10, holder.surfaceFrame.top + 10, width, height)

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
