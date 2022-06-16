package org.obd.graphs.bl.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT

private const val LOGGER_KEY = "TripRecorderBroadcastReceiver"

class TripRecorderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            DATA_LOGGER_CONNECTED_EVENT -> {
                Log.i(LOGGER_KEY, "Received event: DATA_LOGGER_CONNECTED_EVENT")
                TripRecorder.instance.startNewTrip(System.currentTimeMillis())
            }
            DATA_LOGGER_STOPPED_EVENT -> {
                Log.i(LOGGER_KEY, "Received event: DATA_LOGGER_STOPPED_EVENT")
                TripRecorder.instance.saveCurrentTrip()
            }
        }
    }
}