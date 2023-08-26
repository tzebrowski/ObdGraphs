@file:Suppress("DEPRECATION")

package org.obd.graphs

import android.os.AsyncTask


fun <T> runAsync(handler: () -> T) : T {
    val asyncJob: AsyncTask<Void, Void, T> = object : AsyncTask<Void, Void, T>() {
        @Deprecated("Deprecated in Java", ReplaceWith("handler()"))
        override fun doInBackground(vararg params: Void?): T {
            return handler()
        }
    }
    return asyncJob.execute().get()
}