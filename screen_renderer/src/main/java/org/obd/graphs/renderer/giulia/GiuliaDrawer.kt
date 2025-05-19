 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
import android.graphics.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.format
import org.obd.graphs.isNumber
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.toFloat

 private const val FOOTER_SIZE_RATIO = 1.3f
const val MARGIN_END = 30

@Suppress("NOTHING_TO_INLINE")
internal class GiuliaDrawer(context: Context, settings: ScreenSettings): AbstractDrawer(context, settings) {

    inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: Metric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float,
        valueLeft: Float,
    ): Float {

        var top1 = top
        val footerValueTextSize = textSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        val (t1, t2) = drawTitle(
            canvas,
            metric, left1, top1,
            textSizeBase
        )

        top1 = t1

        top1 += drawValue(
            canvas = canvas,
            metric = metric,
            left = valueLeft,
            top = t2?:t1,
            textSize = valueTextSize
        )

        if (settings.isStatisticsEnabled()) {
            if (metric.source.command.pid.historgam.isMinEnabled) {
                left1 = drawText(
                    canvas,
                    "min",
                    left,
                    top1,
                    Color.DKGRAY,
                    footerTitleTextSize
                )
                left1 = drawText(
                    canvas,
                    metric.min.format(pid = metric.pid()),
                    left1,
                    top1,
                    Color.LTGRAY,
                    footerValueTextSize
                )
            }
            if (metric.source.command.pid.historgam.isMaxEnabled) {
                left1 = drawText(
                    canvas,
                    "max",
                    left1,
                    top1,
                    Color.DKGRAY,
                    footerTitleTextSize
                )
                left1 = drawText(
                    canvas,
                    metric.max.format(pid = metric.pid()),
                    left1,
                    top1,
                    Color.LTGRAY,
                    footerValueTextSize
                )
            }

            if (metric.source.command.pid.historgam.isAvgEnabled) {
                left1 = drawText(
                    canvas,
                    "avg",
                    left1,
                    top1,
                    Color.DKGRAY,
                    footerTitleTextSize
                )

                left1 = drawText(
                    canvas,
                    metric.mean.format(pid = metric.pid()),
                    left1,
                    top1,
                    Color.LTGRAY,
                    footerValueTextSize
                )
            }
            drawAlertingLegend(canvas, metric, left1, top1)
            top1 += getTextHeight("min", paint) / 2
        }

        drawProgressBar(
            canvas,
            left,
            itemWidth(area).toFloat(), top1, metric,
            color = settings.getColorTheme().progressColor
        )

        top1 += calculateDividerSpacing()

        drawDivider(
            canvas,
            left, itemWidth(area).toFloat(), top1,
            color = settings.getColorTheme().dividerColor
        )

        top1 += (textSizeBase * 1.3).toInt()

        if (top1 > area.height()) {
            return top1
        }

        return top1
    }

    private inline fun calculateDividerSpacing() = when (settings.getMaxColumns()) {
        1 -> 14
        else -> 8
    }

    fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        color: Int,
        textSize: Float

    ): Float = drawText(canvas, text, left, top, color, textSize, paint)

    fun drawProgressBar(
        canvas: Canvas,
        left: Float,
        width: Float,
        top: Float,
        it: Metric,
        color: Int
    ) {
        if (it.source.isNumber()){
            paint.color = color
            val progress =  valueConverter.scaleToNewRange(
                it.source.toFloat(),
                it.source.command.pid.min.toFloat(), it.source.command.pid.max.toFloat(), left, left + width - MARGIN_END
            )

            canvas.drawRect(
                left - 6,
                top + 4,
                progress,
                top + calculateProgressBarHeight(),
                paint
            )
        }
    }

    fun drawAlertingLegend(canvas: Canvas, metric: Metric, left: Float, top: Float) {
        if (settings.isAlertLegendEnabled() && (metric.source.command.pid.alert.lowerThreshold != null ||
                    metric.source.command.pid.alert.upperThreshold != null)
        ) {

            val text = "  alerting "
            drawText(
                canvas,
                text,
                left,
                top,
                Color.LTGRAY,
                12f,
                alertingLegendPaint
            )

            val hPos = left + getTextWidth(text, alertingLegendPaint) + 2f

            var label = ""
            if (metric.source.command.pid.alert.lowerThreshold != null) {
                label += "X<${metric.source.command.pid.alert.lowerThreshold}"
            }

            if (metric.source.command.pid.alert.upperThreshold != null) {
                label += " X>${metric.source.command.pid.alert.upperThreshold}"
            }

            drawText(
                canvas,
                label,
                hPos + 4,
                top,
                Color.YELLOW,
                14f,
                alertingLegendPaint
            )
        }
    }

    fun drawValue(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float
    ): Float {
        valuePaint.color = valueColorScheme(metric)

        val left1 = left - 4
        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.source.valueToString()
        canvas.drawText(text, left1, top, valuePaint)

        metric.source.command.pid.units?.let {
            valuePaint.color = Color.LTGRAY
            valuePaint.textAlign = Paint.Align.LEFT
            valuePaint.textSize = (textSize * 0.4).toFloat()
            canvas.drawText(it, (left1 + 2), top, valuePaint)
        }

        return getTextHeight(text, valuePaint) - 1f
    }



    fun drawTitle(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float
    ) : Pair<Float,Float?> {
        val topMargin = 12
        var top1 = top
        titlePaint.textSize = textSize

        val description = if (metric.source.command.pid.longDescription == null || metric.source.command.pid.longDescription.isEmpty()) {
            metric.source.command.pid.description
        } else {
            metric.source.command.pid.longDescription
        }

        if (settings.isBreakLabelTextEnabled()) {

            val text = description.split("\n")
            if (text.size == 1) {
                canvas.drawText(
                    text[0],
                    left,
                    top,
                    titlePaint
                )
                return Pair(top1 + topMargin,null)
            } else {
                titlePaint.textSize = textSize
                var vPos = top
                text.forEach {
                    canvas.drawText(
                        it,
                        left,
                        vPos,
                        titlePaint
                    )
                    vPos += titlePaint.textSize

                }
                top1 += titlePaint.textSize
                return Pair(top1 + topMargin, top + topMargin)
            }

        } else {
            val text = description.replace("\n", " ")
            canvas.drawText(
                text,
                left,
                top,
                titlePaint
            )
            return Pair(top1 + topMargin,null)
        }
    }

    private fun calculateProgressBarHeight() = when (settings.getMaxColumns()) {
        1 -> 16
        else -> 10
    }


    private inline fun itemWidth(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> area.width()
            else -> area.width() / 2
        }
}
