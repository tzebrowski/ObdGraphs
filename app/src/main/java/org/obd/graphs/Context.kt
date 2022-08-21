package org.obd.graphs

import android.app.Activity
import android.content.ContextWrapper
import android.util.Log
import androidx.car.app.CarContext
import java.lang.ref.WeakReference

private lateinit var activityContext: WeakReference<ContextWrapper>
private lateinit var carContext: WeakReference<ContextWrapper>

fun setActivityContext(activity: Activity) {
    activityContext = WeakReference(activity)
}

fun setCarContext(carContext: CarContext) {
    org.obd.graphs.carContext = WeakReference(carContext)
}

private const val LOG_KEY = "Context"

fun getContext(): ContextWrapper? =
    when {
        //Application context has priority over Car context
        ::activityContext.isInitialized -> {
            Log.v(LOG_KEY,"Application context is initialized")
            activityContext.get()
        }
        ::carContext.isInitialized -> {
            Log.v(LOG_KEY,"Car context is initialized")
            carContext.get()
        }
        else -> {
            null
        }
    }