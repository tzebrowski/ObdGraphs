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
package org.obd.graphs.renderer.giulia

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings

private const val FOOTER_SIZE_RATIO = 1.3f
const val MARGIN_END = 30

@Suppress("NOTHING_TO_INLINE")
internal class Drawer(context: Context, settings: ScreenSettings): AbstractDrawer(context, settings) {

    inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: CarMetric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float,
        valueTop: Float,
    ): Float {

        var top1 = top
        val footerValueTextSize = textSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        top1 = drawTitle(
            canvas,
            metric, left1, top1,
            textSizeBase
        )

        drawValue(
            canvas,
            metric,
            valueTop,
            top1 + 10,
            valueTextSize
        )

        if (settings.isHistoryEnabled()) {
            top1 += textSizeBase / FOOTER_SIZE_RATIO
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
                metric.toNumber(metric.min),
                left1,
                top1,
                Color.LTGRAY,
                footerValueTextSize
            )

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
                metric.toNumber(metric.max),
                left1,
                top1,
                Color.LTGRAY,
                footerValueTextSize
            )

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
                    metric.toNumber(metric.mean),
                    left1,
                    top1,
                    Color.LTGRAY,
                    footerValueTextSize
                )
            }

            drawAlertingLegend(canvas, metric, left1, top1)

        } else {
            top1 += 12
        }

        top1 += 6f

        drawProgressBar(
            canvas,
            left,
            itemWidth(area).toFloat(), top1, metric,
            color = settings.colorTheme().progressColor
        )

        top1 += calculateDividerSpacing()

        drawDivider(
            canvas,
            left, itemWidth(area).toFloat(), top1,
            color = settings.colorTheme().dividerColor
        )

        top1 += (textSizeBase * 1.7).toInt()

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
        it: CarMetric,
        color: Int
    ) {
        paint.color = color

        val progress = valueScaler.scaleToNewRange(
            it.source.value?.toFloat() ?: it.source.command.pid.min.toFloat(),
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

    fun drawAlertingLegend(canvas: Canvas, metric: CarMetric, left: Float, top: Float) {
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
        metric: CarMetric,
        left: Float,
        top: Float,
        textSize: Float
    ) {
        val colorTheme = settings.colorTheme()
        valuePaint.color = if (inAlert(metric)) {
            colorTheme.currentValueInAlertColor
        } else {
            colorTheme.currentValueColor
        }

        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.source.valueToString()
        canvas.drawText(text, left, top, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas.drawText(metric.source.command.pid.units, (left + 2), top, valuePaint)
    }

    fun drawTitle(
        canvas: Canvas,
        metric: CarMetric,
        left: Float,
        top: Float,
        textSize: Float
    ) : Float {
        var top1 = top
        titlePaint.textSize = textSize

        if (settings.isBreakLabelTextEnabled()) {
            val text = metric.source.command.pid.description.split("\n")
            if (text.size == 1) {
                canvas.drawText(
                    text[0],
                    left,
                    top,
                    titlePaint
                )
            } else {
                paint.textSize = textSize * 0.8f
                var vPos = top - 12
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
            }

        } else {
            val text = metric.source.command.pid.description.replace("\n", " ")
            canvas.drawText(
                text,
                left,
                top,
                titlePaint
            )

        }
        return top1
    }

    private fun calculateProgressBarHeight() = when (settings.getMaxColumns()) {
        1 -> 16
        else -> 10
    }

    private fun inAlert(metric: CarMetric) = settings.isAlertingEnabled() && metric.isInAlert()

    private inline fun itemWidth(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> area.width()
            else -> area.width() / 2
        }
}