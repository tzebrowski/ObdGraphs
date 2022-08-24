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
import org.obd.metrics.api.model.ObdMetric


class SurfaceController(private val carContext: CarContext, lifecycle: Lifecycle) :
    DefaultLifecycleObserver {

    private val renderer: CarScreenRenderer = CarScreenRenderer(carContext)
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null

    private val surfaceCallback: SurfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            synchronized(this@SurfaceController) {
                surface = surfaceContainer.surface
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
                this@SurfaceController.stableArea = stableArea
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

    fun render(metric: ObdMetric? = null) {
        surface?.let {
            if (it.isValid) {
                val canvas = it.lockCanvas(null)
                renderer.render(
                    canvas = canvas,
                    visibleArea = visibleArea,
                    obdMetric = metric)

                it.unlockCanvasAndPost(canvas)
            }
        }
    }

    init {
        lifecycle.addObserver(this)
    }
}
