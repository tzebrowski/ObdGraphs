package org.obd.graphs.aa

import android.graphics.Rect
import android.util.Log
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner


class SurfaceController(private val carContext: CarContext, lifecycle: Lifecycle) :
    DefaultLifecycleObserver {

    private val renderer: SimpleScreenRenderer = SimpleScreenRenderer(carContext)
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface is now available")
                surface?.release()
                surface = surfaceContainer.surface
                metricsCollector.configure()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface visible area changed")
                this@SurfaceController.visibleArea = visibleArea
                render()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface stable area changed")
                render()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                Log.i(LOG_KEY, "Surface destroyed")
                surface?.release()
                surface = null
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
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
        render()
    }

    fun render() {
        surface?.let {
            if (it.isValid && !surfaceLocked) {
                try {
                    val canvas = it.lockCanvas(null)
                    surfaceLocked = true
                    renderer.render(
                        canvas = canvas,
                        visibleArea = visibleArea
                    )
                    it.unlockCanvasAndPost(canvas)
                } catch (e: IllegalArgumentException) {
                    try {
                        Log.e(LOG_KEY, "Canvas already locked. Destroying currently allocated surface")
                        it.release()
                        surface = null
                    } finally {
                        carToast(carContext, R.string.pref_aa_reopen_app)
                    }
                } finally {
                    surfaceLocked = false
                }
            }
        }
    }

    init {
        lifecycle.addObserver(this)
    }
}
