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
package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.*
import android.util.Log
import org.obd.graphs.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.commons.R
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.ui.common.*
import kotlin.math.*

private const val MIN_TEXT_VALUE_HEIGHT = 30
private const val NUMERALS_RADIUS_SCALE_FACTOR = 0.75f

data class DrawerSettings(
    val gaugeProgressWidth: Float = 1.5f,
    val gaugeProgressBarType: GaugeProgressBarType = GaugeProgressBarType.LONG,
    val startAngle: Float = 200f,
    val sweepAngle: Float = 180f,
    val scaleStep: Int = 2,
    val dividersCount: Int = 12,
    val dividersStepAngle: Float = sweepAngle / dividersCount,
    val longPointerSize: Float = 1f,
    val padding: Float = 10f,
    val dividerWidth: Float = 1f,
    val lineOffset: Float = 8f,
    val valueTextSize: Float = 46f,
    val labelTextSize: Float = 16f,
    val scaleNumbersTextSize: Float = 12f,
    val dividerHighlightStart: Int = 9,
)

@Suppress("NOTHING_TO_INLINE")
internal class GaugeDrawer(
    settings: ScreenSettings,
    context: Context,
    private val drawerSettings: DrawerSettings = DrawerSettings(),
): AbstractDrawer(context, settings) {

    private val strokeColor = Color.parseColor("#0D000000")

    private val numbersPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.gray)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.gray)
    }

    private val histogramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_WHITE
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.BUTT
        style = Paint.Style.STROKE
        color = COLOR_WHITE
    }

    private val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.BUTT
    }

    fun drawGauge(canvas: Canvas, metric: Metric, left: Float, top: Float, width: Float,
                  fontSize: Int = settings.getGaugeRendererSetting().getFontSize(),
                  labelCenterYPadding: Float = 0f,
                  scaleEnabled: Boolean = settings.isScaleEnabled()) {
        paint.shader = null

        val rect = calculateRect(left, width, top)

        val rescaleValue = scaleRationBasedOnScreenSize(rect)
        val arcTopOffset = 8 * rescaleValue
        val strokeWidth = 8f * rescaleValue

        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        paint.color = strokeColor

        canvas.drawArc(rect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        paint.color = color(R.color.gray_dark)
        paint.strokeWidth = 2f

        val arcTopRect = RectF()
        arcTopRect[rect.left - arcTopOffset,
                rect.top - arcTopOffset,
                rect.right + arcTopOffset] =
            rect.bottom + arcTopOffset

        canvas.drawArc(arcTopRect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        val r2 = RectF()
        val r2Offset = arcTopOffset * 3
        r2[rect.left + r2Offset,
                rect.top + r2Offset,
                rect.right - r2Offset] =
            rect.bottom - r2Offset

        pp.color = color(R.color.black)
        canvas.drawArc(r2, drawerSettings.startAngle, drawerSettings.sweepAngle, false, pp)

        val arcBottomRect = RectF()
        val r3Offset = arcTopOffset + 4
        arcBottomRect[rect.left + r3Offset,
                rect.top + r3Offset,
                rect.right - r3Offset] =
            rect.bottom - r3Offset

        canvas.drawArc(arcBottomRect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        drawProgressBar(metric, canvas, rect, (arcBottomRect.top - arcTopRect.top - 2f))

        paint.strokeWidth = strokeWidth

        drawScale(
            canvas,
            rect
        )

        if (scaleEnabled) {
            drawNumerals(
                metric,
                canvas,
                calculateRadius(width),
                arcTopRect,
            )
        }

        drawGauge(canvas, area = rect, metric = metric, radius = calculateRadius(width),labelCenterYPadding = labelCenterYPadding,
            fontSize = fontSize)
    }

    private fun drawProgressBar(
        metric: Metric,
        canvas: Canvas,
        rect: RectF,
        strokeWidth: Float
    ) {
        if (metric.source.isNumber()){
            val progressRect = RectF()
            val progressRectOffset = 2f
            progressRect[rect.left + progressRectOffset,
                    rect.top + progressRectOffset,
                    rect.right - progressRectOffset] =
                rect.bottom - progressRectOffset

            if (settings.isProgressGradientEnabled()) {
                setProgressGradient(rect)
            }

            val value = metric.source.toFloat()
            val startValue = metric.pid().min.toFloat()
            val endValue = metric.pid().max.toFloat()

            if (value == startValue) {
                canvas.drawArc(
                    progressRect, drawerSettings.startAngle, drawerSettings.longPointerSize, false,
                    progressPaint
                )
            } else {

                val pointAngle = abs(drawerSettings.sweepAngle).toDouble() / (endValue - startValue)
                val point = (drawerSettings.startAngle + (value - startValue) * pointAngle).toInt()
                when (drawerSettings.gaugeProgressBarType){
                    GaugeProgressBarType.SHORT -> {
                        progressPaint.strokeWidth = strokeWidth

                        canvas.drawArc(
                            progressRect, drawerSettings.startAngle + (point - drawerSettings.startAngle),
                            drawerSettings.gaugeProgressWidth, false,
                            progressPaint
                        )
                    }
                    GaugeProgressBarType.LONG -> {
                        progressPaint.strokeWidth = strokeWidth/2f
                        canvas.drawArc(
                            progressRect, drawerSettings.startAngle, (point - drawerSettings.startAngle), false,
                            progressPaint
                        )
                    }
                }
            }
            paint.shader = null
        }
    }


    private fun calculateRect(
        left: Float,
        width: Float,
        top: Float
    ): RectF {

        val height = width - 2 * drawerSettings.padding
        val calculatedHeight = if (width > height) width else height
        val calculatedWidth = width - 2 * drawerSettings.padding
        val radius = calculateRadius(width)

        val rectLeft = left + (width - 2 * drawerSettings.padding) / 2 - radius + drawerSettings.padding
        val rectTop = top + (calculatedHeight - 2 * drawerSettings.padding) / 2 - radius + drawerSettings.padding
        val rectRight = left + (width - 2 * drawerSettings.padding) / 2 - radius + drawerSettings.padding + calculatedWidth
        val rectBottom = top + (height - 2 * drawerSettings.padding) / 2 - radius + drawerSettings.padding + height
        val rect = RectF()
        rect[rectLeft, rectTop, rectRight] = rectBottom
        return rect
    }

    private fun setProgressGradient(rect: RectF) {
        val colors = intArrayOf(COLOR_WHITE, settings.getColorTheme().progressColor)
        val gradient = SweepGradient(rect.centerY(), rect.centerX(), colors, null)
        val matrix = Matrix()
        matrix.postRotate(90f, rect.centerY(), rect.centerX())
        gradient.setLocalMatrix(matrix)
        paint.shader = gradient
    }

    private fun drawGauge(
        canvas: Canvas,
        area: RectF,
        metric: Metric,
        radius: Float,
        labelCenterYPadding:Float = 0f,
        fontSize: Int
    ) {
        val calculatedFontSize = calculateFontSize(multiplier = area.width() / 22f, fontSize=fontSize) *  3.4f

        val value = metric.source.format(castToInt = false)
        valuePaint.textSize = calculatedFontSize
        valuePaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)
        valuePaint.color = colorScheme(metric)

        val textRect = Rect()
        valuePaint.getTextBounds(value, 0, value.length, textRect)

        var centerY = (area.centerY() + labelCenterYPadding - (if (settings.isStatisticsEnabled()) 8 else 1) * scaleRationBasedOnScreenSize(area))
        val valueHeight = max(textRect.height(), MIN_TEXT_VALUE_HEIGHT) + settings.getGaugeRendererSetting().topOffset
        val valueY = centerY - valueHeight
        canvas.drawText(value, area.centerX() - (textRect.width() / 2), valueY, valuePaint)

        valuePaint.textSize = calculatedFontSize * 0.4f
        valuePaint.color = color(R.color.gray)

        val unitRect = Rect()
        val pid = metric.pid()

        val unitY = centerY - valueHeight

        pid.units?.let {
            valuePaint.getTextBounds(it, 0, it.length, unitRect)
            canvas.drawText(it, area.centerX() + textRect.width() / 2  + 4, unitY, valuePaint)
            centerY += unitRect.height() / 2
        }

        labelPaint.textSize = calculatedFontSize * 0.6f
        labelPaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)

        var labelY = 0f

        val text = pid.description.split("\n")
        if (settings.isBreakLabelTextEnabled()  && text.size > 1) {
            labelPaint.textSize *= 0.95f
            text.forEachIndexed { i, it ->
                val labelRect = Rect()
                labelPaint.getTextBounds( it, 0,  it.length, labelRect)
                labelY = unitY + (i+1) *  labelPaint.textSize
                canvas.drawText(
                    it,
                    area.centerX() - (labelRect.width() / 2),
                    labelY,
                    labelPaint
                )
            }
        } else {
            val label = pid.description

            val labelRect = Rect()
            labelPaint.getTextBounds(label, 0, label.length, labelRect)

            labelY = unitY + labelRect.height()
            canvas.drawText(label, area.centerX() - (labelRect.width() / 2), labelY, labelPaint)
        }

        if (settings.isStatisticsEnabled()) {

            val hists =
                "${if(pid.historgam.isMinEnabled) metric.min.format(pid) else ""}   " +
                        "${if(pid.historgam.isAvgEnabled) metric.mean.format(pid) else ""}    " +
                        "${if(pid.historgam.isMaxEnabled) metric.max.format(pid) else ""}"
            histogramPaint.textSize = calculatedFontSize * 0.4f
            val histsRect = Rect()
            histogramPaint.getTextBounds(hists, 0, hists.length, histsRect)
            val histY = labelY + histsRect.height()
            canvas.drawText(hists, area.centerX() - (histsRect.width() / 2), histY + 8, histogramPaint)
        }
    }


    private inline fun scaleColor(j: Int): Int = if (j == drawerSettings.dividerHighlightStart || j == drawerSettings.dividersCount) {
        settings.getColorTheme().progressColor
    } else {
        color(R.color.gray_light)
    }

    private fun drawScale(
        canvas: Canvas, rect: RectF
    ) {
        val scaleRect = RectF()

        scaleRect[rect.left + drawerSettings.lineOffset,
                rect.top + drawerSettings.lineOffset,
                rect.right - drawerSettings.lineOffset] =
            rect.bottom - drawerSettings.lineOffset

        val start = 0
        val end = drawerSettings.dividersCount + 1

        drawScale(canvas, scaleRect, start, end, paintColor = {
            if (it == 10 || it == 12) {
                settings.getColorTheme().progressColor
            } else {
                color(R.color.gray_light)
            }
        }) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle
        }

        drawScale(canvas, scaleRect, start, drawerSettings.dividersCount + 2) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle * 0.5f
        }

        drawScale(canvas, rect, start, end, paintColor = { scaleColor(it) }) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle
        }

        drawScale(canvas, rect, (drawerSettings.dividersStepAngle * drawerSettings.dividerHighlightStart + 3).toInt(),
            (drawerSettings.dividersStepAngle * (drawerSettings.dividersCount - 1)).toInt(),
            paintColor = { settings.getColorTheme().progressColor }) {
            drawerSettings.startAngle + it
        }

        val width = (drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 1)) -
                (drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 3))

        canvas.drawArc(
            rect,
            drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 2),
            width,
            false,
            paint
        )
    }

    private fun drawScale(
        canvas: Canvas,
        rect: RectF,
        start: Int,
        end: Int,
        width: Float = drawerSettings.dividerWidth,
        paintColor: (j: Int) -> Int = { color(R.color.gray_light) },
        angle: (j: Int) -> Float
    ) {
        for (j in start..end step drawerSettings.scaleStep) {

            paint.color = paintColor(j)
            canvas.drawArc(
                rect,
                angle(j),
                width,
                false,
                paint
            )
        }
    }

    private fun drawNumerals(
        metric: Metric,
        canvas: Canvas,
        radius: Float,
        area: RectF,
    ) {
        if (metric.source.isNumber()) {

            val pid = metric.pid()
            val startValue = pid.min.toDouble()
            val endValue = pid.max.toDouble()

            val numberOfItems = (drawerSettings.dividersCount / drawerSettings.scaleStep)

            val scaleRation = scaleRationBasedOnScreenSize(area, targetMin = 0.4f, targetMax = 1.9f)
            val stepValue = (endValue - startValue) / numberOfItems
            val baseRadius = radius * NUMERALS_RADIUS_SCALE_FACTOR

            val start = 0
            val end = drawerSettings.dividersCount + 1

            for (j in start..end step drawerSettings.scaleStep) {
                val angle = (drawerSettings.startAngle + j * drawerSettings.dividersStepAngle) * (Math.PI / 180)
                val text = valueAsString(metric, value = (startValue + stepValue * j / drawerSettings.scaleStep).round(1))
                val rect = Rect()
                numbersPaint.getTextBounds(text, 0, text.length, rect)
                numbersPaint.textSize = drawerSettings.scaleNumbersTextSize * scaleRation

                val x = area.left + (area.width() / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
                val y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

                numbersPaint.color =
                    if (j == (numberOfItems - 1) * drawerSettings.scaleStep || j == numberOfItems * drawerSettings.scaleStep) {
                        settings.getColorTheme().progressColor
                    } else {
                        color(R.color.gray)
                    }

                canvas.drawText(text, x, y, numbersPaint)
            }
        }
    }

    private inline fun valueAsString(metric: Metric, value: Double): String = if (metric.source.command.pid.max.toInt() > 20) {
        value.toInt().toString()
    } else {
        value.toString()
    }

    private fun scaleRationBasedOnScreenSize(area: RectF):Float  = scaleRationBasedOnScreenSize(area = area, targetMin = 0.7f, targetMax = 2.4f)

    private fun scaleRationBasedOnScreenSize(area: RectF, targetMin:Float = 0.7f, targetMax: Float = 2.4f): Float = valueConverter.scaleToNewRange(
        currentValue = area.width() * area.height(),
        currentMin = 8875f,
        currentMax = (settings.getHeightPixels() * settings.getWidthPixels()) * 0.9f,
        targetMin = targetMin,
        targetMax = targetMax
    )

    private fun calculateRadius(width: Float): Float {
        val calculatedWidth = width - 2 * drawerSettings.padding
        val height = width - 2 * drawerSettings.padding
        return if (calculatedWidth < height) calculatedWidth / 2 else height / 2
    }
}
