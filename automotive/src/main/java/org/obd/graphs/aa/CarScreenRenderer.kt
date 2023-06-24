package org.obd.graphs.aa

import android.graphics.*
import androidx.core.content.ContextCompat
import org.obd.graphs.ValueScaler
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import kotlin.math.min

private const val ROW_SPACING = 12
private const val MARGIN_START = 15
private const val MARGIN_END = 30

private const val DEFAULT_ITEMS_IN_COLUMN = 6
private const val DEFAULT_FONT_SIZE= 34

private const val PREF_MAX_PIDS_IN_COLUMN = "pref.aa.max_pids_in_column"
private const val PREF_SCREEN_FONT_SIZE = "pref.aa.screen_font_size"

internal class CarScreenRenderer {

    private val cardinal by lazy { ContextCompat.getColor(getContext()!!, R.color.cardinal) }
    private val philippineGreen by lazy { ContextCompat.getColor(getContext()!!, R.color.philippine_green) }

    private val paint = Paint()
    private val valuePaint = Paint()
    private val valueScaler: ValueScaler = ValueScaler()

    fun configure(){
        metricsCollector.configure()
    }

    fun render(canvas: Canvas, visibleArea: Rect?) {

        val maxItemsInColumn = maxItemsInColumn()
        visibleArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            val baseFontSize = calculateFontSize(metrics)

            val textHeight = min(area.height() / 8, baseFontSize)
            val updatedSize = textHeight - ROW_SPACING
            paint.textSize = updatedSize.toFloat()
            canvas.drawRect(area, paint)
            canvas.drawColor(Color.BLACK)

            var verticalPos = area.top - paint.fontMetrics.ascent + 4
            val verticalPosCpy = verticalPos

            var margin = MARGIN_START
            val infoDiv = 1.3f

            var valueHorizontalPos = initialValueHorizontalPos(area)
            metrics.chunked(maxItemsInColumn).forEach { chunk ->

                chunk.forEach { metric ->
                    val originalSize = updatedSize.toFloat()
                    val valueTextSize = updatedSize.toFloat() / infoDiv
                    val labelTextSize = updatedSize.toFloat() / infoDiv / 1.3f
                    var horizontalPos = margin.toFloat()

                    drawTitle(canvas, metric, horizontalPos, verticalPos)
                    drawValue(metric.valueToString(),canvas, valueHorizontalPos, verticalPos + 6, Color.WHITE,Typeface.NORMAL,updatedSize.toFloat() + 14)
                    verticalPos += textHeight.toFloat() / infoDiv

                    horizontalPos = drawText("min",canvas, margin.toFloat(), verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    horizontalPos = drawText(metric.toNumber(metric.min).toString(),canvas, horizontalPos, verticalPos, Color.LTGRAY,Typeface.NORMAL,valueTextSize)

                    horizontalPos = drawText("max",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    horizontalPos = drawText(metric.toNumber(metric.max).toString(),canvas, horizontalPos, verticalPos, Color.LTGRAY,Typeface.NORMAL,valueTextSize)

                    horizontalPos = drawText("avg",canvas, horizontalPos, verticalPos, Color.DKGRAY,Typeface.NORMAL,labelTextSize)
                    drawText(metric.toNumber(metric.avg).toString(),canvas, horizontalPos, verticalPos, Color.LTGRAY,Typeface.NORMAL,valueTextSize)

                    drawDivider(canvas, margin.toFloat(),verticalPos, itemWidth(area).toFloat())
                    verticalPos += 1
                    drawProgressBar(canvas, margin.toFloat(),
                        itemWidth(area).toFloat(), verticalPos,metric)

                    verticalPos += textHeight.toFloat() + 10
                    paint.textSize =  originalSize
                    paint.color = Color.BLACK

                }

                if (maxItemsInColumn() > 1) {
                    valueHorizontalPos += area.width() / 2
                }

                margin += calculateMargin(canvas)
                verticalPos = calculateVerticalPos(textHeight, verticalPos, verticalPosCpy)
            }
        }
    }
    private fun initialValueHorizontalPos (area: Rect) : Float = when (maxItemsInColumn()){
            1 -> ((area.width()) - MARGIN_END).toFloat()
            else -> ((area.width()/2) - MARGIN_END).toFloat()
    }

    private fun calculateVerticalPos(textHeight: Int,verticalPos: Float, verticalPosCpy: Float) : Float  =  when (maxItemsInColumn()) {
            1 ->   verticalPos + (textHeight / 3)
            else ->  verticalPosCpy
        }

    private fun calculateMargin(canvas: Canvas) : Int  =
        when (maxItemsInColumn()) {
            1 ->   0
            else ->  canvas.width / 2
        }

    private fun itemWidth(area: Rect) : Int  =
        when (maxItemsInColumn()) {
            1 ->  area.width()
            else -> area.width() / 2
        }

    private fun maxItemsInColumn() =
        Integer.valueOf(Prefs.getString(PREF_MAX_PIDS_IN_COLUMN, "$DEFAULT_ITEMS_IN_COLUMN"))

    private fun drawTitle(
        canvas: Canvas,
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float
    ) : Float{

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.LTGRAY
        val text = metric.pid.description.replace("\n", " ")
        canvas.drawText(
           text,
            horizontalPos,
            verticalPos,
            paint
        )

       return (horizontalPos + getTextWidth(text,paint) * 1.05f)
    }


    private fun calculateFontSize(data: MutableCollection<CarMetric>): Int {
        val maxFontSize =
            Integer.valueOf(Prefs.getString(PREF_SCREEN_FONT_SIZE, "$DEFAULT_FONT_SIZE"))

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

    private fun drawValue(
        text: String,
        canvas: Canvas,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        font: Int,
        textSize: Float

    ): Float {
        valuePaint.typeface = Typeface.create(Typeface.DEFAULT, font)
        valuePaint.color = color
        valuePaint.textSize =  textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, horizontalPos, verticalPos, valuePaint)
        return (horizontalPos + getTextWidth(text,valuePaint) * 1.25f)
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
        return (horizontalPos + getTextWidth(text,paint) * 1.25f)
    }

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }

    private fun drawDivider(
        canvas: Canvas,
        start: Float,
        verticalPos: Float,
        width: Float
    ) {
        paint.color = philippineGreen
        paint.strokeWidth = 2f
        canvas.drawLine(start - 6, verticalPos + 4, start + width - MARGIN_END, verticalPos  + calculateDividerHeight() , paint)
    }

    private fun calculateDividerHeight() = when (maxItemsInColumn()){
        1 -> 8
        else -> 4
    }

    private fun calculateProgressBarHeight() = when (maxItemsInColumn()){
            1 -> 24
            else -> 11
    }

    private fun drawProgressBar(
        canvas: Canvas,
        start: Float,
        width: Float,
        verticalPos: Float,
        it: CarMetric
    ) {
        paint.color = cardinal
        val progress = valueScaler.scaleToNewRange((it.value?: it.pid.min).toFloat(),
            it.pid.min.toFloat(),it.pid.max.toFloat(),start,start + width - MARGIN_END)

        canvas.drawRect(
            start - 6,
            verticalPos + 4,
            progress,
            verticalPos + calculateProgressBarHeight(),
            paint
        )
    }

    init {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }
}
