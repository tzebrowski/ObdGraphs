package org.obd.graphs

import android.app.Activity
import android.content.ContextWrapper
import android.util.Log
import androidx.car.app.CarContext
import java.lang.ref.WeakReference

private lateinit var ApplicationContext: WeakReference<ContextWrapper>
private lateinit var CarApplicationContext: WeakReference<ContextWrapper>

fun setActivityContext(activity: Activity) {
    ApplicationContext = WeakReference(activity)
}

fun setCarContext(carContext: CarContext) {
    CarApplicationContext = WeakReference(carContext)
}

private const val LOG_KEY = "Context"

fun getContext(): ContextWrapper? =
    when {
        //Application context has priority over Car context
        ::ApplicationContext.isInitialized -> {
            Log.v(LOG_KEY,"Application context is initialized")
            ApplicationContext.get()
        }
        ::CarApplicationContext.isInitialized -> {
            Log.v(LOG_KEY,"Car context is initialized")
            CarApplicationContext.get()
        }
        else -> {
            null
        }
    }