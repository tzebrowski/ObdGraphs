package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import kotlin.math.min

private const val LOG_KEY = "SimpleScreenRenderer"

internal class SimpleScreenRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    private val fps: Fps
) : ScreenRenderer {

    private val drawingManager = DrawingManager(context, settings)
    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            drawingManager.updateCanvas(canvas)

            val metrics = metricsCollector.metrics()

            val baseFontSize = calculateFontSize(metrics)
            val textHeight = min(area.height() / 8, baseFontSize).toFloat()
            val valueTextHeight =   min(itemWidth(area, metrics).toFloat() / 10.0f, 50f)
            val textSize = textHeight - ROW_SPACING

            drawingManager.drawBackground(area)

            var verticalPos = area.top + textHeight / 2
            var leftMargin =  drawingManager.getMarginLeft (area)

            if (settings.isStatusPanelEnabled()) {
                verticalPos = drawingManager.drawStatusBar(area, fps.get()) + 18
                drawingManager.drawDivider(leftMargin, area.width().toFloat(), area.top + 10f, Color.DKGRAY)
            }

            val verticalPosCpy = verticalPos
            var valueHorizontalPos = initialValueHorizontalPos(area, metrics)

            val infoDiv = 1.3f

            val maxItemsInColumn = getMaxItemsInColumn(metrics)


            metrics.chunked(maxItemsInColumn).forEach { chunk ->

                chunk.forEach lit@{ metric ->

                    val footerValueTextSize = textSize / infoDiv
                    val footerTitleTextSize = textSize / infoDiv / 1.3f
                    var horizontalPos = leftMargin

                    drawingManager.drawTitle(
                        metric, horizontalPos, verticalPos,
                        calculateTitleTextSize(textSize, metrics),
                        getMaxItemsInColumn(metrics)
                    )

                    drawingManager.drawValue(
                        metric,
                        valueHorizontalPos,
                        verticalPos + 10,
                        valueTextHeight
                    )

                    if (settings.isHistoryEnabled()) {
                        verticalPos += textHeight/ infoDiv
                        horizontalPos = drawingManager.drawText(
                            "min",
                            leftMargin,
                            verticalPos,
                            Color.DKGRAY,
                            footerTitleTextSize
                        )
                        horizontalPos = drawingManager.drawText(
                            metric.toNumber(metric.min),
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
                            metric.toNumber(metric.max),
                            horizontalPos,
                            verticalPos,
                            Color.LTGRAY,
                            footerValueTextSize
                        )

                        if (metric.source.command.pid.historgam.isAvgEnabled) {
                            horizontalPos = drawingManager.drawText(
                                "avg",
                                horizontalPos,
                                verticalPos,
                                Color.DKGRAY,
                                footerTitleTextSize
                            )

                            horizontalPos = drawingManager.drawText(
                                metric.toNumber(metric.mean),
                                horizontalPos,
                                verticalPos,
                                Color.LTGRAY,
                                footerValueTextSize
                            )
                        }

                        drawingManager.drawAlertingLegend(metric, horizontalPos, verticalPos)

                    } else {
                        verticalPos += 12
                    }

                    verticalPos += 6f

                    drawingManager.drawProgressBar(
                        leftMargin,
                        itemWidth(area, metrics).toFloat(), verticalPos, metric,
                        color = settings.colorTheme().progressColor
                    )

                    verticalPos += calculateDividerSpacing(metrics)

                    drawingManager.drawDivider(
                        leftMargin, itemWidth(area, metrics).toFloat(), verticalPos,
                        color = settings.colorTheme().dividerColor
                    )

                    verticalPos += (textHeight * 0.95).toInt()

                    if (verticalPos > area.height()) {
                        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                            Log.v(LOG_KEY, "Skipping entry to display verticalPos=$verticalPos},area.height=${area.height()}")
                        }
                        return@lit
                    }
                }

                if (getMaxItemsInColumn(metrics) > 1) {
                    valueHorizontalPos += area.width() / 2 - 18
                }

                leftMargin += calculateMargin(area, metrics)
                verticalPos = calculateVerticalPos(textHeight, verticalPos, verticalPosCpy, metrics)
            }
        }
    }



    private fun calculateDividerSpacing(metrics: Collection<CarMetric>) = when (getMaxItemsInColumn(metrics)) {
        1 -> 14
        else -> 8
    }

    private fun calculateTitleTextSize(textSize: Float, metrics: Collection<CarMetric>): Float =
        when (getMaxItemsInColumn(metrics)) {
            1 -> textSize
            else -> textSize / 1.1f
        }

    private fun initialValueHorizontalPos(area: Rect, metrics: Collection<CarMetric>): Float =
        when (getMaxItemsInColumn(metrics)) {
            1 -> area.left + ((area.width()) - 42).toFloat()
            else -> area.left +  ((area.width() / 2) - 32).toFloat()
        }

    private fun calculateVerticalPos(
        textHeight: Float,
        verticalPos: Float,
        verticalPosCpy: Float,
        metrics: Collection<CarMetric>
    ): Float = when (getMaxItemsInColumn(metrics)) {
        1 -> verticalPos + (textHeight / 3) - 10
        else -> verticalPosCpy
    }

    private fun calculateMargin(area: Rect, metrics: Collection<CarMetric>): Int =
        when (getMaxItemsInColumn(metrics)) {
            1 -> 0
            else -> (area.width() / 2)
        }

    private fun itemWidth(area: Rect, metrics: Collection<CarMetric>): Int =
        when (getMaxItemsInColumn(metrics)) {
            1 -> area.width()
            else -> area.width() / 2
        }

    private fun getMaxItemsInColumn(metrics: Collection<CarMetric>): Int =
        if (metrics.size < settings.getMaxAllowedItemsInColumn()) {
            1
        } else {
            settings.getMaxItemsInColumn()
        }

    private fun calculateFontSize(data: List<CarMetric>): Int {
        val maxFontSize = settings.getMaxFontSize()
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