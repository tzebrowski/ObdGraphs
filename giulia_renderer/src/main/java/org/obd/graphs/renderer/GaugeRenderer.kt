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
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_LIGHT_SHADE_GRAY
import org.obd.graphs.ui.common.COLOR_WHITE
import org.obd.graphs.ui.common.color
import kotlin.math.*


private const val DEFAULT_LONG_POINTER_SIZE = 1f
private const val SCALE_STEP = 2

private const val MAX_DIVIDER_SIZE = 1.8f
private const val startAngle = 180
private const val sweepAngle = 180
private const val padding = 10f
private const val dividersCount = 10

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

        val pointSize = 0
        val strokeCap: String = Paint.Cap.BUTT.name

        var strokeWidth = 10f

        val pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
        val point = (startAngle + (value - startValue) * pointAngle).toInt()

        val dividerSize = min(sweepAngle / abs(endValue - startValue), MAX_DIVIDER_SIZE)

        val dividerStepAngle = sweepAngle / dividersCount

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
        val decorLineOffset = 12 * rescaleValue
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
        paint.color = COLOR_DYNAMIC_SELECTOR_SPORT

        if (pointSize > 0) {

            if (point > startAngle + pointSize / 2) {
                canvas.drawArc(
                    rect, (point - pointSize / 2).toFloat(), pointSize.toFloat(), false,
                    paint
                )
            } else {
                canvas.drawArc(rect, point.toFloat(), pointSize.toFloat(), false, paint)
            }
        } else {
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
        }

        if (dividerSize > 0) {
            drawDivider(
                canvas,
                rect,
                isDividerDrawFirst,
                isDividerDrawLast,
                dividersCount,
                startAngle,
                dividerStepAngle,
                dividerSize
            )
        }
        if (gaugeDrawScale) {
            drawNumbers(
                canvas, dividersCount, startValue,
                radius,
                endValue,
                decorRect,
                screenArea,
            )
        }

        drawValue(canvas,  area = rect, text = metric.valueToString(), screenArea)
        drawLabel(canvas,  area = rect, label = metric.source.command.pid.description, screenArea)
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
        isDividerDrawFirst: Boolean, isDividerDrawLast: Boolean, dividersCount: Int,
        startAngle: Int, dividerStepAngle: Int, dividerSize: Float
    ) {
        paint.color = COLOR_WHITE
        paint.shader = null
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

    private fun drawLabel(
        canvas: Canvas,
        area: RectF,
        label: String,
        screenArea: Rect
    ) {

        labelPaint.textSize = 14f * scaleRation(area, screenArea)
        val textRect = Rect()
        labelPaint.getTextBounds(label, 0, label.length, textRect)
        canvas.drawText(label, area.centerX() - (textRect.width() / 2), area.centerY(), labelPaint)
    }

    private fun drawValue(
        canvas: Canvas,
        area: RectF,
        text: String,
        screenArea: Rect
    ) {

        valuePaint.textSize = 40f * scaleRation(area, screenArea)
        val textRect = Rect()
        valuePaint.getTextBounds(text, 0, text.length, textRect)
        canvas.drawText(text, area.centerX() - (textRect.width()/2),area.centerY() - textRect.height(), valuePaint)
    }

    private fun drawNumbers(
        canvas: Canvas, dividersCount: Int,
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
            var y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

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