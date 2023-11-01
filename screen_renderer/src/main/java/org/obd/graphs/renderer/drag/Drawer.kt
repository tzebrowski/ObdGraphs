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
import org.obd.graphs.bl.drag.DragRacingResults
import org.obd.graphs.bl.drag.DragRacingEntry
import org.obd.graphs.bl.drag.VALUE_NOT_SET
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.round
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_WHITE

private const val FOOTER_SIZE_RATIO = 1.3f
private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
const val MARGIN_END = 30
private const val readyToStartText =  "Ready to start"


@Suppress("NOTHING_TO_INLINE")
internal class Drawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {
    var screenRefreshCounter = 0

    inline fun drawDragRaceResults(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        dragRacingResults: DragRacingResults
    ) {

        val (_, textSizeBase) = calculateFontSize(area)

        val currentXPos = area.centerX() / 1.5f
        val lastXPos = area.centerX() + 60f
        val bestXPos = area.centerX() * 1.60f

        // legend
        drawText(canvas, "Current", currentXPos, top, textSizeBase, color = Color.LTGRAY)
        drawText(canvas, "Last", lastXPos, top, textSizeBase, color = Color.LTGRAY)
        drawText(canvas, "Best", bestXPos, top, textSizeBase, color = Color.LTGRAY)

        // 0-60
        var rowTop = top + textSizeBase + 12f
        drawDragRacingEntry(dragRacingResults._0_60, "0-60 km/h", currentXPos, rowTop, left, lastXPos,bestXPos, canvas, textSizeBase)

        // 0 - 100
        rowTop = top + (2 * textSizeBase) + 24f
        drawDragRacingEntry(dragRacingResults._0_100,"0-100 km/h", currentXPos, rowTop, left, lastXPos,bestXPos, canvas, textSizeBase)

        // 60 - 140
        rowTop = top + (3 * textSizeBase) + 36f
        drawDragRacingEntry(dragRacingResults._60_140,"60-140 km/h", currentXPos, rowTop, left, lastXPos,bestXPos, canvas, textSizeBase)

        // 0 - 160
        rowTop = top + (4 * textSizeBase) + 48f
        drawDragRacingEntry(dragRacingResults._0_160, "0-160 km/h", currentXPos, rowTop, left, lastXPos,bestXPos, canvas, textSizeBase)

        // 100 - 200
        rowTop = top + (5 * textSizeBase) + 60f
        drawDragRacingEntry(dragRacingResults._100_200, "100-200 km/h", currentXPos, rowTop, left, lastXPos,bestXPos, canvas, textSizeBase)
    }

    inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: CarMetric,
        left: Float,
        top: Float,
        dragRacingResults: DragRacingResults
    ): Float {

        val (valueTextSize, textSizeBase) = calculateFontSize(area)

        var top1 = top
        var left1 = left

        if (settings.getDragRacingSettings().vehicleSpeedEnabled) {
            drawText(
                canvas,
                metric.source.command.pid.description,
                left1,
                top1,
                textSizeBase
            )
        }

        if (dragRacingResults.readyToRace){
            if (screenRefreshCounter%4 == 0) {
                val tt = if (settings.getDragRacingSettings().vehicleSpeedEnabled) 24f else 12f
                val ll = getTextWidth(readyToStartText, titlePaint)
                drawText(
                    canvas,
                    readyToStartText,
                    area.exactCenterX() - ll,
                    top1 + tt,
                    textSizeBase * 1.6f,
                    color = Color.GREEN
                )
            }
            top1 += if (settings.getDragRacingSettings().vehicleSpeedEnabled) 0 else 20

            screenRefreshCounter++
        } else {
            screenRefreshCounter = 0
        }

        if (settings.getDragRacingSettings().vehicleSpeedEnabled) {

            drawValue(
                canvas,
                metric,
                area,
                top1 + 10,
                valueTextSize
            )

            if (settings.getDragRacingSettings().vehicleSpeedFrequencyReadEnabled) {

                val frequencyTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
                top1 += textSizeBase / FOOTER_SIZE_RATIO
                left1 = drawText(
                    canvas,
                    "freq:",
                    left,
                    top1,
                    Color.DKGRAY,
                    frequencyTextSize
                )
                drawText(
                    canvas,
                    "${metric.rate?.round(2)} read/sec",
                    left1,
                    top1,
                    Color.WHITE,
                    frequencyTextSize
                )
            }

            top1 += 12f

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
        }

        top1 += 10f + (textSizeBase).toInt()
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

        valuePaint.color = COLOR_WHITE
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
        textSize: Float,
        typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
        color: Int = Color.WHITE
    ) {
        titlePaint.textSize = textSize
        titlePaint.typeface = typeface
        titlePaint.color = color
        canvas.drawText(
            text.replace("\n", " "),
            left,
            top,
            titlePaint
        )
    }

    private fun calculateProgressBarHeight() = 16

    private inline fun getAreaWidth(area: Rect): Float = area.width().toFloat()

    private inline fun calculateDividerSpacing(): Int = 14

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(30f, CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = area.width()

        val valueTextSize = (areaWidth / 18f) * scaleRatio
        val textSizeBase = (areaWidth / 21f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }


    private inline fun drawDragRacingEntry(dragRacingEntry: DragRacingEntry, label: String, currentXPos: Float,
                                           rowTop: Float, left:Float, lastXPos: Float,
                                           bestXPos: Float,
                                           canvas: Canvas,
                                           textSizeBase: Float) {

        drawText(canvas, label, left, rowTop, textSizeBase, color = Color.LTGRAY)
        drawText(canvas, timeToString(dragRacingEntry.current), currentXPos, rowTop, textSizeBase)

        if (settings.getDragRacingSettings().vehicleSpeedDisplayDebugEnabled) {
            val ll = getTextWidth(timeToString(dragRacingEntry.current), titlePaint) * 1.25f
            drawText(canvas, speedToString(dragRacingEntry.currentSpeed), currentXPos + ll, rowTop, textSizeBase / 1.5f)
        }

        drawText(canvas, timeToString(dragRacingEntry.last), lastXPos, rowTop, textSizeBase)
        drawText(canvas, timeToString(dragRacingEntry.best), bestXPos, rowTop, textSizeBase, color = COLOR_CARDINAL)
    }


    private inline fun timeToString(value: Long): String = if (value == VALUE_NOT_SET) "---" else (value / 1000.0).round(2).toString()
    private inline fun speedToString(value: Int): String = if (value == VALUE_NOT_SET.toInt()) "" else "$value km/h"
}