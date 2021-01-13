package org.openobd2.core.logger.bl

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.StatusListener
import org.openobd2.core.workflow.Workflow


val NOTIFICATION_CONNECTING = "data.logger.connecting"
val NOTIFICATION_COMPLETE = "data.logger.complete"
val NOTIFICATION_ERROR = "data.logger.error"
val NOTIFICATION_STOPPING = "data.logger.stopping"

internal class DataLogger {

    private var modelUpdate = ModelChangePublisher()
    private lateinit var context: Context
    private val LOG_KEY = "DATA_LOGGER_DL"

    private var statusListener = object : StatusListener {

        override fun onConnecting() {
            Log.i(LOG_KEY, "Start collecting process for the Device: $device")
            modelUpdate.data.clear()

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTING
            })

        }

        override fun onError(msg: String) {
            Log.e(
                LOG_KEY,
                "An error occurred for the Device: $device. Msg: $msg"
            )

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_ERROR
            })
        }

        override fun onComplete() {
            Log.i(
                LOG_KEY,
                "Collecting process completed for the Device: $device"
            )

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_COMPLETE
            })
        }

        override fun onStopping() {
            Log.i(LOG_KEY, "Stop collecting process for the Device: $device")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPING
            })
        }
    }

    private var mode1: Workflow = Workflow.mode1("rhino", modelUpdate, statusListener)
    private var mode22: Workflow = Workflow.mode22("rhino", modelUpdate, statusListener)

    private lateinit var device: String

    fun stop() {
        mode1.stop()
    }

    fun start(context: Context) {
        this.context = context

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var selectedMode = pref.getString("pref.mode", "")
        var adapterName = pref.getString("pref.adapter.id", "OBDII")

        Log.i(LOG_KEY, "Selected OBD mode: $selectedMode")

        this.device = adapterName.toString()

        when (selectedMode) {
            "Generic mode" -> {
                var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())
                Log.i(LOG_KEY, "Generic mode, selected pids: $selectedPids")

                mode1.start(BluetoothConnection(this.device.toString()), selectedPids)
            }

            else -> {
                var selectedPids = pref.getStringSet("pref.pids.mode22", emptySet())

                Log.i(LOG_KEY, "Mode 22, selected pids: $selectedPids")
                mode22.start(BluetoothConnection(this.device.toString()), selectedPids)
            }
        }

        Log.i(LOG_KEY, "Start collecting process for device $adapterName")
    }
}