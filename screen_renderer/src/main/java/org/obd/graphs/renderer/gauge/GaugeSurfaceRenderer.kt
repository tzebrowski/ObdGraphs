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
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.*
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import kotlin.math.min


@Suppress("NOTHING_TO_INLINE")
internal class GaugeSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    viewSettings: ViewSettings
) : AbstractSurfaceRenderer(settings, context, fps, metricsCollector, viewSettings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(gaugeProgressBarType = settings.getGaugeRendererSetting().gaugeProgressBarType)
    )

    override fun getDrawTop(area: Rect): Float   = if (settings.isStatusPanelEnabled()) {
            area.top + viewSettings.marginTop.toFloat() + MARGIN_TOP
        } else {
            area.top + viewSettings.marginTop.toFloat()
        }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metrics()

            gaugeDrawer.drawBackground(canvas, area)

            var top = getDrawTop(area)

            if (settings.isStatusPanelEnabled()) {
                val left = gaugeDrawer.getMarginLeft(area.left.toFloat())
                gaugeDrawer.drawStatusPanel(canvas,top, left, fps)
                top += 4
                gaugeDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 10
            }
            when (metrics.size) {
                0 -> {}
                1 -> {
                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = area.left + area.width() / 6f,
                        top = top,
                        width = area.width() * widthScaleRatio(metrics),
                        metric = metrics[0],
                        labelCenterYPadding = 22f
                    )
                }

                2 -> {

                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = area.left.toFloat(),
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metric = metrics[0],
                        labelCenterYPadding = 22f
                    )

                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = (area.left + area.width() / 2f) - 10,
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metric = metrics[1],
                        labelCenterYPadding = 22f
                    )
                }
                4 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top)
                }
                3 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top)
                }
                else -> {
                    draw(canvas, area, metrics, top = top,labelCenterYPadding = 20f)
                }
            }
        }
    }

    override fun recycle() {
        gaugeDrawer.recycle()
    }

    private fun draw(
        canvas: Canvas,
        area: Rect,
        metrics: List<Metric>,
        marginLeft: Float = 5f,
        top: Float,
        labelCenterYPadding: Float = 0f
    ) {

        val maxItems = min(metrics.size, settings.getMaxItems())
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
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = area.left + left,
                top = top,
                width = width,
                metric = it,
                labelCenterYPadding = labelCenterYPadding

            )
            left += width - padding
        }
        if (maxItems > 1) {
            left = marginLeft
            secondHalf.forEach {
                gaugeDrawer.drawGauge(
                    canvas = canvas,
                    left = area.left + left,
                    top = top + height - 8f,
                    width = width,
                    metric = it,
                    labelCenterYPadding = labelCenterYPadding
                )
                left += width - padding
            }
        }
    }

    private inline fun widthScaleRatio(metrics: List<Metric>): Float = when (metrics.size) {
        1 -> 0.65f
        2 -> 1f
        3 -> 0.8f
        4 -> 0.8f
        5 -> 1.02f
        6 -> 1.02f
        7 -> 1.02f
        8 -> 1.02f
        else -> 1f
    }
}