package org.obd.graphs.bl.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT


private const val LOGGER_KEY = "TripRecorderBroadcastReceiver"

class TripManagerBroadcastReceiver : BroadcastReceiver() {
    private fun isDataCollectingProcessWorking() =
        (Cache[DATA_LOGGER_PROCESS_IS_RUNNING] as Boolean?) ?: false

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {

            DATA_LOGGER_CONNECTING_EVENT -> {
                Cache[DATA_LOGGER_PROCESS_IS_RUNNING] = true
            }

            DATA_LOGGER_CONNECTED_EVENT -> {
                Log.i(LOGGER_KEY, "Received event: DATA_LOGGER_CONNECTED_EVENT")
                TripManager.INSTANCE.startNewTrip(System.currentTimeMillis())
            }

            DATA_LOGGER_STOPPED_EVENT -> {
                Cache[DATA_LOGGER_PROCESS_IS_RUNNING] = false
                Log.i(LOGGER_KEY, "Received event: DATA_LOGGER_STOPPED_EVENT")
                TripManager.INSTANCE.saveCurrentTrip()
            }
            TRIP_LOAD_EVENT -> {
                val trip = intent.extras?.get("extra") as String
                if (!isDataCollectingProcessWorking()) {
                    context?.run {
                        DoAsync {
                            try {
                                Log.i(LOGGER_KEY, "Loading trip: '$trip' ...................")
                                sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
                                TripManager.INSTANCE.loadTrip(trip)
                                Log.i(LOGGER_KEY, "Trip: '$trip' is loaded")
                            } finally {
                                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                            }
                        }.execute()
                    }
                }
            }
        }
    }
}