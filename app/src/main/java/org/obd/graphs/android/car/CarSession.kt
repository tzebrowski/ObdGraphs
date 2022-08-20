package org.obd.graphs.android.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver

class CarSession : Session(), DefaultLifecycleObserver {

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)
        return CarScreen(carContext)
    }
}
