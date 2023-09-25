package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetricsCollector


internal class GaugeScreenRenderer (
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractRenderer(settings, context, fps) {

    private val gaugeRenderer = GaugeRenderer(settings)

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            gaugeRenderer.reset(canvas, area)

            val i = 2f

            if (metrics.size > i) {
                val margin = 10
                var left = area.left.toFloat() + margin
                val width = (area.width() / i) - margin
                val height = (area.height() / i)

                metrics.subList(0, i.toInt()).forEach { carMetric ->
                    gaugeRenderer.onDraw(canvas, left = left, top = area.top.toFloat(), width, height, carMetric, screenArea = area)
                    left += width
                }
            }
        }
    }

}