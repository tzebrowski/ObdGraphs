package org.obd.graphs

import android.content.Intent

fun sendBroadcastEvent(actionName: String) {
    ApplicationContext.get()?.run {
        sendBroadcast(Intent().apply {
            action = actionName
        })
    }
}