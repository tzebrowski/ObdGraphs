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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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

    // Reusable objects to prevent Garbage Collection stutter
    private val columnArea = Rect()
    private var metricTops = FloatArray(100)
    private var metricHeights = FloatArray(100)

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
                top += 3 * MARGIN_TOP
            }

            val metrics = metricsCollector.getMetrics()
            val metricsCount = min(settings.getMaxItems(), metrics.size)

            if (metricsCount > metricTops.size) {
                metricTops = FloatArray(metricsCount)
                metricHeights = FloatArray(metricsCount)
            }

            val columns = max(1, settings.getMaxColumns())
            val columnWidth = area.width().toFloat() / columns
            val pageSize = max(1, ceil(metricsCount / columns.toDouble()).toInt())

            val viewportTop = top
            var maxBottom = viewportTop

            for (col in 0 until columns) {
                var currentTop = viewportTop
                val colStartIndex = col * pageSize
                val colEndIndex = min(colStartIndex + pageSize, metricsCount)

                for (i in colStartIndex until colEndIndex) {
                    metricTops[i] = currentTop
                    val itemHeight = giuliaDrawer.calculateMetricHeight(metrics[i], textSizeBase)
                    metricHeights[i] = itemHeight
                    currentTop += itemHeight
                }
                maxBottom = max(maxBottom, currentTop)
            }

            val viewportHeight = area.bottom - viewportTop
            val contentHeight = maxBottom - viewportTop + 20f
            val maxScroll = max(0f, contentHeight - viewportHeight)
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            val visibleTop = viewportTop + scrollOffset
            val visibleBottom = visibleTop + viewportHeight

            canvas.save()
            canvas.clipRect(area)
            canvas.translate(0f, -scrollOffset)

            for (col in 0 until columns) {
                val colLeft = leftMargin + (col * columnWidth)
                val valueLeftOffset = if (columns == 1) 42f else 32f
                val valueLeft = area.left + ((col + 1) * columnWidth) - valueLeftOffset

                columnArea.set(colLeft.toInt(), area.top, (colLeft + columnWidth).toInt(), area.bottom)

                val colStartIndex = col * pageSize
                val colEndIndex = min(colStartIndex + pageSize, metricsCount)

                for (i in colStartIndex until colEndIndex) {
                    val itemTop = metricTops[i]
                    val itemBottom = itemTop + metricHeights[i]

                    if (itemBottom >= visibleTop && itemTop <= visibleBottom) {
                        giuliaDrawer.drawMetric(
                            canvas = canvas,
                            area = columnArea,
                            metric = metrics[i],
                            textSizeBase = textSizeBase,
                            valueTextSize = valueTextSize,
                            left = colLeft,
                            top = itemTop,
                            valueLeft = valueLeft,
                            valueCastToInt = false,
                        )
                    }
                }
            }

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
        val scaleRatio = settings.getGiuliaScreenSettings().getFontSize().mapRange(CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)
        val columns = max(1, settings.getMaxColumns())
        val areaWidth = min(area.width() / columns, AREA_MAX_WIDTH)

        return Pair((areaWidth / 10f) * scaleRatio, (areaWidth / 16f) * scaleRatio)
    }
}
