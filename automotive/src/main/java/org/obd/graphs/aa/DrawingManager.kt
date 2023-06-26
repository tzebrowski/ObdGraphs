package org.obd.graphs.aa

import android.graphics.*
import org.obd.graphs.bl.datalogger.dataLogger

internal class DrawingManager(private val paint: Paint, private val valuePaint: Paint, private val statusPaint: Paint) {

    fun drawText(
        text: String,
        canvas: Canvas,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float

    ): Float = drawText(text, canvas, horizontalPos, verticalPos, color, textSize, paint)





     fun drawStatusBar(area: Rect, canvas: Canvas, margin: Int) {
        val statusVerticalPos = area.top +  4f
        var text = "connection: "

        drawText(
            text,
            canvas,
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
            canvas,
            margin.toFloat() + verticalAlignment,
            statusVerticalPos,
            color,
            18f,
            statusPaint
        )
    }


    fun drawValue(
        canvas: Canvas,
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ){
        valuePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        valuePaint.color = Color.WHITE
        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.valueToString()
        canvas.drawText(text, horizontalPos, verticalPos, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas.drawText(metric.pid.units, (horizontalPos + 2), verticalPos , valuePaint)
    }


    fun drawTitle(
        canvas: Canvas,
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ){

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.LTGRAY
        paint.textSize = textSize
        val text = metric.pid.description.replace("\n", " ")
        canvas.drawText(
            text,
            horizontalPos,
            verticalPos,
            paint
        )
    }

    private fun drawText(
        text: String,
        canvas: Canvas,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float,
        paint1: Paint
    ): Float {
        paint1.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint1.color = color
        paint1.textSize = textSize
        canvas.drawText(text, horizontalPos, verticalPos, paint1)
        return (horizontalPos + getTextWidth(text, paint1) * 1.25f)
    }

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }
}