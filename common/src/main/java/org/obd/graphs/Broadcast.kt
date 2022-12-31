package org.obd.graphs

import android.content.Intent

const val EXTRA_PARAM_NAME = "extra"
fun sendBroadcastEvent(actionName: String, extra: String = "") {
    getContext()?.run {
        sendBroadcast(Intent().apply {
            action = actionName
            putExtra(EXTRA_PARAM_NAME, extra)
        })
    }
}