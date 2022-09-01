package org.obd.graphs.bl.datalogger

import android.app.IntentService
import android.content.Intent
import org.obd.graphs.getContext

private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"

class DataLoggerService : IntentService("DataLoggerService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                DataLogger.instance.start()
            }
            ACTION_STOP -> {
                DataLogger.instance.stop()
            }
        }
    }

    companion object {

        @JvmStatic
        fun start() {
            getContext()?.let {
                it.startService(Intent(it, DataLoggerService::class.java).apply {
                    action = ACTION_START
                })
            }
        }

        @JvmStatic
        fun stop() {
            getContext()?.let {
                it.startService(Intent(it, DataLoggerService::class.java).apply {
                    action = ACTION_STOP
                })
            }
        }
    }
}