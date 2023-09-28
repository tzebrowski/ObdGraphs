package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.*
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.commons.R
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.ui.common.COLOR_WHITE
import org.obd.graphs.ui.common.color
import kotlin.math.*

private const val DEFAULT_LONG_POINTER_SIZE = 1f
private const val SCALE_STEP = 2

private const val START_ANGLE = 180
private const val SWEEP_ANGLE = 180
private const val PADDING = 10f
private const val DIVIDERS_COUNT = 10
private const val DIVIDER_STEP_ANGLE = SWEEP_ANGLE / DIVIDERS_COUNT

private const val VALUE_TEXT_SIZE_BASE = 46f
private const val LABEL_TEXT_SIZE_BASE = 16f
private const val SCALE_NUMBERS_TEXT_SIZE_BASE = 12f

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

private const val DIVIDER_WIDTH = 1f

private const val DIVIDER_HIGHLIGHT_START = 8

private const val MIN_TEXT_VALUE_HEIGHT = 30

internal class Drawer(private val settings: ScreenSettings, context: Context) {
    private val valueScaler = ValueScaler()

    private val strokeColor = Color.parseColor("#0D000000")
    private val backgroundPaint = Paint()

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    private val numbersPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.gray)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_WHITE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.gray)
    }

    private val histogramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_WHITE
    }

    private val paint = Paint().apply {
        strokeCap = Paint.Cap.BUTT
    }

    fun recycle() {
        background.recycle()
    }


    fun drawGauge(
        canvas: Canvas, left: Float, top: Float, width: Float,
        metric: CarMetric
    ) {
        paint.shader = null
        val startValue = metric.source.command.pid.min.toFloat()
        val endValue = metric.source.command.pid.max.toFloat()
        val value = metric.source.value?.toFloat() ?: metric.source.command.pid.min.toFloat()

        val pointAngle = abs(SWEEP_ANGLE).toDouble() / (endValue - startValue)
        val point = (START_ANGLE + (value - startValue) * pointAngle).toInt()

        val rect = calculateRect(left, width, top)

        val rescaleValue = scaleRationBasedOnScreenSize(rect)
        val decorLineOffset = 8 * rescaleValue
        val strokeWidth = 8f * rescaleValue

        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        val decorRect = RectF()
        decorRect[rect.left - decorLineOffset,
                rect.top - decorLineOffset,
                rect.right + decorLineOffset] =
            rect.bottom + decorLineOffset

        paint.color = strokeColor

        canvas.drawArc(rect, START_ANGLE.toFloat(), SWEEP_ANGLE.toFloat(), false, paint)

        paint.color = color(R.color.gray_dark)
        paint.strokeWidth = 2f

        canvas.drawArc(decorRect, START_ANGLE.toFloat(), SWEEP_ANGLE.toFloat(), false, paint)

        paint.strokeWidth = strokeWidth
        paint.color = settings.colorTheme().progressColor

        if (settings.isProgressGradientEnabled()) {
            setProgressGradient(rect)
        }

        if (value == startValue) {
            canvas.drawArc(
                rect, START_ANGLE.toFloat(), DEFAULT_LONG_POINTER_SIZE, false,
                paint
            )
        } else {
            canvas.drawArc(
                rect, START_ANGLE.toFloat(), (point - START_ANGLE).toFloat(), false,
                paint
            )
        }

        paint.shader = null

        if (settings.isScaleEnabled()) {
            drawScale(
                canvas,
                rect, false
            )

            drawScaleNumbers(
                canvas, startValue,
                calculateRadius(width),
                endValue,
                decorRect,
            )
        }

        drawMetric(canvas, area = rect, metric = metric, radius = calculateRadius(width))
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
        width: Float,
        top: Float
    ): RectF {

        val height = width - 2 * PADDING
        val calculatedHeight = if (width > height) width else height
        val calculatedWidth = width - 2 * PADDING
        val radius = calculateRadius(width)

        val rectLeft = left + (width - 2 * PADDING) / 2 - radius + PADDING
        val rectTop = top + (calculatedHeight - 2 * PADDING) / 2 - radius + PADDING
        val rectRight = left + (width - 2 * PADDING) / 2 - radius + PADDING + calculatedWidth
        val rectBottom = top + (height - 2 * PADDING) / 2 - radius + PADDING + height
        val rect = RectF()
        rect[rectLeft, rectTop, rectRight] = rectBottom
        return rect
    }

    private fun setProgressGradient(rect: RectF) {
        val colors = intArrayOf(COLOR_WHITE, settings.colorTheme().progressColor)
        val gradient = SweepGradient(rect.centerY(), rect.centerX(), colors, null)
        val matrix = Matrix()
        matrix.postRotate(90f, rect.centerY(), rect.centerX())
        gradient.setLocalMatrix(matrix)
        paint.shader = gradient
    }


    private fun drawMetric(
        canvas: Canvas,
        area: RectF,
        metric: CarMetric,
        radius: Float
    ) {

        val userScaleRatio = userScaleRatio()

        val value = metric.valueToString()
        valuePaint.textSize = VALUE_TEXT_SIZE_BASE * scaleRationBasedOnScreenSize(area) * userScaleRatio
        valuePaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)
        valuePaint.color = COLOR_WHITE

        val textRect = Rect()
        valuePaint.getTextBounds(value, 0, value.length, textRect)

        val centerY = area.centerY() - (if (settings.isHistoryEnabled()) 8 else 1) * scaleRationBasedOnScreenSize(area)
        val valueHeight = max(textRect.height(), MIN_TEXT_VALUE_HEIGHT)

        canvas.drawText(value, area.centerX() - (textRect.width() / 2), centerY - valueHeight, valuePaint)

        valuePaint.textSize = VALUE_TEXT_SIZE_BASE / 3 * scaleRationBasedOnScreenSize(area) * userScaleRatio
        valuePaint.color = color(R.color.gray)
        canvas.drawText(metric.source.command.pid.units, area.centerX() + textRect.width() / 2 + 6, centerY - valueHeight, valuePaint)

        val label = metric.source.command.pid.description
        labelPaint.textSize = LABEL_TEXT_SIZE_BASE * scaleRationBasedOnScreenSize(area) * userScaleRatio
        labelPaint.setShadowLayer(radius / 4, 0f, 0f, Color.WHITE)

        val labelRect = Rect()
        labelPaint.getTextBounds(label, 0, label.length, labelRect)

        val labelY = centerY - valueHeight / 2
        canvas.drawText(label, area.centerX() - (labelRect.width() / 2), labelY, labelPaint)

        if (settings.isHistoryEnabled()) {
            val hists =
                "${metric.toNumber(metric.min)}    ${if (metric.source.command.pid.historgam.isAvgEnabled) metric.toNumber(metric.mean) else ""}     ${
                    metric.toNumber(metric.max)
                }"
            histogramPaint.textSize = 18f * scaleRationBasedOnScreenSize(area) * userScaleRatio
            val histsRect = Rect()
            histogramPaint.getTextBounds(hists, 0, hists.length, histsRect)
            canvas.drawText(hists, area.centerX() - (histsRect.width() / 2), labelY + labelRect.height() + 8, histogramPaint)
        }
    }

    private fun userScaleRatio() =
        valueScaler.scaleToNewRange(settings.getFontSize().toFloat(), CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)


    private fun drawScale(
        canvas: Canvas, rect: RectF, drawDetailedScale: Boolean = false
    ) {
        val decorLineOffset = 8
        val r1 = RectF()


        r1[rect.left + decorLineOffset,
                rect.top + decorLineOffset,
                rect.right - decorLineOffset] =
            rect.bottom - decorLineOffset

        drawScale(canvas, r1)
        drawScale(canvas, rect)

        if (drawDetailedScale) {
            paint.color = color(R.color.gray_light)

            val i = 0
            val max = DIVIDER_STEP_ANGLE * DIVIDER_HIGHLIGHT_START
            for (j in i..max step SCALE_STEP) {
                canvas.drawArc(
                    rect,
                    (START_ANGLE + j).toFloat(),
                    DIVIDER_WIDTH,
                    false,
                    paint
                )
            }
        }
        paint.color = settings.colorTheme().progressColor
        val i = DIVIDER_STEP_ANGLE * DIVIDER_HIGHLIGHT_START
        val max = DIVIDER_STEP_ANGLE * DIVIDERS_COUNT
        for (j in i..max step SCALE_STEP) {
            canvas.drawArc(
                rect,
                (START_ANGLE + j).toFloat(),
                DIVIDER_WIDTH,
                false,
                paint
            )
        }

    }

    private fun drawScale(canvas: Canvas, rect: RectF) {

        val i = 0
        val max = DIVIDERS_COUNT + 1
        for (j in i..max step SCALE_STEP) {

            paint.color = if (j == DIVIDER_HIGHLIGHT_START || j == DIVIDERS_COUNT) {
                settings.colorTheme().progressColor
            } else {
                color(R.color.gray_light)
            }

            canvas.drawArc(
                rect,
                (START_ANGLE + j * DIVIDER_STEP_ANGLE).toFloat(),
                DIVIDER_WIDTH,
                false,
                paint
            )
        }

    }

    private fun drawScaleNumbers(
        canvas: Canvas,
        startValue: Float,
        radius: Float,
        endValue: Float,
        area: RectF,
    ) {
        val numberOfItems = (DIVIDERS_COUNT / SCALE_STEP)
        val radiusFactor = 0.80f

        val scaleRation = scaleRationBasedOnScreenSize(area)
        val stepValue = round((endValue - startValue) / numberOfItems)
        val baseRadius = radius * radiusFactor
        for (i in 0..numberOfItems) {
            val text = (round(startValue + stepValue * i)).toInt().toString()
            val rect = Rect()
            numbersPaint.getTextBounds(text, 0, text.length, rect)
            numbersPaint.textSize = SCALE_NUMBERS_TEXT_SIZE_BASE * scaleRation
            val angle = Math.PI / numberOfItems * (i - numberOfItems).toFloat()
            val x = area.left + (area.width() / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
            val y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

            numbersPaint.color = if (i == numberOfItems - 1 || i == numberOfItems) {
                settings.colorTheme().progressColor
            } else {
                color(R.color.gray)
            }

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    private fun scaleRationBasedOnScreenSize(area: RectF): Float = valueScaler.scaleToNewRange(
        area.width() * area.height(),
        0.0f,
        (settings.getHeightPixels() * settings.getWidthPixels()).toFloat(),
        0.9f,
        2.4f
    )


    private fun calculateRadius(width: Float): Float {
        val calculatedWidth = width - 2 * PADDING
        val height = width - 2 * PADDING
        return if (calculatedWidth < height) calculatedWidth / 2 else height / 2
    }
}