@file:Suppress("DEPRECATION")

package org.obd.graphs

import android.os.AsyncTask


fun <T> runAsync(wait: Boolean = true, handler: () -> T) : T {
    val asyncJob: AsyncTask<Void, Void, T> = object : AsyncTask<Void, Void, T>() {
        @Deprecated("Deprecated in Java", ReplaceWith("handler()"))
        override fun doInBackground(vararg params: Void?): T {
            return handler()
        }
    }
    return if (wait) {
        asyncJob.execute().get()
    } else {
        asyncJob.execute()
        null as T
    }
}