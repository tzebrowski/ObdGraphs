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

        val textSizeBase = calculateFontSize(area)

        val x = 130
        var rowTop = top + 10f
        drawMetric(tripInfo.airTemp, "Intake Temp", rowTop, left, canvas, textSizeBase)
        drawMetric(tripInfo.ambientTemp, "Ambient Temp", rowTop, left + x, canvas, textSizeBase)
        drawMetric(tripInfo.coolantTemp, "Coolant Temp", rowTop, left + 2 * x, canvas, textSizeBase)
        drawMetric(tripInfo.oilTemp, "Oil Temp", rowTop, left + 3 * x, canvas, textSizeBase)
        drawMetric(tripInfo.exhaustTemp, "Exhaust Temp", rowTop, left + 4 * x, canvas, textSizeBase)
        drawMetric(tripInfo.gearboxOilTemp, "Gearbox Temp", rowTop, left + 5 * x, canvas, textSizeBase)


        rowTop = top + (textSizeBase) + 42f
        drawMetric(tripInfo.fuellevel, "Fuel Level", rowTop, left, canvas, textSizeBase)
        drawMetric(tripInfo.gearboxEngaged, "Selected gear", rowTop, left + x, canvas, textSizeBase)
        drawMetric(tripInfo.atmPressure, "Atm pressure", rowTop, left + 2 * x, canvas, textSizeBase)
        drawMetric(tripInfo.vehicleSpeed, "Vehicle speed", rowTop, left + 3 * x, canvas, textSizeBase)
        drawMetric(tripInfo.fuelConsumption, "Fuel Consumption", rowTop, left + 4 * x, canvas, textSizeBase)

    }


    private fun drawText(
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
    ): Float = (area.width() / 12f) * valueScaler.scaleToNewRange(
        settings.getDragRacingSettings().fontSize.toFloat(),
        CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX
    )


    private inline fun drawMetric(
        value: Number?,
        label: String,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float
    ) {
        drawText(canvas, timeToString(value), left, top, textSizeBase, color = Color.WHITE)
        drawText(canvas, label, left, top + 24, textSizeBase * 0.35F, color = Color.LTGRAY)
    }

    private inline fun timeToString(value: Number?): String = value?.toString() ?: "---"
}