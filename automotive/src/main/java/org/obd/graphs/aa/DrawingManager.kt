package org.obd.graphs.aa

import android.graphics.*
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.dataLogger

const val MARGIN_END = 30

internal class DrawingManager(carContext: CarContext) {

    private val valueScaler: ValueScaler = ValueScaler()
    private val colorCardinal = ContextCompat.getColor(carContext, R.color.cardinal)
    private val colorPhilippineGreen = ContextCompat.getColor(carContext, R.color.philippine_green)

    private val paint = Paint()
    private val statusPaint = Paint()
    private val valuePaint = Paint()
    private val backgroundPaint = Paint()
    private var canvas: Canvas? = null

    private val background: Bitmap =
        BitmapFactory.decodeResource(carContext.resources, R.drawable.background)


    init {
        valuePaint.color = Color.WHITE
        valuePaint.isAntiAlias = true
        valuePaint.style = Paint.Style.FILL

        statusPaint.color = Color.WHITE
        statusPaint.isAntiAlias = true
        statusPaint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }

    fun updateCanvas(canvas: Canvas) {
        this.canvas = canvas
    }

    fun getStatusBarSpacing(area: Rect): Float = area.top - paint.fontMetrics.ascent + 12

    fun drawBackground(area: Rect) {
        canvas?.drawRect(area, paint)
        canvas?.drawColor(Color.BLACK)
        canvas?.drawBitmap(background, 0f, 0f, backgroundPaint)
    }

    fun drawText(
        text: String,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float

    ): Float = drawText(text, horizontalPos, verticalPos, color, textSize, paint)

    fun drawProgressBar(
        start: Float,
        width: Float,
        verticalPos: Float,
        it: CarMetric
    ) {
        paint.color = colorCardinal
        val progress = valueScaler.scaleToNewRange(
            (it.value ?: it.pid.min).toFloat(),
            it.pid.min.toFloat(), it.pid.max.toFloat(), start, start + width - MARGIN_END
        )

        canvas?.drawRect(
            start - 6,
            verticalPos + 4,
            progress,
            verticalPos + calculateProgressBarHeight(),
            paint
        )
    }

    fun drawStatusBar(area: Rect, margin: Int) {
        val statusVerticalPos = area.top + 4f
        var text = "connection: "

        drawText(
            text,
            margin.toFloat(),
            statusVerticalPos,
            Color.WHITE,
            12f,
            statusPaint
        )
        val verticalAlignment = getTextWidth(text, statusPaint) + 2

        val color: Int
        if (dataLogger.isRunning()) {
            text = "connected"
            color = Color.GREEN
        } else {
            text = "disconnected"
            color = Color.YELLOW
        }

        drawText(
            text,
            margin.toFloat() + verticalAlignment,
            statusVerticalPos,
            color,
            18f,
            statusPaint
        )
    }

    fun drawValue(
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ) {
        valuePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        valuePaint.color = Color.WHITE
        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.valueToString()
        canvas?.drawText(text, horizontalPos, verticalPos, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas?.drawText(metric.pid.units, (horizontalPos + 2), verticalPos, valuePaint)
    }

    fun drawTitle(
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ) {

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.LTGRAY
        paint.textSize = textSize
        val text = metric.pid.description.replace("\n", " ")
        canvas?.drawText(
            text,
            horizontalPos,
            verticalPos,
            paint
        )
    }

    private fun drawText(
        text: String,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float,
        paint1: Paint
    ): Float {
        paint1.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint1.color = color
        paint1.textSize = textSize
        canvas?.drawText(text, horizontalPos, verticalPos, paint1)
        return (horizontalPos + getTextWidth(text, paint1) * 1.25f)
    }

    fun drawDivider(
        start: Float,
        verticalPos: Float,
        width: Float
    ) {
        paint.color = colorPhilippineGreen
        paint.strokeWidth = 2f
        canvas?.drawLine(
            start - 6,
            verticalPos + 4,
            start + width - MARGIN_END,
            verticalPos + calculateDividerHeight(),
            paint
        )
    }

    private fun calculateDividerHeight() = when (carScreenSettings.maxItemsInColumn()) {
        1 -> 8
        else -> 4
    }

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }

    private fun calculateProgressBarHeight() = when (carScreenSettings.maxItemsInColumn()) {
        1 -> 24
        else -> 11
    }
}