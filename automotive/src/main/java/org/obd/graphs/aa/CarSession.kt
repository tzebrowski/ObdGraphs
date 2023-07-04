package org.obd.graphs.aa

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.setCarContext


private const val LOG_TAG = "CarSession"

internal class CarSession : Session(), DefaultLifecycleObserver {
    private lateinit var surfaceController: SurfaceController

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)
        setCarContext(carContext)
        surfaceController = SurfaceController(carContext)
        lifecycle.addObserver(surfaceController)
        return CarScreen(carContext, surfaceController)
    }


    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        surfaceController.onCarConfigurationChanged()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(LOG_TAG, "Received onResume event")
        lifecycle.addObserver(surfaceController)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_TAG, "Received onPause event")
        lifecycle.removeObserver(surfaceController)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_TAG, "Received onDestroy event")
        lifecycle.removeObserver(surfaceController)
    }
}
