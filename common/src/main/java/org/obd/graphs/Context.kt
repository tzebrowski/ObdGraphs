package org.obd.graphs

import android.content.ContextWrapper
import java.lang.ref.WeakReference

private lateinit var activityContext: WeakReference<ContextWrapper>
private lateinit var carContext: WeakReference<ContextWrapper>

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
            activityContext.get()
        }
        ::carContext.isInitialized -> {
            carContext.get()
        }
        else -> {
            null
        }
    }