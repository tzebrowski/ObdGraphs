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
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.format
import org.obd.graphs.isNumber
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.cache.TextCache
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.toDouble
import org.obd.graphs.toFloat
import kotlin.math.max

private const val FOOTER_SIZE_RATIO = 1.3f
const val MARGIN_END = 30
private const val METRIC_TOP_NUDGE = 0.02f
private const val SINGLE_LINE_VALUE_TOP_OFFSET = 0.4f
private const val DOUBLE_LINE_VALUE_TOP_OFFSET = 0.55f
private const val SINGLE_LINE_STATS_GAP = 0.30f
private const val TWO_LINE_STATS_GAP = 0.30f
private const val SINGLE_LINE_POST_STATS_GAP = 0.55f
private const val TWO_LINE_POST_STATS_GAP = 0.55f
private const val SINGLE_LINE_DIVIDER_GAP = 0.20f
private const val TWO_LINE_DIVIDER_GAP = 0.15f
private const val TOTAL_AREA_HEIGHT_MULTIPLIER = 1.55f
private const val PROGRESS_BAR_H_1_COL = 0.28f
private const val PROGRESS_BAR_H_2_COL = 0.18f
private const val GLOW_RADIUS = 12f


 private class GiuliaDrawingCache {
    val progressGradientColors = IntArray(2)
}

