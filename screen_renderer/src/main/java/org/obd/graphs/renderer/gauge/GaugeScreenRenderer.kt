package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.renderer.AbstractRenderer
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenSettings
import kotlin.math.min

private const val MAX_ITEMS = 6

@Suppress("NOTHING_TO_INLINE")
internal class GaugeScreenRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractRenderer(settings, context, fps) {

    private val drawer = Drawer(settings = settings, context = context)

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            drawer.drawBackground(canvas, area)

            when (metrics.size) {
                0 -> {}
                1 -> {
                    drawer.drawGauge(
                        canvas,
                        left = area.left + area.width() / 6f,
                        top = area.top.toFloat(),
                        width = area.width() * widthScaleRatio(metrics),
                        metrics[0],
                    )
                }

                2 -> {

                    drawer.drawGauge(
                        canvas,
                        left = area.left.toFloat(),
                        top = area.top.toFloat() + area.height() / 6,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metrics[0],
                    )

                    drawer.drawGauge(
                        canvas, left = (area.left + area.width() / 2f) - 10,
                        top = area.top.toFloat() + area.height() / 6,
                        width = area.width() / 2 * widthScaleRatio(metrics),
                        metrics[1],
                    )
                }
                4 -> {
                    draw(area, canvas, metrics, marginLeft = area.width() / 8f)
                }
                3 -> {
                    draw(area, canvas, metrics, marginLeft = area.width() / 8f)
                }
                else -> {
                    draw(area, canvas, metrics)
                }
            }
        }
    }

    override fun release() {
        drawer.recycle()
    }

    private fun draw(
        area: Rect,
        canvas: Canvas,
        metrics: List<CarMetric>,
        marginLeft: Float = 5f
    ) {

        val maxItems = min(metrics.size, MAX_ITEMS)
        val firstHalf = metrics.subList(0, maxItems / 2)
        val secondHalf = metrics.subList(maxItems / 2, maxItems)
        val height = (area.height() / 2)

        val widthDivider = when (maxItems) {
            2 -> 2
            1 -> 1
            else -> secondHalf.size
        }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(metrics)
        var left = marginLeft
        val padding = 10f
        firstHalf.forEach {
            drawer.drawGauge(
                canvas, left = area.left + left, top = area.top.toFloat(), width = width,
                it
            )
            left += width - padding
        }
        if (maxItems > 1) {
            left = marginLeft
            secondHalf.forEach {
                drawer.drawGauge(
                    canvas, left = area.left + left, top = area.top.toFloat() + height, width = width,
                    it
                )
                left += width - padding
            }
        }
    }

    private inline fun widthScaleRatio(metrics: List<CarMetric>): Float = when (metrics.size) {
        1 -> 0.65f
        2 -> 1f
        3 -> 0.8f
        4 -> 0.8f
        5 -> 1.02f
        6 -> 1.02f
        else -> 1f
    }
}