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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector

private const val margin = 10f


internal class GaugeScreenRenderer (
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractRenderer(settings, context, fps) {

    private val gaugeRenderer = GaugeRenderer(settings, context)

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            gaugeRenderer.drawBackground(canvas, area)

            val left = area.left.toFloat() + margin

            when (metrics.size){
                0 -> {}
                1 -> {
                    gaugeRenderer.drawGauge(canvas,
                        left = left + 3 * margin,
                        top = area.top.toFloat() + margin,
                        width = area.width()  * 0.8f, metrics[0], screenArea = area)
                }

                2 -> {
                    val width = (area.width() / 2) - 2 * margin

                    gaugeRenderer.drawGauge(canvas, left = left, top = area.top.toFloat(), width = width,
                        metrics[0], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left + width + margin, top = area.top.toFloat(), width = width,
                        metrics[1], screenArea = area)

                }
                3 -> {
                    val width = (area.width() / 2) - 3 * margin
                    val height = area.height() / 2f
                    gaugeRenderer.drawGauge(canvas, left = left, top = area.top.toFloat(), width = width,
                        metrics[0], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left + width + margin, top = area.top.toFloat(), width = width,
                         metrics[1], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left, top = area.top.toFloat() + height, width = width,
                        metrics[2], screenArea = area)
                }
                4 -> {
                    val width = (area.width() / 2) - 2 * margin
                    val height = (area.height() / 2)
                    gaugeRenderer.drawGauge(canvas, left = left, top = area.top.toFloat(), width = width,
                        metrics[0], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left + width  + margin, top = area.top.toFloat(), width = width,
                        metrics[1], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left, top = area.top.toFloat() + height, width = width,
                        metrics[2], screenArea = area)

                    gaugeRenderer.drawGauge(canvas, left = left + width  + margin, top = area.top.toFloat() + height, width = width,
                        metrics[3], screenArea = area)
                }
                5 -> {
                    draw(area, canvas, metrics, 5)
                }
                6 -> {
                    draw(area, canvas, metrics)

                }
                else -> {
                    draw(area, canvas, metrics)
                }
            }
        }
    }

    private fun draw(
        area: Rect,
        canvas: Canvas,
        metrics: List<CarMetric>,
        size: Int = 6
    ) {
        val width = (area.width() / 3f)
        val height = (area.height() / 2)

        gaugeRenderer.drawGauge(
            canvas, left = 0f, top = area.top.toFloat(), width = width,
            metrics[0], screenArea = area
        )

        gaugeRenderer.drawGauge(
            canvas, left = width, top = area.top.toFloat(), width = width,
            metrics[1], screenArea = area
        )

        gaugeRenderer.drawGauge(
            canvas, left = (2 * width), top = area.top.toFloat(), width = width,
            metrics[2], screenArea = area
        )

        gaugeRenderer.drawGauge(
            canvas, left = 0f, top = area.top.toFloat() + height, width = width,
            metrics[3], screenArea = area
        )

        gaugeRenderer.drawGauge(
            canvas, left = width, top = area.top.toFloat() + height, width = width,
            metrics[4], screenArea = area
        )

        if (size > 5) {
            gaugeRenderer.drawGauge(
                canvas, left = 2 * width, top = area.top.toFloat() + height, width = width,
                metrics[5], screenArea = area
            )
        }
    }

}