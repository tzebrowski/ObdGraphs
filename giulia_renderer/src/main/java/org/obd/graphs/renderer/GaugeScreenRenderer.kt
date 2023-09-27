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
import kotlin.math.min


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

            when (metrics.size){
                0 -> {}
                1 -> {
                    gaugeRenderer.drawGauge(
                        canvas, left = area.left + 80f, top = area.top.toFloat(), width = area.width() * widthScaleRatio(metrics),
                        metrics[0], screenArea = area
                    )
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
        metrics: List<CarMetric>
    ) {

        val size = min(metrics.size,6)
        val firstHalf = metrics.subList(0, size / 2)
        val secondHalf = metrics.subList(size / 2, size)
        val height = (area.height() / 2)

        val widthDivider  = when (size) {
            2 -> 2
            1 -> 1
            else -> secondHalf.size
        }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(metrics)
        var left = 0f
        firstHalf.forEach {
            gaugeRenderer.drawGauge(
                canvas, left =  area.left +  left, top = area.top.toFloat(), width = width,
                it, screenArea = area
            )
            left += width
        }
        if (size > 1) {
            left = 0f

            secondHalf.forEach {
                gaugeRenderer.drawGauge(
                    canvas, left =  area.left + left, top = area.top.toFloat() + height, width = width,
                    it, screenArea = area
                )
                left += width
            }
        }
    }

    private fun widthScaleRatio(metrics: List<CarMetric>): Float  = when (metrics.size) {
        1 -> 0.8f
        2 -> 0.9f
        3 -> 0.9f
        4 -> 0.9f
        else -> 1f
    }
}