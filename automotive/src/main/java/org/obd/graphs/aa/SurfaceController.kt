package org.obd.graphs.aa

import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.aa.renderer.ScreenRenderer


class SurfaceController(private val carContext: CarContext) :
    DefaultLifecycleObserver {

    private val renderer: ScreenRenderer = ScreenRenderer.of(carContext)
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
                    val frameRate = carSettings.getSurfaceFrameRate() + 5f
                    Log.i(LOG_KEY,"Setting surface Frame Rate to=$frameRate")
                    surface?.setFrameRate(frameRate,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
                metricsCollector.configure()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface visible area changed")
                this@SurfaceController.visibleArea = visibleArea
                renderFrame()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface stable area changed")
                renderFrame()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface destroyed")
                surface = null
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.i(LOG_KEY, "SurfaceRenderer created")
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.i(LOG_KEY, "SurfaceRenderer destroyed")
        surface?.release()
        surface = null
    }

    fun onCarConfigurationChanged() {
        renderFrame()
    }

    fun renderFrame() {
        surface?.let {
            if (it.isValid && !surfaceLocked) {
                try {
                    val canvas = it.lockCanvas(null)
                    canvas.drawColor(if (carContext.isDarkMode) Color.DKGRAY else Color.LTGRAY)

                    surfaceLocked = true
                    renderer.onDraw(
                        canvas = canvas,
                        visibleArea = visibleArea
                    )
                    it.unlockCanvasAndPost(canvas)
                } catch (e: IllegalArgumentException) {
                    try {
                        Log.e(LOG_KEY, "Canvas already locked. Destroying currently allocated surface",e)
                        surface = null
                    } finally {
                        carToast(carContext, R.string.pref_aa_reopen_app)
                        carContext.finishCarApp()
                    }
                } finally {
                    surfaceLocked = false
                }
            }
        }
    }
}
