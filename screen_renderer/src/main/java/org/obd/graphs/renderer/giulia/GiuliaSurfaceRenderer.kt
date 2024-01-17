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
package org.obd.graphs.renderer.giulia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.*
import kotlin.math.min


private const val LOG_KEY = "GiuliaScreenRenderer"

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
private const val AREA_MAX_WIDTH = 500


@Suppress("NOTHING_TO_INLINE")
internal class GiuliaSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    viewSettings: ViewSettings
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector, viewSettings) {

    private val valueScaler = ValueScaler()
    private val drawer = Drawer(context, settings)

    override fun getType(): SurfaceRendererType = SurfaceRendererType.GIULIA

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metrics()

            val (valueTextSize, textSizeBase) = calculateFontSize(area)

            drawer.drawBackground(canvas, area)

            var top = getDrawTop(area)
            var left = drawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                drawer.drawStatusPanel(canvas, top, left, fps)
                top += 4
                drawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 32
            }

            val topCpy = top
            var valueTop = initialValueTop(area)

            splitIntoChunks(metrics).forEach { chunk ->
                chunk.forEach lit@{ metric ->
                    top = drawer.drawMetric(
                        canvas = canvas,
                        area = area,
                        metric = metric,
                        textSizeBase = textSizeBase,
                        valueTextSize = valueTextSize,
                        left = left,
                        top = top,
                        valueLeft = valueTop
                    )
                }

                if (settings.getMaxColumns() > 1) {
                    valueTop += area.width() / 2 - 18
                }

                left += calculateLeftMargin(area)
                top = calculateTop(textSizeBase, top, topCpy)
            }
        }
    }

    override fun release() {
        drawer.recycle()
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(settings.getFontSize().toFloat(), CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = min(
            when (settings.getMaxColumns()) {
                1 -> area.width()
                else -> area.width() / 2
            }, AREA_MAX_WIDTH
        )

        val valueTextSize = (areaWidth / 10f) * scaleRatio
        val textSizeBase = (areaWidth / 16f) * scaleRatio

        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
            Log.v(
                LOG_KEY,
                "areaWidth=$areaWidth valueTextSize=$valueTextSize textSizeBase=$textSizeBase scaleRatio=$scaleRatio"
            )
        }
        return Pair(valueTextSize, textSizeBase)
    }

    private inline fun calculateTop(
        textHeight: Float,
        top: Float,
        topCpy: Float
    ): Float = when (settings.getMaxColumns()) {
        1 -> top + (textHeight / 3) - 10
        else -> topCpy
    }

    private inline fun calculateLeftMargin(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> 0
            else -> (area.width() / 2)
        }

}