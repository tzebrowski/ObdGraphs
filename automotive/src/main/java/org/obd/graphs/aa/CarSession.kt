package org.obd.graphs.aa

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.setCarContext

class CarSession : Session(), DefaultLifecycleObserver {
    private lateinit var surfaceController: SurfaceController

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)
        setCarContext(carContext)
        surfaceController =  SurfaceController(carContext, lifecycle)
        return CarScreen(carContext, surfaceController)
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        surfaceController.onCarConfigurationChanged()
    }
}
