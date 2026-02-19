 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.commons.R
import org.obd.graphs.format
import org.obd.graphs.getContext
import org.obd.graphs.isNumber
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.round
import org.obd.graphs.toFloat
import org.obd.graphs.ui.common.COLOR_WHITE
import org.obd.graphs.ui.common.color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MIN_TEXT_VALUE_HEIGHT = 30
private const val NUMERALS_RADIUS_SCALE_FACTOR = 0.75f
private const val CACHE_SCALE = 2f

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

private data class ScaleBitmapCache(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val dividerCount: Int,
    val progressColor: Int,
    val scaleEnabled: Boolean,
)

@Suppress("NOTHING_TO_INLINE")
internal class GaugeDrawer(
    settings: ScreenSettings,
    context: Context,
    private val drawerSettings: DrawerSettings = DrawerSettings(),
) : AbstractDrawer(context, settings) {
    private val numbersPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = color(R.color.gray)
        }

    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = color(R.color.gray)
        }

    private val histogramPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
        }

    private val progressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.BUTT
            style = Paint.Style.STROKE
            color = COLOR_WHITE
        }

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }

    private val backgroundGradientPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f * context.resources.displayMetrics.density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.gray)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var scaleBitmapCache: ScaleBitmapCache? = null

    fun drawGauge(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        width: Float,
        fontSize: Int = settings.getGaugeRendererSetting().getFontSize(),
        labelCenterYPadding: Float = 0f,
        scaleEnabled: Boolean = settings.isScaleEnabled(),
        statsEnabled: Boolean = settings.isStatisticsEnabled(),
        drawBorder: Boolean = false,
        borderArea: RectF? = null,
        drawModule: Boolean = false
    ) {
        paint.shader = null

        val dynamicPadding = max(drawerSettings.padding, width * 0.055f)

        val rect = calculateRect(left, width, top, dynamicPadding)
        val radius = calculateRadius(width, dynamicPadding)

        val strokeWidth = rect.width() * 0.037f

        val arcTopRect = RectF()
        arcTopRect[
            rect.left - strokeWidth,
            rect.top - strokeWidth,
            rect.right + strokeWidth,
        ] = rect.bottom + strokeWidth

        if (drawModule) {
            drawModuleName(metric, rect, fontSize, width, left, top, canvas)
        }

        if (drawBorder) {
            drawBorder(canvas, width, left, top, borderArea)
        }

        drawContainerBackground(canvas, width, left, top, borderArea)

        drawBackground(canvas, rect, arcTopRect, strokeWidth, strokeWidth, metric)

        drawScale(
            canvas,
            rect,
            arcTopRect,
            metric,
            scaleEnabled,
            radius,
            dynamicPadding,
        )

        drawStatistics(
            canvas,
            area = rect,
            metric = metric,
            radius = radius,
            labelCenterYPadding = labelCenterYPadding,
            fontSize = fontSize,
            statsEnabled = statsEnabled,
            borderArea = borderArea,
        )
    }

    private fun drawModuleName(
        metric: Metric,
        rect: RectF,
        fontSize: Int,
        width: Float,
        left: Float,
        top: Float,
        canvas: Canvas
    ) {

        if (!metric.moduleName.isNullOrEmpty()) {
            val baseFontSize = calculateFontSize(multiplier = rect.width() / 22f, fontSize = fontSize)
            modulePaint.textSize = baseFontSize * 0.6f

            val cornerOffset = width * 0.015f

            val textX = left + cornerOffset
            val textY = top + cornerOffset + modulePaint.textSize
            canvas.drawText(metric.moduleName!!, textX, textY, modulePaint)
        }
    }

    private fun drawContainerBackground(
        canvas: Canvas,
        width: Float,
        left: Float,
        top: Float,
        area: RectF?,
        gradientColor: Int = settings.getGaugeRendererSetting().getGaugeContainerColor(),
    ) {
        val destRect =
            area ?: run {
                val borderPadding = width * 0.01f
                RectF(
                    left + borderPadding,
                    top + borderPadding,
                    left + width - borderPadding,
                    top + width - borderPadding,
                )
            }

        val gradientRadius = min(destRect.width(), destRect.height()) * 0.45f
        val edgeColor = Color.TRANSPARENT
        val gradient =
            RadialGradient(
                destRect.centerX(),
                destRect.centerY(),
                gradientRadius,
                intArrayOf(gradientColor, edgeColor),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP,
            )

        backgroundGradientPaint.shader = gradient
        val cornerRadius = 8f * getContext()!!.resources.displayMetrics.density
        canvas.drawRoundRect(destRect, cornerRadius, cornerRadius, backgroundGradientPaint)
    }

    private fun drawBorder(
        canvas: Canvas,
        width: Float,
        left: Float,
        top: Float,
        area: RectF?,
    ) {
        val borderRect =
            if (area != null) {
                area
            } else {
                val borderPadding = width * 0.01f
                RectF(
                    left + borderPadding,
                    top + borderPadding,
                    left + width - borderPadding,
                    top + width - borderPadding,
                )
            }
        val cornerRadius = width * 0.04f
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)
    }

    private fun drawBackground(
        canvas: Canvas,
        rect: RectF,
        arcTopRect: RectF,
        arcTopOffset: Float,
        strokeWidth: Float,
        metric: Metric,
    ) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#0D000000")
        paint.strokeWidth = strokeWidth
        canvas.drawArc(rect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        paint.color = color(R.color.gray_dark)
        paint.strokeWidth = 2f
        canvas.drawArc(arcTopRect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        val backgroundArcRect = RectF()
        val r2Offset = arcTopOffset * 3
        backgroundArcRect[
            rect.left + r2Offset,
            rect.top + r2Offset,
            rect.right - r2Offset,
        ] = rect.bottom - r2Offset

        val arcBottomRect = RectF()
        val r3Offset = arcTopOffset + 4
        arcBottomRect[
            rect.left + r3Offset,
            rect.top + r3Offset,
            rect.right - r3Offset,
        ] = rect.bottom - r3Offset

        canvas.drawArc(arcBottomRect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        val progressBarHeight = (arcBottomRect.top - arcTopRect.top - 2f)
        drawProgressBar(metric, canvas, rect, progressBarHeight)

        paint.strokeWidth = strokeWidth
    }

    private fun drawProgressBar(
        metric: Metric,
        canvas: Canvas,
        rect: RectF,
        strokeWidth: Float,
    ) {
        if (metric.source.isNumber()) {
            val progressRect = RectF()
            val progressRectOffset = 2f
            progressRect[
                rect.left + progressRectOffset,
                rect.top + progressRectOffset,
                rect.right - progressRectOffset,
            ] = rect.bottom - progressRectOffset

            if (settings.isProgressGradientEnabled()) {
                setProgressGradient(rect)
            }

            val value = metric.source.toFloat()
            val startValue = metric.pid.min.toFloat()
            val endValue = metric.pid.max.toFloat()

            if (value == startValue) {
                canvas.drawArc(
                    progressRect,
                    drawerSettings.startAngle,
                    drawerSettings.longPointerSize,
                    false,
                    progressPaint,
                )
            } else {
                val pointAngle = abs(drawerSettings.sweepAngle).toDouble() / (endValue - startValue)
                val point = (drawerSettings.startAngle + (value - startValue) * pointAngle).toInt()
                val currentSweep =
                    if (drawerSettings.gaugeProgressBarType == GaugeProgressBarType.SHORT) {
                        drawerSettings.gaugeProgressWidth
                    } else {
                        (point - drawerSettings.startAngle)
                    }

                val startAngle =
                    if (drawerSettings.gaugeProgressBarType == GaugeProgressBarType.SHORT) {
                        drawerSettings.startAngle + (point - drawerSettings.startAngle)
                    } else {
                        drawerSettings.startAngle
                    }

                val progressBarWidth =
                    if (drawerSettings.gaugeProgressBarType ==
                        GaugeProgressBarType.SHORT
                    ) {
                        strokeWidth
                    } else {
                        strokeWidth / 2f
                    }

                glowPaint.color = settings.getColorTheme().progressColor
                glowPaint.strokeWidth = progressBarWidth * 2.5f

                canvas.drawArc(
                    progressRect,
                    startAngle,
                    currentSweep,
                    false,
                    glowPaint,
                )

                progressPaint.strokeWidth = progressBarWidth
                canvas.drawArc(
                    progressRect,
                    startAngle,
                    currentSweep,
                    false,
                    progressPaint,
                )
            }
            paint.shader = null
        }
    }

    private fun drawStatistics(
        canvas: Canvas,
        area: RectF,
        metric: Metric,
        radius: Float,
        labelCenterYPadding: Float = 0f,
        fontSize: Int,
        statsEnabled: Boolean,
        borderArea: RectF? = null,
    ) {
        val calculatedFontSize = calculateFontSize(multiplier = area.width() / 22f, fontSize = fontSize) * 3.8f
        val value = metric.source.format(castToInt = false)

        valuePaint.textSize = calculatedFontSize

        val textRect = Rect()
        valuePaint.getTextBounds(value, 0, value.length, textRect)

        val pid = metric.pid
        val unitText = pid.units
        var unitWidth = 0f
        val unitRect = Rect()

        if (unitText != null) {
            valuePaint.textSize = calculatedFontSize * 0.32f
            valuePaint.getTextBounds(unitText, 0, unitText.length, unitRect)
            unitWidth = unitRect.width().toFloat()
            valuePaint.textSize = calculatedFontSize
        }

        val unitPadding = calculatedFontSize * 0.3f
        var valueX = area.centerX() - (textRect.width() / 2f)

        if (value.length >= 4 && unitText != null) {
            val totalWidth = textRect.width() + unitPadding + unitWidth
            valueX = area.centerX() - (totalWidth / 2f)
        }

        val verticalShift = if (statsEnabled) 14 else 1

        val relativeFontSize = calculatedFontSize / area.height()
        val dynamicTopOffset = area.height() * (0.10f - relativeFontSize * 0.2f)

        var centerY = (area.centerY() + dynamicTopOffset + labelCenterYPadding - verticalShift * calculateScaleRatio(area))

        if (statsEnabled && borderArea != null) {
            labelPaint.textSize = calculatedFontSize * 0.42f
            histogramPaint.textSize = calculatedFontSize * 0.4f

            val verticalGap = calculatedFontSize * 0.2f
            val valueLineH = max(textRect.height(), MIN_TEXT_VALUE_HEIGHT) + settings.getGaugeRendererSetting().topOffset

            val labelRect = Rect()
            labelPaint.getTextBounds("Ty", 0, 2, labelRect)
            val labelLineH = labelRect.height()

            val histsRect = Rect()
            histogramPaint.getTextBounds("0000", 0, 4, histsRect)
            val statsLineH = histsRect.height()

            val unitY = centerY - valueLineH
            val labelY = unitY + labelLineH + verticalGap
            val statsY = labelY + statsLineH + verticalGap

            val predictedBottom = statsY + (statsLineH * 0.5f)
            val bottomLimit = borderArea.bottom - (borderArea.height() * 0.02f)

            if (predictedBottom > bottomLimit) {
                val overflow = predictedBottom - bottomLimit
                centerY -= overflow
            }
        }

        val valueHeight = max(textRect.height(), MIN_TEXT_VALUE_HEIGHT) + settings.getGaugeRendererSetting().topOffset
        val valueY = centerY - valueHeight

        valuePaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)
        valuePaint.color = valueColorScheme(metric)
        canvas.drawText(value, valueX, valueY, valuePaint)

        val unitY = centerY - valueHeight
        if (unitText != null) {
            valuePaint.textSize = calculatedFontSize * 0.32f
            valuePaint.color = color(R.color.gray)
            val unitX = valueX + textRect.width() + unitPadding
            canvas.drawText(unitText, unitX, unitY, valuePaint)
        }

        labelPaint.textSize = calculatedFontSize * 0.42f
        labelPaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)

        val verticalGap = calculatedFontSize * 0.2f
        var labelY = 0f
        val text = pid.description.split("\n")

        if (settings.isBreakLabelTextEnabled() && text.size > 1) {
            labelPaint.textSize *= 0.95f
            text.forEachIndexed { i, it ->
                val labelRect = Rect()
                labelPaint.getTextBounds(it, 0, it.length, labelRect)
                labelY = unitY + (i + 1) * labelPaint.textSize + verticalGap
                canvas.drawText(it, area.centerX() - (labelRect.width() / 2), labelY, labelPaint)
            }
        } else {
            val label = pid.description
            val labelRect = Rect()
            labelPaint.getTextBounds(label, 0, label.length, labelRect)
            labelY = unitY + labelRect.height() + verticalGap
            canvas.drawText(label, area.centerX() - (labelRect.width() / 2), labelY, labelPaint)
        }

        if (statsEnabled) {
            histogramPaint.textSize = calculatedFontSize * 0.4f
            val histsRect = Rect()
            histogramPaint.getTextBounds("0000", 0, "0000".length, histsRect)
            var left = area.centerX() - (histsRect.width() * 1.5f)

            val statsY = labelY + histsRect.height() + verticalGap

            if (pid.historgam.isMinEnabled) {
                histogramPaint.color = minValueColorScheme(metric)
                canvas.drawText(metric.min.format(pid), left, statsY, histogramPaint)
                left += (histsRect.width() * 1.2f)
            }

            if (pid.historgam.isAvgEnabled) {
                histogramPaint.color = settings.getColorTheme().valueColor
                canvas.drawText(metric.mean.format(pid), left, statsY, histogramPaint)
                left += (histsRect.width() * 1.5f)
            }

            if (pid.historgam.isMaxEnabled) {
                histogramPaint.color = maxValueColorScheme(metric)
                canvas.drawText(metric.max.format(pid), left, statsY, histogramPaint)
            }
        }
    }

    private fun drawScale(
        canvas: Canvas,
        rect: RectF,
        arcTopRect: RectF,
        metric: Metric,
        scaleEnabled: Boolean,
        radius: Float,
        bitmapPadding: Float,
    ) {
        val targetWidth = ceil(rect.width()).toInt()
        val targetHeight = ceil(rect.height()).toInt()

        val currentCache = scaleBitmapCache
        val isValid =
            currentCache != null &&
                currentCache.scaleEnabled == scaleEnabled &&
                currentCache.progressColor == settings.getColorTheme().progressColor &&
                currentCache.width == targetWidth &&
                currentCache.height == targetHeight &&
                currentCache.dividerCount == drawerSettings.dividersCount

        if (isValid && currentCache != null) {
            val destRect = RectF(rect)
            destRect.inset(-bitmapPadding, -bitmapPadding)
            canvas.drawBitmap(currentCache.bitmap, null, destRect, bitmapPaint)
        } else {
            if (targetWidth <= 0 || targetHeight <= 0) return

            val paddedWidth = targetWidth + (bitmapPadding * 2).toInt()
            val paddedHeight = targetHeight + (bitmapPadding * 2).toInt()
            val scaledWidth = (paddedWidth * CACHE_SCALE).toInt()
            val scaledHeight = (paddedHeight * CACHE_SCALE).toInt()

            val cachedBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val cacheCanvas = Canvas(cachedBitmap)

            cacheCanvas.scale(CACHE_SCALE, CACHE_SCALE)
            cacheCanvas.translate(-rect.left + bitmapPadding, -rect.top + bitmapPadding)

            if (scaleEnabled && metric.source.isNumber()) {
                drawNumbers(cacheCanvas, arcTopRect, metric, radius)
            }
            drawTicks(cacheCanvas, rect)

            scaleBitmapCache =
                ScaleBitmapCache(
                    cachedBitmap,
                    targetWidth,
                    targetHeight,
                    drawerSettings.dividersCount,
                    settings.getColorTheme().progressColor,
                    scaleEnabled,
                )

            val destRect = RectF(rect)
            destRect.inset(-bitmapPadding, -bitmapPadding)
            canvas.drawBitmap(cachedBitmap, null, destRect, bitmapPaint)
        }
    }

    private fun drawNumbers(
        canvas: Canvas,
        area: RectF,
        metric: Metric,
        radius: Float,
    ) {
        val pid = metric.pid
        val startValue = pid.min.toDouble()
        val endValue = pid.max.toDouble()
        val scaleRatio = calculateScaleRatio(area, targetMin = 0.4f, targetMax = 1.9f)
        val numberOfItems = (drawerSettings.dividersCount / drawerSettings.scaleStep)
        val stepValue = (endValue - startValue) / numberOfItems
        val baseRadius = radius * NUMERALS_RADIUS_SCALE_FACTOR
        val start = 0
        val end = drawerSettings.dividersCount + 1

        for (j in start..end step drawerSettings.scaleStep) {
            val angle = (drawerSettings.startAngle + j * drawerSettings.dividersStepAngle) * (Math.PI / 180)
            val text = valueAsString(metric, value = (startValue + stepValue * j / drawerSettings.scaleStep).round(1))
            val textRect = Rect()
            numbersPaint.getTextBounds(text, 0, text.length, textRect)
            numbersPaint.textSize = drawerSettings.scaleNumbersTextSize * scaleRatio

            val x = area.left + (area.width() / 2.0f + cos(angle) * baseRadius - textRect.width() / 2).toFloat()
            val y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + textRect.height() / 2).toFloat()

            numbersPaint.color =
                if (j == (numberOfItems - 1) * drawerSettings.scaleStep || j == numberOfItems * drawerSettings.scaleStep) {
                    settings.getColorTheme().progressColor
                } else {
                    color(R.color.gray)
                }

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    private fun drawTicks(
        canvas: Canvas,
        rect: RectF,
    ) {
        val scaleRect = RectF()
        scaleRect[
            rect.left + drawerSettings.lineOffset,
            rect.top + drawerSettings.lineOffset,
            rect.right - drawerSettings.lineOffset,
        ] = rect.bottom - drawerSettings.lineOffset

        val start = 0
        val end = drawerSettings.dividersCount + 1

        drawArcTicks(canvas, scaleRect, start, end, paintColor = {
            if (it == 10 || it == 12) {
                settings.getColorTheme().progressColor
            } else {
                color(R.color.gray_light)
            }
        }) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle
        }

        drawArcTicks(canvas, scaleRect, start, drawerSettings.dividersCount + 2) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle * 0.5f
        }

        val alignedOuterRect = RectF(rect)
        alignedOuterRect.inset(2f, 2f)

        val grayEndIndex = drawerSettings.dividerHighlightStart
        drawArcTicks(canvas, alignedOuterRect, start, grayEndIndex, paintColor = { getScaleColor(it) }) {
            drawerSettings.startAngle + it * drawerSettings.dividersStepAngle
        }

        val highlightStartDegrees = (drawerSettings.dividersStepAngle * drawerSettings.dividerHighlightStart + 3).toInt()
        val highlightEndDegrees = (drawerSettings.dividersStepAngle * (drawerSettings.dividersCount - 1)).toInt()

        drawLineTicks(
            canvas,
            alignedOuterRect,
            highlightStartDegrees,
            highlightEndDegrees,
            widthInDegrees = drawerSettings.dividerWidth,
            paintColor = { settings.getColorTheme().progressColor },
        ) {
            drawerSettings.startAngle + it
        }

        val widthArc =
            (drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 1)) -
                (drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 3))

        paint.color = settings.getColorTheme().progressColor
        canvas.drawArc(
            alignedOuterRect,
            drawerSettings.startAngle + drawerSettings.dividersCount * (drawerSettings.dividersStepAngle - 2),
            widthArc,
            false,
            paint,
        )
    }

    private fun drawArcTicks(
        canvas: Canvas,
        rect: RectF,
        start: Int,
        end: Int,
        width: Float = drawerSettings.dividerWidth,
        paintColor: (j: Int) -> Int = { color(R.color.gray_light) },
        angle: (j: Int) -> Float,
    ) {
        for (j in start..end step drawerSettings.scaleStep) {
            paint.color = paintColor(j)
            canvas.drawArc(rect, angle(j), width, false, paint)
        }
    }

    private fun drawLineTicks(
        canvas: Canvas,
        rect: RectF,
        start: Int,
        end: Int,
        widthInDegrees: Float,
        paintColor: (j: Int) -> Int,
        angle: (j: Int) -> Float,
    ) {
        val radius = rect.width() / 2f
        val circumference = 2 * Math.PI * radius
        val dashLength = (circumference * (widthInDegrees / 360f)).toFloat()

        val linePaint = Paint(paint)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.BUTT
        linePaint.strokeWidth = paint.strokeWidth

        val cx = rect.centerX()
        val cy = rect.centerY()

        canvas.save()
        canvas.translate(cx, cy)

        for (j in start..end step drawerSettings.scaleStep) {
            val startAngle = angle(j)
            val color = paintColor(j)

            linePaint.color = color

            canvas.save()
            val centerAngle = startAngle + (widthInDegrees / 2f)
            canvas.rotate(centerAngle)

            if (color != color(R.color.gray_light)) {
                glowPaint.color = color
                glowPaint.strokeWidth = paint.strokeWidth * 2.0f
                canvas.drawLine(radius, -dashLength / 2f, radius, dashLength / 2f, glowPaint)
            }

            canvas.drawLine(radius, -dashLength / 2f, radius, dashLength / 2f, linePaint)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun calculateRect(
        left: Float,
        width: Float,
        top: Float,
        padding: Float,
    ): RectF {
        val height = width - 2 * padding
        val calculatedHeight = if (width > height) width else height
        val calculatedWidth = width - 2 * padding
        val radius = calculateRadius(width, padding)

        val rectLeft = left + (width - 2 * padding) / 2 - radius + padding
        val rectTop = top + (calculatedHeight - 2 * padding) / 2 - radius + padding
        val rectRight = left + (width - 2 * padding) / 2 - radius + padding + calculatedWidth
        val rectBottom = top + (height - 2 * padding) / 2 - radius + padding + height
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

    private fun calculateScaleRatio(
        area: RectF,
        targetMin: Float = 0.7f,
        targetMax: Float = 2.4f,
    ): Float =
        (area.width() * area.height()).mapRange(
            8875f,
            (settings.getHeightPixels() * settings.getWidthPixels()) * 0.9f,
            targetMin,
            targetMax,
        )

    private fun calculateRadius(
        width: Float,
        padding: Float,
    ): Float = (width - 2 * padding) / 2

    private inline fun getScaleColor(j: Int): Int =
        if (j == drawerSettings.dividerHighlightStart || j == drawerSettings.dividersCount) {
            settings.getColorTheme().progressColor
        } else {
            color(R.color.gray_light)
        }

    private inline fun valueAsString(
        metric: Metric,
        value: Double,
    ): String =
        if (metric.source.command.pid.max
                .toInt() > 20
        ) {
            value.toInt().toString()
        } else {
            value.toString()
        }
}
