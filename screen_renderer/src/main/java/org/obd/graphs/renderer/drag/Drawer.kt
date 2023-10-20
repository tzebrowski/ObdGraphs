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
package org.obd.graphs.renderer.drag

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.datalogger.drag.DragRaceResults
import org.obd.graphs.bl.datalogger.drag.VALUE_NOT_SET
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings

private const val FOOTER_SIZE_RATIO = 1.3f
const val MARGIN_END = 30

@Suppress("NOTHING_TO_INLINE")
internal class Drawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    inline fun drawDragRaceResults(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        textSizeBase: Float,
        dragRaceResults: DragRaceResults
    ) {

        val currentXPos = area.centerX() / 1.5f
        val lastXPos = area.centerX() + 20f
        val bestXPos = area.centerX() * 1.5f

        drawText(canvas, "Current (ms)", currentXPos, top, textSizeBase)
        drawText(canvas, "Last (ms)", lastXPos, top, textSizeBase)
        drawText(canvas, "Best (ms)", bestXPos, top, textSizeBase)

        drawText(canvas, "0-100 km/h", left, top + textSizeBase, textSizeBase)
        drawText(canvas, toString(dragRaceResults.current._0_100val), currentXPos, top + textSizeBase, textSizeBase)
        drawText(canvas, toString(dragRaceResults.last._0_100val), lastXPos, top + textSizeBase, textSizeBase)
        drawText(canvas, toString(dragRaceResults.best._0_100val), bestXPos, top + textSizeBase, textSizeBase)

        drawText(canvas, "0-160 km/h", left, top + (3 * textSizeBase), textSizeBase)
        drawText(canvas, toString(dragRaceResults.current._0_160val), currentXPos, top + (3 * textSizeBase), textSizeBase)
        drawText(canvas, toString(dragRaceResults.last._0_160val), lastXPos, top + (3 * textSizeBase), textSizeBase)
        drawText(canvas, toString(dragRaceResults.best._0_160val), bestXPos, top + (3 * textSizeBase), textSizeBase)

        drawText(canvas, "100-200 km/h", left, top + (5 * textSizeBase), textSizeBase)
        drawText(canvas, toString(dragRaceResults.current._100_200val), currentXPos, top + (5 * textSizeBase), textSizeBase)
        drawText(canvas,  toString(dragRaceResults.last._100_200val), lastXPos, top + (5 * textSizeBase), textSizeBase)
        drawText(canvas, toString(dragRaceResults.best._100_200val), bestXPos, top + (5 * textSizeBase), textSizeBase)
    }

    inline fun toString(value: Long): String = if (value == VALUE_NOT_SET) "---" else value.toString()

    inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: CarMetric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float
    ): Float {

        var top1 = top
        val footerValueTextSize = textSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        drawText(
            canvas,
            metric.source.command.pid.description,
            left1,
            top1,
            textSizeBase
        )

        drawValue(
            canvas,
            metric,
            area,
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

        } else {
            top1 += 12
        }

        top1 += 6f

        drawProgressBar(
            canvas,
            left,
            getAreaWidth(area), top1, metric,
            color = settings.colorTheme().progressColor
        )

        top1 += calculateDividerSpacing()

        drawDivider(
            canvas,
            left, getAreaWidth(area), top1,
            color = settings.colorTheme().dividerColor
        )

        top1 += (textSizeBase * 1.7).toInt()

        if (top1 > area.height()) {
            return top1
        }

        return top1
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



    fun drawValue(
        canvas: Canvas,
        metric: CarMetric,
        area: Rect,
        top: Float,
        textSize: Float
    ) {
        val colorTheme = settings.colorTheme()
        valuePaint.color = if (inAlert(metric)) {
            colorTheme.currentValueInAlertColor
        } else {
            colorTheme.currentValueColor
        }
        val x = area.right - 50f
        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.source.valueToString()
        canvas.drawText(text, x, top, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas.drawText(metric.source.command.pid.units, (x + 2), top, valuePaint)
    }

    fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        textSize: Float
    ) {
        titlePaint.textSize = textSize

        if (settings.isBreakLabelTextEnabled()) {
            val split = text.split("\n")
            if (split.size == 1) {
                canvas.drawText(
                    split[0],
                    left,
                    top,
                    titlePaint
                )
            } else {
                paint.textSize = textSize * 0.8f
                var vPos = top - 12
                split.forEach {
                    canvas.drawText(
                        it,
                        left,
                        vPos,
                        titlePaint
                    )
                    vPos += titlePaint.textSize
                }
            }
        } else {
            canvas.drawText(
                text.replace("\n", " "),
                left,
                top,
                titlePaint
            )

        }
    }

    fun drawDivider(
        canvas: Canvas,
        left: Float,
        width: Float,
        top: Float,
        color: Int
    ) {

        paint.color = color
        paint.strokeWidth = 2f
        canvas.drawLine(
            left - 6,
            top + 4,
            left + width - MARGIN_END,
            top + 4,
            paint
        )
    }

    private fun calculateProgressBarHeight() = 16

    private fun inAlert(metric: CarMetric) = settings.isAlertingEnabled() && metric.isInAlert()

    private inline fun getAreaWidth(area: Rect): Float = area.width().toFloat()

    private inline fun calculateDividerSpacing(): Int = 14

}