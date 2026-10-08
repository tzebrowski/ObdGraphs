/*
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.commons.R
import org.obd.graphs.format
import org.obd.graphs.isNumber
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.api.GaugeProgressBarType
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.cache.TextCache
import org.obd.graphs.round
import org.obd.graphs.toDouble
import org.obd.graphs.toFloat
import org.obd.graphs.toNumber
import org.obd.graphs.ui.common.COLOR_WHITE
import org.obd.graphs.ui.common.color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MIN_TEXT_VALUE_HEIGHT = 30
private const val CACHE_SCALE = 2f

// Share of the dial width the value (with its unit) may take before it is shrunk.
private const val VALUE_MAX_WIDTH_RATIO = 0.7f

// Share of the card width the min / avg / max row may take before it is shrunk.
private const val STATS_MAX_WIDTH_RATIO = 0.9f
private const val STATS_CAPTION_RATIO = 0.6f
private const val MIN_CAPTION = "\u25BC"
private const val MAX_CAPTION = "\u25B2"

data class DrawerSettings(
    val gaugeProgressWidth: Float = 1.5f,
    val gaugeProgressBarType: GaugeProgressBarType = GaugeProgressBarType.LONG,
    val startAngle: Float = 200f,
    val sweepAngle: Float = 180f,
    val scaleStep: Int = 2,
    val longPointerSize: Float = 1f,
    val padding: Float = 10f,
    val dividerWidth: Float = 1f,
    val lineOffset: Float = 8f,
    val valueTextSize: Float = 46f,
    val labelTextSize: Float = 16f,
    val scaleNumbersTextSize: Float = 12f
)

private data class ScaleBitmapCache(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val scale: GaugeScale,
    val redZones: GaugeRedZones,
    val progressColor: Int,
    val scaleEnabled: Boolean
)

// The scale and red zones of one PID, rebuilt only when its range or thresholds change.
private class PidScale(
    val scale: GaugeScale,
    val redZones: GaugeRedZones
)

private class CachedGradient(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val color: Int,
    val shader: RadialGradient
) {
    fun matches(
        rect: RectF,
        color: Int
    ) = rect.left == left && rect.top == top && rect.right == right && rect.bottom == bottom && color == this.color
}

private class GaugeDrawingCache {
    val workingRect = RectF()
    val arcTopRect = RectF()
    val backgroundArcRect = RectF()
    val arcBottomRect = RectF()
    val progressRect = RectF()
    val destRectF = RectF()
    val borderRect = RectF()
    val scaleRect = RectF()
    val alignedOuterRect = RectF()

    val textRect = Rect()
    val unitRect = Rect()
    val labelRect = Rect()
    val histsRect = Rect()
    val numberTextRect = Rect()

    val shaderMatrix = Matrix()
    val gradientColors2 = IntArray(2)
    val backgroundPositions = floatArrayOf(0.0f, 1.0f)
}

@Suppress("NOTHING_TO_INLINE")
internal class GaugeDrawer(
    settings: ScreenSettings,
    context: Context,
    private val drawerSettings: DrawerSettings = DrawerSettings()
) : AbstractDrawer(context, settings) {
    private val textCache = TextCache()
    private val drawingCache = GaugeDrawingCache()

    private val colorGray = color(R.color.gray)
    private val colorGrayDark = color(R.color.gray_dark)
    private val colorGrayLight = color(R.color.gray_light)
    private val trackShadowColor = "#0D000000".toColorInt()

    private val pidScales = mutableMapOf<Long, PidScale>()
    private val gradientCache = mutableMapOf<Long, CachedGradient>()

    private val numbersPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGray
        }

    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGray
        }

    private val statsCaptionPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGray
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

    private val modulePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGray
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scaleBitmapCache = mutableMapOf<Long, ScaleBitmapCache>()

    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
        }

    override fun invalidate() {
        super.invalidate()
        textCache.clear()
    }

    override fun recycle() {
        super.recycle()
        scaleBitmapCache.values.forEach { it.bitmap.recycle() }
        scaleBitmapCache.clear()
        pidScales.clear()
        gradientCache.clear()
        textCache.clear()
    }

    private fun pidScale(metric: Metric): PidScale {
        val pid = metric.pid
        val min = pid.min.toDouble()
        val max = pid.max.toDouble()
        val lower = pid.alert?.lowerThreshold?.toDouble()
        val upper = pid.alert?.upperThreshold?.toDouble()

        val cached = pidScales[pid.id]
        if (cached != null &&
            cached.scale.sourceMin == min &&
            cached.scale.sourceMax == max &&
            cached.redZones.lower == lower &&
            cached.redZones.upper == upper
        ) {
            return cached
        }
        return PidScale(GaugeScale.of(min, max), GaugeRedZones(lower, upper)).also { pidScales[pid.id] = it }
    }

    fun drawGauge(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        width: Float,
        fontSize: Int = settings.getGaugeScreenSettings().getFontSize(),
        labelCenterYPadding: Float = 0f,
        scaleEnabled: Boolean = settings.isScaleEnabled(),
        statsEnabled: Boolean = settings.isStatisticsEnabled(),
        drawBorder: Boolean = false,
        borderArea: RectF? = null,
        drawModule: Boolean = false,
        drawMetricRate: Boolean = false
    ) {
        paint.shader = null

        val dynamicPadding = max(drawerSettings.padding, width * 0.055f)

        calculateRect(left, width, top, dynamicPadding, drawingCache.workingRect)
        val radius = calculateRadius(width, dynamicPadding)

        val strokeWidth = drawingCache.workingRect.width() * 0.037f

        drawingCache.arcTopRect.set(
            drawingCache.workingRect.left - strokeWidth,
            drawingCache.workingRect.top - strokeWidth,
            drawingCache.workingRect.right + strokeWidth,
            drawingCache.workingRect.bottom + strokeWidth
        )

        // A card taller than the dial keeps its captions in its own top corners.
        val cardTop = borderArea?.top ?: top

        if (drawMetricRate) {
            drawMetricRate(metric, drawingCache.workingRect, fontSize, width, left, cardTop, canvas)
        }

        if (drawModule) {
            drawModuleName(metric, drawingCache.workingRect, fontSize, width, left, cardTop, canvas)
        }

        if (drawBorder) {
            drawBorder(canvas, width, left, top, borderArea)
        }

        drawContainerBackground(canvas, metric.pid.id, width, left, top, borderArea)

        val pidScale = pidScale(metric)

        drawBackground(canvas, drawingCache.workingRect, drawingCache.arcTopRect, strokeWidth, strokeWidth, metric, pidScale.scale)

        drawScale(
            canvas,
            drawingCache.workingRect,
            drawingCache.arcTopRect,
            metric,
            pidScale,
            scaleEnabled,
            radius,
            dynamicPadding
        )

        drawStatistics(
            canvas,
            area = drawingCache.workingRect,
            metric = metric,
            radius = radius,
            labelCenterYPadding = labelCenterYPadding,
            fontSize = fontSize,
            statsEnabled = statsEnabled,
            borderArea = borderArea
        )
    }

    private fun drawMetricRate(
        metric: Metric,
        rect: RectF,
        fontSize: Int,
        width: Float,
        left: Float,
        top: Float,
        canvas: Canvas
    ) {
        val rateValue = metric.rate ?: 0.0
        val txt = textCache.rate.get(metric.pid.id, rateValue) { "rate ${rateValue.round(2)}" }

        val baseFontSize = calculateFontSize(multiplier = rect.width() / 22f, fontSize = fontSize)
        modulePaint.textSize = baseFontSize * 0.75f
        val cornerOffset = width * 0.015f
        val textWidth = modulePaint.measureText(txt)
        val textX = left + width - cornerOffset - textWidth
        val textY = top + cornerOffset + modulePaint.textSize
        canvas.drawText(txt, textX, textY, modulePaint)
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
            modulePaint.textSize = baseFontSize * 0.75f

            val cornerOffset = width * 0.015f

            val textX = left + cornerOffset
            val textY = top + cornerOffset + modulePaint.textSize
            canvas.drawText(metric.moduleName!!, textX, textY, modulePaint)
        }
    }

    private fun drawContainerBackground(
        canvas: Canvas,
        cacheKey: Long,
        width: Float,
        left: Float,
        top: Float,
        area: RectF?,
        gradientColor: Int = settings.getGaugeScreenSettings().getGaugeContainerColor()
    ) {
        val destRect =
            area ?: run {
                val borderPadding = width * 0.01f
                drawingCache.destRectF.set(
                    left + borderPadding,
                    top + borderPadding,
                    left + width - borderPadding,
                    top + width - borderPadding
                )
                drawingCache.destRectF
            }

        // A RadialGradient per gauge per frame was pure allocation churn: the card rarely moves.
        val cached = gradientCache[cacheKey]
        val gradient =
            if (cached != null && cached.matches(destRect, gradientColor)) {
                cached.shader
            } else {
                drawingCache.gradientColors2[0] = gradientColor
                drawingCache.gradientColors2[1] = Color.TRANSPARENT

                RadialGradient(
                    destRect.centerX(),
                    destRect.centerY(),
                    min(destRect.width(), destRect.height()) * 0.45f,
                    drawingCache.gradientColors2,
                    drawingCache.backgroundPositions,
                    Shader.TileMode.CLAMP
                ).also {
                    gradientCache[cacheKey] =
                        CachedGradient(destRect.left, destRect.top, destRect.right, destRect.bottom, gradientColor, it)
                }
            }

        backgroundGradientPaint.shader = gradient
        val cornerRadius = 8f * context.resources.displayMetrics.density
        canvas.drawRoundRect(destRect, cornerRadius, cornerRadius, backgroundGradientPaint)
    }

    private fun drawBorder(
        canvas: Canvas,
        width: Float,
        left: Float,
        top: Float,
        area: RectF?
    ) {
        val rectToDraw =
            area ?: run {
                val borderPadding = width * 0.01f
                drawingCache.borderRect.set(
                    left + borderPadding,
                    top + borderPadding,
                    left + width - borderPadding,
                    top + width - borderPadding
                )
                drawingCache.borderRect
            }
        val cornerRadius = width * 0.04f
        canvas.drawRoundRect(rectToDraw, cornerRadius, cornerRadius, borderPaint)
    }

    private fun drawBackground(
        canvas: Canvas,
        rect: RectF,
        arcTopRect: RectF,
        arcTopOffset: Float,
        strokeWidth: Float,
        metric: Metric,
        scale: GaugeScale
    ) {
        paint.style = Paint.Style.STROKE
        paint.color = trackShadowColor
        paint.strokeWidth = strokeWidth
        canvas.drawArc(rect, drawerSettings.startAngle, drawerSettings.sweepAngle, false, paint)

        paint.color = colorGrayDark
        paint.strokeWidth = 2f
        canvas.drawArc(
            arcTopRect,
            drawerSettings.startAngle,
            drawerSettings.sweepAngle,
            false,
            paint
        )

        val r2Offset = arcTopOffset * 3
        drawingCache.backgroundArcRect.set(
            rect.left + r2Offset,
            rect.top + r2Offset,
            rect.right - r2Offset,
            rect.bottom - r2Offset
        )

        val r3Offset = arcTopOffset + 4
        drawingCache.arcBottomRect.set(
            rect.left + r3Offset,
            rect.top + r3Offset,
            rect.right - r3Offset,
            rect.bottom - r3Offset
        )

        canvas.drawArc(
            drawingCache.arcBottomRect,
            drawerSettings.startAngle,
            drawerSettings.sweepAngle,
            false,
            paint
        )

        val progressBarHeight = (drawingCache.arcBottomRect.top - arcTopRect.top - 2f)
        drawProgressBar(metric, scale, canvas, rect, progressBarHeight)

        paint.strokeWidth = strokeWidth
    }

    private fun drawProgressBar(
        metric: Metric,
        scale: GaugeScale,
        canvas: Canvas,
        rect: RectF,
        strokeWidth: Float
    ) {
        if (metric.source.isNumber()) {
            val progressRectOffset = 2f
            drawingCache.progressRect.set(
                rect.left + progressRectOffset,
                rect.top + progressRectOffset,
                rect.right - progressRectOffset,
                rect.bottom - progressRectOffset
            )

            if (settings.isProgressGradientEnabled()) {
                setProgressGradient(rect)
            }

            // Clamped: a value past the PID's max used to sweep beyond the dial, below min backwards.
            val fraction = scale.fraction(metric.source.toDouble())

            if (fraction == 0f) {
                canvas.drawArc(
                    drawingCache.progressRect,
                    drawerSettings.startAngle,
                    drawerSettings.longPointerSize,
                    false,
                    progressPaint
                )
            } else {
                // Not truncated to whole degrees: on a short range that made the bar step visibly.
                val point = drawerSettings.startAngle + fraction * abs(drawerSettings.sweepAngle)
                val isShort = drawerSettings.gaugeProgressBarType == GaugeProgressBarType.SHORT
                val currentSweep = if (isShort) drawerSettings.gaugeProgressWidth else point - drawerSettings.startAngle
                val startAngle = if (isShort) point else drawerSettings.startAngle
                val progressBarWidth = if (isShort) strokeWidth else strokeWidth / 2f

                glowPaint.color = settings.getColorTheme().progressColor
                glowPaint.strokeWidth = progressBarWidth * 2.5f

                canvas.drawArc(
                    drawingCache.progressRect,
                    startAngle,
                    currentSweep,
                    false,
                    glowPaint
                )

                progressPaint.strokeWidth = progressBarWidth
                canvas.drawArc(
                    drawingCache.progressRect,
                    startAngle,
                    currentSweep,
                    false,
                    progressPaint
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
        borderArea: RectF? = null
    ) {
        val calculatedFontSize = calculateFontSize(multiplier = area.width() / 22f, fontSize = fontSize) * 3.8f

        val value =
            textCache.value.get(metric.pid.id, metric.source.toNumber()) {
                metric.source.format(castToInt = false)
            }

        valuePaint.textSize = calculatedFontSize
        valuePaint.getTextBounds(value, 0, value.length, drawingCache.textRect)

        val pid = metric.pid
        val unitText = pid.units
        var unitWidth = 0f

        if (unitText != null) {
            valuePaint.textSize = calculatedFontSize * 0.32f
            valuePaint.getTextBounds(unitText, 0, unitText.length, drawingCache.unitRect)
            unitWidth = drawingCache.unitRect.width().toFloat()
            valuePaint.textSize = calculatedFontSize
        }

        // Layout height is taken before shrinking, so a long value does not move the label and stats.
        val valueLayoutHeight = drawingCache.textRect.height()

        // A long value (e.g. "100.0" plus its unit) ran past the dial; shrink it to the inner width.
        val fit =
            GaugeGeometry.fitScale(
                drawingCache.textRect.width() + if (unitText != null) calculatedFontSize * 0.3f + unitWidth else 0f,
                area.width() * VALUE_MAX_WIDTH_RATIO
            )
        val valueFontSize = calculatedFontSize * fit
        if (fit < 1f) {
            valuePaint.textSize = valueFontSize
            valuePaint.getTextBounds(value, 0, value.length, drawingCache.textRect)
            unitWidth *= fit
        }

        val unitPadding = valueFontSize * 0.3f
        var valueX = area.centerX() - (drawingCache.textRect.width() / 2f)

        if (value.length >= 4 && unitText != null) {
            val totalWidth = drawingCache.textRect.width() + unitPadding + unitWidth
            valueX = area.centerX() - (totalWidth / 2f)
        }

        val verticalShift = if (statsEnabled) 14 else 1
        val relativeFontSize = calculatedFontSize / area.height()
        val offset = if (settings.isAA()) 0.02f else 0.1f
        val dynamicTopOffset = area.height() * (offset - relativeFontSize * 0.2f)

        var centerY = (area.centerY() + dynamicTopOffset + labelCenterYPadding - verticalShift * calculateScaleRatio(area))

        if (statsEnabled && borderArea != null) {
            labelPaint.textSize = calculatedFontSize * 0.42f
            histogramPaint.textSize = calculatedFontSize * 0.4f

            val verticalGap = calculatedFontSize * 0.2f
            val valueLineH = max(valueLayoutHeight, MIN_TEXT_VALUE_HEIGHT) + settings.getGaugeScreenSettings().topOffset

            labelPaint.getTextBounds("Ty", 0, 2, drawingCache.labelRect)
            val labelLineH = drawingCache.labelRect.height()

            histogramPaint.getTextBounds("0000", 0, 4, drawingCache.histsRect)
            val statsLineH = drawingCache.histsRect.height()

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

        val valueHeight = max(valueLayoutHeight, MIN_TEXT_VALUE_HEIGHT) + settings.getGaugeScreenSettings().topOffset
        val valueY = centerY - valueHeight

        valuePaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)
        valuePaint.color = valueColorScheme(metric)
        canvas.drawText(value, valueX, valueY, valuePaint)

        val unitY = centerY - valueHeight
        if (unitText != null) {
            valuePaint.textSize = valueFontSize * 0.32f
            valuePaint.color = colorGray
            val unitX = valueX + drawingCache.textRect.width() + unitPadding
            canvas.drawText(unitText, unitX, unitY, valuePaint)
        }

        labelPaint.textSize = calculatedFontSize * 0.42f
        labelPaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)

        val verticalGap = calculatedFontSize * 0.2f
        var labelY = 0f

        val text =
            textCache.labelSplit.getOrPut(pid.id) {
                pid.description.split("\n")
            }

        if (settings.isBreakLabelTextEnabled() && text.size > 1) {
            labelPaint.textSize *= 0.95f
            text.forEachIndexed { i, it ->
                labelPaint.getTextBounds(it, 0, it.length, drawingCache.labelRect)
                labelY = unitY + (i + 1) * labelPaint.textSize + verticalGap
                canvas.drawText(it, area.centerX() - (drawingCache.labelRect.width() / 2), labelY, labelPaint)
            }
        } else {
            val label = pid.description
            labelPaint.getTextBounds(label, 0, label.length, drawingCache.labelRect)
            labelY = unitY + drawingCache.labelRect.height() + verticalGap
            canvas.drawText(label, area.centerX() - (drawingCache.labelRect.width() / 2), labelY, labelPaint)
        }

        if (statsEnabled) {
            drawStatsRow(canvas, area, metric, calculatedFontSize, labelY, verticalGap, borderArea)
        }
    }

    // min / avg / max, centred with equal gaps. Fixed offsets crowded long values to the right, and
    // nothing said which number was which: min and max get a small ▼ / ▲ caption.
    private fun drawStatsRow(
        canvas: Canvas,
        area: RectF,
        metric: Metric,
        calculatedFontSize: Float,
        labelY: Float,
        verticalGap: Float,
        borderArea: RectF?
    ) {
        val pid = metric.pid
        val statsTextSize = calculatedFontSize * 0.4f
        histogramPaint.textSize = statsTextSize
        histogramPaint.getTextBounds("0000", 0, "0000".length, drawingCache.histsRect)
        val statsY = labelY + drawingCache.histsRect.height() + verticalGap

        val minStr = if (pid.historgam.isMinEnabled) textCache.min.get(pid.id, metric.min) { metric.min.format(pid) } else null
        val avgStr = if (pid.historgam.isAvgEnabled) textCache.avg.get(pid.id, metric.mean) { metric.mean.format(pid) } else null
        val maxStr = if (pid.historgam.isMaxEnabled) textCache.max.get(pid.id, metric.max) { metric.max.format(pid) } else null

        val captionTextSize = statsTextSize * STATS_CAPTION_RATIO
        statsCaptionPaint.textSize = captionTextSize
        val captionGap = statsTextSize * 0.1f

        fun width(
            caption: String?,
            text: String?
        ): Float =
            if (text == null) {
                0f
            } else {
                histogramPaint.measureText(text) + if (caption != null) statsCaptionPaint.measureText(caption) + captionGap else 0f
            }

        val widths = floatArrayOf(width(MIN_CAPTION, minStr), width(null, avgStr), width(MAX_CAPTION, maxStr))
        val present = widths.indices.filter { widths[it] > 0f }
        if (present.isEmpty()) return

        val maxWidth = (borderArea?.width() ?: area.width()) * STATS_MAX_WIDTH_RATIO
        val row =
            GaugeGeometry.statsRow(
                present.map { widths[it] }.toFloatArray(),
                gap = drawingCache.histsRect.width() * 0.35f,
                centerX = area.centerX(),
                maxWidth = maxWidth
            )

        histogramPaint.textSize = statsTextSize * row.scale
        statsCaptionPaint.textSize = captionTextSize * row.scale

        present.forEachIndexed { slot, item ->
            var x = row.lefts[slot]
            val (caption, text, color) =
                when (item) {
                    0 -> Triple(MIN_CAPTION, minStr!!, minValueColorScheme(metric))
                    1 -> Triple(null, avgStr!!, settings.getColorTheme().valueColor)
                    else -> Triple(MAX_CAPTION, maxStr!!, maxValueColorScheme(metric))
                }
            if (caption != null) {
                canvas.drawText(caption, x, statsY, statsCaptionPaint)
                x += statsCaptionPaint.measureText(caption) + captionGap * row.scale
            }
            histogramPaint.color = color
            canvas.drawText(text, x, statsY, histogramPaint)
        }
    }

    private fun drawScale(
        canvas: Canvas,
        rect: RectF,
        arcTopRect: RectF,
        metric: Metric,
        pidScale: PidScale,
        scaleEnabled: Boolean,
        radius: Float,
        bitmapPadding: Float
    ) {
        val targetWidth = ceil(rect.width()).toInt()
        val targetHeight = ceil(rect.height()).toInt()

        val pidId = metric.pid.id
        val currentCache = scaleBitmapCache[pidId]

        // The scale and red zones are part of the key: editing a PID's range or alerts must redraw it.
        val isValid =
            currentCache != null &&
                currentCache.scaleEnabled == scaleEnabled &&
                currentCache.progressColor == settings.getColorTheme().progressColor &&
                currentCache.width == targetWidth &&
                currentCache.height == targetHeight &&
                currentCache.scale == pidScale.scale &&
                currentCache.redZones == pidScale.redZones

        drawingCache.destRectF.set(rect)
        drawingCache.destRectF.inset(-bitmapPadding, -bitmapPadding)

        if (isValid && currentCache != null) {
            canvas.drawBitmap(currentCache.bitmap, null, drawingCache.destRectF, bitmapPaint)
        } else {
            if (targetWidth <= 0 || targetHeight <= 0) return

            val paddedWidth = targetWidth + (bitmapPadding * 2).toInt()
            val paddedHeight = targetHeight + (bitmapPadding * 2).toInt()
            val scaledWidth = (paddedWidth * CACHE_SCALE).toInt()
            val scaledHeight = (paddedHeight * CACHE_SCALE).toInt()

            val cachedBitmap = createBitmap(scaledWidth, scaledHeight)
            val cacheCanvas = Canvas(cachedBitmap)

            cacheCanvas.scale(CACHE_SCALE, CACHE_SCALE)
            cacheCanvas.translate(-rect.left + bitmapPadding, -rect.top + bitmapPadding)

            if (scaleEnabled && metric.source.isNumber()) {
                drawNumbers(cacheCanvas, arcTopRect, pidScale, radius)
            }
            drawTicks(cacheCanvas, rect, pidScale)

            scaleBitmapCache.put(
                pidId,
                ScaleBitmapCache(
                    cachedBitmap,
                    targetWidth,
                    targetHeight,
                    pidScale.scale,
                    pidScale.redZones,
                    settings.getColorTheme().progressColor,
                    scaleEnabled
                )
            )?.bitmap?.recycle()

            canvas.drawBitmap(cachedBitmap, null, drawingCache.destRectF, bitmapPaint)
        }
    }

    private fun angleOf(fraction: Float): Float = drawerSettings.startAngle + fraction * drawerSettings.sweepAngle

    private fun drawNumbers(
        canvas: Canvas,
        area: RectF,
        pidScale: PidScale,
        radius: Float
    ) {
        val scale = pidScale.scale
        val baseRadius = radius * 0.75f

        numbersPaint.textSize = area.width() * 0.055f

        for (i in 0..scale.intervals) {
            val angle = angleOf(i.toFloat() / scale.intervals) * (Math.PI / 180)
            val text = scale.label(i)

            numbersPaint.getTextBounds(text, 0, text.length, drawingCache.numberTextRect)
            val textWidth = drawingCache.numberTextRect.width().toFloat()
            val textHeight = drawingCache.numberTextRect.height().toFloat()

            val labelRadius = GaugeGeometry.labelCenterRadius(baseRadius, angle, textWidth, textHeight)
            val x = area.left + (area.width() / 2.0f + cos(angle) * labelRadius - textWidth / 2).toFloat()
            val y = area.top + (area.height() / 2.0f + sin(angle) * labelRadius + textHeight / 2).toFloat()

            numbersPaint.color =
                if (pidScale.redZones.contains(scale.value(i))) settings.getColorTheme().progressColor else colorGray

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    // Major ticks sit on the labels, minor ones halfway between. Red marks the PID's alert ranges
    // only; it used to be painted on the last part of every dial, alert or not.
    private fun drawTicks(
        canvas: Canvas,
        rect: RectF,
        pidScale: PidScale
    ) {
        val scale = pidScale.scale
        val zones = pidScale.redZones.ranges(scale)
        val progressColor = settings.getColorTheme().progressColor

        drawingCache.scaleRect.set(
            rect.left + drawerSettings.lineOffset,
            rect.top + drawerSettings.lineOffset,
            rect.right - drawerSettings.lineOffset,
            rect.bottom - drawerSettings.lineOffset
        )

        val ticks = scale.intervals * 2
        for (k in 0..ticks) {
            val fraction = k.toFloat() / ticks
            val major = k % 2 == 0
            paint.color = if (major && zones.any { fraction in it }) progressColor else colorGrayLight
            canvas.drawArc(drawingCache.scaleRect, angleOf(fraction), drawerSettings.dividerWidth, false, paint)
        }

        drawingCache.alignedOuterRect.set(rect)
        drawingCache.alignedOuterRect.inset(2f, 2f)

        for (i in 0..scale.intervals) {
            val fraction = i.toFloat() / scale.intervals
            if (zones.none { fraction in it }) {
                paint.color = colorGrayLight
                canvas.drawArc(drawingCache.alignedOuterRect, angleOf(fraction), drawerSettings.dividerWidth, false, paint)
            }
        }

        paint.color = progressColor
        zones.forEach { zone ->
            val startDegrees = (zone.start * drawerSettings.sweepAngle).toInt()
            val endDegrees = (zone.endInclusive * drawerSettings.sweepAngle).toInt()

            drawLineTicks(
                canvas,
                drawingCache.alignedOuterRect,
                startDegrees,
                endDegrees,
                widthInDegrees = drawerSettings.dividerWidth,
                paintColor = { progressColor }
            ) {
                drawerSettings.startAngle + it
            }

            // A solid band on the outer half of the zone, towards the dial's end.
            val half = (zone.endInclusive - zone.start) / 2f
            val bandStart = if (zone.endInclusive >= 1f) zone.start + half else zone.start
            paint.color = progressColor
            canvas.drawArc(drawingCache.alignedOuterRect, angleOf(bandStart), half * drawerSettings.sweepAngle, false, paint)
        }
    }

    private inline fun drawLineTicks(
        canvas: Canvas,
        rect: RectF,
        start: Int,
        end: Int,
        widthInDegrees: Float,
        paintColor: (j: Int) -> Int,
        angle: (j: Int) -> Float
    ) {
        val radius = rect.width() / 2f
        val circumference = 2 * Math.PI * radius
        val dashLength = (circumference * (widthInDegrees / 360f)).toFloat()

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

            if (color != colorGrayLight) {
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
        outRect: RectF
    ) {
        val height = width - 2 * padding
        val calculatedHeight = if (width > height) width else height
        val calculatedWidth = width - 2 * padding
        val radius = calculateRadius(width, padding)

        val rectLeft = left + (width - 2 * padding) / 2 - radius + padding
        val rectTop = top + (calculatedHeight - 2 * padding) / 2 - radius + padding
        val rectRight = left + (width - 2 * padding) / 2 - radius + padding + calculatedWidth
        val rectBottom = top + (height - 2 * padding) / 2 - radius + padding + height

        outRect.set(rectLeft, rectTop, rectRight, rectBottom)
    }

    private fun setProgressGradient(rect: RectF) {
        drawingCache.gradientColors2[0] = COLOR_WHITE
        drawingCache.gradientColors2[1] = settings.getColorTheme().progressColor

        val gradient = SweepGradient(rect.centerY(), rect.centerX(), drawingCache.gradientColors2, null)

        drawingCache.shaderMatrix.reset()
        drawingCache.shaderMatrix.postRotate(90f, rect.centerY(), rect.centerX())
        gradient.setLocalMatrix(drawingCache.shaderMatrix)

        paint.shader = gradient
    }

    private fun calculateScaleRatio(
        area: RectF,
        targetMin: Float = 0.7f,
        targetMax: Float = 2.4f
    ): Float =
        (area.width() * area.height()).mapRange(
            8875f,
            (getHeightPixels() * getWidthPixels()) * 0.9f,
            targetMin,
            targetMax
        )

    private fun calculateRadius(
        width: Float,
        padding: Float
    ): Float = (width - 2 * padding) / 2

    private fun getHeightPixels(): Int = context.resources.displayMetrics.heightPixels

    private fun getWidthPixels(): Int = context.resources.displayMetrics.widthPixels
}
