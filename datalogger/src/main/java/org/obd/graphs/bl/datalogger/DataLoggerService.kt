package org.obd.graphs.bl.datalogger

import android.app.IntentService
import android.content.Intent
import android.util.Log
import org.obd.graphs.getContext

private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"

class DataLoggerService : IntentService("DataLoggerService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                dataLogger.start()
            }
            ACTION_STOP -> {
                dataLogger.stop()
            }
        }
    }

    companion object {

        @JvmStatic
        fun start() {
            getContext()?.let {
                try {
                    it.startService(Intent(it, DataLoggerService::class.java).apply {
                        action = ACTION_START
                    })
                }catch (e: IllegalStateException){
                    Log.e("DataLoggerService", "Failed to start DataLoggerService",e)
                }
            }
        }

        @JvmStatic
        fun stop() {
            getContext()?.let {
                try {
                    it.startService(Intent(it, DataLoggerService::class.java).apply {
                        action = ACTION_STOP
                    })
                }catch (e: IllegalStateException){
                    Log.e("DataLoggerService", "Failed to stop DataLoggerService",e)
                }
            }
        }
    }
}