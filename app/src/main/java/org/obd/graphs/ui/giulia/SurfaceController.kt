package org.obd.graphs.ui.giulia

import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.MainThread
import org.obd.graphs.renderer.ScreenRenderer

private const val LOG_KEY = "SurfaceController"

class SurfaceController(private val renderer: ScreenRenderer) : SurfaceHolder.Callback {

    private lateinit var surfaceHolder: SurfaceHolder
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        surfaceHolder.addCallback(this)
        visibleArea = Rect()
        visibleArea?.set(
            holder.surfaceFrame.left + 10,
            holder.surfaceFrame.top + 10,
            holder.surfaceFrame.right + 10,
            holder.surfaceFrame.bottom
        )
        surface = surfaceHolder.surface
        renderFrame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surface = surfaceHolder.surface
        visibleArea?.set(holder.surfaceFrame.left + 10, holder.surfaceFrame.top + 10, width, height)
        renderFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface?.release()
    }

    @MainThread
    fun renderFrame() {

        surface?.let {
            var canvas: Canvas? = null
            if (it.isValid && !surfaceLocked) {
                try {
                    canvas = it.lockHardwareCanvas()
                    surfaceLocked = true
                    renderer.onDraw(
                        canvas = canvas,
                        drawArea = visibleArea
                    )

                } catch (e: Throwable) {
                    Log.e(LOG_KEY, "Exception was thrown during surface locking.", e)
                    surface = null
                } finally {
                    try {
                        canvas?.let { c ->
                            it.unlockCanvasAndPost(c)
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Exception was thrown during surface un-locking.", e)
                    }

                    surfaceLocked = false
                }
            }
        }
    }
}