package org.obd.graphs

import android.content.Intent

fun sendBroadcastEvent(actionName: String) {
    getContext()?.run {
        sendBroadcast(Intent().apply {
            action = actionName
        })
    }
}