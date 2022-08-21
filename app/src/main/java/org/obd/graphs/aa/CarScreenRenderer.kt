package org.obd.graphs.aa

import android.graphics.*
import androidx.core.content.ContextCompat
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.getContext
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.common.toNumber
import kotlin.math.min


private const val ROW_SPACING = 12
private const val LEFT_MARGIN = 15
private const val MAX_FONT_SIZE = 30
private const val MAX_ITEMS_IN_ROW = 5

class CarScreenRenderer {
    private val paint = Paint()

    fun render(
        canvas: Canvas,
        stableArea: Rect?
    ) {

        stableArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }
            val height = min(area.height() / 8, MAX_FONT_SIZE)
            val updatedSize = height - ROW_SPACING
            paint.textSize = updatedSize.toFloat()
            canvas.drawRect(area, paint)
            canvas.drawColor(ContextCompat.getColor(getContext()!!, R.color.white));

            val fm = paint.fontMetrics
            var verticalPos = area.top - fm.ascent
            var verticalPosCpy = verticalPos

            val data = MetricsProvider().findMetrics(aaPIDs())
            val histogram = DataLogger.instance.diagnostics().histogram()
            var margin = LEFT_MARGIN
            val infoDiv = 1.3f

            data.chunked(MAX_ITEMS_IN_ROW).forEach { chunk ->
                chunk.forEach {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

                    canvas.drawText(it.command.pid.description.replace("\n", " "), margin.toFloat(), verticalPos, paint)
                    verticalPos += height.toFloat() / infoDiv

                    val originalSize = updatedSize.toFloat()

                    val hist = histogram.findBy(it.command.pid)

                    var horizontalPos = margin.toFloat()
                    val valueTextSize = updatedSize.toFloat() / infoDiv
                    val labelTextSize = updatedSize.toFloat() / infoDiv / 1.3f

                    horizontalPos = drawText("current",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,40,labelTextSize)
                    horizontalPos = drawText(it.valueToString(),canvas, horizontalPos, verticalPos, Color.RED,Typeface.BOLD,50,valueTextSize)

                    horizontalPos = drawText("min",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,30,labelTextSize)
                    horizontalPos = drawText(it.toNumber(hist.min).toString(),canvas, horizontalPos, verticalPos, Color.RED,Typeface.BOLD,35,valueTextSize)

                    horizontalPos = drawText("max",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,30,labelTextSize)
                    horizontalPos = drawText(it.toNumber(hist.max).toString(),canvas, horizontalPos, verticalPos, Color.RED,Typeface.BOLD,35,valueTextSize)

                    horizontalPos = drawText("avg",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,30,labelTextSize)
                    drawText(it.toNumber(hist.mean).toString(),canvas, horizontalPos, verticalPos, Color.RED,Typeface.BOLD,35,valueTextSize)

                    verticalPos += height.toFloat()
                    paint.textSize =  originalSize
                    paint.color = Color.BLACK
                }
                margin += canvas.width / 2
                verticalPos = verticalPosCpy
            }
        }
    }

    private fun drawText(
        txt: String,
        canvas: Canvas,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        font: Int,
        inc: Int,
        textSize: Float
    ): Float {

        paint.typeface = Typeface.create(Typeface.DEFAULT, font);
        paint.color = color
        paint.textSize =  textSize

        canvas.drawText(txt, horizontalPos, verticalPos, paint)
        return (horizontalPos + inc)
    }

    init {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }
}
