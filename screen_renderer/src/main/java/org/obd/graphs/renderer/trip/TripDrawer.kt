/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.*
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

@Suppress("NOTHING_TO_INLINE")
internal class TripDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        tripInfo: TripInfoDetails
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
        drawDragRacingEntry(area, tripInfo.airTemp, "airTemp",  rowTop, left,canvas, textSizeBase)

        // 0 - 100
        rowTop = top + (2 * textSizeBase) + 24f
        drawDragRacingEntry(area, tripInfo.ambientTemp, "ambientTemp",  rowTop, left, canvas, textSizeBase)

        // 60 - 140
        rowTop = top + (3 * textSizeBase) + 36f
        drawDragRacingEntry(area,tripInfo.coolantTemp, "coolantTemp", rowTop, left,canvas, textSizeBase)

        // 0 - 160
        rowTop = top + (4 * textSizeBase) + 48f
        drawDragRacingEntry(area,tripInfo.fuellevel, "fuelevel", rowTop, left, canvas, textSizeBase)

        // 100 - 200
        rowTop = top + (5 * textSizeBase) + 60f
        drawDragRacingEntry(area, tripInfo.fuelConsumption, "fuelConsumption", rowTop, left, canvas, textSizeBase)
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

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(settings.getDragRacingSettings().fontSize.toFloat(),
            CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = area.width()
        val valueTextSize = (areaWidth / 18f) * scaleRatio
        val textSizeBase = (areaWidth / 21f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }


    private inline fun drawDragRacingEntry(area: Rect,
                                           value: Number?,
                                           label: String,
                                           top: Float,
                                           left:Float,
                                           canvas: Canvas,
                                           textSizeBase: Float) {


        val currentXPos = area.centerX() / 1.5f
        val lastXPos = area.centerX() + 60f

        drawText(canvas, label, left, top, textSizeBase, color = Color.LTGRAY)
        drawText(canvas, timeToString(value), currentXPos, top, textSizeBase)

        drawText(canvas, timeToString(value), lastXPos, top, textSizeBase)
    }

    private inline fun timeToString(value: Number?): String = value?.toString() ?: "---"
}