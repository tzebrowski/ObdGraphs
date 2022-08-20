package org.obd.graphs.aa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.common.toNumber
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet
import kotlin.math.min

private const val ROW_SPACING = 12
private const val LEFT_MARGIN = 15
private const val MAX_FONT_SIZE = 30

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
            val fm = paint.fontMetrics
            var verticalPos = area.top - fm.ascent

            val data = MetricsProvider().findMetrics(aaPIDs())
            val histogram = DataLogger.instance.diagnostics().histogram()

            data.forEach {
                val info = StringBuilder().apply {
                    append(it.command.pid.description.replace("\n", " "))
                    append("  value=${it.valueToString()}\n")
                }

                histogram.findBy(it.command.pid).let { hist ->
                    info.append("min=${it.toNumber(hist.min)}")
                    info.append(" max=${it.toNumber(hist.max)}")
                    info.append(" avg=${it.toNumber(hist.mean)}")
                }

                canvas.drawText(info.toString(), LEFT_MARGIN.toFloat(), verticalPos, paint)
                verticalPos += height.toFloat()
            }
        }
    }


    private fun aaPIDs() =
        Prefs.getStringSet("pref.aa.pids.selected").map { s -> s.toLong() }.toSet()

    init {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
    }
}
