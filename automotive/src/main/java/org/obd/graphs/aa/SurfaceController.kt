package org.obd.graphs.aa

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.MainThread
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenRenderer
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.sendBroadcastEvent

private const val LOG_KEY = "SurfaceController"

internal class SurfaceController(private val carContext: CarContext,
                                 private val settings: ScreenSettings,
                                 private val metricsCollector: CarMetricsCollector,
                                 fps: Fps
) :
    DefaultLifecycleObserver {


    private val renderer: ScreenRenderer = ScreenRenderer.of(carContext, settings, metricsCollector, fps)
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {

        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface is now available")
                surface?.release()
                surface = surfaceContainer.surface
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val frameRate = settings.getSurfaceFrameRate() + 5f
                    Log.i(LOG_KEY, "Setting surface Frame Rate to=$frameRate")
                    surface?.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
                metricsCollector.applyFilter(settings.getSelectedPIDs())
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface visible area changed")
                this@SurfaceController.visibleArea = visibleArea
                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface stable area changed")
                sendBroadcastEvent(SURFACE_AREA_CHANGED_EVENT)
                renderFrame()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface destroyed")
                surface?.release()
                surface = null
                sendBroadcastEvent(SURFACE_DESTROYED_EVENT)
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.i(LOG_KEY, "SurfaceRenderer created")
        surface?.release()
        surface = null

        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_KEY, "(onDestroy) SurfaceRenderer destroyed")
        surface?.release()
        surface = null
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_KEY, "(onPause) SurfaceRenderer destroyed")
        surface?.release()
        surface = null
    }

    fun onCarConfigurationChanged() {
        renderFrame()
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
                        visibleArea = visibleArea
                    )

                } catch (e: Throwable) {
                    Log.e(LOG_KEY, "Exception was thrown during surface locking.", e)
                    surface = null
                    sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                } finally {
                    try {
                        canvas?.let { c ->
                            it.unlockCanvasAndPost(c)
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Exception was thrown during surface un-locking.", e)
                        sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                    }

                    surfaceLocked = false
                }
            }
        }
    }
}
