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
import org.obd.graphs.bl.collector.MetricsBuilder
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.drag.MARGIN_END
import org.obd.metrics.api.model.ObdMetric

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

@Suppress("NOTHING_TO_INLINE")
internal class TripDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val metricBuilder = MetricsBuilder()

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        tripInfo: TripInfoDetails
    ) {

        val (textSizeBase) = calculateFontSize(area)

        val x = 135
        var rowTop = top + 12f
        drawMetric(tripInfo.airTemp!!, top = rowTop, left = left, canvas, textSizeBase, statsEnabled = true)
        drawMetric(tripInfo.coolantTemp!!, rowTop, left + 1 * x, canvas, textSizeBase, statsEnabled = true)
        drawMetric(tripInfo.oilTemp!!, rowTop, left + 2 * x, canvas, textSizeBase, statsEnabled = true)
        drawMetric(tripInfo.exhaustTemp!!, rowTop, left + 3 * x, canvas, textSizeBase, statsEnabled = true)
        drawMetric(tripInfo.gearboxOilTemp!!, rowTop, left + 4 * x, canvas, textSizeBase, statsEnabled = true)
        drawMetric(diff(tripInfo.distance!!), rowTop, left + 5 * x, canvas, textSizeBase)

        //second row
        rowTop = top + (textSizeBase) + 52f
        drawMetric(tripInfo.fuellevel!!, rowTop, left, canvas, textSizeBase)
        drawMetric(diff(tripInfo.fuelConsumed!!), rowTop, left + 1 * x, canvas, textSizeBase)
        drawMetric(tripInfo.fuelConsumption!!, rowTop, left + 2 * x, canvas, textSizeBase, statsEnabled = true, unitEnabled = false)
        drawMetric(tripInfo.gearboxEngaged!!, rowTop, left + 3 * x, canvas, textSizeBase)
        drawMetric(tripInfo.oilLevel!!, rowTop, left + 4 * x, canvas, textSizeBase, statsEnabled = true)
        drawMetric(tripInfo.totalMisfires!!, rowTop, left + 5 * x, canvas, textSizeBase, unitEnabled = false)

        drawDivider(canvas, left, area.width().toFloat(), rowTop + textSizeBase + 2, Color.DKGRAY)

        //metrics
        rowTop += 2.2f * textSizeBase
        drawMetric(canvas, area, tripInfo.intakePressure!!, left, rowTop)
        drawMetric(canvas, area, tripInfo.torque!!, left + getAreaWidth(area) + 10, rowTop)
    }

    private fun diff(metric: Metric) : Metric  {
        val value: Number? = if (metric.source.value == null){
            null
        } else {
            metric.max - metric.min
        }

        return metricBuilder.buildFor(ObdMetric.builder()
            .command(metric.source.command)
            .value(value).build())
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(
            settings.getTripInfoSettings().fontSize.toFloat(),
            CURRENT_MIN,
            CURRENT_MAX,
            NEW_MIN,
            NEW_MAX
        )

        val areaWidth = area.width()
        val valueTextSize = (areaWidth / 17f) * scaleRatio
        val textSizeBase = (areaWidth / 22f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }

    private inline fun drawMetric(
        canvas: Canvas,
        area: Rect,
        metric: Metric,
        left: Float,
        top: Float
    ): Float {

        val (valueTextSize, textSizeBase) = calculateFontSize(area)

        var top1 = top
        var left1 = left

        drawTitle(
            canvas,
            metric,
            left1,
            top1,
            textSizeBase
        )


        drawValue(
            canvas,
            metric,
            top1 + 44,
            valueTextSize,
            left = left + getAreaWidth(area) - 50f
        )

        top1 += 52f

        if (settings.isStatisticsEnabled()) {
            val tt = textSizeBase * 0.6f
            if (metric.source.command.pid.historgam.isMinEnabled) {
                left1 = drawText(
                    canvas,
                    "min",
                    left,
                    top1,
                    Color.LTGRAY,
                    tt * 0.8f,
                    valuePaint
                )
                left1 = drawText(
                    canvas,
                    metric.toNumber(metric.min),
                    left1,
                    top1,
                    Color.LTGRAY,
                    tt,
                    valuePaint
                )
            }
            if (metric.source.command.pid.historgam.isMaxEnabled) {
                left1 = drawText(
                    canvas,
                    "max",
                    left1,
                    top1,
                    Color.LTGRAY,
                    tt * 0.8f,
                    valuePaint
                )
                drawText(
                    canvas,
                    metric.toNumber(metric.max),
                    left1,
                    top1,
                    Color.LTGRAY,
                    tt,
                    valuePaint
                )
            }


            top1 += getTextHeight("min", paint) / 2
        }

        top1 += 4f

        drawProgressBar(
            canvas,
            left,
            getAreaWidth(area), top1, metric,
            color = settings.getColorTheme().progressColor
        )

        top1 += calculateDividerSpacing()

        drawDivider(
            canvas,
            left, getAreaWidth(area), top1,
            color = settings.getColorTheme().dividerColor
        )

        top1 += 10f + (textSizeBase).toInt()
        return top1
    }

    private inline fun calculateDividerSpacing(): Int = 14
    private inline fun getAreaWidth(area: Rect): Float = area.width().toFloat() / 2

    private fun drawProgressBar(
        canvas: Canvas,
        left: Float,
        width: Float,
        top: Float,
        it: Metric,
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

    private fun drawValue(
        canvas: Canvas,
        metric: Metric,
        top: Float,
        textSize: Float,
        left: Float,
    ) {

        valuePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        valuePaint.color = Color.WHITE

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

    private fun drawValue(
        canvas: Canvas,
        metric: Metric,
        top: Float,
        textSize: Float,
        left: Float,
        typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
        color: Int = Color.WHITE,
        statsEnabled: Boolean,
        unitEnabled: Boolean

    ) {

        valuePaint.typeface = typeface
        valuePaint.color = color

        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)
        valuePaint.textSize = textSize
        val text = metric.source.valueToString()
        canvas.drawText(text, left, top, valuePaint)
        var textWidth = getTextWidth(text, valuePaint) + 2

        if (unitEnabled) {
            valuePaint.color = Color.LTGRAY
            valuePaint.textSize = (textSize * 0.4).toFloat()
            canvas.drawText(metric.source.command.pid.units, (left + textWidth), top, valuePaint)
            textWidth += getTextWidth(metric.source.command.pid.units, valuePaint) + 2

        }

        if (settings.isStatisticsEnabled() && statsEnabled) {
            valuePaint.color = Color.LTGRAY
            valuePaint.textSize = (textSize * 0.65).toFloat()
            canvas.drawText(metric.toNumber(metric.min), (left + textWidth), top, valuePaint)
            canvas.drawText(metric.toNumber(metric.max), (left + textWidth), top - 20, valuePaint)
        }
    }

    private fun calculateProgressBarHeight() = 16

    private inline fun drawMetric(
        metric: Metric,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float,
        statsEnabled: Boolean = false,
        unitEnabled: Boolean = true
    ) {

        drawValue(
            canvas,
            metric,
            top = top,
            textSize = textSizeBase * 0.8f,
            left = left,
            color = Color.WHITE,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
            statsEnabled = statsEnabled,
            unitEnabled = unitEnabled

        )

        drawTitle(canvas, metric, left, top + 24, textSizeBase * 0.35F)
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
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

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