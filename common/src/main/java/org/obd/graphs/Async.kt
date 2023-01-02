@file:Suppress("DEPRECATION")

package org.obd.graphs

import android.os.AsyncTask

fun runAsync(handler: () -> Unit) {
    val asyncJob: AsyncTask<Void, Void, Void> = object : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }
    asyncJob.execute()
}