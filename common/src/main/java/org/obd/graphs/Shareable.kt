package org.obd.graphs

import android.os.AsyncTask

const val DATA_LOGGER_PROCESS_IS_RUNNING = "data.logger.collecting_process_is_running"
const val TRIP_LOAD_EVENT = "trip.load.event"
const val SCREEN_LOCK_PROGRESS_EVENT = "screen.block.event"
const val SCREEN_UNLOCK_PROGRESS_EVENT = "screen.unlock.event"

class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}
