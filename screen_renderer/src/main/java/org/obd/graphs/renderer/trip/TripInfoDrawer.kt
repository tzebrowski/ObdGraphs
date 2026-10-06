/*
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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsBuilder
import org.obd.graphs.format
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.MARGIN_END
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.cache.TextCache
import org.obd.graphs.renderer.giulia.GiuliaDrawer
import org.obd.graphs.toNumber

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

const val MAX_ITEM_IN_THE_ROW = 6

internal class TripInfoLayoutCache {
    val area = Rect()
    var valueTextSize: Float = 0f
    var textSizeBase: Float = 0f
    var bottomRowTextSizeBase: Float = 0f
    var bottomColWidth: Float = 0f
    val labels = TripInfoLabelLayout()
    var grid: TripInfoGrid = TripInfoMetrics.grid(0)

    fun requiresLayoutUpdate(
        newArea: Rect,
        newTopMetricsCount: Int,
        newBottomMetricsCount: Int,
        newBottom: List<TripInfoItem>,
        newBreakLabelTextEnabled: Boolean
    ): Boolean =
        area != newArea ||
            labels.requiresUpdate(newTopMetricsCount, newBottomMetricsCount, newBottom, newBreakLabelTextEnabled)
}

@Suppress("NOTHING_TO_INLINE")
internal class TripInfoDrawer(
    context: Context,
    settings: ScreenSettings
) : AbstractDrawer(context, settings) {
    private val metricBuilder = MetricsBuilder()
    private val giuliaDrawer = GiuliaDrawer(context, settings)

    private val layoutCache = TripInfoLayoutCache()
    private val textCache = TextCache()
    private val defaultTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    override fun invalidate() {
        super.invalidate()
        giuliaDrawer.invalidate()
        textCache.clear()
        layoutCache.area.setEmpty()
        layoutCache.labels.reset()
    }

    override fun recycle() {
        super.recycle()
        giuliaDrawer.recycle()
        textCache.clear()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        tripInfo: TripInfoDetails
    ) {
        val currentTopCount = countAvailable(tripInfo.top)
        val currentBottomCount = countAvailable(tripInfo.bottom)

        val breakLabelTextEnabled = settings.isBreakLabelTextEnabled()
        if (layoutCache.requiresLayoutUpdate(area, currentTopCount, currentBottomCount, tripInfo.bottom, breakLabelTextEnabled)) {
            calculateLayout(area, tripInfo, currentTopCount, currentBottomCount, breakLabelTextEnabled)
        }

        val grid = layoutCache.grid
        val textSizeBase = layoutCache.textSizeBase
        val gridTextSizeBase = textSizeBase * grid.scale
        val valueTextSize = layoutCache.valueTextSize
        val dynamicPadding = gridTextSizeBase * 0.1f
        val x = maxItemWidth(area)

        var rowTop = top + (gridTextSizeBase * 0.3f)
        var colIndex = 0
        var drawnTopCount = 0

        for (i in tripInfo.top.indices) {
            val descriptor = tripInfo.top[i].descriptor
            val source = tripInfo.top[i].metric ?: continue
            if (drawnTopCount >= grid.shown) break

            val metric = if (descriptor.diff) metricBuilder.buildDiff(source) else source

            if (colIndex >= grid.columns) {
                colIndex = 0
                rowTop += (gridTextSizeBase * 1.8f)
            }

            drawMetric(
                metric = metric,
                top = rowTop,
                left = left + (colIndex * x) + dynamicPadding,
                canvas = canvas,
                textSizeBase = gridTextSizeBase,
                statsEnabled = descriptor.statsEnabled,
                unitEnabled = descriptor.unitEnabled,
                area = area,
                valueDoublePrecision = descriptor.valueDoublePrecision,
                statsDoublePrecision = descriptor.statsDoublePrecision,
                castToInt = descriptor.castToInt
            )
            colIndex++
            drawnTopCount++
        }

        if (grid.hidden > 0) {
            if (colIndex >= grid.columns) {
                colIndex = 0
                rowTop += (gridTextSizeBase * 1.8f)
            }
            drawHiddenCount(canvas, grid.hidden, left + (colIndex * x) + dynamicPadding, rowTop, gridTextSizeBase * 0.8f)
        }

        rowTop += 2.2f * gridTextSizeBase

        giuliaDrawer.drawDivider(
            canvas = canvas,
            left = left,
            width = area.width().toFloat(),
            top = rowTop - (gridTextSizeBase * 0.8f),
            color = Color.DKGRAY
        )

        rowTop += 6

        var drawnBottomCount = 0
        for (i in tripInfo.bottom.indices) {
            val descriptor = tripInfo.bottom[i].descriptor
            val source = tripInfo.bottom[i].metric ?: continue
            val metric = if (descriptor.diff) metricBuilder.buildDiff(source) else source

            drawBottomMetric(
                metric = metric,
                castToInt = descriptor.castToInt,
                left = left,
                index = drawnBottomCount,
                area = area,
                dynamicPadding = textSizeBase * 0.1f,
                colWidth = layoutCache.bottomColWidth,
                canvas = canvas,
                rowTextSizeBase = layoutCache.bottomRowTextSizeBase,
                valueTextSize = valueTextSize,
                rowTop = rowTop
            )
            drawnBottomCount++
        }
    }

    // Selected PIDs the grid has no room for; the user has to deselect some to see them.
    fun drawHiddenCount(
        canvas: Canvas,
        hidden: Int,
        left: Float,
        top: Float,
        textSize: Float
    ) {
        valuePaint.typeface = defaultTypeface
        valuePaint.color = Color.LTGRAY
        valuePaint.clearShadowLayer()
        valuePaint.textSize = textSize
        canvas.drawText("+$hidden", left, top, valuePaint)
    }

    private fun countAvailable(items: List<TripInfoItem>): Int {
        var count = 0
        for (i in items.indices) {
            if (items[i].metric != null) count++
        }
        return count
    }

    private fun calculateLayout(
        area: Rect,
        tripInfo: TripInfoDetails,
        validTopMetricsCount: Int,
        validBottomMetricsCount: Int,
        breakLabelTextEnabled: Boolean
    ) {
        layoutCache.area.set(area)
        layoutCache.labels.update(validTopMetricsCount, validBottomMetricsCount, tripInfo.bottom, breakLabelTextEnabled)
        layoutCache.grid = TripInfoMetrics.grid(validTopMetricsCount)

        val scaleRatio = getScaleRatio()
        val areaWidth = area.width()

        layoutCache.valueTextSize = (areaWidth / 17f) * scaleRatio
        layoutCache.textSizeBase = (areaWidth / 22f) * scaleRatio

        if (validBottomMetricsCount > 0) {
            val colWidth = areaWidth / validBottomMetricsCount.toFloat()
            layoutCache.bottomColWidth = colWidth

            var rowTextSizeBase = layoutCache.textSizeBase

            tripInfo.bottom.forEach { item ->
                val metric = item.metric ?: return@forEach
                val pid = metric.source.command.pid

                val description = pid.longDescription?.takeIf { it.isNotEmpty() } ?: pid.description
                val longestLine =
                    if (breakLabelTextEnabled) {
                        description.split("\n").maxByOrNull { it.length } ?: description
                    } else {
                        description.replace("\n", " ")
                    }

                titlePaint.textSize = layoutCache.textSizeBase
                val titleWidth = getTextWidth(longestLine, titlePaint)
                val maxTitleWidth = colWidth * 0.75f

                if (titleWidth > maxTitleWidth && titleWidth > 0f) {
                    val scaleFactor = maxTitleWidth / titleWidth
                    rowTextSizeBase = minOf(rowTextSizeBase, layoutCache.textSizeBase * scaleFactor)
                }
            }
            layoutCache.bottomRowTextSizeBase = rowTextSizeBase
        }
    }

    fun drawBottomMetric(
        metric: Metric,
        castToInt: Boolean,
        left: Float,
        index: Int,
        area: Rect,
        dynamicPadding: Float,
        colWidth: Float,
        canvas: Canvas,
        rowTextSizeBase: Float,
        valueTextSize: Float,
        rowTop: Float
    ) {
        val metricLeft = left + (index * colWidth) + dynamicPadding
        val metricRight = metricLeft + colWidth - dynamicPadding
        val valueLeft = metricRight - MARGIN_END
        val boundedArea = Rect(metricLeft.toInt(), area.top, metricRight.toInt(), area.bottom)

        giuliaDrawer.drawMetric(
            canvas = canvas,
            area = boundedArea,
            metric = metric,
            textSizeBase = rowTextSizeBase * 0.8f,
            valueTextSize = valueTextSize * 0.8f,
            left = metricLeft,
            top = rowTop,
            valueLeft = valueLeft,
            valueCastToInt = castToInt
        )
    }

    private inline fun getScaleRatio() =
        settings.getTripInfoScreenSettings().fontSize.toFloat().mapRange(
            CURRENT_MIN,
            CURRENT_MAX,
            NEW_MIN,
            NEW_MAX
        )

    private fun drawValue(
        canvas: Canvas,
        metric: Metric,
        top: Float,
        textSize: Float,
        left: Float,
        statsEnabled: Boolean,
        unitEnabled: Boolean,
        area: Rect,
        valueDoublePrecision: Int = 2,
        statsDoublePrecision: Int = 2,
        castToInt: Boolean = false
    ) {
        valuePaint.typeface = defaultTypeface
        valuePaint.color = valueColorScheme(metric)
        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)
        valuePaint.textSize = textSize

        val text = textCache.value.get(metric.pid.id, metric.source.toNumber()) {
            metric.source.format(castToInt = castToInt, precision = valueDoublePrecision)
        }

        val textPadding = textSize * 0.05f

        canvas.drawText(text, left, top, valuePaint)
        var textWidth = getTextWidth(text, valuePaint) + textPadding

        if (unitEnabled) {
            metric.source.command.pid.units?.let {
                valuePaint.color = Color.LTGRAY
                valuePaint.textSize = (textSize * 0.4).toFloat()
                canvas.drawText(it, (left + textWidth), top, valuePaint)
                textWidth += getTextWidth(it, valuePaint) + textPadding
            }
        }

        if (settings.isStatisticsEnabled() && statsEnabled) {
            valuePaint.textSize = (textSize * 0.60).toFloat()
            val pid = metric.pid

            val minText = textCache.min.get(pid.id, metric.min) {
                metric.min.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt)
            }
            val maxText = textCache.max.get(pid.id, metric.max) {
                metric.max.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt)
            }

            val minWidth = getTextWidth(minText, valuePaint)
            val maxWidth = getTextWidth(maxText, valuePaint)
            val maxStatWidth = maxOf(minWidth, maxWidth)

            val itemWidth = textWidth + maxStatWidth

            if (itemWidth <= (maxItemWidth(area))) {
                valuePaint.color = minValueColorScheme(metric)
                canvas.drawText(minText, (left + textWidth), top, valuePaint)

                valuePaint.color = maxValueColorScheme(metric)
                canvas.drawText(
                    maxText,
                    (left + textWidth),
                    top - (getTextHeight(minText, valuePaint) * 1.1f),
                    valuePaint
                )
            }
        }
    }

    // Performance reuses drawMetric without drawScreen, so the grid stays at its default 6 columns there.
    private inline fun maxItemWidth(area: Rect) = (area.width() / layoutCache.grid.columns)

    inline fun drawMetric(
        metric: Metric,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float,
        statsEnabled: Boolean = false,
        unitEnabled: Boolean = true,
        area: Rect,
        valueDoublePrecision: Int = 2,
        statsDoublePrecision: Int = 2,
        castToInt: Boolean = false
    ) {
        drawValue(
            canvas = canvas,
            metric = metric,
            top = top,
            textSize = textSizeBase * 0.8f,
            left = left,
            statsEnabled = statsEnabled,
            unitEnabled = unitEnabled,
            area = area,
            valueDoublePrecision = valueDoublePrecision,
            statsDoublePrecision = statsDoublePrecision,
            castToInt = castToInt
        )

        drawTitle(canvas, metric, left, top + (textSizeBase * 0.40f), textSizeBase * 0.35F)
    }
}
