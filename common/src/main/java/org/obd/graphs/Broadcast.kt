package org.obd.graphs

import android.content.Intent

private const val EXTRA_PARAM_NAME = "extra"

fun Intent.getExtraParam(): String  = extras?.get(EXTRA_PARAM_NAME) as String

fun sendBroadcastEvent(actionName: String, extra: String? = "") {
    getContext()?.run {
        sendBroadcast(Intent().apply {
            action = actionName
            putExtra(EXTRA_PARAM_NAME, extra)
        })
    }
}