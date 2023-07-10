package org.obd.graphs.aa.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.aa.Fps
import org.obd.graphs.bl.collector.CarMetricsCollector

interface ScreenRenderer {
    fun onDraw(canvas: Canvas, visibleArea: Rect?)

    companion object {
        fun of(context: Context, settings: ScreenSettings, metricsCollector: CarMetricsCollector, fps: Fps): ScreenRenderer {
            return SimpleScreenRenderer(context, settings, metricsCollector, fps)
        }
    }
}