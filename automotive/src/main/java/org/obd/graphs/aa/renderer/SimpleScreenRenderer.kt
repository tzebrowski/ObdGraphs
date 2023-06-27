package org.obd.graphs.aa.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.car.app.CarContext
import org.obd.graphs.aa.CarMetric
import org.obd.graphs.aa.carScreenSettings
import org.obd.graphs.aa.metricsCollector
import kotlin.math.min


internal class SimpleScreenRenderer(carContext: CarContext): ScreenRenderer {

    private val drawingManager = DrawingManager(carContext)

    override fun render(canvas: Canvas, visibleArea: Rect?) {

        val maxItemsInColumn = carScreenSettings.maxItemsInColumn()
        visibleArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }
            drawingManager.updateCanvas(canvas)

            val metrics = metricsCollector.metrics()
            val baseFontSize = calculateFontSize(metrics)
            val textHeight = min(area.height() / 8, baseFontSize)
            val textSize = textHeight - ROW_SPACING

            drawingManager.drawBackground(area)

            var margin = MARGIN_START
            val infoDiv = 1.3f

            drawingManager.drawStatusBar(area, margin)

            var verticalPos = drawingManager.getStatusBarSpacing(area)
            val verticalPosCpy = verticalPos
            var valueHorizontalPos = initialValueHorizontalPos(area)
            metrics.chunked(maxItemsInColumn).forEach { chunk ->

                chunk.forEach { metric ->
                    val footerValueTextSize = textSize.toFloat() / infoDiv
                    val footerTitleTextSize = textSize.toFloat() / infoDiv / 1.3f
                    var horizontalPos = margin.toFloat()

                    drawingManager.drawTitle(
                        metric, horizontalPos, verticalPos,
                        calculateTitleTextSize(textSize)
                    )
                    drawingManager.drawValue(
                        metric,
                        valueHorizontalPos,
                        verticalPos + 6,
                        textSize.toFloat() + 14
                    )

                    verticalPos += textHeight.toFloat() / infoDiv

                    horizontalPos = drawingManager.drawText(
                        "min",
                        margin.toFloat(),
                        verticalPos,
                        Color.DKGRAY,
                        footerTitleTextSize
                    )
                    horizontalPos = drawingManager.drawText(
                        metric.toNumber(metric.min).toString(),
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        footerValueTextSize
                    )

                    horizontalPos = drawingManager.drawText(
                        "max",
                        horizontalPos,
                        verticalPos,
                        Color.DKGRAY,
                        footerTitleTextSize
                    )
                    horizontalPos = drawingManager.drawText(
                        metric.toNumber(metric.max).toString(),
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        footerValueTextSize
                    )

                    horizontalPos = drawingManager.drawText(
                        "avg",
                        horizontalPos,
                        verticalPos,
                        Color.DKGRAY,
                        footerTitleTextSize
                    )
                    drawingManager.drawText(
                        metric.toNumber(metric.avg).toString(),
                        horizontalPos,
                        verticalPos,
                        Color.LTGRAY,
                        footerValueTextSize
                    )

                    drawingManager.drawDivider(margin.toFloat(), verticalPos, itemWidth(area).toFloat())
                    verticalPos += 1
                    drawingManager.drawProgressBar(
                        margin.toFloat(),
                        itemWidth(area).toFloat(), verticalPos, metric
                    )

                    verticalPos += textHeight.toFloat() + 10
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
            1 -> ((area.width()) - 32).toFloat()
            else -> ((area.width() / 2) - 32).toFloat()
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
}