package org.obd.graphs.renderer

import android.content.res.Resources
import android.graphics.*
import android.text.TextUtils
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_LIGHT_SHADE_GRAY
import org.obd.graphs.ui.common.COLOR_WHITE
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


private const val DEFAULT_LONG_POINTER_SIZE = 1f
private const val SCALE_STEP = 2

class GaugeRenderer(private val settings: ScreenSettings) {
    private val valueScaler = ValueScaler()
    private val padding = 10f

    private val isDividerDrawFirst = true
    private val isDividerDrawLast = true

    private val startAngle = 180
    private val sweepAngle = 180

    private val strokeColor = Color.parseColor("#0D000000")

    private val numbersPaint = Paint().apply {
        color = COLOR_WHITE
        isAntiAlias = true
    }

    private val valuePaint = Paint().apply {
        color = COLOR_WHITE
        isAntiAlias = true
    }

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    fun onDraw(
        canvas: Canvas, left: Float, top: Float, width: Float, pHeight: Float,
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
        var dividerSize = 1f
        val dividerStep = 10

        val pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
        val point = (startAngle + (value - startValue) * pointAngle).toInt()

        var dividerStepAngle = 0
        var dividersCount = 0
        if (dividerSize > 0) {
            dividerSize = sweepAngle / (abs(endValue - startValue) / dividerSize)
            dividersCount = 100 / dividerStep
            dividerStepAngle = sweepAngle / dividersCount
        }

        val rescaleValue = calculateRescaleValue(width, pHeight)
        val decorLineOffset = 12 * rescaleValue

        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        if (TextUtils.isEmpty(strokeCap)) {
            paint.strokeCap = Paint.Cap.BUTT
        } else {
            paint.strokeCap = Paint.Cap.valueOf(strokeCap)
        }

        paint.style = Paint.Style.STROKE
        strokeWidth *= rescaleValue

        val calculatedWidth = width - 2 * padding
        val height = width - 2 * padding

        val calculatedHeight =
            if (width > height) width else height

        val radius = if (calculatedWidth < height) calculatedWidth / 2 else height / 2

        val rect = calculateRect(left, width, top)

        val decorRect = RectF()
        decorRect[rect.left - decorLineOffset,
                rect.top - decorLineOffset,
                rect.right + decorLineOffset] =
            rect.bottom + decorLineOffset

        paint.color = strokeColor
        canvas.drawArc(rect, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)

        paint.color = COLOR_LIGHT_SHADE_GRAY
        paint.strokeWidth = 2f
        paint.isAntiAlias = true
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
                canvas, width, height, dividersCount, startValue,
                radius,
                calculatedHeight,
                endValue,
                rect,
                screenArea,

            )
        }

        drawValue(canvas,  area = rect, value = metric.source.value,screenArea, pHeight)
        drawLabel(canvas,  area = rect, label = metric.source.command.pid.description,screenArea,pHeight)
    }

    fun reset(canvas: Canvas, rect: Rect) {
        canvas.drawRect(rect, paint)
        canvas.drawColor(settings.getBackgroundColor())
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
        val rectBottom = (height - 2 * padding) / 2 - radius + padding + height
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
        screenArea: Rect,
        height: Float,
    ) {

       valuePaint.textSize = 16f * scaleRation(area, height, screenArea)
        val textRect = Rect()
        valuePaint.getTextBounds(label, 0, label.length, textRect)
        canvas.drawText(label, area.centerX() - (textRect.width() / 2), area.centerY(), valuePaint)
    }

    private fun drawValue(
        canvas: Canvas,
        area: RectF,
        value: Number?,
        screenArea: Rect,
        height: Float,
    ) {

        valuePaint.textSize = 44f * scaleRation(area, height, screenArea)
        val text = (value ?: "No Data").toString()
        val textRect = Rect()
        valuePaint.getTextBounds(text, 0, text.length, textRect)
        canvas.drawText(text, area.centerX() - (textRect.width()/2),area.centerY() - textRect.height(), valuePaint)
    }

    private fun drawNumbers(
        canvas: Canvas, width: Float, height: Float, dividersCount: Int,
        startValue: Float,
        radius: Float,
        calculatedHeight: Float,
        endValue: Float,
        area: RectF,
        screenArea: Rect,

    ) {
        val numberOfItems = (dividersCount / SCALE_STEP)
        val radiusFactor = 0.80f


        val scaleRation = scaleRation(area, calculatedHeight, screenArea)
        val stepValue = round((endValue - startValue) / numberOfItems)
        val baseRadius = radius * radiusFactor

        for (i in 0..numberOfItems) {
            val text = (round(startValue + stepValue * i)).toInt().toString()
            val rect = Rect()
            numbersPaint.getTextBounds(text, 0, text.length, rect)
            numbersPaint.textSize = 14f *  scaleRation
            val angle = Math.PI / numberOfItems * (i - numberOfItems).toFloat()
            val x = area.left - 10 + (width / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
            var y = area.top + (calculatedHeight / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()

            if (width > height) {
                y *= radiusFactor
            }

            canvas.drawText(text, x, y, numbersPaint)
        }
    }

    private fun scaleRation(area: RectF, calculatedHeight: Float, screenArea: Rect): Float = valueScaler.scaleToNewRange(
            area.width() * calculatedHeight,
            0.0f,
            screenArea.width().toFloat() * screenArea.height(),
            0.5f,
            2f
        )


    private fun calculateRescaleValue(width: Float, height: Float): Float = (valueScaler.scaleToNewRange(
        width * height,
        0.0f,
        Resources.getSystem().displayMetrics.widthPixels * Resources.getSystem().displayMetrics.heightPixels.toFloat(),
        1f,
        3f
    ).apply {
        return this * 0.85f
    })
}