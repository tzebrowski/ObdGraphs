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
package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRendererType
import kotlin.math.min

private const val MAX_ITEMS = 6

@Suppress("NOTHING_TO_INLINE")
internal class GaugeSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: CarMetricsCollector,
    fps: Fps,
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector) {

    private val drawer = Drawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(gaugeProgressBarType = settings.getGaugeProgressBarType())
    )

    override fun getType(): SurfaceRendererType = SurfaceRendererType.GAUGE

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()

            drawer.drawBackground(canvas, area)

            var top = area.top.toFloat() + 4

            if (settings.isStatusPanelEnabled()) {
                top = drawer.drawStatusBar(canvas = canvas, top = top, left = area.left.toFloat(), fps = fps)
            }
            when (metrics.size) {
                0 -> {}
                1 -> {
                    drawer.drawGauge(
                        canvas = canvas,
                        left = area.left + area.width() / 6f,
                        top = top,
                        width = area.width() * widthScaleRatio(metrics),
                        metric = metrics[0]
                    )
                }

                2 -> {

                    drawer.drawGauge(
                        canvas = canvas,
                        left = area.left.toFloat(),
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metric = metrics[0],
                    )

                    drawer.drawGauge(
                        canvas = canvas,
                        left = (area.left + area.width() / 2f) - 10,
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metric = metrics[1],
                    )
                }
                4 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top)
                }
                3 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top)
                }
                else -> {
                    draw(canvas, area, metrics, top = top)
                }
            }
        }
    }

    override fun release() {
        drawer.recycle()
    }

    private fun draw(
        canvas: Canvas,
        area: Rect,
        metrics: List<CarMetric>,
        marginLeft: Float = 5f,
        top: Float,
    ) {

        val maxItems = min(metrics.size, MAX_ITEMS)
        val firstHalf = metrics.subList(0, maxItems / 2)
        val secondHalf = metrics.subList(maxItems / 2, maxItems)
        val height = (area.height() / 2)

        val widthDivider = when (maxItems) {
            2 -> 2
            1 -> 1
            else -> secondHalf.size
        }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(metrics)
        var left = marginLeft
        val padding = 10f
        firstHalf.forEach {
            drawer.drawGauge(
                canvas = canvas,
                left = area.left + left,
                top = top,
                width = width,
                metric = it
            )
            left += width - padding
        }
        if (maxItems > 1) {
            left = marginLeft
            secondHalf.forEach {
                drawer.drawGauge(
                    canvas = canvas,
                    left = area.left + left,
                    top = top + height - 8f,
                    width = width,
                    metric = it
                )
                left += width - padding
            }
        }
    }

    private inline fun widthScaleRatio(metrics: List<CarMetric>): Float = when (metrics.size) {
        1 -> 0.65f
        2 -> 1f
        3 -> 0.8f
        4 -> 0.8f
        5 -> 1.02f
        6 -> 1.02f
        else -> 1f
    }
}