@Suppress("NOTHING_TO_INLINE")
internal class GiuliaDrawer(
    context: Context,
    settings: ScreenSettings,
) : AbstractDrawer(context, settings) {
    private val density = context.resources.displayMetrics.density

    private val textCache = TextCache()
    private val drawingCache = GiuliaDrawingCache()

    // Replaces the need to return a Pair object from drawTitle
    var currentSecondLineTop: Float = -1f

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(GLOW_RADIUS * density, BlurMaskFilter.Blur.NORMAL)
        }

    override fun recycle() {
        super.recycle()
        textCache.clear()
    }

    inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: Metric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float,
        valueLeft: Float,
        valueCastToInt: Boolean = false,
    ) {
        var viewportTop = top + (textSizeBase * METRIC_TOP_NUDGE)

        titlePaint.textSize = textSizeBase
        val footerValueTextSize = textSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        val newTop = drawTitle(canvas, metric, left1, viewportTop, textSizeBase)
        val isTwoLines = currentSecondLineTop != -1f

        val valueNudge =
            if (isTwoLines) (textSizeBase * DOUBLE_LINE_VALUE_TOP_OFFSET) else (textSizeBase * SINGLE_LINE_VALUE_TOP_OFFSET)
        val valueDrawingTop = (if (isTwoLines) currentSecondLineTop else viewportTop) + valueNudge

        drawValue(canvas, metric, valueLeft, valueDrawingTop, valueTextSize, valueCastToInt)

        viewportTop =
            if (isTwoLines) {
                newTop + (textSizeBase * TWO_LINE_STATS_GAP)
            } else {
                newTop + (textSizeBase * SINGLE_LINE_STATS_GAP)
            }

        if (settings.isStatisticsEnabled()) {
            viewportTop += (2f * density)
            val pid = metric.pid

            if (pid.historgam.isMinEnabled) {
                left1 =
                    drawText(canvas, "min", left, viewportTop, Color.DKGRAY, footerTitleTextSize)
                val minStr = textCache.min.get(pid.id, metric.min) { metric.min.format(pid = pid) }
                left1 = drawText(
                    canvas,
                    minStr,
                    left1,
                    viewportTop,
                    minValueColorScheme(metric),
                    footerValueTextSize
                )
            }

            if (pid.historgam.isMaxEnabled) {
                left1 =
                    drawText(canvas, "max", left1, viewportTop, Color.DKGRAY, footerTitleTextSize)
                val maxStr = textCache.max.get(pid.id, metric.max) { metric.max.format(pid = pid) }
                left1 = drawText(
                    canvas,
                    maxStr,
                    left1,
                    viewportTop,
                    maxValueColorScheme(metric),
                    footerValueTextSize
                )
            }

            if (pid.historgam.isAvgEnabled) {
                left1 =
                    drawText(canvas, "avg", left1, viewportTop, Color.DKGRAY, footerTitleTextSize)
                val avgStr =
                    textCache.avg.get(pid.id, metric.mean) { metric.mean.format(pid = pid) }
                left1 =
                    drawText(canvas, avgStr, left1, viewportTop, Color.LTGRAY, footerValueTextSize)
            }
            drawAlertingLegend(canvas, metric, left1, viewportTop)

            viewportTop += if (isTwoLines) (textSizeBase * TWO_LINE_POST_STATS_GAP) else (textSizeBase * SINGLE_LINE_POST_STATS_GAP)
        } else {
            viewportTop += if (isTwoLines) (textSizeBase * 0.15f) else (textSizeBase * 0.40f)
        }

        drawProgressBar(
            canvas,
            left,
            itemWidth(area).toFloat(),
            viewportTop,
            metric,
            settings.getColorTheme().progressColor,
            textSizeBase
        )

        viewportTop += calculateDividerSpacing(textSizeBase, isTwoLines)
        drawDivider(
            canvas,
            left,
            itemWidth(area).toFloat(),
            viewportTop,
            settings.getColorTheme().dividerColor
        )
    }

    fun calculateMetricHeight(metric: Metric, textSizeBase: Float): Float {
        val topMargin = max((textSizeBase * 0.22f).toInt(), (8 * density).toInt()).toFloat()

        val description = if (metric.source.command.pid.longDescription.isNullOrEmpty()) {
            metric.source.command.pid.description
        } else {
            metric.source.command.pid.longDescription
        }
        val safeDescription = description ?: ""

        var top = textSizeBase * METRIC_TOP_NUDGE

        val newTop: Float
        val isTwoLines: Boolean

        if (settings.isBreakLabelTextEnabled()) {
            val text = textCache.labelSplit.getOrPut(metric.pid.id) { safeDescription.split("\n") }
            if (text.size == 1) {
                newTop = top + topMargin
                isTwoLines = false
            } else {
                var vPos = top
                text.forEach { _ ->
                    vPos += textSizeBase
                }
                newTop = vPos + (topMargin / 2)
                isTwoLines = true
            }
        } else {
            newTop = top + topMargin
            isTwoLines = false
        }

        top = if (isTwoLines) {
            newTop + (textSizeBase * TWO_LINE_STATS_GAP)
        } else {
            newTop + (textSizeBase * SINGLE_LINE_STATS_GAP)
        }

        if (settings.isStatisticsEnabled()) {
            top += (2f * density)
            top += if (isTwoLines) (textSizeBase * TWO_LINE_POST_STATS_GAP) else (textSizeBase * SINGLE_LINE_POST_STATS_GAP)
        } else {
            top += if (isTwoLines) (textSizeBase * 0.15f) else (textSizeBase * 0.40f)
        }

        top += calculateDividerSpacing(textSizeBase, isTwoLines)
        top += (textSizeBase * TOTAL_AREA_HEIGHT_MULTIPLIER).toInt()
        return top
    }

    private inline fun calculateDividerSpacing(
        textSizeBase: Float,
        isTwoLines: Boolean,
    ): Int {
        val multiplier = if (isTwoLines) TWO_LINE_DIVIDER_GAP else SINGLE_LINE_DIVIDER_GAP
        return when (settings.getMaxColumns()) {
            1 -> max((textSizeBase * multiplier * 1.2f).toInt(), (10 * density).toInt())
            else -> max((textSizeBase * multiplier).toInt(), (6 * density).toInt())
        }
    }

    fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        color: Int,
        textSize: Float,
    ): Float {
        paint.color = color
        paint.textSize = textSize
        canvas.drawText(text, left, top, paint)
        return left + getTextWidth(text, paint) + (4f * density)
    }

    fun drawProgressBar(
        canvas: Canvas,
        left: Float,
        width: Float,
        top: Float,
        it: Metric,
        color: Int,
        textSizeBase: Float,
    ) {
        if (it.source.isNumber()) {
            val progress =
                it.source.toFloat().mapRange(
                    it.source.command.pid.min.toFloat(),
                    it.source.command.pid.max.toFloat(),
                    left,
                    left + width - (MARGIN_END * density),
                )

            val rectLeft = left - (3f * density)
            val rectTop = top + (1f * density)
            val rectRight = progress
            val rectBottom = top + calculateProgressBarHeight(textSizeBase)

            val maxRight = left + width - (MARGIN_END * density)

            paint.shader = null
            paint.color = Color.parseColor("#33FFFFFF")
            canvas.drawRect(rectLeft, rectTop, maxRight, rectBottom, paint)

            val glowExpansion = (rectBottom - rectTop) * 0.6f
            glowPaint.color = color
            canvas.drawRect(
                rectLeft,
                rectTop - glowExpansion,
                rectRight,
                rectBottom + glowExpansion,
                glowPaint
            )

            paint.color = color
            if (settings.isProgressGradientEnabled()) {
                drawingCache.progressGradientColors[0] = Color.WHITE
                drawingCache.progressGradientColors[1] = color
                paint.shader = LinearGradient(
                    rectLeft,
                    rectTop,
                    maxRight,
                    rectTop,
                    drawingCache.progressGradientColors,
                    null,
                    Shader.TileMode.CLAMP
                )
            }

            canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)
            paint.shader = null
        }
    }

    fun drawAlertingLegend(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
    ) {
        if (settings.isAlertLegendEnabled() &&
            (metric.source.command.pid.alert.lowerThreshold != null || metric.source.command.pid.alert.upperThreshold != null)
        ) {
            val text = "  alerting "
            drawText(canvas, text, left, top, Color.LTGRAY, 9f * density, alertingLegendPaint)
            val hPos = left + getTextWidth(text, alertingLegendPaint) + (1f * density)

            val label = textCache.alertLabel.getOrPut(metric.pid.id) {
                var temp = ""
                if (metric.source.command.pid.alert.lowerThreshold != null) temp += "X<${metric.source.command.pid.alert.lowerThreshold}"
                if (metric.source.command.pid.alert.upperThreshold != null) temp += " X>${metric.source.command.pid.alert.upperThreshold}"
                temp
            }

            drawText(
                canvas,
                label,
                hPos + (1 * density),
                top,
                Color.YELLOW,
                11f * density,
                alertingLegendPaint
            )
        }
    }

    fun drawValue(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float,
        castToInt: Boolean = false,
    ): Float {
        valuePaint.color = valueColorScheme(metric)
        val units = metric.source.command.pid.units ?: ""
        val left1 = left - getTextWidth(units, valuePaint)
        valuePaint.setShadowLayer(30f * density, 0f, 0f, Color.WHITE)

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT

        val value = textCache.value.get(metric.pid.id, metric.source.toDouble()) {
            metric.source.format(castToInt = castToInt)
        }

        canvas.drawText(value, left1, top, valuePaint)

        metric.source.command.pid.units?.let {
            valuePaint.color = Color.LTGRAY
            valuePaint.textAlign = Paint.Align.LEFT
            valuePaint.textSize = (textSize * 0.4).toFloat()
            canvas.drawText(it, (left1 + (1f * density)), top, valuePaint)
        }
        return getTextHeight(value, valuePaint).toFloat()
    }

    fun drawTitle(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float,
    ): Float {
        currentSecondLineTop = -1f // Reset for each metric
        val topMargin = max((textSize * 0.22f).toInt(), (8 * density).toInt())
        titlePaint.textSize = textSize

        val description =
            if (metric.source.command.pid.longDescription.isNullOrEmpty()) {
                metric.source.command.pid.description
            } else {
                metric.source.command.pid.longDescription
            }
        val safeDescription = description ?: ""

        if (settings.isBreakLabelTextEnabled()) {
            val text = textCache.labelSplit.getOrPut(metric.pid.id) { safeDescription.split("\n") }
            if (text.size == 1) {
                canvas.drawText(text[0], left, top, titlePaint)
                return top + topMargin
            } else {
                var vPos = top
                text.forEachIndexed { index, s ->
                    canvas.drawText(s.trim(), left, vPos, titlePaint)
                    if (index == 1) {
                        currentSecondLineTop = vPos
                    }
                    vPos += titlePaint.textSize
                }
                return vPos + (topMargin / 2)
            }
        } else {
            val text = textCache.labelReplace.getOrPut(metric.pid.id) {
                safeDescription.replace(
                    "\n",
                    " "
                )
            }
            canvas.drawText(text, left, top, titlePaint)
            return top + topMargin
        }
    }

    private fun calculateProgressBarHeight(textSizeBase: Float): Int =
        when (settings.getMaxColumns()) {
            1 -> max((textSizeBase * PROGRESS_BAR_H_1_COL).toInt(), (10 * density).toInt())
            else -> max((textSizeBase * PROGRESS_BAR_H_2_COL).toInt(), (7 * density).toInt())
        }

    private inline fun itemWidth(area: Rect): Int = area.width()
}
