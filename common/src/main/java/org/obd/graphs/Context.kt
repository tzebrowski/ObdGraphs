package org.obd.graphs

import android.content.ContextWrapper
import android.util.Log
import java.lang.ref.WeakReference

private lateinit var activityContext: WeakReference<ContextWrapper>
private lateinit var carContext: WeakReference<ContextWrapper>
private const val LOG_KEY = "Context"

fun setActivityContext(activity: ContextWrapper) {
    activityContext = WeakReference(activity)
}

fun setCarContext(carContext: ContextWrapper) {
    org.obd.graphs.carContext = WeakReference(carContext)
}

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