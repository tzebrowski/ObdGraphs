package org.obd.graphs.aa

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.setCarContext

class CarSession : Session(), DefaultLifecycleObserver {

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)
        setCarContext(carContext)
        val surfaceController = SurfaceController(carContext, lifecycle)
        return CarScreen(carContext,surfaceController)
    }
}
