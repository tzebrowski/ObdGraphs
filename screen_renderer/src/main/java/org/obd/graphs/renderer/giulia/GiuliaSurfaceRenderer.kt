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
package org.obd.graphs.renderer.giulia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
private const val AREA_MAX_WIDTH = 500

@Suppress("NOTHING_TO_INLINE")
internal class GiuliaSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
) : AbstractSurfaceRenderer(context) {
    private val giuliaDrawer = GiuliaDrawer(context, settings)

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?,
    ) {
        drawArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val (valueTextSize, textSizeBase) = calculateFontSize(area)
            giuliaDrawer.drawBackground(canvas, area)

            var top = getTop(area)
            val leftMargin = giuliaDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                giuliaDrawer.drawStatusPanel(canvas, top, leftMargin, fps)
                top += MARGIN_TOP
                giuliaDrawer.drawDivider(canvas, leftMargin, area.width().toFloat(), top, Color.DKGRAY)
                top += valueTextSize
            } else {
                top += 2 * MARGIN_TOP
            }

            val metrics = metricsCollector.getMetrics()
            val metricsCount = min(settings.getMaxItems(), metrics.size)
            val pageSize = max(min(metricsCount, round(metricsCount / settings.getMaxColumns().toFloat()).toInt()), 1)

            canvas.save()
            canvas.clipRect(area)
            canvas.translate(0f, -scrollOffset)

            val viewportTop = top
            var currentTop = top
            val initialLeft = initialLeft(area)

            for (i in 0 until pageSize) {
                currentTop =
                    giuliaDrawer.drawMetric(canvas, area, metrics[i], textSizeBase, valueTextSize, leftMargin, currentTop, initialLeft)
            }

            val firstColumnBottom = currentTop

            if (settings.getMaxColumns() > 1 && metricsCount > pageSize) {
                var secondColTop = calculateTop(textSizeBase, currentTop, top)
                val secondColLeft = leftMargin + calculateLeftMargin(area)
                val secondColValueLeft = initialLeft + (area.width() / 2) - 18

                for (i in pageSize until metricsCount) {
                    secondColTop =
                        giuliaDrawer.drawMetric(
                            canvas,
                            area,
                            metrics[i],
                            textSizeBase,
                            valueTextSize,
                            secondColLeft,
                            secondColTop,
                            secondColValueLeft,
                        )
                }
                currentTop = max(firstColumnBottom, secondColTop)
            }

            val contentHeight = currentTop - viewportTop + 20f
            val viewportHeight = (area.bottom - viewportTop)
            val maxScroll = max(0f, contentHeight - viewportHeight)
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            canvas.restore()

            if (settings.isScrollbarEnabled() && contentHeight > viewportHeight) {
                drawScrollbar(
                    canvas = canvas,
                    area = area,
                    contentHeight = contentHeight,
                    viewportHeight = viewportHeight,
                    topOffset = viewportTop,
                    verticalMargin = 10f,
                )
            }
        }
    }

    override fun recycle() {
        giuliaDrawer.recycle()
    }

    private inline fun calculateFontSize(area: Rect): Pair<Float, Float> {
        val scaleRatio = settings.getGiuliaRendererSetting().getFontSize().mapRange(CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)
        val areaWidth =
            min(
                when (settings.getMaxColumns()) {
                    1 -> area.width()
                    else -> area.width() / 2
                },
                AREA_MAX_WIDTH,
            )

        return Pair((areaWidth / 10f) * scaleRatio, (areaWidth / 16f) * scaleRatio)
    }

    private inline fun calculateTop(
        textHeight: Float,
        top: Float,
        topCpy: Float,
    ): Float =
        when (settings.getMaxColumns()) {
            1 -> top + (textHeight / 3) - 10
            else -> topCpy
        }

    private inline fun calculateLeftMargin(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> 0
            else -> (area.width() / 2)
        }

    private inline fun initialLeft(area: Rect): Float =
        when (settings.getMaxColumns()) {
            1 -> area.left + ((area.width()) - 42).toFloat()
            else -> area.left + ((area.width() / 2) - 32).toFloat()
        }
}
