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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.commons.R
import org.obd.graphs.ui.common.COLOR_LIGHT_SHADE_GRAY
import org.obd.graphs.ui.common.COLOR_WHITE
import org.obd.graphs.ui.common.color
import kotlin.math.*


private const val DEFAULT_LONG_POINTER_SIZE = 1f
private const val SCALE_STEP = 2

private const val MAX_DIVIDER_WIDTH = 1.8f
private const val startAngle = 180
private const val sweepAngle = 180
private const val padding = 10f
private const val dividersCount = 10
private const val dividerStepAngle = sweepAngle / dividersCount
private const val VALUE_TEXT_SIZE_BASE = 46f

class GaugeRenderer(private val settings: ScreenSettings, context: Context) {
    private val valueScaler = ValueScaler()

    private val isDividerDrawFirst = true
    private val isDividerDrawLast = true
    private val strokeColor = Color.parseColor("#0D000000")
    private val backgroundPaint = Paint()

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    private val numbersPaint = Paint().apply {
        color = color(R.color.gray)
        isAntiAlias = true
    }

    private val valuePaint = Paint().apply {
        color = COLOR_WHITE
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = color(R.color.gray)
        isAntiAlias = true
    }

    private val histogramPaint = Paint().apply {
        color = COLOR_WHITE
        isAntiAlias = true
    }

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    fun drawGauge(
        canvas: Canvas, left: Float, top: Float, width: Float,
        metric: CarMetric,
        gaugeDrawScale: Boolean = true,
        screenArea: Rect
    ) {

        val startValue = metric.source.command.pid.min.toFloat()
        val endValue = metric.source.command.pid.max.toFloat()
        val value = metric.source.value?.toFloat() ?: metric.source.command.pid.min.toFloat()

        val strokeCap: String = Paint.Cap.BUTT.name

        var strokeWidth = 14f

        val pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
        val point = (startAngle + (value - startValue) * pointAngle).toInt()


        if (TextUtils.isEmpty(strokeCap)) {
            paint.strokeCap = Paint.Cap.BUTT
        } else {
            paint.strokeCap = Paint.Cap.valueOf(strokeCap)
        }

        val calculatedWidth = width - 2 * padding
        val height = width - 2 * padding

        val radius = if (calculatedWidth < height) calculatedWidth / 2 else height / 2

        val rect = calculateRect(left, width, top)

        val rescaleValue = scaleRation(rect,screenArea)
        val decorLineOffset = 8 * rescaleValue
        strokeWidth *= rescaleValue

        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        val decorRect = RectF()
        decorRect[rect.left - decorLineOffset,
                rect.top - decorLineOffset,
                rect.right + decorLineOffset] =
            rect.bottom + decorLineOffset

        paint.color = strokeColor

        canvas.drawArc(rect, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)

        paint.color = COLOR_LIGHT_SHADE_GRAY
        paint.strokeWidth = 2f

        canvas.drawArc(decorRect, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)

        paint.strokeWidth = strokeWidth
        paint.color = settings.colorTheme().progressColor

        if (value == startValue) {
            canvas.drawArc(
                rect, startAngle.toFloat(), DEFAULT_LONG_POINTER_SIZE, false,
                paint
            )
        } else {
            canvas.drawArc(
                rect, startAngle.toFloat(), (point - startAngle).toFloat(), false,
                paint
            )
        }

        val dividerWidth = min(sweepAngle / abs(endValue - startValue), MAX_DIVIDER_WIDTH)
        drawDivider(
            canvas,
            rect,
            dividerWidth
        )

        if (gaugeDrawScale) {
            drawNumbers(
                canvas, startValue,
                radius,
                endValue,
                decorRect,
                screenArea,
            )
        }

        drawMetric(canvas,  area = rect,metric = metric, screenArea =  screenArea)
    }

    fun drawBackground(canvas: Canvas, rect: Rect) {
        canvas.drawRect(rect, paint)
        canvas.drawColor(settings.getBackgroundColor())
        if (settings.isBackgroundDrawingEnabled()) {
            canvas.drawBitmap(background, rect.left.toFloat(), rect.top.toFloat(), backgroundPaint)
        }
    }


