package org.obd.graphs.aa.renderer

import android.graphics.Canvas
import android.graphics.Rect
import androidx.car.app.CarContext

interface ScreenRenderer {
    fun onDraw(canvas: Canvas, visibleArea: Rect?)

    companion object {
        fun of(carContext: CarContext): ScreenRenderer {
            return SimpleScreenRenderer(carContext)
        }
    }
}