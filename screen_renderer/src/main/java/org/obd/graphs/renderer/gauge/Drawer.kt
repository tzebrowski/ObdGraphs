package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.*
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.commons.R
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.round
import org.obd.graphs.ui.common.*
import kotlin.math.*


private const val DEFAULT_LONG_POINTER_SIZE = 1f
private const val SCALE_STEP = 2

private const val START_ANGLE = 200f
private const val SWEEP_ANGLE = 180f
private const val PADDING = 10f
private const val DIVIDERS_COUNT = 12
private const val DIVIDER_STEP_ANGLE = SWEEP_ANGLE / DIVIDERS_COUNT

private const val VALUE_TEXT_SIZE_BASE = 46f
private const val LABEL_TEXT_SIZE_BASE = 16f
private const val SCALE_NUMBERS_TEXT_SIZE_BASE = 12f

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

private const val DIVIDER_WIDTH = 1f

private const val DIVIDER_HIGHLIGHT_START = 9

private const val MIN_TEXT_VALUE_HEIGHT = 30

private const val LINE_OFFSET = 8

@Suppress("NOTHING_TO_INLINE")
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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.BUTT
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.BUTT
        style = Paint.Style.STROKE
        color = COLOR_DYNAMIC_SELECTOR_ECO
    }

    private val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

        val rect = calculateRect(left, width, top)

        val rescaleValue = scaleRationBasedOnScreenSize(rect)
        val arcTopOffset = 8 * rescaleValue
        val strokeWidth = 8f * rescaleValue

        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        paint.color = strokeColor

        canvas.drawArc(rect, START_ANGLE, SWEEP_ANGLE, false, paint)

        paint.color = color(R.color.gray_dark)
        paint.strokeWidth = 2f

        val arcTopRect = RectF()
        arcTopRect[rect.left - arcTopOffset,
                rect.top - arcTopOffset,
                rect.right + arcTopOffset] =
            rect.bottom + arcTopOffset

        canvas.drawArc(arcTopRect, START_ANGLE, SWEEP_ANGLE, false, paint)

        val r2 = RectF()
        val r2Offset = arcTopOffset * 3
        r2[rect.left + r2Offset,
                rect.top + r2Offset,
                rect.right - r2Offset] =
            rect.bottom - r2Offset

        pp.color = color(R.color.black)
        canvas.drawArc(r2, START_ANGLE, SWEEP_ANGLE, false, pp)

        val arcBottomRect = RectF()
        val r3Offset = arcTopOffset + 4
        arcBottomRect[rect.left + r3Offset,
                rect.top + r3Offset,
                rect.right - r3Offset] =
            rect.bottom - r3Offset

        canvas.drawArc(arcBottomRect, START_ANGLE, SWEEP_ANGLE, false, paint)

        drawProgressBar(metric, canvas, rect, (arcBottomRect.top - arcTopRect.top - 2f))

        paint.strokeWidth = strokeWidth

        if (settings.isScaleEnabled()) {
            drawScale(
                canvas,
                rect
            )

            drawScaleNumbers(
                metric,
                canvas,
                calculateRadius(width),
                arcTopRect,
            )
        }

        drawMetric(canvas, area = rect, metric = metric, radius = calculateRadius(width))
    }

    private fun drawProgressBar(
        metric: CarMetric,
        canvas: Canvas,
        rect: RectF,
        strokeWidth: Float
        ) {

        progressPaint.strokeWidth = strokeWidth

        if (settings.isProgressGradientEnabled()) {
            setProgressGradient(rect)
        }

        val value = metric.source.value?.toFloat() ?: metric.source.command.pid.min.toFloat()
        val startValue = metric.source.command.pid.min.toFloat()
        val endValue = metric.source.command.pid.max.toFloat()

        if (value == startValue) {

            canvas.drawArc(
                rect, START_ANGLE, DEFAULT_LONG_POINTER_SIZE, false,
                progressPaint
            )
        } else {

            val pointAngle = abs(SWEEP_ANGLE).toDouble() / (endValue - startValue)
            val point = (START_ANGLE + (value - startValue) * pointAngle).toInt()
            val width = 3f
            canvas.drawArc(
                rect, START_ANGLE + (point - START_ANGLE), width, false,
                progressPaint
            )
        }

        paint.shader = null
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

        valuePaint.textSize = (VALUE_TEXT_SIZE_BASE / 4) * scaleRationBasedOnScreenSize(area) * userScaleRatio
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

    private inline fun scaleColor(j: Int): Int = if (j == DIVIDER_HIGHLIGHT_START || j == DIVIDERS_COUNT) {
        settings.colorTheme().progressColor
    } else {
        color(R.color.gray_light)
    }

    private fun drawScale(
        canvas: Canvas, rect: RectF
    ) {
        val scaleRect = RectF()

        scaleRect[rect.left + LINE_OFFSET,
                rect.top + LINE_OFFSET,
                rect.right - LINE_OFFSET] =
            rect.bottom - LINE_OFFSET

        val start = 0
        val end = DIVIDERS_COUNT + 1

        drawScale(canvas, scaleRect, start, end, paintColor = {
            if (it == 10 || it == 12) {
              settings.colorTheme().progressColor
            } else {
                color(R.color.gray_light)
            }
        }) {
           START_ANGLE + it * DIVIDER_STEP_ANGLE
        }

        drawScale(canvas, scaleRect, start,  DIVIDERS_COUNT + 2) {
            START_ANGLE + it * DIVIDER_STEP_ANGLE * 0.5f
        }

        drawScale(canvas, rect, start, end, paintColor = { scaleColor(it) }) {
           START_ANGLE + it * DIVIDER_STEP_ANGLE
        }

        drawScale(canvas, rect, (DIVIDER_STEP_ANGLE * DIVIDER_HIGHLIGHT_START + 3).toInt(),
            (DIVIDER_STEP_ANGLE * (DIVIDERS_COUNT - 1)).toInt(),
            paintColor = {settings.colorTheme().progressColor}) {
           START_ANGLE + it
        }

        val width =  (START_ANGLE + DIVIDERS_COUNT * (DIVIDER_STEP_ANGLE - 1)) -
                (START_ANGLE + DIVIDERS_COUNT * (DIVIDER_STEP_ANGLE - 3))

        canvas.drawArc(
            rect,
            START_ANGLE + DIVIDERS_COUNT * (DIVIDER_STEP_ANGLE - 2),
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
        width: Float = DIVIDER_WIDTH,
        paintColor: (j: Int) -> Int = { color(R.color.gray_light) },
        angle: (j: Int) -> Float
    ) {
        for (j in start..end step SCALE_STEP) {

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

    private fun drawScaleNumbers(
        metric: CarMetric,
        canvas: Canvas,
        radius: Float,
        area: RectF,
    ) {

        val startValue = metric.source.command.pid.min.toDouble()
        val endValue = metric.source.command.pid.max.toDouble()

        val numberOfItems = (DIVIDERS_COUNT / SCALE_STEP)
        val radiusFactor = 0.75f

        val scaleRation = scaleRationBasedOnScreenSize(area)
        val stepValue = (endValue - startValue) / numberOfItems
        val baseRadius = radius * radiusFactor

        val start = 0
        val end = DIVIDERS_COUNT + 1

        for (j in start..end step SCALE_STEP) {
            val angle  =  (START_ANGLE + j * DIVIDER_STEP_ANGLE) * (Math.PI / 180)
            val text = valueAsString(metric, value =   (startValue + stepValue * j/SCALE_STEP).round(1))
            val rect = Rect()
            numbersPaint.getTextBounds(text, 0, text.length, rect)
            numbersPaint.textSize = SCALE_NUMBERS_TEXT_SIZE_BASE * scaleRation

            val x = area.left + (area.width() / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
            val y = area.top + (area.height() / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

            numbersPaint.color = if (j == (numberOfItems -  1) * SCALE_STEP   || j == numberOfItems  * SCALE_STEP) {
                COLOR_CARDINAL
            } else {
                color(R.color.gray)
            }

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    private inline fun valueAsString(metric: CarMetric, value: Double): String = if (metric.source.command.pid.max.toInt() > 20) {
            value.toInt().toString()
        } else {
            value.toString()
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