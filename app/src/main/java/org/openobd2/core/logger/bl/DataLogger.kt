package org.openobd2.core.logger.bl

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.StatusObserver
import org.openobd2.core.command.group.AlfaMed17CommandGroup
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.workflow.EcuSpecific
import org.openobd2.core.workflow.Workflow

const val NOTIFICATION_CONNECTED = "data.logger.connected"
const val NOTIFICATION_CONNECTING = "data.logger.connecting"
const val NOTIFICATION_STOPPED = "data.logger.stopped"
const val NOTIFICATION_STOPPING = "data.logger.stopping"
const val NOTIFICATION_ERROR = "data.logger.error"
const val LOG_KEY = "DATA_LOGGER_DL"

class DataLogger {

    private var modelUpdate = ModelChangePublisher()
    private lateinit var context: Context
    private var statusObserver = object : StatusObserver {

        override fun onConnecting() {
            Log.i(LOG_KEY, "Start collecting process for the Device: $device")
            modelUpdate.data.clear()
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTING
            })
        }

        override fun onConnected() {
            Log.i(LOG_KEY, "We are connected to the device: $device")
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTED
            })
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.e(LOG_KEY,
                    "An error occurred during interaction with the device. Msg: $msg")

            mode1.stop()
            mode22.stop()

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_ERROR
            })
        }

        override fun onStopped() {
            Log.i(LOG_KEY,
                    "Collecting process completed for the Device: $device"
            )

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPED
            })
        }

        override fun onStopping() {
            Log.i(LOG_KEY, "Stop collecting process for the Device: $device")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPING
            })
        }
    }

    var mode1: Workflow = Workflow.mode1().equationEngine("rhino").subscriber(modelUpdate).statusObserver(statusObserver).build()

    var mode22: Workflow = Workflow
            .generic()
            .ecuSpecific(EcuSpecific
                    .builder()
                    .initSequence(AlfaMed17CommandGroup.CAN_INIT)
                    .pidFile("alfa.json").build())
            .equationEngine("rhino")
            .subscriber(modelUpdate)
            .statusObserver(statusObserver).build();

    private lateinit var device: String

    fun stop() {
        when (Prefs.getMode(context)) {
            "Generic mode" -> {
                mode1.stop()
            }

            else -> {
                mode22.stop()
            }
        }
    }

    fun start(context: Context) {
        this.context = context
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var adapterName = pref.getString("pref.adapter.id", "OBDII")
        this.device = adapterName.toString()

        when (Prefs.getMode(context)) {
            "Generic mode" -> {
                var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())
                Log.i(LOG_KEY, "Generic mode, selected pids: $selectedPids")

                mode1.start(BluetoothConnection(device.toString()), selectedPids,Prefs.isBatchEnabled(context))
            }

            else -> {
                var selectedPids = pref.getStringSet("pref.pids.mode22", emptySet())

                Log.i(LOG_KEY, "Mode 22, selected pids: $selectedPids")
                mode22.start(BluetoothConnection(device.toString()), selectedPids,Prefs.isBatchEnabled(context))
            }
        }

        Log.i(LOG_KEY, "Start collecting process for device $adapterName")
    }
}