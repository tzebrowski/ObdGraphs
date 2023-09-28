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

internal class GaugeScreenRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractRenderer(settings, context, fps) {

    private val drawer = Drawer(settings, context)

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
                        canvas, left = area.left + 80f, top = area.top.toFloat(), width = area.width() * widthScaleRatio(metrics),
                        metrics[0],
                    )
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
        metrics: List<CarMetric>
    ) {

        val size = min(metrics.size, 6)
        val firstHalf = metrics.subList(0, size / 2)
        val secondHalf = metrics.subList(size / 2, size)
        val height = (area.height() / 2)

        val widthDivider = when (size) {
            2 -> 2
            1 -> 1
            else -> secondHalf.size
        }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(metrics)
        val padding = padding(metrics)
        var left = padding
        firstHalf.forEach {
            drawer.drawGauge(
                canvas, left = area.left + left, top = area.top.toFloat(), width = width,
                it
            )
            left += width + padding
        }
        if (size > 1) {
            left = padding

            secondHalf.forEach {
                drawer.drawGauge(
                    canvas, left = area.left + left, top = area.top.toFloat() + height, width = width,
                    it
                )
                left += width + padding
            }
        }
    }

    private fun padding(metrics: List<CarMetric>): Float = when (metrics.size) {
        2 -> 14f
        3 -> 14f
        4 -> 14f
        else -> 0f
    }

    private fun widthScaleRatio(metrics: List<CarMetric>): Float = when (metrics.size) {
        1 -> 0.8f
        2 -> 0.9f
        3 -> 0.9f
        4 -> 0.9f
        else -> 1f
    }
}