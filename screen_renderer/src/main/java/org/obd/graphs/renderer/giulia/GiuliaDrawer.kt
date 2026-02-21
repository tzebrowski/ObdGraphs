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
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.toFloat
import kotlin.math.max

private const val FOOTER_SIZE_RATIO = 1.3f
const val MARGIN_END = 30
private const val METRIC_TOP_NUDGE = 0.02f
private const val SINGLE_LINE_VALUE_TOP_OFFSET = 0.35f
private const val DOUBLE_LINE_VALUE_TOP_OFFSET = 0.5f
private const val SINGLE_LINE_STATS_GAP = 0.30f
private const val TWO_LINE_STATS_GAP = 0.30f
private const val SINGLE_LINE_POST_STATS_GAP = 0.35f
private const val TWO_LINE_POST_STATS_GAP = 0.35f
private const val SINGLE_LINE_DIVIDER_GAP = 0.20f
private const val TWO_LINE_DIVIDER_GAP = 0.15f
private const val TOTAL_AREA_HEIGHT_MULTIPLIER = 1.55f
private const val PROGRESS_BAR_H_1_COL = 0.28f
private const val PROGRESS_BAR_H_2_COL = 0.18f
private const val GLOW_RADIUS = 12f

@Suppress("NOTHING_TO_INLINE")
internal class GiuliaDrawer(
    context: Context,
    settings: ScreenSettings,
) : AbstractDrawer(context, settings) {
    private val density = context.resources.displayMetrics.density

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(GLOW_RADIUS * density, BlurMaskFilter.Blur.NORMAL)
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
    ): Float {
        val itemW = itemWidth(area).toFloat()
        var safeTextSizeBase = textSizeBase
        var safeValueTextSize = valueTextSize

        var top1 = top + (safeTextSizeBase * METRIC_TOP_NUDGE)

        titlePaint.textSize = safeTextSizeBase
        val footerValueTextSize = safeTextSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = safeTextSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        val (newTop, secondLineTop) = drawTitle(canvas, metric, left1, top1, safeTextSizeBase)
        val isTwoLines = secondLineTop != null

        val valueNudge = if (isTwoLines) (safeTextSizeBase * DOUBLE_LINE_VALUE_TOP_OFFSET) else (safeTextSizeBase * SINGLE_LINE_VALUE_TOP_OFFSET)
        val valueDrawingTop = (secondLineTop ?: top1) + valueNudge

        drawValue(canvas, metric, valueLeft, valueDrawingTop, safeValueTextSize, valueCastToInt)

        top1 =
            if (isTwoLines) {
                newTop + (safeTextSizeBase * TWO_LINE_STATS_GAP)
            } else {
                newTop + (safeTextSizeBase * SINGLE_LINE_STATS_GAP)
            }

        if (settings.isStatisticsEnabled()) {
            top1 += (2f * density)

            if (metric.source.command.pid.historgam.isMinEnabled) {
                left1 = drawText(canvas, "min", left, top1, Color.DKGRAY, footerTitleTextSize)
                left1 = drawText(canvas, metric.min.format(pid = metric.pid), left1, top1, minValueColorScheme(metric), footerValueTextSize)
            }

            if (metric.source.command.pid.historgam.isMaxEnabled) {
                left1 = drawText(canvas, "max", left1, top1, Color.DKGRAY, footerTitleTextSize)
                left1 = drawText(canvas, metric.max.format(pid = metric.pid), left1, top1, maxValueColorScheme(metric), footerValueTextSize)
            }

            if (metric.source.command.pid.historgam.isAvgEnabled) {
                left1 = drawText(canvas, "avg", left1, top1, Color.DKGRAY, footerTitleTextSize)
                left1 = drawText(canvas, metric.mean.format(pid = metric.pid), left1, top1, Color.LTGRAY, footerValueTextSize)
            }
            drawAlertingLegend(canvas, metric, left1, top1)

            top1 += if (isTwoLines) (safeTextSizeBase * TWO_LINE_POST_STATS_GAP) else (safeTextSizeBase * SINGLE_LINE_POST_STATS_GAP)
        } else {
            top1 += if (isTwoLines) (safeTextSizeBase * 0.15f) else (safeTextSizeBase * 0.40f)
        }

        drawProgressBar(canvas, left, itemWidth(area).toFloat(), top1, metric, settings.getColorTheme().progressColor, safeTextSizeBase)

        top1 += calculateDividerSpacing(safeTextSizeBase, isTwoLines)
        drawDivider(canvas, left, itemWidth(area).toFloat(), top1, settings.getColorTheme().dividerColor)

        top1 += (safeTextSizeBase * TOTAL_AREA_HEIGHT_MULTIPLIER).toInt()

        return top1
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
                    it.source.command.pid.min
                        .toFloat(),
                    it.source.command.pid.max
                        .toFloat(),
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
            canvas.drawRect(rectLeft, rectTop - glowExpansion, rectRight, rectBottom + glowExpansion, glowPaint)

            paint.color = color
            if (settings.isProgressGradientEnabled()) {
                val colors = intArrayOf(Color.WHITE, color)
                paint.shader = LinearGradient(rectLeft, rectTop, maxRight, rectTop, colors, null, Shader.TileMode.CLAMP)
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

            var label = ""
            if (metric.source.command.pid.alert.lowerThreshold != null) label += "X<${metric.source.command.pid.alert.lowerThreshold}"
            if (metric.source.command.pid.alert.upperThreshold != null) label += " X>${metric.source.command.pid.alert.upperThreshold}"

            drawText(canvas, label, hPos + (1 * density), top, Color.YELLOW, 11f * density, alertingLegendPaint)
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
        val value = metric.source.format(castToInt = castToInt)
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
    ): Pair<Float, Float?> {
        val topMargin = max((textSize * 0.22f).toInt(), (8 * density).toInt())
        titlePaint.textSize = textSize

        val description =
            if (metric.source.command.pid.longDescription
                    .isNullOrEmpty()
            ) {
                metric.source.command.pid.description
            } else {
                metric.source.command.pid.longDescription
            }
        val safeDescription = description ?: ""

        if (settings.isBreakLabelTextEnabled()) {
            val text = safeDescription.split("\n")
            if (text.size == 1) {
                canvas.drawText(text[0], left, top, titlePaint)
                return Pair(top + topMargin, null)
            } else {
                var vPos = top
                var secondLineTop: Float? = null
                text.forEachIndexed { index, s ->
                    canvas.drawText(s.trim(), left, vPos, titlePaint)
                    if (index == 1) {
                        secondLineTop = vPos
                    }
                    vPos += titlePaint.textSize
                }
                return Pair(vPos + (topMargin / 2), secondLineTop)
            }
        } else {
            canvas.drawText(safeDescription.replace("\n", " "), left, top, titlePaint)
            return Pair(top + topMargin, null)
        }
    }

    private fun calculateProgressBarHeight(textSizeBase: Float): Int =
        when (settings.getMaxColumns()) {
            1 -> max((textSizeBase * PROGRESS_BAR_H_1_COL).toInt(), (10 * density).toInt())
            else -> max((textSizeBase * PROGRESS_BAR_H_2_COL).toInt(), (7 * density).toInt())
        }

    private inline fun itemWidth(area: Rect): Int = area.width()
}