    private fun calculateRect(
        left: Float,
        pWidth: Float,
        top: Float
    ): RectF {

        val height = pWidth - 2 * padding
        val calculatedHeight =
            if (pWidth > height) pWidth else height

        val calculatedWidth = pWidth - 2 * padding

        val radius = if (calculatedWidth < height) calculatedWidth / 2 else height / 2

        val rectLeft = left + (pWidth - 2 * padding) / 2 - radius + padding
        val rectTop = top + (calculatedHeight - 2 * padding) / 2 - radius + padding
        val rectRight = left + (pWidth - 2 * padding) / 2 - radius + padding + calculatedWidth
        val rectBottom = top + (height - 2 * padding) / 2 - radius + padding + height
        val rect = RectF()
        rect[rectLeft, rectTop, rectRight] = rectBottom
        return rect
    }

    private fun drawDivider(
        canvas: Canvas, rect: RectF,
        dividerSize: Float
    ) {
        paint.color = COLOR_WHITE
        val i = if (isDividerDrawFirst) 0 else 1
        val max = if (isDividerDrawLast) dividersCount + 1 else dividersCount
        for (j in i..max step SCALE_STEP) {
            canvas.drawArc(
                rect,
                (startAngle + j * dividerStepAngle).toFloat(),
                dividerSize,
                false,
                paint
            )
        }
    }

    private fun drawMetric(
        canvas: Canvas,
        area: RectF,
        metric: CarMetric,
        screenArea: Rect
    ) {

        val value = metric.valueToString()
        valuePaint.textSize = VALUE_TEXT_SIZE_BASE * scaleRation(area, screenArea)
        val textRect = Rect()
        valuePaint.getTextBounds(value, 0, value.length, textRect)
        canvas.drawText(value, area.centerX() - (textRect.width()/2),area.centerY() - textRect.height() - 10, valuePaint)


        val label = metric.source.command.pid.description
        labelPaint.textSize = 16f * scaleRation(area, screenArea)
        val textRect1 = Rect()
        labelPaint.getTextBounds(label, 0, label.length, textRect1)
        val bb = area.centerY() - 10 - textRect.height()/2
        canvas.drawText(label, area.centerX() - (textRect1.width() / 2), bb, labelPaint)

        val hists  = "${metric.toNumber(metric.min)}    ${metric.toNumber(metric.mean)}     ${metric.toNumber(metric.max)}"
        histogramPaint.textSize = 18f * scaleRation(area, screenArea)
        val textRect2 = Rect()
        histogramPaint.getTextBounds(hists, 0, hists.length, textRect2)
        canvas.drawText(hists, area.centerX() - (textRect2.width() / 2), bb + textRect1.height() + 8, histogramPaint)
    }


    private fun drawNumbers(
        canvas: Canvas,
        startValue: Float,
        radius: Float,
        endValue: Float,
        area: RectF,
        screenArea: Rect,

    ) {
        val numberOfItems = (dividersCount / SCALE_STEP)
        val radiusFactor = 0.80f

        val scaleRation = scaleRation(area, screenArea)
        val stepValue = round((endValue - startValue) / numberOfItems)
        val baseRadius = radius * radiusFactor

        for (i in 0..numberOfItems) {
            val text = (round(startValue + stepValue * i)).toInt().toString()
            val rect = Rect()
            numbersPaint.getTextBounds(text, 0, text.length, rect)
            numbersPaint.textSize = 12f *  scaleRation
            val angle = Math.PI / numberOfItems * (i - numberOfItems).toFloat()
            val x = area.left  + (area.width() / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
            val y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    private fun scaleRation(area: RectF, screenArea: Rect): Float = valueScaler.scaleToNewRange(
            area.width() * area.height(),
            0.0f,
            screenArea.width().toFloat() * screenArea.height(),
            0.5f,
            2f
        )
}