package org.openobd2.core.logger.bl

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log

private const val ACTION_START = "org.openobd2.core.logger.ui.action.START"
private const val ACTION_STOP = "org.openobd2.core.logger.ui.action.STOP"
private const val PARAM_BT_DEVICE_NAME = "org.openobd2.core.logger.ui.extra.BT_DEVICE_NAME"

class DataLoggerService : IntentService("DataLoggerService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                val btDeviceName: String = intent.getStringExtra(PARAM_BT_DEVICE_NAME).toString()
                Log.i("DATA_LOGGER_SVC", "Start collecting process for device $dataLogger")
                dataLogger.start(btDeviceName);
            }
            ACTION_STOP -> {
                Log.i("DATA_LOGGER_SVC", "Stop collecting process")
                dataLogger.stop();
            }
        }
    }

    companion object {
        @JvmStatic
        private var dataLogger: DataLogger =
            DataLogger()

        @JvmStatic
        fun startAction(context: Context, btDeviceName: String) {
            val intent = Intent(context, DataLoggerService::class.java).apply {
                action = ACTION_START
                putExtra(PARAM_BT_DEVICE_NAME, btDeviceName)
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