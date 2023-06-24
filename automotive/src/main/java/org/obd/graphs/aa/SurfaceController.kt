package org.obd.graphs.aa

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner


class SurfaceController(private val carContext: CarContext, lifecycle: Lifecycle) :
    DefaultLifecycleObserver {

    private val renderer: CarScreenRenderer = CarScreenRenderer()
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                surface = surfaceContainer.surface
                surface?.setFrameRate(60f,Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                renderer.configure()
                render()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            synchronized(this@SurfaceController) {
                this@SurfaceController.visibleArea = visibleArea
                render()
            }
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            synchronized(this@SurfaceController) {
                render()
            }
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) { surface = null }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
         carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }

    fun onCarConfigurationChanged() {
        render()
    }

    fun configure() {
        renderer.configure()
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
                }catch (e: IllegalArgumentException){
                    Log.e("SurfaceController", "Canvas already locked")
                } finally {
                    surfaceLocked =  false
                }
            }
        }
    }

    init {
        lifecycle.addObserver(this)
    }
}
