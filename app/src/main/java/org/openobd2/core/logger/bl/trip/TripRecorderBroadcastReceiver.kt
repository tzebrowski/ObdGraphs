package org.openobd2.core.logger.bl.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.openobd2.core.logger.bl.datalogger.*

private const val LOGGER_KEY = "TripRecorderBroadcastReceiver"

class TripRecorderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent? ) {
        when (intent?.action) {
            DATA_LOGGER_CONNECTED_EVENT -> {
                Log.i(LOGGER_KEY,"Received event: DATA_LOGGER_CONNECTED_EVENT")
                TripRecorder.instance.startNewTrip(System.currentTimeMillis())
            }
            DATA_LOGGER_STOPPED_EVENT -> {
                Log.i(LOGGER_KEY,"Received event: DATA_LOGGER_STOPPED_EVENT")
                TripRecorder.instance.saveCurrentTrip()
            }
        }
    }
}