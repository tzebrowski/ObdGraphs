package org.openobd2.core.logger

import android.content.Intent

fun sendBroadcastEvent(actionName: String) {
    ApplicationContext.get()?.run {
        sendBroadcast(Intent().apply {
            action = actionName
        })
    }
}