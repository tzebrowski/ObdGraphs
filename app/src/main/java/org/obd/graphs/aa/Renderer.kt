package org.obd.graphs.aa

import android.graphics.*
import androidx.car.app.CarContext
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

class Renderer {
    private val paint = Paint()

    fun renderFrame(
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
            val infoDiv = 1.4f

            data.chunked(MAX_ITEMS_IN_ROW).forEach { chunk ->
                chunk.forEach {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

                    canvas.drawText(it.command.pid.description.replace("\n", " "), margin.toFloat(), verticalPos, paint)
                    verticalPos += height.toFloat() / infoDiv

                    val originalSize = updatedSize.toFloat()
                    paint.textSize =  originalSize / infoDiv

                    val info = StringBuffer().apply {
                        histogram.findBy(it.command.pid).let { hist ->
                            append(" value=${it.valueToString()}\n")
                            append("min=${it.toNumber(hist.min)}")
                            append(" max=${it.toNumber(hist.max)}")
                            append(" avg=${it.toNumber(hist.mean)}")
                        }
                    }

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
                    canvas.drawText(info.toString(), margin.toFloat(), verticalPos, paint)
                    verticalPos += height.toFloat()
                    paint.textSize =  originalSize
                }
                margin += canvas.width / 2
                verticalPos = verticalPosCpy
            }

        }
    }

    init {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }
}
