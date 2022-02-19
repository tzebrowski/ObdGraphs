package org.openobd2.core.logger.bl.datalogger

import android.app.IntentService
import android.content.Context
import android.content.Intent

private const val ACTION_START = "org.openobd2.core.logger.ui.action.START"
private const val ACTION_STOP = "org.openobd2.core.logger.ui.action.STOP"

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
        fun startAction(context: Context) {
            val intent = Intent(context, DataLoggerService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
        }

        @JvmStatic
        fun stopAction(context: Context) {
            val intent = Intent(context, DataLoggerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}