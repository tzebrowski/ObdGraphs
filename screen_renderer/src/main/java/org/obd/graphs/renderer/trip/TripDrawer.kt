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
import org.obd.graphs.bl.collector.Metric
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

        val x = 135
        var rowTop = top + 12f
        drawMetric(tripInfo.airTemp!!, rowTop, left, canvas, textSizeBase)
        drawMetric(tripInfo.coolantTemp!!, rowTop, left + 1 * x, canvas, textSizeBase)
        drawMetric(tripInfo.oilTemp!!, rowTop, left + 2 * x, canvas, textSizeBase)
        drawMetric(tripInfo.exhaustTemp!!, rowTop, left + 3 * x, canvas, textSizeBase)
        drawMetric(tripInfo.gearboxOilTemp!!, rowTop, left + 4 * x, canvas, textSizeBase)
        drawMetric(tripInfo.ambientTemp!!, rowTop, left + 5 * x, canvas, textSizeBase)

        //second row
        rowTop = top + (textSizeBase) + 52f
        drawMetric(tripInfo.fuellevel!!, rowTop, left, canvas, textSizeBase)
        drawMetric(tripInfo.gearboxEngaged!!, rowTop, left + x, canvas, textSizeBase)
        drawMetric(tripInfo.atmPressure!!, rowTop, left + 2 * x, canvas, textSizeBase)
        drawMetric(tripInfo.totalMisfires!!, rowTop, left + 3 * x, canvas, textSizeBase)
        drawMetric(tripInfo.fuelConsumption!!, rowTop, left + 4 * x, canvas, textSizeBase)
        drawMetric(tripInfo.oilLevel!!, rowTop, left + 5 * x, canvas, textSizeBase)

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
    ): Float = (area.width() / 14f) * valueScaler.scaleToNewRange(
        settings.getDragRacingSettings().fontSize.toFloat(),
        CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX
    )


    private inline fun drawMetric(
        metrics: Metric,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float
    ) {
        drawText(canvas, metrics.valueToIntString(), left, top, textSizeBase, color = Color.WHITE, typeface =
        Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        drawTitle(canvas, metrics, left, top + 24, textSizeBase * 0.35F)
    }

    private fun drawTitle(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float
    ) {

        var top1 = top
        titlePaint.textSize = textSize
        titlePaint. typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

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

            }

        } else {
            val text = description.replace("\n", " ")
            canvas.drawText(
                text,
                left,
                top,
                titlePaint
            )
        }
    }
}