package org.obd.graphs.aa

import android.graphics.*
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat
import org.obd.graphs.ValueScaler
import org.obd.graphs.getContext
import kotlin.math.min


private const val ROW_SPACING = 12
private const val MARGIN_START = 15
private const val MARGIN_END = 30

internal class CarScreenRenderer(carContext: CarContext) {


    private val cardinal by lazy { ContextCompat.getColor(getContext()!!, R.color.cardinal) }
    private val philippineGreen by lazy {
        ContextCompat.getColor(
            getContext()!!,
            R.color.philippine_green
        )
    }

    private val paint = Paint()
    private val valuePaint = Paint()
    private val backgroundPaint = Paint()

    private val valueScaler: ValueScaler = ValueScaler()
    private val background: Bitmap =
        BitmapFactory.decodeResource(carContext.resources, R.drawable.background)

    fun configure() {
        metricsCollector.configure()
    }

    fun render(canvas: Canvas, visibleArea: Rect?) {

        val maxItemsInColumn = carScreenSettings.maxItemsInColumn()
        visibleArea?.let { area ->
            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            val baseFontSize = calculateFontSize(metrics)
            val textHeight = min(area.height() / 8, baseFontSize)
            val textSize = textHeight - ROW_SPACING

            canvas.drawRect(area, paint)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(background, 0f, 0f, backgroundPaint)

            var verticalPos = area.top - paint.fontMetrics.ascent + 4
            val verticalPosCpy = verticalPos

            var margin = MARGIN_START
            val infoDiv = 1.3f

            var valueHorizontalPos = initialValueHorizontalPos(area)
            metrics.chunked(maxItemsInColumn).forEach { chunk ->

                chunk.forEach { metric ->
                    val originalSize = textSize.toFloat()
                    val footerValueTextSize = textSize.toFloat() / infoDiv
                    val footerTitleTextSize = textSize.toFloat() / infoDiv / 1.3f
                    var horizontalPos = margin.toFloat()

                    drawTitle(
                        canvas, metric, horizontalPos, verticalPos,
                        calculateTitleTextSize(textSize)
                    )
                    drawValue(
                        metric.valueToString(),
                        canvas,
                        valueHorizontalPos,
                        verticalPos + 6,
                        Color.WHITE,
                        Typeface.NORMAL,
                        textSize.toFloat() + 14
                    )
                    verticalPos += textHeight.toFloat() / infoDiv

                    horizontalPos = drawText(
                        "min",
                        canvas,
                        margin.toFloat(),
                        verticalPos,
                        Color.DKGRAY,
                        Typeface.NORMAL,
                        footerTitleTextSize
                    )
                    horizontalPos = drawText(
                        metric.toNumber(metric.min).toString(),
                        canvas,
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        Typeface.NORMAL,
                        footerValueTextSize
                    )

                    horizontalPos = drawText(
                        "max",
                        canvas,
                        horizontalPos,
                        verticalPos,
                        Color.DKGRAY,
                        Typeface.NORMAL,
                        footerTitleTextSize
                    )
                    horizontalPos = drawText(
                        metric.toNumber(metric.max).toString(),
                        canvas,
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        Typeface.NORMAL,
                        footerValueTextSize
                    )

                    horizontalPos = drawText(
                        "avg",
                        canvas,
                        horizontalPos,
                        verticalPos,
                        Color.DKGRAY,
                        Typeface.NORMAL,
                        footerTitleTextSize
                    )
                    drawText(
                        metric.toNumber(metric.avg).toString(),
                        canvas,
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        Typeface.NORMAL,
                        footerValueTextSize
                    )

                    drawDivider(canvas, margin.toFloat(), verticalPos, itemWidth(area).toFloat())
                    verticalPos += 1
                    drawProgressBar(
                        canvas, margin.toFloat(),
                        itemWidth(area).toFloat(), verticalPos, metric
                    )

                    verticalPos += textHeight.toFloat() + 10
                    paint.textSize = originalSize
                    paint.color = Color.BLACK

                }

                if (carScreenSettings.maxItemsInColumn() > 1) {
                    valueHorizontalPos += area.width() / 2
                }

                margin += calculateMargin(canvas)
                verticalPos = calculateVerticalPos(textHeight, verticalPos, verticalPosCpy)
            }
        }
    }

    private fun calculateTitleTextSize(textSize: Int): Float =
        when (carScreenSettings.maxItemsInColumn()) {
            1 -> textSize.toFloat()
            else -> textSize / 1.1f
        }

    private fun initialValueHorizontalPos(area: Rect): Float =
        when (carScreenSettings.maxItemsInColumn()) {
            1 -> ((area.width()) - 8).toFloat()
            else -> ((area.width() / 2) - 8).toFloat()
        }

    private fun calculateVerticalPos(
        textHeight: Int,
        verticalPos: Float,
        verticalPosCpy: Float
    ): Float = when (carScreenSettings.maxItemsInColumn()) {
        1 -> verticalPos + (textHeight / 3)
        else -> verticalPosCpy
    }

    private fun calculateMargin(canvas: Canvas): Int =
        when (carScreenSettings.maxItemsInColumn()) {
            1 -> 0
            else -> canvas.width / 2
        }

    private fun itemWidth(area: Rect): Int =
        when (carScreenSettings.maxItemsInColumn()) {
            1 -> area.width()
            else -> area.width() / 2
        }


    private fun drawTitle(
        canvas: Canvas,
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ): Float {

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

        return (horizontalPos + getTextWidth(text, paint) * 1.05f)
    }

    private fun calculateFontSize(data: MutableCollection<CarMetric>): Int {
        val maxFontSize = carScreenSettings.maxFontSize()
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
        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, horizontalPos, verticalPos, valuePaint)
        return (horizontalPos + getTextWidth(text, valuePaint) * 1.25f)
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
        paint.textSize = textSize
        canvas.drawText(text, horizontalPos, verticalPos, paint)
        return (horizontalPos + getTextWidth(text, paint) * 1.25f)
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
        canvas.drawLine(
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

    private fun calculateProgressBarHeight() = when (carScreenSettings.maxItemsInColumn()) {
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
        val progress = valueScaler.scaleToNewRange(
            (it.value ?: it.pid.min).toFloat(),
            it.pid.min.toFloat(), it.pid.max.toFloat(), start, start + width - MARGIN_END
        )

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

        valuePaint.color = Color.WHITE
        valuePaint.isAntiAlias = true
        valuePaint.style = Paint.Style.FILL
    }
}