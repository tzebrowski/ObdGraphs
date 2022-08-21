package org.obd.graphs.aa

import android.graphics.*
import androidx.core.content.ContextCompat
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.getContext
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.common.toNumber
import org.obd.graphs.ui.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric
import kotlin.math.min


private const val ROW_SPACING = 12
private const val LEFT_MARGIN = 15
private const val DEFAULT_ITEMS_IN_COLUMN = 6
private const val DEFAULT_FONT_SIZE= 34

class CarScreenRenderer {
    private val paint = Paint()
    private val cardinal by lazy { ContextCompat.getColor(getContext()!!, R.color.cardinal) }
    private val philippineGreen by lazy { ContextCompat.getColor(getContext()!!, R.color.philippine_green) }

    fun render(
        canvas: Canvas,
        stableArea: Rect?,
        visibleArea: Rect?
    ) {

        val maxItemsInColumn = Integer.valueOf(Prefs.getString("pref.aa.max_pids_in_column", "$DEFAULT_ITEMS_IN_COLUMN"))

        visibleArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }
            val data = MetricsProvider().findMetrics(aaPIDs())
            val baseFontSize = calculateFontSize(data)

            val textHeight = min(area.height() / 8, baseFontSize)
            val updatedSize = textHeight - ROW_SPACING
            paint.textSize = updatedSize.toFloat()
            canvas.drawRect(area, paint)
            canvas.drawColor(Color.WHITE)

            var verticalPos = area.top - paint.fontMetrics.ascent - 4
            val verticalPosCpy = verticalPos

            val histogram = DataLogger.instance.diagnostics().histogram()
            var margin = LEFT_MARGIN
            val infoDiv = 1.3f

            data.chunked(maxItemsInColumn).forEach { chunk ->
                chunk.forEach {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                    canvas.drawText(it.command.pid.description.replace("\n", " "), margin.toFloat(), verticalPos, paint)
                    verticalPos += textHeight.toFloat() / infoDiv

                    val originalSize = updatedSize.toFloat()

                    val hist = histogram.findBy(it.command.pid)

                    var horizontalPos = margin.toFloat()
                    val valueTextSize = updatedSize.toFloat() / infoDiv
                    val labelTextSize = updatedSize.toFloat() / infoDiv / 1.3f


                    drawDivider(horizontalPos,verticalPos,canvas,(area.width()/2).toFloat(), paint)

                    horizontalPos = drawText("current",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    horizontalPos = drawText(it.valueToString(),canvas, horizontalPos, verticalPos, philippineGreen,Typeface.BOLD,valueTextSize)

                    horizontalPos = drawText("min",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    horizontalPos = drawText(it.toNumber(hist.min).toString(),canvas, horizontalPos, verticalPos, cardinal,Typeface.BOLD,valueTextSize)

                    horizontalPos = drawText("max",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    horizontalPos = drawText(it.toNumber(hist.max).toString(),canvas, horizontalPos, verticalPos, cardinal,Typeface.BOLD,valueTextSize)

                    horizontalPos = drawText("avg",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    drawText(it.toNumber(hist.mean).toString(),canvas, horizontalPos, verticalPos, cardinal,Typeface.BOLD,valueTextSize)

                    verticalPos += textHeight.toFloat()
                    paint.textSize =  originalSize
                    paint.color = Color.BLACK
                }
                margin += canvas.width / 2
                verticalPos = verticalPosCpy
            }
        }
    }

    private fun calculateFontSize(data: MutableList<ObdMetric>): Int {
        val maxFontSize =
            Integer.valueOf(Prefs.getString("pref.aa.screen_font_size", "$DEFAULT_FONT_SIZE"))

        return when (data.size) {
            1 -> {
                (maxFontSize * 3)
            }
            2 -> {
                (maxFontSize * 1.6).toInt()
            }
            3 -> {
                (maxFontSize * 1.5).toInt()
            }
            4 -> {
                (maxFontSize * 1.1).toInt()
            }
            5 -> {
                maxFontSize
            }

            else -> maxFontSize
        }
    }

    private fun drawText(
        text: String,
        canvas: Canvas,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        font: Int,
        textSize: Float
    ): Float {

        paint.typeface = Typeface.create(Typeface.DEFAULT, font)
        paint.color = color
        paint.textSize =  textSize

        canvas.drawText(text, horizontalPos, verticalPos, paint)
        return (horizontalPos + getTextWidth(text,paint) * 1.30f)
    }

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }


    private fun drawDivider(horizontalPos: Float,
                            verticalPos: Float,
                            canvas: Canvas, w: Float, paint: Paint) {

        paint.color = Color.LTGRAY
        canvas.drawLine(horizontalPos - 6, verticalPos + 6, horizontalPos + w - 12, verticalPos  + 6 , paint)
    }

    init {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }
}
