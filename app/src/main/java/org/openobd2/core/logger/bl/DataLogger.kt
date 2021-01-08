package org.openobd2.core.logger.bl

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.workflow.State
import org.openobd2.core.workflow.Workflow

internal class DataLogger {

    private var modelUpdate = ModelChangePublisher()

    private var state = object : State {
        override fun onStarting() {
            Log.i("DATA_LOGGER_DL", "Start collecting process for the Device: $device")
            modelUpdate.data.clear();
        }

        override fun onComplete () {
            Log.i(
                "DATA_LOGGER_DL",
                "Collecting process completed for the Device: $device"
            )
        }

        override fun onStopping() {
            Log.i("DATA_LOGGER_DL", "Stop collecting process for the Device: $device")
        }
    }

    private var mode1: Workflow = Workflow
        .mode1()
        .equationEngine("rhino")
        .subscriber(modelUpdate)
        .state(state)
        .buildMode1()
    private var mode22: Workflow = Workflow
        .mode22()
        .equationEngine("rhino")
        .subscriber(modelUpdate)
        .state(state)
        .buildMode2()

    private lateinit var device: String

    fun stop() {
        mode1.stop()
    }

    fun start(context: Context) {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var selectedMode = pref.getString("pref.mode", "")
        var adapterName = pref.getString("pref.adapter.id", "OBDII")

        Log.i("DATA_LOGGER_SVC", "Selected OBD mode: $selectedMode")

        this.device = adapterName.toString()

        if (selectedMode.toString().contains("Generic")) {
            var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())
            Log.i("DATA_LOGGER_SVC", "Selected pids: $selectedPids")

            mode1.start(BluetoothConnection(this.device.toString()), selectedPids)
        } else {
            var selectedPids = pref.getStringSet("pref.pids.mode22", emptySet())

            Log.i("DATA_LOGGER_SVC", "Selected pids: $selectedPids")
            mode22.start(BluetoothConnection(this.device.toString()), selectedPids)
        }
        Log.i("DATA_LOGGER_SVC", "Start collecting process for device $adapterName")
    }
}