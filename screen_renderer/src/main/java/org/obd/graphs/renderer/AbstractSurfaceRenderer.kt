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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect

const val MARGIN_TOP = 8

internal abstract class AbstractSurfaceRenderer(protected val context: Context) : SurfaceRenderer {

    protected var scrollOffset: Float = 0f
    protected val scrollBarWidth = 6f
    protected val scrollBarPaint = android.graphics.Paint().apply {
        color = Color.LTGRAY
        alpha = 160
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }

    open fun getTop(area: Rect): Float = area.top + getDefaultTopMargin()

    override fun updateScrollOffset(scrollOffset: Float) {
        this.scrollOffset += scrollOffset
    }

    fun getDefaultTopMargin(): Float = 20f

    protected fun getArea(
        area: Rect,
        canvas: Canvas,
        margin: Int = 0,
    ): Rect =
        if (area.isEmpty) {
            Rect(0 + margin, 0, canvas.width - 1 - margin, canvas.height)
        } else {
            Rect(area.left + margin, area.top , area.right - margin, area.bottom)
        }
}
